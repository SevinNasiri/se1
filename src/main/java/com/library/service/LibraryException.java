package com.library.service;

/**
 * خطای منطقی برنامه؛ مثلاً امانت گرفتن کتابی که موجود نیست.
 * پیام این خطا مستقیماً به کاربر نمایش داده می‌شود.
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }
}
