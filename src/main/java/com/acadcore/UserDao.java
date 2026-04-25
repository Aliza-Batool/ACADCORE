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
                    return new BasicUser(id, name, em, pwd); // STUDENT for now
                } catch (InvalidPasswordException ex) {
                    throw new SQLException("Bad password data in DB", ex);
                }
            }
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

    static class BasicUser extends User {
        public BasicUser(int id, String name, String email, String password) throws InvalidPasswordException {
            super(id, name, email, password);
        }
    }

}
