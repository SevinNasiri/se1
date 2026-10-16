package com.library.service;

import com.library.db.DatabaseManager;
import com.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * منطق مدیریت کتاب‌ها.
 * داده‌ها در جدول books دیتابیس SQLite نگهداری می‌شوند؛
 * امضاهای متدها مثل قبل است، پس رابط کاربری تغییری لازم ندارد.
 */
public class BookService {

    /** ستون‌های مشترک کوئری‌های SELECT روی جدول books */
    private static final String SELECT_COLUMNS =
            "id, title, author, isbn, publication_year, available ";

    /** افزودن کتاب جدید؛ ISBN تکراری مجاز نیست. */
    public Book addBook(String title, String author, String isbn, int publicationYear) {
        Optional<Book> duplicate = findByIsbn(isbn);
        if (duplicate.isPresent()) {
            throw new LibraryException("A book with this ISBN already exists (id: "
                    + duplicate.get().getId() + ").");
        }
        String sql = "INSERT INTO books (title, author, isbn, publication_year, available) "
                + "VALUES (?, ?, ?, ?, 1)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, author);
            statement.setString(3, isbn);
            statement.setInt(4, publicationYear);
            int id = DatabaseManager.executeInsert(statement);
            return new Book(id, title, author, isbn, publicationYear);
        } catch (SQLException e) {
            throw new LibraryException("Database error while adding the book: " + e.getMessage());
        }
    }

    /** حذف کتاب با شناسه؛ اگر پیدا نشد false برمی‌گرداند. سابقه امانت‌های آن هم پاک می‌شود (ON DELETE CASCADE). */
    public boolean removeById(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Database error while removing the book: " + e.getMessage());
        }
    }

    /** جستجو در عنوان، نویسنده یا ISBN (بدون حساسیت به بزرگی/کوچکی حروف). */
    public List<Book> search(String keyword) {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM books "
                + "WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(isbn) LIKE ?";
        String likePattern = "%" + keyword.toLowerCase() + "%";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, likePattern);
            statement.setString(2, likePattern);
            statement.setString(3, likePattern);
            return mapAll(statement.executeQuery());
        } catch (SQLException e) {
            throw new LibraryException("Database error while searching books: " + e.getMessage());
        }
    }

    /** فهرست همه کتاب‌ها به ترتیب شناسه. */
    public List<Book> getAll() {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM books ORDER BY id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException e) {
            throw new LibraryException("Database error while listing books: " + e.getMessage());
        }
    }

    public Optional<Book> findById(int id) {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM books WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while finding the book: " + e.getMessage());
        }
    }

    private Optional<Book> findByIsbn(String isbn) {
        String sql = "SELECT " + SELECT_COLUMNS + "FROM books WHERE LOWER(isbn) = LOWER(?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, isbn);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while checking the ISBN: " + e.getMessage());
        }
    }

    // ---------------- ابزارهای داخلی ----------------

    /** ساخت یک Book از ردیف جاری ResultSet. */
    private Book mapRow(ResultSet resultSet) throws SQLException {
        Book book = new Book(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("author"),
                resultSet.getString("isbn"),
                resultSet.getInt("publication_year"));
        book.setAvailable(resultSet.getBoolean("available"));
        return book;
    }

    /** خواندن همه ردیف‌های نتیجه کوئری و تبدیل به لیست Book. */
    private List<Book> mapAll(ResultSet resultSet) throws SQLException {
        List<Book> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(mapRow(resultSet));
        }
        return result;
    }
}
