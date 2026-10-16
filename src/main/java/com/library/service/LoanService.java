package com.library.service;

import com.library.db.DatabaseManager;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * منطق امانت دادن / پس گرفتن کتاب و گزارش‌های مرتبط.
 * داده‌ها در جدول loans ذخیره می‌شوند؛ برای ساخت آبجکت‌های کامل Loan،
 * این جدول هنگام خواندن با جداول books و members ادغام (JOIN) می‌شود.
 */
public class LoanService {

    /** قاعده کسب‌وکار: حداکثر تعداد کتاب هم‌زمان برای هر عضو */
    private static final int MAX_ACTIVE_LOANS_PER_MEMBER = 3;

    /** کوئری مشترک: خواندن امانت‌ها همراه با اطلاعات کامل کتاب و عضو */
    private static final String SELECT_WITH_JOINS =
            "SELECT l.id AS loan_id, l.borrow_date, l.return_date, "
                    + "b.id AS b_id, b.title, b.author, b.isbn, b.publication_year, b.available, "
                    + "m.id AS m_id, m.full_name, m.phone_number, m.email, m.join_date "
                    + "FROM loans l "
                    + "JOIN books b ON b.id = l.book_id "
                    + "JOIN members m ON m.id = l.member_id ";

    /** رکورد کوچک برای گزارش «محبوب‌ترین کتاب‌ها» */
    public record BookPopularity(Book book, int timesBorrowed) {
    }

    /** امانت دادن کتاب به عضو، همراه با بررسی قواعد. ثبت امانت و تغییر وضعیت کتاب در یک transaction انجام می‌شود. */
    public Loan borrowBook(Book book, Member member) {
        if (!book.isAvailable()) {
            throw new LibraryException("This book is currently borrowed by another member.");
        }
        if (countActiveLoans(member.getId()) >= MAX_ACTIVE_LOANS_PER_MEMBER) {
            throw new LibraryException("This member already has the maximum of "
                    + MAX_ACTIVE_LOANS_PER_MEMBER + " active loans.");
        }
        LocalDate today = LocalDate.now();
        String insertLoanSql = "INSERT INTO loans (book_id, member_id, borrow_date, return_date) "
                + "VALUES (?, ?, ?, NULL)";
        String updateBookSql = "UPDATE books SET available = 0 WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false); // این دو تغییر اتمی‌اند: یا هر دو، یا هیچ‌کدام
            try (PreparedStatement insertLoan = connection.prepareStatement(insertLoanSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateBook = connection.prepareStatement(updateBookSql)) {
                insertLoan.setInt(1, book.getId());
                insertLoan.setInt(2, member.getId());
                insertLoan.setString(3, today.toString());
                int loanId = DatabaseManager.executeInsert(insertLoan);
                updateBook.setInt(1, book.getId());
                updateBook.executeUpdate();
                connection.commit();
                book.setAvailable(false); // هماهنگ‌کردن آبجکت در حافظه با دیتابیس
                return new Loan(loanId, book, member, today);
            } catch (SQLException e) {
                connection.rollback();
                throw new LibraryException("Database error while borrowing the book: " + e.getMessage());
            }
        } catch (SQLException e) { // خطای خودِ بازکردن اتصال یا rollback
            throw new LibraryException("Database error while borrowing the book: " + e.getMessage());
        }
    }

    /** پس گرفتن کتاب بر اساس شناسه کتاب (هر کتاب حداکثر یک امانت فعال دارد). */
    public Loan returnBook(int bookId) {
        String selectSql = SELECT_WITH_JOINS + "WHERE l.book_id = ? AND l.return_date IS NULL";
        LocalDate today = LocalDate.now();
        try (Connection connection = DatabaseManager.getConnection()) {
            // ۱) پیدا کردن امانت فعال این کتاب (همراه با کتاب و عضو)
            Loan loan;
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setInt(1, bookId);
                try (ResultSet resultSet = select.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new LibraryException("No active loan found for book id " + bookId + ".");
                    }
                    loan = mapRow(resultSet);
                }
            }
            // ۲) ثبت بازگشت و آزادکردن کتاب در یک transaction
            connection.setAutoCommit(false);
            try (PreparedStatement updateLoan = connection.prepareStatement(
                         "UPDATE loans SET return_date = ? WHERE id = ?");
                 PreparedStatement updateBook = connection.prepareStatement(
                         "UPDATE books SET available = 1 WHERE id = ?")) {
                updateLoan.setString(1, today.toString());
                updateLoan.setInt(2, loan.getId());
                updateLoan.executeUpdate();
                updateBook.setInt(1, loan.getBook().getId());
                updateBook.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new LibraryException("Database error while returning the book: " + e.getMessage());
            }
            loan.setReturnDate(today);
            loan.getBook().setAvailable(true);
            return loan;
        } catch (SQLException e) {
            throw new LibraryException("Database error while returning the book: " + e.getMessage());
        }
    }

    /** امانت‌های فعال (کتاب‌هایی که هنوز برنگشته‌اند). */
    public List<Loan> getActiveLoans() {
        String sql = SELECT_WITH_JOINS + "WHERE l.return_date IS NULL ORDER BY l.id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException e) {
            throw new LibraryException("Database error while listing active loans: " + e.getMessage());
        }
    }

    /** امانت‌های دارای تأخیر؛ قاعده تأخیر همان قاعده مدل Loan است، پس فیلتر نهایی در جاوا انجام می‌شود. */
    public List<Loan> getOverdueLoans() {
        return getActiveLoans().stream().filter(Loan::isOverdue).toList();
    }

    /** تاریخچه کامل امانت‌های یک عضو. */
    public List<Loan> getHistoryForMember(int memberId) {
        String sql = SELECT_WITH_JOINS + "WHERE l.member_id = ? ORDER BY l.id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while listing the member history: " + e.getMessage());
        }
    }

    /** آیا این کتاب الان امانت است؟ (برای جلوگیری از حذف کتاب امانت‌رفته) */
    public boolean isBookBorrowed(int bookId) {
        return countLoans("book_id = ? AND return_date IS NULL", bookId) > 0;
    }

    /** آیا این عضو الان کتابی امانت دارد؟ (برای جلوگیری از حذف عضو) */
    public boolean memberHasActiveLoans(int memberId) {
        return countLoans("member_id = ? AND return_date IS NULL", memberId) > 0;
    }

    /** محبوب‌ترین کتاب‌ها بر اساس تعداد کل دفعات امانت. */
    public List<BookPopularity> getMostBorrowedBooks(int limit) {
        String sql = "SELECT b.id AS b_id, b.title, b.author, b.isbn, b.publication_year, b.available, "
                + "COUNT(l.id) AS times_borrowed "
                + "FROM loans l JOIN books b ON b.id = l.book_id "
                + "GROUP BY b.id "
                + "ORDER BY times_borrowed DESC, b.id "
                + "LIMIT ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BookPopularity> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new BookPopularity(mapBook(resultSet), resultSet.getInt("times_borrowed")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while building the report: " + e.getMessage());
        }
    }

    // ---------------- ابزارهای داخلی ----------------

    private long countActiveLoans(int memberId) {
        return countLoans("member_id = ? AND return_date IS NULL", memberId);
    }

    /**
     * شمارش ردیف‌های جدول loans با یک شرط ثابت (شرط از کد خودمان می‌آید، نه از ورودی کاربر،
     * و مقدار پارامتر به‌صورت پارامتری bind می‌شود).
     */
    private long countLoans(String condition, int parameter) {
        String sql = "SELECT COUNT(*) FROM loans WHERE " + condition;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error while counting loans: " + e.getMessage());
        }
    }

    /** ساخت کتاب از ستون‌های books در نتیجه JOIN. */
    private Book mapBook(ResultSet resultSet) throws SQLException {
        Book book = new Book(
                resultSet.getInt("b_id"),
                resultSet.getString("title"),
                resultSet.getString("author"),
                resultSet.getString("isbn"),
                resultSet.getInt("publication_year"));
        book.setAvailable(resultSet.getBoolean("available"));
        return book;
    }

    /** ساخت یک Loan کامل (کتاب + عضو) از ردیف جاری نتیجه JOIN. */
    private Loan mapRow(ResultSet resultSet) throws SQLException {
        Book book = mapBook(resultSet);
        Member member = new Member(
                resultSet.getInt("m_id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone_number"),
                resultSet.getString("email"),
                LocalDate.parse(resultSet.getString("join_date")));
        Loan loan = new Loan(
                resultSet.getInt("loan_id"),
                book,
                member,
                LocalDate.parse(resultSet.getString("borrow_date")));
        String returnDate = resultSet.getString("return_date");
        if (returnDate != null) {
            loan.setReturnDate(LocalDate.parse(returnDate));
        }
        return loan;
    }

    /** خواندن همه ردیف‌های نتیجه کوئری و تبدیل به لیست Loan. */
    private List<Loan> mapAll(ResultSet resultSet) throws SQLException {
        List<Loan> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(mapRow(resultSet));
        }
        return result;
    }
}
