package com.acadcore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

final class AcadcoreWebApp {
    private AcadcoreWebApp() {
    }

    static void start(int port) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/";
                staticFiles.location = Location.CLASSPATH;
            });
        });
        registerRoutes(app);
        app.start(port);
        System.out.println("Open your browser at: http://localhost:" + port);
    }

    private static void registerRoutes(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/index.html"));

        app.post("/api/login", ctx -> ctx.json(handleLogin(ctx.bodyAsClass(LoginRequest.class))));
        app.post("/api/auth/login", ctx -> ctx.json(handleLogin(ctx.bodyAsClass(LoginRequest.class))));

        app.get("/api/admin/overview", ctx -> ctx.json(AdminDbService.getOverviewStats()));
        app.post("/api/admin/timetable/generate", ctx -> ctx.json(AdminDbService.generateClashFreeTimetable(ctx.bodyAsClass(TimetableRequest.class))));
        app.post("/api/admin/exam/seating/generate", ctx -> ctx.json(AdminDbService.generateExamSeatingPlan(ctx.bodyAsClass(ExamSeatingRequest.class))));
        app.post("/api/admin/faculty/add", ctx -> ctx.json(AdminDbService.addFaculty(ctx.bodyAsClass(AdminUserRequest.class))));
        app.post("/api/admin/student/add", ctx -> ctx.json(AdminDbService.addStudent(ctx.bodyAsClass(StudentCreateRequest.class))));
        app.post("/api/admin/class/student/add", ctx -> ctx.json(AdminDbService.addStudentToAcadClass(ctx.bodyAsClass(StudentClassAssignRequest.class))));
        app.post("/api/admin/course/faculty/assign", ctx -> ctx.json(AdminDbService.assignFacultyToCourse(ctx.bodyAsClass(FacultyCourseAssignRequest.class))));
        app.post("/api/faculty/workslot/add", ctx -> ctx.json(AdminDbService.addFacultyWorkslot(ctx.bodyAsClass(FacultyWorkslotRequest.class))));
        app.post("/api/faculty/workslot/add-self", ctx -> ctx.json(addFacultyWorkslotSelf(ctx.bodyAsClass(FacultySelfWorkslotRequest.class))));
        app.get("/api/admin/faculty/slots", ctx -> ctx.json(AdminDbService.getAllFacultyAvailableSlots()));
        app.post("/api/admin/exam/create", ctx -> ctx.json(AdminDbService.createExam(ctx.bodyAsClass(ExamCreateRequest.class))));
        app.post("/api/admin/exam/day/create", ctx -> ctx.json(AdminDbService.createExamDay(ctx.bodyAsClass(ExamDayCreateRequest.class))));
        app.post("/api/admin/exam/day/add", ctx -> ctx.json(AdminDbService.addExamToDay(ctx.bodyAsClass(ExamScheduleAddRequest.class))));
        app.post("/api/admin/exam/day/generate", ctx -> ctx.json(AdminDbService.generateExamDayWise(ctx.bodyAsClass(ExamGenerateRequest.class))));
        app.get("/api/admin/exam/snapshot", ctx -> ctx.json(AdminDbService.getExamSchedulingSnapshot()));
        app.get("/api/admin/workload", ctx -> ctx.json(AdminDbService.getFacultyWorkload()));
        app.get("/api/admin/rooms/utilization", ctx -> ctx.json(AdminDbService.getRoomUtilization()));
        app.get("/api/admin/maintenance", ctx -> ctx.json(AdminDbService.getMaintenanceReports()));
        app.get("/api/admin/makeup", ctx -> ctx.json(loadMakeupRequests()));
        app.get("/api/admin/makeup-requests", ctx -> ctx.json(loadMakeupRequests()));

        app.post("/api/faculty/deadline/set", ctx -> {
            FacultyDeadlineRequest request = ctx.bodyAsClass(FacultyDeadlineRequest.class);
            ctx.json(AdminDbService.persistFacultyDeadline(request));
        });
        app.post("/api/faculty/deadline/done", ctx -> {
            FacultyDeadlineDoneRequest request = ctx.bodyAsClass(FacultyDeadlineDoneRequest.class);
            ctx.json(AdminDbService.markFacultyDeadlineDone(request));
        });
        app.post("/api/faculty/makeup", ctx -> {
            FacultyMakeupRequest request = ctx.bodyAsClass(FacultyMakeupRequest.class);
            ctx.json(AdminDbService.persistMakeupRequest(request));
        });
        app.post("/api/faculty/maintenance/report", ctx -> {
            FacultyMaintenanceRequest request = ctx.bodyAsClass(FacultyMaintenanceRequest.class);
            ctx.json(reportFacultyMaintenance(request));
        });
        app.post("/api/faculty/maintenance", ctx -> {
            FacultyMaintenanceRequest request = ctx.bodyAsClass(FacultyMaintenanceRequest.class);
            ctx.json(reportFacultyMaintenance(request));
        });
        app.get("/api/faculty/overview", ctx -> ctx.json(loadFacultyOverview(ctx.queryParam("email"))));
        app.get("/api/faculty/deadlines", ctx -> ctx.json(loadFacultyDeadlines(ctx.queryParam("email"))));
        app.get("/api/faculty/myslots", ctx -> ctx.json(loadFacultySlots(ctx.queryParam("email"))));
        
        app.get("/api/student/assignments", ctx -> ctx.json(loadStudentAssignments(ctx.queryParam("email"))));
        app.post("/api/student/assignments/add", ctx -> ctx.json(addStudentAssignment(ctx.bodyAsClass(StudentAssignmentRequest.class))));
        app.post("/api/student/assignments/remove", ctx -> ctx.json(removeStudentAssignment(ctx.bodyAsClass(StudentAssignmentRemoveRequest.class))));
        app.get("/api/student/study-groups", ctx -> ctx.json(loadStudyGroups(ctx.queryParam("email"))));
        app.get("/api/student/study-groups/members", ctx -> {
            int groupId = Integer.parseInt(ctx.queryParam("groupId") != null ? ctx.queryParam("groupId") : "0");
            ctx.json(loadGroupMembers(groupId));
        });
        app.post("/api/student/study-groups/join", ctx -> ctx.json(joinStudyGroup(ctx.bodyAsClass(StudentGroupJoinRequest.class))));
        app.get("/api/student/timetable", ctx -> ctx.json(loadStudentTimetable(ctx.queryParam("email"))));
    }

    private static Map<String, Object> handleLogin(LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            return Map.of("success", false, "message", "Email and password are required.");
        }

        try {
            User user = UserDao.findUserByEmail(request.email());
            if (user == null || !user.login(request.email(), request.password())) {
                return Map.of("success", false, "message", "Invalid email or password.");
            }

            String role;
            if (user instanceof Admin) {
                role = "admin";
            } else if (user instanceof Faculty) {
                role = "faculty";
            } else {
                role = "student";
            }
            return Map.of(
                    "success", true,
                    "message", "Login successful.",
                    "name", user.getName(),
                    "role", role,
                    "userId", user.getId()
            );
        } catch (SQLException ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    private static Integer findUserIdByEmail(String email, String role) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT user_id FROM users WHERE LOWER(email) = LOWER(?) AND UPPER(role) = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, role.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private static List<Map<String, Object>> loadStudentAssignments(String email) {
        Integer studentId = findUserIdByEmail(email, "STUDENT");
        if (studentId == null) {
            return List.of();
        }

        String sql = "SELECT course_name, title, days_until_due, weight_percent FROM assignments WHERE student_id = ? ORDER BY days_until_due, title";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseName", rs.getString("course_name"));
                    row.put("title", rs.getString("title"));
                    row.put("daysUntilDue", rs.getInt("days_until_due"));
                    row.put("weightPercent", rs.getInt("weight_percent"));
                    rows.add(row);
                }
            }
        } catch (SQLException ignored) {
            return List.of();
        }
        return rows;
    }

    private static Map<String, Object> addStudentAssignment(StudentAssignmentRequest request) {
        if (request == null || request.email() == null || request.email().isBlank() || request.courseName() == null || request.courseName().isBlank() || request.title() == null || request.title().isBlank()) {
            return Map.of("success", false, "message", "Email, course name, and title are required.");
        }
        Integer studentId = findUserIdByEmail(request.email(), "STUDENT");
        if (studentId == null) {
            return Map.of("success", false, "message", "Student account not found.");
        }
        String sql = "INSERT INTO assignments (student_id, course_name, title, days_until_due, weight_percent) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, request.courseName().trim());
            ps.setString(3, request.title().trim());
            ps.setInt(4, Math.max(1, request.daysUntilDue()));
            ps.setInt(5, Math.max(1, Math.min(100, request.weightPercent())));
            ps.executeUpdate();
            return Map.of("success", true, "message", "Assignment added.");
        } catch (SQLException ex) {
            return Map.of("success", false, "message", "Failed to add assignment: " + ex.getMessage());
        }
    }

    private static Map<String, Object> removeStudentAssignment(StudentAssignmentRemoveRequest request) {
        if (request == null || request.email() == null || request.email().isBlank() || request.courseName() == null || request.courseName().isBlank() || request.title() == null || request.title().isBlank()) {
            return Map.of("success", false, "message", "Email, course name, and title are required.");
        }
        Integer studentId = findUserIdByEmail(request.email(), "STUDENT");
        if (studentId == null) {
            return Map.of("success", false, "message", "Student account not found.");
        }
        String sql = "DELETE FROM assignments WHERE student_id = ? AND course_name = ? AND title = ? LIMIT 1";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, request.courseName().trim());
            ps.setString(3, request.title().trim());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                return Map.of("success", true, "message", "Assignment removed.");
            }
            return Map.of("success", false, "message", "Assignment not found.");
        } catch (SQLException ex) {
            return Map.of("success", false, "message", "Failed to remove assignment: " + ex.getMessage());
        }
    }

    private static Map<String, Object> loadStudyGroups(String email) {
        Integer studentId = findUserIdByEmail(email, "STUDENT");
        if (studentId == null) {
            return Map.of("groups", List.of());
        }

        String sql = "SELECT g.group_id, g.course_code, g.course_name, g.max_size, g.creator_name, COUNT(sgm.student_id) AS member_count "
                + "FROM study_groups g LEFT JOIN study_group_members sgm ON sgm.group_id = g.group_id "
                + "WHERE g.course_code IN (SELECT sc.course_code FROM student_courses sc WHERE sc.student_id = ?) "
                + "GROUP BY g.group_id, g.course_code, g.course_name, g.max_size, g.creator_name ORDER BY g.course_name";
        List<Map<String, Object>> groups = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int groupId = rs.getInt("group_id");
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("groupId", groupId);
                    row.put("courseCode", rs.getInt("course_code"));
                    row.put("courseName", rs.getString("course_name"));
                    row.put("maxSize", rs.getInt("max_size"));
                    row.put("creatorName", rs.getString("creator_name"));
                    row.put("memberCount", rs.getInt("member_count"));
                    row.put("members", loadGroupMembers(groupId));
                    groups.add(row);
                }
            }
        } catch (SQLException ignored) {
            return Map.of("groups", List.of());
        }
        return Map.of("groups", groups);
    }

    private static List<Map<String, Object>> loadGroupMembers(int groupId) {
        String sql = "SELECT s.user_id, s.name, s.email FROM study_group_members sgm JOIN students s ON s.user_id = sgm.student_id WHERE sgm.group_id = ? ORDER BY s.name";
        List<Map<String, Object>> members = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> member = new LinkedHashMap<>();
                    member.put("id", rs.getInt("user_id"));
                    member.put("name", rs.getString("name"));
                    member.put("email", rs.getString("email"));
                    members.add(member);
                }
            }
        } catch (SQLException ignored) {
            return List.of();
        }
        return members;
    }

    private static Map<String, Object> joinStudyGroup(StudentGroupJoinRequest request) {
        if (request == null || request.groupId() <= 0 || request.email() == null || request.email().isBlank()) {
            return Map.of("success", false, "message", "Group and student are required.");
        }
        Integer studentId = findUserIdByEmail(request.email(), "STUDENT");
        if (studentId == null) {
            return Map.of("success", false, "message", "Student account not found.");
        }

        String sql = "INSERT INTO study_group_members (group_id, student_id) VALUES (?, ?)";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, request.groupId());
            ps.setInt(2, studentId);
            ps.executeUpdate();
            return Map.of("success", true, "message", "Joined study group successfully.");
        } catch (SQLException ex) {
            String msg = ex.getMessage() == null ? "Unable to join study group." : ex.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("unique")) {
                return Map.of("success", false, "message", "You are already a member of this group.");
            }
            return Map.of("success", false, "message", "Failed to join group: " + ex.getMessage());
        }
    }

    private static Map<String, Object> loadStudentTimetable(String email) {
        Integer studentId = findUserIdByEmail(email, "STUDENT");
        if (studentId == null) {
            return Map.of("rows", List.of(), "message", "Student not found.");
        }

        ClassTuple cls = findStudentClassTuple(studentId);
        if (cls == null) {
            return Map.of("rows", List.of(), "message", "No class assigned to student.");
        }

        String sql = "SELECT course_name, day_of_week, start_hour, start_minute, end_hour, end_minute, room_number "
                + "FROM scheduled_classes WHERE batch_no = ? AND major = ? AND section = ? AND semester = ? "
                + "ORDER BY day_of_week, start_hour, start_minute";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cls.batchNo);
            ps.setString(2, cls.major);
            ps.setString(3, cls.section);
            ps.setInt(4, cls.semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseName", rs.getString("course_name"));
                    row.put("day", rs.getString("day_of_week"));
                    row.put("startHour", rs.getInt("start_hour"));
                    row.put("startMinute", rs.getInt("start_minute"));
                    row.put("endHour", rs.getInt("end_hour"));
                    row.put("endMinute", rs.getInt("end_minute"));
                    row.put("roomNumber", rs.getInt("room_number"));
                    rows.add(row);
                }
            }
        } catch (SQLException ignored) {
            return Map.of("rows", List.of(), "message", "Unable to load timetable.");
        }

        return Map.of(
                "rows", rows,
                "batchNo", cls.batchNo,
                "major", cls.major,
                "section", cls.section,
                "semester", cls.semester,
                "message", "Timetable loaded."
        );
    }

    private static ClassTuple findStudentClassTuple(int studentId) {
        String studentsSql = "SELECT batch_no, major, section, semester FROM students WHERE user_id = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(studentsSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString("major") != null && rs.getString("section") != null) {
                    return new ClassTuple(rs.getInt("batch_no"), rs.getString("major"), rs.getString("section"), rs.getInt("semester"));
                }
            }
        } catch (SQLException ignored) {
            // fallback below
        }

        String linkSql = "SELECT batch_no, major, section, semester FROM student_acad_classes WHERE student_id = ? ORDER BY assigned_at DESC LIMIT 1";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(linkSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ClassTuple(rs.getInt("batch_no"), rs.getString("major"), rs.getString("section"), rs.getInt("semester"));
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private static Map<String, Object> loadFacultyOverview(String email) {
        Integer facultyId = findUserIdByEmail(email, "FACULTY");
        if (facultyId == null) {
            return Map.of("courses", List.of(), "schedule", List.of(), "workload", Map.of());
        }

        List<Map<String, Object>> courses = new ArrayList<>();
        String courseSql = "SELECT sc.course_code, sc.course_name, sc.credit_hours, COUNT(DISTINCT s.user_id) AS student_count "
                + "FROM scheduled_classes sc LEFT JOIN students s ON s.batch_no = sc.batch_no AND s.major = sc.major AND s.section = sc.section AND s.semester = sc.semester "
                + "WHERE sc.faculty_id = ? GROUP BY sc.course_code, sc.course_name, sc.credit_hours ORDER BY sc.course_name";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(courseSql)) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseCode", rs.getInt("course_code"));
                    row.put("courseName", rs.getString("course_name"));
                    row.put("creditHours", rs.getInt("credit_hours"));
                    row.put("studentCount", rs.getInt("student_count"));
                    courses.add(row);
                }
            }
        } catch (SQLException ignored) {
            return Map.of("courses", List.of(), "schedule", List.of(), "workload", Map.of());
        }

        List<Map<String, Object>> schedule = new ArrayList<>();
        String scheduleSql = "SELECT course_name, day_of_week, start_hour, start_minute, end_hour, end_minute, room_number, section, batch_no, major, semester "
                + "FROM scheduled_classes WHERE faculty_id = ? ORDER BY day_of_week, start_hour, start_minute";
        int mon = 0;
        int tue = 0;
        int wed = 0;
        int thu = 0;
        int fri = 0;
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(scheduleSql)) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String day = rs.getString("day_of_week");
                    if ("Monday".equalsIgnoreCase(day)) mon++;
                    if ("Tuesday".equalsIgnoreCase(day)) tue++;
                    if ("Wednesday".equalsIgnoreCase(day)) wed++;
                    if ("Thursday".equalsIgnoreCase(day)) thu++;
                    if ("Friday".equalsIgnoreCase(day)) fri++;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseName", rs.getString("course_name"));
                    row.put("day", day);
                    row.put("time", String.format("%02d:%02d-%02d:%02d", rs.getInt("start_hour"), rs.getInt("start_minute"), rs.getInt("end_hour"), rs.getInt("end_minute")));
                    row.put("room", rs.getInt("room_number"));
                    row.put("classLabel", rs.getString("major") + "-" + rs.getInt("batch_no") + rs.getString("section") + " (Sem " + rs.getInt("semester") + ")");
                    schedule.add(row);
                }
            }
        } catch (SQLException ignored) {
            return Map.of("courses", courses, "schedule", List.of(), "workload", Map.of());
        }

        Map<String, Object> workload = new LinkedHashMap<>();
        workload.put("mon", mon);
        workload.put("tue", tue);
        workload.put("wed", wed);
        workload.put("thu", thu);
        workload.put("fri", fri);
        workload.put("total", mon + tue + wed + thu + fri);
        return Map.of("courses", courses, "schedule", schedule, "workload", workload);
    }

    private static Map<String, Object> loadFacultyDeadlines(String email) {
        Integer facultyId = findUserIdByEmail(email, "FACULTY");
        if (facultyId == null) {
            return Map.of("deadlines", List.of());
        }
        String sql = "SELECT course_name, task_name, deadline FROM faculty_grading_deadlines WHERE (faculty_id = ? OR faculty_id = 0) AND is_done = 0 ORDER BY deadline, course_name";
        List<Map<String, Object>> deadlines = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("course", rs.getString("course_name"));
                    row.put("task", rs.getString("task_name"));
                    row.put("deadline", rs.getString("deadline"));
                    deadlines.add(row);
                }
            }
        } catch (SQLException ignored) {
            return Map.of("deadlines", List.of());
        }
        return Map.of("deadlines", deadlines);
    }

    private static List<Map<String, Object>> loadFacultySlots(String email) {
        Integer facultyId = findUserIdByEmail(email, "FACULTY");
        if (facultyId == null) {
            return List.of();
        }
        String sql = "SELECT course_name, day_of_week, start_hour, start_minute, end_hour, end_minute, room_number "
                + "FROM faculty_schedule WHERE faculty_id = ? ORDER BY day_of_week, start_hour, start_minute";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseName", rs.getString("course_name"));
                    row.put("day", rs.getString("day_of_week"));
                    row.put("time", String.format("%02d:%02d-%02d:%02d", rs.getInt("start_hour"), rs.getInt("start_minute"), rs.getInt("end_hour"), rs.getInt("end_minute")));
                    row.put("roomNumber", rs.getInt("room_number"));
                    rows.add(row);
                }
            }
        } catch (SQLException ignored) {
            return List.of();
        }
        return rows;
    }

    private static List<Map<String, Object>> loadMakeupRequests() {
        String sql = "SELECT COALESCE(faculty_id, 0) AS faculty_id, course_code, proposed_date, reason, requested_at FROM faculty_makeup_requests ORDER BY requested_at DESC";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("facultyId", rs.getInt("faculty_id"));
                row.put("courseCode", rs.getString("course_code"));
                row.put("proposedDate", rs.getString("proposed_date"));
                row.put("reason", rs.getString("reason"));
                row.put("requestedAt", rs.getString("requested_at"));
                rows.add(row);
            }
        } catch (SQLException ignored) {
            return List.of();
        }
        return rows;
    }

    private static Map<String, Object> reportFacultyMaintenance(FacultyMaintenanceRequest request) {
        if (request == null || request.roomNumber() <= 0 || request.note() == null || request.note().isBlank()) {
            return Map.of("success", false, "message", "Room number and maintenance note are required.");
        }
        String sql = "UPDATE rooms SET maintenance_note = ? WHERE room_number = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, request.note().trim());
            ps.setInt(2, request.roomNumber());
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                return Map.of("success", false, "message", "Room not found.");
            }
            return Map.of("success", true, "message", "Maintenance issue reported.");
        } catch (SQLException ex) {
            return Map.of("success", false, "message", "Failed to report issue: " + ex.getMessage());
        }
    }

    private static Map<String, Object> addFacultyWorkslotSelf(FacultySelfWorkslotRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            return Map.of("success", false, "message", "Faculty email is required.");
        }
        Integer facultyId = findUserIdByEmail(request.email(), "FACULTY");
        if (facultyId == null) {
            return Map.of("success", false, "message", "Faculty account not found.");
        }
        FacultyWorkslotRequest mapped = new FacultyWorkslotRequest(
                facultyId,
                request.courseCode(),
                request.courseName(),
                Math.max(1, request.creditHours()),
                Math.max(0, request.roomNumber()),
                request.dayOfWeek(),
                request.startHour(),
                request.startMinute(),
                request.endHour(),
                request.endMinute(),
                request.section(),
                Math.max(0, request.batchNo()),
                request.major(),
                Math.max(1, request.semester())
        );
        return AdminDbService.addFacultyWorkslot(mapped);
    }

    private static final class ClassTuple {
        private final int batchNo;
        private final String major;
        private final String section;
        private final int semester;

        private ClassTuple(int batchNo, String major, String section, int semester) {
            this.batchNo = batchNo;
            this.major = major;
            this.section = section;
            this.semester = semester;
        }
    }

    record LoginRequest(String email, String password, String role) {
    }

    record TimetableRequest(String semester, String batch) {
    }

    record ExamSeatingRequest(String date, String courses, String rooms) {
    }

    record AdminUserRequest(String name, String email, String password) {
    }

    record StudentCreateRequest(String name, String email, String password, int classId) {
    }

    record StudentClassAssignRequest(int studentId, int classId, int batchNo, String major, String section, int semester) {
    }

    record FacultyCourseAssignRequest(int facultyId, String courseCode, String courseName, int creditHours) {
    }

    record FacultyWorkslotRequest(int facultyId, String courseCode, String courseName, int creditHours, int roomNumber,
                                  String dayOfWeek, int startHour, int startMinute, int endHour, int endMinute,
                                  String section, int batchNo, String major, int semester) {
    }

    record ExamCreateRequest(String examCode, String examName, int totalStudents) {
    }

    record ExamDayCreateRequest(String examDate, String roomsCsv) {
    }

    record ExamScheduleAddRequest(String examDate, String examCode) {
    }

    record ExamGenerateRequest(String examDate) {
    }

    record FacultyDeadlineRequest(String email, String course, String task, String deadline) {
    }

    record FacultyDeadlineDoneRequest(String email, String course, String task) {
    }

    record FacultyMakeupRequest(String courseCode, String proposedDate, String reason) {
    }

    record StudentAssignmentRequest(String email, String courseName, String title, int daysUntilDue, int weightPercent) {
    }

    record StudentAssignmentRemoveRequest(String email, String courseName, String title) {
    }

    record StudentGroupJoinRequest(String email, int groupId) {
    }

    record FacultyMaintenanceRequest(int roomNumber, String note) {
    }

    record FacultySelfWorkslotRequest(String email, String courseCode, String courseName, int creditHours, int roomNumber,
                                      String dayOfWeek, int startHour, int startMinute, int endHour, int endMinute,
                                      String section, int batchNo, String major, int semester) {
    }
}
