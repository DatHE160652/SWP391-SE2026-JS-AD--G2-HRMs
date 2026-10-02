-- UC09 Assign Department to Employee
UPDATE Employees
SET DepartmentId = ?
WHERE EmployeeId = ?;

-- UC10 Assign Position to Employee
UPDATE Employees
SET PositionId = ?
WHERE EmployeeId = ?;

-- UC11 Assign Manager to Employee
UPDATE Employees
SET ManagerId = ?
WHERE EmployeeId = ?;

-- Load current organization information
SELECT EmployeeId, DepartmentId, PositionId, ManagerId
FROM Employees
WHERE EmployeeId = ?;

-- Validation queries
SELECT COUNT(*) FROM Employees WHERE EmployeeId = ?;
SELECT COUNT(*) FROM Departments WHERE DepartmentId = ?;
SELECT COUNT(*) FROM Positions WHERE PositionId = ?;
