package com.acadcore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AdminDbService {
    private static final String USERS_TABLE = "users";
    private static final String FACULTY_SCHEDULE_TABLE = "faculty_schedule";
    private static final String ROOMS_TABLE = "rooms";
    private static final String GENERATED_TIMETABLE_TABLE = "generated_timetable_runs";
    private static final String GENERATED_SEATING_TABLE = "generated_exam_seating_runs";
    private static final String FACULTY_DEADLINES_TABLE = "faculty_grading_deadlines";
    private static final String FACULTY_MAKEUP_TABLE = "faculty_makeup_requests";

    private AdminDbService() {
    }

    static Map<String, Object> getOverviewStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", Math.max(0, countUsers(null)));
        data.put("totalFaculty", Math.max(0, countUsers("FACULTY")));
        data.put("totalStudents", Math.max(0, countUsers("STUDENT")));
        data.put("totalRooms", countFirstAvailableCount(
                "SELECT COUNT(*) FROM " + ROOMS_TABLE,
                "SELECT COUNT(DISTINCT room_number) FROM " + FACULTY_SCHEDULE_TABLE));
        data.put("totalCourses", countFirstAvailableCount(
                "SELECT COUNT(*) FROM courses",
                "SELECT COUNT(DISTINCT course_code) FROM " + FACULTY_SCHEDULE_TABLE));
        return data;
    }

    static Map<String, Object> generateClashFreeTimetable(AcadcoreWebApp.TimetableRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        String semesterText = request == null ? null : request.semester();
        String batchText = request == null ? null : request.batch();

        List<TimetableSeedRow> rows = fetchTimetableSeedRows(batchText, semesterText);
        if (rows.isEmpty()) {
            response.put("success", false);
            response.put("message", "No timetable rows found for the selected filters.");
            return response;
        }

        AcademicSystem system = new AcademicSystem();
        Map<Integer, Faculty> facultyCache = new HashMap<>();
        Map<String, AcadClass> classCache = new HashMap<>();
        Map<Integer, Room> roomCache = new HashMap<>();
        int scheduledCount = 0;

        for (TimetableSeedRow row : rows) {
            try {
                Faculty faculty = facultyCache.computeIfAbsent(row.facultyId, AdminDbService::loadFaculty);
                faculty.addAvailableSlot(row.toTimeSlot());

                Room room = roomCache.computeIfAbsent(row.roomNumber, key -> new Room(row.roomNumber, row.roomCapacity));
                AcadClass acadClass = classCache.computeIfAbsent(row.classKey(), key -> createClass(row));
                Course course = new Course(row.courseCode, row.courseName, row.creditHours);
                ScheduledClass scheduledClass = new ScheduledClass(course, faculty, room, row.toTimeSlot(), row.section, acadClass);

                system.timetable.addClass(scheduledClass);
                scheduledCount++;
            } catch (ScheduleConflictException | IllegalStateException ex) {
                response.put("success", false);
                response.put("message", "Timetable generation failed: " + ex.getMessage());
                return response;
            }
        }

        persistTimetableRun(batchText, semesterText, scheduledCount);
        response.put("success", true);
        response.put("message", "Generated clash-free timetable with " + scheduledCount + " scheduled classes.");
        response.put("scheduledCount", scheduledCount);
        return response;
    }

    static Map<String, Object> generateExamSeatingPlan(AcadcoreWebApp.ExamSeatingRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.date())) {
            response.put("success", false);
            response.put("message", "Exam date is required.");
            return response;
        }

        List<String> courses = splitCsv(request.courses());
        List<String> rooms = splitCsv(request.rooms());
        if (courses.isEmpty() || rooms.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please provide at least one course and one room.");
            return response;
        }

        List<Map<String, Object>> allocations = new ArrayList<>();
        int roomIndex = 0;
        for (String course : courses) {
            String room = rooms.get(roomIndex % rooms.size());
            allocations.add(Map.of(
                    "course", course,
                    "room", room,
                    "date", request.date(),
                    "seatStart", (roomIndex * 25) + 1,
                    "seatEnd", (roomIndex * 25) + 25
            ));
            roomIndex++;
        }

        persistSeatingRun(request.date(), request.courses(), request.rooms(), allocations.size());
        response.put("success", true);
        response.put("message", "Generated exam seating plan for " + allocations.size() + " course(s).");
        response.put("allocations", allocations);
        return response;
    }

    static List<Map<String, Object>> getFacultyWorkload() {
        String sql = "SELECT fs.faculty_id, COALESCE(u.name, CONCAT('Faculty ', fs.faculty_id)) AS faculty_name, " +
                "COUNT(*) AS total_lectures, COALESCE(SUM(fs.credit_hours), 0) AS total_credits, " +
                "SUM(CASE WHEN fs.day_of_week = 'Monday' THEN 1 ELSE 0 END) AS mon, " +
                "SUM(CASE WHEN fs.day_of_week = 'Tuesday' THEN 1 ELSE 0 END) AS tue, " +
                "SUM(CASE WHEN fs.day_of_week = 'Wednesday' THEN 1 ELSE 0 END) AS wed, " +
                "SUM(CASE WHEN fs.day_of_week = 'Thursday' THEN 1 ELSE 0 END) AS thu, " +
                "SUM(CASE WHEN fs.day_of_week = 'Friday' THEN 1 ELSE 0 END) AS fri " +
                "FROM " + FACULTY_SCHEDULE_TABLE + " fs LEFT JOIN " + USERS_TABLE + " u ON u.user_id = fs.faculty_id " +
                "GROUP BY fs.faculty_id, u.name ORDER BY faculty_name";
        return fetchRows(sql, statement -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("facultyId", rs.getInt("faculty_id"));
                    row.put("facultyName", rs.getString("faculty_name"));
                    row.put("totalLectures", rs.getInt("total_lectures"));
                    row.put("totalCredits", rs.getInt("total_credits"));
                    row.put("mon", rs.getInt("mon"));
                    row.put("tue", rs.getInt("tue"));
                    row.put("wed", rs.getInt("wed"));
                    row.put("thu", rs.getInt("thu"));
                    row.put("fri", rs.getInt("fri"));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    static List<Map<String, Object>> getRoomUtilization() {
        String sql = "SELECT r.room_number, COALESCE(r.capacity, 0) AS capacity, COUNT(fs.room_number) AS booked_slots " +
                "FROM " + ROOMS_TABLE + " r LEFT JOIN " + FACULTY_SCHEDULE_TABLE + " fs ON fs.room_number = r.room_number " +
                "GROUP BY r.room_number, r.capacity ORDER BY r.room_number";
        return fetchRows(sql, statement -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("roomNumber", rs.getInt("room_number"));
                    row.put("capacity", rs.getInt("capacity"));
                    row.put("bookedSlots", rs.getInt("booked_slots"));
                    row.put("utilization", calculateUtilization(rs.getInt("booked_slots"), rs.getInt("capacity")));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    static List<Map<String, Object>> getMaintenanceReports() {
        String sql = "SELECT room_number, COALESCE(capacity, 0) AS capacity, COALESCE(maintenance_note, '') AS maintenance_note " +
                "FROM " + ROOMS_TABLE + " WHERE COALESCE(maintenance_note, '') <> '' ORDER BY room_number";
        return fetchRows(sql, statement -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("roomNumber", rs.getInt("room_number"));
                    row.put("capacity", rs.getInt("capacity"));
                    row.put("note", rs.getString("maintenance_note"));
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    static Map<String, Object> persistFacultyDeadline(AcadcoreWebApp.FacultyDeadlineRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.course()) || isBlank(request.task()) || isBlank(request.deadline())) {
            response.put("success", false);
            response.put("message", "Course, task, and deadline are required.");
            return response;
        }

        executeUpdateSafely("INSERT INTO " + FACULTY_DEADLINES_TABLE + " (faculty_id, course_name, task_name, deadline, is_done) VALUES (0, ?, ?, ?, 0)", ps -> {
            ps.setString(1, request.course());
            ps.setString(2, request.task());
            ps.setString(3, request.deadline());
            ps.executeUpdate();
        });
        response.put("success", true);
        response.put("message", "Deadline saved.");
        return response;
    }

    static Map<String, Object> markFacultyDeadlineDone(AcadcoreWebApp.FacultyDeadlineDoneRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.course()) || isBlank(request.task())) {
            response.put("success", false);
            response.put("message", "Course and task are required.");
            return response;
        }

        executeUpdateSafely("UPDATE " + FACULTY_DEADLINES_TABLE + " SET is_done = 1 WHERE course_name = ? AND task_name = ?", ps -> {
            ps.setString(1, request.course());
            ps.setString(2, request.task());
            ps.executeUpdate();
        });
        response.put("success", true);
        response.put("message", "Deadline marked as done.");
        return response;
    }

    static Map<String, Object> persistMakeupRequest(AcadcoreWebApp.FacultyMakeupRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.courseCode()) || isBlank(request.reason())) {
            response.put("success", false);
            response.put("message", "Course code and reason are required.");
            return response;
        }

        executeUpdateSafely("INSERT INTO " + FACULTY_MAKEUP_TABLE + " (course_code, proposed_date, reason) VALUES (?, ?, ?)", ps -> {
            ps.setString(1, request.courseCode());
            ps.setString(2, request.proposedDate());
            ps.setString(3, request.reason());
            ps.executeUpdate();
        });
        response.put("success", true);
        response.put("message", "Makeup request saved.");
        return response;
    }

    private static List<TimetableSeedRow> fetchTimetableSeedRows(String batch, String semester) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fs.faculty_id, COALESCE(u.name, CONCAT('Faculty ', fs.faculty_id)) AS faculty_name, ");
        sql.append("COALESCE(u.email, CONCAT('faculty', fs.faculty_id, '@acadcore.local')) AS faculty_email, ");
        sql.append("COALESCE(u.password_hash, 'secret123') AS password_hash, fs.course_code, fs.course_name, fs.credit_hours, ");
        sql.append("fs.room_number, COALESCE(r.capacity, 50) AS room_capacity, fs.day_of_week, fs.start_hour, fs.start_minute, ");
        sql.append("fs.end_hour, fs.end_minute, fs.section, fs.batch_no, fs.major, fs.semester ");
        sql.append("FROM ").append(FACULTY_SCHEDULE_TABLE).append(" fs ");
        sql.append("LEFT JOIN ").append(USERS_TABLE).append(" u ON u.user_id = fs.faculty_id ");
        sql.append("LEFT JOIN ").append(ROOMS_TABLE).append(" r ON r.room_number = fs.room_number ");
        sql.append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (!isBlank(batch)) {
            sql.append("AND CAST(fs.batch_no AS CHAR) = ? ");
            params.add(batch.trim());
        }
        if (!isBlank(semester)) {
            sql.append("AND CAST(fs.semester AS CHAR) = ? ");
            params.add(semester.trim());
        }
        sql.append("ORDER BY fs.batch_no, fs.major, fs.section, fs.day_of_week, fs.start_hour, fs.start_minute");

        List<TimetableSeedRow> rows = fetchRows(sql.toString(), statement -> {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            List<TimetableSeedRow> queryRows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    queryRows.add(new TimetableSeedRow(
                            rs.getInt("faculty_id"),
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
            return queryRows;
        });
        return rows == null ? new ArrayList<>() : rows;
    }

    private static Faculty loadFaculty(int facultyId) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT user_id, name, email, password_hash FROM " + USERS_TABLE + " WHERE user_id = ?")) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String password = rs.getString("password_hash");
                    if (isBlank(password)) {
                        password = "secret123";
                    }
                    return new Faculty(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), password);
                }
            }
        } catch (SQLException | InvalidPasswordException ex) {
            // Fall back to a synthetic faculty object when the user table is not available.
        }

        try {
            return new Faculty(facultyId, "Faculty " + facultyId, "faculty" + facultyId + "@acadcore.local", "secret123");
        } catch (InvalidPasswordException ex) {
            throw new IllegalStateException("Unable to create faculty fallback.", ex);
        }
    }

    private static AcadClass createClass(TimetableSeedRow row) {
        try {
            return new AcadClass(row.batchNo, row.major, row.section, row.semester);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid class data for timetable generation.", ex);
        }
    }

    private static void persistTimetableRun(String batch, String semester, int scheduledCount) {
        executeUpdateSafely("INSERT INTO " + GENERATED_TIMETABLE_TABLE + " (batch_no, semester, scheduled_count) VALUES (?, ?, ?)", ps -> {
            ps.setString(1, batch);
            ps.setString(2, semester);
            ps.setInt(3, scheduledCount);
            ps.executeUpdate();
        });
    }

    private static void persistSeatingRun(String date, String courses, String rooms, int allocationCount) {
        executeUpdateSafely("INSERT INTO " + GENERATED_SEATING_TABLE + " (exam_date, courses, rooms, allocation_count) VALUES (?, ?, ?, ?)", ps -> {
            ps.setString(1, date);
            ps.setString(2, courses);
            ps.setString(3, rooms);
            ps.setInt(4, allocationCount);
            ps.executeUpdate();
        });
    }

    private static int countUsers(String role) {
        if (role == null) {
            return Math.max(0, countFirstAvailableCount("SELECT COUNT(*) FROM " + USERS_TABLE));
        }
        String sql = "SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE UPPER(role) = ?";
        return Math.max(0, fetchCount(sql, ps -> ps.setString(1, role)));
    }

    private static int countFirstAvailableCount(String... statements) {
        for (String sql : statements) {
            int count = fetchCount(sql, null);
            if (count >= 0) {
                return count;
            }
        }
        return 0;
    }

    private static int fetchCount(String sql, StatementConfigurer configurer) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (configurer != null) {
                configurer.configure(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            return -1;
        }
        return 0;
    }

    private static <T> T fetchRows(String sql, RowFetcher<T> fetcher) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            return fetcher.fetch(ps);
        } catch (SQLException ex) {
            try {
                return fetcher.empty();
            } catch (SQLException ignored) {
                throw new IllegalStateException(ignored);
            }
        }
    }

    private static void executeUpdateSafely(String sql, StatementConfigurer configurer) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            configurer.configure(ps);
        } catch (SQLException ignored) {
            // If the optional audit table is not present, keep the API responsive.
        }
    }

    private static List<String> splitCsv(String input) {
        List<String> values = new ArrayList<>();
        if (isBlank(input)) {
            return values;
        }
        for (String token : input.split(",")) {
            String value = token.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static double calculateUtilization(int bookedSlots, int capacity) {
        if (capacity <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (bookedSlots * 100.0) / Math.max(capacity, 1));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }

    private interface RowFetcher<T> {
        T fetch(PreparedStatement statement) throws SQLException;

        default T empty() throws SQLException {
            return null;
        }
    }

    private static final class TimetableSeedRow {
        private final int facultyId;
        private final int courseCode;
        private final String courseName;
        private final int creditHours;
        private final int roomNumber;
        private final int roomCapacity;
        private final String dayOfWeek;
        private final int startHour;
        private final int startMinute;
        private final int endHour;
        private final int endMinute;
        private final char section;
        private final int batchNo;
        private final String major;
        private final int semester;

        private TimetableSeedRow(int facultyId, int courseCode, String courseName, int creditHours, int roomNumber, int roomCapacity,
                                 String dayOfWeek, int startHour, int startMinute, int endHour, int endMinute,
                                 char section, int batchNo, String major, int semester) {
            this.facultyId = facultyId;
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

        private TimeSlot toTimeSlot() {
            return new TimeSlot(dayToChoice(dayOfWeek), startHour, startMinute, endHour, endMinute);
        }

        private String classKey() {
            return major + "-" + batchNo + section + "-" + semester;
        }
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
