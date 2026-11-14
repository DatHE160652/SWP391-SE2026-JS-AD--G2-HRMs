package com.hrms.dao;

import com.hrms.model.EmployeeCareerEvent;
import com.hrms.util.DBConnection;
import java.sql.*;
import java.util.*;

public class EmployeeCareerHistoryDAO {
    public List<EmployeeCareerEvent> getHistory(int companyId, int employeeId) throws SQLException {
        try (Connection connection=DBConnection.getConnection()) {
            requireEmployee(connection,companyId,employeeId);
            EmployeeHistoryValues formatter=new EmployeeHistoryValues(connection,companyId);
            List<EmployeeCareerEvent> events=new ArrayList<>();
            readPositionChanges(connection,companyId,employeeId,formatter,events);
            readInitialAssignments(connection,companyId,employeeId,formatter,events);
            readDepartmentChanges(connection,companyId,employeeId,formatter,events);
            events.sort(Comparator.comparing(EmployeeCareerEvent::effectiveDate,Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(EmployeeCareerEvent::recordedAt,Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(EmployeeCareerEvent::eventId,Comparator.reverseOrder()));
            return events.stream().limit(100).toList();
        }
    }

    private void requireEmployee(Connection connection,int companyId,int employeeId) throws SQLException {
        try (PreparedStatement statement=connection.prepareStatement("SELECT 1 FROM Employees WHERE CompanyId=? AND EmployeeId=?")) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId);
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) throw new SecurityException("Không tìm thấy nhân viên trong công ty của bạn.");
            }
        }
    }

    private void readPositionChanges(Connection connection,int companyId,int employeeId,EmployeeHistoryValues formatter,
                                      List<EmployeeCareerEvent> events) throws SQLException {
        String sql="""
                SELECT TOP (100) c.PositionChangeId,c.ChangeType,c.Status,c.EffectiveDate,c.Reason,
                    COALESCE(c.AppliedAt,c.ApprovedAt,c.CreatedAt) AS RecordedAt,
                    c.OldPositionId,c.NewPositionId,c.OldManagerId,c.NewManagerId,oldPosition.DepartmentId AS OldDepartmentId,
                    newPosition.DepartmentId AS NewDepartmentId,oldLevel.ResponsibilityLevelId AS OldLevelId,
                    newLevel.ResponsibilityLevelId AS NewLevelId,oldLevel.LevelName AS OldLevelName,
                    newLevel.LevelName AS NewLevelName,oldLevel.RankOrder AS OldRank,newLevel.RankOrder AS NewRank,
                    COALESCE(approver.DisplayName,creator.DisplayName,N'Không ghi nhận') AS ChangedBy
                FROM EmployeePositionChanges c
                LEFT JOIN Positions oldPosition ON oldPosition.CompanyId=c.CompanyId AND oldPosition.PositionId=c.OldPositionId
                LEFT JOIN Positions newPosition ON newPosition.CompanyId=c.CompanyId AND newPosition.PositionId=c.NewPositionId
                LEFT JOIN ResponsibilityLevels oldLevel ON oldLevel.CompanyId=oldPosition.CompanyId AND oldLevel.ResponsibilityLevelId=oldPosition.ResponsibilityLevelId
                LEFT JOIN ResponsibilityLevels newLevel ON newLevel.CompanyId=newPosition.CompanyId AND newLevel.ResponsibilityLevelId=newPosition.ResponsibilityLevelId
                LEFT JOIN Users creator ON creator.CompanyId=c.CompanyId AND creator.UserId=c.CreatedByUserId
                LEFT JOIN Users approver ON approver.CompanyId=c.CompanyId AND approver.UserId=c.ApprovedByUserId
                WHERE c.CompanyId=? AND c.EmployeeId=? AND c.ChangeType<>'MANAGER_CHANGE'
                ORDER BY COALESCE(c.AppliedAt,c.ApprovedAt,c.CreatedAt) DESC,c.PositionChangeId DESC
                """;
        List<PositionRow> pending=new ArrayList<>();
        try (PreparedStatement statement=prepare(connection,sql,companyId,employeeId); ResultSet rows=statement.executeQuery()) {
            while (rows.next()) {
                String oldRaw="DepartmentId="+rows.getObject("OldDepartmentId")+";PositionId="+rows.getObject("OldPositionId");
                String newRaw="DepartmentId="+rows.getObject("NewDepartmentId")+";PositionId="+rows.getObject("NewPositionId");
                if("MANAGER_CHANGE".equals(rows.getString("ChangeType"))) {
                    oldRaw="ManagerId="+rows.getObject("OldManagerId");
                    newRaw="ManagerId="+rows.getObject("NewManagerId");
                }
                boolean changedLevel=!Objects.equals(rows.getObject("OldLevelId"),rows.getObject("NewLevelId"));
                pending.add(new PositionRow(new EmployeeCareerEvent("change-"+rows.getLong("PositionChangeId"),
                        rows.getString("ChangeType"),timestamp(rows,"RecordedAt"),rows.getString("EffectiveDate"),
                        rows.getString("Status"),oldRaw,newRaw,rows.getString("Reason"),rows.getString("ChangedBy")),
                        changedLevel?level(rows,"OldLevelName","OldRank"):null,
                        changedLevel?level(rows,"NewLevelName","NewRank"):null));
            }
        }
        for (PositionRow row:pending) {
            EmployeeCareerEvent event=row.event();
            var values=formatter.format(event.oldValue(),event.newValue());
            events.add(new EmployeeCareerEvent(event.eventId(),event.eventType(),event.recordedAt(),event.effectiveDate(),event.status(),
                    values.oldValue()+(row.oldLevel()==null?"":"\nCấp trách nhiệm: "+row.oldLevel()),
                    values.newValue()+(row.newLevel()==null?"":"\nCấp trách nhiệm: "+row.newLevel()),event.reason(),event.changedBy()));
        }
    }

    private void readInitialAssignments(Connection connection,int companyId,int employeeId,EmployeeHistoryValues formatter,
                                         List<EmployeeCareerEvent> events) throws SQLException {
        String sql="""
                SELECT TOP (100) a.AssignmentId,a.DepartmentId,a.PositionId,a.EffectiveFrom,
                    COALESCE(l.CreatedAt,a.CreatedAt) AS CreatedAt,
                    CASE WHEN l.LogId IS NOT NULL AND a.ChangeType<>'BASELINE' THEN 'HIRE' ELSE a.ChangeType END AS ChangeType,a.Reason,
                    l.OldValues,l.NewValues,COALESCE(u.DisplayName,N'Không ghi nhận') AS ChangedBy,
                    CASE WHEN l.LogId IS NOT NULL AND a.SourceChangeId IS NOT NULL THEN 'CORRECTED'
                         WHEN a.EffectiveFrom>CONVERT(date,SYSDATETIME()) THEN 'SCHEDULED'
                         WHEN a.EffectiveTo<=CONVERT(date,SYSDATETIME()) THEN 'ENDED' ELSE 'ACTIVE' END AS Status
                FROM EmployeePositionAssignments a
                LEFT JOIN AuditLogs l ON l.CompanyId=a.CompanyId AND l.TableName='EmployeePositionAssignments'
                    AND l.RecordId=CONVERT(varchar(100),a.AssignmentId) AND l.Action='ASSIGN_POSITION'
                LEFT JOIN Users u ON u.CompanyId=a.CompanyId AND u.UserId=l.UserId
                WHERE a.CompanyId=? AND a.EmployeeId=? AND a.ChangeType<>'MANAGER_CHANGE' AND (a.SourceChangeId IS NULL OR l.LogId IS NOT NULL)
                ORDER BY COALESCE(l.CreatedAt,a.CreatedAt) DESC,a.AssignmentId DESC
                """;
        List<EmployeeCareerEvent> pending=new ArrayList<>();
        try (PreparedStatement statement=prepare(connection,sql,companyId,employeeId); ResultSet rows=statement.executeQuery()) {
            while (rows.next()) pending.add(new EmployeeCareerEvent("assignment-"+rows.getLong("AssignmentId"),rows.getString("ChangeType"),
                    timestamp(rows,"CreatedAt"),rows.getString("EffectiveFrom"),rows.getString("Status"),
                    rows.getString("OldValues")==null?"Không ghi nhận vị trí trước mốc dữ liệu":rows.getString("OldValues"),
                    rows.getString("NewValues")==null?"DepartmentId="+rows.getObject("DepartmentId")+";PositionId="+rows.getObject("PositionId"):withoutReason(rows.getString("NewValues")),
                    rows.getString("NewValues")==null?rows.getString("Reason"):reason(rows.getString("NewValues")),rows.getString("ChangedBy")));
        }
        for (EmployeeCareerEvent event:pending) {
            boolean legacy="Không ghi nhận vị trí trước mốc dữ liệu".equals(event.oldValue());
            var values=formatter.format(legacy?null:event.oldValue(),event.newValue());
            events.add(new EmployeeCareerEvent(event.eventId(),event.eventType(),event.recordedAt(),event.effectiveDate(),event.status(),
                    legacy?event.oldValue():values.oldValue(),
                    values.newValue(),event.reason(),event.changedBy()));
        }
    }

    private void readDepartmentChanges(Connection connection,int companyId,int employeeId,EmployeeHistoryValues formatter,
                                      List<EmployeeCareerEvent> events) throws SQLException {
        String sql="""
                SELECT TOP (100) l.LogId,l.Action,l.CreatedAt,l.OldValues,l.NewValues,
                    CONVERT(date,l.CreatedAt AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time') AS EffectiveDate,
                    COALESCE(u.DisplayName,N'Không ghi nhận') AS ChangedBy
                FROM AuditLogs l LEFT JOIN Users u ON u.CompanyId=? AND u.UserId=l.UserId
                WHERE (l.CompanyId=? OR l.CompanyId IS NULL) AND l.TableName='Employees' AND l.RecordId=?
                    AND l.Action IN ('CHANGE_ORGANIZATION','CHANGE_DEPARTMENT')
                ORDER BY l.CreatedAt DESC,l.LogId DESC
                """;
        List<ManagerRow> pending=new ArrayList<>();
        try (PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setInt(1,companyId); statement.setInt(2,companyId); statement.setString(3,String.valueOf(employeeId));
            try (ResultSet rows=statement.executeQuery()) {
                while (rows.next()) pending.add(new ManagerRow(rows.getLong("LogId"),rows.getString("Action"),
                        timestamp(rows,"CreatedAt"),rows.getString("EffectiveDate"),rows.getString("OldValues"),rows.getString("NewValues"),rows.getString("ChangedBy")));
            }
        }
        for (ManagerRow row:pending) {
            var values=formatter.format(row.oldValue(),row.newValue());
            if (line(values.oldValue(),"Chức vụ:")==null && line(values.newValue(),"Chức vụ:")==null) {
                String oldDepartment=line(values.oldValue(),"Phòng ban:");
                String newDepartment=line(values.newValue(),"Phòng ban:");
                if (!Objects.equals(oldDepartment,newDepartment)) events.add(new EmployeeCareerEvent("audit-"+row.logId(),
                        oldDepartment!=null && oldDepartment.endsWith("Chưa có phòng ban")?"DEPARTMENT_ASSIGNED":"DEPARTMENT_TRANSFERRED",
                        row.createdAt(),row.effectiveDate(),"APPLIED",oldDepartment,newDepartment,reason(values.newValue()),row.changedBy()));
            }
        }
    }

    private PreparedStatement prepare(Connection connection,String sql,int companyId,int employeeId) throws SQLException {
        PreparedStatement statement=connection.prepareStatement(sql);
        statement.setInt(1,companyId); statement.setInt(2,employeeId);
        return statement;
    }

    private String line(String value,String prefix) {
        if (value==null) return null;
        return Arrays.stream(value.split("\\R")).filter(part->part.startsWith(prefix)).findFirst().orElse(null);
    }

    private String withoutReason(String value) {
        return value==null?null:Arrays.stream(value.split("\\R")).filter(part->!part.startsWith("Lý do:"))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String reason(String value) {
        String reason=line(value,"Lý do:"); return reason==null?null:reason.substring("Lý do:".length()).strip();
    }

    private String timestamp(ResultSet rows,String column) throws SQLException {
        Timestamp value=rows.getTimestamp(column); return value==null?null:value.toLocalDateTime().toString();
    }

    private String level(ResultSet rows,String name,String rank) throws SQLException {
        return rows.getString(name)==null?"Không ghi nhận":rows.getString(name)+" (thứ bậc "+rows.getInt(rank)+")";
    }

    private record PositionRow(EmployeeCareerEvent event,String oldLevel,String newLevel) {}
    private record ManagerRow(long logId,String action,String createdAt,String effectiveDate,String oldValue,String newValue,String changedBy) {}
}
