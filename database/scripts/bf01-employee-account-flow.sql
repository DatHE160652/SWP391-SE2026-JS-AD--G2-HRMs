USE HRMsDB;
GO

ALTER TABLE Employees ALTER COLUMN UserId INT NULL;
GO

-- If your Iteration 1 schema does not assign department/position yet,
-- these columns also need to allow NULL for basic Employee CRUD.
ALTER TABLE Employees ALTER COLUMN DepartmentId INT NULL;
ALTER TABLE Employees ALTER COLUMN PositionId INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM Companies)
BEGIN
    INSERT INTO Companies (WorkspaceCode, CompanyName, ContactEmail)
    VALUES ('demo', N'Demo Company', 'admin@hrms.local');
END
GO

IF NOT EXISTS (SELECT 1 FROM Roles WHERE RoleCode = 'EMPLOYEE')
BEGIN
    INSERT INTO Roles (RoleCode, RoleName, Description)
    VALUES ('EMPLOYEE', N'Nhân viên', N'Tài khoản nhân viên được tạo tự động');
END
GO
