package com.hrms.service;

import com.hrms.dao.EmployeeDAO;
import com.hrms.dao.OrganizationAssignmentDAO;
import com.hrms.model.Employee;
import com.hrms.model.EmployeeCompensation;
import com.hrms.dao.EmployeeLifecycleDAO;
import com.hrms.dao.EmployeeAssignmentDAO;
import com.hrms.dao.OrganizationTransaction;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EmployeeService {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public List<Employee> getAllEmployees() throws SQLException {
        return employeeDAO.getAllEmployees();
    }

    public List<Employee> getAllEmployees(int companyId) throws SQLException {
        return employeeDAO.getAllEmployees(companyId);
    }

    public Employee getEmployeeById(int employeeId) throws SQLException {
        return employeeDAO.getEmployeeById(employeeId);
    }

    public Employee getEmployeeById(int companyId, int employeeId) throws SQLException {
        return employeeDAO.getEmployeeById(companyId, employeeId);
    }

    public boolean createEmployee(Employee employee, int companyId, String employeeCodeMode) throws SQLException {
        return createEmployee(employee, companyId, employeeCodeMode, null, null);
    }

    public boolean createEmployee(Employee employee, int companyId, String employeeCodeMode,
                                  EmployeeCompensation compensation, Integer actorId) throws SQLException {
        return createEmployee(employee, companyId, employeeCodeMode, compensation, actorId, true);
    }

    public boolean createEmployeeProfile(Employee employee, int companyId, String employeeCodeMode, Integer actorId) throws SQLException {
        if (employee.getDepartmentId() != null && employee.getPositionId() == null)
            throw new FieldValidationException("positionId", "Chọn chức danh thuộc phòng ban, hoặc để trống cả hai để phân công sau.");
        return createEmployee(employee, companyId, employeeCodeMode, null, actorId, false);
    }

    private boolean createEmployee(Employee employee, int companyId, String employeeCodeMode,
            EmployeeCompensation compensation, Integer actorId, boolean assignInitialManager) throws SQLException {
        if (!assignInitialManager) employee.setManagerId(null);
        if (employeeCodeMode != null && !"MANUAL".equals(employeeCodeMode)) {
            throw new FieldValidationException("employeeCodeMode", "Mã nhân viên phải được nhập thủ công; không còn hỗ trợ tự động tạo mã.");
        }
        employee.setEmployeeCode(validateManualEmployeeCode(employee.getEmployeeCode()));
        employee.setCompanyId(companyId);
        validate(employee, true);
        validateUniquePhone(employee);
        validatePersonal(employee);
        if (actorId == null) throw new SecurityException("Cần xác định người thực hiện để ghi nhật ký tạo nhân viên.");
        EmployeeLifecycleDAO.validateCompensation(compensation);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                OrganizationTransaction.lockCompany(connection, companyId);
                new EmployeeLifecycleDAO().requireActor(connection, companyId, actorId);
                new EmployeeLifecycleDAO().validateEmail(connection, employee);
                new OrganizationAssignmentDAO().validateSelection(connection, companyId, employee.getDepartmentId(), employee.getPositionId());
                if (employeeCodeExists(connection, companyId, employee.getEmployeeCode())) {
                    throw new FieldValidationException("employeeCode", "Mã nhân viên đã tồn tại trong công ty.");
                }
                int employeeId = employeeDAO.insertEmployee(connection, employee, companyId);
                employee.setEmployeeId(employeeId);
                new OrganizationAssignmentDAO().assignOrganization(connection, companyId, employeeId, employee.getDepartmentId(), employee.getPositionId());
                EmployeeLifecycleDAO lifecycle = new EmployeeLifecycleDAO();
                Integer managerId = assignInitialManager ? lifecycle.resolveInitialManager(connection, employee) : null;
                new EmployeeAssignmentDAO().validateManagerAssignment(connection, companyId, employeeId, managerId, employee.getDepartmentId());
                new EmployeeAssignmentDAO().updateManager(connection, companyId, employeeId, managerId);
                employee.setManagerId(managerId);
                if (compensation != null && compensation.provided()) lifecycle.insertCompensation(connection, employee, compensation, actorId);
                OrganizationTransaction.audit(connection, companyId, actorId, employeeId, "CREATE_EMPLOYEE", null, employee.getEmployeeCode());
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    static String validateManualEmployeeCode(String employeeCode) {
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new FieldValidationException("employeeCode", "Vui lòng nhập mã nhân viên.");
        }
        String trimmedCode = employeeCode.trim();
        if (trimmedCode.length() > 50) {
            throw new FieldValidationException("employeeCode", "Mã nhân viên không được vượt quá 50 ký tự.");
        }
        return trimmedCode;
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        return updateEmployee(employee, null);
    }

    public boolean updateEmployee(Employee employee, Integer actorId) throws SQLException {
        validate(employee, false);
        validatePersonal(employee);
        validateUniquePhone(employee);
        Employee existing = employee.getCompanyId() == null ? null : getEmployeeById(employee.getCompanyId(), employee.getEmployeeId());
        if (existing == null || employee.getCompanyId() == null || !employee.getCompanyId().equals(existing.getCompanyId())) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty của bạn.");
        }
        if (!java.util.Objects.equals(existing.getStatus(), employee.getStatus()))
            throw new FieldValidationException("status", "Vui lòng dùng thao tác kích hoạt hoặc ngừng làm việc để đổi trạng thái nhân viên.");
        if (!java.util.Objects.equals(existing.getEmployeeCode(), employee.getEmployeeCode()))
            throw new FieldValidationException("employeeCode", "Không được đổi mã nhân viên khi chỉnh sửa hồ sơ.");
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (actorId == null) throw new SecurityException("Cần xác định người thực hiện để ghi nhật ký cập nhật nhân viên.");
                OrganizationTransaction.lockCompany(connection, employee.getCompanyId());
                new EmployeeLifecycleDAO().requireActor(connection, employee.getCompanyId(), actorId);
                new EmployeeLifecycleDAO().validateEmail(connection, employee);
                new OrganizationAssignmentDAO().assignOrganization(connection, employee.getCompanyId(), employee.getEmployeeId(),
                        employee.getDepartmentId(), employee.getPositionId());
                boolean updated = employeeDAO.updateEmployee(connection, employee);
                if (!updated) throw new SQLException("Không cập nhật được hồ sơ nhân viên.");
                OrganizationTransaction.audit(connection, employee.getCompanyId(), actorId, employee.getEmployeeId(), "UPDATE_EMPLOYEE",
                        existing.getFullName(), employee.getFullName());
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally { connection.setAutoCommit(true); }
        }
    }

    public boolean activateEmployee(int companyId, int employeeId, Integer actorId) throws SQLException {
        return OrganizationTransaction.execute(companyId,connection -> {
            new EmployeeLifecycleDAO().requireActor(connection,companyId,actorId);
            var employee=new EmployeeLifecycleDAO().employee(connection,companyId,employeeId);
            if ("Active".equals(employee.get("Status"))) throw new IllegalArgumentException("Nhân viên đã ở trạng thái đang hoạt động.");
            new EmployeeLifecycleDAO().execute(connection,"UPDATE Employees SET Status='Active',EndDate=NULL,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=?",companyId,employeeId);
            OrganizationTransaction.audit(connection,companyId,actorId,employeeId,"ACTIVATE_EMPLOYEE",String.valueOf(employee.get("Status")),"Active; portal access remains separately controlled");
            return true;
        });
    }

    private void validate(Employee employee, boolean creating) {
        if (!creating && employee.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên.");
        }
        if (employee.getFullName() == null || employee.getFullName().isBlank()) {
            throw new FieldValidationException("fullName", "Vui lòng nhập họ và tên.");
        }
        employee.setPhone(com.hrms.util.ValidationSupport.validatePhone(employee.getPhone()));
        if (employee.getGender() == null || employee.getGender().isBlank()) {
            throw new FieldValidationException("gender", "Vui lòng chọn giới tính.");
        }
        if (employee.getDateOfBirth() == null) {
            throw new FieldValidationException("dateOfBirth", "Vui lòng nhập ngày sinh.");
        }
        if (!java.util.Set.of("Active", "Inactive").contains(employee.getStatus() == null ? "" : employee.getStatus())) {
            throw new FieldValidationException("status", "Vui lòng chọn đang hoạt động hoặc ngừng hoạt động.");
        }
        employee.setIdentityNumber(validateIdentityNumber(employee.getIdentityNumber(), creating));
        if (employee.getStatus() == null || employee.getStatus().isBlank()) {
            throw new FieldValidationException("status", "Vui lòng chọn trạng thái.");
        }
    }

    static String validateIdentityNumber(String identityNumber, boolean required) {
        if (identityNumber == null || identityNumber.isBlank()) {
            if (required) {
                throw new FieldValidationException("identityNumber", "Vui lòng nhập mã định danh (CCCD).");
            }
            return null;
        }
        String trimmedNumber = identityNumber.trim();
        if (!trimmedNumber.matches("[0-9]{12}")) {
            throw new FieldValidationException("identityNumber", "Mã định danh (CCCD) phải gồm đúng 12 chữ số.");
        }
        return trimmedNumber;
    }

    private void validateUniquePhone(Employee employee) throws SQLException {
        Integer excludeEmployeeId = employee.getEmployeeId() > 0 ? employee.getEmployeeId() : null;
        if (employeeDAO.phoneExists(employee.getPhone().trim(), excludeEmployeeId)) {
            throw new FieldValidationException("phone", "Số điện thoại đã tồn tại.");
        }
    }

    private boolean employeeCodeExists(Connection connection, int companyId, String employeeCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Employees WHERE CompanyId = ? AND EmployeeCode = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, employeeCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    static void validatePersonal(Employee employee) {
        checkLength("fullName", employee.getFullName(), 150);
        checkLength("phone", employee.getPhone(), 30);
        checkLength("identityIssuedPlace", employee.getIdentityIssuedPlace(), 200);
        checkLength("permanentAddress", employee.getPermanentAddress(), 500);
        checkLength("hometown", employee.getHometown(), 200);
        checkLength("major", employee.getMajor(), 200);
        if (employee.getDateOfBirth() != null && employee.getDateOfBirth().toLocalDate().isAfter(java.time.LocalDate.now()))
            throw new FieldValidationException("dateOfBirth", "Ngày sinh không được ở tương lai.");
        if (employee.getIdentityIssuedDate() != null && (employee.getIdentityIssuedDate().toLocalDate().isAfter(java.time.LocalDate.now())
                || employee.getDateOfBirth() != null && employee.getIdentityIssuedDate().before(employee.getDateOfBirth())))
            throw new FieldValidationException("identityIssuedDate", "Ngày cấp căn cước phải sau ngày sinh và không được ở tương lai.");
        if (!java.util.Set.of("Male", "Female", "Other").contains(employee.getGender() == null ? "" : employee.getGender()))
            throw new FieldValidationException("gender", "Vui lòng chọn giới tính hợp lệ.");
        if (employee.getEmail() != null && (employee.getEmail().length() > 150 || !employee.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
            throw new FieldValidationException("email", "Vui lòng nhập địa chỉ email hợp lệ.");
        if (employee.getIntendedContractType() != null && !java.util.Set.of("PROBATION", "FIXED_TERM", "INDEFINITE").contains(employee.getIntendedContractType()))
            throw new FieldValidationException("intendedContractType", "Vui lòng chọn loại hợp đồng dự kiến hợp lệ.");
    }

    private static void checkLength(String field, String value, int maximum) {
        if (value != null && value.length() > maximum) throw new FieldValidationException(field, "Độ dài tối đa là " + maximum + ".");
    }

}
