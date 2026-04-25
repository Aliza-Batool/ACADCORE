package com.acadcore;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

final class AdminMenu {
    private AdminMenu() {
    }

    static void run(Admin admin, Scanner sc, AdminDAO dao) {
        if (admin == null || sc == null || dao == null) {
            return;
        }

        AcademicSystem sys = new AcademicSystem();
        while (true) {
            System.out.println("\n── Admin Menu for " + admin.getName() + " ──");
            System.out.println("1. Generate Clash-Free Timetable");
            System.out.println("2. Monitor Faculty Workload Distribution");
            System.out.println("3. Generate Exam Seating Plan");
            System.out.println("4. Track Room Utilization");
            System.out.println("5. View Maintenance Issue Reports");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> generateTimetable(admin, dao, sys);
                case "2" -> monitorFacultyWorkload(dao);
                case "3" -> generateExamSeatingPlan(admin, dao, sc);
                case "4" -> trackRoomUtilization(dao);
                case "5" -> viewMaintenanceReports(dao);
                case "6" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void generateTimetable(Admin admin, AdminDAO dao, AcademicSystem sys) {
        try {
            loadClasses(dao, sys);
            loadCurricula(dao, sys);
            loadFaculty(dao, sys);
            loadRooms(dao, sys);

            admin.generateClashFreeTimetable(sys);

            for (int i = 0; i < sys.timetable.getTotalEntries(); i++) {
                dao.saveScheduledClass(sys.timetable.getSchedule()[i]);
            }

            System.out.println("Timetable generated and saved.");
        } catch (Exception ex) {
            System.out.println("Timetable generation failed: " + ex.getMessage());
        }
    }

    private static void monitorFacultyWorkload(AdminDAO dao) {
        try {
            List<AdminDAO.FacultyRow> facultyRows = dao.getAllFaculty();
            AcademicSystem sys = new AcademicSystem();
            for (AdminDAO.FacultyRow row : facultyRows) {
                Faculty faculty = new Faculty(row.id, row.name, row.email, row.passwordHash == null || row.passwordHash.isBlank() ? "secret123" : row.passwordHash);
                for (AdminDAO.ScheduledClassRow scheduledRow : dao.getScheduledClassesByFaculty(row.id)) {
                    faculty.loadScheduledClassFromDatabase(buildScheduledClass(scheduledRow, faculty));
                }
                sys.addFaculty(faculty);
            }
            WorkloadService.reportAllFacultyWorkload(sys);
        } catch (Exception ex) {
            System.out.println("Failed to load faculty workload: " + ex.getMessage());
        }
    }

    private static void generateExamSeatingPlan(Admin admin, AdminDAO dao, Scanner sc) {
        try {
            System.out.print("Enter course code: ");
            int courseCode = Integer.parseInt(sc.nextLine().trim());
            List<AdminDAO.CourseRow> courseRows = dao.getAllCourses();
            Course selectedCourse = null;
            for (AdminDAO.CourseRow row : courseRows) {
                if (row.courseCode == courseCode) {
                    selectedCourse = new Course(row.courseCode, row.courseName, row.creditHours);
                    break;
                }
            }

            if (selectedCourse == null) {
                System.out.println("Course not found.");
                return;
            }

            System.out.print("Enter exam date: ");
            String date = sc.nextLine().trim();

            AcademicSystem sys = new AcademicSystem();
            Room[] rooms = loadRooms(dao, sys);
            ExamDaySchedule daySchedule = new ExamDaySchedule(date);
            ExamPaper paper = new ExamPaper(selectedCourse, sys);
            paper.FindClasses();
            daySchedule.addPaper(paper);
            ExamSeating seating = new ExamSeating(daySchedule, rooms);
            seating.generateSeatingPlanForDay();
            sys.addGeneratedExamSeatingPlan(seating);

            System.out.println("Exam seating plan generated.");
        } catch (RoomCapacityException ex) {
            System.out.println(ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Failed to generate exam seating plan: " + ex.getMessage());
        }
    }

    private static void trackRoomUtilization(AdminDAO dao) {
        AcademicSystem sys = new AcademicSystem();
        try {
            loadRooms(dao, sys);
            RoomUtilizationService.reportRoomUtilization(sys);
        } catch (Exception ex) {
            System.out.println("Failed to load room utilization: " + ex.getMessage());
        }
    }

    private static void viewMaintenanceReports(AdminDAO dao) {
        AcademicSystem sys = new AcademicSystem();
        try {
            for (AdminDAO.RoomRow row : dao.getRoomsWithMaintenance()) {
                Room room = new Room(row.roomNumber, row.capacity);
                room.reportMaintenanceIssue(row.maintenanceNote);
                sys.addRoom(room);
            }
            MaintenanceService.showMaintenanceReports(sys);
        } catch (SQLException ex) {
            System.out.println("Failed to load maintenance reports: " + ex.getMessage());
        }
    }

    private static void loadClasses(AdminDAO dao, AcademicSystem sys) throws SQLException {
        for (AdminDAO.ClassRow row : dao.getAllClasses()) {
            sys.addClass(new AcadClass(row.batchNo, row.major, row.section, row.semester));
        }
    }

    private static void loadCurricula(AdminDAO dao, AcademicSystem sys) throws SQLException {
        for (AdminDAO.CurriculumRow row : dao.getAllCurricula()) {
            Curriculum curriculum = findOrCreateCurriculum(sys, row.major, row.semester);
            curriculum.addCourse(new Course(row.courseCode, row.courseName, row.creditHours));
        }
    }

    private static void loadFaculty(AdminDAO dao, AcademicSystem sys) throws SQLException {
        for (AdminDAO.FacultyRow row : dao.getAllFaculty()) {
            Faculty faculty = new Faculty(row.id, row.name, row.email, row.passwordHash == null || row.passwordHash.isBlank() ? "secret123" : row.passwordHash);
            for (AdminDAO.ScheduledClassRow scheduledRow : dao.getScheduledClassesByFaculty(row.id)) {
                faculty.loadScheduledClassFromDatabase(buildScheduledClass(scheduledRow, faculty));
            }
            sys.addFaculty(faculty);
        }
    }

    private static Room[] loadRooms(AdminDAO dao, AcademicSystem sys) throws SQLException {
        List<AdminDAO.RoomRow> rows = dao.getAllRooms();
        Room[] rooms = new Room[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            AdminDAO.RoomRow row = rows.get(i);
            Room room = new Room(row.roomNumber, row.capacity);
            if (row.maintenanceNote != null && !row.maintenanceNote.isBlank()) {
                room.reportMaintenanceIssue(row.maintenanceNote);
            }
            sys.addRoom(room);
            rooms[i] = room;
        }
        return rooms;
    }

    private static Curriculum findOrCreateCurriculum(AcademicSystem sys, String major, int semester) {
        for (int i = 0; i < sys.curriculumCount; i++) {
            Curriculum existing = sys.curricula[i];
            if (existing != null && existing.getMajor().equalsIgnoreCase(major) && existing.getSemester() == semester) {
                return existing;
            }
        }

        Curriculum curriculum = new Curriculum(major, semester);
        sys.addCurriculum(curriculum);
        return curriculum;
    }

    private static ScheduledClass buildScheduledClass(AdminDAO.ScheduledClassRow row, Faculty faculty) {
        Course course = new Course(row.courseCode, row.courseName, row.creditHours);
        Room room = new Room(row.roomNumber, row.roomCapacity);
        TimeSlot slot = new TimeSlot(dayToChoice(row.dayOfWeek), row.startHour, row.startMinute, row.endHour, row.endMinute);
        AcadClass acadClass;
        try {
            acadClass = new AcadClass(row.batchNo, row.major, row.section, row.semester);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex);
        }
        return new ScheduledClass(course, faculty, room, slot, row.section, acadClass);
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