package com.hrms.dao;

import com.hrms.util.DBConnection;
import java.sql.*;

public final class OrganizationTransaction {
    private OrganizationTransaction() {}

    public static void lockCompany(Connection connection, int companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CompanyId FROM Companies WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=?")) {
            statement.setInt(1, companyId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("Không tìm thấy công ty.");
            }
        }
    }

    public static <Result> Result execute(int companyId, Work<Result> work) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockCompany(connection, companyId);
                Result result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @FunctionalInterface
    public interface Work<Result> {
        Result run(Connection connection) throws SQLException;
    }

    public static void audit(Connection connection, int companyId, Integer actorId, int employeeId,
                              String action, String oldValue, String newValue) throws SQLException {
        com.hrms.model.AuditLog log = new com.hrms.model.AuditLog();
        log.setCompanyId(companyId);
        log.setUserId(actorId);
        log.setTableName("Employees");
        log.setRecordId(String.valueOf(employeeId));
        log.setAction(action);
        log.setOldValues(oldValue);
        log.setNewValues(newValue);
        new AuditLogDAO().insert(connection, log);
    }

    public static void recordManagerChange(Connection connection,int companyId,Integer actorId,int employeeId,
                                            Integer oldManager,Integer newManager,String reason) throws SQLException {
        if (java.util.Objects.equals(oldManager,newManager)) return;
        if (actorId==null) throw new SecurityException("Cần quyền quản trị nhân sự để ghi nhận phân công quản lý.");
        var values=new EmployeeHistoryValues(connection,companyId).format("ManagerId="+oldManager,"ManagerId="+newManager);
        String explanation=reason==null || reason.isBlank()?"":"\nLý do: "+reason.strip().replace('\r',' ').replace('\n',' ');
        String type=oldManager==null?"MANAGER_ASSIGNED":newManager==null?"MANAGER_REMOVED":"MANAGER_TRANSFERRED";
        audit(connection,companyId,actorId,employeeId,type,values.oldValue(),values.newValue()+explanation);
        String employeeName;
        try (PreparedStatement statement=connection.prepareStatement("SELECT CONCAT(FullName,N' (',EmployeeCode,N')') FROM Employees WHERE CompanyId=? AND EmployeeId=?")) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId);
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Không tìm thấy nhân viên khi ghi lịch sử nhóm.");
                employeeName=rows.getString(1);
            }
        }
        String before="Nhân viên: "+employeeName+"\n"+values.oldValue();
        String after="Nhân viên: "+employeeName+"\n"+values.newValue()+explanation;
        if (oldManager!=null) audit(connection,companyId,actorId,oldManager,
                newManager==null?"TEAM_MEMBER_REMOVED":"TEAM_MEMBER_TRANSFERRED",before,after);
        if (newManager!=null) audit(connection,companyId,actorId,newManager,
                oldManager==null?"TEAM_MEMBER_ASSIGNED":"TEAM_MEMBER_TRANSFERRED",before,after);
    }
}
