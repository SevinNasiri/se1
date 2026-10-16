package com.library.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * مدیریت اتصال به دیتابیس SQLite و ساخت جدول‌ها در اولین اجرا.
 * فایل دیتابیس با نام «library.db» در مسیر جاری (ریشه پروژه) ساخته می‌شود.
 */
public final class DatabaseManager {

    /** آدرس اتصال؛ «jdbc:sqlite:library.db» یعنی فایل library.db در پوشه‌ای که برنامه از آن اجرا می‌شود. */
    public static final String DB_URL = "jdbc:sqlite:library.db";

    private DatabaseManager() {
        // کلاس ابزاری است؛ نباید نمونه‌سازی شود
    }

    /** باز کردن یک اتصال جدید؛ فراخوان باید آن را با try-with-resources ببندد. */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        // در SQLite کلیدهای خارجی به‌صورت پیش‌فرض خاموش‌اند و باید در هر اتصال فعال شوند
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /** ساخت جدول‌ها در صورت نبود؛ در شروع برنامه یک بار صدا زده می‌شود. */
    public static void initDatabase() throws SQLException {
        String createBooks = """
                CREATE TABLE IF NOT EXISTS books (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    title            TEXT    NOT NULL,
                    author           TEXT    NOT NULL,
                    isbn             TEXT    NOT NULL UNIQUE,
                    publication_year INTEGER NOT NULL,
                    available        INTEGER NOT NULL DEFAULT 1   -- 1 = موجود، 0 = امانت‌رفته
                )""";

        String createMembers = """
                CREATE TABLE IF NOT EXISTS members (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name    TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    email        TEXT NOT NULL UNIQUE,
                    join_date    TEXT NOT NULL                    -- تاریخ ISO (yyyy-MM-dd)
                )""";

        // تاریخ‌ها در همه جدول‌ها به فرمت ISO (yyyy-MM-dd) و به‌صورت TEXT ذخیره می‌شوند
        String createLoans = """
                CREATE TABLE IF NOT EXISTS loans (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id     INTEGER NOT NULL,
                    member_id   INTEGER NOT NULL,
                    borrow_date TEXT NOT NULL,
                    return_date TEXT,                              -- NULL یعنی امانت فعال است
                    FOREIGN KEY (book_id)   REFERENCES books (id)   ON DELETE CASCADE,
                    FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE
                )""";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createBooks);
            statement.execute(createMembers);
            statement.execute(createLoans);
        }
    }

    /** اجرای دستور INSERT و برگرداندن شناسه‌ای که دیتابیس برای ردیف جدید تولید کرده است. */
    public static int executeInsert(PreparedStatement statement) throws SQLException {
        statement.executeUpdate();
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Could not read the generated id.");
    }
}
