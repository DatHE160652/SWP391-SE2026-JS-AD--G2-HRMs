USE HRMsDB;
GO

SET NOCOUNT ON;

-- ========================================================================
-- SCRIPT TẠO 5 NHÂN VIÊN MẪU KÈM ĐẦY ĐỦ DỮ LIỆU
-- Bao gồm:
-- 1. Tài khoản User (Đăng nhập mật khẩu: 123)
-- 2. Hồ sơ Nhân viên (Employees) gắn đúng Phòng ban & Chức danh
-- 3. Hạn mức Ngày nghỉ phép 2026 (LeaveBalances)
-- 4. Đơn xin nghỉ phép mẫu đã duyệt (LeaveRequests)
-- 5. Kỳ lương Tháng 10/2026 & Phiếu lương (PayrollPeriods, Payslips)
-- 6. Dữ liệu Chấm công đa dạng (Attendances) từ ngày 20/10/2026 đến 24/10/2026
-- An toàn, có kiểm tra trùng lặp (Idempotent), chạy nhiều lần không lỗi.
-- ========================================================================

DECLARE @CompanyId INT;
SELECT TOP 1 @CompanyId = CompanyId FROM Companies WHERE WorkspaceCode = 'technova';
IF @CompanyId IS NULL SELECT TOP 1 @CompanyId = CompanyId FROM Companies;

IF @CompanyId IS NULL
BEGIN
    RAISERROR(N'Không tìm thấy Công ty nào trong bảng Companies. Vui lòng kiểm tra lại!', 16, 1);
    RETURN;
END;

DECLARE @RoleEmployeeId INT = (SELECT TOP 1 RoleId FROM Roles WHERE RoleCode = 'EMPLOYEE');
IF @RoleEmployeeId IS NULL SET @RoleEmployeeId = 5;

-- ------------------------------------------------------------------------
-- 1. ĐẢM BẢO DANH MỤC PHÒNG BAN (DEPARTMENTS)
-- ------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'TECH')
    INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName) VALUES (@CompanyId, 'TECH', N'Phòng Công Nghệ');
IF NOT EXISTS (SELECT 1 FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'HR')
    INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName) VALUES (@CompanyId, 'HR', N'Phòng Nhân Sự');
IF NOT EXISTS (SELECT 1 FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'FIN')
    INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName) VALUES (@CompanyId, 'FIN', N'Phòng Tài Chính Kế Toán');
IF NOT EXISTS (SELECT 1 FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'MKT')
    INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName) VALUES (@CompanyId, 'MKT', N'Phòng Marketing');

DECLARE @DeptTech INT = (SELECT TOP 1 DepartmentId FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'TECH');
DECLARE @DeptHR INT = (SELECT TOP 1 DepartmentId FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'HR');
DECLARE @DeptFin INT = (SELECT TOP 1 DepartmentId FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'FIN');
DECLARE @DeptMkt INT = (SELECT TOP 1 DepartmentId FROM Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'MKT');

-- ------------------------------------------------------------------------
-- 2. ĐẢM BẢO CHỨC DANH CÔNG VIỆC (POSITIONS)
-- Lưu ý: Positions cần có DepartmentId tương ứng để thỏa mãn FK_Employees_Company_Department_Position
-- ------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Senior Developer')
    INSERT INTO Positions (CompanyId, DepartmentId, PositionTitle, Description) VALUES (@CompanyId, @DeptTech, N'Senior Developer', N'Lập trình viên cấp cao');
ELSE
    UPDATE Positions SET DepartmentId = @DeptTech WHERE CompanyId = @CompanyId AND PositionTitle = N'Senior Developer';

IF NOT EXISTS (SELECT 1 FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'QA Engineer')
    INSERT INTO Positions (CompanyId, DepartmentId, PositionTitle, Description) VALUES (@CompanyId, @DeptTech, N'QA Engineer', N'Kiểm thử chất lượng');
ELSE
    UPDATE Positions SET DepartmentId = @DeptTech WHERE CompanyId = @CompanyId AND PositionTitle = N'QA Engineer';

IF NOT EXISTS (SELECT 1 FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Chuyên viên Nhân sự')
    INSERT INTO Positions (CompanyId, DepartmentId, PositionTitle, Description) VALUES (@CompanyId, @DeptHR, N'Chuyên viên Nhân sự', N'Tuyển dụng và chế độ');
ELSE
    UPDATE Positions SET DepartmentId = @DeptHR WHERE CompanyId = @CompanyId AND PositionTitle = N'Chuyên viên Nhân sự';

IF NOT EXISTS (SELECT 1 FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Kế toán viên')
    INSERT INTO Positions (CompanyId, DepartmentId, PositionTitle, Description) VALUES (@CompanyId, @DeptFin, N'Kế toán viên', N'Theo dõi thu chi và bảng lương');
ELSE
    UPDATE Positions SET DepartmentId = @DeptFin WHERE CompanyId = @CompanyId AND PositionTitle = N'Kế toán viên';

IF NOT EXISTS (SELECT 1 FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Chuyên viên Marketing')
    INSERT INTO Positions (CompanyId, DepartmentId, PositionTitle, Description) VALUES (@CompanyId, @DeptMkt, N'Chuyên viên Marketing', N'Phát triển thương hiệu');
ELSE
    UPDATE Positions SET DepartmentId = @DeptMkt WHERE CompanyId = @CompanyId AND PositionTitle = N'Chuyên viên Marketing';

DECLARE @PosDev INT = (SELECT TOP 1 PositionId FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Senior Developer');
DECLARE @PosQA INT = (SELECT TOP 1 PositionId FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'QA Engineer');
DECLARE @PosHR INT = (SELECT TOP 1 PositionId FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Chuyên viên Nhân sự');
DECLARE @PosAcc INT = (SELECT TOP 1 PositionId FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Kế toán viên');
DECLARE @PosMkt INT = (SELECT TOP 1 PositionId FROM Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Chuyên viên Marketing');

-- ------------------------------------------------------------------------
-- 3. ĐẢM BẢO CHÍNH SÁCH NGHỈ PHÉP (LEAVETYPES)
-- ------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'ANNUAL')
    INSERT INTO LeaveTypes (CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid) VALUES (@CompanyId, 'ANNUAL', N'Nghỉ phép năm', 12, 1);
IF NOT EXISTS (SELECT 1 FROM LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'SICK')
    INSERT INTO LeaveTypes (CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid) VALUES (@CompanyId, 'SICK', N'Nghỉ ốm', 10, 1);
IF NOT EXISTS (SELECT 1 FROM LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'UNPAID')
    INSERT INTO LeaveTypes (CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid) VALUES (@CompanyId, 'UNPAID', N'Nghỉ không lương', 0, 0);

DECLARE @LeaveAnnual INT = (SELECT TOP 1 LeaveTypeId FROM LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'ANNUAL');
DECLARE @LeaveSick INT = (SELECT TOP 1 LeaveTypeId FROM LeaveTypes WHERE CompanyId = @CompanyId AND TypeCode = 'SICK');

-- ------------------------------------------------------------------------
-- 4. ĐẢM BẢO KỲ LƯƠNG THÁNG 10/2026 (PAYROLLPERIODS)
-- ------------------------------------------------------------------------
DECLARE @PayrollPeriodId INT = (SELECT TOP 1 PayrollPeriodId FROM PayrollPeriods WHERE CompanyId = @CompanyId AND Month = 10 AND Year = 2026);
IF @PayrollPeriodId IS NULL
BEGIN
    INSERT INTO PayrollPeriods (CompanyId, PeriodName, Month, Year, Status)
    VALUES (@CompanyId, N'Bảng lương Tháng 10/2026', 10, 2026, 'Approved');
    SET @PayrollPeriodId = SCOPE_IDENTITY();
END;

-- ------------------------------------------------------------------------
-- 5. TẠO 5 NHÂN VIÊN VÀ ĐẦY ĐỦ THÔNG TIN LIÊN QUAN
-- ------------------------------------------------------------------------

DECLARE @SeedEmployees TABLE (
    EmpIndex INT IDENTITY(1,1),
    EmpCode VARCHAR(50),
    FullName NVARCHAR(150),
    Email VARCHAR(150),
    Username VARCHAR(50),
    Phone VARCHAR(30),
    Gender NVARCHAR(20),
    Dob DATE,
    StartDate DATE,
    BaseSalary DECIMAL(18,2),
    DeptId INT,
    PosId INT
);

INSERT INTO @SeedEmployees (EmpCode, FullName, Email, Username, Phone, Gender, Dob, StartDate, BaseSalary, DeptId, PosId)
VALUES 
('TEC100001', N'Nguyễn Văn An', 'an.nguyen@tec.hrms', 'an.nguyen', '0912345601', N'Nam', '1995-03-15', '2023-01-10', 25000000.00, @DeptTech, @PosDev),
('TEC100002', N'Trần Thị Bích', 'bich.tran@tec.hrms', 'bich.tran', '0912345602', N'Nữ', '1997-07-22', '2023-03-15', 18000000.00, @DeptTech, @PosQA),
('TEC100003', N'Lê Hoàng Cường', 'cuong.le@tec.hrms', 'cuong.le', '0912345603', N'Nam', '1994-11-05', '2022-08-01', 15000000.00, @DeptHR, @PosHR),
('TEC100004', N'Phạm Minh Dũng', 'dung.pham@tec.hrms', 'dung.pham', '0912345604', N'Nam', '1996-09-18', '2023-05-20', 16500000.00, @DeptFin, @PosAcc),
('TEC100005', N'Vũ Thuỳ Linh', 'linh.vu@tec.hrms', 'linh.vu', '0912345605', N'Nữ', '1998-12-30', '2024-02-01', 14000000.00, @DeptMkt, @PosMkt);

DECLARE @i INT = 1;
DECLARE @TotalEmp INT = (SELECT COUNT(*) FROM @SeedEmployees);

WHILE @i <= @TotalEmp
BEGIN
    DECLARE @CurCode VARCHAR(50), @CurName NVARCHAR(150), @CurEmail VARCHAR(150), @CurUsername VARCHAR(50);
    DECLARE @CurPhone VARCHAR(30), @CurGender NVARCHAR(20), @CurDob DATE, @CurStartDate DATE;
    DECLARE @CurSalary DECIMAL(18,2), @CurDept INT, @CurPos INT;
    DECLARE @CurUserId INT = NULL, @CurEmpId INT = NULL;

    SELECT @CurCode = EmpCode, @CurName = FullName, @CurEmail = Email, @CurUsername = Username,
           @CurPhone = Phone, @CurGender = Gender, @CurDob = Dob, @CurStartDate = StartDate,
           @CurSalary = BaseSalary, @CurDept = DeptId, @CurPos = PosId
    FROM @SeedEmployees WHERE EmpIndex = @i;

    -- A. Tạo User đăng nhập (mật khẩu mặc định: 123)
    SELECT @CurUserId = UserId FROM Users WHERE Email = @CurEmail OR Username = @CurUsername;
    IF @CurUserId IS NULL
    BEGIN
        INSERT INTO Users (CompanyId, RoleId, Username, Email, DisplayName, Password, MustChangePassword, ActivatedAt, PasswordChangedAt, IsActive, CreatedAt)
        VALUES (@CompanyId, @RoleEmployeeId, @CurUsername, @CurEmail, @CurName, '123', 0, SYSUTCDATETIME(), SYSUTCDATETIME(), 1, SYSUTCDATETIME());
        SET @CurUserId = SCOPE_IDENTITY();
    END;

    -- B. Tạo hồ sơ Employee
    SELECT @CurEmpId = EmployeeId FROM Employees WHERE CompanyId = @CompanyId AND EmployeeCode = @CurCode;
    IF @CurEmpId IS NULL
    BEGIN
        INSERT INTO Employees (CompanyId, UserId, DepartmentId, PositionId, EmployeeCode, FullName, Phone, Gender, DateOfBirth, StartDate, BaseSalary, Status, CreatedAt)
        VALUES (@CompanyId, @CurUserId, @CurDept, @CurPos, @CurCode, @CurName, @CurPhone, @CurGender, @CurDob, @CurStartDate, @CurSalary, 'Active', SYSUTCDATETIME());
        SET @CurEmpId = SCOPE_IDENTITY();
    END
    ELSE
    BEGIN
        UPDATE Employees
        SET DepartmentId = @CurDept, PositionId = @CurPos, FullName = @CurName,
            BaseSalary = @CurSalary, Status = 'Active'
        WHERE EmployeeId = @CurEmpId;
    END;

    -- C. Tạo Hạn mức Ngày nghỉ (LeaveBalances - Năm 2026)
    -- Phép năm (12 ngày, đã dùng 1 ngày -> Còn lại 11 ngày)
    IF NOT EXISTS (SELECT 1 FROM LeaveBalances WHERE EmployeeId = @CurEmpId AND LeaveTypeId = @LeaveAnnual AND Year = 2026)
    BEGIN
        INSERT INTO LeaveBalances (EmployeeId, LeaveTypeId, Year, EntitledDays, UsedDays, CreatedAt)
        VALUES (@CurEmpId, @LeaveAnnual, 2026, 12.00, 1.00, SYSUTCDATETIME());
    END;

    -- Nghỉ ốm (10 ngày, chưa dùng -> Còn lại 10 ngày)
    IF NOT EXISTS (SELECT 1 FROM LeaveBalances WHERE EmployeeId = @CurEmpId AND LeaveTypeId = @LeaveSick AND Year = 2026)
    BEGIN
        INSERT INTO LeaveBalances (EmployeeId, LeaveTypeId, Year, EntitledDays, UsedDays, CreatedAt)
        VALUES (@CurEmpId, @LeaveSick, 2026, 10.00, 0.00, SYSUTCDATETIME());
    END;

    -- D. Tạo Đơn xin nghỉ phép mẫu (LeaveRequests - Đã duyệt)
    IF NOT EXISTS (SELECT 1 FROM LeaveRequests WHERE EmployeeId = @CurEmpId AND StartDate = '2026-10-16')
    BEGIN
        INSERT INTO LeaveRequests (CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays, Reason, Status, ReviewedAt, CreatedAt)
        VALUES (@CompanyId, @CurEmpId, @LeaveAnnual, '2026-10-16', '2026-10-16', 1.0, N'Nghỉ giải quyết việc gia đình', 'Approved', SYSUTCDATETIME(), SYSUTCDATETIME());
    END;

    -- E. Tạo Phiếu lương Tháng 10/2026 (Payslips)
    IF NOT EXISTS (SELECT 1 FROM Payslips WHERE PayrollPeriodId = @PayrollPeriodId AND EmployeeId = @CurEmpId)
    BEGIN
        DECLARE @Bonus DECIMAL(18,2) = CASE WHEN @i = 1 THEN 2000000.00 WHEN @i = 5 THEN 1500000.00 ELSE 500000.00 END;
        DECLARE @Deduction DECIMAL(18,2) = 0.00;
        INSERT INTO Payslips (PayrollPeriodId, EmployeeId, BaseSalary, Bonus, Deduction, GeneratedAt)
        VALUES (@PayrollPeriodId, @CurEmpId, @CurSalary, @Bonus, @Deduction, SYSUTCDATETIME());
    END;

    -- F. Tạo Nhật ký Chấm công mẫu Tháng 10/2026 (Attendances)
    -- Ngày 20/10/2026: Đúng giờ (Present)
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @CurEmpId AND AttendanceDate = '2026-10-20')
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, CreatedAt)
        VALUES (@CompanyId, @CurEmpId, '2026-10-20', '08:15:00', '17:35:00', 8.00, 'Present', SYSUTCDATETIME());

    -- Ngày 21/10/2026: Đi muộn (Late)
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @CurEmpId AND AttendanceDate = '2026-10-21')
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, CreatedAt)
        VALUES (@CompanyId, @CurEmpId, '2026-10-21', '08:42:00', '17:32:00', 7.80, 'Late', SYSUTCDATETIME());

    -- Ngày 22/10/2026: Đúng giờ (Present)
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @CurEmpId AND AttendanceDate = '2026-10-22')
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, CreatedAt)
        VALUES (@CompanyId, @CurEmpId, '2026-10-22', '08:20:00', '17:30:00', 8.00, 'Present', SYSUTCDATETIME());

    -- Ngày 23/10/2026: Về sớm (Early Leave)
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @CurEmpId AND AttendanceDate = '2026-10-23')
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, CreatedAt)
        VALUES (@CompanyId, @CurEmpId, '2026-10-23', '08:25:00', '16:30:00', 7.08, 'Early Leave', SYSUTCDATETIME());

    -- Ngày 24/10/2026 (Hôm nay): Đã điểm danh Check-in & Check-out
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @CurEmpId AND AttendanceDate = '2026-10-24')
    BEGIN
        IF @i % 2 = 1
            INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, CreatedAt)
            VALUES (@CompanyId, @CurEmpId, '2026-10-24', '08:18:10', '17:31:05', 8.00, 'Present', SYSUTCDATETIME());
        ELSE
            INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, CreatedAt)
            VALUES (@CompanyId, @CurEmpId, '2026-10-24', '08:22:30', '16:45:00', 7.38, 'Early Leave', SYSUTCDATETIME());
    END;

    SET @i = @i + 1;
END;

PRINT N'========================================================================';
PRINT N'ĐÃ TẠO THÀNH CÔNG 5 NHÂN VIÊN VỚI ĐẦY ĐỦ DỮ LIỆU:';
PRINT N'- Tài khoản User & mật khẩu: 123';
PRINT N'- Hồ sơ nhân viên, chức danh, phòng ban & lương cơ bản';
PRINT N'- Hạn mức ngày nghỉ phép năm 2026 (LeaveBalances) & Đơn nghỉ phép (LeaveRequests)';
PRINT N'- Bảng lương Tháng 10/2026 (PayrollPeriods & Payslips)';
PRINT N'- Dữ liệu chấm công Tháng 10/2026 (Attendances) đa dạng trạng thái!';
PRINT N'========================================================================';
GO
