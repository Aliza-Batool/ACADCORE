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
    private static final String FACULTY_TABLE = "faculty";
    private static final String STUDENTS_TABLE = "students";
    private static final String ACAD_CLASSES_TABLE = "acad_classes";
    private static final String COURSES_TABLE = "courses";
    private static final String STUDENT_CLASS_TABLE = "student_acad_classes";
    private static final String FACULTY_COURSES_TABLE = "faculty_courses";
    private static final List<ExamBlueprint> EXAM_BLUEPRINTS = new ArrayList<>();
    private static final Map<String, ExamDayPlan> EXAM_DAY_PLANS = new LinkedHashMap<>();

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
        ensureTimetableTables();
        Map<String, Object> response = new LinkedHashMap<>();
        String semesterText = normalizeSemesterFilter(request == null ? null : request.semester());
        String batchText = normalizeBatchFilter(request == null ? null : request.batch());

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
        List<Map<String, Object>> generatedRows = new ArrayList<>();
        clearGeneratedScheduledClasses(batchText, semesterText);

        for (TimetableSeedRow row : rows) {
            try {
                Faculty faculty = facultyCache.computeIfAbsent(row.facultyId, AdminDbService::loadFaculty);
                faculty.addAvailableSlot(row.toTimeSlot());

                Room room = roomCache.computeIfAbsent(row.roomNumber, key -> new Room(row.roomNumber, row.roomCapacity));
                AcadClass acadClass = classCache.computeIfAbsent(row.classKey(), key -> createClass(row));
                Course course = new Course(row.courseCode, row.courseName, row.creditHours);
                ScheduledClass scheduledClass = new ScheduledClass(course, faculty, room, row.toTimeSlot(), row.section, acadClass);

                system.timetable.addClass(scheduledClass);
                saveGeneratedScheduledClass(scheduledClass);
                scheduledCount++;
                generatedRows.add(Map.of(
                        "courseCode", row.courseCode,
                        "course", row.courseName,
                        "faculty", faculty.getName(),
                        "room", row.roomNumber,
                        "day", row.dayOfWeek,
                        "time", formatTime(row.startHour, row.startMinute) + " - " + formatTime(row.endHour, row.endMinute),
                        "batch", row.batchNo,
                        "semester", row.semester,
                        "major", row.major,
                        "section", String.valueOf(row.section)
                ));
            } catch (Exception ex) {
    ex.printStackTrace(); // 👈 shows real error in console

    response.put("success", false);
    response.put("message", "Timetable generation failed: " + ex.getMessage());
    return response;
}
        }

        persistTimetableRun(batchText, semesterText, scheduledCount);
        response.put("success", true);
        response.put("message", "Generated clash-free timetable with " + scheduledCount + " scheduled classes.");
        response.put("scheduledCount", scheduledCount);
        response.put("rows", generatedRows);
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
        Map<Integer, LinkedHashMap<String, Object>> aggregated = new LinkedHashMap<>();

        List<Map<String, Object>> scheduleRows = safeWorkloadRows(
            "SELECT fs.faculty_id, COALESCE(u.name, CONCAT('Faculty ', fs.faculty_id)) AS faculty_name, " +
                "COUNT(*) AS total_lectures, COALESCE(SUM(fs.credit_hours), 0) AS total_credits, " +
                "SUM(CASE WHEN fs.day_of_week = 'Monday' THEN 1 ELSE 0 END) AS mon, " +
                "SUM(CASE WHEN fs.day_of_week = 'Tuesday' THEN 1 ELSE 0 END) AS tue, " +
                "SUM(CASE WHEN fs.day_of_week = 'Wednesday' THEN 1 ELSE 0 END) AS wed, " +
                "SUM(CASE WHEN fs.day_of_week = 'Thursday' THEN 1 ELSE 0 END) AS thu, " +
                "SUM(CASE WHEN fs.day_of_week = 'Friday' THEN 1 ELSE 0 END) AS fri " +
                "FROM " + FACULTY_SCHEDULE_TABLE + " fs LEFT JOIN " + USERS_TABLE + " u ON u.user_id = fs.faculty_id " +
                "GROUP BY fs.faculty_id, u.name ORDER BY faculty_name");
        mergeFacultyWorkloadRows(aggregated, scheduleRows);

        List<Map<String, Object>> generatedRows = safeWorkloadRows(
            "SELECT sc.faculty_id, COALESCE(sc.faculty_name, CONCAT('Faculty ', sc.faculty_id)) AS faculty_name, " +
                "COUNT(*) AS total_lectures, COALESCE(SUM(sc.credit_hours), 0) AS total_credits, " +
                "SUM(CASE WHEN sc.day_of_week = 'Monday' THEN 1 ELSE 0 END) AS mon, " +
                "SUM(CASE WHEN sc.day_of_week = 'Tuesday' THEN 1 ELSE 0 END) AS tue, " +
                "SUM(CASE WHEN sc.day_of_week = 'Wednesday' THEN 1 ELSE 0 END) AS wed, " +
                "SUM(CASE WHEN sc.day_of_week = 'Thursday' THEN 1 ELSE 0 END) AS thu, " +
                "SUM(CASE WHEN sc.day_of_week = 'Friday' THEN 1 ELSE 0 END) AS fri " +
                "FROM scheduled_classes sc GROUP BY sc.faculty_id, sc.faculty_name ORDER BY faculty_name");
        mergeFacultyWorkloadRows(aggregated, generatedRows);

        if (aggregated.isEmpty()) {
            List<Map<String, Object>> facultyRows = fetchRows(
                    "SELECT user_id AS faculty_id, name AS faculty_name FROM " + USERS_TABLE + " WHERE UPPER(role) = 'FACULTY' ORDER BY name",
                    statement -> {
                        List<Map<String, Object>> rows = new ArrayList<>();
                        try (ResultSet rs = statement.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("facultyId", rs.getInt("faculty_id"));
                                row.put("facultyName", rs.getString("faculty_name"));
                                row.put("totalLectures", 0);
                                row.put("totalCredits", 0);
                                row.put("mon", 0);
                                row.put("tue", 0);
                                row.put("wed", 0);
                                row.put("thu", 0);
                                row.put("fri", 0);
                                rows.add(row);
                            }
                        }
                        return rows;
                    });
            return normalizeRows(facultyRows);
        }

        return new ArrayList<>(aggregated.values());
    }

    private static List<Map<String, Object>> safeWorkloadRows(String sql) {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
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
            return rows;
        } catch (SQLException ex) {
            return new ArrayList<>();
        }
    }

    private static List<Map<String, Object>> readFacultyWorkloadRows(java.sql.PreparedStatement statement) throws SQLException {
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
    }

    private static void mergeFacultyWorkloadRows(Map<Integer, LinkedHashMap<String, Object>> aggregated, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            int facultyId = asInt(row.get("facultyId"));
            LinkedHashMap<String, Object> target = aggregated.computeIfAbsent(facultyId, key -> new LinkedHashMap<>());
            target.put("facultyId", facultyId);
            target.put("facultyName", row.getOrDefault("facultyName", "Faculty " + facultyId));
            target.put("totalLectures", ((Number) target.getOrDefault("totalLectures", 0)).intValue() + asInt(row.get("totalLectures")));
            target.put("totalCredits", ((Number) target.getOrDefault("totalCredits", 0)).intValue() + asInt(row.get("totalCredits")));
            target.put("mon", ((Number) target.getOrDefault("mon", 0)).intValue() + asInt(row.get("mon")));
            target.put("tue", ((Number) target.getOrDefault("tue", 0)).intValue() + asInt(row.get("tue")));
            target.put("wed", ((Number) target.getOrDefault("wed", 0)).intValue() + asInt(row.get("wed")));
            target.put("thu", ((Number) target.getOrDefault("thu", 0)).intValue() + asInt(row.get("thu")));
            target.put("fri", ((Number) target.getOrDefault("fri", 0)).intValue() + asInt(row.get("fri")));
        }
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
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
        List<Map<String, Object>> rows = normalizeRows(fetchRows(sql, statement -> {
            List<Map<String, Object>> reportRows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("roomNumber", rs.getInt("room_number"));
                    row.put("capacity", rs.getInt("capacity"));
                    row.put("note", rs.getString("maintenance_note"));
                    reportRows.add(row);
                }
            }
            return reportRows;
        }));
        if (!rows.isEmpty()) {
            return rows;
        }

        String allRoomsSql = "SELECT room_number, COALESCE(capacity, 0) AS capacity FROM " + ROOMS_TABLE + " ORDER BY room_number";
        return normalizeRows(fetchRows(allRoomsSql, statement -> {
            List<Map<String, Object>> roomRows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("roomNumber", rs.getInt("room_number"));
                    row.put("capacity", rs.getInt("capacity"));
                    row.put("note", "No maintenance issue reported");
                    roomRows.add(row);
                }
            }
            return roomRows;
        }));
    }

    static Map<String, Object> addFaculty(AcadcoreWebApp.AdminUserRequest request) {
        return addUserByRole(request, "FACULTY");
    }

    static Map<String, Object> addStudent(AcadcoreWebApp.StudentCreateRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            response.put("success", false);
            response.put("message", "Name, email, and password are required.");
            return response;
        }
        if (request.classId() <= 0) {
            response.put("success", false);
            response.put("message", "Class ID is required when creating a student.");
            return response;
        }
        if (request.password().trim().length() < 6) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters.");
            return response;
        }

        String normalizedEmail = request.email().trim();
        int emailCount = fetchCount("SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE LOWER(email) = LOWER(?)", ps -> ps.setString(1, normalizedEmail));
        if (emailCount > 0) {
            response.put("success", false);
            response.put("message", "A user with this email already exists.");
            return response;
        }

        AcadClassRow acadClass = findAcadClassById(request.classId());
        if (acadClass == null) {
            response.put("success", false);
            response.put("message", "Acad class not found for the given Class ID.");
            return response;
        }

        ensureStudentClassTable();
        int currentClassStrength = fetchCount(
                "SELECT COUNT(*) FROM " + STUDENT_CLASS_TABLE + " WHERE batch_no = ? AND major = ? AND section = ? AND semester = ?",
                ps -> {
                    ps.setInt(1, acadClass.batchNo);
                    ps.setString(2, acadClass.major);
                    ps.setString(3, acadClass.section);
                    ps.setInt(4, acadClass.semester);
                });
        if (acadClass.maxStudents > 0 && currentClassStrength >= acadClass.maxStudents) {
            response.put("success", false);
            response.put("message", "Class is full. Max students: " + acadClass.maxStudents);
            return response;
        }

        int userId = generateNextUserId();
        String normalizedName = request.name().trim();
        String normalizedPassword = request.password().trim();

        try (Connection con = Db.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement userInsert = con.prepareStatement(
                        "INSERT INTO " + USERS_TABLE + " (user_id, role, name, email, password_hash) VALUES (?, ?, ?, ?, ?)")) {
                    userInsert.setInt(1, userId);
                    userInsert.setString(2, "STUDENT");
                    userInsert.setString(3, normalizedName);
                    userInsert.setString(4, normalizedEmail);
                    userInsert.setString(5, normalizedPassword);
                    userInsert.executeUpdate();
                }

                try (PreparedStatement studentInsert = con.prepareStatement(
                        "INSERT INTO " + STUDENTS_TABLE + " (user_id, name, email, password_hash, batch_no, major, section, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    studentInsert.setInt(1, userId);
                    studentInsert.setString(2, normalizedName);
                    studentInsert.setString(3, normalizedEmail);
                    studentInsert.setString(4, normalizedPassword);
                    studentInsert.setInt(5, acadClass.batchNo);
                    studentInsert.setString(6, acadClass.major);
                    studentInsert.setString(7, acadClass.section);
                    studentInsert.setInt(8, acadClass.semester);
                    studentInsert.executeUpdate();
                } catch (SQLException ex) {
                    // Backward-compatible fallback when students table has no class columns.
                    try (PreparedStatement studentInsertFallback = con.prepareStatement(
                            "INSERT INTO " + STUDENTS_TABLE + " (user_id, name, email, password_hash) VALUES (?, ?, ?, ?)")) {
                        studentInsertFallback.setInt(1, userId);
                        studentInsertFallback.setString(2, normalizedName);
                        studentInsertFallback.setString(3, normalizedEmail);
                        studentInsertFallback.setString(4, normalizedPassword);
                        studentInsertFallback.executeUpdate();
                    }
                }

                try (PreparedStatement classLinkInsert = con.prepareStatement(
                        "INSERT INTO " + STUDENT_CLASS_TABLE + " (student_id, batch_no, major, section, semester) VALUES (?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE batch_no = VALUES(batch_no), major = VALUES(major), section = VALUES(section), semester = VALUES(semester)")) {
                    classLinkInsert.setInt(1, userId);
                    classLinkInsert.setInt(2, acadClass.batchNo);
                    classLinkInsert.setString(3, acadClass.major);
                    classLinkInsert.setString(4, acadClass.section);
                    classLinkInsert.setInt(5, acadClass.semester);
                    classLinkInsert.executeUpdate();
                }

                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            response.put("success", false);
            response.put("message", "Failed to create student with class assignment: " + ex.getMessage());
            return response;
        }

        response.put("success", true);
        response.put("message", "Student created and assigned to acad class successfully.");
        response.put("userId", userId);
        response.put("classId", acadClass.classId);
        response.put("batchNo", acadClass.batchNo);
        response.put("major", acadClass.major);
        response.put("section", acadClass.section);
        response.put("semester", acadClass.semester);
        return response;
    }

    static Map<String, Object> addStudentToAcadClass(AcadcoreWebApp.StudentClassAssignRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || request.studentId() <= 0 || request.classId() <= 0) {
            response.put("success", false);
            response.put("message", "Student ID and Class ID are required.");
            return response;
        }

        int studentCount = fetchCount("SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE user_id = ? AND UPPER(role) = 'STUDENT'", ps -> ps.setInt(1, request.studentId()));
        if (studentCount <= 0) {
            response.put("success", false);
            response.put("message", "Student user not found.");
            return response;
        }

        AcadClassRow acadClass = findAcadClassById(request.classId());
        if (acadClass == null) {
            response.put("success", false);
            response.put("message", "Acad class not found for the given Class ID.");
            return response;
        }

        int alreadyAssignedToClass = fetchCount(
                "SELECT COUNT(*) FROM " + STUDENT_CLASS_TABLE + " WHERE student_id = ? AND batch_no = ? AND major = ? AND section = ? AND semester = ?",
                ps -> {
                    ps.setInt(1, request.studentId());
                    ps.setInt(2, acadClass.batchNo);
                    ps.setString(3, acadClass.major);
                    ps.setString(4, acadClass.section);
                    ps.setInt(5, acadClass.semester);
                });
        if (alreadyAssignedToClass <= 0) {
            int currentClassStrength = fetchCount(
                    "SELECT COUNT(*) FROM " + STUDENT_CLASS_TABLE + " WHERE batch_no = ? AND major = ? AND section = ? AND semester = ?",
                    ps -> {
                        ps.setInt(1, acadClass.batchNo);
                        ps.setString(2, acadClass.major);
                        ps.setString(3, acadClass.section);
                        ps.setInt(4, acadClass.semester);
                    });
            if (acadClass.maxStudents > 0 && currentClassStrength >= acadClass.maxStudents) {
                response.put("success", false);
                response.put("message", "Class is full. Max students: " + acadClass.maxStudents);
                return response;
            }
        }

        ensureStudentClassTable();
        try {
            executeRequiredUpdate(
                    "INSERT INTO " + STUDENT_CLASS_TABLE + " (student_id, batch_no, major, section, semester) VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE batch_no = VALUES(batch_no), major = VALUES(major), section = VALUES(section), semester = VALUES(semester)",
                    ps -> {
                        ps.setInt(1, request.studentId());
                        ps.setInt(2, acadClass.batchNo);
                        ps.setString(3, acadClass.major);
                        ps.setString(4, acadClass.section);
                        ps.setInt(5, acadClass.semester);
                    });

            executeUpdateSafely(
                    "UPDATE " + STUDENTS_TABLE + " SET batch_no = ?, major = ?, section = ?, semester = ? WHERE user_id = ?",
                    ps -> {
                        ps.setInt(1, acadClass.batchNo);
                        ps.setString(2, acadClass.major);
                        ps.setString(3, acadClass.section);
                        ps.setInt(4, acadClass.semester);
                        ps.setInt(5, request.studentId());
                        ps.executeUpdate();
                    });
        } catch (SQLException ex) {
            response.put("success", false);
            response.put("message", "Failed to assign student to class: " + ex.getMessage());
            return response;
        }

        response.put("success", true);
        response.put("message", "Student assigned to acad class successfully.");
        response.put("classId", acadClass.classId);
        response.put("batchNo", acadClass.batchNo);
        response.put("major", acadClass.major);
        response.put("section", acadClass.section);
        response.put("semester", acadClass.semester);
        return response;
    }

    private static AcadClassRow findAcadClassById(int classId) {
        String sql = "SELECT class_id, batch_no, major, section, semester, COALESCE(max_students, 0) AS max_students FROM " + ACAD_CLASSES_TABLE + " WHERE class_id = ?";
        return fetchRows(sql, statement -> {
            statement.setInt(1, classId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new AcadClassRow(
                            rs.getInt("class_id"),
                            rs.getInt("batch_no"),
                            rs.getString("major"),
                            normalizeSection(rs.getString("section")),
                            rs.getInt("semester"),
                            rs.getInt("max_students")
                    );
                }
            }
            return null;
        });
    }

    static Map<String, Object> assignFacultyToCourse(AcadcoreWebApp.FacultyCourseAssignRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || request.facultyId() <= 0 || isBlank(request.courseCode()) || isBlank(request.courseName())) {
            response.put("success", false);
            response.put("message", "Faculty, course code, and course name are required.");
            return response;
        }

        int facultyCount = fetchCount("SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE user_id = ? AND UPPER(role) = 'FACULTY'", ps -> ps.setInt(1, request.facultyId()));
        if (facultyCount <= 0) {
            response.put("success", false);
            response.put("message", "Faculty user not found.");
            return response;
        }

        ensureCourseExists(request.courseCode().trim(), request.courseName().trim(), Math.max(1, request.creditHours()));
        ensureFacultyCoursesTable();
        try {
            executeRequiredUpdate(
                    "INSERT INTO " + FACULTY_COURSES_TABLE + " (faculty_id, course_code, course_name, credit_hours) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), credit_hours = VALUES(credit_hours)",
                    ps -> {
                        ps.setInt(1, request.facultyId());
                        ps.setString(2, request.courseCode().trim());
                        ps.setString(3, request.courseName().trim());
                        ps.setInt(4, Math.max(1, request.creditHours()));
                    });
        } catch (SQLException ex) {
            response.put("success", false);
            response.put("message", "Failed to assign faculty to course: " + ex.getMessage());
            return response;
        }

        response.put("success", true);
        response.put("message", "Faculty assigned to course successfully.");
        return response;
    }

    static Map<String, Object> addFacultyWorkslot(AcadcoreWebApp.FacultyWorkslotRequest request) {
        ensureTimetableTables();
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || request.facultyId() <= 0 || isBlank(request.courseCode()) || isBlank(request.courseName()) || isBlank(request.dayOfWeek())) {
            response.put("success", false);
            response.put("message", "Faculty, course, and day are required.");
            return response;
        }

        executeUpdateSafely("INSERT INTO " + FACULTY_SCHEDULE_TABLE + " (faculty_id, course_code, course_name, credit_hours, room_number, day_of_week, start_hour, start_minute, end_hour, end_minute, section, batch_no, major, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setInt(1, request.facultyId());
            ps.setString(2, request.courseCode().trim());
            ps.setString(3, request.courseName().trim());
            ps.setInt(4, Math.max(1, request.creditHours()));
            ps.setInt(5, Math.max(0, request.roomNumber()));
            ps.setString(6, normalizeDay(request.dayOfWeek()));
            ps.setInt(7, clampHour(request.startHour()));
            ps.setInt(8, clampMinute(request.startMinute()));
            ps.setInt(9, clampHour(request.endHour()));
            ps.setInt(10, clampMinute(request.endMinute()));
            ps.setString(11, normalizeSection(request.section()));
            ps.setInt(12, Math.max(0, request.batchNo()));
            ps.setString(13, defaultText(request.major(), "General"));
            ps.setInt(14, Math.max(1, request.semester()));
            ps.executeUpdate();
        });

        response.put("success", true);
        response.put("message", "Faculty workslot added.");
        return response;
    }

    static Map<String, Object> createExam(AcadcoreWebApp.ExamCreateRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.examCode()) || isBlank(request.examName())) {
            response.put("success", false);
            response.put("message", "Exam code and exam name are required.");
            return response;
        }

        String code = request.examCode().trim().toUpperCase();
        for (ExamBlueprint blueprint : EXAM_BLUEPRINTS) {
            if (blueprint.examCode.equals(code)) {
                response.put("success", false);
                response.put("message", "Exam already exists.");
                return response;
            }
        }

        EXAM_BLUEPRINTS.add(new ExamBlueprint(code, request.examName().trim(), Math.max(1, request.totalStudents())));
        response.put("success", true);
        response.put("message", "Exam created.");
        response.put("exams", getExamCatalog());
        return response;
    }

    static Map<String, Object> createExamDay(AcadcoreWebApp.ExamDayCreateRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.examDate())) {
            response.put("success", false);
            response.put("message", "Exam date is required.");
            return response;
        }

        String dayKey = request.examDate().trim();
        List<String> rooms = splitCsv(request.roomsCsv());
        if (rooms.isEmpty()) {
            response.put("success", false);
            response.put("message", "Provide at least one room for the day.");
            return response;
        }

        EXAM_DAY_PLANS.put(dayKey, new ExamDayPlan(dayKey, rooms));
        response.put("success", true);
        response.put("message", "Exam day created.");
        response.put("day", toExamDayMap(EXAM_DAY_PLANS.get(dayKey)));
        return response;
    }

    static Map<String, Object> addExamToDay(AcadcoreWebApp.ExamScheduleAddRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.examDate()) || isBlank(request.examCode())) {
            response.put("success", false);
            response.put("message", "Exam day and exam code are required.");
            return response;
        }

        ExamDayPlan plan = EXAM_DAY_PLANS.get(request.examDate().trim());
        if (plan == null) {
            response.put("success", false);
            response.put("message", "Exam day not found. Create exam day first.");
            return response;
        }

        ExamBlueprint selectedExam = null;
        String code = request.examCode().trim().toUpperCase();
        for (ExamBlueprint blueprint : EXAM_BLUEPRINTS) {
            if (blueprint.examCode.equals(code)) {
                selectedExam = blueprint;
                break;
            }
        }
        if (selectedExam == null) {
            response.put("success", false);
            response.put("message", "Exam not found. Create exam first.");
            return response;
        }

        for (ExamBlueprint exam : plan.exams) {
            if (exam.examCode.equals(selectedExam.examCode)) {
                response.put("success", false);
                response.put("message", "Exam already added to this day.");
                return response;
            }
        }

        plan.exams.add(selectedExam);
        response.put("success", true);
        response.put("message", "Exam added to day schedule.");
        response.put("day", toExamDayMap(plan));
        return response;
    }

    static Map<String, Object> generateExamDayWise(AcadcoreWebApp.ExamGenerateRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.examDate())) {
            response.put("success", false);
            response.put("message", "Exam day is required.");
            return response;
        }

        ExamDayPlan plan = EXAM_DAY_PLANS.get(request.examDate().trim());
        if (plan == null) {
            response.put("success", false);
            response.put("message", "Exam day not found.");
            return response;
        }
        if (plan.exams.isEmpty()) {
            response.put("success", false);
            response.put("message", "Add exams to the day before generating seating.");
            return response;
        }

        plan.generatedAllocations.clear();
        int roomIdx = 0;
        for (ExamBlueprint exam : plan.exams) {
            String room = plan.rooms.get(roomIdx % plan.rooms.size());
            int seatEnd = Math.max(1, exam.totalStudents);
            Map<String, Object> allocation = new LinkedHashMap<>();
            allocation.put("examCode", exam.examCode);
            allocation.put("examName", exam.examName);
            allocation.put("examDate", plan.examDate);
            allocation.put("room", room);
            allocation.put("seatStart", 1);
            allocation.put("seatEnd", seatEnd);
            allocation.put("totalStudents", exam.totalStudents);
            plan.generatedAllocations.add(allocation);
            roomIdx++;
        }
        plan.generated = true;

        persistSeatingRun(plan.examDate, stringifyExamCodes(plan.exams), String.join(",", plan.rooms), plan.generatedAllocations.size());
        response.put("success", true);
        response.put("message", "Generated day-wise seating for " + plan.generatedAllocations.size() + " exam(s).");
        response.put("day", toExamDayMap(plan));
        return response;
    }

    static Map<String, Object> getExamSchedulingSnapshot() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("exams", getExamCatalog());
        List<Map<String, Object>> dayPlans = new ArrayList<>();
        for (ExamDayPlan plan : EXAM_DAY_PLANS.values()) {
            dayPlans.add(toExamDayMap(plan));
        }
        response.put("days", dayPlans);
        return response;
    }

    static Map<String, Object> persistFacultyDeadline(AcadcoreWebApp.FacultyDeadlineRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.course()) || isBlank(request.task()) || isBlank(request.deadline())) {
            response.put("success", false);
            response.put("message", "Course, task, and deadline are required.");
            return response;
        }

        int facultyId = fetchCount(
                "SELECT user_id FROM " + USERS_TABLE + " WHERE LOWER(email) = LOWER(?) AND UPPER(role) = 'FACULTY' LIMIT 1",
                ps -> ps.setString(1, isBlank(request.email()) ? "" : request.email().trim()));
        final int scopedFacultyId = Math.max(0, facultyId);

        executeUpdateSafely("INSERT INTO " + FACULTY_DEADLINES_TABLE + " (faculty_id, course_name, task_name, deadline, is_done) VALUES (?, ?, ?, ?, 0)", ps -> {
            ps.setInt(1, scopedFacultyId);
            ps.setString(2, request.course());
            ps.setString(3, request.task());
            ps.setString(4, request.deadline());
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

        int facultyId = fetchCount(
                "SELECT user_id FROM " + USERS_TABLE + " WHERE LOWER(email) = LOWER(?) AND UPPER(role) = 'FACULTY' LIMIT 1",
                ps -> ps.setString(1, isBlank(request.email()) ? "" : request.email().trim()));
        final int scopedFacultyId = Math.max(0, facultyId);

        executeUpdateSafely("UPDATE " + FACULTY_DEADLINES_TABLE + " SET is_done = 1 WHERE faculty_id = ? AND course_name = ? AND task_name = ? AND is_done = 0", ps -> {
            ps.setInt(1, scopedFacultyId);
            ps.setString(2, request.course());
            ps.setString(3, request.task());
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

    private static Map<String, Object> addUserByRole(AcadcoreWebApp.AdminUserRequest request, String role) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (request == null || isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            response.put("success", false);
            response.put("message", "Name, email, and password are required.");
            return response;
        }
        if (request.password().trim().length() < 6) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters.");
            return response;
        }

        String normalizedEmail = request.email().trim();
        int emailCount = fetchCount("SELECT COUNT(*) FROM " + USERS_TABLE + " WHERE LOWER(email) = LOWER(?)", ps -> ps.setString(1, normalizedEmail));
        if (emailCount > 0) {
            response.put("success", false);
            response.put("message", "A user with this email already exists.");
            return response;
        }

        int userId = generateNextUserId();
        String normalizedName = request.name().trim();
        String normalizedPassword = request.password().trim();
        try {
            executeRequiredUpdate("INSERT INTO " + USERS_TABLE + " (user_id, role, name, email, password_hash) VALUES (?, ?, ?, ?, ?)", ps -> {
                ps.setInt(1, userId);
                ps.setString(2, role);
                ps.setString(3, normalizedName);
                ps.setString(4, normalizedEmail);
                ps.setString(5, normalizedPassword);
            });
        } catch (SQLException ex) {
            response.put("success", false);
            response.put("message", "Failed to add user: " + ex.getMessage());
            return response;
        }

        mirrorUserIntoRoleTable(role, userId, normalizedName, normalizedEmail, normalizedPassword);

        response.put("success", true);
        response.put("message", role + " added successfully.");
        response.put("userId", userId);
        return response;
    }

    private static void mirrorUserIntoRoleTable(String role, int userId, String name, String email, String passwordHash) {
        if ("FACULTY".equalsIgnoreCase(role)) {
            executeUpdateSafely("INSERT INTO " + FACULTY_TABLE + " (user_id, name, email, password_hash) VALUES (?, ?, ?, ?)", ps -> {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.executeUpdate();
            });
            return;
        }

        if ("STUDENT".equalsIgnoreCase(role)) {
            executeUpdateSafely("INSERT INTO " + STUDENTS_TABLE + " (user_id, name, email, password_hash) VALUES (?, ?, ?, ?)", ps -> {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.executeUpdate();
            });
        }
    }

    private static List<TimetableSeedRow> fetchTimetableSeedRows(String batch, String semester) {
        List<TimetableSeedRow> rows = fetchTimetableSeedRowsFromTable(FACULTY_SCHEDULE_TABLE, batch, semester, true);
        if (!rows.isEmpty()) {
            return rows;
        }
        return fetchTimetableSeedRowsFromTable("scheduled_classes", batch, semester, false);
    }

    private static List<TimetableSeedRow> fetchTimetableSeedRowsFromTable(String sourceTable, String batch, String semester, boolean needsRoomJoin) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fs.faculty_id, COALESCE(u.name, CONCAT('Faculty ', fs.faculty_id)) AS faculty_name, ");
        sql.append("COALESCE(u.email, CONCAT('faculty', fs.faculty_id, '@acadcore.local')) AS faculty_email, ");
        sql.append("COALESCE(u.password_hash, 'secret123') AS password_hash, fs.course_code, fs.course_name, fs.credit_hours, ");
        if (needsRoomJoin) {
            sql.append("fs.room_number, COALESCE(r.capacity, 50) AS room_capacity, ");
        } else {
            sql.append("fs.room_number, COALESCE(fs.room_capacity, 50) AS room_capacity, ");
        }
        sql.append("fs.day_of_week, fs.start_hour, fs.start_minute, ");
        sql.append("fs.end_hour, fs.end_minute, fs.section, fs.batch_no, fs.major, fs.semester ");
        sql.append("FROM ").append(sourceTable).append(" fs ");
        sql.append("LEFT JOIN ").append(USERS_TABLE).append(" u ON u.user_id = fs.faculty_id ");
        if (needsRoomJoin) {
            sql.append("LEFT JOIN ").append(ROOMS_TABLE).append(" r ON r.room_number = fs.room_number ");
        }
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

    private static List<Map<String, Object>> getExamCatalog() {
        List<Map<String, Object>> exams = new ArrayList<>();
        for (ExamBlueprint exam : EXAM_BLUEPRINTS) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("examCode", exam.examCode);
            row.put("examName", exam.examName);
            row.put("totalStudents", exam.totalStudents);
            exams.add(row);
        }
        return exams;
    }

    private static Map<String, Object> toExamDayMap(ExamDayPlan plan) {
        Map<String, Object> day = new LinkedHashMap<>();
        day.put("examDate", plan.examDate);
        day.put("rooms", plan.rooms);
        day.put("generated", plan.generated);
        day.put("scheduledExams", stringifyExamCodes(plan.exams));
        day.put("allocations", plan.generatedAllocations);
        return day;
    }

    private static String stringifyExamCodes(List<ExamBlueprint> exams) {
        List<String> codes = new ArrayList<>();
        for (ExamBlueprint exam : exams) {
            codes.add(exam.examCode);
        }
        return String.join(",", codes);
    }

    private static int generateNextUserId() {
        int count = fetchCount("SELECT COALESCE(MAX(user_id), 1000) + 1 FROM " + USERS_TABLE, null);
        return count <= 0 ? 1001 : count;
    }

    private static String normalizeDay(String day) {
        String value = defaultText(day, "Monday").trim().toLowerCase();
        return switch (value) {
            case "tuesday" -> "Tuesday";
            case "wednesday" -> "Wednesday";
            case "thursday" -> "Thursday";
            case "friday" -> "Friday";
            default -> "Monday";
        };
    }

    private static int clampHour(int hour) {
        return Math.max(0, Math.min(23, hour));
    }

    private static int clampMinute(int minute) {
        return Math.max(0, Math.min(59, minute));
    }

    private static String normalizeSection(String section) {
        if (isBlank(section)) {
            return "A";
        }
        return section.trim().substring(0, 1).toUpperCase();
    }

    private static void ensureTimetableTables() {
        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + USERS_TABLE + " (" +
                        "user_id INT NOT NULL PRIMARY KEY, " +
                        "role VARCHAR(20) NOT NULL, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) NOT NULL UNIQUE, " +
                        "password_hash VARCHAR(255) NOT NULL" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + ROOMS_TABLE + " (" +
                        "room_number INT NOT NULL PRIMARY KEY, " +
                        "capacity INT NOT NULL DEFAULT 50, " +
                        "maintenance_note VARCHAR(500) DEFAULT ''" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + COURSES_TABLE + " (" +
                        "course_code INT NOT NULL PRIMARY KEY, " +
                        "course_name VARCHAR(255) NOT NULL, " +
                        "credit_hours INT NOT NULL DEFAULT 3" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + ACAD_CLASSES_TABLE + " (" +
                        "class_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "batch_no INT NOT NULL, " +
                        "major VARCHAR(64) NOT NULL, " +
                        "section CHAR(1) NOT NULL, " +
                        "semester INT NOT NULL, " +
                        "max_students INT NOT NULL DEFAULT 50, " +
                        "UNIQUE KEY uq_acad_class (batch_no, major, section, semester)" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + FACULTY_SCHEDULE_TABLE + " (" +
                        "schedule_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "faculty_id INT NOT NULL, " +
                        "course_code INT NOT NULL, " +
                        "course_name VARCHAR(255) NOT NULL, " +
                        "credit_hours INT NOT NULL DEFAULT 3, " +
                        "room_number INT NOT NULL DEFAULT 0, " +
                        "day_of_week VARCHAR(16) NOT NULL, " +
                        "start_hour INT NOT NULL, " +
                        "start_minute INT NOT NULL DEFAULT 0, " +
                        "end_hour INT NOT NULL, " +
                        "end_minute INT NOT NULL DEFAULT 0, " +
                        "section CHAR(1) NOT NULL DEFAULT 'A', " +
                        "batch_no INT NOT NULL DEFAULT 0, " +
                        "major VARCHAR(64) NOT NULL DEFAULT 'General', " +
                        "semester INT NOT NULL DEFAULT 1, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS scheduled_classes (" +
                        "scheduled_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "course_code INT NOT NULL, " +
                        "course_name VARCHAR(255) NOT NULL, " +
                        "credit_hours INT NOT NULL DEFAULT 3, " +
                        "faculty_id INT NOT NULL, " +
                        "faculty_name VARCHAR(255) NOT NULL, " +
                        "room_number INT NOT NULL, " +
                        "room_capacity INT NOT NULL DEFAULT 50, " +
                        "day_of_week VARCHAR(16) NOT NULL, " +
                        "start_hour INT NOT NULL, " +
                        "start_minute INT NOT NULL DEFAULT 0, " +
                        "end_hour INT NOT NULL, " +
                        "end_minute INT NOT NULL DEFAULT 0, " +
                        "section CHAR(1) NOT NULL, " +
                        "batch_no INT NOT NULL, " +
                        "major VARCHAR(64) NOT NULL, " +
                        "semester INT NOT NULL, " +
                        "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "UNIQUE KEY uq_scheduled_slot (faculty_id, course_code, batch_no, major, section, semester, day_of_week, start_hour, start_minute)" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + GENERATED_TIMETABLE_TABLE + " (" +
                        "run_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "batch_no VARCHAR(32), " +
                        "semester VARCHAR(32), " +
                        "scheduled_count INT NOT NULL DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                ps -> ps.executeUpdate());

        executeUpdateSafely(
                "INSERT IGNORE INTO " + ROOMS_TABLE + " (room_number, capacity, maintenance_note) VALUES (101, 50, '')",
                ps -> ps.executeUpdate());
    }

    private static void clearGeneratedScheduledClasses(String batch, String semester) {
        StringBuilder sql = new StringBuilder("DELETE FROM scheduled_classes WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (!isBlank(batch)) {
            sql.append("AND CAST(batch_no AS CHAR) = ? ");
            params.add(batch.trim());
        }
        if (!isBlank(semester)) {
            sql.append("AND CAST(semester AS CHAR) = ? ");
            params.add(semester.trim());
        }
        if (params.isEmpty()) {
            sql.append("AND generated_at IS NOT NULL ");
        }
        executeUpdateSafely(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        });
    }

    private static void saveGeneratedScheduledClass(ScheduledClass sc) {
        if (sc == null || sc.getCourse() == null || sc.getTeacher() == null || sc.getRoom() == null || sc.getSlot() == null || sc.getAcadClass() == null) {
            return;
        }
        String sql = "INSERT INTO scheduled_classes (course_code, course_name, credit_hours, faculty_id, faculty_name, room_number, room_capacity, " +
                "day_of_week, start_hour, start_minute, end_hour, end_minute, section, batch_no, major, semester) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), credit_hours = VALUES(credit_hours), faculty_name = VALUES(faculty_name), " +
                "room_number = VALUES(room_number), room_capacity = VALUES(room_capacity), end_hour = VALUES(end_hour), end_minute = VALUES(end_minute), generated_at = CURRENT_TIMESTAMP";
        executeUpdateSafely(sql, ps -> {
            ps.setInt(1, sc.getCourse().getCourseCode());
            ps.setString(2, sc.getCourse().getCourseName());
            ps.setInt(3, sc.getCourse().getCreditHours());
            ps.setInt(4, sc.getTeacher().getId());
            ps.setString(5, sc.getTeacher().getName());
            ps.setInt(6, sc.getRoom().getRoomNumber());
            ps.setInt(7, sc.getRoom().getCapacity());
            ps.setString(8, sc.getSlot().getDay());
            ps.setInt(9, sc.getSlot().getStartTime().getHour());
            ps.setInt(10, sc.getSlot().getStartTime().getMinute());
            ps.setInt(11, sc.getSlot().getEndTime().getHour());
            ps.setInt(12, sc.getSlot().getEndTime().getMinute());
            ps.setString(13, String.valueOf(sc.getSection()));
            ps.setInt(14, sc.getAcadClass().getBatchNo());
            ps.setString(15, sc.getAcadClass().getMajor());
            ps.setInt(16, sc.getAcadClass().getSemester());
            ps.executeUpdate();
        });
    }

    private static void ensureStudentClassTable() {
        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + STUDENT_CLASS_TABLE + " (" +
                        "student_id INT NOT NULL, " +
                        "batch_no INT NOT NULL, " +
                        "major VARCHAR(64) NOT NULL, " +
                        "section CHAR(1) NOT NULL, " +
                        "semester INT NOT NULL, " +
                        "assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (student_id, batch_no, major, section, semester)" +
                        ")",
                ps -> ps.executeUpdate());
    }

    private static void ensureFacultyCoursesTable() {
        executeUpdateSafely(
                "CREATE TABLE IF NOT EXISTS " + FACULTY_COURSES_TABLE + " (" +
                        "faculty_id INT NOT NULL, " +
                        "course_code VARCHAR(64) NOT NULL, " +
                        "course_name VARCHAR(255) NOT NULL, " +
                        "credit_hours INT NOT NULL DEFAULT 3, " +
                        "assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (faculty_id, course_code)" +
                        ")",
                ps -> ps.executeUpdate());
    }

    private static void ensureCourseExists(String courseCode, String courseName, int creditHours) {
        int existing = fetchCount("SELECT COUNT(*) FROM " + COURSES_TABLE + " WHERE CAST(course_code AS CHAR) = ?", ps -> ps.setString(1, courseCode));
        if (existing > 0) {
            return;
        }
        executeUpdateSafely(
                "INSERT INTO " + COURSES_TABLE + " (course_code, course_name, credit_hours) VALUES (?, ?, ?)",
                ps -> {
                    ps.setString(1, courseCode);
                    ps.setString(2, courseName);
                    ps.setInt(3, creditHours);
                    ps.executeUpdate();
                });
    }

    private static String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static String normalizeBatchFilter(String batch) {
    if (batch == null || batch.equalsIgnoreCase("All") || batch.isBlank()) {
        return null;
    }
    return batch;
}

    private static String normalizeSemesterFilter(String semester) {
    if (semester == null || semester.equalsIgnoreCase("All") || semester.isBlank()) {
        return null;
    }
    return semester;
}


    private static String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", clampHour(hour), clampMinute(minute));
    }

    private static List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
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

    int batch = (row.batchNo > 0) ? row.batchNo : 23;

    int semester = (row.semester >= 1 && row.semester <= 8)
            ? row.semester
            : 1;

    String major = (row.major != null && !row.major.isBlank())
            ? row.major
            : "CS";

    char section = (row.section != '\0' && row.section != ' ')
            ? row.section
            : 'A';

    return new AcadClass(batch, major, section, semester);
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

    private static void executeRequiredUpdate(String sql, StatementConfigurer configurer) throws SQLException {
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            configurer.configure(ps);
            int affectedRows = ps.executeUpdate();
            if (affectedRows <= 0) {
                throw new SQLException("No rows were affected by the insert operation.");
            }
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

    private static final class ExamBlueprint {
        private final String examCode;
        private final String examName;
        private final int totalStudents;

        private ExamBlueprint(String examCode, String examName, int totalStudents) {
            this.examCode = examCode;
            this.examName = examName;
            this.totalStudents = totalStudents;
        }
    }

    private static final class AcadClassRow {
        private final int classId;
        private final int batchNo;
        private final String major;
        private final String section;
        private final int semester;
        private final int maxStudents;

        private AcadClassRow(int classId, int batchNo, String major, String section, int semester, int maxStudents) {
            this.classId = classId;
            this.batchNo = batchNo;
            this.major = major;
            this.section = section;
            this.semester = semester;
            this.maxStudents = maxStudents;
        }
    }

    private static final class ExamDayPlan {
        private final String examDate;
        private final List<String> rooms;
        private final List<ExamBlueprint> exams;
        private final List<Map<String, Object>> generatedAllocations;
        private boolean generated;

        private ExamDayPlan(String examDate, List<String> rooms) {
            this.examDate = examDate;
            this.rooms = new ArrayList<>(rooms);
            this.exams = new ArrayList<>();
            this.generatedAllocations = new ArrayList<>();
            this.generated = false;
        }
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
    // CHANGED: New method — admin reads all faculty slots (read-only view).
// Faculty adds their own slots via /api/faculty/workslot/add.
static List<Map<String, Object>> getAllFacultyAvailableSlots() {
    String sql =
        "SELECT fs.faculty_id, " +
        "COALESCE(u.name, CONCAT('Faculty ', fs.faculty_id)) AS faculty_name, " +
        "fs.course_code, fs.course_name, fs.day_of_week, " +
        "fs.start_hour, fs.start_minute, fs.end_hour, fs.end_minute, " +
        "fs.room_number, fs.section, fs.batch_no, fs.major, fs.semester " +
        "FROM faculty_schedule fs " +
        "LEFT JOIN users u ON u.user_id = fs.faculty_id " +
        "ORDER BY fs.faculty_id, fs.day_of_week, fs.start_hour, fs.start_minute";

    List<Map<String, Object>> rows = new ArrayList<>();

    try (Connection con = Db.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("facultyId", rs.getInt("faculty_id"));
            row.put("facultyName", rs.getString("faculty_name"));
            row.put("courseCode", rs.getString("course_code"));
            row.put("courseName", rs.getString("course_name"));
            row.put("day", rs.getString("day_of_week"));

            row.put("time", String.format(
                    "%02d:%02d - %02d:%02d",
                    rs.getInt("start_hour"),
                    rs.getInt("start_minute"),
                    rs.getInt("end_hour"),
                    rs.getInt("end_minute")
            ));

            row.put("roomNumber", rs.getInt("room_number"));
            row.put("section", rs.getString("section"));
            row.put("batchNo", rs.getInt("batch_no"));
            row.put("major", rs.getString("major"));
            row.put("semester", rs.getInt("semester"));

            rows.add(row);
        }

    } catch (SQLException ex) {
        ex.printStackTrace();
        return List.of();
    }

    return rows;
}
private static void loadFacultyCoursesFromDB(Faculty f) {

    String sql = "SELECT course_code, course_name, credit_hours FROM faculty_schedule WHERE faculty_id = ?";

    try (Connection con = Db.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, f.getId());

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Course c = new Course(
                rs.getInt("course_code"),
                rs.getString("course_name"),
                rs.getInt("credit_hours")
            );

            try {
                f.assignCourse(c);   // ✅ THIS IS KEY
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
}
}

