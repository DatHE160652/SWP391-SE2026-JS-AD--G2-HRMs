package com.hrms.service.scheduler;

import com.hrms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tác vụ nền thực hiện đối soát chấm công tự động cuối ngày.
 * Tự động phát hiện các nhân viên đang hoạt động nhưng chưa có bản ghi chấm công;
 * nếu có đơn nghỉ phép đã được duyệt thì ghi nhận 'On Leave', ngược lại ghi nhận vắng mặt 'Absent'.
 */
public class EndOfDayReconciliationWorker {
    private static final Logger LOGGER = Logger.getLogger(EndOfDayReconciliationWorker.class.getName());
    public static final ZoneId TIME_ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalTime RUN_TIME = LocalTime.of(23, 59, 0);

    private static ScheduledExecutorService scheduler;

    /**
     * Đối tượng chứa kết quả thống kê sau khi chạy đối soát dữ liệu chấm công.
     */
    public static class ReconciliationResult {
        private final LocalDate date;
        private final int totalActiveChecked;
        private final int onLeaveCreated;
        private final int absentCreated;

        public ReconciliationResult(LocalDate date, int totalActiveChecked, int onLeaveCreated, int absentCreated) {
            this.date = date;
            this.totalActiveChecked = totalActiveChecked;
            this.onLeaveCreated = onLeaveCreated;
            this.absentCreated = absentCreated;
        }

        public LocalDate getDate() {
            return date;
        }

        public int getTotalActiveChecked() {
            return totalActiveChecked;
        }

        public int getOnLeaveCreated() {
            return onLeaveCreated;
        }

        public int getAbsentCreated() {
            return absentCreated;
        }

        public int getTotalCreated() {
            return onLeaveCreated + absentCreated;
        }

        @Override
        public String toString() {
            return "ReconciliationResult{" +
                    "date=" + date +
                    ", totalActiveChecked=" + totalActiveChecked +
                    ", onLeaveCreated=" + onLeaveCreated +
                    ", absentCreated=" + absentCreated +
                    '}';
        }
    }

    private static class MissingEmployeeRecord {
        int companyId;
        int employeeId;
        boolean hasApprovedLeave;
        String leaveTypeName;
    }

    /**
     * Thực thi thuật toán đối soát chấm công cho một ngày cụ thể trên toàn bộ nhân viên đang hoạt động:
     * 1. Tìm các nhân viên có Status = 'Active' nhưng chưa có bản ghi trong bảng Attendances.
     * 2. Kiểm tra nếu có đơn nghỉ phép 'Approved' phủ qua ngày -> ghi nhận 'On Leave' (8.00 giờ).
     * 3. Nếu không có đơn nghỉ phép hợp lệ -> tự động ghi nhận 'Absent' (0.00 giờ).
     */
    public ReconciliationResult reconcileDate(LocalDate targetDate) throws SQLException {
        if (targetDate == null) {
            targetDate = LocalDate.now(TIME_ZONE_VN);
        }

        String selectSql = """
            SELECT e.CompanyId, e.EmployeeId, lr.LeaveRequestId, lt.TypeName
            FROM Employees e
            LEFT JOIN Attendances a 
                ON e.EmployeeId = a.EmployeeId AND a.AttendanceDate = ?
            LEFT JOIN LeaveRequests lr 
                ON e.EmployeeId = lr.EmployeeId 
                AND lr.Status = 'Approved' 
                AND ? BETWEEN lr.StartDate AND lr.EndDate
            LEFT JOIN LeaveTypes lt 
                ON lr.LeaveTypeId = lt.LeaveTypeId
            WHERE e.Status = 'Active' 
                AND a.AttendanceId IS NULL
        """;

        List<MissingEmployeeRecord> missing = new ArrayList<>();
        int activeCount = 0;

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM Employees WHERE Status = 'Active'")) {
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        activeCount = rs.getInt(1);
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setDate(1, Date.valueOf(targetDate));
                stmt.setDate(2, Date.valueOf(targetDate));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        MissingEmployeeRecord rec = new MissingEmployeeRecord();
                        rec.companyId = rs.getInt("CompanyId");
                        rec.employeeId = rs.getInt("EmployeeId");
                        int leaveReqId = rs.getInt("LeaveRequestId");
                        rec.hasApprovedLeave = !rs.wasNull() && leaveReqId > 0;
                        rec.leaveTypeName = rs.getString("TypeName");
                        missing.add(rec);
                    }
                }
            }

            if (missing.isEmpty()) {
                LOGGER.info("JOB-03: No missing attendance records for " + targetDate);
                return new ReconciliationResult(targetDate, activeCount, 0, 0);
            }

            String insertSql = """
                INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, Status, TotalHours, Note, CreatedAt)
                VALUES (?, ?, ?, ?, ?, ?, SYSUTCDATETIME())
            """;

            int onLeaveCount = 0;
            int absentCount = 0;

            conn.setAutoCommit(false);
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (MissingEmployeeRecord rec : missing) {
                    insertStmt.setInt(1, rec.companyId);
                    insertStmt.setInt(2, rec.employeeId);
                    insertStmt.setDate(3, Date.valueOf(targetDate));

                    if (rec.hasApprovedLeave) {
                        insertStmt.setString(4, "On Leave");
                        insertStmt.setBigDecimal(5, BigDecimal.valueOf(8.00));
                        insertStmt.setString(6, "Nghỉ phép đã duyệt" + (rec.leaveTypeName != null ? ": " + rec.leaveTypeName : ""));
                        onLeaveCount++;
                    } else {
                        insertStmt.setString(4, "Absent");
                        insertStmt.setBigDecimal(5, BigDecimal.ZERO);
                        insertStmt.setString(6, "Tự động ghi nhận vắng mặt cuối ngày (JOB-03)");
                        absentCount++;
                    }
                    insertStmt.addBatch();
                }

                insertStmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

            LOGGER.info(String.format("JOB-03 Completed for %s: %d active checked, %d on-leave created, %d absent created",
                    targetDate, activeCount, onLeaveCount, absentCount));

            return new ReconciliationResult(targetDate, activeCount, onLeaveCount, absentCount);
        }
    }

    /**
     * Khởi động tiến trình lập lịch định kỳ, tự động chạy vào mốc 23:59:00 hàng ngày (UTC+7).
     */
    public static synchronized void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HRMs-JOB03-Worker");
            t.setDaemon(true);
            return t;
        });

        long initialDelaySeconds = calculateInitialDelayToNextRun();
        long periodSeconds = TimeUnit.DAYS.toSeconds(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDate today = LocalDate.now(TIME_ZONE_VN);
                new EndOfDayReconciliationWorker().reconcileDate(today);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "JOB-03: Lỗi khi thực thi đối soát chấm công cuối ngày", e);
            }
        }, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);

        LOGGER.info(String.format("JOB-03 Scheduler đã kích hoạt. Lần chạy tiếp theo sau %d giây (lúc 23:59:00 UTC+7).", initialDelaySeconds));
    }

    /**
     * Dừng an toàn bộ lập lịch (Graceful shutdown) khi ứng dụng web ngừng hoạt động.
     */
    public static synchronized void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("JOB-03 Scheduler đã dừng.");
        }
    }

    /**
     * Tính toán khoảng thời gian chờ (giây) từ thời điểm hiện tại đến 23:59:00 gần nhất.
     */
    private static long calculateInitialDelayToNextRun() {
        ZonedDateTime now = ZonedDateTime.now(TIME_ZONE_VN);
        ZonedDateTime nextRun = now.with(RUN_TIME);
        if (now.compareTo(nextRun) >= 0) {
            nextRun = nextRun.plusDays(1);
        }
        return Duration.between(now, nextRun).getSeconds();
    }
}

