package com.hrms.dao;

import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.DBConnection;
import com.hrms.util.Json;
import com.hrms.util.SecuritySupport;
import java.sql.*;
import java.util.*;

/** UC22 transaction keeps actor, employee, role and contract state stable until commit. */
public class EmployeePasswordResetDAO {
    @FunctionalInterface interface ConnectionProvider { Connection open() throws SQLException; }
    private final ConnectionProvider connections;
    public EmployeePasswordResetDAO() { this(DBConnection::getConnection); }
    EmployeePasswordResetDAO(ConnectionProvider connections) { this.connections=connections; }
    public static class ConflictException extends RuntimeException {
        public ConflictException() { super("Dữ liệu đã thay đổi hoặc tài khoản không thể đặt lại. Vui lòng tìm lại nhân viên."); }
    }
    @FunctionalInterface public interface Preparation<T> { T prepare(Map<String,Object> target); }
    /**
     * The two email columns use different collations (Employees.Email inherits the database
     * default while Users.Email is Latin1_General_100_CI_AS). Each CASE branch needs its own
     * COLLATE; placing COLLATE only after END raises SQL Server error 457.
     */
    private static final String EMAIL_EXPR =
            "CASE WHEN e.UserId IS NULL THEN e.Email COLLATE Latin1_General_100_CI_AS ELSE u.Email COLLATE Latin1_General_100_CI_AS END";
    private static final String SELECT = """
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.IdentityNumber,e.Phone,e.Status,
                   u.UserId,
            """ + EMAIL_EXPR + """
             AS Email,
                   r.RoleCode,u.IsActive,u.IsLocked,u.MustChangePassword,u.RowVersion,c.WorkspaceCode,
                   CASE WHEN u.LockedUntil>SYSUTCDATETIME() THEN 1 ELSE 0 END AS TimedLock,
                   CASE WHEN
            """ + ContractAccessPolicy.ALLOWED + """
                   THEN 1 ELSE 0 END AS ContractAllowed
            FROM Employees e
            JOIN Companies c ON c.CompanyId=e.CompanyId
            LEFT JOIN Users u ON u.CompanyId=e.CompanyId AND u.UserId=e.UserId
            LEFT JOIN Roles r ON r.RoleId=u.RoleId
            WHERE e.CompanyId=?
            """;

    public List<Map<String,Object>> search(TenantPrincipal actor, String email, String identityNumber) throws SQLException {
        String sql=SELECT+" AND (e.UserId IS NULL OR r.RoleCode IN ('MANAGER','EMPLOYEE'))"
                +(email==null?"":" AND LOWER(LTRIM(RTRIM("+EMAIL_EXPR+")))=?")
                +(identityNumber==null?"":" AND e.IdentityNumber=?")+" ORDER BY e.EmployeeId";
        try (Connection connection=connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                requireActor(connection,actor);
                List<Map<String,Object>> result=new ArrayList<>();
                try (PreparedStatement statement=connection.prepareStatement(sql)) {
                    int i=1; statement.setInt(i++,actor.getCompanyId());
                    if(email!=null) statement.setString(i++,email);
                    if(identityNumber!=null) statement.setString(i,identityNumber);
                    try(ResultSet rows=statement.executeQuery()) { while(rows.next()) result.add(map(rows,actor)); }
                }
                connection.commit(); return result;
            } catch(SQLException|RuntimeException exception) { connection.rollback(); throw exception; }
        }
    }

    public <T> T reset(TenantPrincipal actor, int employeeId, byte[] version, String hash,
                       String operationId, Preparation<T> preparation) throws SQLException {
        try(Connection connection=connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                try(Statement options=connection.createStatement()) {
                    options.execute("SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET NUMERIC_ROUNDABORT OFF;");
                }
                OrganizationTransaction.lockCompany(connection,actor.getCompanyId());
                requireActor(connection,actor);
                Map<String,Object> target;
                String locked=SELECT.replace("FROM Employees e", "FROM Employees e WITH (UPDLOCK,HOLDLOCK)")
                        .replace("LEFT JOIN Users u", "LEFT JOIN Users u WITH (UPDLOCK,HOLDLOCK)");
                try(PreparedStatement statement=connection.prepareStatement(locked+" AND e.EmployeeId=?")) {
                    statement.setInt(1,actor.getCompanyId()); statement.setInt(2,employeeId);
                    try(ResultSet rows=statement.executeQuery()) {
                        if(!rows.next()) throw new ConflictException();
                        target=map(rows,actor);
                    }
                }
                if(!Boolean.TRUE.equals(target.get("canReset")) || !HexFormat.of().formatHex(version).equals(target.get("version")))
                    throw new ConflictException();
                T prepared=preparation.prepare(target); // local configuration validation only, never SMTP
                try(PreparedStatement statement=connection.prepareStatement("UPDATE Users SET Password=?,MustChangePassword=1,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND UserId=? AND RowVersion=?")) {
                    statement.setString(1,hash); statement.setInt(2,actor.getCompanyId());
                    statement.setInt(3,(Integer)target.get("userId")); statement.setBytes(4,version);
                    if(statement.executeUpdate()!=1) throw new ConflictException();
                }
                audit(connection,actor,employeeId,operationId,"PORTAL_RESET","PENDING");
                connection.commit();
                return prepared;
            } catch(SQLException|RuntimeException exception) { connection.rollback(); throw exception; }
        }
    }

    private void requireActor(Connection connection,TenantPrincipal actor) throws SQLException {
        String sql="""
                SELECT u.Password FROM Users u WITH (HOLDLOCK)
                JOIN Roles r WITH (HOLDLOCK) ON r.RoleId=u.RoleId
                JOIN Companies c WITH (HOLDLOCK) ON c.CompanyId=u.CompanyId
                JOIN Subscriptions s WITH (HOLDLOCK) ON s.CompanyId=c.CompanyId
                WHERE u.UserId=? AND u.CompanyId=? AND r.RoleCode=? AND c.WorkspaceCode=?
                AND r.RoleCode IN ('COMPANY_ADMIN','HR') AND u.IsActive=1 AND u.IsLocked=0
                AND u.MustChangePassword=0 AND (u.LockedUntil IS NULL OR u.LockedUntil<=SYSUTCDATETIME())
                AND c.IsActive=1 AND s.Status=N'ACTIVE'
                AND NOT EXISTS (SELECT 1 FROM Employees ae WHERE ae.CompanyId=u.CompanyId AND ae.UserId=u.UserId AND ae.Status<>'Active')
                """+" AND "+ContractAccessPolicy.ALLOWED;
        try(PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setInt(1,actor.getUserId()); statement.setInt(2,actor.getCompanyId());
            statement.setString(3,actor.getRoleCode()); statement.setString(4,actor.getWorkspaceCode());
            try(ResultSet rows=statement.executeQuery()) {
                if(!rows.next() || actor.getCredentialVersion()==null
                        || !java.security.MessageDigest.isEqual(actor.getCredentialVersion().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        SecuritySupport.credentialVersion(rows.getString(1)).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    throw new SecurityException("Phiên quản trị không còn quyền thực hiện thao tác.");
            }
        }
    }

    public void recordDelivery(TenantPrincipal actor,int employeeId,String operationId,String status) throws SQLException {
        try(Connection connection=connections.open()) {
            audit(connection,actor,employeeId,operationId,"PORTAL_RESET_EMAIL",status);
        }
    }
    private void audit(Connection connection,TenantPrincipal actor,int employeeId,String operationId,String action,String status) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("INSERT INTO AuditLogs (CompanyId,UserId,Action,TableName,RecordId,NewValues) VALUES (?,?,?,'Employees',?,?)")) {
            statement.setInt(1,actor.getCompanyId()); statement.setInt(2,actor.getUserId()); statement.setString(3,action);
            statement.setString(4,Integer.toString(employeeId));
            statement.setString(5,Json.encode(Map.of("operationId",operationId,"deliveryStatus",status)));
            if(statement.executeUpdate()!=1) throw new SQLException("Reset audit was not recorded.");
        }
    }

    private Map<String,Object> map(ResultSet rows,TenantPrincipal actor) throws SQLException {
        Map<String,Object> data=new LinkedHashMap<>();
        Integer userId=rows.getObject("UserId",Integer.class);
        boolean active=rows.getBoolean("IsActive") && "Active".equals(rows.getString("Status"));
        String role=rows.getString("RoleCode");
        String reason=unavailableReason(userId,actor.getUserId(),role,active,rows.getBoolean("ContractAllowed"),rows.getString("Email"));
        data.put("employeeId",rows.getInt("EmployeeId")); data.put("userId",userId);
        data.put("employeeCode",rows.getString("EmployeeCode")); data.put("fullName",rows.getString("FullName"));
        data.put("email",rows.getString("Email")); data.put("identityNumber",rows.getString("IdentityNumber"));
        data.put("phone",rows.getString("Phone")); data.put("roleCode",role); data.put("isActive",active);
        data.put("isLocked",rows.getBoolean("IsLocked") || rows.getBoolean("TimedLock"));
        data.put("mustChangePassword",rows.getBoolean("MustChangePassword")); data.put("canReset",reason==null);
        data.put("unavailableReason",reason);
        byte[] version=rows.getBytes("RowVersion"); data.put("version",version==null?null:HexFormat.of().formatHex(version));
        data.put("workspaceCode",rows.getString("WorkspaceCode")); return data;
    }
    static String unavailableReason(Integer userId,Integer actorId,String role,boolean active,boolean contract,String email) {
        if(userId==null) return "Nhân viên chưa được cấp tài khoản. Vui lòng cấp tài khoản trước.";
        if(userId.equals(actorId)) return "Không thể đặt lại mật khẩu của chính mình tại đây.";
        if(!"MANAGER".equals(role) && !"EMPLOYEE".equals(role)) return "Chỉ được đặt lại tài khoản Manager/Employee.";
        if(!active) return "Nhân viên hoặc tài khoản không hoạt động.";
        if(!contract) return "Tài khoản không còn quyền truy cập theo hợp đồng.";
        if(email==null || email.isBlank()) return "Tài khoản chưa có email đăng nhập hợp lệ.";
        return null;
    }
}
