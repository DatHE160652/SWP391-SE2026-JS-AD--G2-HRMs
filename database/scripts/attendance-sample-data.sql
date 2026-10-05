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
    VALUES ('technova', N'TechNova JSC', N'technova jsc', 'NORTH', 'contact@technova.vn');
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
