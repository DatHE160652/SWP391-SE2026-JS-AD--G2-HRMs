-- Bootstrap tạo database mới; không chạy lại trên database đang có dữ liệu.
-- Schema chính được hợp nhất với dbver.text ngày 26/10/2026.
-- Database đã có dữ liệu dùng scripts/database-update-20261026.sql và các migration payroll.
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
-- Mật khẩu lưu trực tiếp trong Users.Password (chỉ dùng cho đồ án local/demo)
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
    Username VARCHAR(150) NOT NULL UNIQUE,           -- Định danh nội bộ legacy; không dùng để đăng nhập
    Email VARCHAR(150) COLLATE Latin1_General_100_CI_AS NOT NULL, -- Định danh đăng nhập duy nhất; backend trim/lowercase
    EmailNormalized AS (LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS) PERSISTED,
    DisplayName NVARCHAR(150) NOT NULL,
    AvatarUrl NVARCHAR(500) NULL,
    -- CẢNH BÁO: mật khẩu lưu dạng plaintext, chỉ dùng cho local/demo; không dùng production.
    Password NVARCHAR(255) NOT NULL,
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
    CONSTRAINT CK_Users_Email CHECK (LEN(Email) > 0 AND DATALENGTH(Email) = DATALENGTH(LTRIM(RTRIM(Email)))),
    CONSTRAINT CK_Users_DisplayName CHECK (LEN(LTRIM(RTRIM(DisplayName))) > 0),
    CONSTRAINT CK_Users_Activation CHECK (
        (ActivatedAt IS NULL AND MustChangePassword = 1)
        OR (ActivatedAt IS NOT NULL AND PasswordChangedAt IS NOT NULL)
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
    Email VARCHAR(150) COLLATE Latin1_General_100_CI_AS NOT NULL,
    EmailNormalized AS (LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS) PERSISTED,
    Phone VARCHAR(10) COLLATE Latin1_General_100_BIN2 NOT NULL,
    CompanyName NVARCHAR(200) NOT NULL,
    Region VARCHAR(10) NOT NULL CHECK (Region IN ('NORTH', 'CENTRAL', 'SOUTH')),
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
    CONSTRAINT CK_RegistrationRequests_Email CHECK (LEN(Email) > 0 AND DATALENGTH(Email) = DATALENGTH(LTRIM(RTRIM(Email)))),
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
-- UNKNOWN: timeout/crash, không xác định kết quả; không retry mật khẩu cũ.
-- Credential được lưu trực tiếp trong Users.Password cho đồ án local/demo; UC11 sinh mật khẩu mới.

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
    IsActive BIT NOT NULL CONSTRAINT DF_Departments_IsActive DEFAULT 1,
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
    DepartmentId INT NULL,                          -- Nullable để tương thích Position legacy
    JobTitleId INT NULL,
    ResponsibilityLevelId INT NULL,                 -- Luồng tạo Position cũ chưa cấp level
    PositionCode VARCHAR(50) NULL,
    IsActive BIT NOT NULL CONSTRAINT DF_Positions_IsActive DEFAULT 1,
    UpdatedAt DATETIME2 NULL,
    CanManageEmployees BIT NOT NULL CONSTRAINT DF_Positions_CanManageEmployees DEFAULT 0,
    PositionAllowance DECIMAL(18,2) NULL,
    ReportsToPositionId INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Positions_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT UQ_Positions_Company_Department_Title UNIQUE (CompanyId, DepartmentId, PositionTitle),
    CONSTRAINT CK_Positions_Allowance CHECK (PositionAllowance IS NULL OR PositionAllowance >= 0),
    CONSTRAINT CK_Positions_NotSelfReporting CHECK (ReportsToPositionId IS NULL OR ReportsToPositionId <> PositionId)
);

-- ------------------------------------------------------------------------
-- BẢNG 8: Employees (Hồ sơ nhân viên trung tâm)
-- ------------------------------------------------------------------------
CREATE TABLE Employees (
    EmployeeId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    UserId INT NULL,                                 -- UNIQUE có lọc ở cuối file; cho phép hồ sơ chưa cấp User
    ManagerId INT NULL,                              -- Tự tham chiếu cấp trên quản lý trực tiếp
    DepartmentId INT NULL,
    PositionId INT NULL,
    EmployeeCode VARCHAR(50) NOT NULL,               -- Mã nhân viên nội bộ (VD: 'EMP-001')
    FullName NVARCHAR(150) NOT NULL,
    Phone VARCHAR(30) NULL,
    Gender NVARCHAR(20) NULL,
    DateOfBirth DATE NULL,
    IdentityNumber VARCHAR(50) NULL,
    Email VARCHAR(150) NULL,
    IdentityIssuedDate DATE NULL,
    IdentityIssuedPlace NVARCHAR(200) NULL,
    PermanentAddress NVARCHAR(500) NULL,
    Hometown NVARCHAR(200) NULL,
    Major NVARCHAR(200) NULL,
    IntendedContractType VARCHAR(30) NULL,
    TerminationReason NVARCHAR(500) NULL,
    EndDate DATE NULL,
    BaseSalary DECIMAL(18,2) NOT NULL DEFAULT 0.00 CHECK (BaseSalary >= 0), -- Chỉ tương thích; payroll đọc lịch sử lương
    Status NVARCHAR(30) NOT NULL DEFAULT 'Active'
        CHECK (Status IN ('Active', 'On Leave', 'Inactive')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Employees_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT FK_Employees_Users FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE,
    CONSTRAINT FK_Employees_Manager FOREIGN KEY (ManagerId) REFERENCES Employees(EmployeeId),
    CONSTRAINT FK_Employees_Departments FOREIGN KEY (DepartmentId) REFERENCES Departments(DepartmentId),
    CONSTRAINT FK_Employees_Positions FOREIGN KEY (PositionId) REFERENCES Positions(PositionId),
    CONSTRAINT UQ_Company_EmployeeCode UNIQUE (CompanyId, EmployeeCode),
    CONSTRAINT CK_Employees_NotOwnManager CHECK (ManagerId IS NULL OR ManagerId <> EmployeeId),
    CONSTRAINT CK_Employees_Position_RequiresDepartment CHECK (PositionId IS NULL OR DepartmentId IS NOT NULL)
);

-- ========================================================================
-- PHẦN 4: THEO DÕI CHẤM CÔNG HÀNG NGÀY
-- Workflow 3: Attendance Management
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 9: CompanyShiftSettings (Cấu hình ca làm việc theo từng công ty)
-- ------------------------------------------------------------------------
-- TimesheetCutoffDay và ngưỡng số giờ được ứng dụng hiện tại sử dụng để
-- tính công và tự động khóa chốt công.
IF OBJECT_ID(N'dbo.CompanyShiftSettings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CompanyShiftSettings (
        CompanyId          INT PRIMARY KEY,
        ShiftStartTime     TIME(0) NOT NULL DEFAULT '08:30:00',
        ShiftEndTime       TIME(0) NOT NULL DEFAULT '17:30:00',
        LunchBreakMinutes  INT NOT NULL DEFAULT 60,
        GracePeriodMinutes INT NOT NULL DEFAULT 0,
        TimesheetCutoffDay INT NOT NULL DEFAULT 5,
        MinHoursHalfDay    DECIMAL(4,2) NOT NULL DEFAULT 4.00,
        MinHoursFullDay    DECIMAL(4,2) NOT NULL DEFAULT 6.00,
        UpdatedAt          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        UpdatedByUserId    INT NULL,
        CONSTRAINT FK_CompanyShiftSettings_Companies FOREIGN KEY (CompanyId)
            REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
        CONSTRAINT FK_CompanyShiftSettings_Users FOREIGN KEY (UpdatedByUserId)
            REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_CompanyShiftSettings_Times CHECK (ShiftEndTime > ShiftStartTime),
        CONSTRAINT CK_CompanyShiftSettings_CutoffDay CHECK (TimesheetCutoffDay BETWEEN 1 AND 31),
        CONSTRAINT CK_CompanyShiftSettings_Hours CHECK (
            MinHoursFullDay > MinHoursHalfDay AND MinHoursHalfDay > 0
        )
    );
END;
GO

-- ------------------------------------------------------------------------
-- BẢNG 10: Attendances (Bản ghi chấm công theo ca)
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
GO

-- ------------------------------------------------------------------------
-- BẢNG 11: AttendanceExplanations (Giải trình chấm công và quy trình duyệt)
-- ------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.AttendanceExplanations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AttendanceExplanations (
        ExplanationId    INT IDENTITY(1,1) PRIMARY KEY,
        CompanyId        INT NOT NULL,
        EmployeeId       INT NOT NULL,
        AttendanceDate   DATE NOT NULL,
        ProposedCheckIn  TIME(0) NULL,
        ProposedCheckOut TIME(0) NULL,
        ReasonType       NVARCHAR(50) NOT NULL,
        ReasonDetail     NVARCHAR(500) NOT NULL,
        Status           NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
        ReviewerId       INT NULL,
        ReviewerComment  NVARCHAR(500) NULL,
        ReviewedAt       DATETIME NULL,
        CreatedAt        DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_AttExp_Company FOREIGN KEY (CompanyId)
            REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
        CONSTRAINT FK_AttExp_Employee FOREIGN KEY (EmployeeId)
            REFERENCES dbo.Employees(EmployeeId),
        CONSTRAINT FK_AttExp_Reviewer FOREIGN KEY (ReviewerId)
            REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_AttExp_ReasonType CHECK (
            ReasonType IN (N'FORGOT_PUNCH', N'BUSINESS_TRIP', N'TECHNICAL_ISSUE', N'OTHER')
        ),
        CONSTRAINT CK_AttExp_Status CHECK (
            Status IN (N'PENDING', N'APPROVED', N'REJECTED', N'CANCELLED')
        )
    );
END;
GO

-- ------------------------------------------------------------------------
-- BẢNG 12: TimesheetLocks (Khóa chốt công theo tháng)
-- ------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.TimesheetLocks', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TimesheetLocks (
        LockId         INT IDENTITY(1,1) PRIMARY KEY,
        CompanyId      INT NOT NULL,
        [Month]        INT NOT NULL,
        [Year]         INT NOT NULL,
        IsLocked       BIT NOT NULL DEFAULT 1,
        LockedByUserId INT NOT NULL,
        LockedAt       DATETIME NOT NULL DEFAULT GETDATE(),
        Notes          NVARCHAR(255) NULL,
        CONSTRAINT UQ_TimesheetLocks UNIQUE (CompanyId, [Month], [Year]),
        CONSTRAINT FK_TimesheetLocks_Company FOREIGN KEY (CompanyId)
            REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
        CONSTRAINT FK_TimesheetLocks_User FOREIGN KEY (LockedByUserId)
            REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_TimesheetLocks_Month CHECK ([Month] BETWEEN 1 AND 12),
        CONSTRAINT CK_TimesheetLocks_Year CHECK ([Year] >= 2000)
    );
END;
GO

-- ========================================================================
-- PHẦN 5: QUẢN LÝ NGHỈ PHÉP VÀ XÉT DUYỆT
-- Workflow 4: Leave Management
-- ========================================================================

-- ------------------------------------------------------------------------
-- BẢNG 14: LeaveTypes (Danh mục chính sách các loại ngày nghỉ)
-- ------------------------------------------------------------------------
CREATE TABLE LeaveTypes (
    LeaveTypeId INT IDENTITY(1,1) PRIMARY KEY,
    CompanyId INT NOT NULL,
    TypeCode VARCHAR(50) NOT NULL,                   -- ANNUAL, SICK, UNPAID, MATERNITY
    TypeName NVARCHAR(100) NOT NULL,
    DefaultDaysPerYear INT NOT NULL DEFAULT 12,
    IsPaid BIT NOT NULL DEFAULT 1,                   -- Nghỉ có hưởng lương hay không
    IsActive BIT NOT NULL CONSTRAINT DF_LeaveTypes_IsActive DEFAULT 1,
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
-- Công thức: NetSalary = BaseSalary + ResponsibilityAllowance + Bonus - Deduction
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
    ReviewedByUserId INT NULL,
    ReviewedAt DATETIME2 NULL,
    IsClosed BIT NOT NULL DEFAULT 0,                -- Lock flag; not a fifth payroll status
    PublishedAt DATETIME2 NULL,                     -- Employee-visible only after publication
    PublishedByUserId INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_PayrollPeriods_Companies FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId) ON DELETE CASCADE,
    CONSTRAINT FK_PayrollPeriods_ApprovedBy FOREIGN KEY (ApprovedByUserId) REFERENCES Users(UserId),
    CONSTRAINT FK_PayrollPeriods_ReviewedBy FOREIGN KEY (ReviewedByUserId) REFERENCES Users(UserId),
    CONSTRAINT CK_PayrollPeriods_ReviewMetadata CHECK (
        (ReviewedByUserId IS NULL AND ReviewedAt IS NULL)
        OR (ReviewedByUserId IS NOT NULL AND ReviewedAt IS NOT NULL)),
    CONSTRAINT FK_PayrollPeriods_PublishedBy FOREIGN KEY (PublishedByUserId) REFERENCES Users(UserId),
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
    ResponsibilityAllowance DECIMAL(18,2) NOT NULL CONSTRAINT DF_Payslips_ResponsibilityAllowance DEFAULT 0.00,
    -- Cột tính toán tự động: NetSalary = BaseSalary + ResponsibilityAllowance + Bonus - Deduction
    NetSalary AS (BaseSalary + ResponsibilityAllowance + Bonus - Deduction),
    GeneratedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Payslips_PayrollPeriods FOREIGN KEY (PayrollPeriodId) REFERENCES PayrollPeriods(PayrollPeriodId),
    CONSTRAINT FK_Payslips_Employees FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId),
    CONSTRAINT UQ_Period_Employee_Payslip UNIQUE (PayrollPeriodId, EmployeeId),
    CONSTRAINT CK_Payslips_ResponsibilityAllowance CHECK (ResponsibilityAllowance >= 0)
);

-- Publication and each employee's in-system notification are committed together.
CREATE TABLE PayrollNotifications (
    NotificationId BIGINT IDENTITY(1,1) PRIMARY KEY,
    PayslipId BIGINT NOT NULL,
    EmployeeId INT NOT NULL,
    CompanyId INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ReadAt DATETIME2 NULL,
    CONSTRAINT FK_PayrollNotifications_Payslip FOREIGN KEY (PayslipId) REFERENCES Payslips(PayslipId),
    CONSTRAINT FK_PayrollNotifications_Employee FOREIGN KEY (EmployeeId) REFERENCES Employees(EmployeeId),
    CONSTRAINT FK_PayrollNotifications_Company FOREIGN KEY (CompanyId) REFERENCES Companies(CompanyId),
    CONSTRAINT UQ_PayrollNotifications_Payslip UNIQUE (PayslipId)
);
CREATE INDEX IX_PayrollNotifications_Employee ON PayrollNotifications(CompanyId,EmployeeId,CreatedAt DESC);

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

-- Enforce each tenant's subscribed MaxAccounts limit for every Users insert/update.
-- CompanyId NULL is reserved for platform accounts and is not part of a tenant quota.
CREATE OR ALTER TRIGGER dbo.TR_Users_MaxAccounts
ON dbo.Users
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @AffectedCompanies TABLE (
        CompanyId INT NOT NULL PRIMARY KEY
    );

    INSERT INTO @AffectedCompanies (CompanyId)
    SELECT CompanyId FROM inserted WHERE CompanyId IS NOT NULL
    UNION
    SELECT CompanyId FROM deleted WHERE CompanyId IS NOT NULL;

    DECLARE @CompanyId INT;
    DECLARE @LockResult INT;
    DECLARE @Resource NVARCHAR(255);
    DECLARE @AccountCount BIGINT;
    DECLARE @MaxAccounts INT;

    DECLARE CompanyQuotaCursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT CompanyId FROM @AffectedCompanies ORDER BY CompanyId;

    OPEN CompanyQuotaCursor;
    FETCH NEXT FROM CompanyQuotaCursor INTO @CompanyId;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @MaxAccounts = NULL;
        SET @Resource = N'Users.MaxAccounts.Company.' + CONVERT(NVARCHAR(20), @CompanyId);
        EXEC @LockResult = sys.sp_getapplock
            @Resource = @Resource,
            @LockMode = 'Exclusive',
            @LockOwner = 'Transaction',
            @LockTimeout = 10000;

        IF @LockResult < 0
        BEGIN
            CLOSE CompanyQuotaCursor;
            DEALLOCATE CompanyQuotaCursor;
            THROW 51160, 'Could not acquire the tenant account quota lock.', 1;
        END;

        SELECT @MaxAccounts = p.MaxAccounts
        FROM dbo.Subscriptions s WITH (UPDLOCK, HOLDLOCK)
        INNER JOIN dbo.Plans p WITH (UPDLOCK, HOLDLOCK) ON p.PlanId = s.PlanId
        WHERE s.CompanyId = @CompanyId;

        IF @MaxAccounts IS NULL
        BEGIN
            CLOSE CompanyQuotaCursor;
            DEALLOCATE CompanyQuotaCursor;
            THROW 51161, 'A tenant account requires an active Subscription and Plan.', 1;
        END;

        SELECT @AccountCount = COUNT_BIG(*)
        FROM dbo.Users WITH (UPDLOCK, HOLDLOCK, INDEX(IX_Users_CompanyId))
        WHERE CompanyId = @CompanyId;

        IF @AccountCount > @MaxAccounts
        BEGIN
            CLOSE CompanyQuotaCursor;
            DEALLOCATE CompanyQuotaCursor;
            THROW 51162, 'The tenant account limit for its Subscription Plan has been reached.', 1;
        END;

        FETCH NEXT FROM CompanyQuotaCursor INTO @CompanyId;
    END;

    CLOSE CompanyQuotaCursor;
    DEALLOCATE CompanyQuotaCursor;
END;
GO

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
    MimeType VARCHAR(100) NULL,
    FileSizeBytes BIGINT NULL,
    ContractId BIGINT NULL,
    DocumentPeriodId BIGINT NULL,

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

-- ========================================================================
-- PHẦN 10: SCHEMA ITER2 — HỢP ĐỒNG, VỊ TRÍ VÀ LỊCH SỬ NHÂN SỰ
-- Giữ Password, Employees.StartDate/BaseSalary và Position nullable cho code legacy.
-- Không seed tenant, baseline hoặc đánh dấu version migration đã chạy.
-- ========================================================================

CREATE TABLE [dbo].[JobTitles](
    [JobTitleId] [int] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [JobTitleCode] [varchar](50) NOT NULL,
    [JobTitleName] [nvarchar](150) NOT NULL,
    [Description] [nvarchar](500) NULL,
    [IsActive] [bit] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [UpdatedAt] [datetime2](7) NULL,
 CONSTRAINT [PK_JobTitles] PRIMARY KEY CLUSTERED
(
    [JobTitleId] ASC
),
 CONSTRAINT [UQ_JobTitles_Company_Code] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [JobTitleCode] ASC
),
 CONSTRAINT [UQ_JobTitles_Company_Id] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [JobTitleId] ASC
),
 CONSTRAINT [UQ_JobTitles_Company_Name] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [JobTitleName] ASC
)
);
GO

CREATE TABLE [dbo].[ResponsibilityLevels](
    [ResponsibilityLevelId] [int] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [LevelCode] [varchar](50) NOT NULL,
    [LevelName] [nvarchar](100) NOT NULL,
    [RankOrder] [int] NOT NULL,
    [IsActive] [bit] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [UpdatedAt] [datetime2](7) NULL,
 CONSTRAINT [PK_ResponsibilityLevels] PRIMARY KEY CLUSTERED
(
    [ResponsibilityLevelId] ASC
),
 CONSTRAINT [UQ_ResponsibilityLevels_Company_Code] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [LevelCode] ASC
),
 CONSTRAINT [UQ_ResponsibilityLevels_Company_Id] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [ResponsibilityLevelId] ASC
),
 CONSTRAINT [UQ_ResponsibilityLevels_Company_Rank] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [RankOrder] ASC
)
);
GO

CREATE TABLE [dbo].[ResponsibilityAllowancePolicies](
    [AllowancePolicyId] [bigint] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [ResponsibilityLevelId] [int] NOT NULL,
    [Amount] [decimal](18, 2) NOT NULL,
    [EffectiveFrom] [date] NOT NULL,
    [EffectiveTo] [date] NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [CreatedByUserId] [int] NULL,
 CONSTRAINT [PK_ResponsibilityAllowancePolicies] PRIMARY KEY CLUSTERED
(
    [AllowancePolicyId] ASC
),
 CONSTRAINT [UQ_AllowancePolicies_Level_Start] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [ResponsibilityLevelId] ASC,
    [EffectiveFrom] ASC
)
);
GO

CREATE TABLE [dbo].[EmploymentContracts](
    [ContractId] [bigint] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [EmployeeId] [int] NOT NULL,
    [ContractCode] [varchar](60) NOT NULL,
    [ContractType] [varchar](30) NOT NULL,
    [StartDate] [date] NOT NULL,
    [EndDate] [date] NULL,
    [GrossSalary] [decimal](18, 2) NOT NULL,
    [Status] [varchar](25) NOT NULL,
    [SignedDate] [date] NULL,
    [TerminatedDate] [date] NULL,
    [TerminationReason] [nvarchar](500) NULL,
    [Notes] [nvarchar](1000) NULL,
    [CreatedByUserId] [int] NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [UpdatedAt] [datetime2](7) NULL,
    [TravelAllowance] [decimal](18, 2) NULL,
 CONSTRAINT [PK_EmploymentContracts] PRIMARY KEY CLUSTERED
(
    [ContractId] ASC
),
 CONSTRAINT [UQ_EmploymentContracts_Company_Code] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [ContractCode] ASC
)
);
GO

CREATE TABLE [dbo].[EmployeePositionChanges](
    [PositionChangeId] [bigint] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [EmployeeId] [int] NOT NULL,
    [OldPositionId] [int] NULL,
    [NewPositionId] [int] NOT NULL,
    [ChangeType] [varchar](20) NOT NULL,
    [Status] [varchar](20) NOT NULL,
    [EffectiveDate] [date] NOT NULL,
    [NewBaseSalary] [decimal](18, 2) NULL,
    [NewManagerId] [int] NULL,
    [Reason] [nvarchar](500) NOT NULL,
    [CreatedByUserId] [int] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [ApprovedByUserId] [int] NULL,
    [ApprovedAt] [datetime2](7) NULL,
    [AppliedAt] [datetime2](7) NULL,
    [OldManagerId] [int] NULL,
 CONSTRAINT [PK_EmployeePositionChanges] PRIMARY KEY CLUSTERED
(
    [PositionChangeId] ASC
),
 CONSTRAINT [UQ_EmployeePositionChanges_Company_Id] UNIQUE NONCLUSTERED
(
    [CompanyId] ASC,
    [PositionChangeId] ASC
)
);
GO

CREATE TABLE [dbo].[EmployeePositionAssignments](
    [AssignmentId] [bigint] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [EmployeeId] [int] NOT NULL,
    [DepartmentId] [int] NOT NULL,
    [PositionId] [int] NOT NULL,
    [EffectiveFrom] [date] NOT NULL,
    [EffectiveTo] [date] NULL,
    [ChangeType] [varchar](20) NOT NULL,
    [Reason] [nvarchar](500) NULL,
    [SourceChangeId] [bigint] NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [ManagerId] [int] NULL,
 CONSTRAINT [PK_EmployeePositionAssignments] PRIMARY KEY CLUSTERED
(
    [AssignmentId] ASC
)
);
GO

CREATE TABLE [dbo].[EmployeeCompensationHistories](
    [CompensationHistoryId] [bigint] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [EmployeeId] [int] NOT NULL,
    [BaseSalary] [decimal](18, 2) NOT NULL,
    [EffectiveFrom] [date] NOT NULL,
    [EffectiveTo] [date] NULL,
    [Reason] [nvarchar](500) NULL,
    [SourceChangeId] [bigint] NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [CreatedByUserId] [int] NULL,
    [InsuranceSalary] [decimal](18, 2) NULL,
    [PositionAllowance] [decimal](18, 2) NULL,
    [TravelAllowance] [decimal](18, 2) NULL,
    [SourceContractId] [bigint] NULL,
    [SalaryStructureCode] [varchar](50) NULL,          -- Bảo toàn metadata khi tạo lịch sử lương mới
 CONSTRAINT [PK_EmployeeCompensationHistories] PRIMARY KEY CLUSTERED
(
    [CompensationHistoryId] ASC
)
);
GO

CREATE TABLE [dbo].[ProfileUpdatePeriods](
    [PeriodId] [bigint] IDENTITY(1,1) NOT NULL,
    [CompanyId] [int] NOT NULL,
    [Title] [nvarchar](150) NOT NULL,
    [StartsAt] [datetime2](7) NOT NULL,
    [EndsAt] [datetime2](7) NOT NULL,
    [ClosedAt] [datetime2](7) NULL,
    [CreatedByUserId] [int] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [PeriodType] [varchar](10) NOT NULL,
PRIMARY KEY CLUSTERED
(
    [PeriodId] ASC
)
);
GO

CREATE TABLE [dbo].[CompanyDocumentTypes](
    [DocumentTypeId] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [CompanyId] [int] NOT NULL,
    [TypeName] [nvarchar](100) NOT NULL,
    [IsActive] [bit] NOT NULL CONSTRAINT [DF_CompanyDocumentTypes_IsActive] DEFAULT ((1)),
    [CreatedAt] [datetime2](7) NOT NULL CONSTRAINT [DF_CompanyDocumentTypes_CreatedAt] DEFAULT (sysutcdatetime()),
    CONSTRAINT [FK_CompanyDocumentTypes_Companies] FOREIGN KEY ([CompanyId]) REFERENCES [dbo].[Companies]([CompanyId]),
    CONSTRAINT [UQ_CompanyDocumentTypes_Company_Name] UNIQUE ([CompanyId], [TypeName])
);
GO

CREATE TABLE [dbo].[DocumentUpdatePeriods](
    [DocumentPeriodId] [bigint] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [CompanyId] [int] NOT NULL,
    [Title] [nvarchar](150) NOT NULL,
    [StartsAt] [datetime2](7) NOT NULL,
    [EndsAt] [datetime2](7) NOT NULL,
    [ScopeType] [varchar](20) NOT NULL CONSTRAINT [DF_DocumentUpdatePeriods_ScopeType] DEFAULT ('ALL'),
    [ClosedAt] [datetime2](7) NULL,
    [CreatedByUserId] [int] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL CONSTRAINT [DF_DocumentUpdatePeriods_CreatedAt] DEFAULT (sysutcdatetime()),
    CONSTRAINT [FK_DocumentUpdatePeriods_Companies] FOREIGN KEY ([CompanyId]) REFERENCES [dbo].[Companies]([CompanyId]),
    CONSTRAINT [FK_DocumentUpdatePeriods_Users] FOREIGN KEY ([CreatedByUserId]) REFERENCES [dbo].[Users]([UserId]),
    CONSTRAINT [CK_DocumentUpdatePeriods_Dates] CHECK ([EndsAt] > [StartsAt]),
    CONSTRAINT [CK_DocumentUpdatePeriods_Scope] CHECK ([ScopeType] IN ('ALL', 'SELECTED'))
);
GO

CREATE TABLE [dbo].[DocumentUpdatePeriodTypes](
    [DocumentPeriodId] [bigint] NOT NULL,
    [DocumentTypeId] [int] NOT NULL,
    CONSTRAINT [PK_DocumentUpdatePeriodTypes] PRIMARY KEY ([DocumentPeriodId], [DocumentTypeId]),
    CONSTRAINT [FK_DocumentUpdatePeriodTypes_Period] FOREIGN KEY ([DocumentPeriodId])
        REFERENCES [dbo].[DocumentUpdatePeriods]([DocumentPeriodId]) ON DELETE CASCADE,
    CONSTRAINT [FK_DocumentUpdatePeriodTypes_Type] FOREIGN KEY ([DocumentTypeId])
        REFERENCES [dbo].[CompanyDocumentTypes]([DocumentTypeId]) ON DELETE CASCADE
);
GO

CREATE TABLE [dbo].[DocumentUpdatePeriodEmployees](
    [DocumentPeriodId] [bigint] NOT NULL,
    [EmployeeId] [int] NOT NULL,
    CONSTRAINT [PK_DocumentUpdatePeriodEmployees] PRIMARY KEY ([DocumentPeriodId], [EmployeeId]),
    CONSTRAINT [FK_DocumentUpdatePeriodEmployees_Period] FOREIGN KEY ([DocumentPeriodId])
        REFERENCES [dbo].[DocumentUpdatePeriods]([DocumentPeriodId]) ON DELETE CASCADE,
    CONSTRAINT [FK_DocumentUpdatePeriodEmployees_Employee] FOREIGN KEY ([EmployeeId])
        REFERENCES [dbo].[Employees]([EmployeeId]) ON DELETE CASCADE
);
GO

CREATE TABLE [dbo].[LifecycleSchemaVersions](
    [Version] [varchar](80) NOT NULL,
    [AppliedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED
(
    [Version] ASC
)
);
GO


-- Defaults của các bảng Iter2.

ALTER TABLE [dbo].[EmployeeCompensationHistories] ADD  CONSTRAINT [DF_EmployeeCompensationHistories_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[EmployeePositionAssignments] ADD  CONSTRAINT [DF_EmployeePositionAssignments_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[EmployeePositionChanges] ADD  CONSTRAINT [DF_EmployeePositionChanges_Status]  DEFAULT ('PENDING') FOR [Status];
GO

ALTER TABLE [dbo].[EmployeePositionChanges] ADD  CONSTRAINT [DF_EmployeePositionChanges_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[EmploymentContracts] ADD  CONSTRAINT [DF_EmploymentContracts_Status]  DEFAULT ('DRAFT') FOR [Status];
GO

ALTER TABLE [dbo].[EmploymentContracts] ADD  CONSTRAINT [DF_EmploymentContracts_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[JobTitles] ADD  CONSTRAINT [DF_JobTitles_IsActive]  DEFAULT ((1)) FOR [IsActive];
GO

ALTER TABLE [dbo].[JobTitles] ADD  CONSTRAINT [DF_JobTitles_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[LifecycleSchemaVersions] ADD  DEFAULT (sysutcdatetime()) FOR [AppliedAt];
GO

ALTER TABLE [dbo].[ProfileUpdatePeriods] ADD  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[ProfileUpdatePeriods] ADD  CONSTRAINT [DF_ProfileUpdatePeriods_PeriodType]  DEFAULT ('PROFILE') FOR [PeriodType];
GO

ALTER TABLE [dbo].[ResponsibilityAllowancePolicies] ADD  CONSTRAINT [DF_ResponsibilityAllowancePolicies_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO

ALTER TABLE [dbo].[ResponsibilityLevels] ADD  CONSTRAINT [DF_ResponsibilityLevels_IsActive]  DEFAULT ((1)) FOR [IsActive];
GO

ALTER TABLE [dbo].[ResponsibilityLevels] ADD  CONSTRAINT [DF_ResponsibilityLevels_CreatedAt]  DEFAULT (sysutcdatetime()) FOR [CreatedAt];
GO


-- Index hỗ trợ khóa ghép theo Company và truy vấn lịch sử.

CREATE UNIQUE NONCLUSTERED INDEX [UX_Departments_Company_Id] ON [dbo].[Departments]
(
    [CompanyId] ASC,
    [DepartmentId] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_Compensation_SourceContract] ON [dbo].[EmployeeCompensationHistories]
(
    [CompanyId] ASC,
    [EmployeeId] ASC,
    [SourceContractId] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_EmployeeCompensationHistories_Dates] ON [dbo].[EmployeeCompensationHistories]
(
    [CompanyId] ASC,
    [EmployeeId] ASC,
    [EffectiveFrom] DESC
)
INCLUDE([EffectiveTo],[BaseSalary]);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_EmployeeCompensationHistories_OpenEnded] ON [dbo].[EmployeeCompensationHistories]
(
    [CompanyId] ASC,
    [EmployeeId] ASC
)
WHERE ([EffectiveTo] IS NULL);
GO

CREATE NONCLUSTERED INDEX [IX_Assignments_PositionPeriod] ON [dbo].[EmployeePositionAssignments]
(
    [CompanyId] ASC,
    [PositionId] ASC,
    [EffectiveFrom] ASC
)
INCLUDE([EmployeeId],[EffectiveTo]);
GO

CREATE NONCLUSTERED INDEX [IX_EmployeePositionAssignments_Dates] ON [dbo].[EmployeePositionAssignments]
(
    [CompanyId] ASC,
    [EmployeeId] ASC,
    [EffectiveFrom] DESC
)
INCLUDE([EffectiveTo],[PositionId],[DepartmentId]);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_EmployeePositionAssignments_OpenEnded] ON [dbo].[EmployeePositionAssignments]
(
    [CompanyId] ASC,
    [EmployeeId] ASC
)
WHERE ([EffectiveTo] IS NULL);
GO

CREATE NONCLUSTERED INDEX [IX_EmployeePositionChanges_Due] ON [dbo].[EmployeePositionChanges]
(
    [Status] ASC,
    [EffectiveDate] ASC,
    [CompanyId] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_EmployeePositionChanges_Employee] ON [dbo].[EmployeePositionChanges]
(
    [CompanyId] ASC,
    [EmployeeId] ASC,
    [EffectiveDate] DESC
);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Employees_Company_Email] ON [dbo].[Employees]
(
    [CompanyId] ASC,
    [Email] ASC
)
WHERE ([Email] IS NOT NULL);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Employees_Company_Id] ON [dbo].[Employees]
(
    [CompanyId] ASC,
    [EmployeeId] ASC
);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Employees_LinkedUser] ON [dbo].[Employees]
(
    [UserId] ASC
)
WHERE ([UserId] IS NOT NULL);
GO

CREATE NONCLUSTERED INDEX [IX_EmploymentContracts_Employee_Start] ON [dbo].[EmploymentContracts]
(
    [EmployeeId] ASC,
    [StartDate] DESC
);
GO

CREATE NONCLUSTERED INDEX [IX_EmploymentContracts_Expiry] ON [dbo].[EmploymentContracts]
(
    [Status] ASC,
    [EndDate] ASC
);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Contracts_CompanyEmployeeContract] ON [dbo].[EmploymentContracts]
(
    [CompanyId] ASC,
    [EmployeeId] ASC,
    [ContractId] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_Positions_Department_Active] ON [dbo].[Positions]
(
    [CompanyId] ASC,
    [DepartmentId] ASC,
    [IsActive] ASC
)
INCLUDE([PositionTitle],[ResponsibilityLevelId],[CanManageEmployees]);
GO

CREATE NONCLUSTERED INDEX [IX_Positions_ReportsToPosition] ON [dbo].[Positions]
(
    [CompanyId] ASC,
    [ReportsToPositionId] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_Positions_ResponsibilityLevel] ON [dbo].[Positions]
(
    [CompanyId] ASC,
    [ResponsibilityLevelId] ASC
);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Positions_Company_Code] ON [dbo].[Positions]
(
    [CompanyId] ASC,
    [PositionCode] ASC
)
WHERE ([PositionCode] IS NOT NULL);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Positions_Company_Department_Id] ON [dbo].[Positions]
(
    [CompanyId] ASC,
    [DepartmentId] ASC,
    [PositionId] ASC
);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Positions_Company_Id] ON [dbo].[Positions]
(
    [CompanyId] ASC,
    [PositionId] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_ProfileUpdatePeriods_CompanyDates] ON [dbo].[ProfileUpdatePeriods]
(
    [CompanyId] ASC,
    [ClosedAt] ASC,
    [StartsAt] ASC,
    [EndsAt] ASC
);
GO

CREATE NONCLUSTERED INDEX [IX_ProfileUpdatePeriods_CompanyTypeDates] ON [dbo].[ProfileUpdatePeriods]
(
    [CompanyId] ASC,
    [PeriodType] ASC,
    [StartsAt] ASC,
    [EndsAt] ASC
)
INCLUDE([ClosedAt]);
GO

CREATE NONCLUSTERED INDEX [IX_AllowancePolicies_Lookup] ON [dbo].[ResponsibilityAllowancePolicies]
(
    [CompanyId] ASC,
    [ResponsibilityLevelId] ASC,
    [EffectiveFrom] DESC
)
INCLUDE([EffectiveTo],[Amount]);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_AllowancePolicies_OpenEnded] ON [dbo].[ResponsibilityAllowancePolicies]
(
    [CompanyId] ASC,
    [ResponsibilityLevelId] ASC
)
WHERE ([EffectiveTo] IS NULL);
GO

CREATE UNIQUE NONCLUSTERED INDEX [UX_Lifecycle_Users_CompanyUser] ON [dbo].[Users]
(
    [CompanyId] ASC,
    [UserId] ASC
);
GO


-- CHECK mới; giữ CHECK/collation WF1/WF2 hiện có.

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [CK_EmployeeCompensations_DateRange] CHECK  (([EffectiveTo] IS NULL OR [EffectiveTo]>=[EffectiveFrom]));
GO

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [CK_EmployeeCompensations_Salary] CHECK  (([BaseSalary]>=(0)));
GO

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [CK_Lifecycle_CompensationAmounts] CHECK  ((([InsuranceSalary] IS NULL OR [InsuranceSalary]>=(0)) AND ([PositionAllowance] IS NULL OR [PositionAllowance]>=(0)) AND ([TravelAllowance] IS NULL OR [TravelAllowance]>=(0))));
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionAssignments_DateRange] CHECK  (([EffectiveTo] IS NULL OR [EffectiveTo]>=[EffectiveFrom]));
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionAssignments_Type] CHECK  (([ChangeType]='MANAGER_CHANGE' OR [ChangeType]='CORRECTION' OR [ChangeType]='DEMOTION' OR [ChangeType]='PROMOTION' OR [ChangeType]='TRANSFER' OR [ChangeType]='HIRE' OR [ChangeType]='BASELINE'));
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionChanges_Applied] CHECK  (([Status]<>'APPLIED' OR [AppliedAt] IS NOT NULL));
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionChanges_Reason] CHECK  ((len(ltrim(rtrim([Reason])))>(0)));
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionChanges_Salary] CHECK  (([NewBaseSalary] IS NULL OR [NewBaseSalary]>=(0)));
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionChanges_Status] CHECK  (([Status]='CANCELLED' OR [Status]='REJECTED' OR [Status]='APPLIED' OR [Status]='APPROVED' OR [Status]='PENDING'));
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [CK_EmployeePositionChanges_Type] CHECK  (([ChangeType]='MANAGER_CHANGE' OR [ChangeType]='CORRECTION' OR [ChangeType]='DEMOTION' OR [ChangeType]='PROMOTION' OR [ChangeType]='TRANSFER'));
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [CK_EmploymentContracts_Dates] CHECK  (([EndDate] IS NULL OR [EndDate]>=[StartDate]));
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [CK_EmploymentContracts_Salary] CHECK  (([GrossSalary]>=(0)));
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [CK_EmploymentContracts_Status] CHECK  (([Status]='CANCELLED' OR [Status]='TERMINATED' OR [Status]='EXPIRED' OR [Status]='ACTIVE' OR [Status]='DRAFT'));
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [CK_EmploymentContracts_TravelAllowance] CHECK  (([TravelAllowance] IS NULL OR [TravelAllowance]>=(0)));
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [CK_EmploymentContracts_Type] CHECK  (([ContractType]='INDEFINITE' OR [ContractType]='FIXED_TERM' OR [ContractType]='PROBATION'));
GO

ALTER TABLE [dbo].[JobTitles]  WITH CHECK ADD  CONSTRAINT [CK_JobTitles_Names] CHECK  ((len(ltrim(rtrim([JobTitleCode])))>(0) AND len(ltrim(rtrim([JobTitleName])))>(0)));
GO

ALTER TABLE [dbo].[ProfileUpdatePeriods]  WITH CHECK ADD  CONSTRAINT [CK_ProfileUpdatePeriods_Dates] CHECK  (([EndsAt]>[StartsAt]));
GO

ALTER TABLE [dbo].[ProfileUpdatePeriods]  WITH CHECK ADD  CONSTRAINT [CK_ProfileUpdatePeriods_PeriodType] CHECK  (([PeriodType]='DOCUMENT' OR [PeriodType]='PROFILE'));
GO

ALTER TABLE [dbo].[ResponsibilityAllowancePolicies]  WITH CHECK ADD  CONSTRAINT [CK_AllowancePolicies_Amount] CHECK  (([Amount]>=(0)));
GO

ALTER TABLE [dbo].[ResponsibilityAllowancePolicies]  WITH CHECK ADD  CONSTRAINT [CK_AllowancePolicies_DateRange] CHECK  (([EffectiveTo] IS NULL OR [EffectiveTo]>[EffectiveFrom]));
GO

ALTER TABLE [dbo].[ResponsibilityLevels]  WITH CHECK ADD  CONSTRAINT [CK_ResponsibilityLevels_Name] CHECK  ((len(ltrim(rtrim([LevelCode])))>(0) AND len(ltrim(rtrim([LevelName])))>(0)));
GO

ALTER TABLE [dbo].[ResponsibilityLevels]  WITH CHECK ADD  CONSTRAINT [CK_ResponsibilityLevels_Rank] CHECK  (([RankOrder]>=(0)));
GO


-- FK được khai báo sau khi đủ bảng và unique index hỗ trợ.

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [FK_Compensation_SourceContract] FOREIGN KEY([CompanyId], [EmployeeId], [SourceContractId])
REFERENCES [dbo].[EmploymentContracts] ([CompanyId], [EmployeeId], [ContractId]);
GO

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [FK_EmployeeCompensations_Change] FOREIGN KEY([CompanyId], [SourceChangeId])
REFERENCES [dbo].[EmployeePositionChanges] ([CompanyId], [PositionChangeId]);
GO

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [FK_EmployeeCompensations_CreatedBy] FOREIGN KEY([CreatedByUserId])
REFERENCES [dbo].[Users] ([UserId]);
GO

ALTER TABLE [dbo].[EmployeeCompensationHistories]  WITH CHECK ADD  CONSTRAINT [FK_EmployeeCompensations_Employee] FOREIGN KEY([CompanyId], [EmployeeId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeeDocuments]  WITH CHECK ADD  CONSTRAINT [FK_EmployeeDocuments_Contract] FOREIGN KEY([ContractId])
REFERENCES [dbo].[EmploymentContracts] ([ContractId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_Assignments_Company_Department_Position] FOREIGN KEY([CompanyId], [DepartmentId], [PositionId])
REFERENCES [dbo].[Positions] ([CompanyId], [DepartmentId], [PositionId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionAssignments_Change] FOREIGN KEY([CompanyId], [SourceChangeId])
REFERENCES [dbo].[EmployeePositionChanges] ([CompanyId], [PositionChangeId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionAssignments_Department] FOREIGN KEY([CompanyId], [DepartmentId])
REFERENCES [dbo].[Departments] ([CompanyId], [DepartmentId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionAssignments_Employee] FOREIGN KEY([CompanyId], [EmployeeId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionAssignments_Manager] FOREIGN KEY([ManagerId])
REFERENCES [dbo].[Employees] ([EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionAssignments_Position] FOREIGN KEY([CompanyId], [PositionId])
REFERENCES [dbo].[Positions] ([CompanyId], [PositionId]);
GO

ALTER TABLE [dbo].[EmployeePositionAssignments]  WITH CHECK ADD  CONSTRAINT [FK_Lifecycle_AssignmentManager] FOREIGN KEY([CompanyId], [ManagerId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_ApprovedBy] FOREIGN KEY([ApprovedByUserId])
REFERENCES [dbo].[Users] ([UserId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_CreatedBy] FOREIGN KEY([CreatedByUserId])
REFERENCES [dbo].[Users] ([UserId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_Employee] FOREIGN KEY([CompanyId], [EmployeeId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_NewManager] FOREIGN KEY([CompanyId], [NewManagerId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_NewPosition] FOREIGN KEY([CompanyId], [NewPositionId])
REFERENCES [dbo].[Positions] ([CompanyId], [PositionId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_OldManager] FOREIGN KEY([CompanyId], [OldManagerId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[EmployeePositionChanges]  WITH CHECK ADD  CONSTRAINT [FK_EmployeePositionChanges_OldPosition] FOREIGN KEY([CompanyId], [OldPositionId])
REFERENCES [dbo].[Positions] ([CompanyId], [PositionId]);
GO

ALTER TABLE [dbo].[Employees]  WITH CHECK ADD  CONSTRAINT [FK_Employees_Company_Department] FOREIGN KEY([CompanyId], [DepartmentId])
REFERENCES [dbo].[Departments] ([CompanyId], [DepartmentId]);
GO

ALTER TABLE [dbo].[Employees]  WITH CHECK ADD  CONSTRAINT [FK_Employees_Company_Department_Position] FOREIGN KEY([CompanyId], [DepartmentId], [PositionId])
REFERENCES [dbo].[Positions] ([CompanyId], [DepartmentId], [PositionId]);
GO

ALTER TABLE [dbo].[Employees]  WITH CHECK ADD  CONSTRAINT [FK_Employees_Company_Manager] FOREIGN KEY([CompanyId], [ManagerId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[Employees]  WITH CHECK ADD  CONSTRAINT [FK_Lifecycle_EmployeeUserCompany] FOREIGN KEY([CompanyId], [UserId])
REFERENCES [dbo].[Users] ([CompanyId], [UserId]);
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [FK_EmploymentContracts_Company] FOREIGN KEY([CompanyId])
REFERENCES [dbo].[Companies] ([CompanyId]);
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [FK_EmploymentContracts_Creator] FOREIGN KEY([CreatedByUserId])
REFERENCES [dbo].[Users] ([UserId]);
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [FK_EmploymentContracts_Employee] FOREIGN KEY([EmployeeId])
REFERENCES [dbo].[Employees] ([EmployeeId]);
GO

ALTER TABLE [dbo].[EmploymentContracts]  WITH CHECK ADD  CONSTRAINT [FK_Lifecycle_ContractEmployeeCompany] FOREIGN KEY([CompanyId], [EmployeeId])
REFERENCES [dbo].[Employees] ([CompanyId], [EmployeeId]);
GO

ALTER TABLE [dbo].[JobTitles]  WITH CHECK ADD  CONSTRAINT [FK_JobTitles_Companies] FOREIGN KEY([CompanyId])
REFERENCES [dbo].[Companies] ([CompanyId]);
GO

ALTER TABLE [dbo].[Positions]  WITH CHECK ADD  CONSTRAINT [FK_Positions_Company_Department] FOREIGN KEY([CompanyId], [DepartmentId])
REFERENCES [dbo].[Departments] ([CompanyId], [DepartmentId]);
GO

ALTER TABLE [dbo].[Positions]  WITH CHECK ADD  CONSTRAINT [FK_Positions_JobTitles] FOREIGN KEY([CompanyId], [JobTitleId])
REFERENCES [dbo].[JobTitles] ([CompanyId], [JobTitleId]);
GO

ALTER TABLE [dbo].[Positions]  WITH CHECK ADD  CONSTRAINT [FK_Positions_ReportsToPosition] FOREIGN KEY([CompanyId], [ReportsToPositionId])
REFERENCES [dbo].[Positions] ([CompanyId], [PositionId]);
GO

ALTER TABLE [dbo].[Positions]  WITH CHECK ADD  CONSTRAINT [FK_Positions_ResponsibilityLevels] FOREIGN KEY([CompanyId], [ResponsibilityLevelId])
REFERENCES [dbo].[ResponsibilityLevels] ([CompanyId], [ResponsibilityLevelId]);
GO

ALTER TABLE [dbo].[ProfileUpdatePeriods]  WITH CHECK ADD  CONSTRAINT [FK_ProfileUpdatePeriods_Companies] FOREIGN KEY([CompanyId])
REFERENCES [dbo].[Companies] ([CompanyId]);
GO

ALTER TABLE [dbo].[ProfileUpdatePeriods]  WITH CHECK ADD  CONSTRAINT [FK_ProfileUpdatePeriods_Users] FOREIGN KEY([CreatedByUserId])
REFERENCES [dbo].[Users] ([UserId]);
GO

ALTER TABLE [dbo].[ResponsibilityAllowancePolicies]  WITH CHECK ADD  CONSTRAINT [FK_AllowancePolicies_CreatedBy] FOREIGN KEY([CreatedByUserId])
REFERENCES [dbo].[Users] ([UserId]);
GO

ALTER TABLE [dbo].[ResponsibilityAllowancePolicies]  WITH CHECK ADD  CONSTRAINT [FK_AllowancePolicies_Levels] FOREIGN KEY([CompanyId], [ResponsibilityLevelId])
REFERENCES [dbo].[ResponsibilityLevels] ([CompanyId], [ResponsibilityLevelId]);
GO

ALTER TABLE [dbo].[ResponsibilityLevels]  WITH CHECK ADD  CONSTRAINT [FK_ResponsibilityLevels_Companies] FOREIGN KEY([CompanyId])
REFERENCES [dbo].[Companies] ([CompanyId]);
GO


-- Procedure Iter2; chỉ chạy khi ứng dụng gọi với ngày hiệu lực theo Company.
-- Cần assignment baseline; procedure không tự tạo hoặc duyệt thay đổi.

CREATE PROCEDURE  [dbo].[usp_ApplyApprovedPositionChange]
    @PositionChangeId BIGINT,
    @AsOfDate DATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @AsOfDate IS NULL
        THROW 51000, '@AsOfDate (company-local) is required.', 1;

    DECLARE @CompanyId INT, @EmployeeId INT, @OldPositionId INT,
            @NewPositionId INT, @NewDepartmentId INT, @EffectiveDate DATE,
            @NewBaseSalary DECIMAL(18,2), @NewManagerId INT,
            @ChangeType VARCHAR(20), @ChangeStatus VARCHAR(20),
            @Reason NVARCHAR(500), @CreatedByUserId INT,
            @CurrentPositionId INT, @CurrentDepartmentId INT,
            @OldRank INT, @NewRank INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        SELECT @CompanyId=c.CompanyId, @EmployeeId=c.EmployeeId,
               @OldPositionId=c.OldPositionId, @NewPositionId=c.NewPositionId,
               @EffectiveDate=c.EffectiveDate, @NewBaseSalary=c.NewBaseSalary,
               @NewManagerId=c.NewManagerId, @ChangeType=c.ChangeType,
               @ChangeStatus=c.Status, @Reason=c.Reason, @CreatedByUserId=c.CreatedByUserId
        FROM dbo.EmployeePositionChanges c WITH (UPDLOCK, HOLDLOCK)
        WHERE c.PositionChangeId=@PositionChangeId;

        IF @CompanyId IS NULL
            THROW 51001, 'Position change not found.', 1;
        IF @ChangeStatus <> 'APPROVED'
            THROW 51002, 'Position change must be APPROVED.', 1;
        IF @EffectiveDate > @AsOfDate
            THROW 51003, 'Effective date is in the future.', 1;

        SELECT @CurrentPositionId=e.PositionId, @CurrentDepartmentId=e.DepartmentId
        FROM dbo.Employees e WITH (UPDLOCK, HOLDLOCK)
        WHERE e.CompanyId=@CompanyId AND e.EmployeeId=@EmployeeId;

        IF @CurrentPositionId IS NULL OR @OldPositionId IS NULL OR @CurrentPositionId <> @OldPositionId
            THROW 51004, 'Current Position is missing/changed; re-approve the request.', 1;
        IF @ChangeType='MANAGER_CHANGE' AND (
            @NewPositionId<>@CurrentPositionId OR @NewBaseSalary IS NOT NULL
            OR ISNULL((SELECT OldManagerId FROM dbo.EmployeePositionChanges WHERE PositionChangeId=@PositionChangeId AND CompanyId=@CompanyId),0)
                <>ISNULL((SELECT ManagerId FROM dbo.Employees WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId),0)
            OR ISNULL(@NewManagerId,0)=ISNULL((SELECT ManagerId FROM dbo.Employees WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId),0))
            THROW 51123, 'Manager-only change snapshot is stale or invalid.', 1;
        IF @ChangeType<>'MANAGER_CHANGE' AND @CurrentPositionId=@NewPositionId
            THROW 51005, 'New Position matches current Position.', 1;

        SELECT @NewDepartmentId=p.DepartmentId, @NewRank=rl.RankOrder
        FROM dbo.Positions p
        JOIN dbo.ResponsibilityLevels rl ON rl.CompanyId=p.CompanyId AND rl.ResponsibilityLevelId=p.ResponsibilityLevelId
        WHERE p.PositionId=@NewPositionId AND p.CompanyId=@CompanyId AND p.IsActive=1 AND rl.IsActive=1;

        IF @NewDepartmentId IS NULL
            THROW 51006, 'New Position is missing, inactive or incomplete.', 1;

        SELECT @OldRank=rl.RankOrder
        FROM dbo.Positions p
        JOIN dbo.ResponsibilityLevels rl ON rl.CompanyId=p.CompanyId AND rl.ResponsibilityLevelId=p.ResponsibilityLevelId
        WHERE p.PositionId=@CurrentPositionId AND p.CompanyId=@CompanyId;

        IF @ChangeType='PROMOTION' AND (@OldRank IS NULL OR @NewRank <= @OldRank)
            THROW 51007, 'PROMOTION must increase ResponsibilityLevel RankOrder.', 1;

        IF EXISTS (
            SELECT 1 FROM dbo.PayrollPeriods pp
            WHERE pp.CompanyId=@CompanyId AND pp.Status='Approved'
              AND (pp.[Year] > YEAR(@EffectiveDate)
                  OR (pp.[Year]=YEAR(@EffectiveDate) AND pp.[Month]>=MONTH(@EffectiveDate)))
        ) THROW 51008, 'Approved payroll period affected. Use controlled payroll correction.', 1;

        IF NOT EXISTS (
            SELECT 1 FROM dbo.EmployeePositionAssignments a WITH (UPDLOCK, HOLDLOCK)
            WHERE a.CompanyId=@CompanyId AND a.EmployeeId=@EmployeeId
                AND a.PositionId=@CurrentPositionId AND a.DepartmentId=@CurrentDepartmentId
                AND a.EffectiveTo IS NULL AND a.EffectiveFrom < @EffectiveDate
        ) THROW 51009, 'Missing current assignment/baseline or invalid effective date.', 1;

        UPDATE dbo.EmployeePositionAssignments
        SET EffectiveTo=@EffectiveDate
        WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId AND PositionId=@CurrentPositionId
              AND DepartmentId=@CurrentDepartmentId AND EffectiveTo IS NULL
              AND EffectiveFrom < @EffectiveDate;

        IF @@ROWCOUNT <> 1
            THROW 51010, 'Could not close previous position assignment.', 1;

        INSERT INTO dbo.EmployeePositionAssignments
            (CompanyId,EmployeeId,DepartmentId,PositionId,ManagerId,EffectiveFrom,EffectiveTo,
             ChangeType,Reason,SourceChangeId)
        VALUES
            (@CompanyId,@EmployeeId,@NewDepartmentId,@NewPositionId,@NewManagerId,@EffectiveDate,NULL,
             @ChangeType,@Reason,@PositionChangeId);

        IF @NewBaseSalary IS NOT NULL
        BEGIN
            IF EXISTS (
                SELECT 1 FROM dbo.EmployeeCompensationHistories s
                WHERE s.CompanyId=@CompanyId AND s.EmployeeId=@EmployeeId
                      AND s.EffectiveTo IS NULL AND s.EffectiveFrom>=@EffectiveDate
            ) THROW 51011, 'Conflicting current salary effective date.', 1;

            UPDATE dbo.EmployeeCompensationHistories
            SET EffectiveTo=@EffectiveDate
            WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId
                  AND EffectiveTo IS NULL AND EffectiveFrom<@EffectiveDate;

            INSERT INTO dbo.EmployeeCompensationHistories
                (CompanyId,EmployeeId,BaseSalary,EffectiveFrom,Reason,SourceChangeId,CreatedByUserId)
            VALUES (@CompanyId,@EmployeeId,@NewBaseSalary,@EffectiveDate,@Reason,@PositionChangeId,@CreatedByUserId);
        END;

        UPDATE dbo.Employees
        SET PositionId=@NewPositionId,
            DepartmentId=@NewDepartmentId,
            ManagerId=@NewManagerId,
            -- Đồng bộ cột lương legacy được giữ để tương thích EmployeeDAO.
            BaseSalary=COALESCE(@NewBaseSalary, BaseSalary),
            UpdatedAt=SYSUTCDATETIME()
        WHERE EmployeeId=@EmployeeId AND CompanyId=@CompanyId;

        UPDATE dbo.EmployeePositionChanges
        SET Status='APPLIED',AppliedAt=SYSUTCDATETIME()
        WHERE PositionChangeId=@PositionChangeId AND CompanyId=@CompanyId;

        INSERT INTO dbo.AuditLogs
            (CompanyId,UserId,[Action],TableName,RecordId,OldValues,NewValues,CreatedAt)
        VALUES
            (@CompanyId,@CreatedByUserId,N'POSITION_CHANGE_APPLIED',N'Employees',
             CONVERT(VARCHAR(100),@EmployeeId),
             CONCAT(N'{"PositionId":',@CurrentPositionId,N',"DepartmentId":',@CurrentDepartmentId,N'}'),
             CONCAT(N'{"PositionId":',@NewPositionId,N',"DepartmentId":',@NewDepartmentId,N'}'),
             SYSUTCDATETIME());

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

-- Payroll uses existing project roles; no account is promoted or granted
-- access to unrelated administration screens by this projection.
CREATE OR ALTER VIEW dbo.vw_PayrollOperators
AS
    SELECT u.UserId,u.CompanyId,r.RoleCode
    FROM dbo.Users u JOIN dbo.Roles r ON r.RoleId=u.RoleId
    WHERE u.CompanyId IS NOT NULL AND r.RoleCode IN ('MANAGER','COMPANY_ADMIN','HR')
        AND u.IsActive=1 AND u.IsLocked=0 AND u.MustChangePassword=0
        AND (u.LockedUntil IS NULL OR u.LockedUntil<=SYSUTCDATETIME());
GO

CREATE OR ALTER TRIGGER dbo.TR_PayrollPeriods_Reviewer ON dbo.PayrollPeriods
AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.UserId=i.ReviewedByUserId
        WHERE i.ReviewedByUserId IS NOT NULL AND (u.CompanyId IS NULL OR u.CompanyId<>i.CompanyId))
        THROW 51004, 'Payroll reviewer references another company.', 1;
END;
GO

-- Company-scoped catalogue names, including inactive records.
-- Existing duplicates must be resolved before this migration; never rename them automatically.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.Departments') AND name='UX_Departments_Company_Name')
        CREATE UNIQUE INDEX UX_Departments_Company_Name ON dbo.Departments(CompanyId,DepartmentName);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.Positions') AND name='UX_Positions_Company_Title')
        CREATE UNIQUE INDEX UX_Positions_Company_Title ON dbo.Positions(CompanyId,PositionTitle);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.ResponsibilityLevels') AND name='UX_ResponsibilityLevels_Company_Name')
        CREATE UNIQUE INDEX UX_ResponsibilityLevels_Company_Name ON dbo.ResponsibilityLevels(CompanyId,LevelName);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- ========================================================================
-- DocumentUpdatePeriods & ProfileUpdatePeriods Extensions
-- ========================================================================
IF COL_LENGTH('dbo.ProfileUpdatePeriods', 'ScopeType') IS NULL
BEGIN
    ALTER TABLE dbo.ProfileUpdatePeriods
        ADD ScopeType VARCHAR(20) NOT NULL CONSTRAINT DF_ProfileUpdatePeriods_ScopeType DEFAULT ('ALL');
END;
GO

IF OBJECT_ID('dbo.ProfileUpdatePeriodFields', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProfileUpdatePeriodFields (
        PeriodId BIGINT NOT NULL,
        FieldKey VARCHAR(50) NOT NULL,
        CONSTRAINT PK_ProfileUpdatePeriodFields PRIMARY KEY (PeriodId, FieldKey),
        CONSTRAINT FK_ProfileUpdatePeriodFields_Period FOREIGN KEY (PeriodId)
            REFERENCES dbo.ProfileUpdatePeriods(PeriodId) ON DELETE CASCADE
    );
END;
GO

IF OBJECT_ID('dbo.ProfileUpdatePeriodEmployees', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProfileUpdatePeriodEmployees (
        PeriodId BIGINT NOT NULL,
        EmployeeId INT NOT NULL,
        CONSTRAINT PK_ProfileUpdatePeriodEmployees PRIMARY KEY (PeriodId, EmployeeId),
        CONSTRAINT FK_ProfileUpdatePeriodEmployees_Period FOREIGN KEY (PeriodId)
            REFERENCES dbo.ProfileUpdatePeriods(PeriodId) ON DELETE CASCADE,
        CONSTRAINT FK_ProfileUpdatePeriodEmployees_Employee FOREIGN KEY (EmployeeId)
            REFERENCES dbo.Employees(EmployeeId) ON DELETE CASCADE
    );
END;
GO

IF OBJECT_ID('dbo.ProfileUpdatePeriodLogs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProfileUpdatePeriodLogs (
        LogId BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        PeriodId BIGINT NOT NULL,
        CompanyId INT NOT NULL,
        EmployeeId INT NOT NULL,
        FieldKey VARCHAR(50) NOT NULL,
        FieldLabel NVARCHAR(100) NOT NULL,
        OldValue NVARCHAR(500) NULL,
        NewValue NVARCHAR(500) NULL,
        UpdatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_ProfileUpdatePeriodLogs_UpdatedAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT FK_ProfileUpdatePeriodLogs_Period FOREIGN KEY (PeriodId)
            REFERENCES dbo.ProfileUpdatePeriods(PeriodId) ON DELETE CASCADE,
        CONSTRAINT FK_ProfileUpdatePeriodLogs_Employee FOREIGN KEY (EmployeeId)
            REFERENCES dbo.Employees(EmployeeId) ON DELETE CASCADE
    );
    CREATE INDEX IX_ProfileUpdatePeriodLogs_Period
        ON dbo.ProfileUpdatePeriodLogs(CompanyId, PeriodId, EmployeeId, UpdatedAt DESC);
END;
GO

IF OBJECT_ID('dbo.CompanyDocumentTypes', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CompanyDocumentTypes (
        DocumentTypeId INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        CompanyId INT NOT NULL,
        TypeName NVARCHAR(100) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_CompanyDocumentTypes_IsActive DEFAULT (1),
        CreatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_CompanyDocumentTypes_CreatedAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT FK_CompanyDocumentTypes_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId),
        CONSTRAINT UQ_CompanyDocumentTypes_Company_Name UNIQUE (CompanyId, TypeName)
    );
END;
GO

IF OBJECT_ID('dbo.DocumentUpdatePeriods', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DocumentUpdatePeriods (
        DocumentPeriodId BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        CompanyId INT NOT NULL,
        Title NVARCHAR(150) NOT NULL,
        StartsAt DATETIME2(7) NOT NULL,
        EndsAt DATETIME2(7) NOT NULL,
        ScopeType VARCHAR(20) NOT NULL CONSTRAINT DF_DocumentUpdatePeriods_ScopeType DEFAULT ('ALL'),
        ClosedAt DATETIME2(7) NULL,
        CreatedByUserId INT NOT NULL,
        CreatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_DocumentUpdatePeriods_CreatedAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT FK_DocumentUpdatePeriods_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId),
        CONSTRAINT FK_DocumentUpdatePeriods_Users FOREIGN KEY (CreatedByUserId) REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_DocumentUpdatePeriods_Dates CHECK (EndsAt > StartsAt),
        CONSTRAINT CK_DocumentUpdatePeriods_Scope CHECK (ScopeType IN ('ALL', 'SELECTED'))
    );
    CREATE INDEX IX_DocumentUpdatePeriods_CompanyDates
        ON dbo.DocumentUpdatePeriods(CompanyId, ClosedAt, StartsAt, EndsAt);
END;
GO

IF OBJECT_ID('dbo.DocumentUpdatePeriodTypes', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DocumentUpdatePeriodTypes (
        DocumentPeriodId BIGINT NOT NULL,
        DocumentTypeId INT NOT NULL,
        CONSTRAINT PK_DocumentUpdatePeriodTypes PRIMARY KEY (DocumentPeriodId, DocumentTypeId),
        CONSTRAINT FK_DocumentUpdatePeriodTypes_Period FOREIGN KEY (DocumentPeriodId)
            REFERENCES dbo.DocumentUpdatePeriods(DocumentPeriodId) ON DELETE CASCADE,
        CONSTRAINT FK_DocumentUpdatePeriodTypes_Type FOREIGN KEY (DocumentTypeId)
            REFERENCES dbo.CompanyDocumentTypes(DocumentTypeId) ON DELETE CASCADE
    );
END;
GO

IF OBJECT_ID('dbo.DocumentUpdatePeriodEmployees', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.DocumentUpdatePeriodEmployees (
        DocumentPeriodId BIGINT NOT NULL,
        EmployeeId INT NOT NULL,
        CONSTRAINT PK_DocumentUpdatePeriodEmployees PRIMARY KEY (DocumentPeriodId, EmployeeId),
        CONSTRAINT FK_DocumentUpdatePeriodEmployees_Period FOREIGN KEY (DocumentPeriodId)
            REFERENCES dbo.DocumentUpdatePeriods(DocumentPeriodId) ON DELETE CASCADE,
        CONSTRAINT FK_DocumentUpdatePeriodEmployees_Employee FOREIGN KEY (EmployeeId)
            REFERENCES dbo.Employees(EmployeeId) ON DELETE CASCADE
    );
END;
GO

IF COL_LENGTH('dbo.EmployeeDocuments', 'DocumentPeriodId') IS NULL
BEGIN
    ALTER TABLE dbo.EmployeeDocuments ADD DocumentPeriodId BIGINT NULL;
END;
GO

IF OBJECT_ID('dbo.UserNotificationReads', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.UserNotificationReads (
        CompanyId INT NOT NULL,
        UserId INT NOT NULL,
        NotificationKey VARCHAR(160) NOT NULL,
        ReadAt DATETIME2(7) NOT NULL CONSTRAINT DF_UserNotificationReads_ReadAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_UserNotificationReads PRIMARY KEY (CompanyId, UserId, NotificationKey)
    );
END;
GO

-- ========================================================================
-- CẤU HÌNH CÔNG THỨC TÍNH LƯƠNG THEO CÔNG TY & CỘT CHI TIẾT PHIẾU LƯƠNG
-- ========================================================================
IF OBJECT_ID('dbo.CompanyPayrollSettings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CompanyPayrollSettings (
        CompanyId INT NOT NULL PRIMARY KEY,
        SalaryCalculationMode VARCHAR(25) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_CalcMode DEFAULT ('WORKDAYS'),
        StandardDaysMode VARCHAR(25) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_StdMode DEFAULT ('MON_FRI'),
        FixedStandardDays DECIMAL(5,2) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_FixedDays DEFAULT (22.00),
        WeekdayWeights VARCHAR(40) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_WeekdayWeights DEFAULT ('1,1,1,1,1,0,0'),
        IncludePaidLeave BIT NOT NULL CONSTRAINT DF_CompanyPayrollSettings_PaidLeave DEFAULT (1),
        IncludeResponsibilityAllowance BIT NOT NULL CONSTRAINT DF_CompanyPayrollSettings_RespAllow DEFAULT (1),
        IncludeTravelAllowance BIT NOT NULL CONSTRAINT DF_CompanyPayrollSettings_TravelAllow DEFAULT (1),
        UpdatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_UpdatedAt DEFAULT (SYSUTCDATETIME()),
        UpdatedByUserId INT NULL,
        CONSTRAINT FK_CompanyPayrollSettings_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
        CONSTRAINT CK_CompanyPayrollSettings_CalcMode CHECK (SalaryCalculationMode IN ('WORKDAYS', 'FIXED_MONTHLY')),
        CONSTRAINT CK_CompanyPayrollSettings_StdMode CHECK (StandardDaysMode IN ('MON_FRI', 'MON_SAT_HALF', 'MON_SAT', 'CUSTOM_WEEKDAYS', 'FIXED')),
        CONSTRAINT CK_CompanyPayrollSettings_FixedDays CHECK (FixedStandardDays >= 1 AND FixedStandardDays <= 31)
    );
END;
IF COL_LENGTH('dbo.CompanyPayrollSettings', 'WeekdayWeights') IS NULL
BEGIN
    ALTER TABLE dbo.CompanyPayrollSettings ADD WeekdayWeights VARCHAR(40) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_WeekdayWeights DEFAULT ('1,1,1,1,1,0,0');
END;
GO

IF COL_LENGTH('dbo.Payslips', 'ContractBaseSalary') IS NULL
BEGIN
    ALTER TABLE dbo.Payslips ADD
        ContractBaseSalary DECIMAL(18, 2) NOT NULL CONSTRAINT DF_Payslips_ContractBase DEFAULT (0),
        ActualWorkDays DECIMAL(5, 2) NOT NULL CONSTRAINT DF_Payslips_ActualDays DEFAULT (0),
        StandardWorkDays DECIMAL(5, 2) NOT NULL CONSTRAINT DF_Payslips_StandardDays DEFAULT (22),
        TravelAllowance DECIMAL(18, 2) NOT NULL CONSTRAINT DF_Payslips_TravelAllow DEFAULT (0);
    ALTER TABLE dbo.Payslips DROP COLUMN NetSalary;
    ALTER TABLE dbo.Payslips ADD NetSalary AS (BaseSalary + ResponsibilityAllowance + TravelAllowance + Bonus - Deduction);
END;
GO
