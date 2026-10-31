package com.hrms.dao;

import com.hrms.model.Employee;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {
    private static final String PROFILE_CONTRACT = """
            OUTER APPLY (SELECT TOP(1) c.ContractType,c.ContractCode,
                CASE WHEN c.Status='ACTIVE' AND c.StartDate>CONVERT(date,SYSDATETIME()) THEN 'UPCOMING'
                     WHEN c.Status='ACTIVE' AND c.EndDate<CONVERT(date,SYSDATETIME()) THEN 'EXPIRED'
                     ELSE c.Status END AS ContractStatus
                FROM EmploymentContracts c WHERE c.CompanyId=e.CompanyId AND c.EmployeeId=e.EmployeeId AND c.Status<>'CANCELLED'
                ORDER BY CASE WHEN c.Status='ACTIVE' AND c.StartDate<=CONVERT(date,SYSDATETIME())
                    AND (c.EndDate IS NULL OR c.EndDate>=CONVERT(date,SYSDATETIME())) THEN 0 ELSE 1 END,
                    c.StartDate DESC,c.ContractId DESC) profileContract
            """;
    public List<Employee> getAllEmployees() throws SQLException {
        return getAllEmployees(null);
    }

    public List<Employee> getAllEmployees(Integer companyId) throws SQLException {
        String sql = """
                SELECT e.*, CONVERT(varchar(33),COALESCE(e.UpdatedAt,e.CreatedAt),126) AS ProfileVersion,
                       d.DepartmentName, p.PositionTitle, manager.FullName AS ManagerName,
                       profileContract.ContractType,profileContract.ContractCode,profileContract.ContractStatus,
                       u.UserId, u.Username, u.Email AS AccountEmail,
                """ + "CASE WHEN " + ContractAccessPolicy.ALLOWED + """
                    THEN u.IsActive ELSE CAST(0 AS bit) END AS AccountActive
                FROM Employees e
                LEFT JOIN Users u ON e.CompanyId=u.CompanyId AND e.UserId = u.UserId
                LEFT JOIN Departments d ON d.CompanyId=e.CompanyId AND d.DepartmentId=e.DepartmentId
                LEFT JOIN Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=e.PositionId
                LEFT JOIN Employees manager ON manager.CompanyId=e.CompanyId AND manager.EmployeeId=e.ManagerId
                """ + PROFILE_CONTRACT + """
                WHERE (? IS NULL OR e.CompanyId=?)
                ORDER BY e.EmployeeId DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, companyId, Types.INTEGER);
            statement.setObject(2, companyId, Types.INTEGER);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Employee> employees = new ArrayList<>();
                while (resultSet.next()) {
                    employees.add(mapEmployee(resultSet));
                }
                return employees;
            }
        }
    }

    public Employee getEmployeeById(int employeeId) throws SQLException {
        return getEmployeeById(null, employeeId);
    }

    public Employee getEmployeeById(Integer companyId, int employeeId) throws SQLException {
        String sql = """
                SELECT e.*, CONVERT(varchar(33),COALESCE(e.UpdatedAt,e.CreatedAt),126) AS ProfileVersion,
                       d.DepartmentName, p.PositionTitle, manager.FullName AS ManagerName,
                       profileContract.ContractType,profileContract.ContractCode,profileContract.ContractStatus,
                       u.UserId, u.Username, u.Email AS AccountEmail,
                """ + "CASE WHEN " + ContractAccessPolicy.ALLOWED + """
                    THEN u.IsActive ELSE CAST(0 AS bit) END AS AccountActive
                FROM Employees e
                LEFT JOIN Users u ON e.CompanyId=u.CompanyId AND e.UserId = u.UserId
                LEFT JOIN Departments d ON d.CompanyId=e.CompanyId AND d.DepartmentId=e.DepartmentId
                LEFT JOIN Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=e.PositionId
                LEFT JOIN Employees manager ON manager.CompanyId=e.CompanyId AND manager.EmployeeId=e.ManagerId
                """ + PROFILE_CONTRACT + """
                WHERE e.EmployeeId = ? AND (? IS NULL OR e.CompanyId=?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setObject(2, companyId, Types.INTEGER);
            statement.setObject(3, companyId, Types.INTEGER);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapEmployee(resultSet) : null;
            }
        }
    }

    public int insertEmployee(Connection connection, Employee employee, int companyId) throws SQLException {
        String sql = """
                INSERT INTO Employees (
                    CompanyId, EmployeeCode, FullName, Phone, Gender,
                    DateOfBirth, IdentityNumber, Status, Email, IdentityIssuedDate, IdentityIssuedPlace,
                    PermanentAddress, Hometown, Major, IntendedContractType
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, companyId);
            statement.setString(2, employee.getEmployeeCode());
            statement.setString(3, employee.getFullName());
            statement.setString(4, employee.getPhone());
            statement.setString(5, employee.getGender());
            setNullableDate(statement, 6, employee.getDateOfBirth());
            statement.setString(7, employee.getIdentityNumber());
            statement.setString(8, employee.getStatus());
            bindPersonal(statement, 9, employee);
            statement.setString(15,employee.getIntendedContractType());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Không tạo được hồ sơ nhân viên.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new SQLException("Không tạo được nhân viên: chưa nhận được mã hồ sơ.");
        }
    }

    public boolean updateEmployeeUserId(Connection connection, int employeeId, int userId) throws SQLException {
        String sql = "UPDATE Employees SET UserId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return updateEmployee(connection, employee);
        }
    }

    public boolean updateEmployee(Connection connection, Employee employee) throws SQLException {
        requireNoDirectReportsWhenInactive(connection, employee.getCompanyId(), employee.getEmployeeId(), "Active".equals(employee.getStatus()));
        String sql = """
                UPDATE Employees
                SET FullName = ?,
                    Phone = ?,
                    Gender = ?,
                    DateOfBirth = ?,
                    IdentityNumber = ?,
                    Status = ?, Email=?, IdentityIssuedDate=?, IdentityIssuedPlace=?,
                    PermanentAddress=?, Hometown=?, Major=?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE EmployeeId = ? AND CompanyId = ?
                    AND CONVERT(varchar(33),COALESCE(UpdatedAt,CreatedAt),126)=?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getPhone());
            statement.setString(3, employee.getGender());
            setNullableDate(statement, 4, employee.getDateOfBirth());
            statement.setString(5, employee.getIdentityNumber());
            statement.setString(6, employee.getStatus());
            bindPersonal(statement, 7, employee);
            statement.setInt(13, employee.getEmployeeId());
            statement.setInt(14, employee.getCompanyId());
            statement.setString(15, employee.getUpdatedAt());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean phoneExists(String phone, Integer excludeEmployeeId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Employees
                WHERE Phone = ?
                  AND (? IS NULL OR EmployeeId <> ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            if (excludeEmployeeId == null) {
                statement.setNull(2, Types.INTEGER);
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(2, excludeEmployeeId);
                statement.setInt(3, excludeEmployeeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void requireNoDirectReportsWhenInactive(Connection connection, int companyId, int employeeId, boolean active) throws SQLException {
        if (active) return;
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT 1 FROM Employees WHERE CompanyId=? AND ManagerId=?")) {
            check.setInt(1, companyId);
            check.setInt(2, employeeId);
            try (ResultSet rows = check.executeQuery()) {
                if (rows.next()) throw new com.hrms.service.FieldValidationException("status",
                        "Vui lòng phân công lại cấp dưới trước khi ngừng hoạt động nhân viên này.");
            }
        }
    }

    private Employee mapEmployee(ResultSet resultSet) throws SQLException {
        Employee employee = new Employee();
        employee.setManagerId((Integer) resultSet.getObject("ManagerId"));
        employee.setEmail(resultSet.getString("Email"));
        employee.setIdentityIssuedDate(resultSet.getDate("IdentityIssuedDate"));
        employee.setIdentityIssuedPlace(resultSet.getString("IdentityIssuedPlace"));
        employee.setPermanentAddress(resultSet.getString("PermanentAddress"));
        employee.setHometown(resultSet.getString("Hometown"));
        employee.setMajor(resultSet.getString("Major"));
        employee.setIntendedContractType(resultSet.getString("IntendedContractType"));
        employee.setContractType(resultSet.getString("ContractType"));
        employee.setContractCode(resultSet.getString("ContractCode"));
        employee.setContractStatus(resultSet.getString("ContractStatus"));
        employee.setDepartmentName(resultSet.getString("DepartmentName"));
        employee.setPositionTitle(resultSet.getString("PositionTitle"));
        employee.setManagerName(resultSet.getString("ManagerName"));
        employee.setUpdatedAt(resultSet.getString("ProfileVersion"));
        employee.setCompanyId(resultSet.getInt("CompanyId"));
        employee.setDepartmentId((Integer) resultSet.getObject("DepartmentId"));
        employee.setPositionId((Integer) resultSet.getObject("PositionId"));
        employee.setEmployeeId(resultSet.getInt("EmployeeId"));
        employee.setEmployeeCode(resultSet.getString("EmployeeCode"));
        employee.setFullName(resultSet.getString("FullName"));
        employee.setPhone(resultSet.getString("Phone"));
        employee.setGender(resultSet.getString("Gender"));
        employee.setDateOfBirth(resultSet.getDate("DateOfBirth"));
        employee.setIdentityNumber(resultSet.getString("IdentityNumber"));
        employee.setStatus(resultSet.getString("Status"));
        int userId = resultSet.getInt("UserId");
        employee.setUserId(resultSet.wasNull() ? null : userId);
        employee.setUsername(resultSet.getString("Username"));
        employee.setAccountEmail(resultSet.getString("AccountEmail"));
        boolean accountActive = resultSet.getBoolean("AccountActive");
        employee.setAccountActive(resultSet.wasNull() ? null : accountActive);
        return employee;
    }

    private void setNullableDate(PreparedStatement statement, int index, Date value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, value);
        }
    }

    private void bindPersonal(PreparedStatement statement, int start, Employee employee) throws SQLException {
        statement.setString(start, employee.getEmail());
        setNullableDate(statement, start+1, employee.getIdentityIssuedDate());
        statement.setString(start+2, employee.getIdentityIssuedPlace());
        statement.setString(start+3, employee.getPermanentAddress());
        statement.setString(start+4, employee.getHometown());
        statement.setString(start+5, employee.getMajor());
    }

}
