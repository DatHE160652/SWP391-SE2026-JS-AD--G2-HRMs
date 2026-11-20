package com.hrms.service.companyadmin;

import com.hrms.service.FieldValidationException;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.Set;

public final class ContractDuration {
    private ContractDuration() {}

    /** The end date is inclusive: 01/01 + 12 months ends on 31/12. */
    public static LocalDate endDate(String type, LocalDate start, String duration, String unit) {
        if (!Set.of("PROBATION", "FIXED_TERM", "INDEFINITE").contains(type == null ? "" : type))
            throw new FieldValidationException("contractType", "Vui lòng chọn loại hợp đồng hợp lệ.");
        if (start == null) throw new FieldValidationException("startDate", "Vui lòng nhập ngày bắt đầu.");
        if ("INDEFINITE".equals(type)) return null;
        if (duration == null || !duration.matches("[1-9][0-9]*"))
            throw new FieldValidationException("duration", "Thời hạn hợp đồng phải là số nguyên lớn hơn 0.");
        if (!Set.of("MONTHS", "YEARS").contains(unit == null ? "" : unit))
            throw new FieldValidationException("durationUnit", "Vui lòng chọn thời hạn theo tháng hoặc năm.");
        try {
            long amount = Long.parseLong(duration);
            LocalDate end = ("YEARS".equals(unit) ? start.plusYears(amount) : start.plusMonths(amount)).minusDays(1);
            if (start.getYear() < 1 || end.getYear() > 9999) throw new DateTimeException("Unsupported date");
            return end;
        } catch (NumberFormatException | DateTimeException | ArithmeticException e) {
            throw new FieldValidationException("duration", "Thời hạn hợp đồng vượt quá giới hạn ngày cho phép.");
        }
    }
}
