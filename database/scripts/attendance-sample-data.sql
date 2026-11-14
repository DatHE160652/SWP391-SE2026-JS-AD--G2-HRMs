USE HRMsDB;
GO

-- ========================================================================
-- DỮ LIỆU MẪU CHO PHÂN HỆ CHẤM CÔNG (ATTENDANCE MANAGEMENT)
-- Phục vụ kiểm thử UC29, UC30, UC31, UC32, UC33, UC36
-- ========================================================================

-- Đảm bảo bảng Attendances tồn tại theo thiết kế chuẩn
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Attendances')
BEGIN
    CREATE TABLE Attendances (
        AttendanceId BIGINT IDENTITY(1,1) PRIMARY KEY,
        CompanyId INT NOT NULL,
        EmployeeId INT NOT NULL,
        AttendanceDate DATE NOT NULL,
        CheckInTime TIME(0) NULL,
        CheckOutTime TIME(0) NULL,
        TotalHours DECIMAL(5,2) NOT NULL DEFAULT 0.00,
        Status NVARCHAR(30) NOT NULL DEFAULT 'Absent' 
            CHECK (Status IN ('Present', 'Late', 'Absent', 'On Leave', 'Early Leave')),
        Note NVARCHAR(255) NULL,
        IsManualAdjusted BIT NOT NULL DEFAULT 0,
        AdjustedByUserId INT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Attendances_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
        CONSTRAINT FK_Attendances_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId) ON DELETE CASCADE,
        CONSTRAINT FK_Attendances_AdjustedBy FOREIGN KEY (AdjustedByUserId) REFERENCES Users(UserId),
        CONSTRAINT UQ_Employee_AttendanceDate UNIQUE (EmployeeId, AttendanceDate)
    );
END
GO

-- Lấy CompanyId và danh sách EmployeeId hiện có để tạo bản ghi mẫu
DECLARE @CompanyId INT = (SELECT TOP 1 CompanyId FROM Companies);
IF @CompanyId IS NULL
BEGIN
    INSERT INTO Companies (WorkspaceCode, CompanyName, CompanyNameNormalized, Region, ContactEmail)
    VALUES ('anphat', N'Công ty Cổ phần Công Nghệ An Phát', N'công ty cổ phần công nghệ an phát', 'NORTH', 'contact@anphat.vn');
    SET @CompanyId = SCOPE_IDENTITY();
END

-- Đảm bảo có ít nhất 1 phòng ban và 2 nhân viên để test
DECLARE @DeptId INT = (SELECT TOP 1 DepartmentId FROM Departments WHERE CompanyId = @CompanyId);
IF @DeptId IS NULL
BEGIN
    INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName)
    VALUES (@CompanyId, 'TECH', N'Phòng Công Nghệ');
    SET @DeptId = SCOPE_IDENTITY();
END

DECLARE @Emp1 INT = (SELECT TOP 1 EmployeeId FROM Employees WHERE CompanyId = @CompanyId);
DECLARE @Emp2 INT = (SELECT TOP 1 EmployeeId FROM Employees WHERE CompanyId = @CompanyId AND EmployeeId <> @Emp1);

IF @Emp1 IS NOT NULL
BEGIN
    -- Mẫu 1: Có mặt đúng giờ hôm nay
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @Emp1 AND AttendanceDate = CAST(GETDATE() AS DATE))
    BEGIN
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note)
        VALUES (@CompanyId, @Emp1, CAST(GETDATE() AS DATE), '08:15:00', '17:35:00', 8.33, 'Present', N'Đúng giờ');
    END

    -- Mẫu 2: Đi muộn ngày hôm qua
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @Emp1 AND AttendanceDate = DATEADD(DAY, -1, CAST(GETDATE() AS DATE)))
    BEGIN
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note)
        VALUES (@CompanyId, @Emp1, DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), '08:45:00', '17:40:00', 7.92, 'Late', N'Đi muộn 15 phút do kẹt xe');
    END
END

IF @Emp2 IS NOT NULL
BEGIN
    -- Mẫu 3: Về sớm
    IF NOT EXISTS (SELECT 1 FROM Attendances WHERE EmployeeId = @Emp2 AND AttendanceDate = CAST(GETDATE() AS DATE))
    BEGIN
        INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note)
        VALUES (@CompanyId, @Emp2, CAST(GETDATE() AS DATE), '08:20:00', '16:00:00', 6.67, 'Early Leave', N'Xin về sớm việc gia đình');
    END
END
GO

USE HRMsDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
BEGIN TRANSACTION;

    DECLARE @CompanyId INT;
    DECLARE @RoleId INT;
    DECLARE @Now DATETIME2 = SYSUTCDATETIME();

    -- 1. Lấy đúng CompanyId từ tài khoản Bach
SELECT @CompanyId = CompanyId
FROM dbo.Users
WHERE Username = 'bach';

-- 2. Lấy RoleId của role MANAGER (RoleId = 4)
SELECT @RoleId = RoleId
FROM dbo.Roles
WHERE RoleCode = 'MANAGER';

IF @CompanyId IS NULL
        THROW 50001, 'Không tìm thấy tài khoản bach để lấy CompanyId.', 1;

    IF @RoleId IS NULL
        THROW 50002, 'Role MANAGER không tồn tại trong bảng Roles.', 1;

    -- 3. Thông tin tài khoản Manager mới (có thể đổi username/email/password tùy ý)
    DECLARE @Username VARCHAR(150) = 'manager_bach';
    DECLARE @Email VARCHAR(150) = 'manager_bach@gmail.com';
    DECLARE @DisplayName NVARCHAR(150) = N'Manager Bach';
    DECLARE @Password NVARCHAR(255) = N'123456';

    -- Kiểm tra trùng username hoặc email
    IF EXISTS (
        SELECT 1 FROM dbo.Users
        WHERE Username = @Username
           OR LOWER(LTRIM(RTRIM(Email))) = LOWER(LTRIM(RTRIM(@Email)))
    )
        THROW 50003, 'Username hoặc Email của Manager đã tồn tại trong hệ thống.', 1;

    -- 4. Thêm tài khoản mới (MustChangePassword = 0 để login dùng được ngay mà không bị chặn đổi pass)
INSERT INTO dbo.Users (
    CompanyId,
    RoleId,
    Username,
    Email,
    DisplayName,
    AvatarUrl,
    Password,
    MustChangePassword,
    ActivatedAt,
    PasswordChangedAt,
    IsActive,
    IsLocked,
    FailedLoginAttempts,
    LockedUntil,
    CreatedAt,
    UpdatedAt
)
VALUES (
           @CompanyId,
           @RoleId,
           @Username,
           @Email,
           @DisplayName,
           NULL,
           @Password,
           0,              -- MustChangePassword: 0
           @Now,           -- ActivatedAt (bắt buộc khi MustChangePassword = 0)
           @Now,           -- PasswordChangedAt
           1,              -- IsActive
           0,              -- IsLocked
           0,              -- FailedLoginAttempts
           NULL,
           @Now,
           NULL
       );

DECLARE @NewUserId INT = CONVERT(INT, SCOPE_IDENTITY());

COMMIT TRANSACTION;

-- Hiển thị thông tin vừa tạo
SELECT
    u.UserId,
    u.CompanyId,
    r.RoleCode,
    u.Username,
    u.Email,
    u.DisplayName,
    u.Password,
    u.IsActive
FROM dbo.Users u
         INNER JOIN dbo.Roles r ON r.RoleId = u.RoleId
WHERE u.UserId = @NewUserId;

END TRY
BEGIN CATCH
IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO


USE HRMsDB;
GO
SET NOCOUNT ON;
-- ========================================================================
-- SCRIPT TẠO 5 NHÂN VIÊN MẪU KÈM ĐẦY ĐỦ DỮ LIỆU
-- Bao gồm:
-- 1. Tài khoản User (Đăng nhập mật khẩu: 123)
-- 2. Hồ sơ Nhân viên (Employees) gắn đúng Phòng ban & Chức danh
-- 3. Quota nghỉ phép theo chính sách công ty năm 2026 (CompanyLeavePolicies)
-- 4. Đơn xin nghỉ phép mẫu đã duyệt (LeaveRequests)
-- 5. Kỳ lương Tháng 10/2026 & Phiếu lương (PayrollPeriods, Payslips)
-- 6. Dữ liệu Chấm công đa dạng (Attendances) từ ngày 20/10/2026 đến 24/10/2026
-- An toàn, có kiểm tra trùng lặp (Idempotent), chạy nhiều lần không lỗi.
-- ========================================================================
DECLARE @CompanyId INT;
SELECT TOP 1 @CompanyId = CompanyId FROM Companies WHERE WorkspaceCode = 'anphat';
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