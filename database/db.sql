
CREATE DATABASE HRMsDB;
GO

USE HRMsDB;
GO

-- Bắt buộc cho index trên computed column và filtered index; kết nối ghi dữ liệu
-- cũng phải dùng các SET options này.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

-- ========================================================================
-- PHẦN 1: QUẢN LÝ GÓI CƯỚC NỀN TẢNG VÀ KHÁCH HÀNG DOANH NGHIỆP (SAAS)
-- WF1: Public Portal / WF2: Platform Administration
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 1: Plans (Một cấu hình dịch vụ mặc định, không chọn plan trong MVP)
-- ------------------------------------------------------------------------
CREATE TABLE Plans (
    PlanId INT IDENTITY(1,1) PRIMARY KEY,
    PlanName NVARCHAR(100) NOT NULL UNIQUE,
    MaxEmployees INT NULL,                          -- Legacy HR; không dùng để giới hạn tài khoản
    MaxAccounts INT NOT NULL DEFAULT 20 CHECK (MaxAccounts = 20),
    MaxManagers INT NOT NULL DEFAULT 3 CHECK (MaxManagers = 3),
    Price DECIMAL(18,2) NOT NULL DEFAULT 59000.00 CHECK (Price >= 0),
    BillingCycle NVARCHAR(20) NOT NULL DEFAULT 'MONTHLY'
        CHECK (BillingCycle = 'MONTHLY'),           -- Đơn vị giá tham chiếu, chưa quyết định hết hạn
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_Plans_MaxEmployees CHECK (MaxEmployees IS NULL OR MaxEmployees > 0)
);

-- ------------------------------------------------------------------------
-- BẢNG 2: Companies (Khách hàng doanh nghiệp SME / Workspace)
-- ------------------------------------------------------------------------
CREATE TABLE Companies (
    CompanyId INT IDENTITY(1,1) PRIMARY KEY,
    WorkspaceCode VARCHAR(50) COLLATE Latin1_General_100_BIN2 NOT NULL UNIQUE,
    CompanyName NVARCHAR(200) NOT NULL,
    CompanyNameNormalized NVARCHAR(200) COLLATE Latin1_General_100_CI_AS NOT NULL UNIQUE,
    Region VARCHAR(10) NOT NULL CHECK (Region IN ('NORTH', 'CENTRAL', 'SOUTH')),
    TaxCode VARCHAR(50) NULL,
    ContactEmail VARCHAR(150) NOT NULL,
    ContactPhone VARCHAR(30) NULL,
    Address NVARCHAR(300) NULL,
    IsActive BIT NOT NULL DEFAULT 1,                 -- Company ACTIVE, độc lập với Subscription
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT CK_Companies_Name CHECK (LEN(LTRIM(RTRIM(CompanyName))) > 0),
    -- Backend tạo khóa tên bằng trim + gộp whitespace + lowercase; không bỏ dấu.
    CONSTRAINT CK_Companies_NormalizedName CHECK (LEN(LTRIM(RTRIM(CompanyNameNormalized))) > 0),
    CONSTRAINT CK_Companies_WorkspaceCode CHECK (
        DATALENGTH(WorkspaceCode) BETWEEN 1 AND 50
        AND WorkspaceCode NOT LIKE '%[^a-z0-9-]%'
        AND LEFT(WorkspaceCode, 1) <> '-' AND RIGHT(WorkspaceCode, 1) <> '-'
        AND WorkspaceCode NOT IN ('app', 'admin', 'www', 'api', 'system')
    )
);

-- ------------------------------------------------------------------------
-- BẢNG 3: Subscriptions (Một bản ghi thuê bao/Company trong MVP)
-- ------------------------------------------------------------------------
CREATE TABLE Subscriptions (
    SubscriptionId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL UNIQUE,
    PlanId INT NOT NULL,                             -- Backend luôn dùng cấu hình mặc định
    StartDate DATE NOT NULL,
    EndDate DATE NULL,                               -- Chưa chốt chu kỳ/hết hạn; không tự cộng 30 ngày
    Status NVARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (Status IN ('ACTIVE', 'SUSPENDED')),
    SuspendedAt DATETIME2 NULL,
    SuspendedByUserId INT NULL,
    SuspensionReason NVARCHAR(500) NULL,
    LastActivatedAt DATETIME2 NULL,
    LastActivatedByUserId INT NULL,
    PaymentMethod NVARCHAR(50) NULL,                 -- Legacy; không tích hợp payment trong MVP
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    RowVersion ROWVERSION NOT NULL,
    CONSTRAINT FK_Subscriptions_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_Subscriptions_Plans FOREIGN KEY (PlanId) REFERENCES Plans(PlanId),
    CONSTRAINT CK_Subscription_Dates CHECK (EndDate IS NULL OR EndDate >= StartDate),
    -- Các cột suspend mô tả lần tạm ngưng hiện tại; activate xóa chúng, lịch sử ở AuditLogs.
    CONSTRAINT CK_Subscriptions_Suspension CHECK (
        (Status = 'ACTIVE' AND SuspendedAt IS NULL AND SuspendedByUserId IS NULL AND SuspensionReason IS NULL)
        OR (Status = 'SUSPENDED' AND SuspendedAt IS NOT NULL AND SuspendedByUserId IS NOT NULL
            AND SuspensionReason IS NOT NULL AND LEN(LTRIM(RTRIM(SuspensionReason))) > 0)
    ),
    CONSTRAINT CK_Subscriptions_Activation CHECK (
        (LastActivatedAt IS NULL AND LastActivatedByUserId IS NULL)
        OR (LastActivatedAt IS NOT NULL AND LastActivatedByUserId IS NOT NULL)
    )
);

-- ========================================================================
-- PHẦN 2: PHÂN QUYỀN (RBAC) VÀ BẢO MẬT TÀI KHOẢN NGƯỜI DÙNG
-- Tương thích các giải thuật băm mật khẩu an toàn (BCrypt, Argon2id)
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 4: Roles (Danh mục vai trò chuẩn của nền tảng)
-- ------------------------------------------------------------------------
CREATE TABLE Roles (
    RoleId INT IDENTITY(1,1) PRIMARY KEY,
    RoleCode VARCHAR(50) NOT NULL UNIQUE,            -- SYSTEM_ADMIN, COMPANY_ADMIN, HR, MANAGER, EMPLOYEE
    RoleName NVARCHAR(100) NOT NULL,
    Description NVARCHAR(255) NULL
);

-- ------------------------------------------------------------------------
-- BẢNG 5: Users (Tài khoản định danh người dùng)
-- ------------------------------------------------------------------------
CREATE TABLE Users (
    UserId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NULL,                              -- NULL đối với System Admin cấp platform
    RoleId INT NOT NULL,
    Username VARCHAR(100) NOT NULL UNIQUE,           -- Tên tài khoản
    Email VARCHAR(150) NOT NULL,                     -- Backend validate, trim và lowercase trước khi lưu
    EmailNormalized AS (LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS) PERSISTED,
    DisplayName NVARCHAR(150) NOT NULL,
    AvatarUrl NVARCHAR(500) NULL,
    -- Chỉ lưu salted hash (BCrypt/Argon2id); không lưu plaintext ở bảng/log/email attempt.
    PasswordHash VARCHAR(255) NOT NULL,
    MustChangePassword BIT NOT NULL DEFAULT 1,
    ActivatedAt DATETIME2 NULL,                      -- NULL: chưa hoàn tất kích hoạt
    PasswordChangedAt DATETIME2 NULL,
    RowVersion ROWVERSION NOT NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    IsLocked BIT NOT NULL DEFAULT 0,                 -- Khóa chống tấn công brute-force
    FailedLoginAttempts INT NOT NULL DEFAULT 0,      -- Bộ đếm đăng nhập sai liên tiếp
    LockedUntil DATETIME2 NULL,                      -- Thời gian mở khóa tự động
    LastLoginAt DATETIME2 NULL,                      -- Thời gian truy cập gần nhất
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Users_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleId) REFERENCES Roles(RoleId),
    CONSTRAINT CK_Users_Email CHECK (LEN(LTRIM(RTRIM(Email))) > 0),
    CONSTRAINT CK_Users_DisplayName CHECK (LEN(LTRIM(RTRIM(DisplayName))) > 0),
    CONSTRAINT CK_Users_Activation CHECK (
        (ActivatedAt IS NULL AND MustChangePassword = 1)
        OR (ActivatedAt IS NOT NULL AND MustChangePassword = 0 AND PasswordChangedAt IS NOT NULL)
    )
);

-- Actor FK được tạo sau Users để tránh tham chiếu bảng chưa tồn tại.
ALTER TABLE Subscriptions ADD
    CONSTRAINT FK_Subscriptions_SuspendedBy FOREIGN KEY (SuspendedByUserId) REFERENCES Users(UserId),
    CONSTRAINT FK_Subscriptions_ActivatedBy FOREIGN KEY (LastActivatedByUserId) REFERENCES Users(UserId);

-- ------------------------------------------------------------------------
-- WF1/WF2: RegistrationRequests (Request độc lập, submit chưa tạo tenant)
-- ------------------------------------------------------------------------
CREATE TABLE RegistrationRequests (
    RegistrationRequestId INT IDENTITY(1,1) PRIMARY KEY,
    -- Đúng 5 trường nhập trên form; các cột còn lại là dữ liệu hệ thống.
    FullName NVARCHAR(150) NOT NULL,
    Email VARCHAR(150) NOT NULL,
    Phone VARCHAR(10) COLLATE Latin1_General_100_BIN2 NOT NULL,
    CompanyName NVARCHAR(200) NOT NULL,
    Region VARCHAR(10) NOT NULL CHECK (Region IN ('NORTH', 'CENTRAL', 'SOUTH')),
    EmailNormalized AS (LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS) PERSISTED,
    CompanyNameNormalized NVARCHAR(200) COLLATE Latin1_General_100_CI_AS NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'NEW'
        CHECK (Status IN ('NEW', 'UNDER_REVIEW', 'CONVERTING', 'CONVERTED', 'DELETED')),
    DeletedAt DATETIME2 NULL,
    DeletedByUserId INT NULL,
    DeleteReason NVARCHAR(500) NULL,                  -- Không bắt buộc lý do xóa
    ConvertedCompanyId INT NULL,
    ConvertedAt DATETIME2 NULL,
    ConvertedByUserId INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    RowVersion ROWVERSION NOT NULL,
    CONSTRAINT FK_RegistrationRequests_Company FOREIGN KEY (ConvertedCompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_RegistrationRequests_DeletedBy FOREIGN KEY (DeletedByUserId) REFERENCES Users(UserId),
    CONSTRAINT FK_RegistrationRequests_ConvertedBy FOREIGN KEY (ConvertedByUserId) REFERENCES Users(UserId),
    CONSTRAINT CK_RegistrationRequests_FullName CHECK (LEN(LTRIM(RTRIM(FullName))) > 0),
    CONSTRAINT CK_RegistrationRequests_Email CHECK (LEN(LTRIM(RTRIM(Email))) > 0),
    CONSTRAINT CK_RegistrationRequests_CompanyName CHECK (LEN(LTRIM(RTRIM(CompanyName))) > 0),
    CONSTRAINT CK_RegistrationRequests_NormalizedName CHECK (LEN(LTRIM(RTRIM(CompanyNameNormalized))) > 0),
    CONSTRAINT CK_RegistrationRequests_Phone CHECK (
        DATALENGTH(Phone) = 10 AND LEFT(Phone, 1) = '0' AND Phone NOT LIKE '%[^0-9]%'
    ),
    CONSTRAINT CK_RegistrationRequests_Deletion CHECK (
        (Status = 'DELETED' AND DeletedAt IS NOT NULL AND DeletedByUserId IS NOT NULL)
        OR (Status <> 'DELETED' AND DeletedAt IS NULL AND DeletedByUserId IS NULL AND DeleteReason IS NULL)
    ),
    CONSTRAINT CK_RegistrationRequests_Conversion CHECK (
        (Status = 'CONVERTED' AND ConvertedCompanyId IS NOT NULL AND ConvertedAt IS NOT NULL AND ConvertedByUserId IS NOT NULL)
        OR (Status <> 'CONVERTED' AND ConvertedCompanyId IS NULL AND ConvertedAt IS NULL AND ConvertedByUserId IS NULL)
    )
);
-- CHECK chỉ bảo vệ giá trị cùng một dòng, không bảo vệ chuyển trạng thái/quyền actor.
-- Confirm/soft delete dùng transaction + khóa/conditional UPDATE, kiểm tra affected rows.
-- Confirm tạo Company (kèm WorkspaceCode) + Company Admin + Subscription nguyên tử;
-- chỉ cập nhật CONVERTED sau khi đủ dữ liệu và gửi email sau COMMIT.
-- Quota/role: backend serialize theo Company, tối đa 20 Users, 3 MANAGER, 1 COMPANY_ADMIN.

-- ------------------------------------------------------------------------
-- WF1/WF2: EmailAttempts (Metadata gửi email, KHÔNG phải credential outbox)
-- ------------------------------------------------------------------------
CREATE TABLE EmailAttempts (
    EmailAttemptId BIGINT IDENTITY(1,1) PRIMARY KEY,
    EmailType VARCHAR(30) NOT NULL
        CHECK (EmailType IN ('REQUEST_RECEIPT', 'ACCOUNT_ACTIVATION', 'ACTIVATION_RESEND')),
    RegistrationRequestId INT NULL,
    UserId INT NULL,                                 -- Tài khoản nhận email kích hoạt
    RequestedByUserId INT NULL,                      -- System Admin; NULL với email tiếp nhận
    RecipientEmail VARCHAR(150) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (Status IN ('PENDING', 'ACCEPTED', 'FAILED', 'UNKNOWN')),
    AttemptedAt DATETIME2 NULL,
    CompletedAt DATETIME2 NULL,
    ProviderMessageId VARCHAR(255) NULL,
    ErrorCode VARCHAR(100) NULL,                     -- Chỉ mã lỗi đã làm sạch; không lưu provider payload
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_EmailAttempts_Request FOREIGN KEY (RegistrationRequestId) REFERENCES RegistrationRequests(RegistrationRequestId),
    CONSTRAINT FK_EmailAttempts_User FOREIGN KEY (UserId) REFERENCES Users(UserId),
    CONSTRAINT FK_EmailAttempts_RequestedBy FOREIGN KEY (RequestedByUserId) REFERENCES Users(UserId),
    CONSTRAINT CK_EmailAttempts_Target CHECK (
        (EmailType = 'REQUEST_RECEIPT' AND RegistrationRequestId IS NOT NULL AND UserId IS NULL AND RequestedByUserId IS NULL)
        OR (EmailType IN ('ACCOUNT_ACTIVATION', 'ACTIVATION_RESEND') AND RegistrationRequestId IS NOT NULL
            AND UserId IS NOT NULL AND RequestedByUserId IS NOT NULL)
    ),
    CONSTRAINT CK_EmailAttempts_Result CHECK (
        (Status = 'PENDING' AND AttemptedAt IS NULL AND CompletedAt IS NULL)
        OR (Status IN ('ACCEPTED', 'FAILED', 'UNKNOWN') AND AttemptedAt IS NOT NULL AND CompletedAt IS NOT NULL)
    ),
    CONSTRAINT CK_EmailAttempts_Times CHECK (
        (AttemptedAt IS NULL AND CompletedAt IS NULL)
        OR (AttemptedAt IS NOT NULL AND CompletedAt IS NOT NULL AND CompletedAt >= AttemptedAt)
    )
);
-- ACCEPTED: nhà cung cấp nhận yêu cầu, KHÔNG khẳng định đã đến hộp thư.
-- UNKNOWN: timeout/crash, không xác định kết quả; không retry mật khẩu cũ từ hash.
-- Credential chỉ tồn tại trong bộ nhớ khi gửi sau commit; UC11 sinh mật khẩu mới.

-- ------------------------------------------------------------------------
-- WF1: FAQ công khai; tạo chủ đề/bình luận yêu cầu đăng nhập ở backend
-- ------------------------------------------------------------------------
CREATE TABLE FaqTopics (
    TopicId INT IDENTITY(1,1) PRIMARY KEY,
    Title NVARCHAR(200) NOT NULL,
    Content NVARCHAR(MAX) NOT NULL,
    AuthorUserId INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_FaqTopics_Author FOREIGN KEY (AuthorUserId) REFERENCES Users(UserId),
    CONSTRAINT CK_FaqTopics_Title CHECK (LEN(LTRIM(RTRIM(Title))) > 0),
    CONSTRAINT CK_FaqTopics_Content CHECK (LEN(LTRIM(RTRIM(Content))) > 0)
);

CREATE TABLE FaqComments (
    CommentId BIGINT IDENTITY(1,1) PRIMARY KEY,
    TopicId INT NOT NULL,
    Content NVARCHAR(MAX) NOT NULL,
    AuthorUserId INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_FaqComments_Topic FOREIGN KEY (TopicId) REFERENCES FaqTopics(TopicId),
    CONSTRAINT FK_FaqComments_Author FOREIGN KEY (AuthorUserId) REFERENCES Users(UserId),
    CONSTRAINT CK_FaqComments_Content CHECK (LEN(LTRIM(RTRIM(Content))) > 0)
);

-- ========================================================================
-- PHẦN 3: CƠ CẤU TỔ CHỨC VÀ HỒ SƠ NHÂN SỰ
-- Workflow 2: Employee Management
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 6: Departments (Phòng ban nội bộ theo công ty)
-- ------------------------------------------------------------------------
CREATE TABLE Departments (
    DepartmentId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    DepartmentCode VARCHAR(50) NOT NULL,             -- VD: 'HR', 'TECH', 'SALE'
    DepartmentName NVARCHAR(150) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Departments_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT UQ_Company_DepartmentCode UNIQUE (CompanyId, DepartmentCode)
);

-- ------------------------------------------------------------------------
-- BẢNG 7: Positions (Chức danh / Vị trí công việc)
-- ------------------------------------------------------------------------
CREATE TABLE Positions (
    PositionId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    PositionTitle NVARCHAR(150) NOT NULL,            -- VD: Backend Dev, Senior HR
    Description NVARCHAR(255) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Positions_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT UQ_Company_PositionTitle UNIQUE (CompanyId, PositionTitle)
);

-- ------------------------------------------------------------------------
-- BẢNG 8: Employees (Hồ sơ nhân viên trung tâm)
-- ------------------------------------------------------------------------
CREATE TABLE Employees (
    EmployeeId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    UserId INT NOT NULL UNIQUE,                      -- Ràng buộc 1-1 với tài khoản User
    ManagerId INT NULL,                              -- Tự tham chiếu cấp trên quản lý trực tiếp
    DepartmentId INT NOT NULL,
    PositionId INT NOT NULL,
    EmployeeCode VARCHAR(50) NOT NULL,               -- Mã nhân viên nội bộ (VD: 'EMP-001')
    FullName NVARCHAR(150) NOT NULL,
    Phone VARCHAR(30) NULL,
    Gender NVARCHAR(20) NULL,
    DateOfBirth DATE NULL,
    IdentityNumber VARCHAR(50) NULL,
    EndDate DATE NULL,
    Status NVARCHAR(30) NOT NULL DEFAULT 'Active' 
        CHECK (Status IN ('Active', 'On Leave', 'Inactive')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Employees_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_Employees_Users FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE,
    CONSTRAINT FK_Employees_Manager FOREIGN KEY (ManagerId) REFERENCES Employees(EmployeeId),
    CONSTRAINT FK_Employees_Departments FOREIGN KEY (DepartmentId) REFERENCES Departments(DepartmentId),
    CONSTRAINT FK_Employees_Positions FOREIGN KEY (PositionId) REFERENCES Positions(PositionId),
    CONSTRAINT UQ_Company_EmployeeCode UNIQUE (CompanyId, EmployeeCode)
);

CREATE TABLE ProfileUpdatePeriods (
    PeriodId BIGINT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    Title NVARCHAR(150) NOT NULL,
    StartsAt DATETIME2 NOT NULL,
    EndsAt DATETIME2 NOT NULL,
    ClosedAt DATETIME2 NULL,
    CreatedByUserId INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ProfileUpdatePeriods_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_ProfileUpdatePeriods_Users FOREIGN KEY (CreatedByUserId) REFERENCES Users(UserId),
    CONSTRAINT CK_ProfileUpdatePeriods_Dates CHECK (EndsAt > StartsAt)
);
CREATE INDEX IX_ProfileUpdatePeriods_CompanyDates
    ON ProfileUpdatePeriods (CompanyId, ClosedAt, StartsAt, EndsAt);

-- ========================================================================
-- PHẦN 4: THEO DÕI CHẤM CÔNG HÀNG NGÀY
-- Workflow 3: Attendance Management
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 9: Attendances (Bản ghi chấm công theo ca)
-- ------------------------------------------------------------------------
CREATE TABLE Attendances (
    AttendanceId BIGINT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    EmployeeId INT NOT NULL,
    AttendanceDate DATE NOT NULL,
    CheckInTime TIME(0) NULL,
    CheckOutTime TIME(0) NULL,
    TotalHours DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    Status NVARCHAR(30) NOT NULL DEFAULT 'Absent' 
        CHECK (Status IN ('Present', 'Late', 'Absent', 'On Leave', 'Early Leave')),
    Note NVARCHAR(255) NULL,
    IsManualAdjusted BIT NOT NULL DEFAULT 0,         -- Cờ đánh dấu có điều chỉnh thủ công
    AdjustedByUserId INT NULL,                       -- Tài khoản HR sửa dữ liệu
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Attendances_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_Attendances_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId) ON DELETE CASCADE,
    CONSTRAINT FK_Attendances_AdjustedBy FOREIGN KEY (AdjustedByUserId) REFERENCES Users(UserId),
    -- Đảm bảo mỗi nhân viên chỉ có 1 bản ghi chấm công mỗi ngày
    CONSTRAINT UQ_Employee_AttendanceDate UNIQUE (EmployeeId, AttendanceDate)
);

-- ========================================================================
-- PHẦN 5: QUẢN LÝ NGHỈ PHÉP VÀ XÉT DUYỆT
-- Workflow 4: Leave Management
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 10: LeaveTypes (Danh mục chính sách các loại ngày nghỉ)
-- ------------------------------------------------------------------------
CREATE TABLE LeaveTypes (
    LeaveTypeId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    TypeCode VARCHAR(50) NOT NULL,                   -- ANNUAL, SICK, UNPAID, MATERNITY
    TypeName NVARCHAR(100) NOT NULL,
    DefaultDaysPerYear INT NOT NULL DEFAULT 12,
    IsPaid BIT NOT NULL DEFAULT 1,                   -- Nghỉ có hưởng lương hay không
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_LeaveTypes_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT UQ_Company_LeaveTypeCode UNIQUE (CompanyId, TypeCode)
);

-- ------------------------------------------------------------------------
-- BẢNG 11: LeaveBalances (Hạn mức và số dư ngày phép theo năm)
-- ------------------------------------------------------------------------
CREATE TABLE LeaveBalances (
    BalanceId INT IDENTITY(1,1) PRIMARY KEY,
    EmployeeId INT NOT NULL,
    LeaveTypeId INT NOT NULL,
    Year INT NOT NULL,
    EntitledDays DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- Số ngày được cấp
    UsedDays DECIMAL(5,2) NOT NULL DEFAULT 0.00,     -- Số ngày đã nghỉ
    -- Cột tính toán tự động: Ngày còn lại = Cấp - Đã dùng
    RemainingDays AS (EntitledDays - UsedDays),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_LeaveBalances_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId) ON DELETE CASCADE,
    CONSTRAINT FK_LeaveBalances_LeaveTypes FOREIGN KEY (LeaveTypeId) REFERENCES LeaveTypes(LeaveTypeId),
    CONSTRAINT UQ_Employee_LeaveBalance_Year UNIQUE (EmployeeId, LeaveTypeId, Year)
);

-- ------------------------------------------------------------------------
-- BẢNG 12: LeaveRequests (Đơn xin nghỉ phép và quy trình duyệt)
-- ------------------------------------------------------------------------
CREATE TABLE LeaveRequests (
    LeaveRequestId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    EmployeeId INT NOT NULL,
    LeaveTypeId INT NOT NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    TotalDays DECIMAL(4,1) NOT NULL CHECK (TotalDays > 0),
    Reason NVARCHAR(500) NOT NULL,
    Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' 
        CHECK (Status IN ('Pending', 'Approved', 'Rejected', 'Cancelled')),
    ApproverId INT NULL,                             -- Manager hoặc HR thực hiện duyệt
    ApproverComment NVARCHAR(300) NULL,
    ReviewedAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_LeaveRequests_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_LeaveRequests_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId),
    CONSTRAINT FK_LeaveRequests_LeaveTypes FOREIGN KEY (LeaveTypeId) REFERENCES LeaveTypes(LeaveTypeId),
    CONSTRAINT FK_LeaveRequests_Approvers FOREIGN KEY (ApproverId) REFERENCES Users(UserId),
    CONSTRAINT CK_LeaveRequests_DateRange CHECK (EndDate >= StartDate)
);

-- ========================================================================
-- PHẦN 6: QUẢN LÝ TÍNH LƯƠNG VÀ PHIẾU LƯƠNG
-- Workflow 5: Payroll Management
-- Công thức: NetSalary = BaseSalary + Bonus - Deduction
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 13: PayrollPeriods (Kỳ tính lương hàng tháng)
-- ------------------------------------------------------------------------
CREATE TABLE PayrollPeriods (
    PayrollPeriodId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    PeriodName NVARCHAR(100) NOT NULL,               -- VD: 'Bảng lương Tháng 09/2026'
    Month INT NOT NULL CHECK (Month BETWEEN 1 AND 12),
    Year INT NOT NULL CHECK (Year >= 2000),
    Status NVARCHAR(30) NOT NULL DEFAULT 'Draft' 
        CHECK (Status IN ('Draft', 'Calculated', 'Reviewed', 'Approved')),
    ApprovedByUserId INT NULL,
    ApprovedAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_PayrollPeriods_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT FK_PayrollPeriods_ApprovedBy FOREIGN KEY (ApprovedByUserId) REFERENCES Users(UserId),
    CONSTRAINT UQ_Company_Payroll_Month_Year UNIQUE (CompanyId, Month, Year)
);

-- ------------------------------------------------------------------------
-- BẢNG 14: PayrollAdjustments (Các khoản điều chỉnh lương: Thưởng / Giảm trừ)
-- ------------------------------------------------------------------------
CREATE TABLE PayrollAdjustments (
    AdjustmentId BIGINT IDENTITY(1,1) PRIMARY KEY,
    PayrollPeriodId INT NOT NULL,
    EmployeeId INT NOT NULL,
    Bonus DECIMAL(18,2) NOT NULL DEFAULT 0.00 CHECK (Bonus >= 0),
    Deduction DECIMAL(18,2) NOT NULL DEFAULT 0.00 CHECK (Deduction >= 0),
    Note NVARCHAR(300) NULL,
    CreatedByUserId INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Adjustments_Periods FOREIGN KEY (PayrollPeriodId) REFERENCES PayrollPeriods(PayrollPeriodId) ON DELETE CASCADE,
    CONSTRAINT FK_Adjustments_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId),
    CONSTRAINT FK_Adjustments_Users FOREIGN KEY (CreatedByUserId) REFERENCES Users(UserId),
    CONSTRAINT UQ_Period_Employee_Adjustment UNIQUE (PayrollPeriodId, EmployeeId)
);

-- ------------------------------------------------------------------------
-- BẢNG 15: Payslips (Phiếu lương hoàn chỉnh của nhân viên)
-- ------------------------------------------------------------------------
CREATE TABLE Payslips (
    PayslipId BIGINT IDENTITY(1,1) PRIMARY KEY,
    PayrollPeriodId INT NOT NULL,
    EmployeeId INT NOT NULL,
    BaseSalary DECIMAL(18,2) NOT NULL CHECK (BaseSalary >= 0),
    Bonus DECIMAL(18,2) NOT NULL DEFAULT 0.00 CHECK (Bonus >= 0),
    Deduction DECIMAL(18,2) NOT NULL DEFAULT 0.00 CHECK (Deduction >= 0),
    -- Cột tính toán tự động: NetSalary = BaseSalary + Bonus - Deduction
    NetSalary AS (BaseSalary + Bonus - Deduction),
    GeneratedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Payslips_PayrollPeriods FOREIGN KEY (PayrollPeriodId) REFERENCES PayrollPeriods(PayrollPeriodId),
    CONSTRAINT FK_Payslips_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId),
    CONSTRAINT UQ_Period_Employee_Payslip UNIQUE (PayrollPeriodId, EmployeeId)
);

-- ========================================================================
-- PHẦN 7: NHẬP DỮ LIỆU EXCEL LỚN VÀ NHẬT KÝ KIỂM TOÁN
-- Workflow 6: Bulk Data Import & System Audit
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 16: BulkImportLogs (Lịch sử nạp file Excel hàng loạt)
-- ------------------------------------------------------------------------
CREATE TABLE BulkImportLogs (
    ImportId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    ImportType NVARCHAR(50) NOT NULL 
        CHECK (ImportType IN ('EMPLOYEE_IMPORT', 'ATTENDANCE_IMPORT', 'PAYROLL_ADJUSTMENT_IMPORT')),
    FileName NVARCHAR(255) NOT NULL,
    TotalRows INT NOT NULL DEFAULT 0,
    SuccessRows INT NOT NULL DEFAULT 0,
    FailedRows INT NOT NULL DEFAULT 0,
    ErrorDetails NVARCHAR(MAX) NULL,                 -- Lưu JSON hoặc log lỗi chi tiết từng dòng sai
    ImportedByUserId INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ImportLogs_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT FK_ImportLogs_Users FOREIGN KEY (ImportedByUserId) REFERENCES Users(UserId)
);

-- ------------------------------------------------------------------------
-- BẢNG 17: AuditLogs (Theo dõi hành động can thiệp dữ liệu quan trọng)
-- ------------------------------------------------------------------------
CREATE TABLE AuditLogs (
    LogId BIGINT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NULL,
    UserId INT NOT NULL,
    Action NVARCHAR(100) NOT NULL,                   -- CONFIRM_REQUEST, SOFT_DELETE_REQUEST, SUSPEND_SUBSCRIPTION,
                                                    -- ACTIVATE_SUBSCRIPTION, RESEND_ACTIVATION và nghiệp vụ HR cũ
    TableName NVARCHAR(100) NOT NULL,
    RecordId VARCHAR(100) NULL,
    OldValues NVARCHAR(MAX) NULL,                    -- JSON theo allowlist; không chứa password/hash/email body
    NewValues NVARCHAR(MAX) NULL,                    -- Không serialize toàn bộ Users hoặc credential vào log
    IpAddress VARCHAR(50) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_AuditLogs_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
);
GO

-- ========================================================================
-- PHẦN 8: CHỈ MỤC TỐI ƯU HÓA HIỆU NĂNG (INDEXES)
-- ========================================================================

-- 1. Truy vấn phân quyền và tổ chức
CREATE UNIQUE INDEX UX_Users_EmailNormalized ON Users(EmailNormalized);
CREATE INDEX IX_Users_CompanyId ON Users(CompanyId);
CREATE INDEX IX_Employees_CompanyId ON Employees(CompanyId);
CREATE INDEX IX_Employees_ManagerId ON Employees(ManagerId);
CREATE INDEX IX_Employees_DepartmentId ON Employees(DepartmentId);

-- 2. Truy vấn chấm công theo nhân viên và khoảng thời gian
CREATE INDEX IX_Attendances_Date_Employee ON Attendances(AttendanceDate, EmployeeId);
CREATE INDEX IX_Attendances_Company_Date ON Attendances(CompanyId, AttendanceDate);

-- 3. Truy vấn đơn nghỉ phép đang chờ duyệt (Pending)
CREATE INDEX IX_LeaveRequests_Employee_Status ON LeaveRequests(EmployeeId, Status);
CREATE INDEX IX_LeaveRequests_Company_Status ON LeaveRequests(CompanyId, Status);

-- 4. Truy vấn phiếu lương theo kỳ
CREATE INDEX IX_Payslips_Period ON Payslips(PayrollPeriodId);

-- 5. WF1/WF2: chặn trùng email HOẶC tên công ty giữa mọi trạng thái đang xử lý.
-- Hai index riêng chặn trùng email HOẶC tên giữa cả ba trạng thái đang xử lý.
-- CHECK Status giới hạn năm giá trị; loại CONVERTED/DELETED còn NEW/UNDER_REVIEW/CONVERTING.
CREATE UNIQUE INDEX UX_RegistrationRequests_PendingEmail
    ON RegistrationRequests(EmailNormalized)
    WHERE Status <> 'CONVERTED' AND Status <> 'DELETED';
CREATE UNIQUE INDEX UX_RegistrationRequests_PendingCompanyName
    ON RegistrationRequests(CompanyNameNormalized)
    WHERE Status <> 'CONVERTED' AND Status <> 'DELETED';
CREATE UNIQUE INDEX UX_RegistrationRequests_ConvertedCompany
    ON RegistrationRequests(ConvertedCompanyId) WHERE ConvertedCompanyId IS NOT NULL;
CREATE INDEX IX_RegistrationRequests_Status_CreatedAt ON RegistrationRequests(Status, CreatedAt);
CREATE INDEX IX_EmailAttempts_Request_CreatedAt ON EmailAttempts(RegistrationRequestId, CreatedAt);
CREATE INDEX IX_EmailAttempts_User_CreatedAt ON EmailAttempts(UserId, CreatedAt);
CREATE INDEX IX_FaqComments_Topic_CreatedAt ON FaqComments(TopicId, CreatedAt);
-- Index không thay chính sách đăng ký lại sau DELETED (chưa chốt), cũng không
-- thay kiểm tra/khóa chống trùng xuyên RegistrationRequests, Users và Companies.
GO

-- ========================================================================
-- PHẦN 9: KHỞI TẠO DỮ LIỆU CỐ ĐỊNH (SEED DATA)
-- Giữ 5 role legacy; WF1/WF2 chỉ cấp SYSTEM_ADMIN / COMPANY_ADMIN / MANAGER / EMPLOYEE.
-- HR được giữ cho các bảng/luồng HR cũ, không phải role bổ sung cho phạm vi v1.3.4.
-- ========================================================================

INSERT INTO Roles (RoleCode, RoleName, Description) VALUES
('SYSTEM_ADMIN', N'Quản trị viên nền tảng', N'Xử lý request, provision tenant, quản lý Subscription và xem Account Tree; không xem HR chi tiết'),
('COMPANY_ADMIN', N'Quản trị viên công ty', N'Nhận tài khoản sau confirm, đổi mật khẩu tạm và truy cập workspace theo quyền'),
('HR', N'Quản trị nhân sự', N'Quản lý hồ sơ nhân viên, điều chỉnh chấm công, duyệt phép và tính lương'),
('MANAGER', N'Quản lý trực tiếp', N'Theo dõi chấm công và duyệt đơn nghỉ phép của thành viên trong nhóm'),
('EMPLOYEE', N'Nhân viên', N'Tự chấm công, gửi đơn xin nghỉ phép và tra cứu phiếu lương cá nhân');

-- Một cấu hình mặc định; không seed tenant, User, mật khẩu hoặc gói trial.
INSERT INTO Plans (PlanName, MaxEmployees, MaxAccounts, MaxManagers, Price, BillingCycle, IsActive)
VALUES (N'DEFAULT', NULL, 20, 3, 59000.00, 'MONTHLY', 1);
GO

CREATE TABLE EmployeeDocuments
(
    DocumentId INT IDENTITY PRIMARY KEY,

    EmployeeId INT NOT NULL,

    DocumentName NVARCHAR(255),

    DocumentType NVARCHAR(100),

    FileName NVARCHAR(255),

    FilePath NVARCHAR(500),

    UploadedBy INT,

    UploadedDate DATETIME DEFAULT GETDATE(),

    Status NVARCHAR(30),


    FOREIGN KEY(EmployeeId)
    REFERENCES Employees(EmployeeId)
);
GO

-- ------------------------------------------------------------------------
-- DỮ LIỆU MẪU: Attendances (chỉ thêm khi đã có Company/Employee)
-- ------------------------------------------------------------------------
DECLARE @CompanyId INT;
DECLARE @Emp1 INT;
DECLARE @Emp2 INT;

SELECT TOP (1)
    @CompanyId = CompanyId,
    @Emp1 = EmployeeId
FROM Employees
ORDER BY EmployeeId;

SELECT TOP (1)
    @Emp2 = EmployeeId
FROM Employees
WHERE CompanyId = @CompanyId
  AND EmployeeId <> @Emp1
ORDER BY EmployeeId;

IF @Emp1 IS NOT NULL
BEGIN
    -- Mẫu 2: Đi muộn
    IF NOT EXISTS (
        SELECT 1
        FROM Attendances
        WHERE EmployeeId = @Emp1
          AND AttendanceDate = DATEADD(DAY, -1, CAST(GETDATE() AS DATE))
    )
    BEGIN
        INSERT INTO Attendances (
            CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime,
            TotalHours, Status, Note
        )
        VALUES (
            @CompanyId, @Emp1,
            DATEADD(DAY, -1, CAST(GETDATE() AS DATE)),
            '08:45:00', '17:40:00', 7.92, 'Late',
            N'Đi muộn 15 phút do kẹt xe'
        );
    END;
END;

IF @Emp2 IS NOT NULL
BEGIN
    -- Mẫu 3: Về sớm
    IF NOT EXISTS (
        SELECT 1
        FROM Attendances
        WHERE EmployeeId = @Emp2
          AND AttendanceDate = CAST(GETDATE() AS DATE)
    )
    BEGIN
        INSERT INTO Attendances (
            CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime,
            TotalHours, Status, Note
        )
        VALUES (
            @CompanyId, @Emp2,
            CAST(GETDATE() AS DATE),
            '08:20:00', '16:00:00', 6.67, 'Early Leave',
            N'Xin về sớm việc gia đình'
        );
    END;
END;
GO
