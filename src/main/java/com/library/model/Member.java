package com.library.model;

import java.time.LocalDate;

/**
 * نماینده یک عضو کتابخانه.
 * تاریخ عضویت به‌صورت خودکار هنگام ثبت‌نام ثبت می‌شود.
 */
public class Member {

    private final int id;             // شناسه یکتای عضو
    private final LocalDate joinDate; // تاریخ عضویت
    private String fullName;          // نام و نام خانوادگی
    private String phoneNumber;       // شماره تماس
    private String email;             // ایمیل (باید یکتا باشد)

    public Member(int id, String fullName, String phoneNumber, String email) {
        this(id, fullName, phoneNumber, email, LocalDate.now());
    }

    /** این سازنده هنگام خواندن عضو از دیتابیس استفاده می‌شود (تاریخ عضویتِ ذخیره‌شده). */
    public Member(int id, String fullName, String phoneNumber, String email, LocalDate joinDate) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.joinDate = joinDate;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public LocalDate getJoinDate() { return joinDate; }

    @Override
    public String toString() {
        return String.format("Member #%d | %s | %s | %s | joined: %s",
                id, fullName, phoneNumber, email, joinDate);
    }
}
