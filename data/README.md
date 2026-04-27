# AcadCore Data Folder

This folder is the only place where app state is persisted.

## Main file

- `acadcore-dummy-data.json` stores the sample accounts, role capabilities, timetable results, seating plans, deadlines, study groups, and requests.

## Test accounts

- Admin: `admin@acadcore.edu` / `admin123`
- Faculty: `faculty@acadcore.edu` / `faculty123`
- Student: `student@acadcore.edu` / `student123`

## Notes

- When the app is running, actions like login, timetable generation, deadline updates, makeup requests, and study-group joins update this JSON file.
- If you want the seed data reset, replace the JSON file with a clean copy before restarting the app.