package com.acadcore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
final class StudentDAO {
    @SuppressWarnings("unused")
    StudentDAO() {
    }

    public List<AcademicClassRow> getStudentClassLinks(int studentId) throws SQLException {
        String sql = "SELECT a.batch_no, a.major, a.section, a.semester FROM students s JOIN acad_classes a ON a.batch_no = s.batch_no AND a.major = s.major AND a.section = s.section AND a.semester = s.semester WHERE s.user_id = ?";
        List<AcademicClassRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AcademicClassRow(
                            rs.getInt("batch_no"),
                            rs.getString("major"),
                            rs.getString("section").charAt(0),
                            rs.getInt("semester")
                    ));
                }
            }
        }
        return rows;
    }

    public List<ScheduledClassRow> getScheduledClasses(int studentId) throws SQLException {
        String sql = "SELECT sc.course_code, sc.course_name, sc.credit_hours, sc.room_number, sc.room_capacity, sc.day_of_week, sc.start_hour, sc.start_minute, sc.end_hour, sc.end_minute, sc.section, sc.batch_no, sc.major, sc.semester "
                + "FROM scheduled_classes sc JOIN students s ON s.batch_no = sc.batch_no AND s.major = sc.major AND s.section = sc.section AND s.semester = sc.semester WHERE s.user_id = ? ORDER BY sc.day_of_week, sc.start_hour, sc.start_minute";
        List<ScheduledClassRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
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

    public List<AssignmentRow> getAssignments(int studentId) throws SQLException {
        String sql = "SELECT course_name, title, days_until_due, weight_percent FROM assignments WHERE student_id = ? ORDER BY days_until_due ASC";
        List<AssignmentRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AssignmentRow(
                            rs.getString("course_name"),
                            rs.getString("title"),
                            rs.getInt("days_until_due"),
                            rs.getInt("weight_percent")
                    ));
                }
            }
        }
        return rows;
    }

    public void insertAssignment(int studentId, Assignment assignment) throws SQLException {
        String sql = "INSERT INTO assignments (student_id, course_name, title, days_until_due, weight_percent) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, assignment.getCourseName());
            ps.setString(3, assignment.getTitle());
            ps.setInt(4, assignment.getDaysUntilDue());
            ps.setInt(5, assignment.getWeightPercent());
            ps.executeUpdate();
        }
    }

    /**
     * FIX 1 – Assignment Prioritizer: "remove assignment when done"
     * Deletes the assignment row matching the student, course name, and title.
     * Returns true if a row was actually deleted.
     */
    public boolean removeAssignment(int studentId, String courseName, String title) throws SQLException {
        String sql = "DELETE FROM assignments WHERE student_id = ? AND course_name = ? AND title = ? LIMIT 1";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, courseName);
            ps.setString(3, title);
            int affected = ps.executeUpdate();
            return affected > 0; // true means the row existed and was deleted
        }
    }

    public List<GroupMemberRow> getStudyGroupMembers(int groupId) throws SQLException {
    String sql = "SELECT s.user_id, s.name, s.email "
            + "FROM study_group_members sgm "
            + "JOIN students s ON s.user_id = sgm.student_id "
            + "WHERE sgm.group_id = ? "
            + "ORDER BY s.name";
    List<GroupMemberRow> rows = new ArrayList<>();
    try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, groupId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new GroupMemberRow(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email")
                ));
            }
        }
    }
    return rows;
}

    public List<AttendanceRow> getAttendanceRecords(int studentId) throws SQLException {
        String sql = "SELECT course_name, attended_classes, total_classes FROM attendance_records WHERE student_id = ? ORDER BY course_name";
        List<AttendanceRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AttendanceRow(
                            rs.getString("course_name"),
                            rs.getInt("attended_classes"),
                            rs.getInt("total_classes")
                    ));
                }
            }
        }
        return rows;
    }

    public List<StudyGroupRow> getStudyGroups(int studentId) throws SQLException {
        String sql = "SELECT g.group_id, g.course_code, g.course_name, g.max_size, g.creator_id, g.creator_name FROM study_groups g WHERE g.course_code IN (SELECT sc.course_code FROM student_courses sc WHERE sc.student_id = ?) ORDER BY g.course_name";
        List<StudyGroupRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new StudyGroupRow(
                            rs.getInt("group_id"),
                            rs.getInt("course_code"),
                            rs.getString("course_name"),
                            rs.getInt("max_size"),
                            rs.getInt("creator_id"),
                            rs.getString("creator_name")
                    ));
                }
            }
        }
        return rows;
    }


    public int insertStudyGroup(StudyGroup group, int creatorId) throws SQLException {
        String sql = "INSERT INTO study_groups (course_code, course_name, max_size, creator_id, creator_name) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, group.getCourse().getCourseCode());
            ps.setString(2, group.getCourse().getCourseName());
            ps.setInt(3, group.getMaxSize());
            ps.setInt(4, creatorId);
            ps.setString(5, group.getLeader().getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public void insertStudyGroupMember(int groupId, int studentId) throws SQLException {
        String sql = "INSERT INTO study_group_members (group_id, student_id) VALUES (?, ?)";
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        }
    }

    public List<CourseRow> getStudentCourses(int studentId) throws SQLException {
        String sql = "SELECT c.course_code, c.course_name, c.credit_hours FROM courses c JOIN student_courses sc ON sc.course_code = c.course_code WHERE sc.student_id = ? ORDER BY c.course_name";
        List<CourseRow> rows = new ArrayList<>();
        try (Connection con = Db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new CourseRow(
                            rs.getInt("course_code"),
                            rs.getString("course_name"),
                            rs.getInt("credit_hours")
                    ));
                }
            }
        }
        return rows;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    static final class AcademicClassRow {
        final int batchNo;
        final String major;
        final char section;
        final int semester;

        AcademicClassRow(int batchNo, String major, char section, int semester) {
            this.batchNo = batchNo;
            this.major = major;
            this.section = section;
            this.semester = semester;
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    static final class AssignmentRow {
        final String courseName;
        final String title;
        final int daysUntilDue;
        final int weightPercent;

        AssignmentRow(String courseName, String title, int daysUntilDue, int weightPercent) {
            this.courseName = courseName;
            this.title = title;
            this.daysUntilDue = daysUntilDue;
            this.weightPercent = weightPercent;
        }
    }

    @SuppressWarnings("unused")
    static final class AttendanceRow {
        final String courseName;
        final int attendedClasses;
        final int totalClasses;

        AttendanceRow(String courseName, int attendedClasses, int totalClasses) {
            this.courseName = courseName;
            this.attendedClasses = attendedClasses;
            this.totalClasses = totalClasses;
        }
    }

    @SuppressWarnings("unused")
    static final class StudyGroupRow {
        final int groupId;
        final int courseCode;
        final String courseName;
        final int maxSize;
        final int creatorId;
        final String creatorName;

        StudyGroupRow(int groupId, int courseCode, String courseName, int maxSize, int creatorId, String creatorName) {
            this.groupId = groupId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.maxSize = maxSize;
            this.creatorId = creatorId;
            this.creatorName = creatorName;
        }
    }
        @SuppressWarnings("unused")
    static final class GroupMemberRow {
        final int id;
        final String name;
        final String email;

        GroupMemberRow(int id, String name, String email) {
            this.id    = id;
            this.name  = name;
            this.email = email;
        }
    }
}