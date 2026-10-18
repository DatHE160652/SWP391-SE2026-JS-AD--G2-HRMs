package com.hrms.service;

import com.hrms.model.Employee;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class EmployeeServiceTest {
    private final EmployeeService service = new EmployeeService();

    @Test
    public void preservesLeadingZeroesInIdentityNumber() {
        assertEquals("001234567890", EmployeeService.validateIdentityNumber(" 001234567890 ", true));
    }

    @Test
    public void requiresIdentityNumberForNewEmployees() {
        for (String value : new String[]{null, "", "   "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> EmployeeService.validateIdentityNumber(value, true));
        }
    }

    @Test
    public void allowsExistingEmployeesWithoutIdentityNumber() {
        assertNull(EmployeeService.validateIdentityNumber(null, false));
    }

    @Test
    public void rejectsIdentityNumbersOutsideCccdFormat() {
        for (String value : new String[]{"00123456789", "0012345678901", "00123456789A",
                "001234 67890", "００１２３４５６７８９０", "00123456789-"}) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeService.validateIdentityNumber(value, true));
            assertEquals("Mã định danh (CCCD) phải gồm đúng 12 chữ số.", exception.getMessage());
        }
    }

    @Test
    public void preservesExistingCompanyCodeFormats() {
        for (String code : new String[]{"00123", "nv-HN/2026.001", "Sales 001", "aBc_42"}) {
            assertEquals(code, EmployeeService.validateManualEmployeeCode("  " + code + "  "));
        }
    }

    @Test
    public void acceptsFullDatabaseCodeLength() {
        String code = "A".repeat(50);
        assertEquals(code, EmployeeService.validateManualEmployeeCode(code));
        assertThrows(IllegalArgumentException.class,
                () -> EmployeeService.validateManualEmployeeCode(code + "B"));
    }

    @Test
    public void requiresCodeWhenModeIsOmitted() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createEmployee(new Employee(), 1, null));
        assertEquals("Employee code is required when entering manually.", exception.getMessage());
    }

    @Test
    public void rejectsBlankManualCodes() {
        for (String code : new String[]{"", "   ", "\t"}) {
            Employee employee = new Employee();
            employee.setEmployeeCode(code);
            assertThrows(IllegalArgumentException.class,
                    () -> service.createEmployee(employee, 1, "MANUAL"));
        }
    }

    @Test
    public void automaticModeDoesNotRequireManualCode() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createEmployee(new Employee(), 1, "AUTO"));
        assertEquals("Full name is required.", exception.getMessage());
    }

    @Test
    public void rejectsUnknownModeEvenWithCode() {
        Employee employee = new Employee();
        employee.setEmployeeCode("existing-001");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createEmployee(employee, 1, "OTHER"));
        assertEquals("Employee code mode must be MANUAL or AUTO.", exception.getMessage());
    }
}
