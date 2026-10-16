package com.library.model;

/**
 * نماینده یک کتاب در کتابخانه.
 * هر کتاب یک شناسه (id) یکتا دارد که هنگام ساخت توسط BookService تعیین می‌شود.
 */
public class Book {

    private final int id;          // شناسه یکتای کتاب
    private final String isbn;     // شماره استاندارد بین‌المللی کتاب (باید یکتا باشد)
    private String title;          // عنوان کتاب
    private String author;         // نویسنده
    private int publicationYear;   // سال انتشار
    private boolean available;     // آیا کتاب برای امانت دادن موجود است؟

    public Book(int id, String title, String author, String isbn, int publicationYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.available = true; // هر کتاب در ابتدا قابل امانت است
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getPublicationYear() { return publicationYear; }
    public boolean isAvailable() { return available; }

    /** تغییر وضعیت موجودی کتاب (هنگام امانت دادن / پس گرفتن) */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        String status = available ? "Available" : "Borrowed";
        return String.format("Book #%d | %s | %s | %d | ISBN: %s | %s",
                id, title, author, publicationYear, isbn, status);
    }
}
