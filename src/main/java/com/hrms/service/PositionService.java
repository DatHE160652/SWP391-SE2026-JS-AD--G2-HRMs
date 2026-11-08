package com.hrms.service;

import com.hrms.dao.DepartmentDAO;
import com.hrms.dao.PositionDAO;
import com.hrms.dao.PositionCatalogDAO;
import com.hrms.model.Position;
import java.sql.SQLException;
import java.util.List;

public class PositionService {
    private final PositionDAO positionDAO;
    private final DepartmentDAO departmentDAO;
    private final PositionCatalogDAO catalogDAO;

    public PositionService() {
        this(new PositionDAO(), new DepartmentDAO(), new PositionCatalogDAO());
    }

    PositionService(PositionDAO positionDAO, DepartmentDAO departmentDAO, PositionCatalogDAO catalogDAO) {
        this.positionDAO = positionDAO;
        this.departmentDAO = departmentDAO;
        this.catalogDAO = catalogDAO;
    }

    public List<Position> getPositions(int companyId, Integer departmentId, boolean activeOnly) throws SQLException {
        if (departmentId != null && departmentDAO.getDepartmentById(companyId, departmentId) == null) {
            throw new FieldValidationException("departmentId", "Không tìm thấy phòng ban trong công ty của bạn.");
        }
        return positionDAO.getPositions(companyId, departmentId, activeOnly);
    }

    public Position getPositionDetail(int companyId, int id) throws SQLException {
        return positionDAO.getPositionById(companyId, id);
    }

    public boolean createPosition(int companyId, Position position) throws SQLException {
        position.setCompanyId(companyId);
        validate(position, null);
        if (!positionDAO.insertPosition(position)) throw new IllegalArgumentException("Công ty hoặc danh mục đã chọn không còn khả dụng.");
        return true;
    }

    public boolean updatePosition(int companyId, Position position) throws SQLException {
        Position existing = getPositionDetail(companyId, position.getPositionId());
        if (existing == null) throw new IllegalArgumentException("Không tìm thấy chức danh.");
        position.setCompanyId(companyId);
        if (!position.isAllowanceSpecified()) position.setPositionAllowance(existing.getPositionAllowance());
        if (!position.isReportingPositionSpecified()) position.setReportsToPositionId(existing.getReportsToPositionId());
        validate(position, existing);
        if ((!existing.getDepartmentId().equals(position.getDepartmentId())
                || !existing.getResponsibilityLevelId().equals(position.getResponsibilityLevelId()))
                && positionDAO.isPositionAssignedToEmployees(companyId, position.getPositionId())) {
            throw new IllegalArgumentException("Chức danh đã được sử dụng không được đổi phòng ban hoặc cấp trách nhiệm. Hãy tạo chức danh mới và thay đổi phân công theo ngày hiệu lực.");
        }
        if (!positionDAO.updatePosition(position)) throw new IllegalArgumentException("Chức danh đã thay đổi hoặc đang được lịch sử nhân viên sử dụng. Vui lòng tải lại và thử lại.");
        return true;
    }

    public boolean setPositionActive(int companyId, int id, boolean active) throws SQLException {
        Position position = getPositionDetail(companyId, id);
        if (position == null) throw new IllegalArgumentException("Không tìm thấy chức danh.");
        position.setActive(active);
        return updatePosition(companyId, position);
    }

    public boolean deactivatePosition(int companyId, int id) throws SQLException {
        return setPositionActive(companyId, id, false);
    }

    public String getCatalogJson(int companyId) throws SQLException {
        return catalogDAO.getCatalogJson(companyId);
    }

    private void validate(Position position, Position existing) throws SQLException {
        if (position.getPositionAllowance()!=null && (existing==null
                || !java.util.Objects.equals(position.getPositionAllowance(),existing.getPositionAllowance())))
            throw new FieldValidationException("positionAllowance","Phụ cấp được lấy từ cấp trách nhiệm; không cấu hình riêng cho chức danh.");
        if (position.getDepartmentId() == null
                || departmentDAO.getDepartmentById(position.getCompanyId(), position.getDepartmentId()) == null) {
            throw new FieldValidationException("departmentId", "Vui lòng chọn phòng ban thuộc công ty của bạn.");
        }
        com.hrms.model.Department department = departmentDAO.getDepartmentById(position.getCompanyId(), position.getDepartmentId());
        if (!department.isActive() && (existing == null || !existing.getDepartmentId().equals(position.getDepartmentId())
                || (!existing.isActive() && position.isActive()))) {
            throw new FieldValidationException("departmentId", "Vui lòng chọn phòng ban đang hoạt động.");
        }
        boolean sameLevel = existing != null && existing.getResponsibilityLevelId().equals(position.getResponsibilityLevelId());
        if (!catalogDAO.responsibilityLevelExists(position.getCompanyId(), position.getResponsibilityLevelId(), !sameLevel)) {
            throw new FieldValidationException("responsibilityLevelId", "Vui lòng chọn cấp trách nhiệm đang hoạt động trong công ty.");
        }
        if (position.isCanManageEmployees() && catalogDAO.isEmployeeLevel(position.getCompanyId(), position.getResponsibilityLevelId())) {
            throw new FieldValidationException("canManageEmployees", "Cấp trách nhiệm Employee không được phép quản lý nhân viên.");
        }
        position.setPositionTitle(DepartmentService.required("positionTitle", position.getPositionTitle(), 150));
        if (position.getDescription() != null && position.getDescription().length() > 255) {
            throw new FieldValidationException("description", "Độ dài tối đa là 255 ký tự.");
        }
        if (positionDAO.positionTitleExists(position.getCompanyId(), position.getPositionTitle(),
                existing == null ? null : existing.getPositionId())) {
            throw new FieldValidationException("positionTitle", "Tên chức danh đã tồn tại trong công ty.");
        }
    }
}
