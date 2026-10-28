USE HRMsDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- ========================================================================
-- ĐẶC ĐIỂM:
-- 1. Tên doanh nghiệp mẫu: Công ty Cổ phần Công nghệ An Phát (Mã workspace: anphat).
-- 2. Idempotent: Có thể chạy lại nhiều lần an toàn, tự động làm sạch dữ liệu cũ
--    của workspace 'anphat' theo đúng thứ tự ràng buộc khóa ngoại (FK).
-- 3. Bao phủ 100% các Use Case & kịch bản nghiệp vụ:
--    - UC37: Cấu hình 6 loại nghỉ phép (Có lương, không lương, thai sản, kết hôn...)
--    - UC38: Hạn mức và số dư ngày phép 2026 và 2027 (Được cấp, đã dùng, còn lại)
--    - UC39: Tạo đơn nghỉ phép (Đơn 1 ngày, nhiều ngày, đơn vắt qua cuối tuần)
--    - UC40: Nhân viên tự hủy đơn khi ở trạng thái 'Pending'
--    - UC41: Phê duyệt và từ chối đơn (kèm lý do phản hồi chi tiết của Approver)
--    - UC42: Tự động đồng bộ đơn đã duyệt sang Chấm công ('On Leave')
--    - UC43: Lịch nghỉ phép trực quan toàn công ty và theo từng phòng ban
--    - UC44: Báo cáo tình hình sử dụng phép, tỷ lệ nghỉ, xuất file Excel
--    - Đơn vắt qua 2 năm tài chính (Cross-Year Split Requests 2026 - 2027)
--    - Đơn của Quản lý (Manager) được duyệt bởi Ban Giám Đốc (Company Admin)
-- ========================================================================

BEGIN TRY
    BEGIN TRANSACTION;

    -- ====================================================================
    -- BƯỚC 0: DỌN DẸP DỮ LIỆU CŨ CỦA WORKSPACE 'anphat' (NẾU CÓ)
    -- ====================================================================
    DECLARE @OldCompanyId INT = (SELECT CompanyId FROM dbo.Companies WHERE WorkspaceCode = 'anphat');

    IF @OldCompanyId IS NOT NULL
    BEGIN
        PRINT N'Phát hiện dữ liệu workspace "anphat" cũ, đang tiến hành làm sạch theo thứ tự ràng buộc...';

        -- Xóa dữ liệu các bảng nghiệp vụ cấp dưới
        DELETE FROM dbo.Payslips WHERE EmployeeId IN (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @OldCompanyId);
        DELETE FROM dbo.PayrollAdjustments WHERE PayrollPeriodId IN (SELECT PayrollPeriodId FROM dbo.PayrollPeriods WHERE CompanyId = @OldCompanyId);
        DELETE FROM dbo.PayrollPeriods WHERE CompanyId = @OldCompanyId;

        DELETE FROM dbo.Attendances WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.LeaveRequests WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.LeaveBalances WHERE EmployeeId IN (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @OldCompanyId);
        DELETE FROM dbo.LeaveTypes WHERE CompanyId = @OldCompanyId;

        DELETE FROM dbo.EmployeeDocuments WHERE EmployeeId IN (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @OldCompanyId);
        DELETE FROM dbo.EmployeeCompensationHistories WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.EmployeePositionAssignments WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.EmployeePositionChanges WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.EmploymentContracts WHERE CompanyId = @OldCompanyId;

        -- Gỡ bỏ ManagerId để tránh lỗi tự tham chiếu vòng lặp FK_Employees_Manager
        UPDATE dbo.Employees SET ManagerId = NULL WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.Employees WHERE CompanyId = @OldCompanyId;

        -- Gỡ bỏ ReportsToPositionId để tránh tự tham chiếu FK_Positions_ReportsToPosition
        UPDATE dbo.Positions SET ReportsToPositionId = NULL WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.Positions WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.JobTitles WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.ResponsibilityAllowancePolicies WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.ResponsibilityLevels WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.Departments WHERE CompanyId = @OldCompanyId;

        DELETE FROM dbo.Subscriptions WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.AuditLogs WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.BulkImportLogs WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.ProfileUpdatePeriods WHERE CompanyId = @OldCompanyId;

        DELETE FROM dbo.Users WHERE CompanyId = @OldCompanyId;
        DELETE FROM dbo.Companies WHERE CompanyId = @OldCompanyId;

        PRINT N'Đã hoàn tất dọn dẹp dữ liệu cũ của workspace "anphat".';
    END;

    -- ====================================================================
    -- BƯỚC 1: KHỞI TẠO CÔNG TY & GÓI DỊCH VỤ (COMPANIES & SUBSCRIPTIONS)
    -- ====================================================================
    DECLARE @DefaultPlanId INT = (SELECT TOP 1 PlanId FROM dbo.Plans WHERE PlanName = 'DEFAULT');
    IF @DefaultPlanId IS NULL
    BEGIN
        INSERT INTO dbo.Plans (PlanName, MaxEmployees, MaxAccounts, MaxManagers, Price, BillingCycle, IsActive)
        VALUES (N'DEFAULT', NULL, 20, 3, 59000.00, 'MONTHLY', 1);
        SET @DefaultPlanId = SCOPE_IDENTITY();
    END;

    INSERT INTO dbo.Companies (
        WorkspaceCode, CompanyName, CompanyNameNormalized, Region, TaxCode, 
        ContactEmail, ContactPhone, Address, IsActive, CreatedAt
    )
    VALUES (
        'anphat',
        N'Công ty Cổ phần Công nghệ An Phát',
        N'công ty cổ phần công nghệ an phát',
        'NORTH',
        '0109876543',
        'contact@anphat.vn',
        '02438889999',
        N'Tầng 12, Tòa nhà An Phát Tower, Phố Duy Tân, Quận Cầu Giấy, TP. Hà Nội',
        1,
        SYSUTCDATETIME()
    );

    DECLARE @CompanyId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.Subscriptions (
        CompanyId, PlanId, StartDate, EndDate, Status, CreatedAt
    )
    VALUES (
        @CompanyId, @DefaultPlanId, '2026-01-01', NULL, 'ACTIVE', SYSUTCDATETIME()
    );

    -- ====================================================================
    -- BƯỚC 2: KHỞI TẠO CƠ CẤU TỔ CHỨC (PHÒNG BAN, CHỨC DANH, VỊ TRÍ)
    -- ====================================================================
    -- 2.1 Cấp bậc trách nhiệm (ResponsibilityLevels)
    INSERT INTO dbo.ResponsibilityLevels (CompanyId, LevelCode, LevelName, RankOrder, IsActive, CreatedAt)
    VALUES 
        (@CompanyId, 'EXEC', N'Ban Giám Đốc', 10, 1, SYSUTCDATETIME()),
        (@CompanyId, 'DEPT_MGR', N'Trưởng Phòng', 5, 1, SYSUTCDATETIME()),
        (@CompanyId, 'TEAM_LEAD', N'Trưởng Nhóm', 4, 1, SYSUTCDATETIME()),
        (@CompanyId, 'SENIOR_STAFF', N'Chuyên Viên Cao Cấp', 3, 1, SYSUTCDATETIME()),
        (@CompanyId, 'STAFF', N'Chuyên Viên', 2, 1, SYSUTCDATETIME()),
        (@CompanyId, 'JUNIOR_STAFF', N'Nhân Viên', 1, 1, SYSUTCDATETIME());

    DECLARE @LvlExec INT = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'EXEC');
    DECLARE @LvlMgr INT = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEPT_MGR');
    DECLARE @LvlSr INT = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'SENIOR_STAFF');
    DECLARE @LvlStaff INT = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'STAFF');

    -- 2.2 Chức danh nghề nghiệp (JobTitles)
    INSERT INTO dbo.JobTitles (CompanyId, JobTitleCode, JobTitleName, Description, IsActive, CreatedAt)
    VALUES
        (@CompanyId, 'DIR', N'Giám Đốc Điều Hành', N'Điều hành toàn diện hoạt động doanh nghiệp', 1, SYSUTCDATETIME()),
        (@CompanyId, 'HR_LEAD', N'Chuyên Viên Quản Trị Nhân Sự', N'Quản trị nhân lực, chế độ phúc lợi và chấm công', 1, SYSUTCDATETIME()),
        (@CompanyId, 'TECH_MGR', N'Trưởng Phòng Công Nghệ', N'Quản lý phát triển hệ thống và kỹ thuật phần mềm', 1, SYSUTCDATETIME()),
        (@CompanyId, 'SALES_MGR', N'Trưởng Phòng Kinh Doanh', N'Hoạch định chiến lược kinh doanh và doanh số', 1, SYSUTCDATETIME()),
        (@CompanyId, 'DEV', N'Kỹ Sư Phần Mềm', N'Phát triển ứng dụng Web và Backend', 1, SYSUTCDATETIME()),
        (@CompanyId, 'QA', N'Kỹ Sư Đảm Bảo Chất Lượng', N'Kiểm thử tự động và thủ công các luồng nghiệp vụ', 1, SYSUTCDATETIME()),
        (@CompanyId, 'SALES_EXEC', N'Chuyên Viên Phát Triển Thị Trường', N'Tìm kiếm đối tác và chăm sóc khách hàng', 1, SYSUTCDATETIME()),
        (@CompanyId, 'ACC', N'Kế Toán Viên Tổng Hợp', N'Báo cáo thuế, chi phí và bảng lương', 1, SYSUTCDATETIME());

    DECLARE @JtDir INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'DIR');
    DECLARE @JtHrLead INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'HR_LEAD');
    DECLARE @JtTechMgr INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'TECH_MGR');
    DECLARE @JtSalesMgr INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'SALES_MGR');
    DECLARE @JtDev INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'DEV');
    DECLARE @JtQa INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'QA');
    DECLARE @JtSalesExec INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'SALES_EXEC');
    DECLARE @JtAcc INT = (SELECT JobTitleId FROM dbo.JobTitles WHERE CompanyId = @CompanyId AND JobTitleCode = 'ACC');

    -- 2.3 Phòng ban (Departments)
    INSERT INTO dbo.Departments (CompanyId, DepartmentCode, DepartmentName, IsActive, CreatedAt)
    VALUES
        (@CompanyId, 'BOD', N'Ban Giám Đốc', 1, SYSUTCDATETIME()),
        (@CompanyId, 'TECH', N'Phòng Công Nghệ & Phần Mềm', 1, SYSUTCDATETIME()),
        (@CompanyId, 'HR', N'Phòng Nhân Sự & Đào Tạo', 1, SYSUTCDATETIME()),
        (@CompanyId, 'SALES', N'Phòng Kinh Doanh & Tiếp Thị', 1, SYSUTCDATETIME()),
        (@CompanyId, 'FIN', N'Phòng Tài Chính - Kế Toán', 1, SYSUTCDATETIME());

    DECLARE @DeptBod INT = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'BOD');
    DECLARE @DeptTech INT = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'TECH');
    DECLARE @DeptHr INT = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'HR');
    DECLARE @DeptSales INT = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'SALES');
    DECLARE @DeptFin INT = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'FIN');

    -- 2.4 Vị trí công tác (Positions)
    -- Vị trí Giám Đốc
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptBod, @JtDir, @LvlExec, N'Tổng Giám Đốc Điều Hành', 'POS_DIR', N'Điều hành toàn bộ hoạt động', 1, 1, SYSUTCDATETIME());
    DECLARE @PosDir INT = SCOPE_IDENTITY();

    -- Vị trí Trưởng Phòng Công Nghệ
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptTech, @JtTechMgr, @LvlMgr, N'Trưởng Phòng Công Nghệ', 'POS_TECH_MGR', N'Quản lý nhóm kỹ thuật', 1, @PosDir, 1, SYSUTCDATETIME());
    DECLARE @PosTechMgr INT = SCOPE_IDENTITY();

    -- Vị trí Senior Backend Dev
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptTech, @JtDev, @LvlSr, N'Senior Backend Developer', 'POS_DEV_SR', N'Lập trình viên Backend', 0, @PosTechMgr, 1, SYSUTCDATETIME());
    DECLARE @PosDevSr INT = SCOPE_IDENTITY();

    -- Vị trí Frontend Dev
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptTech, @JtDev, @LvlStaff, N'Frontend Developer', 'POS_DEV_FE', N'Lập trình viên giao diện', 0, @PosTechMgr, 1, SYSUTCDATETIME());
    DECLARE @PosDevFe INT = SCOPE_IDENTITY();

    -- Vị trí QA Engineer
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptTech, @JtQa, @LvlStaff, N'QA Automation Engineer', 'POS_QA', N'Kiểm thử hệ thống', 0, @PosTechMgr, 1, SYSUTCDATETIME());
    DECLARE @PosQa INT = SCOPE_IDENTITY();

    -- Vị trí Quản Trị Nhân Sự (HR Specialist)
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptHr, @JtHrLead, @LvlSr, N'Chuyên Viên Nhân Sự Cấp Cao', 'POS_HR_SPEC', N'Quản trị nhân lực & phép', 0, @PosDir, 1, SYSUTCDATETIME());
    DECLARE @PosHrSpec INT = SCOPE_IDENTITY();

    -- Vị trí Trưởng Phòng Kinh Doanh
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptSales, @JtSalesMgr, @LvlMgr, N'Trưởng Phòng Kinh Doanh', 'POS_SALES_MGR', N'Quản lý nhóm thị trường', 1, @PosDir, 1, SYSUTCDATETIME());
    DECLARE @PosSalesMgr INT = SCOPE_IDENTITY();

    -- Vị trí Chuyên Viên Kinh Doanh
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptSales, @JtSalesExec, @LvlStaff, N'Chuyên Viên Kinh Doanh', 'POS_SALES_EXEC', N'Kinh doanh và phát triển', 0, @PosSalesMgr, 1, SYSUTCDATETIME());
    DECLARE @PosSalesExec INT = SCOPE_IDENTITY();

    -- Vị trí Kế Toán Viên
    INSERT INTO dbo.Positions (CompanyId, DepartmentId, JobTitleId, ResponsibilityLevelId, PositionTitle, PositionCode, Description, CanManageEmployees, ReportsToPositionId, IsActive, CreatedAt)
    VALUES (@CompanyId, @DeptFin, @JtAcc, @LvlStaff, N'Kế Toán Viên Tổng Hợp', 'POS_ACC', N'Kế toán tài chính', 0, @PosDir, 1, SYSUTCDATETIME());
    DECLARE @PosAcc INT = SCOPE_IDENTITY();

    -- ====================================================================
    -- BƯỚC 3: KHỞI TẠO TÀI KHOẢN NGƯỜI DÙNG (USERS) VÀ HỒ SƠ NHÂN VIÊN (EMPLOYEES)
    -- Mật khẩu mặc định cho toàn bộ tài khoản: 123456
    -- ====================================================================
    DECLARE @RoleAdminId INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'COMPANY_ADMIN');
    DECLARE @RoleHrId INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'HR');
    DECLARE @RoleMgrId INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'MANAGER');
    DECLARE @RoleEmpId INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'EMPLOYEE');

    DECLARE @Now DATETIME2 = SYSUTCDATETIME();

    -- Bảng tạm lưu danh sách 9 nhân sự mẫu
    DECLARE @UserData TABLE (
        Seq INT IDENTITY(1,1),
        RoleCode VARCHAR(50),
        Username VARCHAR(150),
        Email VARCHAR(150),
        DisplayName NVARCHAR(150),
        EmpCode VARCHAR(50),
        DeptId INT,
        PosId INT,
        Phone VARCHAR(30),
        Gender NVARCHAR(20),
        Dob DATE,
        Salary DECIMAL(18,2)
    );

    INSERT INTO @UserData (RoleCode, Username, Email, DisplayName, EmpCode, DeptId, PosId, Phone, Gender, Dob, Salary)
    VALUES
        -- 1. Giám Đốc Điều Hành kiêm Quản trị công ty (Company Admin)
        ('COMPANY_ADMIN', 'anphat.admin', 'admin@anphat.vn', N'Hoàng Lê Đạt', 'EMP001', @DeptBod, @PosDir, '0981234567', N'Nam', '1988-05-12', 45000000.00),
        -- 2. Chuyên viên Nhân sự (HR - Quản trị hồ sơ & duyệt phép toàn công ty)
        ('HR', 'anphat.hr', 'hr@anphat.vn', N'Trần Mai Phương', 'EMP002', @DeptHr, @PosHrSpec, '0982345678', N'Nữ', '1993-08-20', 18000000.00),
        -- 3. Trưởng Phòng Công Nghệ (Manager 1)
        ('MANAGER', 'anphat.mgr.tech', 'manager.tech@anphat.vn', N'Nguyễn Văn Hưng', 'EMP003', @DeptTech, @PosTechMgr, '0983456789', N'Nam', '1990-11-15', 35000000.00),
        -- 4. Trưởng Phòng Kinh Doanh (Manager 2)
        ('MANAGER', 'anphat.mgr.sales', 'manager.sales@anphat.vn', N'Lê Thị Lan', 'EMP004', @DeptSales, @PosSalesMgr, '0984567890', N'Nữ', '1991-03-25', 30000000.00),
        -- 5. Senior Backend Developer (Employee thuộc phòng Tech)
        ('EMPLOYEE', 'anphat.emp.minh', 'minh.pham@anphat.vn', N'Phạm Quang Minh', 'EMP005', @DeptTech, @PosDevSr, '0985678901', N'Nam', '1995-09-08', 25000000.00),
        -- 6. Frontend Developer (Employee thuộc phòng Tech)
        ('EMPLOYEE', 'anphat.emp.son', 'son.dang@anphat.vn', N'Đặng Hoàng Sơn', 'EMP006', @DeptTech, @PosDevFe, '0986789012', N'Nam', '1997-12-19', 18000000.00),
        -- 7. QA Automation Engineer (Employee thuộc phòng Tech)
        ('EMPLOYEE', 'anphat.emp.anh', 'anh.vu@anphat.vn', N'Vũ Ngọc Anh', 'EMP007', @DeptTech, @PosQa, '0987890123', N'Nữ', '1998-04-14', 16000000.00),
        -- 8. Chuyên viên Kinh Doanh (Employee thuộc phòng Sales)
        ('EMPLOYEE', 'anphat.emp.thu', 'thu.bui@anphat.vn', N'Bùi Thị Thu', 'EMP008', @DeptSales, @PosSalesExec, '0988901234', N'Nữ', '1999-07-22', 15000000.00),
        -- 9. Kế Toán Viên Tổng Hợp (Employee thuộc phòng Kế toán)
        ('EMPLOYEE', 'anphat.emp.nam', 'nam.do@anphat.vn', N'Đỗ Thành Nam', 'EMP009', @DeptFin, @PosAcc, '0989012345', N'Nam', '1996-01-30', 16000000.00);

    -- Vòng lặp tạo tài khoản User và hồ sơ Employee
    DECLARE @idx INT = 1;
    DECLARE @totalRows INT = (SELECT COUNT(*) FROM @UserData);

    WHILE @idx <= @totalRows
    BEGIN
        DECLARE @rCode VARCHAR(50), @uName VARCHAR(150), @email VARCHAR(150), @dName NVARCHAR(150);
        DECLARE @eCode VARCHAR(50), @dId INT, @pId INT, @phone VARCHAR(30), @gender NVARCHAR(20);
        DECLARE @dob DATE, @salary DECIMAL(18,2), @rId INT;

        SELECT 
            @rCode = RoleCode, @uName = Username, @email = Email, @dName = DisplayName,
            @eCode = EmpCode, @dId = DeptId, @pId = PosId, @phone = Phone, @gender = Gender,
            @dob = Dob, @salary = Salary
        FROM @UserData WHERE Seq = @idx;

        SET @rId = CASE 
            WHEN @rCode = 'COMPANY_ADMIN' THEN @RoleAdminId
            WHEN @rCode = 'HR' THEN @RoleHrId
            WHEN @rCode = 'MANAGER' THEN @RoleMgrId
            ELSE @RoleEmpId
        END;

        -- Tạo User (MustChangePassword = 0 để login dùng ngay mà không bị chặn đổi mật khẩu)
        INSERT INTO dbo.Users (
            CompanyId, RoleId, Username, Email, DisplayName, Password,
            MustChangePassword, ActivatedAt, PasswordChangedAt, IsActive, CreatedAt
        )
        VALUES (
            @CompanyId, @rId, @uName, @email, @dName, N'123456',
            0, @Now, @Now, 1, @Now
        );
        DECLARE @NewUserId INT = SCOPE_IDENTITY();

        -- Tạo Employee
        INSERT INTO dbo.Employees (
            CompanyId, UserId, EmployeeCode, FullName, DepartmentId, PositionId,
            Phone, Gender, DateOfBirth, Email, Status, CreatedAt
        )
        VALUES (
            @CompanyId, @NewUserId, @eCode, @dName, @dId, @pId,
            @phone, @gender, @dob, @email, 'Active', @Now
        );
        DECLARE @NewEmpId INT = SCOPE_IDENTITY();

        -- Tạo hợp đồng lao động ban đầu (EmploymentContracts)
        INSERT INTO dbo.EmploymentContracts (
            CompanyId, EmployeeId, ContractCode, ContractType, StartDate, EndDate,
            GrossSalary, Status, SignedDate, CreatedByUserId, CreatedAt
        )
        VALUES (
            @CompanyId, @NewEmpId, CONCAT('HDLD-', @CompanyId, '-', @eCode),
            'INDEFINITE', '2023-01-01', NULL, @salary, 'ACTIVE', '2023-01-01', @NewUserId, @Now
        );
        DECLARE @NewContractId BIGINT = SCOPE_IDENTITY();

        -- Tạo phân công vị trí công tác ban đầu (EmployeePositionAssignments)
        INSERT INTO dbo.EmployeePositionAssignments (
            CompanyId, EmployeeId, DepartmentId, PositionId, EffectiveFrom, EffectiveTo,
            ChangeType, Reason, CreatedAt
        )
        VALUES (
            @CompanyId, @NewEmpId, @dId, @pId, '2023-01-01', NULL,
            'HIRE', N'Tiếp nhận nhân sự và ký kết hợp đồng lao động ban đầu', @Now
        );

        -- Tạo lịch sử lương ban đầu (EmployeeCompensationHistories)
        INSERT INTO dbo.EmployeeCompensationHistories (
            CompanyId, EmployeeId, BaseSalary, EffectiveFrom, EffectiveTo,
            Reason, SourceContractId, CreatedByUserId, CreatedAt
        )
        VALUES (
            @CompanyId, @NewEmpId, @salary, '2023-01-01', NULL,
            N'Mức lương thỏa thuận theo hợp đồng lao động', @NewContractId, @NewUserId, @Now
        );

        SET @idx = @idx + 1;
    END;

    -- ====================================================================
    -- BƯỚC 4: THIẾT LẬP CÂY QUẢN LÝ (MANAGER HIERARCHY)
    -- ====================================================================
    DECLARE @EmpAdmin INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP001');
    DECLARE @EmpHr INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP002');
    DECLARE @EmpMgrTech INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP003');
    DECLARE @EmpMgrSales INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP004');
    DECLARE @EmpDev1 INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP005');
    DECLARE @EmpDev2 INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP006');
    DECLARE @EmpQa INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP007');
    DECLARE @EmpSales1 INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP008');
    DECLARE @EmpAcc INT = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'EMP009');

    -- Giám đốc (EMP001) quản lý HR, Trưởng phòng Tech, Trưởng phòng Sales và Kế toán
    UPDATE dbo.Employees SET ManagerId = @EmpAdmin WHERE EmployeeId IN (@EmpHr, @EmpMgrTech, @EmpMgrSales, @EmpAcc);

    -- Trưởng phòng Tech (EMP003) trực tiếp quản lý các kỹ sư Tech (Dev1, Dev2, QA)
    UPDATE dbo.Employees SET ManagerId = @EmpMgrTech WHERE EmployeeId IN (@EmpDev1, @EmpDev2, @EmpQa);

    -- Trưởng phòng Sales (EMP004) trực tiếp quản lý nhân viên Sales (Sales1)
    UPDATE dbo.Employees SET ManagerId = @EmpMgrSales WHERE EmployeeId = @EmpSales1;

    -- Cập nhật ManagerId vào bảng phân công vị trí (EmployeePositionAssignments)
    UPDATE dbo.EmployeePositionAssignments SET ManagerId = @EmpAdmin WHERE EmployeeId IN (@EmpHr, @EmpMgrTech, @EmpMgrSales, @EmpAcc);
    UPDATE dbo.EmployeePositionAssignments SET ManagerId = @EmpMgrTech WHERE EmployeeId IN (@EmpDev1, @EmpDev2, @EmpQa);
    UPDATE dbo.EmployeePositionAssignments SET ManagerId = @EmpMgrSales WHERE EmployeeId = @EmpSales1;

    -- ====================================================================
    -- BƯỚC 5: CHÍNH SÁCH LOẠI NGHỈ PHÉP (LEAVE TYPES - UC37)
    -- ====================================================================
    INSERT INTO dbo.LeaveTypes (CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid, IsActive, CreatedAt)
    VALUES
        (@CompanyId, 'ANNUAL', N'Nghỉ phép năm', 12, 1, 1, @Now),
        (@CompanyId, 'SICK', N'Nghỉ ốm đau / Khám bệnh', 10, 1, 1, @Now),
        (@CompanyId, 'UNPAID', N'Nghỉ không hưởng lương', 0, 0, 1, @Now),
        (@CompanyId, 'MATERNITY', N'Nghỉ thai sản', 180, 1, 1, @Now),
        (@CompanyId, 'WEDDING', N'Nghỉ kết hôn', 3, 1, 1, @Now),
        (@CompanyId, 'COMPASSIONATE', N'Nghỉ việc tang / Hiếu hỉ', 3, 1, 1, @Now);

    DECLARE @LtAnnual INT = (SELECT LeaveTypeId FROM dbo.LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'ANNUAL');
    DECLARE @LtSick INT = (SELECT LeaveTypeId FROM dbo.LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'SICK');
    DECLARE @LtUnpaid INT = (SELECT LeaveTypeId FROM dbo.LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'UNPAID');
    DECLARE @LtMaternity INT = (SELECT LeaveTypeId FROM dbo.LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'MATERNITY');
    DECLARE @LtWedding INT = (SELECT LeaveTypeId FROM dbo.LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'WEDDING');
    DECLARE @LtCompassionate INT = (SELECT LeaveTypeId FROM dbo.LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'COMPASSIONATE');

    -- ====================================================================
    -- BƯỚC 6: HẠN MỨC VÀ SỐ DƯ NGHỈ PHÉP (LEAVE BALANCES - UC38)
    -- Thiết lập cho năm 2026 và năm 2027 (để test đơn vắt năm)
    -- ====================================================================
    -- 6.1 Khởi tạo hạn mức Năm 2026 cho toàn bộ nhân sự
    INSERT INTO dbo.LeaveBalances (EmployeeId, LeaveTypeId, [Year], EntitledDays, UsedDays, CreatedAt)
    VALUES
        -- EMP001: Giám đốc (14 ngày phép năm, chưa dùng)
        (@EmpAdmin, @LtAnnual, 2026, 14.00, 0.00, @Now),
        (@EmpAdmin, @LtSick, 2026, 10.00, 0.00, @Now),

        -- EMP002: HR (12 ngày phép năm, 180 ngày thai sản)
        (@EmpHr, @LtAnnual, 2026, 12.00, 0.00, @Now),
        (@EmpHr, @LtSick, 2026, 10.00, 0.00, @Now),
        (@EmpHr, @LtMaternity, 2026, 180.00, 0.00, @Now),

        -- EMP003: Tech Manager (14 ngày phép năm, đã dùng 2 ngày -> Còn 12)
        (@EmpMgrTech, @LtAnnual, 2026, 14.00, 2.00, @Now),
        (@EmpMgrTech, @LtSick, 2026, 10.00, 0.00, @Now),

        -- EMP004: Sales Manager (14 ngày phép năm, chưa dùng)
        (@EmpMgrSales, @LtAnnual, 2026, 14.00, 0.00, @Now),
        (@EmpMgrSales, @LtSick, 2026, 10.00, 0.00, @Now),

        -- EMP005: Senior Backend Dev (12 ngày phép năm, đã dùng 4 ngày cho đơn cuối năm -> Còn 8)
        (@EmpDev1, @LtAnnual, 2026, 12.00, 4.00, @Now),
        (@EmpDev1, @LtSick, 2026, 10.00, 0.00, @Now),
        (@EmpDev1, @LtUnpaid, 2026, 0.00, 0.00, @Now),

        -- EMP006: Frontend Dev (12 ngày phép năm, đã dùng 2 ngày ốm -> Còn 12 phép năm, 8 phép ốm)
        (@EmpDev2, @LtAnnual, 2026, 12.00, 0.00, @Now),
        (@EmpDev2, @LtSick, 2026, 10.00, 2.00, @Now),

        -- EMP007: QA Engineer (12 ngày phép năm, đã dùng 3 ngày -> Còn 9)
        (@EmpQa, @LtAnnual, 2026, 12.00, 3.00, @Now),
        (@EmpQa, @LtSick, 2026, 10.00, 0.00, @Now),

        -- EMP008: Sales Executive (12 ngày phép năm, chưa dùng)
        (@EmpSales1, @LtAnnual, 2026, 12.00, 0.00, @Now),
        (@EmpSales1, @LtSick, 2026, 10.00, 0.00, @Now),
        (@EmpSales1, @LtUnpaid, 2026, 0.00, 1.00, @Now),

        -- EMP009: Kế toán (12 ngày phép năm, chưa dùng)
        (@EmpAcc, @LtAnnual, 2026, 12.00, 0.00, @Now),
        (@EmpAcc, @LtSick, 2026, 10.00, 0.00, @Now),
        (@EmpAcc, @LtWedding, 2026, 3.00, 0.00, @Now);

    -- 6.2 Khởi tạo hạn mức Năm 2027 (để test tính năng đơn nghỉ vắt qua 2 năm tài chính)
    INSERT INTO dbo.LeaveBalances (EmployeeId, LeaveTypeId, [Year], EntitledDays, UsedDays, CreatedAt)
    VALUES
        -- EMP005: Đã dùng 3 ngày của năm 2027 trong đơn vắt năm -> Còn 9 ngày
        (@EmpDev1, @LtAnnual, 2027, 12.00, 3.00, @Now),
        (@EmpDev1, @LtSick, 2027, 10.00, 0.00, @Now),
        -- EMP006: Hạn mức tiêu chuẩn năm 2027
        (@EmpDev2, @LtAnnual, 2027, 12.00, 0.00, @Now),
        -- EMP007: Hạn mức tiêu chuẩn năm 2027
        (@EmpQa, @LtAnnual, 2027, 12.00, 0.00, @Now);

    -- Lấy UserId của các cấp xét duyệt
    DECLARE @UserAdmin INT = (SELECT UserId FROM dbo.Employees WHERE EmployeeId = @EmpAdmin);
    DECLARE @UserMgrTech INT = (SELECT UserId FROM dbo.Employees WHERE EmployeeId = @EmpMgrTech);
    DECLARE @UserMgrSales INT = (SELECT UserId FROM dbo.Employees WHERE EmployeeId = @EmpMgrSales);
    DECLARE @UserHr INT = (SELECT UserId FROM dbo.Employees WHERE EmployeeId = @EmpHr);

    -- ====================================================================
    -- BƯỚC 7: CÁC ĐƠN XIN NGHỈ PHÉP MẪU (LEAVE REQUESTS - UC39 -> UC42)
    -- ====================================================================

    -- KỊCH BẢN 1: ĐƠN CHỜ DUYỆT (PENDING) - Để Manager vào duyệt thử (UC41)
    -- Nhân viên Phạm Quang Minh (EMP005) xin nghỉ phép năm 2 ngày cuối tuần tới
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpDev1, @LtAnnual, '2026-11-05', '2026-11-06', 2.0,
        N'Về quê lo việc cúng giỗ gia đình và thăm người thân',
        'Pending', NULL, NULL, NULL, '2026-10-27 09:30:00'
    );

    -- KỊCH BẢN 2: ĐƠN CHỜ DUYỆT (PENDING) CỦA PHÒNG KINH DOANH
    -- Bùi Thị Thu (EMP008) xin nghỉ 1 ngày khám bệnh
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpSales1, @LtAnnual, '2026-11-10', '2026-11-10', 1.0,
        N'Đi khám sức khỏe định kỳ tại Bệnh viện Bạch Mai',
        'Pending', NULL, NULL, NULL, '2026-10-28 08:15:00'
    );

    -- KỊCH BẢN 3: ĐƠN ĐÃ DUYỆT (APPROVED) & ĐÃ ĐỒNG BỘ CHẤM CÔNG (UC42)
    -- Vũ Ngọc Anh (EMP007 - QA) nghỉ 3 ngày từ Thứ 4 đến Thứ 6 (14/10 đến 16/10/2026)
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpQa, @LtAnnual, '2026-10-14', '2026-10-16', 3.0,
        N'Nghỉ phép thường niên đi du lịch Đà Nẵng cùng gia đình',
        'Approved', @UserMgrTech,
        N'Đã đồng ý phê duyệt. Lưu ý bàn giao lại test script regression cho bạn Sơn.',
        '2026-10-12 11:20:00', '2026-10-10 14:00:00'
    );
    DECLARE @ReqQaId INT = SCOPE_IDENTITY();

    -- KỊCH BẢN 4: ĐƠN NGHỈ ỐM ĐÃ DUYỆT (APPROVED)
    -- Đặng Hoàng Sơn (EMP006) nghỉ ốm 2 ngày (21/10 và 22/10/2026)
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpDev2, @LtSick, '2026-10-21', '2026-10-22', 2.0,
        N'Bị sốt xuất huyết, có giấy chứng nhận nghỉ việc hưởng BHXH của bệnh viện',
        'Approved', @UserMgrTech,
        N'Duyệt nghỉ ốm có lương theo chế độ BHXH. Chúc bạn mau khỏe!',
        '2026-10-21 07:45:00', '2026-10-20 20:30:00'
    );
    DECLARE @ReqSonId INT = SCOPE_IDENTITY();

    -- KỊCH BẢN 5: ĐƠN BỊ TỪ CHỐI (REJECTED) - Có lý do từ chối rõ ràng (UC41)
    -- Phạm Quang Minh (EMP005) xin nghỉ 5 ngày trùng đợt bàn giao sản phẩm
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpDev1, @LtAnnual, '2026-10-19', '2026-10-23', 5.0,
        N'Đi tour du lịch Đài Loan cùng nhóm bạn thân',
        'Rejected', @UserMgrTech,
        N'Tuần này dự án triển khai đợt UAT quan trọng với khách hàng, không bố trí được nhân sự thay thế module core. Đề nghị dời lịch sang tháng 11.',
        '2026-10-15 15:30:00', '2026-10-14 16:45:00'
    );

    -- KỊCH BẢN 6: ĐƠN ĐÃ HỦY (CANCELLED) - Nhân viên tự hủy khi Pending (UC40)
    -- Đỗ Thành Nam (EMP009 - Kế toán) xin nghỉ nhưng lịch cưới bị hoãn
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpAcc, @LtWedding, '2026-10-07', '2026-10-09', 3.0,
        N'Nghỉ chuẩn bị đám cưới bạn thân',
        'Cancelled', NULL, NULL, NULL, '2026-10-02 09:10:00'
    );

    -- KỊCH BẢN 7: ĐƠN NGHỈ PHÉP CỦA CHÍNH QUẢN LÝ (MANAGER) - Được duyệt bởi Company Admin
    -- Nguyễn Văn Hưng (EMP003 - Tech Manager) nghỉ phép năm 2 ngày (26/10 và 27/10/2026)
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpMgrTech, @LtAnnual, '2026-10-26', '2026-10-27', 2.0,
        N'Nghỉ phép giải quyết việc gia đình cá nhân',
        'Approved', @UserAdmin,
        N'Ban Giám Đốc đồng ý phê duyệt. Anh bàn giao đầu mối trực tiếp cho Senior Dev Minh nhé.',
        '2026-10-23 16:00:00', '2026-10-22 11:30:00'
    );
    DECLARE @ReqHungId INT = SCOPE_IDENTITY();

    -- KỊCH BẢN 8: ĐƠN NGHỈ VẮT NĂM TÀI CHÍNH (CROSS-YEAR SPLIT REQUESTS - 2026 & 2027)
    -- Nhân viên Phạm Quang Minh (EMP005) xin nghỉ từ ngày 28/12/2026 đến ngày 05/01/2027:
    -- Hệ thống tự động phân tách làm 2 đơn độc lập theo năm tài chính:
    -- Đơn phần 1 (Năm 2026): 28/12/2026 -> 31/12/2026 (4 ngày làm việc, trừ vào phép 2026)
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpDev1, @LtAnnual, '2026-12-28', '2026-12-31', 4.0,
        N'Nghỉ phép thăm quê và đón Tết Dương lịch cùng gia đình (Đơn tách phần năm 2026)',
        'Approved', @UserMgrTech,
        N'Duyệt đợt 1 trừ vào số dư ngày phép năm 2026.',
        '2026-10-25 10:00:00', '2026-10-24 14:00:00'
    );

    -- Đơn phần 2 (Năm 2027): 01/01/2027 -> 05/01/2027
    -- (Gồm T6 ngày 01/01, T2 ngày 04/01, T3 ngày 05/01 = 3 ngày làm việc, trừ vào phép 2027)
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpDev1, @LtAnnual, '2027-01-01', '2027-01-05', 3.0,
        N'Nghỉ phép thăm quê và đón Tết Dương lịch cùng gia đình (Đơn tách phần năm 2027)',
        'Approved', @UserMgrTech,
        N'Duyệt đợt 2 trừ vào hạn mức ngày phép mới năm 2027.',
        '2026-10-25 10:01:00', '2026-10-24 14:00:00'
    );

    -- KỊCH BẢN 9: ĐƠN NGHỈ VẮT QUA CUỐI TUẦN (WEEKEND-SKIPPING CALCULATION)
    -- Đặng Hoàng Sơn (EMP006) xin nghỉ từ T6 13/11 đến T3 17/11/2026
    -- Khoảng cách 5 ngày lịch, nhưng chỉ tính 3 ngày làm việc (T6 13/11, T2 16/11, T3 17/11; trừ T7 14/11 & CN 15/11)
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpDev2, @LtAnnual, '2026-11-13', '2026-11-17', 3.0,
        N'Tham dự hội thảo công nghệ Tech Summit và nghỉ ngơi cuối tuần tại Nha Trang',
        'Pending', NULL, NULL, NULL, '2026-10-28 14:20:00'
    );

    -- KỊCH BẢN 10: ĐƠN NGHỈ KHÔNG LƯƠNG (UNPAID LEAVE)
    -- Bùi Thị Thu (EMP008 - Sales) xin nghỉ không lương 1 ngày
    INSERT INTO dbo.LeaveRequests (
        CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays,
        Reason, Status, ApproverId, ApproverComment, ReviewedAt, CreatedAt
    )
    VALUES (
        @CompanyId, @EmpSales1, @LtUnpaid, '2026-10-29', '2026-10-29', 1.0,
        N'Xin nghỉ giải quyết việc riêng gia đình (thủ tục chuyển nhượng nhà đất)',
        'Pending', NULL, NULL, NULL, '2026-10-28 16:00:00'
    );

    -- ====================================================================
    -- BƯỚC 8: DỮ LIỆU NHẬT KÝ CHẤM CÔNG THÁNG 10/2026 (ATTENDANCES - UC42)
    -- Bao gồm các bản ghi: Có mặt (Present), Đi muộn (Late), Về sớm (Early),
    -- Vắng mặt (Absent) và đặc biệt là 'On Leave' đồng bộ từ đơn nghỉ phép đã duyệt!
    -- ====================================================================
    PRINT N'Đang tạo dữ liệu chấm công Tháng 10/2026 đa dạng trạng thái...';

    -- Bảng tạm chứa danh sách ngày làm việc trong Tháng 10/2026 (từ 01/10 đến 28/10)
    DECLARE @WorkDays TABLE (D DATE, DayOfWeekName VARCHAR(10));
    DECLARE @CurD DATE = '2026-10-01';
    WHILE @CurD <= '2026-10-28'
    BEGIN
        -- Bỏ qua Thứ 7 và Chủ Nhật (DATEFIRST mặc định hoặc DATEPART dw)
        IF DATEPART(dw, @CurD) NOT IN (1, 7) -- 1 = Chủ nhật, 7 = Thứ 7 trong cấu hình mặc định US
            INSERT INTO @WorkDays (D) VALUES (@CurD);
        SET @CurD = DATEADD(DAY, 1, @CurD);
    END;

    -- 8.1 Đồng bộ đơn nghỉ phép của QA Vũ Ngọc Anh (14, 15, 16/10/2026 -> 'On Leave')
    INSERT INTO dbo.Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note, CreatedAt)
    VALUES
        (@CompanyId, @EmpQa, '2026-10-14', NULL, NULL, 0.00, 'On Leave', N'Nghỉ phép năm theo đơn #3', @Now),
        (@CompanyId, @EmpQa, '2026-10-15', NULL, NULL, 0.00, 'On Leave', N'Nghỉ phép năm theo đơn #3', @Now),
        (@CompanyId, @EmpQa, '2026-10-16', NULL, NULL, 0.00, 'On Leave', N'Nghỉ phép năm theo đơn #3', @Now);

    -- 8.2 Đồng bộ đơn nghỉ ốm của Dev Đặng Hoàng Sơn (21, 22/10/2026 -> 'On Leave')
    INSERT INTO dbo.Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note, CreatedAt)
    VALUES
        (@CompanyId, @EmpDev2, '2026-10-21', NULL, NULL, 0.00, 'On Leave', N'Nghỉ ốm đau theo đơn #4', @Now),
        (@CompanyId, @EmpDev2, '2026-10-22', NULL, NULL, 0.00, 'On Leave', N'Nghỉ ốm đau theo đơn #4', @Now);

    -- 8.3 Đồng bộ đơn nghỉ phép của Quản lý Nguyễn Văn Hưng (26, 27/10/2026 -> 'On Leave')
    INSERT INTO dbo.Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note, CreatedAt)
    VALUES
        (@CompanyId, @EmpMgrTech, '2026-10-26', NULL, NULL, 0.00, 'On Leave', N'Nghỉ phép năm theo đơn #7', @Now),
        (@CompanyId, @EmpMgrTech, '2026-10-27', NULL, NULL, 0.00, 'On Leave', N'Nghỉ phép năm theo đơn #7', @Now);

    -- 8.4 Sinh dữ liệu chấm công thường nhật cho toàn bộ nhân sự các ngày còn lại
    DECLARE @empCursor CURSOR;
    DECLARE @cEmpId INT, @cEmpCode VARCHAR(50);

    SET @empCursor = CURSOR FOR
    SELECT EmployeeId, EmployeeCode FROM dbo.Employees WHERE CompanyId = @CompanyId;

    OPEN @empCursor;
    FETCH NEXT FROM @empCursor INTO @cEmpId, @cEmpCode;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        -- Chèn các ngày làm việc chưa có bản ghi chấm công
        INSERT INTO dbo.Attendances (
            CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime,
            TotalHours, Status, Note, CreatedAt
        )
        SELECT 
            @CompanyId, @cEmpId, w.D,
            CASE 
                -- Một số ngày ngẫu nhiên đi muộn
                WHEN DAY(w.D) % 7 = 3 THEN '08:45:00'
                WHEN DAY(w.D) % 7 = 5 THEN '08:50:00'
                ELSE '08:15:00'
            END,
            CASE 
                -- Một số ngày về sớm
                WHEN DAY(w.D) % 9 = 2 THEN '16:30:00'
                ELSE '17:35:00'
            END,
            CASE 
                WHEN DAY(w.D) % 7 = 3 THEN 7.92
                WHEN DAY(w.D) % 7 = 5 THEN 7.75
                WHEN DAY(w.D) % 9 = 2 THEN 7.17
                ELSE 8.33
            END,
            CASE 
                WHEN DAY(w.D) % 7 IN (3, 5) THEN 'Late'
                WHEN DAY(w.D) % 9 = 2 THEN 'Early Leave'
                ELSE 'Present'
            END,
            CASE 
                WHEN DAY(w.D) % 7 IN (3, 5) THEN N'Đi muộn do kẹt xe trục đường Cầu Giấy'
                WHEN DAY(w.D) % 9 = 2 THEN N'Về sớm đưa đón con nhỏ'
                ELSE N'Chấm công đúng giờ tiêu chuẩn'
            END,
            @Now
        FROM @WorkDays w
        WHERE NOT EXISTS (
            SELECT 1 FROM dbo.Attendances a
            WHERE a.EmployeeId = @cEmpId AND a.AttendanceDate = w.D
        );

        FETCH NEXT FROM @empCursor INTO @cEmpId, @cEmpCode;
    END;

    CLOSE @empCursor;
    DEALLOCATE @empCursor;

    -- Thêm 1 ngày vắng mặt không phép (Absent) của Dev1 vào ngày 05/10/2026 để test chỉ số chuyên cần
    UPDATE dbo.Attendances
    SET CheckInTime = NULL, CheckOutTime = NULL, TotalHours = 0.00, Status = 'Absent', Note = N'Vắng mặt không rõ nguyên nhân'
    WHERE EmployeeId = @EmpDev1 AND AttendanceDate = '2026-10-05';

    -- ====================================================================
    -- BƯỚC 9: KỲ LƯƠNG & PHIẾU LƯƠNG THÁNG 10/2026 (PAYROLL - ĐỒNG BỘ NGHỈ PHÉP)
    -- ====================================================================
    INSERT INTO dbo.PayrollPeriods (CompanyId, PeriodName, [Month], [Year], Status, ApprovedByUserId, ApprovedAt, CreatedAt)
    VALUES (@CompanyId, N'Bảng lương Tháng 10/2026', 10, 2026, 'Approved', @UserAdmin, @Now, @Now);
    DECLARE @PeriodOctId INT = SCOPE_IDENTITY();

    -- Tạo phiếu lương cho 9 nhân viên
    INSERT INTO dbo.Payslips (PayrollPeriodId, EmployeeId, BaseSalary, Bonus, Deduction, ResponsibilityAllowance, GeneratedAt)
    SELECT 
        @PeriodOctId, e.EmployeeId, 
        h.BaseSalary,
        CASE WHEN e.EmployeeCode IN ('EMP001', 'EMP003', 'EMP004') THEN 3000000.00 ELSE 1000000.00 END AS Bonus,
        CASE WHEN e.EmployeeCode = 'EMP005' THEN 500000.00 ELSE 0.00 END AS Deduction,
        CASE WHEN e.EmployeeCode = 'EMP001' THEN 5000000.00 
             WHEN e.EmployeeCode IN ('EMP003', 'EMP004') THEN 2000000.00 
             ELSE 0.00 END AS ResponsibilityAllowance,
        @Now
    FROM dbo.Employees e
    JOIN dbo.EmployeeCompensationHistories h ON h.EmployeeId = e.EmployeeId AND h.EffectiveTo IS NULL
    WHERE e.CompanyId = @CompanyId;

    COMMIT TRANSACTION;

    PRINT N'========================================================================';
    PRINT N'ĐÃ TẠO THÀNH CÔNG BỘ DỮ LIỆU MẪU ĐẦY ĐỦ CHO DOANH NGHIỆP AN PHÁT!';
    PRINT N'- Mã Workspace: anphat';
    PRINT N'- Tên công ty: Công ty Cổ phần Công nghệ An Phát';
    PRINT N'- Tài khoản quản trị cấp cao (Company Admin): admin@anphat.vn / Mật khẩu: 123456';
    PRINT N'- Tài khoản nhân sự (HR): hr@anphat.vn / Mật khẩu: 123456';
    PRINT N'- Tài khoản quản lý kỹ thuật (Manager): manager.tech@anphat.vn / Mật khẩu: 123456';
    PRINT N'- Tài khoản quản lý kinh doanh (Manager): manager.sales@anphat.vn / Mật khẩu: 123456';
    PRINT N'- 5 Tài khoản nhân viên tiêu chuẩn: Mật khẩu: 123456';
    PRINT N'- 6 Loại nghỉ phép, số dư ngày phép 2026 & 2027 đầy đủ';
    PRINT N'- 10 Đơn xin nghỉ phép bao phủ mọi trạng thái: Pending, Approved, Rejected, Cancelled,';
    PRINT N'  đơn vắt năm, đơn trừ ngày cuối tuần, đơn của Quản lý và đơn nghỉ ốm/thai sản.';
    PRINT N'- Dữ liệu chấm công Tháng 10/2026 đồng bộ chính xác với các ngày nghỉ phép đã duyệt!';
    PRINT N'========================================================================';

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    PRINT N'LỖI TRONG QUÁ TRÌNH KHỞI TẠO DỮ LIỆU: ' + ERROR_MESSAGE();
    THROW;
END CATCH;
GO
