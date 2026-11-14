package com.hrms.dao;

import com.hrms.model.Position;
import com.hrms.service.FieldValidationException;
import java.sql.Connection;
import java.sql.SQLException;

public final class ReportingPositionDAO {
    public void validatePosition(Connection connection, Position position) throws SQLException {
        var dao=new EmployeeLifecycleDAO();
        var levels=dao.query(connection,"SELECT LevelCode,LevelName FROM ResponsibilityLevels WHERE CompanyId=? AND ResponsibilityLevelId=?",
                position.getCompanyId(),position.getResponsibilityLevelId());
        if (levels.isEmpty()) throw new FieldValidationException("responsibilityLevelId","Không tìm thấy cấp trách nhiệm.");
        var level=levels.get(0);
        boolean employee=java.util.Set.of("EMP","EMPLOYEE").contains(level.get("LevelCode").toString().strip().toUpperCase(java.util.Locale.ROOT))
                || "EMPLOYEE".equalsIgnoreCase(level.get("LevelName").toString().strip());
        boolean manager=!employee && (position.isCanManageEmployees() || "MANAGER".equalsIgnoreCase(level.get("LevelCode").toString().strip())
                || "MANAGER".equalsIgnoreCase(level.get("LevelName").toString().strip()));
        Integer parent=position.getReportsToPositionId();
        if (parent!=null) {
            if (parent.equals(position.getPositionId()) || manager)
                throw new FieldValidationException("reportsToPositionId","Chức danh quản lý không nhận quản lý cấp trên và không được tự quản lý.");
            var parents=dao.query(connection,"""
                    SELECT 1 FROM Positions p JOIN ResponsibilityLevels r
                        ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                    WHERE p.CompanyId=? AND p.PositionId=? AND p.DepartmentId=? AND p.IsActive=1 AND p.CanManageEmployees=1
                        AND r.IsActive=1 AND UPPER(LTRIM(RTRIM(r.LevelCode))) NOT IN ('EMP','EMPLOYEE')
                        AND UPPER(LTRIM(RTRIM(r.LevelName)))<>'EMPLOYEE' AND p.ReportsToPositionId IS NULL
                    """,position.getCompanyId(),parent,position.getDepartmentId());
            if (parents.isEmpty()) throw new FieldValidationException("reportsToPositionId","Chọn chức danh quản lý đang hoạt động trong cùng phòng ban.");
            if (position.getPositionId()!=null && !dao.query(connection,"""
                    SELECT 1 FROM Employees e LEFT JOIN Employees manager ON manager.CompanyId=e.CompanyId AND manager.EmployeeId=e.ManagerId
                    WHERE e.CompanyId=? AND e.PositionId=? AND e.ManagerId IS NOT NULL
                        AND (manager.PositionId IS NULL OR manager.PositionId<>?)
                    """,position.getCompanyId(),position.getPositionId(),parent).isEmpty())
                throw new FieldValidationException("reportsToPositionId","Có nhân viên đang thuộc quản lý khác. Hãy xử lý phân công trước khi đổi liên kết chức danh quản lý.");
        }
        if (position.getPositionId()!=null && !dao.query(connection,"""
                SELECT 1 FROM Positions child WHERE child.CompanyId=? AND child.ReportsToPositionId=?
                    AND (child.DepartmentId<>? OR ?=0 OR ?=0 OR ? IS NOT NULL)
                """,position.getCompanyId(),position.getPositionId(),position.getDepartmentId(),
                position.isActive(),position.isCanManageEmployees() && !employee,parent).isEmpty())
            throw new FieldValidationException("canManageEmployees","Chức danh đang được liên kết làm quản lý. Hãy cập nhật các chức danh phụ thuộc trước.");
    }

    public void validateManager(Connection connection, int companyId, Integer positionId, Integer managerId) throws SQLException {
        if (positionId==null || managerId==null) return;
        var dao=new EmployeeLifecycleDAO();
        var positions=dao.query(connection,"SELECT ReportsToPositionId FROM Positions WHERE CompanyId=? AND PositionId=?",companyId,positionId);
        if (positions.isEmpty()) throw new FieldValidationException("positionId","Không tìm thấy chức danh.");
        Integer parent=(Integer)positions.get(0).get("ReportsToPositionId");
        if (parent==null) return;
        if (dao.query(connection,"""
                SELECT 1 FROM Employees manager JOIN EmployeePositionAssignments a
                    ON a.CompanyId=manager.CompanyId AND a.EmployeeId=manager.EmployeeId
                WHERE manager.CompanyId=? AND manager.EmployeeId=? AND manager.Status='Active' AND manager.PositionId=?
                    AND a.PositionId=? AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                    AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME()))
                """,companyId,managerId,parent,parent).isEmpty())
            throw new FieldValidationException("managerId","Quản lý trực tiếp phải đảm nhiệm đúng chức danh quản lý đã cấu hình cho nhân viên, không chọn quản lý team khác.");
    }
}
