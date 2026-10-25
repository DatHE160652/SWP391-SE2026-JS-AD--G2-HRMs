package com.hrms.service;

import com.hrms.dao.EmployeeLifecycleDAO;
import com.hrms.dao.OrganizationTransaction;
import com.hrms.dao.UserDAO;
import com.hrms.model.EmploymentContract;
import com.hrms.model.User;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmployeeLifecycleService {
    private final EmployeeLifecycleDAO dao = new EmployeeLifecycleDAO();

    public Map<String,Object> ownCompensation(TenantPrincipal principal) throws SQLException {
        try (Connection connection=DBConnection.getConnection()) {
            requireOwnEmployee(connection,principal);
            return Map.of("history",dao.compensationHistory(connection,principal.getCompanyId(),principal.getEmployeeId()),
                    "allowances",dao.currentAllowance(connection,principal.getCompanyId(),principal.getEmployeeId()));
        }
    }

    public Map<String,Object> ownWorkspace(TenantPrincipal principal) throws SQLException {
        try (Connection connection=DBConnection.getConnection()) {
            requireOwnEmployee(connection,principal);
            return new com.hrms.dao.PersonalWorkspaceDAO().read(connection,principal.getCompanyId(),principal.getEmployeeId(),
                    "MANAGER".equals(principal.getRoleCode()));
        }
    }

    public Map<String,Object> teamMember(TenantPrincipal principal,int employeeId) throws SQLException {
        try (Connection connection=DBConnection.getConnection()) {
            requireOwnEmployee(connection,principal);
            if (!"MANAGER".equals(principal.getRoleCode()))
                throw new SecurityException("Chỉ quản lý trực tiếp được xem thông tin công việc này.");
            return new com.hrms.dao.PersonalWorkspaceDAO().teamMember(connection,principal.getCompanyId(),principal.getEmployeeId(),employeeId);
        }
    }

    public Object personalView(TenantPrincipal principal,String resource,Integer employeeId) throws SQLException {
        try(Connection connection=DBConnection.getConnection()) {
            requireOwnEmployee(connection,principal);
            var personal=new com.hrms.dao.PersonalWorkspaceDAO();
            return switch(resource) {
                case "organization-chart" -> {
                    var graph=new java.util.LinkedHashMap<String,Object>(personal.organization(connection,principal.getCompanyId(),principal.getEmployeeId(),"MANAGER".equals(principal.getRoleCode())));
                    graph.put("selfEmployeeId",principal.getEmployeeId());
                    yield graph;
                }
                case "contracts" -> personal.ownContracts(connection,principal.getCompanyId(),principal.getEmployeeId());
                case "colleague" -> {
                    if(employeeId==null || employeeId<=0) throw new IllegalArgumentException("Vui lòng chọn nhân viên hợp lệ.");
                    yield personal.colleague(connection,principal.getCompanyId(),principal.getEmployeeId(),employeeId,"MANAGER".equals(principal.getRoleCode()));
                }
                default -> throw new IllegalArgumentException("Không tìm thấy nội dung cá nhân.");
            };
        }
    }

    private void requireOwnEmployee(Connection connection,TenantPrincipal principal) throws SQLException {
        if (principal == null || principal.getCompanyId() == null || principal.getEmployeeId() == null
                || principal.isMustChangePassword()
                || !Set.of("EMPLOYEE","MANAGER","HR","COMPANY_ADMIN").contains(principal.getRoleCode()))
            throw new SecurityException("Vui lòng đăng nhập bằng tài khoản được liên kết với hồ sơ nhân viên.");
        requireCurrentActor(connection,principal);
        if (dao.query(connection,"SELECT 1 FROM Employees WHERE CompanyId=? AND EmployeeId=? AND UserId=?",
                principal.getCompanyId(),principal.getEmployeeId(),principal.getUserId()).isEmpty())
            throw new SecurityException("Không tìm thấy hồ sơ cá nhân của tài khoản trong workspace này.");
    }

    public void requireEditor(TenantPrincipal principal) throws SQLException {
        if (principal == null || principal.getCompanyId() == null || principal.isMustChangePassword()
                || !principal.isCompanyAdministrator())
            throw new SecurityException("Vui lòng đăng nhập bằng tài khoản nhân sự hoặc quản trị viên công ty.");
        try (Connection connection = DBConnection.getConnection()) {
            requireCurrentActor(connection, principal);
        }
    }

    private void requireCurrentActor(Connection connection, TenantPrincipal principal) throws SQLException {
        if (dao.query(connection, """
                SELECT 1 FROM Users u JOIN Roles r ON r.RoleId=u.RoleId
                WHERE u.CompanyId=? AND u.UserId=? AND u.IsActive=1 AND u.IsLocked=0 AND r.RoleCode=?
                """ + " AND " + com.hrms.dao.ContractAccessPolicy.ALLOWED, principal.getCompanyId(), principal.getUserId(), principal.getRoleCode()).isEmpty())
            throw new SecurityException("Quyền truy cập đã bị thu hồi hoặc vai trò đã thay đổi. Vui lòng đăng nhập lại.");
    }

    public List<Map<String, Object>> read(TenantPrincipal principal, String resource, Integer employeeId) throws SQLException {
        requireEditor(principal);
        int companyId = principal.getCompanyId();
        try (Connection connection = DBConnection.getConnection()) {
            if (employeeId != null && dao.query(connection, "SELECT 1 FROM Employees WHERE CompanyId=? AND EmployeeId=?", companyId, employeeId).isEmpty())
                throw new SecurityException("Không tìm thấy nhân viên trong công ty của bạn.");
            return switch (resource) {
                case "contracts" -> dao.query(connection, """
                        SELECT c.*,e.FullName,e.EmployeeCode,
                            CONVERT(varchar(33),COALESCE(c.UpdatedAt,c.CreatedAt),126) AS Version,
                            CASE WHEN c.Status='ACTIVE' AND c.EndDate<CONVERT(date,SYSDATETIME()) THEN 'EXPIRED' ELSE c.Status END AS DisplayStatus,
                            CASE WHEN c.Status='ACTIVE' AND c.EndDate BETWEEN CONVERT(date,SYSDATETIME()) AND DATEADD(day,30,CONVERT(date,SYSDATETIME())) THEN 1 ELSE 0 END AS Expiring
                        FROM EmploymentContracts c JOIN Employees e ON e.CompanyId=c.CompanyId AND e.EmployeeId=c.EmployeeId
                        WHERE c.CompanyId=? AND (? IS NULL OR c.EmployeeId=?) ORDER BY c.StartDate DESC,c.ContractId DESC
                        """, companyId, employeeId, employeeId);
                case "allowance" -> dao.currentAllowance(connection,companyId,employeeId);
                case "compensation" -> dao.compensationHistory(connection,companyId,employeeId);
                case "accounts" -> dao.query(connection, """
                        SELECT e.EmployeeId,e.FullName,e.EmployeeCode,e.Status AS EmploymentStatus,u.UserId,u.Email,e.Email AS ContactEmail,
                            CASE WHEN policy.ContractAccessAllowed=1 THEN u.IsActive ELSE CAST(0 AS bit) END AS IsActive,
                            u.IsActive AS AdministrativeIsActive,policy.ContractAccessAllowed,u.IsLocked,
                            u.MustChangePassword,r.RoleCode,p.PositionTitle,
                            CASE WHEN UPPER(LTRIM(RTRIM(level.LevelCode))) IN ('EMP','EMPLOYEE')
                                OR UPPER(LTRIM(RTRIM(level.LevelName)))='EMPLOYEE' THEN 'EMPLOYEE'
                                WHEN UPPER(LTRIM(RTRIM(level.LevelCode)))='MANAGER'
                                OR UPPER(LTRIM(RTRIM(level.LevelName)))='MANAGER' OR p.CanManageEmployees=1
                                THEN 'MANAGER' ELSE 'EMPLOYEE' END AS AssignmentRoleCode,
                            CONVERT(varchar(33),u.UpdatedAt,126) AS Version
                        FROM Employees e LEFT JOIN Users u ON u.CompanyId=e.CompanyId AND u.UserId=e.UserId
                        CROSS APPLY (SELECT CAST(CASE WHEN
                        """ + com.hrms.dao.ContractAccessPolicy.ALLOWED + """
                            THEN 1 ELSE 0 END AS bit) AS ContractAccessAllowed) policy
                        LEFT JOIN Roles r ON r.RoleId=u.RoleId
                        OUTER APPLY (SELECT TOP(1) a.PositionId FROM EmployeePositionAssignments a
                            WHERE a.CompanyId=e.CompanyId AND a.EmployeeId=e.EmployeeId
                                AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                                AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME()))
                            ORDER BY a.EffectiveFrom DESC,a.AssignmentId DESC) currentAssignment
                        LEFT JOIN Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=currentAssignment.PositionId
                        LEFT JOIN ResponsibilityLevels level ON level.CompanyId=p.CompanyId AND level.ResponsibilityLevelId=p.ResponsibilityLevelId
                        WHERE e.CompanyId=? ORDER BY e.FullName
                        """, companyId);
                case "organization-admins" -> dao.query(connection, """
                        SELECT u.UserId,u.DisplayName,r.RoleCode,c.CompanyName,e.EmployeeId
                        FROM Users u JOIN Roles r ON r.RoleId=u.RoleId
                        JOIN Companies c ON c.CompanyId=u.CompanyId
                        OUTER APPLY (SELECT TOP(1) employee.EmployeeId FROM Employees employee
                            WHERE employee.CompanyId=u.CompanyId AND employee.UserId=u.UserId
                            ORDER BY employee.EmployeeId) e
                        WHERE u.CompanyId=? AND u.IsActive=1 AND u.IsLocked=0
                            AND r.RoleCode IN ('COMPANY_ADMIN','HR')
                        ORDER BY u.DisplayName,u.UserId
                        """, companyId);
                case "organization" -> dao.query(connection, """
                        SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,a.AssignmentId,a.DepartmentId,a.PositionId,a.ManagerId,
                            d.DepartmentName,p.PositionTitle,
                            CASE WHEN UPPER(LTRIM(RTRIM(r.LevelCode))) IN ('EMP','EMPLOYEE')
                                OR UPPER(LTRIM(RTRIM(r.LevelName)))='EMPLOYEE' THEN CAST(0 AS bit)
                                ELSE p.CanManageEmployees END AS CanManageEmployees,
                            r.LevelName,manager.FullName AS ManagerName
                        FROM EmployeePositionAssignments a
                        JOIN Employees e ON e.CompanyId=a.CompanyId AND e.EmployeeId=a.EmployeeId
                        JOIN Departments d ON d.CompanyId=a.CompanyId AND d.DepartmentId=a.DepartmentId
                        JOIN Positions p ON p.CompanyId=a.CompanyId AND p.PositionId=a.PositionId
                        LEFT JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                        LEFT JOIN Employees manager ON manager.CompanyId=a.CompanyId AND manager.EmployeeId=a.ManagerId
                        WHERE a.CompanyId=? AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                            AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME())) ORDER BY d.DepartmentName,e.FullName
                        """, companyId);
                case "changes" -> dao.query(connection, """
                        SELECT c.*,e.FullName,p.PositionTitle,m.FullName AS NewManagerName,
                            CAST(CASE WHEN c.Status='APPROVED' AND c.EffectiveDate<=CONVERT(date,SYSDATETIME()) THEN 1 ELSE 0 END AS bit) AS CanApply
                        FROM EmployeePositionChanges c
                        JOIN Employees e ON e.CompanyId=c.CompanyId AND e.EmployeeId=c.EmployeeId
                        JOIN Positions p ON p.CompanyId=c.CompanyId AND p.PositionId=c.NewPositionId
                        LEFT JOIN Employees m ON m.CompanyId=c.CompanyId AND m.EmployeeId=c.NewManagerId
                        WHERE c.CompanyId=? AND (? IS NULL OR c.EmployeeId=?) ORDER BY c.CreatedAt DESC
                        """, companyId, employeeId, employeeId);
                default -> throw new IllegalArgumentException("Không tìm thấy dữ liệu nhân sự được yêu cầu.");
            };
        }
    }

    public long saveContract(TenantPrincipal principal, EmploymentContract contract) throws SQLException {
        requireEditor(principal);
        validateContract(contract);
        return OrganizationTransaction.execute(principal.getCompanyId(), connection -> {
            requireCurrentActor(connection, principal);
            var employee = dao.employee(connection, principal.getCompanyId(), contract.employeeId());
            if (!"Active".equals(employee.get("Status"))) throw new IllegalArgumentException("Chỉ tạo hợp đồng cho nhân viên đang hoạt động.");
            long contractId;
            if (contract.contractId() == null) {
                contractId = dao.insert(connection, """
                        INSERT INTO EmploymentContracts(CompanyId,EmployeeId,ContractCode,ContractType,StartDate,EndDate,GrossSalary,Notes,Status,CreatedByUserId,TravelAllowance)
                        VALUES(?,?,?,?,?,?,?,?,'DRAFT',?,?)
                        """, principal.getCompanyId(), contract.employeeId(), contract.contractCode().strip(), contract.contractType(),
                        contract.startDate(), contract.endDate(), contract.grossSalary(), contract.notes(), principal.getUserId(), contract.travelAllowance());
            } else {
                contractId = contract.contractId();
                if (dao.execute(connection, """
                        UPDATE EmploymentContracts SET ContractCode=?,ContractType=?,StartDate=?,EndDate=?,GrossSalary=?,Notes=?,TravelAllowance=?,UpdatedAt=SYSUTCDATETIME()
                        WHERE CompanyId=? AND ContractId=? AND EmployeeId=? AND Status='DRAFT'
                            AND CONVERT(varchar(33),COALESCE(UpdatedAt,CreatedAt),126)=?
                        """, contract.contractCode().strip(), contract.contractType(), contract.startDate(), contract.endDate(), contract.grossSalary(), contract.notes(),
                        contract.travelAllowance(), principal.getCompanyId(), contractId, contract.employeeId(), contract.expectedUpdatedAt()) != 1)
                    throw new IllegalArgumentException("Bản nháp đã thay đổi hoặc hợp đồng đã ký. Vui lòng tải lại trước khi chỉnh sửa.");
            }
            OrganizationTransaction.audit(connection, principal.getCompanyId(), principal.getUserId(), contract.employeeId(), "SAVE_CONTRACT", null, "ContractId="+contractId);
            return contractId;
        });
    }

    public long recordSignedContract(TenantPrincipal principal, EmploymentContract contract, LocalDate signedDate,
            boolean synchronizeSalary) throws SQLException {
        requireEditor(principal);
        validateContract(contract);
        if (contract.contractId() != null)
            throw new IllegalArgumentException("Hợp đồng đã có trên hệ thống phải sử dụng bản ghi hiện có, không ghi nhận lại.");
        return OrganizationTransaction.execute(principal.getCompanyId(), connection -> {
            requireCurrentActor(connection, principal);
            var employee = dao.employee(connection, principal.getCompanyId(), contract.employeeId());
            LocalDate today = LocalDate.parse(dao.query(connection, "SELECT CONVERT(date,SYSDATETIME()) AS Today").get(0).get("Today").toString());
            if (signedDate == null || signedDate.isAfter(today))
                throw new FieldValidationException("signedDate", "Nhập ngày ký thực tế, không được sau hôm nay.");
            if (contract.endDate() != null && signedDate.isAfter(contract.endDate()))
                throw new FieldValidationException("signedDate", "Ngày ký không được sau ngày kết thúc hợp đồng.");
            if (contract.startDate().isAfter(today))
                throw new FieldValidationException("startDate", "Luồng này dành cho hợp đồng đã bắt đầu. Hợp đồng mới dùng Tạo hợp đồng.");
            boolean expired = contract.endDate() != null && contract.endDate().isBefore(today);
            if (!expired && !"Active".equals(employee.get("Status")))
                throw new IllegalArgumentException("Chỉ ghi nhận hợp đồng còn hiệu lực cho nhân viên đang làm việc.");
            if (expired && synchronizeSalary)
                throw new IllegalArgumentException("Hợp đồng đã hết hạn chỉ được ghi nhận lịch sử, không đồng bộ lương hiện tại.");
            if (!dao.query(connection, "SELECT 1 FROM EmploymentContracts WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND ContractCode=?",
                    principal.getCompanyId(), contract.contractCode().strip()).isEmpty())
                throw new FieldValidationException("contractCode", "Mã hợp đồng đã có trên hệ thống. Vui lòng sử dụng bản ghi hiện có.");
            requireNoContractOverlap(connection, principal.getCompanyId(), contract.employeeId(), null, contract.startDate(), contract.endDate());
            long contractId = dao.insert(connection, """
                    INSERT INTO EmploymentContracts(CompanyId,EmployeeId,ContractCode,ContractType,StartDate,EndDate,
                        GrossSalary,Notes,Status,SignedDate,CreatedByUserId,TravelAllowance)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                    """, principal.getCompanyId(), contract.employeeId(), contract.contractCode().strip(), contract.contractType(),
                    contract.startDate(), contract.endDate(), contract.grossSalary(), contract.notes(), expired ? "EXPIRED" : "ACTIVE",
                    signedDate, principal.getUserId(), contract.travelAllowance());
            if (synchronizeSalary)
                new com.hrms.dao.ContractCompensationDAO().synchronize(connection, principal.getCompanyId(), contract.employeeId(),
                        contractId, contract.contractCode().strip(), contract.startDate(), contract.grossSalary(), contract.travelAllowance(), principal.getUserId());
            OrganizationTransaction.audit(connection, principal.getCompanyId(), principal.getUserId(), contract.employeeId(),
                    "RECORD_SIGNED_CONTRACT", null, "ContractId="+contractId+"; "+(synchronizeSalary ? "Đồng bộ lương theo hợp đồng" : "Giữ nguyên lịch sử lương"));
            return contractId;
        });
    }

    private void requireNoContractOverlap(Connection connection, int companyId, int employeeId, Long excludedContractId,
            LocalDate startDate, LocalDate endDate) throws SQLException {
        if (!dao.query(connection, """
                SELECT 1 FROM EmploymentContracts WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND EmployeeId=? AND (? IS NULL OR ContractId<>?) AND Status IN ('ACTIVE','EXPIRED','TERMINATED')
                    AND StartDate<=COALESCE(CONVERT(date,?),CONVERT(date,'99991231'))
                    AND COALESCE(TerminatedDate,EndDate,CONVERT(date,'99991231'))>=?
                """, companyId, employeeId, excludedContractId, excludedContractId, endDate, startDate).isEmpty())
            throw new IllegalArgumentException("Thời hạn hợp đồng bị trùng với hợp đồng đã ký. Vui lòng kiểm tra bản ghi hiện có.");
    }

    static void validateContract(EmploymentContract contract) {
        if (contract.employeeId() <= 0 || contract.contractCode() == null || contract.contractCode().isBlank() || contract.contractCode().length() > 60)
            throw new IllegalArgumentException("Vui lòng chọn nhân viên và nhập mã hợp đồng không quá 60 ký tự.");
        if (!Set.of("PROBATION", "FIXED_TERM", "INDEFINITE").contains(contract.contractType() == null ? "" : contract.contractType()))
            throw new IllegalArgumentException("Vui lòng chọn loại hợp đồng hợp lệ.");
        if (contract.startDate() == null || contract.endDate() != null && contract.endDate().isBefore(contract.startDate()))
            throw new IllegalArgumentException("Ngày kết thúc hợp đồng không được trước ngày bắt đầu.");
        if (!"INDEFINITE".equals(contract.contractType()) && contract.endDate() == null)
            throw new IllegalArgumentException("Loại hợp đồng này bắt buộc có ngày kết thúc.");
        if (contract.grossSalary() == null || contract.grossSalary().signum() < 0 || contract.grossSalary().scale()>2
                || contract.grossSalary().compareTo(new BigDecimal("9999999999999999.99"))>0)
            throw new IllegalArgumentException("Nhập lương trước khấu trừ không âm và tối đa 2 chữ số thập phân.");
        if (contract.notes() != null && contract.notes().length()>1000) throw new IllegalArgumentException("Ghi chú không được vượt quá 1.000 ký tự.");
        com.hrms.dao.OrganizationAllowanceDAO.validate(contract.travelAllowance(), "travelAllowance");
    }

    public void transitionContract(TenantPrincipal principal, long contractId, String action, String version, String reason) throws SQLException {
        requireEditor(principal);
        OrganizationTransaction.execute(principal.getCompanyId(), connection -> {
            requireCurrentActor(connection, principal);
            var records = dao.query(connection, """
                    SELECT *,CONVERT(varchar(33),COALESCE(UpdatedAt,CreatedAt),126) AS Version
                    FROM EmploymentContracts WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND ContractId=?
                    """, principal.getCompanyId(), contractId);
            if (records.isEmpty() || !java.util.Objects.equals(records.get(0).get("Version"), version))
                throw new IllegalArgumentException("Hợp đồng đã thay đổi. Vui lòng tải lại trang.");
            var contract = records.get(0);
            int employeeId = ((Number) contract.get("EmployeeId")).intValue();
            String status = (String) contract.get("Status");
            if ("activate".equals(action)) {
                if (!"DRAFT".equals(status)) throw new IllegalArgumentException("Chỉ được kích hoạt hợp đồng ở trạng thái bản nháp.");
                if (!"Active".equals(dao.employee(connection, principal.getCompanyId(), employeeId).get("Status")))
                    throw new IllegalArgumentException("Nhân viên phải đang hoạt động.");
                LocalDate end = contract.get("EndDate") == null ? null : LocalDate.parse(contract.get("EndDate").toString());
                if (end != null && end.isBefore(LocalDate.now())) throw new IllegalArgumentException("Không thể kích hoạt hợp đồng đã hết hạn.");
                requireNoContractOverlap(connection, principal.getCompanyId(), employeeId, contractId,
                        LocalDate.parse(contract.get("StartDate").toString()), end);
                dao.execute(connection, "UPDATE EmploymentContracts SET Status='ACTIVE',SignedDate=CONVERT(date,SYSDATETIME()),UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND ContractId=?", principal.getCompanyId(), contractId);
                new com.hrms.dao.ContractCompensationDAO().synchronize(connection,principal.getCompanyId(),employeeId,contractId,
                        (String)contract.get("ContractCode"),LocalDate.parse(contract.get("StartDate").toString()),
                        (BigDecimal)contract.get("GrossSalary"),(BigDecimal)contract.get("TravelAllowance"),principal.getUserId());
            } else if ("terminate".equals(action)) {
                if (!"ACTIVE".equals(status) || LocalDate.parse(contract.get("StartDate").toString()).isAfter(LocalDate.now()))
                    throw new IllegalArgumentException("Chỉ được chấm dứt hợp đồng đã kích hoạt và đã đến ngày hiệu lực.");
                if (contract.get("EndDate") != null && LocalDate.parse(contract.get("EndDate").toString()).isBefore(LocalDate.now()))
                    throw new IllegalArgumentException("Hợp đồng đã hết hạn.");
                requireReason(reason);
                dao.execute(connection, "UPDATE EmploymentContracts SET Status='TERMINATED',TerminatedDate=CONVERT(date,SYSDATETIME()),TerminationReason=?,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND ContractId=?", reason, principal.getCompanyId(), contractId);
            } else throw new IllegalArgumentException("Thao tác hợp đồng không hợp lệ.");
            OrganizationTransaction.audit(connection, principal.getCompanyId(), principal.getUserId(), employeeId, "CONTRACT_"+action.toUpperCase(java.util.Locale.ROOT), status, "ContractId="+contractId);
            return null;
        });
    }

    public String account(TenantPrincipal principal, int employeeId, String action, String email, String roleCode) throws SQLException {
        requireEditor(principal);
        if (!principal.isCompanyAdministrator()) throw new SecurityException("Chỉ quản trị viên công ty được quản lý tài khoản nhân viên.");
        if ("role".equals(action)) throw new FieldValidationException("roleCode","Vai trò Employee/Manager được đồng bộ khi phân công phòng ban và chức danh, không phân quyền riêng tại đây.");
        if (!Set.of("create","reset","lock","unlock","revoke","role").contains(action == null ? "" : action)) throw new IllegalArgumentException("Vui lòng chọn thao tác tài khoản hợp lệ.");
        return OrganizationTransaction.execute(principal.getCompanyId(), connection -> {
            requireCurrentActor(connection, principal);
            var employee = dao.employee(connection, principal.getCompanyId(), employeeId);
            Integer userId = (Integer) employee.get("UserId");
            String temporaryPassword = null;
            if (userId != null && userId.equals(principal.getUserId())) throw new IllegalArgumentException("Vui lòng dùng cài đặt bảo mật cá nhân để thay đổi quyền truy cập của chính mình.");
            if ("create".equals(action)) {
                temporaryPassword=EmployeeService.validateIdentityNumber((String)employee.get("IdentityNumber"),true);
            } else if ("reset".equals(action)) {
                byte[] entropy = new byte[24];
                new SecureRandom().nextBytes(entropy);
                temporaryPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
            }
            if (Set.of("create","unlock","reset","role").contains(action) && !"Active".equals(employee.get("Status")))
                throw new IllegalArgumentException("Chỉ cấp quyền truy cập cho nhân viên đang hoạt động.");
            if ("unlock".equals(action) && userId != null && dao.query(connection,
                    "SELECT 1 FROM Users u WHERE u.CompanyId=? AND u.UserId=? AND " + com.hrms.dao.ContractAccessPolicy.ALLOWED,
                    principal.getCompanyId(),userId).isEmpty())
                throw new IllegalArgumentException("Tài khoản đang ngừng hoạt động do hợp đồng kết thúc. Cần kích hoạt hợp đồng mới và đến ngày hiệu lực trước khi mở tài khoản.");
            if ("create".equals(action)) {
                if (userId != null) throw new IllegalArgumentException("Nhân viên đã có tài khoản truy cập.");
                String assignedRole=new com.hrms.dao.OrganizationRoleDAO().roleForEmployee(connection,principal.getCompanyId(),employeeId);
                if (roleCode != null && !roleCode.isBlank() && !assignedRole.equals(roleCode))
                    throw new FieldValidationException("roleCode","Vai trò tài khoản phải theo chức danh đang được phân công.");
                String contactEmail=(String)employee.get("Email");
                if (email != null && !email.isBlank() && (contactEmail == null || !email.strip().equalsIgnoreCase(contactEmail.strip())))
                    throw new FieldValidationException("email","Email đăng nhập phải là email liên hệ đã lưu trong hồ sơ nhân viên.");
                var identity=new PortalIdentityGenerator().fromContactEmail(connection,contactEmail);
                User user = new User();
                user.setCompanyId(principal.getCompanyId());
                user.setUsername(identity.username());
                user.setEmail(identity.email());
                user.setDisplayName((String) employee.get("FullName"));
                user.setPassword(new BCryptPasswordHasher().hash(temporaryPassword));
                userId = new UserDAO().createEmployeeAccount(connection, user);
                dao.execute(connection,"UPDATE Users SET MustChangePassword=1,ActivatedAt=NULL,PasswordChangedAt=NULL WHERE CompanyId=? AND UserId=?",principal.getCompanyId(),userId);
                dao.execute(connection, "UPDATE Employees SET UserId=?,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=? AND UserId IS NULL", userId, principal.getCompanyId(), employeeId);
                new com.hrms.dao.OrganizationRoleDAO().synchronize(connection,principal.getCompanyId(),employeeId,(Integer)employee.get("PositionId"),principal.getUserId());
            } else {
                if (userId == null) throw new IllegalArgumentException("Nhân viên chưa có tài khoản truy cập.");
                switch (action) {
                    case "lock" -> dao.execute(connection, "UPDATE Users SET IsLocked=1,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND UserId=?", principal.getCompanyId(), userId);
                    case "unlock" -> dao.execute(connection, "UPDATE Users SET IsLocked=0,IsActive=1,LockedUntil=NULL,FailedLoginAttempts=0,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND UserId=?", principal.getCompanyId(), userId);
                    case "revoke" -> dao.execute(connection, "UPDATE Users SET IsActive=0,IsLocked=1,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND UserId=?", principal.getCompanyId(), userId);
                    case "reset" -> dao.execute(connection, "UPDATE Users SET Password=?,MustChangePassword=1,ActivatedAt=NULL,PasswordChangedAt=NULL,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND UserId=?", new BCryptPasswordHasher().hash(temporaryPassword), principal.getCompanyId(), userId);
                    default -> throw new IllegalArgumentException("Thao tác tài khoản không hợp lệ.");
                }
            }
            OrganizationTransaction.audit(connection, principal.getCompanyId(), principal.getUserId(), employeeId, "PORTAL_"+action.toUpperCase(java.util.Locale.ROOT), null, "UserId="+userId);
            return temporaryPassword;
        });
    }


    public void terminate(TenantPrincipal principal, int employeeId, String reason) throws SQLException {
        requireEditor(principal);
        requireReason(reason);
        OrganizationTransaction.execute(principal.getCompanyId(), connection -> {
            requireCurrentActor(connection, principal);
            var employee = dao.employee(connection, principal.getCompanyId(), employeeId);
            if (!"Active".equals(employee.get("Status"))) throw new IllegalArgumentException("Chỉ thực hiện ngừng làm việc cho nhân viên đang hoạt động.");
            if (java.util.Objects.equals(employee.get("UserId"), principal.getUserId())) throw new IllegalArgumentException("Bạn không được tự thực hiện ngừng làm việc cho chính mình.");
            if (!dao.query(connection, "SELECT 1 FROM Employees WHERE CompanyId=? AND ManagerId=?", principal.getCompanyId(), employeeId).isEmpty())
                throw new IllegalArgumentException("Vui lòng phân công lại cấp dưới trước khi ghi nhận ngừng làm việc.");
            if (!dao.query(connection, "SELECT 1 FROM EmployeePositionChanges WHERE CompanyId=? AND EmployeeId=? AND Status IN ('PENDING','APPROVED')", principal.getCompanyId(), employeeId).isEmpty())
                throw new IllegalArgumentException("Vui lòng xử lý đề xuất chức danh đang chờ trước khi ghi nhận ngừng làm việc.");
            if (!dao.query(connection,"SELECT 1 FROM EmployeePositionAssignments WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL AND EffectiveFrom>CONVERT(date,SYSDATETIME())",principal.getCompanyId(),employeeId).isEmpty())
                throw new IllegalArgumentException("Vui lòng xử lý phân công tương lai trước khi ghi nhận ngừng làm việc.");
            dao.execute(connection, """
                    UPDATE EmployeePositionAssignments SET EffectiveTo=CONVERT(date,SYSDATETIME())
                    WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL
                    """, principal.getCompanyId(), employeeId);
            dao.execute(connection,"""
                    UPDATE EmploymentContracts SET Status=CASE WHEN Status='ACTIVE' AND EndDate<CONVERT(date,SYSDATETIME()) THEN 'EXPIRED'
                        WHEN StartDate>CONVERT(date,SYSDATETIME()) OR Status='DRAFT' THEN 'CANCELLED' ELSE 'TERMINATED' END,
                        TerminatedDate=CASE WHEN Status='ACTIVE' AND EndDate<CONVERT(date,SYSDATETIME()) THEN TerminatedDate ELSE CONVERT(date,SYSDATETIME()) END,
                        TerminationReason=CASE WHEN Status='ACTIVE' AND EndDate<CONVERT(date,SYSDATETIME()) THEN TerminationReason ELSE ? END,
                        UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=? AND Status IN ('ACTIVE','DRAFT')
                    """,reason,principal.getCompanyId(),employeeId);
            dao.execute(connection, "UPDATE Users SET IsActive=0,IsLocked=1,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND UserId=?", principal.getCompanyId(), employee.get("UserId"));
            dao.execute(connection, "UPDATE Employees SET Status='Inactive',EndDate=CONVERT(date,SYSDATETIME()),TerminationReason=?,DepartmentId=NULL,PositionId=NULL,ManagerId=NULL,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=?", reason, principal.getCompanyId(), employeeId);
            OrganizationTransaction.audit(connection, principal.getCompanyId(), principal.getUserId(), employeeId, "TERMINATE_EMPLOYEE", "Active", reason);
            return null;
        });
    }

    public Map<String, Object> dashboard(TenantPrincipal principal) throws SQLException {
        requireEditor(principal);
        int companyId = principal.getCompanyId();
        try (Connection connection = DBConnection.getConnection()) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("employees", dao.query(connection, "SELECT COUNT(*) AS Total,SUM(CASE WHEN Status='Active' THEN 1 ELSE 0 END) AS Active FROM Employees WHERE CompanyId=?", companyId));
            result.put("departments", dao.query(connection, "SELECT COALESCE(d.DepartmentName,N'Chưa phân công') AS Label,COUNT(*) AS Total FROM Employees e LEFT JOIN Departments d ON d.CompanyId=e.CompanyId AND d.DepartmentId=e.DepartmentId WHERE e.CompanyId=? GROUP BY d.DepartmentName", companyId));
            result.put("gender", dao.query(connection, "SELECT COALESCE(Gender,N'Chưa có dữ liệu') AS Label,COUNT(*) AS Total FROM Employees WHERE CompanyId=? GROUP BY Gender", companyId));
            result.put("age", dao.query(connection, "SELECT CASE WHEN DateOfBirth IS NULL THEN N'Chưa có dữ liệu' WHEN DATEADD(year,30,DateOfBirth)>CONVERT(date,SYSDATETIME()) THEN N'Dưới 30' WHEN DATEADD(year,40,DateOfBirth)>CONVERT(date,SYSDATETIME()) THEN N'30–39' ELSE N'40+' END AS Label,COUNT(*) AS Total FROM Employees WHERE CompanyId=? GROUP BY CASE WHEN DateOfBirth IS NULL THEN N'Chưa có dữ liệu' WHEN DATEADD(year,30,DateOfBirth)>CONVERT(date,SYSDATETIME()) THEN N'Dưới 30' WHEN DATEADD(year,40,DateOfBirth)>CONVERT(date,SYSDATETIME()) THEN N'30–39' ELSE N'40+' END", companyId));
            result.put("tenure", dao.query(connection, "SELECT CASE WHEN DATEDIFF(day,FirstAssignment,CONVERT(date,SYSDATETIME()))<365 THEN N'Dưới 1 năm' WHEN DATEDIFF(day,FirstAssignment,CONVERT(date,SYSDATETIME()))<1096 THEN N'1–3 năm' ELSE N'3+ năm' END AS Label,COUNT(*) AS Total FROM (SELECT MIN(EffectiveFrom) FirstAssignment FROM EmployeePositionAssignments WHERE CompanyId=? GROUP BY EmployeeId) history GROUP BY CASE WHEN DATEDIFF(day,FirstAssignment,CONVERT(date,SYSDATETIME()))<365 THEN N'Dưới 1 năm' WHEN DATEDIFF(day,FirstAssignment,CONVERT(date,SYSDATETIME()))<1096 THEN N'1–3 năm' ELSE N'3+ năm' END", companyId));
            result.put("contracts", dao.query(connection, "SELECT CASE WHEN Status='ACTIVE' AND EndDate<CONVERT(date,SYSDATETIME()) THEN 'EXPIRED' ELSE Status END AS Label,COUNT(*) AS Total FROM EmploymentContracts WHERE CompanyId=? GROUP BY CASE WHEN Status='ACTIVE' AND EndDate<CONVERT(date,SYSDATETIME()) THEN 'EXPIRED' ELSE Status END", companyId));
            result.put("expiring", dao.query(connection, "SELECT COUNT(*) AS Total FROM EmploymentContracts WHERE CompanyId=? AND Status='ACTIVE' AND EndDate BETWEEN CONVERT(date,SYSDATETIME()) AND DATEADD(day,90,CONVERT(date,SYSDATETIME()))", companyId));
            if (!dao.query(connection, "SELECT 1 FROM sys.tables WHERE object_id=OBJECT_ID('dbo.Attendances')").isEmpty())
                result.put("attendance", dao.query(connection, "SELECT COUNT(DISTINCT EmployeeId) AS Total FROM Attendances WHERE CompanyId=? AND AttendanceDate=CONVERT(date,SYSDATETIME()) AND CheckInTime IS NOT NULL", companyId));
            if (!dao.query(connection, "SELECT 1 FROM sys.tables WHERE object_id=OBJECT_ID('dbo.LeaveRequests')").isEmpty()) {
                result.put("pendingLeave", dao.query(connection, "SELECT COUNT(*) AS Total FROM LeaveRequests WHERE CompanyId=? AND Status='Pending'", companyId));
                result.put("recentLeave", dao.query(connection, "SELECT TOP(5) e.FullName,l.StartDate,l.EndDate,l.Status FROM LeaveRequests l JOIN Employees e ON e.CompanyId=l.CompanyId AND e.EmployeeId=l.EmployeeId WHERE l.CompanyId=? ORDER BY l.CreatedAt DESC", companyId));
            }
            return result;
        }
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length()>500) throw new IllegalArgumentException("A reason of 1–500 characters is required.");
    }

    public long requestPositionChange(TenantPrincipal principal,int employeeId,int positionId,LocalDate effectiveDate,
            String changeType,String managerAction,Integer managerId,String reason) throws SQLException {
        requireEditor(principal);
        requireReason(reason);
        if (!Set.of("TRANSFER","PROMOTION","DEMOTION","CORRECTION","MANAGER_CHANGE").contains(changeType == null ? "" : changeType)) throw new IllegalArgumentException("Vui lòng chọn loại thay đổi hợp lệ.");
        if (effectiveDate == null || effectiveDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Ngày hiệu lực không được ở quá khứ.");
        return OrganizationTransaction.execute(principal.getCompanyId(),connection -> {
            requireCurrentActor(connection,principal);
            var employee=dao.employee(connection,principal.getCompanyId(),employeeId);
            if (!"Active".equals(employee.get("Status")) || employee.get("PositionId")==null) throw new IllegalArgumentException("Nhân viên phải đang hoạt động và có phân công hiện tại.");
            if (!dao.query(connection,"SELECT 1 FROM EmployeePositionChanges WHERE CompanyId=? AND EmployeeId=? AND Status IN ('PENDING','APPROVED')",principal.getCompanyId(),employeeId).isEmpty()) throw new IllegalArgumentException("Vui lòng xử lý đề xuất đang chờ trước.");
            var target=dao.query(connection,"SELECT DepartmentId FROM Positions WHERE CompanyId=? AND PositionId=?",principal.getCompanyId(),positionId);
            if(target.isEmpty()) throw new IllegalArgumentException("Không tìm thấy chức danh trong công ty của bạn.");
            int departmentId=((Number)target.get(0).get("DepartmentId")).intValue();
            validatePositionRanks(connection,principal.getCompanyId(),(Integer)employee.get("PositionId"),positionId,changeType);
            new com.hrms.dao.OrganizationAssignmentDAO().validateSelection(connection,principal.getCompanyId(),departmentId,positionId);
            new com.hrms.dao.PositionOccupancyDAO().validate(connection,principal.getCompanyId(),employeeId,positionId,effectiveDate,null);
            Integer resolvedManager=switch(managerAction == null ? "" : managerAction) {
                case "KEEP" -> (Integer)employee.get("ManagerId");
                case "ASSIGN" -> { if(managerId==null) throw new IllegalArgumentException("Vui lòng chọn quản lý trực tiếp."); yield managerId; }
                case "REMOVE" -> null;
                default -> throw new IllegalArgumentException("Vui lòng chọn giữ nguyên, gán mới hoặc gỡ quản lý trực tiếp.");
            };
            if("MANAGER_CHANGE".equals(changeType) && java.util.Objects.equals(resolvedManager,employee.get("ManagerId")))
                throw new com.hrms.service.FieldValidationException("managerId","Vui lòng chọn quản lý khác hoặc đề xuất gỡ quản lý hiện tại.");
            if("MANAGER_CHANGE".equals(changeType))
                new com.hrms.dao.EmployeeAssignmentDAO().requireManagerSnapshot(connection,principal.getCompanyId(),employeeId,(Integer)employee.get("ManagerId"));
            new com.hrms.dao.EmployeeAssignmentDAO().validateManagerAssignment(connection,principal.getCompanyId(),employeeId,resolvedManager,departmentId,positionId);
            if(!dao.query(connection,"SELECT 1 FROM EmployeePositionAssignments WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL AND EffectiveFrom>=?",principal.getCompanyId(),employeeId,effectiveDate).isEmpty()) throw new IllegalArgumentException("Ngày hiệu lực phải sau ngày bắt đầu phân công hiện tại.");
            long changeId=dao.insert(connection,"""
                    INSERT INTO EmployeePositionChanges(CompanyId,EmployeeId,OldPositionId,NewPositionId,ChangeType,EffectiveDate,NewManagerId,OldManagerId,Reason,CreatedByUserId,Status)
                    VALUES(?,?,?,?,?,?,?,?,?,?,'PENDING')
                    """,principal.getCompanyId(),employeeId,employee.get("PositionId"),positionId,changeType,effectiveDate,resolvedManager,employee.get("ManagerId"),reason,principal.getUserId());
            if("MANAGER_CHANGE".equals(changeType))
                OrganizationTransaction.audit(connection,principal.getCompanyId(),principal.getUserId(),employeeId,"REQUEST_MANAGER_CHANGE",
                        "ManagerId="+employee.get("ManagerId"),"ManagerId="+resolvedManager+"\nLý do: "+reason);
            else OrganizationTransaction.audit(connection,principal.getCompanyId(),principal.getUserId(),employeeId,"REQUEST_POSITION_CHANGE",null,"ChangeId="+changeId);
            return changeId;
        });
    }

    public void transitionPositionChange(TenantPrincipal principal,long changeId,String action) throws SQLException {
        requireEditor(principal);
        OrganizationTransaction.execute(principal.getCompanyId(),connection -> {
            requireCurrentActor(connection,principal);
            var records=dao.query(connection,"SELECT * FROM EmployeePositionChanges WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND PositionChangeId=?",principal.getCompanyId(),changeId);
            if(records.isEmpty()) throw new IllegalArgumentException("Không tìm thấy đề xuất thay đổi chức danh trong công ty.");
            var change=records.get(0);
            int employeeId=((Number)change.get("EmployeeId")).intValue();
            if("APPROVE".equals(action) || "REJECT".equals(action)) {
                if(!"PENDING".equals(change.get("Status"))) throw new IllegalArgumentException("Chỉ được duyệt đề xuất đang chờ phê duyệt.");
                dao.execute(connection,"UPDATE EmployeePositionChanges SET Status=?,ApprovedByUserId=?,ApprovedAt=SYSUTCDATETIME() WHERE CompanyId=? AND PositionChangeId=?",
                        "APPROVE".equals(action)?"APPROVED":"REJECTED",principal.getUserId(),principal.getCompanyId(),changeId);
            } else if("APPLY".equals(action)) {
                if(!"APPROVED".equals(change.get("Status"))) throw new IllegalArgumentException("Cần phê duyệt đề xuất trước khi áp dụng.");
                LocalDate today=LocalDate.parse(dao.query(connection,"SELECT CONVERT(date,SYSDATETIME()) AS Today").get(0).get("Today").toString());
                LocalDate effectiveDate=LocalDate.parse(change.get("EffectiveDate").toString());
                if(effectiveDate.isAfter(today))
                    throw new IllegalArgumentException("Đề xuất chưa đến ngày hiệu lực. Chỉ được áp dụng từ ngày "+effectiveDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))+". Vui lòng quay lại áp dụng khi đến hạn.");
                new com.hrms.dao.PositionOccupancyDAO().validate(connection,principal.getCompanyId(),employeeId,
                        ((Number)change.get("NewPositionId")).intValue(),effectiveDate,null);
                var employee=dao.employee(connection,principal.getCompanyId(),employeeId);
                if(!"Active".equals(employee.get("Status"))) throw new IllegalArgumentException("Nhân viên phải còn đang hoạt động.");
                if("MANAGER_CHANGE".equals(change.get("ChangeType")) && !java.util.Objects.equals(employee.get("ManagerId"),change.get("OldManagerId")))
                    throw new IllegalArgumentException("Quản lý hiện tại đã thay đổi. Vui lòng tạo đề xuất mới.");
                if("MANAGER_CHANGE".equals(change.get("ChangeType")))
                    new com.hrms.dao.EmployeeAssignmentDAO().requireManagerSnapshot(connection,principal.getCompanyId(),employeeId,(Integer)employee.get("ManagerId"));
                if(!dao.query(connection,"""
                        SELECT 1 FROM PayrollPeriods WHERE CompanyId=? AND Status='Approved'
                            AND ([Year]>YEAR(?) OR ([Year]=YEAR(?) AND [Month]>=MONTH(?)))
                        """,principal.getCompanyId(),effectiveDate,effectiveDate,effectiveDate).isEmpty())
                    throw new IllegalArgumentException("Không thể áp dụng thay đổi vào kỳ lương đã được duyệt.");
                var target=dao.query(connection,"SELECT DepartmentId FROM Positions WHERE CompanyId=? AND PositionId=?",principal.getCompanyId(),change.get("NewPositionId"));
                if(target.isEmpty()) throw new IllegalArgumentException("Chức danh đích không còn tồn tại.");
                int departmentId=((Number)target.get(0).get("DepartmentId")).intValue();
                if (!java.util.Objects.equals(employee.get("PositionId"),change.get("OldPositionId"))) throw new IllegalArgumentException("Chức danh hiện tại đã thay đổi. Vui lòng tạo đề xuất mới.");
                validatePositionRanks(connection,principal.getCompanyId(),(Integer)change.get("OldPositionId"),(Integer)change.get("NewPositionId"),(String)change.get("ChangeType"));
                if (change.get("NewManagerId") != null && dao.query(connection,"""
                        SELECT 1 FROM EmployeePositionAssignments WHERE CompanyId=? AND EmployeeId=? AND DepartmentId=?
                            AND EffectiveFrom<=? AND (EffectiveTo IS NULL OR EffectiveTo>?)
                        """,principal.getCompanyId(),change.get("NewManagerId"),departmentId,change.get("EffectiveDate"),change.get("EffectiveDate")).isEmpty())
                    throw new IllegalArgumentException("Quản lý đã chọn không đủ điều kiện tại ngày hiệu lực của phân công.");
                new com.hrms.dao.OrganizationAssignmentDAO().validateSelection(connection,principal.getCompanyId(),departmentId,((Number)change.get("NewPositionId")).intValue());
                var assignments=new com.hrms.dao.EmployeeAssignmentDAO();
                assignments.validateManagerAssignment(connection,principal.getCompanyId(),employeeId,(Integer)change.get("NewManagerId"),departmentId,(Integer)change.get("NewPositionId"));
                assignments.validateDirectReports(connection,principal.getCompanyId(),employeeId,departmentId,(Integer)change.get("NewPositionId"));
                try(java.sql.CallableStatement statement=connection.prepareCall("{call dbo.usp_ApplyApprovedPositionChange(?,?)}")) {
                    statement.setLong(1,changeId); statement.setDate(2,java.sql.Date.valueOf(today)); statement.execute();
                }
                assignments.updateManager(connection,principal.getCompanyId(),employeeId,(Integer)change.get("NewManagerId"));
                if("MANAGER_CHANGE".equals(change.get("ChangeType")))
                    OrganizationTransaction.recordManagerChange(connection,principal.getCompanyId(),principal.getUserId(),employeeId,
                            (Integer)change.get("OldManagerId"),(Integer)change.get("NewManagerId"),(String)change.get("Reason"));
            } else throw new IllegalArgumentException("Vui lòng chọn duyệt, từ chối hoặc áp dụng.");
            if("MANAGER_CHANGE".equals(change.get("ChangeType")))
                OrganizationTransaction.audit(connection,principal.getCompanyId(),principal.getUserId(),employeeId,"MANAGER_CHANGE_"+action,
                        "ManagerId="+change.get("OldManagerId"),"ManagerId="+change.get("NewManagerId"));
            else OrganizationTransaction.audit(connection,principal.getCompanyId(),principal.getUserId(),employeeId,"POSITION_"+action,String.valueOf(change.get("Status")),"ChangeId="+changeId);
            return null;
        });
    }

    public void adjustCompensation(TenantPrincipal principal, int employeeId, LocalDate effectiveDate,
            com.hrms.model.EmployeeCompensation compensation, String reason) throws SQLException {
        requireEditor(principal);
        requireReason(reason);
        EmployeeLifecycleDAO.validateCompensation(compensation);
        if (compensation == null || !compensation.provided() || effectiveDate == null) throw new IllegalArgumentException("Vui lòng nhập lương và ngày hiệu lực.");
        OrganizationTransaction.execute(principal.getCompanyId(), connection -> {
            requireCurrentActor(connection,principal);
            if (!"Active".equals(dao.employee(connection,principal.getCompanyId(),employeeId).get("Status"))) throw new IllegalArgumentException("Nhân viên phải đang hoạt động.");
            if (!dao.query(connection,"""
                    SELECT 1 FROM PayrollPeriods WHERE CompanyId=? AND Status='Approved'
                        AND ([Year]>YEAR(?) OR ([Year]=YEAR(?) AND [Month]>=MONTH(?)))
                    """,principal.getCompanyId(),effectiveDate,effectiveDate,effectiveDate).isEmpty()) throw new IllegalArgumentException("Không thể điều chỉnh vì kỳ lương đã được duyệt.");
            if (!dao.query(connection,"SELECT 1 FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND EffectiveFrom>=?",
                    principal.getCompanyId(),employeeId,effectiveDate).isEmpty()) throw new IllegalArgumentException("Chọn ngày sau kỳ lương mới nhất. Lịch sử lương hiện có được giữ nguyên.");
            dao.execute(connection,"UPDATE EmployeeCompensationHistories SET EffectiveTo=? WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL",effectiveDate,principal.getCompanyId(),employeeId);
            dao.execute(connection,"""
                    INSERT INTO EmployeeCompensationHistories(CompanyId,EmployeeId,BaseSalary,PositionAllowance,TravelAllowance,
                        EffectiveFrom,Reason,CreatedByUserId) VALUES(?,?,?,?,?,?,?,?)
                    """,principal.getCompanyId(),employeeId,compensation.grossSalary(),new com.hrms.dao.OrganizationAllowanceDAO().employeeAllowance(connection,principal.getCompanyId(),employeeId,effectiveDate),compensation.travelAllowance(),
                    effectiveDate,reason,principal.getUserId());
            OrganizationTransaction.audit(connection,principal.getCompanyId(),principal.getUserId(),employeeId,"ADJUST_COMPENSATION",null,reason);
            return null;
        });
    }

    private void validatePositionRanks(Connection connection,int companyId,int oldPosition,int newPosition,String type) throws SQLException {
        if("MANAGER_CHANGE".equals(type)) {
            if(oldPosition!=newPosition) throw new IllegalArgumentException("Đề xuất đổi quản lý phải giữ nguyên chức danh hiện tại.");
            return;
        }
        if (oldPosition==newPosition) throw new IllegalArgumentException("Vui lòng chọn chức danh khác.");
        var ranks=dao.query(connection,"""
                SELECT previousLevel.RankOrder AS PreviousRank,nextLevel.RankOrder AS NextRank FROM Positions previous
                JOIN ResponsibilityLevels previousLevel ON previousLevel.CompanyId=previous.CompanyId AND previousLevel.ResponsibilityLevelId=previous.ResponsibilityLevelId
                JOIN Positions nextPosition ON nextPosition.CompanyId=previous.CompanyId AND nextPosition.PositionId=?
                JOIN ResponsibilityLevels nextLevel ON nextLevel.CompanyId=nextPosition.CompanyId AND nextLevel.ResponsibilityLevelId=nextPosition.ResponsibilityLevelId
                WHERE previous.CompanyId=? AND previous.PositionId=?
                """,newPosition,companyId,oldPosition);
        if(ranks.isEmpty()) throw new IllegalArgumentException("Chưa cấu hình đầy đủ cấp trách nhiệm của chức danh.");
        int previous=((Number)ranks.get(0).get("PreviousRank")).intValue();
        int next=((Number)ranks.get(0).get("NextRank")).intValue();
        if("PROMOTION".equals(type) && next<=previous || "DEMOTION".equals(type) && next>=previous)
            throw new IllegalArgumentException("Thăng chức phải tăng thứ bậc trách nhiệm; hạ chức phải giảm thứ bậc trách nhiệm.");
    }
}
