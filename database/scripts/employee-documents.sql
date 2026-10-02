USE HRMsDB;
GO

IF OBJECT_ID('EmployeeDocuments', 'U') IS NULL
BEGIN
    CREATE TABLE EmployeeDocuments
    (
        DocumentId INT IDENTITY(1,1) PRIMARY KEY,
        EmployeeId INT NOT NULL,
        DocumentName NVARCHAR(255) NOT NULL,
        DocumentType NVARCHAR(100) NULL,
        FileName NVARCHAR(255) NULL,
        FilePath NVARCHAR(500) NULL,
        UploadedBy INT NULL,
        UploadedDate DATETIME NOT NULL DEFAULT GETDATE(),
        Status NVARCHAR(30) NOT NULL DEFAULT 'Active',
        CONSTRAINT FK_EmployeeDocuments_Employees
            FOREIGN KEY(EmployeeId) REFERENCES Employees(EmployeeId)
    );
END
GO
