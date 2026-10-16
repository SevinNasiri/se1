package com.library;

import com.library.db.DatabaseManager;
import com.library.service.BookService;
import com.library.service.LoanService;
import com.library.service.MemberService;
import com.library.ui.ConsoleUI;

import java.sql.SQLException;

/**
 * نقطه شروع برنامه (entry point).
 * ابتدا دیتابیس را آماده می‌کند (ساخت جدول‌ها در اولین اجرا)،
 * سپس سرویس‌ها را می‌سازد و رابط کنسولی را اجرا می‌کند.
 */
public class Main {

    public static void main(String[] args) {
        // ساخت جدول‌ها اگر وجود نداشته باشند؛ فایل library.db در اولین اجرا ساخته می‌شود
        try {
            DatabaseManager.initDatabase();
        } catch (SQLException e) {
            System.err.println("Could not initialize the database: " + e.getMessage());
            return;
        }

        // ساخت سرویس‌ها؛ حالا هر سرویس مستقیم با دیتابیس SQLite کار می‌کند
        BookService bookService = new BookService();
        MemberService memberService = new MemberService();
        LoanService loanService = new LoanService();

        // داده نمونه فقط وقتی درج می‌شود که دیتابیس خالی باشد (یعنی بار اول)
        if (bookService.getAll().isEmpty()) {
            loadSampleData(bookService, memberService);
        }

        // ساخت و اجرای رابط کاربری کنسولی
        ConsoleUI consoleUI = new ConsoleUI(bookService, memberService, loanService);
        consoleUI.start();
    }

    /** چند کتاب و عضو نمونه برای تست راحت‌تر در اولین اجرا. */
    private static void loadSampleData(BookService bookService, MemberService memberService) {
        bookService.addBook("Clean Code", "Robert C. Martin", "978-0132350884", 2008);
        bookService.addBook("Effective Java", "Joshua Bloch", "978-0134685991", 2018);
        bookService.addBook("The Pragmatic Programmer", "Andrew Hunt", "978-0201616224", 1999);
        bookService.addBook("Refactoring", "Martin Fowler", "978-0134757599", 2018);

        memberService.register("Sara Ahmadi", "0912-111-0001", "sara@example.com");
        memberService.register("Ali Rezaei", "0912-111-0002", "ali@example.com");
        memberService.register("Maryam Karimi", "0912-111-0003", "maryam@example.com");
    }
}
