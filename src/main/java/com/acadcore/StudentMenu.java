package com.acadcore;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("unused")
final class StudentMenu {
    private StudentMenu() {
    }

    static void run(Student student, Scanner sc, StudentDAO dao) {
        if (student == null || sc == null || dao == null) {
            return;
        }

        while (true) {
            System.out.println("\n── Student Menu for " + student.getName() + " ──");
            System.out.println("1. View Personal Timetable");
            System.out.println("2. Detect Course or Exam Clashes");
            System.out.println("3. Use Assignment Prioritizer");
            System.out.println("4. Get Attendance Risk Alerts");
            System.out.println("5. Join or Create Smart Study Group");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewTimetable(student, dao);
                case "2" -> detectClashes(student, dao);
                case "3" -> assignmentPrioritizer(student, sc, dao);
                case "4" -> attendanceRiskAlerts(student, dao);
                case "5" -> studyGroupMenu(student, sc, dao);
                case "6" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void viewTimetable(Student student, StudentDAO dao) {
        try {
            StudentDAO.AcademicClassRow row = firstClassRow(dao.getStudentClassLinks(student.getId()));
            if (row == null) {
                System.out.println("No class found for student.");
                return;
            }

            AcadClass acadClass = new AcadClass(row.batchNo, row.major, row.section, row.semester);
            student.setClass(acadClass);
            for (StudentDAO.ScheduledClassRow scheduledRow : dao.getScheduledClasses(student.getId())) {
                ScheduledClass scheduledClass = buildScheduledClass(scheduledRow, acadClass);
                if (scheduledClass != null) {
                    acadClass.addToClassSchedule(scheduledClass);
                }
            }
            student.viewMyClassSchedule();
        } catch (SQLException | IllegalArgumentException ex) {
            System.out.println("Failed to load timetable: " + ex.getMessage());
        }
    }

    private static void detectClashes(Student student, StudentDAO dao) {
        try {
            List<StudentDAO.ScheduledClassRow> rows = dao.getScheduledClasses(student.getId());
            TimeSlot[] slots = new TimeSlot[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                StudentDAO.ScheduledClassRow row = rows.get(i);
                slots[i] = new TimeSlot(dayToChoice(row.dayOfWeek), row.startHour, row.startMinute, row.endHour, row.endMinute);
            }
            student.detectClash(slots);
        } catch (SQLException | IllegalArgumentException ex) {
            System.out.println("Failed to detect clashes: " + ex.getMessage());
        }
    }

    private static void assignmentPrioritizer(Student student, Scanner sc, StudentDAO dao) {
        try {
            loadStudentCourses(student, dao);
            loadAssignments(student, dao);
            System.out.print("Add a new assignment? (y/n): ");
            if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                System.out.print("Course name: ");
                String courseName = sc.nextLine().trim();
                System.out.print("Title: ");
                String title = sc.nextLine().trim();
                System.out.print("Days until due: ");
                int daysUntilDue = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Weight percent: ");
                int weightPercent = Integer.parseInt(sc.nextLine().trim());
                Assignment assignment = new Assignment(courseName, title, daysUntilDue, weightPercent);
                student.addAssignment(assignment);
                dao.insertAssignment(student.getId(), assignment);
            }
            student.showPrioritizedAssignments();
        } catch (SQLException | NumberFormatException ex) {
            System.out.println("Failed to load assignments: " + ex.getMessage());
        }
    }

    private static void attendanceRiskAlerts(Student student, StudentDAO dao) {
        try {
            loadStudentCourses(student, dao);
            loadAttendance(student, dao);
            for (int i = 0; i < student.getCourseCount(); i++) {
                Course course = student.getEnrolledCourse(i);
                if (course != null && student.checkAttendanceRisks(course.getCourseName())) {
                    System.out.println("Attendance risk alert: " + course.getCourseName());
                }
            }
        } catch (SQLException ex) {
            System.out.println("Failed to load attendance data: " + ex.getMessage());
        }
    }

    private static void studyGroupMenu(Student student, Scanner sc, StudentDAO dao) {
        System.out.println("1. Create a group");
        System.out.println("2. Join a group");
        System.out.print("Choose an option: ");
        String choice = sc.nextLine().trim();

        try {
            if ("1".equals(choice)) {
                loadStudentCourses(student, dao);
                System.out.print("Course code: ");
                int courseCode = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Max size: ");
                int maxSize = Integer.parseInt(sc.nextLine().trim());
                Course course = findCourse(student, courseCode);
                if (course == null) {
                    System.out.println("Course not found in your enrollments.");
                    return;
                }
                StudyGroup group = new StudyGroup(course, maxSize, student);
                int groupId = dao.insertStudyGroup(group, student.getId());
                System.out.println(groupId > 0 ? "Study group created." : "Study group creation failed.");
            } else if ("2".equals(choice)) {
                loadStudentCourses(student, dao);
                List<StudentDAO.StudyGroupRow> groups = dao.getStudyGroups(student.getId());
                if (groups.isEmpty()) {
                    System.out.println("No available groups.");
                    return;
                }
                for (int i = 0; i < groups.size(); i++) {
                    StudentDAO.StudyGroupRow row = groups.get(i);
                    System.out.println((i + 1) + ". " + row.courseName + " | size limit " + row.maxSize + " | leader " + row.creatorName);
                }
                System.out.print("Choose a group: ");
                int index = Integer.parseInt(sc.nextLine().trim()) - 1;
                if (index < 0 || index >= groups.size()) {
                    System.out.println("Invalid selection.");
                    return;
                }
                StudentDAO.StudyGroupRow row = groups.get(index);
                Course course = findCourse(student, row.courseCode);
                if (course == null) {
                    System.out.println("You are not enrolled in that course.");
                    return;
                }
                Student leader = student;
                StudyGroup group = new StudyGroup(course, row.maxSize, leader);
                if (group.addMember(student)) {
                    dao.insertStudyGroupMember(row.groupId, student.getId());
                }
            }
        } catch (SQLException | NumberFormatException ex) {
            System.out.println("Study group action failed: " + ex.getMessage());
        }
    }

    private static void loadStudentCourses(Student student, StudentDAO dao) throws SQLException {
        for (StudentDAO.CourseRow row : dao.getStudentCourses(student.getId())) {
            Course course = new Course(row.courseCode, row.courseName, row.creditHours);
            try {
                student.enrollCourse(course);
            } catch (CourseCapacityException ex) {
                System.out.println("Skipping duplicate course enrollment: " + course.getCourseName());
            }
        }
    }

    private static void loadAssignments(Student student, StudentDAO dao) throws SQLException {
        for (StudentDAO.AssignmentRow row : dao.getAssignments(student.getId())) {
            student.addAssignment(new Assignment(row.courseName, row.title, row.daysUntilDue, row.weightPercent));
        }
    }

    private static void loadAttendance(Student student, StudentDAO dao) throws SQLException {
        for (StudentDAO.AttendanceRow row : dao.getAttendanceRecords(student.getId())) {
            Course course = findOrCreateCourse(student, row.courseName);
            try {
                student.enrollCourse(course);
            } catch (CourseCapacityException ex) {
                System.out.println("Skipping duplicate attendance course: " + course.getCourseName());
            }
            for (int i = 0; i < row.attendedClasses; i++) {
                student.markAttendance(course.getCourseName(), true);
            }
            for (int i = row.attendedClasses; i < row.totalClasses; i++) {
                student.markAttendance(course.getCourseName(), false);
            }
        }
    }

    private static Course findOrCreateCourse(Student student, String courseName) {
        for (int i = 0; i < student.getCourseCount(); i++) {
            Course course = student.getEnrolledCourse(i);
            if (course != null && course.getCourseName().equalsIgnoreCase(courseName)) {
                return course;
            }
        }
        return new Course(0, courseName, 3);
    }

    private static Course findCourse(Student student, int courseCode) {
        for (int i = 0; i < student.getCourseCount(); i++) {
            Course course = student.getEnrolledCourse(i);
            if (course != null && course.getCourseCode() == courseCode) {
                return course;
            }
        }
        return null;
    }

    private static ScheduledClass buildScheduledClass(StudentDAO.ScheduledClassRow row, AcadClass acadClass) {
        try {
            Course course = new Course(row.courseCode, row.courseName, row.creditHours);
            Room room = new Room(row.roomNumber, row.roomCapacity);
            TimeSlot slot = new TimeSlot(dayToChoice(row.dayOfWeek), row.startHour, row.startMinute, row.endHour, row.endMinute);
            Faculty faculty = new Faculty(0, "Faculty", "faculty@acadcore.local", "secret123");
            return new ScheduledClass(course, faculty, room, slot, row.section, acadClass);
        } catch (InvalidPasswordException ex) {
            System.out.println("Skipping schedule entry: " + ex.getMessage());
            return null;
        }
    }

    private static StudentDAO.AcademicClassRow firstClassRow(List<StudentDAO.AcademicClassRow> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static int dayToChoice(String day) {
        if (day == null) {
            return 1;
        }
        return switch (day.trim().toLowerCase()) {
            case "monday" -> 1;
            case "tuesday" -> 2;
            case "wednesday" -> 3;
            case "thursday" -> 4;
            case "friday" -> 5;
            default -> 1;
        };
    }

}