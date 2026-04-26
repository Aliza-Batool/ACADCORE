package com.acadcore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class AdminDAO {
    private AdminDAO() {
    }

    public List<FacultyRow> getAllFaculty() throws SQLException {
        List<FacultyRow> rows = new ArrayList<>();

        rows.addAll(fetchFacultyRows("SELECT user_id, name, email, password_hash FROM faculty ORDER BY name"));
        if (rows.isEmpty()) {
            rows.addAll(fetchFacultyRows("SELECT user_id, name, email, password_hash FROM users WHERE UPPER(role) = 'FACULTY' ORDER BY name"));
        }

        return rows;
    }

    public List<RoomRow> getAllRooms() throws SQLException {
        String sql = "SELECT room_number, capacity, COALESCE(maintenance_note, '') AS maintenance_note FROM rooms ORDER BY room_number";
        List<RoomRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new RoomRow(
                        rs.getInt("room_number"),
                        rs.getInt("capacity"),
                        rs.getString("maintenance_note")
                ));
            }
        }
        return rows;
    }

    public List<ClassRow> getAllClasses() throws SQLException {
        String sql = "SELECT batch_no, major, section, semester FROM acad_classes ORDER BY major, batch_no, section";
        List<ClassRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new ClassRow(
                        rs.getInt("batch_no"),
                        rs.getString("major"),
                        rs.getString("section").charAt(0),
                        rs.getInt("semester")
                ));
            }
        }
        return rows;
    }

    public List<CurriculumRow> getAllCurricula() throws SQLException {
        String sql = "SELECT major, semester, course_code, course_name, credit_hours FROM curricula ORDER BY major, semester, course_code";
        List<CurriculumRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CurriculumRow(
                        rs.getString("major"),
                        rs.getInt("semester"),
                        rs.getInt("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours")
                ));
            }
        }
        return rows;
    }

    public List<CourseRow> getAllCourses() throws SQLException {
        String sql = "SELECT course_code, course_name, credit_hours FROM courses ORDER BY course_name";
        List<CourseRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CourseRow(
                        rs.getInt("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours")
                ));
            }
        }
        return rows;
    }

    public List<ScheduledClassRow> getScheduledClassesByFaculty(int facultyId) throws SQLException {
        List<ScheduledClassRow> rows = new ArrayList<>();

        rows.addAll(fetchScheduledRows(
                "SELECT course_code, course_name, credit_hours, room_number, room_capacity, day_of_week, start_hour, start_minute, end_hour, end_minute, section, batch_no, major, semester "
                        + "FROM scheduled_classes WHERE faculty_id = ? ORDER BY day_of_week, start_hour, start_minute",
                facultyId));

        if (rows.isEmpty()) {
            rows.addAll(fetchScheduledRows(
                    "SELECT course_code, course_name, credit_hours, room_number, room_capacity, day_of_week, start_hour, start_minute, end_hour, end_minute, section, batch_no, major, semester "
                            + "FROM faculty_schedule WHERE faculty_id = ? ORDER BY day_of_week, start_hour, start_minute",
                    facultyId));
        }

        return rows;
    }

    private List<FacultyRow> fetchFacultyRows(String sql) throws SQLException {
        List<FacultyRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new FacultyRow(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash")
                ));
            }
        }
        return rows;
    }

    private List<ScheduledClassRow> fetchScheduledRows(String sql, int facultyId) throws SQLException {
        List<ScheduledClassRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ScheduledClassRow(
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
        return rows;
    }

    public List<StudentRow> getEnrolledStudents(int courseCode, int batchNo, String major, char section, int semester) throws SQLException {
        String sql = "SELECT s.user_id, s.name, s.email, s.password_hash "
                + "FROM students s "
                + "JOIN student_courses sc ON sc.student_id = s.user_id "
                + "WHERE sc.course_code = ? AND sc.batch_no = ? AND sc.major = ? AND sc.section = ? AND sc.semester = ? "
                + "ORDER BY s.name";
        List<StudentRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courseCode);
            ps.setInt(2, batchNo);
            ps.setString(3, major);
            ps.setString(4, String.valueOf(section));
            ps.setInt(5, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new StudentRow(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash")
                    ));
                }
            }
        }
        return rows;
    }

    public void saveScheduledClass(ScheduledClass sc) throws SQLException {
        String sql = "INSERT INTO scheduled_classes (course_code, course_name, credit_hours, faculty_id, faculty_name, room_number, room_capacity, day_of_week, start_hour, start_minute, end_hour, end_minute, section, batch_no, major, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
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
        }
    }

    public List<RoomRow> getRoomsWithMaintenance() throws SQLException {
        String sql = "SELECT room_number, capacity, COALESCE(maintenance_note, '') AS maintenance_note FROM rooms WHERE COALESCE(maintenance_note, '') <> '' ORDER BY room_number";
        List<RoomRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new RoomRow(
                        rs.getInt("room_number"),
                        rs.getInt("capacity"),
                        rs.getString("maintenance_note")
                ));
            }
        }
        return rows;
    }

    static final class FacultyRow {
        final int id;
        final String name;
        final String email;
        final String passwordHash;

        FacultyRow(int id, String name, String email, String passwordHash) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.passwordHash = passwordHash;
        }
    }

    static final class RoomRow {
        final int roomNumber;
        final int capacity;
        final String maintenanceNote;

        RoomRow(int roomNumber, int capacity, String maintenanceNote) {
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.maintenanceNote = maintenanceNote;
        }
    }

    static final class ClassRow {
        final int batchNo;
        final String major;
        final char section;
        final int semester;

        ClassRow(int batchNo, String major, char section, int semester) {
            this.batchNo = batchNo;
            this.major = major;
            this.section = section;
            this.semester = semester;
        }
    }

    static final class CurriculumRow {
        final String major;
        final int semester;
        final int courseCode;
        final String courseName;
        final int creditHours;

        CurriculumRow(String major, int semester, int courseCode, String courseName, int creditHours) {
            this.major = major;
            this.semester = semester;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.creditHours = creditHours;
        }
    }

    static final class CourseRow {
        final int courseCode;
        final String courseName;
        final int creditHours;

        CourseRow(int courseCode, String courseName, int creditHours) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.creditHours = creditHours;
        }
    }

    static final class ScheduledClassRow {
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

        ScheduledClassRow(int courseCode, String courseName, int creditHours, int roomNumber, int roomCapacity,
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
    }

    static final class StudentRow {
        final int id;
        final String name;
        final String email;
        final String passwordHash;

        StudentRow(int id, String name, String email, String passwordHash) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.passwordHash = passwordHash;
        }
    }
}