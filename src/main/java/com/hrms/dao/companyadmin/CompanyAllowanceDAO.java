package com.hrms.dao.companyadmin;

import com.hrms.model.ContractAllowance;
import com.hrms.service.FieldValidationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class CompanyAllowanceDAO {
    private final EmployeeLifecycleDAO dao = new EmployeeLifecycleDAO();
    private static boolean initialized;

    public static synchronized void ensureSchema(Connection connection) throws SQLException {
        if (initialized) return;
        try (var stream = CompanyAllowanceDAO.class.getResourceAsStream("/db/company-contract-allowances.sql")) {
            if (stream == null) throw new SQLException("Missing allowance migration.");
            try (var statement = connection.createStatement()) {
                statement.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
            initialized = true;
        } catch (IOException | SQLException e) {
            try (var statement = connection.createStatement()) { statement.execute("IF @@TRANCOUNT>0 ROLLBACK TRANSACTION"); }
            throw new SQLException("Cannot initialize company allowances.", e);
        }
    }

    public List<Map<String,Object>> types(Connection connection, int companyId) throws SQLException {
        // Preserve contracts imported through the previous single-allowance format.
        if (!dao.query(connection, """
                SELECT 1 FROM EmploymentContracts c WHERE c.CompanyId=? AND c.TravelAllowance>0
                AND NOT EXISTS(SELECT 1 FROM ContractAllowances a WHERE a.CompanyId=c.CompanyId AND a.ContractId=c.ContractId)
                """, companyId).isEmpty()) {
            if (dao.query(connection,"SELECT 1 FROM CompanyAllowanceTypes WHERE CompanyId=? AND LegacyTravel=1", companyId).isEmpty())
                dao.execute(connection,"INSERT CompanyAllowanceTypes(CompanyId,TypeName,DefaultAmount,LegacyTravel) VALUES(?,N'Phụ cấp đi lại (cũ)',0,1)", companyId);
            dao.execute(connection,"""
                    INSERT ContractAllowances(CompanyId,ContractId,AllowanceTypeId,TypeName,Amount)
                    SELECT c.CompanyId,c.ContractId,t.AllowanceTypeId,t.TypeName,c.TravelAllowance
                    FROM EmploymentContracts c JOIN CompanyAllowanceTypes t ON t.CompanyId=c.CompanyId AND t.LegacyTravel=1
                    WHERE c.CompanyId=? AND c.TravelAllowance>0
                    AND NOT EXISTS(SELECT 1 FROM ContractAllowances a WHERE a.CompanyId=c.CompanyId AND a.ContractId=c.ContractId)
                    """,companyId);
        }
        var types=dao.query(connection,"SELECT AllowanceTypeId,TypeName,IsActive,LegacyTravel FROM CompanyAllowanceTypes WHERE CompanyId=? ORDER BY TypeName,AllowanceTypeId",companyId);
        var rates=dao.query(connection,"SELECT AllowanceTypeId,ResponsibilityLevelId,Amount FROM CompanyAllowanceLevelAmounts WHERE CompanyId=?",companyId);
        for (var type : types) type.put("LevelAmounts",rates.stream().filter(rate -> rate.get("AllowanceTypeId").equals(type.get("AllowanceTypeId"))).toList());
        return types;
    }

    public void saveLevelAmounts(Connection connection,int companyId,int typeId,Map<Integer,BigDecimal> amounts) throws SQLException {
        if (dao.query(connection,"SELECT 1 FROM CompanyAllowanceTypes WHERE CompanyId=? AND AllowanceTypeId=?",companyId,typeId).isEmpty())
            throw new IllegalArgumentException("Không tìm thấy loại phụ cấp trong công ty.");
        var levels=dao.query(connection,"SELECT ResponsibilityLevelId FROM ResponsibilityLevels WHERE CompanyId=? AND IsActive=1",companyId);
        Set<Integer> ids=new HashSet<>();
        for (var level : levels) ids.add(((Number)level.get("ResponsibilityLevelId")).intValue());
        if (!ids.equals(amounts.keySet())) throw new IllegalArgumentException("Vui lòng cấu hình mức phụ cấp cho đầy đủ cấp trách nhiệm đang sử dụng trong công ty.");
        for (var entry : amounts.entrySet()) {
            if (entry.getValue()==null) throw new IllegalArgumentException("Nhập số tiền cho từng cấp trách nhiệm; dùng 0 nếu không áp dụng.");
            OrganizationAllowanceDAO.validate(entry.getValue(),"levelAmount");
            dao.execute(connection,"DELETE FROM CompanyAllowanceLevelAmounts WHERE CompanyId=? AND AllowanceTypeId=? AND ResponsibilityLevelId=?",companyId,typeId,entry.getKey());
            dao.execute(connection,"INSERT CompanyAllowanceLevelAmounts(CompanyId,AllowanceTypeId,ResponsibilityLevelId,Amount) VALUES(?,?,?,?)",companyId,typeId,entry.getKey(),entry.getValue());
        }
    }

    public Map<String,Object> preview(Connection connection,int companyId,int employeeId,java.time.LocalDate date) throws SQLException {
        if (date==null) throw new FieldValidationException("startDate","Vui lòng nhập ngày bắt đầu.");
        if (dao.query(connection,"SELECT 1 FROM Employees WHERE CompanyId=? AND EmployeeId=?",companyId,employeeId).isEmpty())
            throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty.");
        var levels=dao.query(connection,"""
                SELECT l.ResponsibilityLevelId,l.LevelName FROM EmployeePositionAssignments a
                JOIN Positions p ON p.CompanyId=a.CompanyId AND p.PositionId=a.PositionId
                JOIN ResponsibilityLevels l ON l.CompanyId=p.CompanyId AND l.ResponsibilityLevelId=p.ResponsibilityLevelId
                WHERE a.CompanyId=? AND a.EmployeeId=? AND a.EffectiveFrom<=? AND (a.EffectiveTo IS NULL OR a.EffectiveTo>?)
                """,companyId,employeeId,date,date);
        if (levels.size()>1) throw new IllegalArgumentException("Nhân viên có nhiều phân công cùng hiệu lực.");
        Integer levelId=levels.isEmpty()?null:((Number)levels.get(0).get("ResponsibilityLevelId")).intValue();
        var amounts=dao.query(connection,"""
                SELECT t.AllowanceTypeId,t.TypeName,r.Amount FROM CompanyAllowanceTypes t
                LEFT JOIN CompanyAllowanceLevelAmounts r ON r.CompanyId=t.CompanyId AND r.AllowanceTypeId=t.AllowanceTypeId AND r.ResponsibilityLevelId=?
                WHERE t.CompanyId=? AND t.IsActive=1 ORDER BY t.TypeName,t.AllowanceTypeId
                """,levelId,companyId);
        var result=new LinkedHashMap<String,Object>();
        result.put("ResponsibilityLevelId",levelId);
        result.put("LevelName",levels.isEmpty()?null:levels.get(0).get("LevelName"));
        result.put("Allowances",amounts);
        String warning=amounts.isEmpty()?"":levelId==null?"Nhân viên chưa có cấp trách nhiệm tại ngày bắt đầu hợp đồng. Vui lòng phân công trước khi tạo hợp đồng.":
                amounts.stream().anyMatch(row -> row.get("Amount")==null)?"Chưa cấu hình đủ mức phụ cấp cho cấp trách nhiệm này.":"";
        result.put("Warning",warning);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ContractAllowance> fixedAllowances(Connection connection,int companyId,int employeeId,java.time.LocalDate date) throws SQLException {
        var policy=preview(connection,companyId,employeeId,date);
        if (!"".equals(policy.get("Warning"))) throw new FieldValidationException("allowances",policy.get("Warning").toString());
        var lines=((List<Map<String,Object>>)policy.get("Allowances")).stream()
                .map(row -> new ContractAllowance(((Number)row.get("AllowanceTypeId")).intValue(),(BigDecimal)row.get("Amount"))).toList();
        total(lines);
        return lines;
    }

    public long saveType(Connection connection,int companyId,Integer id,String name,BigDecimal amount,boolean active) throws SQLException {
        if (name == null || name.isBlank() || name.strip().length()>150)
            throw new FieldValidationException("typeName","Nhập tên loại phụ cấp, tối đa 150 ký tự.");
        if (amount == null) throw new FieldValidationException("defaultAmount","Vui lòng nhập mức phụ cấp mặc định.");
        OrganizationAllowanceDAO.validate(amount,"defaultAmount");
        if (!dao.query(connection,"SELECT 1 FROM CompanyAllowanceTypes WHERE CompanyId=? AND TypeName=? AND AllowanceTypeId<>?",companyId,name.strip(),id==null?0:id).isEmpty())
            throw new FieldValidationException("typeName","Tên loại phụ cấp đã tồn tại trong công ty.");
        if (id == null) return dao.insert(connection,"INSERT CompanyAllowanceTypes(CompanyId,TypeName,DefaultAmount,IsActive) VALUES(?,?,?,?)",companyId,name.strip(),amount,active);
        if (dao.execute(connection,"UPDATE CompanyAllowanceTypes SET TypeName=?,DefaultAmount=?,IsActive=? WHERE CompanyId=? AND AllowanceTypeId=?",name.strip(),amount,active,companyId,id)!=1)
            throw new IllegalArgumentException("Không tìm thấy loại phụ cấp trong công ty.");
        return id;
    }

    public List<Map<String,Object>> lines(Connection connection,int companyId,long contractId) throws SQLException {
        return dao.query(connection,"SELECT AllowanceTypeId,TypeName,Amount FROM ContractAllowances WHERE CompanyId=? AND ContractId=? ORDER BY AllowanceTypeId",companyId,contractId);
    }

    public static BigDecimal total(List<ContractAllowance> lines) {
        if (lines.size()>100) throw new IllegalArgumentException("Tối đa 100 loại phụ cấp trên một hợp đồng.");
        Set<Integer> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var line : lines) {
            if (line == null || line.allowanceTypeId()<=0 || !seen.add(line.allowanceTypeId()))
                throw new IllegalArgumentException("Loại phụ cấp không hợp lệ hoặc bị trùng.");
            if (line.amount()==null) throw new IllegalArgumentException("Vui lòng nhập số tiền cho loại phụ cấp đã chọn.");
            OrganizationAllowanceDAO.validate(line.amount(),"allowances");
            total=total.add(line.amount());
        }
        OrganizationAllowanceDAO.validate(total,"allowances");
        return total;
    }

    public void saveLines(Connection connection,int companyId,long contractId,List<ContractAllowance> lines) throws SQLException {
        if (lines==null) return; // Existing integrations may still send TravelAllowance only.
        total(lines);
        if (dao.query(connection,"SELECT 1 FROM EmploymentContracts WHERE CompanyId=? AND ContractId=?",companyId,contractId).isEmpty())
            throw new IllegalArgumentException("Không tìm thấy hợp đồng trong công ty.");
        var old = lines(connection,companyId,contractId);
        var selected = new ArrayList<Map<String,Object>>();
        for (var line : lines) {
            var types = dao.query(connection,"SELECT TypeName,IsActive FROM CompanyAllowanceTypes WHERE CompanyId=? AND AllowanceTypeId=?",companyId,line.allowanceTypeId());
            var previous=old.stream().filter(row -> ((Number)row.get("AllowanceTypeId")).intValue()==line.allowanceTypeId()).findFirst();
            if (types.isEmpty() || (!Boolean.TRUE.equals(types.get(0).get("IsActive")) && previous.isEmpty()))
                throw new IllegalArgumentException("Loại phụ cấp đã ngừng sử dụng hoặc không thuộc công ty.");
            // A catalog rename does not rewrite the agreed name on an existing contract.
            selected.add(Map.of("id",line.allowanceTypeId(),"name",previous.isPresent()?previous.get().get("TypeName"):types.get(0).get("TypeName"),"amount",line.amount()));
        }
        dao.execute(connection,"DELETE FROM ContractAllowances WHERE CompanyId=? AND ContractId=?",companyId,contractId);
        for (var line : selected)
            dao.execute(connection,"INSERT ContractAllowances(CompanyId,ContractId,AllowanceTypeId,TypeName,Amount) VALUES(?,?,?,?,?)",companyId,contractId,line.get("id"),line.get("name"),line.get("amount"));
    }
}
