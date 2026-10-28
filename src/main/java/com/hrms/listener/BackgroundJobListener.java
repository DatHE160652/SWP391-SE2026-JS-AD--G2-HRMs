package com.hrms.listener;

import com.hrms.service.scheduler.EndOfDayReconciliationWorker;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

/**
 * Listener quản lý vòng đời ứng dụng web:
 * - Tự động khởi chạy các tác vụ nền (scheduler) khi máy chủ web khởi động.
 * - Giải phóng tài nguyên và dừng luồng an toàn khi máy chủ tắt.
 */
@WebListener
public class BackgroundJobListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(BackgroundJobListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("BackgroundJobListener: Khởi động các tiến trình nền...");
        EndOfDayReconciliationWorker.startScheduler();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("BackgroundJobListener: Đang dừng an toàn các tiến trình nền...");
        EndOfDayReconciliationWorker.stopScheduler();
    }
}

