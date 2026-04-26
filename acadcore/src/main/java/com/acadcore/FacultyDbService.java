package com.acadcore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class FacultyDbService {
    private static final String FACULTY_SCHEDULE_TABLE = "faculty_schedule";
    private static final String FACULTY_DEADLINES_TABLE = "faculty_grading_deadlines";
    private static final String FACULTY_ATTENDANCE_TABLE = "faculty_attendance_records";
    private static final String FACULTY_MAKEUP_TABLE = "faculty_makeup_requests";

    private FacultyDbService() {
    }

    static void hydrateFacultySchedule(Faculty faculty) throws SQLException {
        //
        // Rebuild the faculty object from the database before calling the existing display methods.
        faculty.clearLoadedSchedule();
        for (ScheduleSnapshot snapshot : fetchScheduleSnapshots(faculty.getId())) {
            faculty.loadScheduledClassFromDatabase(snapshot.toScheduledClass(faculty));
        }
    }

    static void hydrateFacultyDeadlines(Faculty faculty) throws SQLException {
        // Load active deadlines into the existing in-memory deadline manager.
        faculty.clearLoadedDeadlines();
        for (DeadlineSnapshot snapshot : fetchDeadlineSnapshots(faculty.getId())) {
            faculty.loadDeadlineFromDatabase(snapshot.toCourse(), snapshot.taskName, snapshot.deadline);
        }
    }

    static List<ScheduleSnapshot> fetchScheduleSnapshots(int facultyId) throws SQLException {
        String sql = "SELECT course_code, course_name, credit_hours, room_number, room_capacity, day_of_week, start_hour, start_minute, end_hour, end_minute, section, batch_no, major, semester "
                + "FROM " + FACULTY_SCHEDULE_TABLE + " WHERE faculty_id = ? ORDER BY batch_no, major, section, day_of_week, start_hour, start_minute";
        List<ScheduleSnapshot> snapshots = new ArrayList<>();

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    snapshots.add(new ScheduleSnapshot(
                            rs.getInt("course_code"),
                            rs.getString("course_name"),
                            rs.getInt("credit_hours"),
                            rs.getInt("room_number"),
                            rs.getInt("room_capacity"),
                            rs.getString("day_of_week"),
                            rs.getInt("start_hour"),
                            rs.getInt("start_minute"),
                            rs.getInt("end_hour"),
                            rs.getInt("end_minute"),
                            rs.getString("section").charAt(0),
                            rs.getInt("batch_no"),
                            rs.getString("major"),
                            rs.getInt("semester")
                    ));
                }
            }
        }

        return snapshots;
    }

    static List<DeadlineSnapshot> fetchDeadlineSnapshots(int facultyId) throws SQLException {
        String sql = "SELECT course_code, course_name, credit_hours, task_name, deadline "
                + "FROM " + FACULTY_DEADLINES_TABLE + " WHERE faculty_id = ? AND is_done = 0 ORDER BY deadline, course_name, task_name";
        List<DeadlineSnapshot> snapshots = new ArrayList<>();

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facultyId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    snapshots.add(new DeadlineSnapshot(
                            rs.getInt("course_code"),
                            rs.getString("course_name"),
                            rs.getInt("credit_hours"),
                            rs.getString("task_name"),
                            rs.getString("deadline")
                    ));
                }
            }
        }

        return snapshots;
    }

    static AttendanceContext loadAttendanceContext(Faculty faculty, ScheduleSnapshot snapshot) throws SQLException {
        if (faculty == null || snapshot == null) {
            return null;
        }

        AcadClass acadClass;
        Course course;
        Room room;
        TimeSlot slot;
        try {
            acadClass = new AcadClass(snapshot.batchNo, snapshot.major, snapshot.section, snapshot.semester);
            course = new Course(snapshot.courseCode, snapshot.courseName, snapshot.creditHours);
            room = new Room(snapshot.roomNumber, snapshot.roomCapacity);
            slot = snapshot.toTimeSlot();
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Unable to build attendance context from schedule data.", ex);
        }

        ScheduledClass scheduledClass = new ScheduledClass(course, faculty, room, slot, snapshot.section, acadClass);
        acadClass.addToClassSchedule(scheduledClass);

        String sql = "SELECT student_id, student_name, student_email, password_hash, attended_classes, total_classes "
                + "FROM " + FACULTY_ATTENDANCE_TABLE + " WHERE faculty_id = ? AND course_name = ? AND batch_no = ? AND major = ? AND section = ? AND semester = ? ORDER BY student_name";

        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, faculty.getId());
            ps.setString(2, snapshot.courseName);
            ps.setInt(3, snapshot.batchNo);
            ps.setString(4, snapshot.major);
            ps.setString(5, String.valueOf(snapshot.section));
            ps.setInt(6, snapshot.semester);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String password = rs.getString("password_hash");
                    if (password == null || password.isBlank()) {
                        password = "student123";
                    }

                    try {
                        Student student = new Student(
                                rs.getInt("student_id"),
                                rs.getString("student_name"),
                                rs.getString("student_email"),
                                password,
                                acadClass);
                        student.enrollCourse(course);

                        int attended = rs.getInt("attended_classes");
                        int total = rs.getInt("total_classes");
                        for (int i = 0; i < attended; i++) {
                            student.markAttendance(course.getCourseName(), true);
                        }
                        for (int i = attended; i < total; i++) {
                            student.markAttendance(course.getCourseName(), false);
                        }
                    } catch (InvalidPasswordException ex) {
                        throw new SQLException("Invalid password data for attendance snapshot.", ex);
                    } catch (CourseCapacityException ex) {
                        throw new SQLException("Unable to enroll attendance snapshot student.", ex);
                    }
                }
            }
        }

        return new AttendanceContext(acadClass, course);
    }

    static void insertDeadline(Faculty faculty, Course course, String taskName, String deadline) throws SQLException {
        // Write-through save: the deadline is already in memory, and this keeps the database in sync.
        String sql = "INSERT INTO " + FACULTY_DEADLINES_TABLE + " (faculty_id, course_code, course_name, credit_hours, task_name, deadline, is_done) VALUES (?, ?, ?, ?, ?, ?, 0)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, faculty.getId());
            ps.setInt(2, course.getCourseCode());
            ps.setString(3, course.getCourseName());
            ps.setInt(4, course.getCreditHours());
            ps.setString(5, taskName);
            ps.setString(6, deadline);
            ps.executeUpdate();
        }
    }

    static void markDeadlineDone(Faculty faculty, String courseName, String taskName) throws SQLException {
        // Mark the matching deadline row done after the in-memory list has been updated.
        String sql = "UPDATE " + FACULTY_DEADLINES_TABLE + " SET is_done = 1 WHERE faculty_id = ? AND course_name = ? AND task_name = ? AND is_done = 0";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, faculty.getId());
            ps.setString(2, courseName);
            ps.setString(3, taskName);
            ps.executeUpdate();
        }
    }

    static void insertMakeupRequest(Faculty faculty, String courseCode, String reason) throws SQLException {
        // Preserve the console message in Faculty while also persisting the request.
        String sql = "INSERT INTO " + FACULTY_MAKEUP_TABLE + " (faculty_id, course_code, reason, requested_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, faculty.getId());
            ps.setString(2, courseCode);
            ps.setString(3, reason);
            ps.executeUpdate();
        }
    }

    static final class ScheduleSnapshot {
        final int courseCode;
        final String courseName;
        final int creditHours;
        final int roomNumber;
        final int roomCapacity;
        final String dayOfWeek;
        final int startHour;
        final int startMinute;
        final int endHour;
        final int endMinute;
        final char section;
        final int batchNo;
        final String major;
        final int semester;

        ScheduleSnapshot(int courseCode, String courseName, int creditHours, int roomNumber, int roomCapacity,
                         String dayOfWeek, int startHour, int startMinute, int endHour, int endMinute,
                         char section, int batchNo, String major, int semester) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.creditHours = creditHours;
            this.roomNumber = roomNumber;
            this.roomCapacity = roomCapacity;
            this.dayOfWeek = dayOfWeek;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.endHour = endHour;
            this.endMinute = endMinute;
            this.section = section;
            this.batchNo = batchNo;
            this.major = major;
            this.semester = semester;
        }

        ScheduledClass toScheduledClass(Faculty faculty) throws SQLException {
            try {
                Course course = new Course(courseCode, courseName, creditHours);
                Room room = new Room(roomNumber, roomCapacity);
                TimeSlot slot = toTimeSlot();
                AcadClass acadClass = new AcadClass(batchNo, major, section, semester);
                return new ScheduledClass(course, faculty, room, slot, section, acadClass);
            } catch (IllegalArgumentException ex) {
                throw new SQLException("Invalid schedule data for " + courseName + ".", ex);
            }
        }

        TimeSlot toTimeSlot() {
            return new TimeSlot(dayToChoice(dayOfWeek), startHour, startMinute, endHour, endMinute);
        }

        String displayLabel() {
            return courseName + " | " + major + "-" + batchNo + section + " | " + dayOfWeek + " "
                    + twoDigits(startHour) + ":" + twoDigits(startMinute) + "-" + twoDigits(endHour) + ":" + twoDigits(endMinute)
                    + " | Room " + roomNumber;
        }
    }

    static final class DeadlineSnapshot {
        final int courseCode;
        final String courseName;
        final int creditHours;
        final String taskName;
        final String deadline;

        DeadlineSnapshot(int courseCode, String courseName, int creditHours, String taskName, String deadline) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.creditHours = creditHours;
            this.taskName = taskName;
            this.deadline = deadline;
        }

        Course toCourse() {
            return new Course(courseCode, courseName, creditHours);
        }
    }

    static final class AttendanceContext {
        private final AcadClass acadClass;
        private final Course course;

        AttendanceContext(AcadClass acadClass, Course course) {
            this.acadClass = acadClass;
            this.course = course;
        }

        AcadClass getAcadClass() {
            return acadClass;
        }

        Course getCourse() {
            return course;
        }
    }

    private static int dayToChoice(String day) {
        if (day == null) {
            return 1;
        }
        switch (day.trim().toLowerCase()) {
            case "monday":
                return 1;
            case "tuesday":
                return 2;
            case "wednesday":
                return 3;
            case "thursday":
                return 4;
            case "friday":
                return 5;
            default:
                return 1;
        }
    }

    private static String twoDigits(int value) {
        return String.format("%02d", value);
    }
}

final class RoleMenuRunner {
    private RoleMenuRunner() {
    }

    static void run(User user, java.util.Scanner sc) {
        if (user == null) {
            return;
        }
        if (user instanceof Faculty faculty) {
            // Faculty gets a dedicated menu that routes each option back through the existing methods.
            FacultyPortal.run(faculty, sc);
            return;
        }

        String userLabel = user == null ? "User" : user.getClass().getSimpleName();
        while (true) {
            System.out.println("\n── " + userLabel + " Menu ──");
            System.out.println("1. View Profile");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            if ("1".equals(choice)) {
                user.displayInfo();
            } else if ("2".equals(choice)) {
                return;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}

final class FacultyPortal {
    private FacultyPortal() {
    }

    static void run(Faculty faculty, java.util.Scanner sc) {
        try {
            // Load the latest data before presenting the menu so read actions always start from DB state.
            FacultyDbService.hydrateFacultySchedule(faculty);
            FacultyDbService.hydrateFacultyDeadlines(faculty);
        } catch (SQLException e) {
            System.out.println("Warning: unable to load some faculty data from the database: " + e.getMessage());
        }

        while (true) {
            System.out.println("\n── Faculty Menu for " + faculty.getName() + " ──");
            System.out.println("1. View Teaching Schedule");
            System.out.println("2. View Weekly Workload");
            System.out.println("3. View Grading Deadline Reminders");
            System.out.println("4. Mark a Deadline as Done");
            System.out.println("5. Request a Makeup Class");
            System.out.println("6. View At-Risk Students (Attendance Analytics)");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    refreshScheduleAndShow(faculty);
                    break;
                case "2":
                    refreshScheduleAndShowWorkload(faculty);
                    break;
                case "3":
                    refreshDeadlinesAndShow(faculty);
                    break;
                case "4":
                    refreshDeadlinesAndMarkDone(faculty, sc);
                    break;
                case "5":
                    requestMakeupClass(faculty, sc);
                    break;
                case "6":
                    viewAtRiskStudents(faculty, sc);
                    break;
                case "7":
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void refreshScheduleAndShow(Faculty faculty) {
        try {
            // Reload first, then reuse the existing schedule printer.
            FacultyDbService.hydrateFacultySchedule(faculty);
        } catch (SQLException e) {
            System.out.println("Failed to load teaching schedule: " + e.getMessage());
        }
        faculty.viewTeachingSchedule();
    }

    private static void refreshScheduleAndShowWorkload(Faculty faculty) {
        try {
            // Workload reporting uses the Faculty object, so refresh the schedule backing it.
            FacultyDbService.hydrateFacultySchedule(faculty);
        } catch (SQLException e) {
            System.out.println("Failed to load workload data: " + e.getMessage());
        }
        WorkloadService.reportWeeklyWorkload(faculty);
    }

    private static void refreshDeadlinesAndShow(Faculty faculty) {
        try {
            // Reload pending deadlines so the printed reminder list matches the saved rows.
            FacultyDbService.hydrateFacultyDeadlines(faculty);
        } catch (SQLException e) {
            System.out.println("Failed to load deadlines: " + e.getMessage());
        }
        faculty.getPendingDeadlines();
    }

    private static void refreshDeadlinesAndMarkDone(Faculty faculty, java.util.Scanner sc) {
        refreshDeadlinesAndShow(faculty);
        System.out.print("Course name: ");
        String course = sc.nextLine().trim();
        System.out.print("Task name: ");
        String task = sc.nextLine().trim();
        faculty.markDeadlineDone(course, task);
    }

    private static void requestMakeupClass(Faculty faculty, java.util.Scanner sc) {
        System.out.print("Course code: ");
        String courseCode = sc.nextLine().trim();
        System.out.print("Reason: ");
        String reason = sc.nextLine().trim();
        faculty.requestMakeup(courseCode, reason);
    }

    private static void viewAtRiskStudents(Faculty faculty, java.util.Scanner sc) {
        List<FacultyDbService.ScheduleSnapshot> snapshots;
        try {
            // Use the saved schedule to let faculty choose which class to inspect.
            snapshots = FacultyDbService.fetchScheduleSnapshots(faculty.getId());
        } catch (SQLException e) {
            System.out.println("Failed to load schedule options: " + e.getMessage());
            return;
        }

        if (snapshots.isEmpty()) {
            System.out.println("No scheduled classes found for at-risk analytics.");
            return;
        }

        System.out.println("\nSelect a class to inspect:");
        for (int i = 0; i < snapshots.size(); i++) {
            System.out.println((i + 1) + ". " + snapshots.get(i).displayLabel());
        }
        System.out.print("Choose a class: ");

        int selection;
        try {
            selection = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException ex) {
            System.out.println("Invalid selection.");
            return;
        }

        if (selection < 1 || selection > snapshots.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        try {
            // Build a temporary class + students from DB rows, then reuse the existing risk checker.
            FacultyDbService.AttendanceContext context = FacultyDbService.loadAttendanceContext(faculty, snapshots.get(selection - 1));
            if (context == null) {
                System.out.println("No attendance snapshot available for that class.");
                return;
            }
            faculty.showAtRiskStudents(context.getAcadClass(), context.getCourse().getCourseName());
        } catch (SQLException e) {
            System.out.println("Failed to load attendance analytics: " + e.getMessage());
        }
    }
}