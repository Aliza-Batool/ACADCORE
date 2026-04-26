package com.acadcore;

import java.sql.SQLException;
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
        app.get("/api/admin/workload", ctx -> ctx.json(AdminDbService.getFacultyWorkload()));
        app.get("/api/admin/rooms/utilization", ctx -> ctx.json(AdminDbService.getRoomUtilization()));
        app.get("/api/admin/maintenance", ctx -> ctx.json(AdminDbService.getMaintenanceReports()));

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

        app.get("/api/student/clashes", ctx -> ctx.json(Map.of(
                "hasClash", false,
                "message", "No clashes detected"
        )));

        app.get("/api/student/assignments", ctx -> {
            String email = ctx.queryParam("email");
            try {
                User user = UserDao.findUserByEmail(email);
                if (!(user instanceof Student student)) {
                    ctx.json(Map.of("success", false, "message", "Student not found."));
                    return;
                }
                StudentDAO dao = new StudentDAO();
                var rows = dao.getAssignments(student.getId());
                ctx.json(rows);
            } catch (Exception ex) {
                ctx.json(Map.of("success", false, "message", ex.getMessage()));
            }
        });

        app.post("/api/student/assignments/add", ctx -> {
            record AddReq(String email, String courseName, String title, int daysUntilDue, int weightPercent) {}
            AddReq req = ctx.bodyAsClass(AddReq.class);
            try {
                User user = UserDao.findUserByEmail(req.email());
                if (!(user instanceof Student student)) {
                    ctx.json(Map.of("success", false, "message", "Student not found."));
                    return;
                }
                Assignment a = new Assignment(req.courseName(), req.title(), req.daysUntilDue(), req.weightPercent());
                new StudentDAO().insertAssignment(student.getId(), a);
                ctx.json(Map.of("success", true, "message", "Assignment added."));
            } catch (Exception ex) {
                ctx.json(Map.of("success", false, "message", ex.getMessage()));
            }
        });

        app.post("/api/student/assignments/remove", ctx -> {
            record RemoveReq(String email, String courseName, String title) {}
            RemoveReq req = ctx.bodyAsClass(RemoveReq.class);
            try {
                User user = UserDao.findUserByEmail(req.email());
                if (!(user instanceof Student student)) {
                    ctx.json(Map.of("success", false, "message", "Student not found."));
                    return;
                }
                boolean removed = new StudentDAO().removeAssignment(student.getId(), req.courseName(), req.title());
                ctx.json(Map.of("success", removed, "message", removed ? "Removed." : "Assignment not found."));
            } catch (Exception ex) {
                ctx.json(Map.of("success", false, "message", ex.getMessage()));
            }
        });
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
                    "role", role
            );
        } catch (SQLException ex) {
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    record LoginRequest(String email, String password, String role) {
    }

    record TimetableRequest(String semester, String batch) {
    }

    record ExamSeatingRequest(String date, String courses, String rooms) {
    }

    record FacultyDeadlineRequest(String course, String task, String deadline) {
    }

    record FacultyDeadlineDoneRequest(String course, String task) {
    }

    record FacultyMakeupRequest(String courseCode, String proposedDate, String reason) {
    }
}