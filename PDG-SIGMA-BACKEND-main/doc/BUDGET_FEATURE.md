Feature: Department/Program Budgeting for Monitorías

Endpoints
- GET /budget/{programName}/{semester}
  Returns: { program, semester, totalHours, usedHours, remainingHours }

- POST /budget/set
  Body: { programName: string, semester: string, totalHours: number }
  Sets or updates total available hours for a program in a semester.

Data Model
- Table department_budget(program_id, semester, total_hours)
- Monitoring now stores estimatedHours and hourlyRate for each monitoría.

Validation
- When creating a monitoring with estimatedHours > 0, the service validates that usedHours + estimatedHours <= totalHours for the program/semester. Otherwise, it returns an error.
