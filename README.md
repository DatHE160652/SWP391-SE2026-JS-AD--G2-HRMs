# HRMs – HR Management Platform for SMEs

## Team

**Group 2**

| Member | Student ID |
| --- | --- |
| Nguyen Hoang Bach | HE190498 |
| Dang Hoang Son | HE180524 |
| Pham Ngoc Anh | HE180850 |
| Nguyen Huy Hoang | HE181330 |
| Hoang Le Dat | HE160652 |

## 1. Project Overview

HRMs is a cloud-based, B2B SaaS platform that helps small and medium-sized enterprises (SMEs) manage employees throughout their active employment period.

The platform centralizes employee information and supports five core workflows:

- Attendance management
- Leave management
- Task management
- Custom performance management
- Payroll management

HRMs is designed to reduce manual administrative work, improve visibility into employee operations, and allow each company to configure processes that match its own policies.
---

## Business Model

HRMs follows a **B2B (Business-to-Business)** model:

```
HRMs Provider
      |
      v
SME Customer
      |
      v
Employee Work Management Services
```

Businesses register for the platform, choose a service plan, create a company workspace, configure their organization, add existing employees, and use the available modules.

---

## 2. Project Scope

### In Scope

The system supports employees who have already been hired and are actively working in an organization.

- Company registration and workspace creation
- Service plan selection, renewal, and upgrade
- Organization configuration
- Employee profile management
- Department and position management
- Attendance management
- Leave management
- Task management
- Configurable performance management
- Payroll management
- Reports and analytics

### Out of Scope

The initial version does not cover:

- Recruitment and candidate management
- Interview management and candidate selection
- Pre-employment onboarding
- Employment contract signing
- Resignation and termination procedures
- Exit interviews
- Complex legal payroll calculations, including tax and social insurance

---

## 3. User Roles

### System Admin

Manages the overall HRMs platform:

- Manage companies, accounts, service plans, and subscriptions
- Monitor platform activity
- View platform-level reports

### Company Admin / HR

Manages employee-related operations within a company workspace:

- Configure departments, positions, and organization structure
- Manage employee profiles, attendance, and leave policies
- Configure performance frameworks
- Manage payroll
- View reports and analytics

### Department Manager

Supervises employees within a department or team:

- Assign tasks and monitor progress
- Approve or reject leave requests
- Review employee performance
- View team attendance and performance

### Employee

Uses the platform for daily work-related activities:

- Check in and check out
- View attendance records and leave balances
- Submit leave requests
- View and update assigned tasks
- View performance results and payslips

---

## 4. Core Supporting Module

### Employee Management

Employee Management is a shared supporting module used by every main workflow. It stores the employee data required by attendance, leave, tasks, performance, and payroll.

Key capabilities:

- Add and update employee information
- Assign departments, positions, and managers
- Set employment status
- Create employee accounts
- View employee profiles

Supported employee statuses:

```text
Active
On Leave
Inactive
```

HRMs stores and updates the employee's current status; it does not manage recruitment or termination workflows.

---

## 5. Core Workflows

### 5.1 Attendance Management

Tracks employee working time and attendance status.

```text
Employee
   |
   v
Check In
   |
   v
Working
   |
   v
Check Out
   |
   v
Calculate Working Hours
   |
   v
Generate Attendance Status
```

Key capabilities:

- Check in and check out
- View attendance history
- Calculate working hours and overtime
- Record late arrivals, early departures, and absences
- Allow HR to adjust attendance records

Possible statuses: `Present`, `Late`, `Absent`, `On Leave`, `Early Leave`, and `Overtime`.

### 5.2 Leave Management

Allows employees to request leave and managers to approve or reject requests.

```text
Employee
   |
   v
Create Leave Request
   |
   v
Select Leave Type and Dates
   |
   v
Check Leave Balance
   |
   v
Manager Reviews Request
   |
   +--> Approved --> Update Balance --> Notify Employee
   |
   +--> Rejected ---------------------> Notify Employee
```

Key capabilities:

- Submit and review leave requests
- Configure leave types and company policies
- Check and update leave balances
- Approve or reject requests
- View leave history

### 5.3 Task Management

Tracks work assigned to employees from creation through completion.

```text
Manager
   |
   v
Create and Assign Task
   |
   v
Set Priority and Deadline
   |
   v
Employee Updates Progress
   |
   v
Submit Task
   |
   v
Manager Reviews and Completes Task
```

Task statuses:

```text
To Do
In Progress
Pending Review
Completed
Overdue
```

Key capabilities:

- Create, assign, and prioritize tasks
- Set deadlines and update progress
- Review submitted work
- Track overdue tasks and task history

### 5.4 Custom Performance Management

Each company can configure its own performance framework instead of using a fixed KPI model.

```text
Create Framework
      |
      v
Define Criteria, Weights, and Rating Scale
      |
      v
Assign Framework to Departments or Positions
      |
      v
Start Evaluation Cycle
      |
      v
Manager Evaluates Employee
      |
      v
Calculate Score and Review
      |
      v
Finalize Evaluation
      |
      v
Employee Views Result
```

Key capabilities:

- Create frameworks, criteria, and rating scales
- Set criterion weights
- Assign frameworks by department or position
- Create evaluation cycles
- Calculate, review, finalize, and track evaluations

Example weighted criteria:

| Department | Criteria |
| --- | --- |
| IT | Task Completion (40%), Work Quality (30%), Attendance (10%), Teamwork (20%) |
| Sales | Sales Revenue (50%), New Customers (20%), Customer Feedback (15%), Discipline (15%) |

Example calculation:

```text
Final Performance Score = Sum(Criterion Score x Weight)
```

### 5.5 Payroll Management

Calculates employee pay using employee information and work-related data.

```text
Employee Information
        +
Attendance and Leave
        +
Overtime, Bonuses, and Deductions
        |
        v
Payroll Calculation
        |
        v
HR Review and Approval
        |
        v
Generate Payslip
        |
        v
Employee Views Payslip
```

Initial payroll formula:

```text
Net Salary = Base Salary + Overtime + Bonus - Deduction
```

Key capabilities:

- Configure base salaries
- Add overtime, bonuses, and deductions
- Calculate and review payroll
- Approve payroll and generate payslips
- View payroll history

---

## 6. Workflow Relationships

All workflows use employee data from the Employee Management module.

```text
Attendance + Leave --> Payroll
Task                --> Performance
```

Attendance and leave information can be used in payroll calculations, while task results can contribute to performance evaluations.

---

## 7. End-to-End Business Process

```text
Business Registration
          |
          v
Select Service Plan
          |
          v
Create Company Workspace
          |
          v
Configure Organization
          |
          v
Add Existing Employees
          |
          v
Use Employee Work Management Modules
          |
          v
View Reports and Analytics
          |
          v
Renew or Upgrade Subscription
```

---

## 8. Reports and Analytics

### Company Admin / HR Dashboard

- Total employees and employees by department
- Employees present, absent, or on leave
- Pending leave requests
- Completed and overdue tasks
- Payroll costs
- Performance results

### Manager Dashboard

- Team members and attendance
- Pending leave requests
- Tasks in progress, completed, or overdue
- Team performance

### Employee Dashboard

- Attendance summary
- Leave balance
- Assigned tasks and task progress
- Performance score
- Payslip information

---

## 9. Team Workflow Distribution

| Team Member | Assigned Workflow |
| --- | --- |
| Member 1 | Attendance Management |
| Member 2 | Leave Management |
| Member 3 | Task Management |
| Member 4 | Custom Performance Management |
| Member 5 | Payroll Management |

Employee Management is a shared core module maintained and used by the entire team.

---

## 10. Tools and Technologies

| Area | Tools | Purpose |
| --- | --- | --- |
| Requirements and design | Figma | UI/UX design, wireframes, mockups, and prototypes |
| Diagrams | Draw.io | BPMN, use case, ERD, activity, sequence, and architecture diagrams |
| Development | Visual Studio Code, Visual Studio | Source-code editing, application development, and debugging |
| Database | SQL Server Management Studio 2022 | Database design, querying, administration, and debugging |
| Testing | JUnit or NUnit | Unit testing, depending on the backend technology |
| API and web testing | Postman, Selenium | API testing and automated web testing |
| Project management | Jira | Task management, sprint planning, and issue tracking |
| Version control | Git, GitHub / Gitea | Repository hosting, branching, collaboration, and code review |
| Deployment | Docker | Application containerization and deployment |
| AI assistance | ChatGPT, Gemini | Requirement analysis, research, documentation, and development assistance |

---

## 11. Proposed Repository Structure

```text
HRMs/
├── frontend/
│   └── source code
├── backend/
│   └── source code and APIs
├── database/
│   ├── schema/
│   ├── scripts/
│   └── sample-data/
├── tests/
│   ├── unit/
│   ├── api/
│   └── automation/
├── docs/
│   ├── requirements/
│   ├── business-process/
│   ├── diagrams/
│   ├── database-design/
│   └── ui-ux/
├── docker/
│   └── configuration/
├── .gitignore
├── docker-compose.yml
└── README.md
```

---

## 12. Project Information

| Property | Value |
| --- | --- |
| Project name | HRMs — HR Management Platform for SMEs |
| Business model | B2B (Business-to-Business) |
| Project type | Web-based employee work management system |
| Target customers | Small and medium-sized enterprises (SMEs) |
| Platform model | SaaS |
| Repository | GitHub / Gitea |
| Status | In development |