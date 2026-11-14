IF OBJECT_ID('dbo.Positions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Positions (
        PositionId INT IDENTITY(1,1) PRIMARY KEY,
        CompanyId INT NOT NULL,
        PositionTitle NVARCHAR(150) NOT NULL,
        Description NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Positions_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId)
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'UX_Positions_Company_PositionTitle'
      AND object_id = OBJECT_ID('dbo.Positions')
)
BEGIN
    CREATE UNIQUE INDEX UX_Positions_Company_PositionTitle
    ON dbo.Positions (CompanyId, PositionTitle);
END;

-- CRUD queries used by UC08:
-- SELECT PositionId, CompanyId, PositionTitle, Description, CreatedAt FROM Positions WHERE CompanyId = ? ORDER BY PositionId DESC;
-- SELECT PositionId, CompanyId, PositionTitle, Description, CreatedAt FROM Positions WHERE PositionId = ?;
-- INSERT INTO Positions (CompanyId, PositionTitle, Description, CreatedAt) VALUES (?, ?, ?, SYSUTCDATETIME());
-- UPDATE Positions SET PositionTitle = ?, Description = ? WHERE PositionId = ?;
-- DELETE FROM Positions WHERE PositionId = ?;
