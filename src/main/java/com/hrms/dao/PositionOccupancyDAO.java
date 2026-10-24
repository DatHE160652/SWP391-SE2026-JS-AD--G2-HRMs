package com.hrms.dao;

import com.hrms.service.FieldValidationException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class PositionOccupancyDAO {
    public void validate(Connection connection, int companyId, int employeeId, Integer positionId,
            LocalDate effectiveFrom, LocalDate effectiveTo) throws SQLException {
        if (positionId == null) return;
        var dao = new EmployeeLifecycleDAO();
        if (dao.query(connection, """
                SELECT 1 FROM Positions p JOIN ResponsibilityLevels r
                    ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                WHERE p.CompanyId=? AND p.PositionId=? AND UPPER(r.LevelCode)='MANAGER'
                """, companyId, positionId).isEmpty()) return;
        if (!dao.query(connection, """
                SELECT 1 FROM EmployeePositionAssignments WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND PositionId=? AND EmployeeId<>?
                    AND (EffectiveTo IS NULL OR EffectiveTo>EffectiveFrom)
                    AND (EffectiveTo IS NULL OR EffectiveTo>?)
                    AND (? IS NULL OR EffectiveFrom<CONVERT(date,?))
                """, companyId, positionId, employeeId, effectiveFrom, effectiveTo, effectiveTo).isEmpty())
            throw new FieldValidationException("positionId", "Chức danh thuộc cấp Manager đã có người được phân công trong thời gian này. Mỗi chức danh cấp Manager chỉ được phân công cho một người; hãy chọn chức danh khác hoặc xử lý điều chuyển người hiện tại trước.");
    }
}
