package com.hrms.dao;

import com.hrms.model.EmployeeDocument;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {
    public List<EmployeeDocument> getDocumentsByEmployee(int employeeId) throws SQLException {
        ensureTableExists();
        String sql = """
                SELECT DocumentId, EmployeeId, DocumentName, DocumentType, FileName, FilePath,
                       UploadedBy, UploadedDate, Status
                FROM EmployeeDocuments
                WHERE EmployeeId = ? AND Status = 'Active'
                ORDER BY UploadedDate DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EmployeeDocument> documents = new ArrayList<>();
                while (resultSet.next()) {
                    documents.add(mapDocument(resultSet));
                }
                return documents;
            }
        }
    }

    public EmployeeDocument getDocumentById(int documentId) throws SQLException {
        ensureTableExists();
        String sql = """
                SELECT DocumentId, EmployeeId, DocumentName, DocumentType, FileName, FilePath,
                       UploadedBy, UploadedDate, Status
                FROM EmployeeDocuments
                WHERE DocumentId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapDocument(resultSet) : null;
            }
        }
    }

    public boolean insertDocument(EmployeeDocument document) throws SQLException {
        ensureTableExists();
        String sql = """
                INSERT INTO EmployeeDocuments (
                    EmployeeId, DocumentName, DocumentType, FileName, FilePath, UploadedBy
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, document.getEmployeeId());
            statement.setString(2, document.getDocumentName());
            statement.setString(3, document.getDocumentType());
            statement.setString(4, document.getFileName());
            statement.setString(5, document.getFilePath());
            if (document.getUploadedBy() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, document.getUploadedBy());
            }
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteDocument(int documentId) throws SQLException {
        ensureTableExists();
        String sql = "UPDATE EmployeeDocuments SET Status = 'Deleted' WHERE DocumentId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            return statement.executeUpdate() > 0;
        }
    }

    private EmployeeDocument mapDocument(ResultSet resultSet) throws SQLException {
        EmployeeDocument document = new EmployeeDocument();
        document.setDocumentId(resultSet.getInt("DocumentId"));
        document.setEmployeeId(resultSet.getInt("EmployeeId"));
        document.setDocumentName(resultSet.getString("DocumentName"));
        document.setDocumentType(resultSet.getString("DocumentType"));
        document.setFileName(resultSet.getString("FileName"));
        document.setFilePath(resultSet.getString("FilePath"));
        int uploadedBy = resultSet.getInt("UploadedBy");
        document.setUploadedBy(resultSet.wasNull() ? null : uploadedBy);
        document.setUploadedDate(resultSet.getTimestamp("UploadedDate"));
        document.setStatus(resultSet.getString("Status"));
        return document;
    }

    private void ensureTableExists() throws SQLException {
        String sql = """
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
                    )
                END
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
