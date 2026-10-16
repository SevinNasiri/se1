package com.library.service;

import com.library.db.DatabaseManager;
import com.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * منطق مدیریت اعضا.
 * داده‌ها در جدول members دیتابیس SQLite نگهداری می‌شوند.
 */
public class MemberService {

    /** ستون‌های مشترک کوئری‌های SELECT روی جدول members */
    private static final String SELECT_COLUMNS =
            "id, full_name, phone_number, email, join_date ";

    /** ثبت عضو جدید؛ ایمیل تکراری مجاز نیست. */
    public Member register(String fullName, String phoneNumber, String email) {
        if (findByEmail(email).isPresent()) {
            throw new LibraryException("A member with this email already exists.");
        }
        String sql = "INSERT INTO members (full_name, phone_number, email, join_date) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.setString(2, phoneNumber);
            statement.setString(3, email);
            statement.setString(4, LocalDate.now().toString()); // تاریخ عضویت به فرمت ISO
            int id = DatabaseManager.executeInsert(statement);
            return new Member(id, fullName, phoneNumber, email);
        } catch (SQLException e) {
            throw new LibraryException("Database error while registering the member: " + e.getMessage());
        }
    }

    /** حذف عضو با شناسه؛ اگر پیدا نشد false برمی‌گرداند. سابقه امانت‌های او هم پاک می‌شود (ON DELETE CASCADE). */
    public boolean removeById(int id) {
        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Database error while removing the member: " + e.getMessage());
        }
    }

    /** جستجو بر اساس نام، ایمیل یا شناسه. */
    public List<Member> search(String keyword) {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM members "
                + "WHERE LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR CAST(id AS TEXT) = ?";
        String likePattern = "%" + keyword.toLowerCase() + "%";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, likePattern);
            statement.setString(2, likePattern);
            statement.setString(3, keyword);
            return mapAll(statement.executeQuery());
        } catch (SQLException e) {
            throw new LibraryException("Database error while searching members: " + e.getMessage());
        }
    }

    /** فهرست همه اعضا به ترتیب شناسه. */
    public List<Member> getAll() {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM members ORDER BY id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException e) {
            throw new LibraryException("Database error while listing members: " + e.getMessage());
        }
    }

    public Optional<Member> findById(int id) {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM members WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while finding the member: " + e.getMessage());
        }
    }

    private Optional<Member> findByEmail(String email) {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM members WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while checking the email: " + e.getMessage());
        }
    }

    // ---------------- ابزارهای داخلی ----------------

    /** ساخت یک Member از ردیف جاری ResultSet (با تاریخ عضویتِ ذخیره‌شده در دیتابیس). */
    private Member mapRow(ResultSet resultSet) throws SQLException {
        return new Member(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone_number"),
                resultSet.getString("email"),
                LocalDate.parse(resultSet.getString("join_date")));
    }

    /** خواندن همه ردیف‌های نتیجه کوئری و تبدیل به لیست Member. */
    private List<Member> mapAll(ResultSet resultSet) throws SQLException {
        List<Member> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(mapRow(resultSet));
        }
        return result;
    }
}
