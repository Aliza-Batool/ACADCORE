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
            System.out.println("2. Use Assignment Prioritizer");
            //fix 4 removed student clash detection 
            System.out.println("3. Get Attendance Risk Alerts");
            System.out.println("4. Join or Create Smart Study Group");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewTimetable(student, dao);
                case "2" -> assignmentPrioritizer(student, sc, dao);
                case "3" -> attendanceRiskAlerts(student, dao);
                case "4" -> studyGroupMenu(student, sc, dao);
                case "5" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    //fix 2: shows all 9-5 slots even when free
    private static void viewTimetable(Student student, StudentDAO dao) {
        try {
            StudentDAO.AcademicClassRow row = firstClassRow(dao.getStudentClassLinks(student.getId()));
            if (row == null) {
                System.out.println("No class found for student.");
                return;
            }

            AcadClass acadClass = new AcadClass(row.batchNo, row.major, row.section, row.semester);
            student.setClass(acadClass);

            // Load all scheduled classes from the DB into the AcadClass object
            List<StudentDAO.ScheduledClassRow> scheduledRows = dao.getScheduledClasses(student.getId());
            for (StudentDAO.ScheduledClassRow scheduledRow : scheduledRows) {
                ScheduledClass scheduledClass = buildScheduledClass(scheduledRow, acadClass);
                if (scheduledClass != null) {
                    acadClass.addToClassSchedule(scheduledClass);
                }
            }

            // ── Print the full 9-to-17 grid ──────────────────────────────────────────
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            // Hour slots: 09:00-10:00, 10:00-11:00, ... 16:00-17:00
            int[] startHours = {9, 10, 11, 12, 13, 14, 15, 16};

            System.out.println("\n── Timetable: " + row.major + "-" + row.batchNo + row.section
                    + " (Sem " + row.semester + ") ──");
            System.out.printf("%-12s", "Time");
            for (String day : days) {
                System.out.printf("  %-18s", day);
            }
            System.out.println();
            System.out.println("─".repeat(12 + days.length * 20));

            ScheduledClass[] schedule = acadClass.getClassSchedule();
            int schedCount = acadClass.getClassScheduleCount();

            for (int hour : startHours) {
                // Time label for this row, e.g. "09:00-10:00"
                System.out.printf("%-12s", String.format("%02d:00-%02d:00", hour, hour + 1));

                for (String day : days) {
                    // Find a scheduled class that falls on this day and overlaps this hour
                    String cell = "-- FREE --";
                    for (int k = 0; k < schedCount; k++) {
                        ScheduledClass sc = schedule[k];
                        if (sc == null || sc.getSlot() == null) continue;
                        TimeSlot slot = sc.getSlot();
                        if (!day.equalsIgnoreCase(slot.getDay())) continue;
                        // Check that the class occupies some part of this 1-hour slot
                        int classStart = slot.getStartTime().getHour();
                        int classEnd   = slot.getEndTime().getHour();
                        if (classStart <= hour && classEnd > hour) {
                            // Truncate long course names to 18 chars so the table stays aligned
                            String name = sc.getCourse().getCourseName();
                            if (name.length() > 16) name = name.substring(0, 15) + "…";
                            cell = name;
                            break;
                        }
                    }
                    System.out.printf("  %-18s", cell);
                }
                System.out.println();
            }

        } catch (SQLException | IllegalArgumentException ex) {
            System.out.println("Failed to load timetable: " + ex.getMessage());
        }
    }

    private static void assignmentPrioritizer(Student student, Scanner sc, StudentDAO dao) {
        try {
            loadStudentCourses(student, dao);
            loadAssignments(student, dao);

            // ── ADD a new assignment ─────────────────────────────────────────────────
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
                System.out.println("Assignment added.");
            }

            // Show the prioritized list first so the student can see what to remove
            student.showPrioritizedAssignments();

            // ── REMOVE a completed assignment ────────────────────────────────────────
            if (student.getAssignmentCount() == 0) {
                return; // nothing to remove
            }
            System.out.print("\nMark an assignment as done / remove it? (y/n): ");
            if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
                return;
            }

            // Build a numbered list of current assignments so the user can pick one
            Assignment[] current = student.getAssignmentsCopy(); // returns defensive copy
            if (current.length == 0) {
                System.out.println("No assignments to remove.");
                return;
            }
            System.out.println("Select assignment to remove:");
            for (int i = 0; i < current.length; i++) {
                System.out.printf("  %d. [%s] %s%n", i + 1, current[i].getCourseName(), current[i].getTitle());
            }
            System.out.print("Enter number: ");
            int idx;
            try {
                idx = Integer.parseInt(sc.nextLine().trim()) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return;
            }
            if (idx < 0 || idx >= current.length) {
                System.out.println("Invalid selection.");
                return;
            }
            Assignment toRemove = current[idx];
            // Remove from DB first, then from the in-memory Student object
            boolean removedFromDb = dao.removeAssignment(student.getId(),
                    toRemove.getCourseName(), toRemove.getTitle());
            student.removeAssignment(toRemove.getCourseName(), toRemove.getTitle());
            System.out.println(removedFromDb
                    ? "Assignment \"" + toRemove.getTitle() + "\" removed."
                    : "Assignment removed from view (not found in DB — may have already been deleted).");

        } catch (SQLException | NumberFormatException ex) {
            System.out.println("Failed to process assignments: " + ex.getMessage());
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
//fix 3: joines and shows details of members
    private static void studyGroupMenu(Student student, Scanner sc, StudentDAO dao) {
        System.out.println("1. Create a group");
        System.out.println("2. Join a group");
        System.out.print("Choose an option: ");
        String choice = sc.nextLine().trim();

        try {
            if ("1".equals(choice)) {
                // ── CREATE ───────────────────────────────────────────────────────────
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
                if (groupId > 0) {
                    // Also insert the creator as the first member so getStudyGroupMembers works
                    dao.insertStudyGroupMember(groupId, student.getId());
                    System.out.println("Study group created. Members so far:");
                    printGroupMembers(groupId, dao);
                } else {
                    System.out.println("Study group creation failed.");
                }

            } else if ("2".equals(choice)) {
                // ── JOIN ─────────────────────────────────────────────────────────────
                loadStudentCourses(student, dao);
                List<StudentDAO.StudyGroupRow> groups = dao.getStudyGroups(student.getId());
                if (groups.isEmpty()) {
                    System.out.println("No available groups.");
                    return;
                }

                // Print the group list with a short summary
                for (int i = 0; i < groups.size(); i++) {
                    StudentDAO.StudyGroupRow grpRow = groups.get(i);
                    System.out.printf("%d. [%s] max %d members | leader: %s%n",
                            i + 1, grpRow.courseName, grpRow.maxSize, grpRow.creatorName);
                }
                System.out.println("Enter group number to join, or type 'details <number>' to see members first:");
                String input = sc.nextLine().trim();

                // Allow the student to inspect members before committing
                while (input.toLowerCase().startsWith("details")) {
                    String[] parts = input.split("\\s+");
                    if (parts.length == 2) {
                        try {
                            int detailIdx = Integer.parseInt(parts[1]) - 1;
                            if (detailIdx >= 0 && detailIdx < groups.size()) {
                                printGroupMembers(groups.get(detailIdx).groupId, dao);
                            } else {
                                System.out.println("Invalid group number.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Usage: details <number>");
                        }
                    }
                    System.out.print("Enter group number to join (or 'details <number>'): ");
                    input = sc.nextLine().trim();
                }

                int index;
                try {
                    index = Integer.parseInt(input) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input.");
                    return;
                }
                if (index < 0 || index >= groups.size()) {
                    System.out.println("Invalid selection.");
                    return;
                }

                StudentDAO.StudyGroupRow grpRow = groups.get(index);
                Course course = findCourse(student, grpRow.courseCode);
                if (course == null) {
                    System.out.println("You are not enrolled in that course.");
                    return;
                }
                // Build a transient StudyGroup object just to call addMember (which validates)
                StudyGroup group = new StudyGroup(course, grpRow.maxSize, student);
                if (group.addMember(student)) {
                    dao.insertStudyGroupMember(grpRow.groupId, student.getId());
                    System.out.println("You have joined the group! Current members:");
                    // FIX 3 core: print the full member list so the student can see who else is in
                    printGroupMembers(grpRow.groupId, dao);
                }
            }
        } catch (SQLException | NumberFormatException ex) {
            System.out.println("Study group action failed: " + ex.getMessage());
        }
    }

    //fix 3 helper
    private static void printGroupMembers(int groupId, StudentDAO dao) throws SQLException {
        List<StudentDAO.GroupMemberRow> members = dao.getStudyGroupMembers(groupId);
        if (members.isEmpty()) {
            System.out.println("  (no members yet)");
            return;
        }
        System.out.println("  Members:");
        for (int i = 0; i < members.size(); i++) {
            StudentDAO.GroupMemberRow m = members.get(i);
            System.out.printf("    %d. %s  <%s>%n", i + 1, m.name, m.email);
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