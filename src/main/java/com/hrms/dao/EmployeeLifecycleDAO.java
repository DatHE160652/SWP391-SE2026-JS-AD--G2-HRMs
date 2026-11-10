package com.hrms.dao;

import com.hrms.model.Employee;
import com.hrms.model.EmployeeCompensation;
import com.hrms.service.FieldValidationException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeLifecycleDAO {
    public List<Map<String,Object>> currentAllowance(Connection connection,int companyId,Integer employeeId) throws SQLException {
        if (employeeId == null) throw new IllegalArgumentException("Vui lòng chọn nhân viên để xem phụ cấp.");
        var allowance = query(connection, """
                SELECT r.LevelName,policy.Amount,policy.PolicyCount,policy.PolicyFrom
                FROM EmployeePositionAssignments assignment
                JOIN Positions p ON p.CompanyId=assignment.CompanyId AND p.PositionId=assignment.PositionId
                JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                OUTER APPLY (SELECT MAX(Amount) AS Amount,COUNT(*) AS PolicyCount,MAX(EffectiveFrom) AS PolicyFrom
                    FROM ResponsibilityAllowancePolicies
                    WHERE CompanyId=p.CompanyId AND ResponsibilityLevelId=p.ResponsibilityLevelId
                        AND EffectiveFrom<=CONVERT(date,SYSDATETIME())
                        AND (EffectiveTo IS NULL OR EffectiveTo>CONVERT(date,SYSDATETIME()))) policy
                WHERE assignment.CompanyId=? AND assignment.EmployeeId=?
                    AND assignment.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                    AND (assignment.EffectiveTo IS NULL OR assignment.EffectiveTo>CONVERT(date,SYSDATETIME()))
                """, companyId, employeeId);
        if (allowance.size()>1) throw new IllegalArgumentException("Có nhiều phân công cùng hiệu lực. Cần kiểm tra lịch sử phân công.");
        if (!allowance.isEmpty() && ((Number)allowance.get(0).get("PolicyCount")).intValue()>1)
            throw new IllegalArgumentException("Có nhiều chính sách phụ cấp cùng hiệu lực. Cần kiểm tra cấu hình cấp trách nhiệm.");
        return allowance;
    }

    public List<Map<String,Object>> compensationHistory(Connection connection,int companyId,Integer employeeId) throws SQLException {
        return query(connection, """
            SELECT h.CompensationHistoryId,h.BaseSalary,h.PositionAllowance,h.SourceContractId,
                c.ContractCode,
                h.TravelAllowance,h.EffectiveFrom,
                CASE
                    WHEN c.Status='TERMINATED' AND c.TerminatedDate IS NOT NULL AND (h.EffectiveTo IS NULL OR h.EffectiveTo>c.TerminatedDate)
                        THEN c.TerminatedDate
                    WHEN c.EndDate IS NOT NULL AND (h.EffectiveTo IS NULL OR h.EffectiveTo>c.EndDate)
                        THEN c.EndDate
                    WHEN e.Status<>'Active' AND e.EndDate IS NOT NULL AND (h.EffectiveTo IS NULL OR h.EffectiveTo>e.EndDate)
                        THEN e.EndDate
                    ELSE h.EffectiveTo
                END AS EffectiveTo,
                h.Reason,
                CASE WHEN h.EffectiveTo=h.EffectiveFrom THEN 1 ELSE 0 END AS IsSuperseded,
                CASE
                    WHEN e.Status='Active'
                        AND h.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                        AND (h.EffectiveTo IS NULL OR h.EffectiveTo>CONVERT(date,SYSDATETIME()))
                        AND c.Status='ACTIVE'
                        AND c.StartDate<=CONVERT(date,SYSDATETIME())
                        AND (c.EndDate IS NULL OR c.EndDate>=CONVERT(date,SYSDATETIME()))
                    THEN 1 ELSE 0
                END AS IsCurrent
            FROM EmployeeCompensationHistories h
            JOIN Employees e ON e.CompanyId=h.CompanyId AND e.EmployeeId=h.EmployeeId
            LEFT JOIN EmploymentContracts c ON c.CompanyId=h.CompanyId AND c.EmployeeId=h.EmployeeId AND c.ContractId=h.SourceContractId
            WHERE h.CompanyId=? AND h.EmployeeId=?
            ORDER BY h.EffectiveFrom DESC,h.CompensationHistoryId DESC
            """, companyId, employeeId);
    }

    public void requireActor(Connection connection, int companyId, Integer actorId) throws SQLException {
        if (actorId == null || query(connection,"""
                SELECT 1 FROM Users u JOIN Roles r ON r.RoleId=u.RoleId WHERE u.CompanyId=? AND u.UserId=?
                    AND u.IsActive=1 AND u.IsLocked=0 AND r.RoleCode IN ('HR','COMPANY_ADMIN')
                """,companyId,actorId).isEmpty()) throw new SecurityException("Cần tài khoản nhân sự hoặc quản trị viên công ty đang hoạt động.");
    }
    public List<Map<String, Object>> query(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rows = statement.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();
                while (rows.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= rows.getMetaData().getColumnCount(); index++)
                        row.put(rows.getMetaData().getColumnLabel(index), rows.getObject(index));
                    result.add(row);
                }
                return result;
            }
        }
    }

    public int execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    public long insert(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, parameters);
            if (statement.executeUpdate() != 1) throw new SQLException("Không lưu được bản ghi.");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Chưa nhận được mã bản ghi.");
                return keys.getLong(1);
            }
        }
    }

    public Map<String, Object> employee(Connection connection, int companyId, int employeeId) throws SQLException {
        var rows = query(connection, "SELECT * FROM Employees WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=?", companyId, employeeId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty của bạn.");
        return rows.get(0);
    }

    public void validateEmail(Connection connection, Employee employee) throws SQLException {
        if (employee.getEmail() != null && !query(connection,
                "SELECT 1 FROM Employees WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND Email=? AND EmployeeId<>?",
                employee.getCompanyId(), employee.getEmail(), employee.getEmployeeId()).isEmpty())
            throw new FieldValidationException("email", "Email đã được sử dụng bởi nhân viên khác trong công ty.");
    }

    public Integer resolveInitialManager(Connection connection, Employee employee) throws SQLException {
        if (employee.getManagerId() != null) return employee.getManagerId();
        if (employee.getPositionId() == null) return null;
        var managers = query(connection, """
                SELECT e.EmployeeId FROM Employees e
                JOIN Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=e.PositionId
                JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                JOIN Positions target ON target.CompanyId=e.CompanyId AND target.PositionId=?
                JOIN ResponsibilityLevels targetLevel ON targetLevel.CompanyId=target.CompanyId AND targetLevel.ResponsibilityLevelId=target.ResponsibilityLevelId
                WHERE e.CompanyId=? AND e.DepartmentId=? AND e.EmployeeId<>? AND e.Status='Active'
                    AND p.CanManageEmployees=1 AND p.IsActive=1 AND r.RankOrder>targetLevel.RankOrder
                    AND UPPER(LTRIM(RTRIM(r.LevelCode))) NOT IN ('EMP','EMPLOYEE')
                    AND UPPER(LTRIM(RTRIM(r.LevelName)))<>'EMPLOYEE'
                    AND (target.ReportsToPositionId IS NULL OR p.PositionId=target.ReportsToPositionId)
                    AND EXISTS(SELECT 1 FROM EmployeePositionAssignments a WHERE a.CompanyId=e.CompanyId AND a.EmployeeId=e.EmployeeId
                        AND a.PositionId=e.PositionId AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                        AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME())))
                """, employee.getPositionId(), employee.getCompanyId(), employee.getDepartmentId(), employee.getEmployeeId());
        if (managers.size() > 1) throw new FieldValidationException("managerId", "Có nhiều quản lý phù hợp. Vui lòng chọn quản lý trực tiếp.");
        return managers.isEmpty() ? null : ((Number) managers.get(0).get("EmployeeId")).intValue();
    }

    public static void validateCompensation(EmployeeCompensation compensation) {
        if (compensation == null || !compensation.provided()) return;
        if (compensation.grossSalary() == null) throw new FieldValidationException("grossSalary", "Vui lòng nhập lương trước khấu trừ khi cung cấp thông tin lương.");
        BigDecimal[] amounts = {compensation.grossSalary(), compensation.positionAllowance(), compensation.travelAllowance()};
        String[] fields = {"grossSalary", "positionAllowance", "travelAllowance"};
        for (int index = 0; index < amounts.length; index++) {
            BigDecimal amount = amounts[index];
            if (amount != null && (amount.signum() < 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 16))
                throw new FieldValidationException(fields[index], "Nhập số tiền không âm, tối đa 16 chữ số phần nguyên và 2 chữ số thập phân.");
        }
    }

    public void insertCompensation(Connection connection, Employee employee, EmployeeCompensation compensation, Integer actorId) throws SQLException {
        validateCompensation(compensation);
        execute(connection, """
                INSERT INTO EmployeeCompensationHistories(CompanyId,EmployeeId,BaseSalary,
                    PositionAllowance,TravelAllowance,EffectiveFrom,Reason,CreatedByUserId)
                VALUES(?,?,?,?,?,CONVERT(date,SYSDATETIME()),N'Initial compensation',?)
                """, employee.getCompanyId(), employee.getEmployeeId(), compensation.grossSalary(),
                new OrganizationAllowanceDAO().positionAllowance(connection, employee.getCompanyId(), employee.getPositionId(), java.time.LocalDate.now()), compensation.travelAllowance(), actorId);
    }

    private void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
    }
}
