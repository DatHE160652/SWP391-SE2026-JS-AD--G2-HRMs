package com.hrms.service;

import com.hrms.dao.ResponsibilityLevelDAO;
import com.hrms.dao.DepartmentDAO;
import com.hrms.model.ResponsibilityLevel;
import java.sql.SQLException;
import java.util.List;

public class ResponsibilityLevelService {
    private final ResponsibilityLevelDAO levelDAO=new ResponsibilityLevelDAO();
    private final DepartmentDAO departmentDAO=new DepartmentDAO();

    public List<ResponsibilityLevel> getLevels(int companyId) throws SQLException { return levelDAO.getLevels(companyId); }
    public ResponsibilityLevel getLevel(int companyId,int levelId) throws SQLException { return levelDAO.getLevel(companyId,levelId); }

    public boolean createLevel(int companyId,ResponsibilityLevel level) throws SQLException {
        level.setCompanyId(companyId);
        validate(level,null);
        if (!levelDAO.insertLevel(level)) throw new IllegalArgumentException("Công ty không còn hoạt động.");
        return true;
    }

    public boolean updateLevel(int companyId,ResponsibilityLevel level) throws SQLException {
        if (level.getResponsibilityLevelId()==null || getLevel(companyId,level.getResponsibilityLevelId())==null) {
            throw new IllegalArgumentException("Không tìm thấy cấp trách nhiệm trong công ty.");
        }
        level.setCompanyId(companyId);
        validate(level,level.getResponsibilityLevelId());
        if (!levelDAO.updateLevel(level)) throw new FieldValidationException("rankOrder",
                "Cấp trách nhiệm đang được chức danh hoặc chính sách phụ cấp tham chiếu; không thể đổi mã hoặc thứ bậc. Hãy tạo cấp mới.");
        return true;
    }

    public boolean deactivateLevel(int companyId,int levelId) throws SQLException {
        if (!levelDAO.deactivateLevel(companyId,levelId)) throw new IllegalArgumentException("Không tìm thấy cấp trách nhiệm trong công ty.");
        return true;
    }

    private void validate(ResponsibilityLevel level,Integer excludeId) throws SQLException {
        com.hrms.dao.OrganizationAllowanceDAO.validate(level.getDefaultAllowance(),"defaultAllowance");
        level.setLevelCode(DepartmentService.required("levelCode",level.getLevelCode(),50));
        level.setLevelName(DepartmentService.required("levelName",level.getLevelName(),100));
        if (level.getRankOrder()<0) throw new FieldValidationException("rankOrder","Thứ bậc phải là số nguyên không âm.");
        if (!departmentDAO.companyExists(level.getCompanyId())) throw new IllegalArgumentException("Công ty không hợp lệ hoặc đã ngừng hoạt động.");
        if (levelDAO.valueExists(level.getCompanyId(),level.getLevelCode(),null,excludeId)) {
            throw new FieldValidationException("levelCode","Mã cấp trách nhiệm đã tồn tại trong công ty.");
        }
        if (new com.hrms.dao.OrganizationNameDAO().exists(com.hrms.dao.OrganizationNameDAO.Catalog.RESPONSIBILITY_LEVEL,
                level.getCompanyId(), level.getLevelName(), excludeId)) {
            throw new FieldValidationException("levelName","Tên cấp trách nhiệm đã tồn tại trong công ty.");
        }
        if (levelDAO.valueExists(level.getCompanyId(),null,level.getRankOrder(),excludeId)) {
            throw new FieldValidationException("rankOrder","Thứ bậc đã được sử dụng trong công ty. Vui lòng chọn giá trị khác.");
        }
    }
}
