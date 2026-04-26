package com.acadcore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {
    public static User findUserByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, role, name, email, password_hash FROM users WHERE email = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                int id = rs.getInt("user_id");
                String role = rs.getString("role");
                String name = rs.getString("name");
                String em = rs.getString("email");
                String pwd = rs.getString("password_hash");

                try {
                    if ("ADMIN".equalsIgnoreCase(role)) return new Admin(id, name, em, pwd);
                    if ("FACULTY".equalsIgnoreCase(role)) return new Faculty(id, name, em, pwd);
                    if ("STUDENT".equalsIgnoreCase(role)) {
    AcadClass acadClass = loadStudentClassFromStudentsTable(id);

    if (acadClass == null) {
        acadClass = loadStudentClassFromClassLinkTable(id);
    }

    // ✅ fallback to avoid login failure
    if (acadClass == null) {
        acadClass = new AcadClass(
            2023,   // default batch
            "CS",   // default major
            'A',    // default section
            1       // default semester
        );
    }

    return new Student(id, name, em, pwd, acadClass);
}
                } catch (InvalidPasswordException ex) {
                    throw new SQLException("Bad password data in DB", ex);
                }
            }
        }
        return null;
    }

    private static AcadClass loadStudentClassFromStudentsTable(int userId) {
        String studentSql = "SELECT batch_no, major, section, semester FROM students WHERE user_id = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(studentSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String section = rs.getString("section");
                if (section == null || section.isBlank()) {
                    return null;
                }
                return new AcadClass(
                        rs.getInt("batch_no"),
                        rs.getString("major"),
                        section.trim().charAt(0),
                        rs.getInt("semester")
                );
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static AcadClass loadStudentClassFromClassLinkTable(int userId) {
        String linkSql = "SELECT batch_no, major, section, semester FROM student_acad_classes WHERE student_id = ? "
                + "ORDER BY assigned_at DESC LIMIT 1";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(linkSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String section = rs.getString("section");
                if (section == null || section.isBlank()) {
                    return null;
                }
                return new AcadClass(
                        rs.getInt("batch_no"),
                        rs.getString("major"),
                        section.trim().charAt(0),
                        rs.getInt("semester")
                );
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public static void updateUserName(int userId, String newName) throws SQLException {
        String sql = "UPDATE users SET name = ? WHERE user_id = ?";
        try (Connection con = Db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

}
