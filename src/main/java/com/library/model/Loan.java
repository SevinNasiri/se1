package com.library.model;

import java.time.LocalDate;

/**
 * نماینده یک تراکنش «امانت»: چه کتابی، به چه عضوی و در چه تاریخی.
 * تا وقتی returnDate برابر null باشد، امانت «فعال» است.
 */
public class Loan {

    /** طول دوره مجاز امانت (به روز) */
    public static final int LOAN_PERIOD_DAYS = 14;

    private final int id;               // شناسه یکتای امانت
    private final Book book;            // کتاب امانت‌رفته
    private final Member member;        // عضوی که کتاب را برده است
    private final LocalDate borrowDate; // تاریخ امانت گرفتن
    private LocalDate returnDate;       // تاریخ بازگشت (null یعنی هنوز برنگشته)

    public Loan(int id, Book book, Member member, LocalDate borrowDate) {
        this.id = id;
        this.book = book;
        this.member = member;
        this.borrowDate = borrowDate;
    }

    public int getId() { return id; }
    public Book getBook() { return book; }
    public Member getMember() { return member; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getReturnDate() { return returnDate; }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    /** تاریخ موعد بازگشت = تاریخ امانت + طول دوره امانت */
    public LocalDate getDueDate() {
        return borrowDate.plusDays(LOAN_PERIOD_DAYS);
    }

    /** آیا کتاب هنوز برگردانده نشده است؟ */
    public boolean isActive() {
        return returnDate == null;
    }

    /** آیا موعد گذشته ولی کتاب هنوز برنگشته است؟ */
    public boolean isOverdue() {
        return isActive() && LocalDate.now().isAfter(getDueDate());
    }

    @Override
    public String toString() {
        String status;
        if (!isActive()) {
            status = "returned on " + returnDate;
        } else if (isOverdue()) {
            status = "OVERDUE (due date was " + getDueDate() + ")";
        } else {
            status = "due on " + getDueDate();
        }
        return String.format("Loan #%d | \"%s\" | member: %s | borrowed: %s | %s",
                id, book.getTitle(), member.getFullName(), borrowDate, status);
    }
}
