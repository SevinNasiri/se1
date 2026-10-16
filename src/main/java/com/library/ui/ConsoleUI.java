package com.library.ui;

import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.service.BookService;
import com.library.service.LibraryException;
import com.library.service.LoanService;
import com.library.service.MemberService;

import java.util.List;
import java.util.Scanner;

/**
 * رابط کاربری کنسولی: نمایش منوها، خواندن ورودی و چاپ خروجی.
 * این کلاس فقط «نمایش» را انجام می‌دهد؛ تمام منطق در کلاس‌های service است.
 */
public class ConsoleUI {

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleUI(BookService bookService, MemberService memberService, LoanService loanService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
    }

    /** حلقه اصلی برنامه */
    public void start() {
        System.out.println("Welcome to the Library Management System!");
        System.out.println("Tip: a few sample books and members are preloaded for easy testing.\n");
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Select an option: ");
            switch (choice) {
                case 1 -> bookMenu();
                case 2 -> memberMenu();
                case 3 -> loanMenu();
                case 4 -> reportMenu();
                case 0 -> running = false;
                default -> System.out.println("Invalid option, please try again.\n");
            }
        }
        System.out.println("Goodbye!");
    }

    // ---------------- منوها ----------------

    private void printMainMenu() {
        System.out.print("""
                ========== MAIN MENU ==========
                1. Books
                2. Members
                3. Loans (borrow / return)
                4. Reports
                0. Exit
                ================================
                """);
    }

    private void bookMenu() {
        boolean back = false;
        while (!back) {
            System.out.print("""
                    ------------ BOOKS ------------
                    1. Add book
                    2. Remove book
                    3. Search books
                    4. List all books
                    0. Back
                    --------------------------------
                    """);
            int choice = readInt("Select an option: ");
            switch (choice) {
                case 1 -> addBook();
                case 2 -> removeBook();
                case 3 -> searchBooks();
                case 4 -> printBooks(bookService.getAll());
                case 0 -> back = true;
                default -> System.out.println("Invalid option, please try again.\n");
            }
        }
    }

    private void memberMenu() {
        boolean back = false;
        while (!back) {
            System.out.print("""
                    ----------- MEMBERS -----------
                    1. Register member
                    2. Remove member
                    3. Search members
                    4. List all members
                    0. Back
                    --------------------------------
                    """);
            int choice = readInt("Select an option: ");
            switch (choice) {
                case 1 -> registerMember();
                case 2 -> removeMember();
                case 3 -> searchMembers();
                case 4 -> printMembers(memberService.getAll());
                case 0 -> back = true;
                default -> System.out.println("Invalid option, please try again.\n");
            }
        }
    }

    private void loanMenu() {
        boolean back = false;
        while (!back) {
            System.out.print("""
                    ------------ LOANS ------------
                    1. Borrow a book
                    2. Return a book
                    3. List active loans
                    0. Back
                    --------------------------------
                    """);
            int choice = readInt("Select an option: ");
            switch (choice) {
                case 1 -> borrowBook();
                case 2 -> returnBook();
                case 3 -> printLoans(loanService.getActiveLoans());
                case 0 -> back = true;
                default -> System.out.println("Invalid option, please try again.\n");
            }
        }
    }

    private void reportMenu() {
        boolean back = false;
        while (!back) {
            System.out.print("""
                    ------------ REPORTS ------------
                    1. Library overview (statistics)
                    2. Currently borrowed books
                    3. Overdue loans
                    4. Borrow history of a member
                    5. Most borrowed books (top 3)
                    0. Back
                    ---------------------------------
                    """);
            int choice = readInt("Select an option: ");
            switch (choice) {
                case 1 -> printOverview();
                case 2 -> printLoans(loanService.getActiveLoans());
                case 3 -> printLoans(loanService.getOverdueLoans());
                case 4 -> printMemberHistory();
                case 5 -> printMostBorrowedBooks();
                case 0 -> back = true;
                default -> System.out.println("Invalid option, please try again.\n");
            }
        }
    }

    // ---------------- عملیات کتاب‌ها ----------------

    private void addBook() {
        System.out.println("\n--- Add book ---");
        String title = readNonEmpty("Title: ");
        String author = readNonEmpty("Author: ");
        String isbn = readNonEmpty("ISBN: ");
        int year = readInt("Publication year: ");
        try {
            Book book = bookService.addBook(title, author, isbn, year);
            System.out.println("Added successfully -> " + book + "\n");
        } catch (LibraryException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void removeBook() {
        System.out.println("\n--- Remove book ---");
        int id = readInt("Book id to remove: ");
        if (bookService.findById(id).isEmpty()) {
            System.out.println("No book found with id " + id + ".\n");
            return;
        }
        if (loanService.isBookBorrowed(id)) {
            System.out.println("Cannot remove: this book is currently borrowed and must be returned first.\n");
            return;
        }
        bookService.removeById(id);
        System.out.println("Book removed.\n");
    }

    private void searchBooks() {
        System.out.println("\n--- Search books ---");
        String keyword = readNonEmpty("Search by title / author / ISBN: ");
        printBooks(bookService.search(keyword));
    }

    // ---------------- عملیات اعضا ----------------

    private void registerMember() {
        System.out.println("\n--- Register member ---");
        String fullName = readNonEmpty("Full name: ");
        String phone = readNonEmpty("Phone number: ");
        String email = readNonEmpty("Email: ");
        try {
            Member member = memberService.register(fullName, phone, email);
            System.out.println("Registered successfully -> " + member + "\n");
        } catch (LibraryException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void removeMember() {
        System.out.println("\n--- Remove member ---");
        int id = readInt("Member id to remove: ");
        if (memberService.findById(id).isEmpty()) {
            System.out.println("No member found with id " + id + ".\n");
            return;
        }
        if (loanService.memberHasActiveLoans(id)) {
            System.out.println("Cannot remove: this member has books that are not returned yet.\n");
            return;
        }
        memberService.removeById(id);
        System.out.println("Member removed.\n");
    }

    private void searchMembers() {
        System.out.println("\n--- Search members ---");
        String keyword = readNonEmpty("Search by name / email / id: ");
        printMembers(memberService.search(keyword));
    }

    // ---------------- عملیات امانت ----------------

    private void borrowBook() {
        System.out.println("\n--- Borrow a book ---");
        int memberId = readInt("Member id: ");
        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            System.out.println("No member found with id " + memberId + ".\n");
            return;
        }
        int bookId = readInt("Book id: ");
        Book book = bookService.findById(bookId).orElse(null);
        if (book == null) {
            System.out.println("No book found with id " + bookId + ".\n");
            return;
        }
        try {
            Loan loan = loanService.borrowBook(book, member);
            System.out.println("Book borrowed successfully -> " + loan + "\n");
        } catch (LibraryException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void returnBook() {
        System.out.println("\n--- Return a book ---");
        int bookId = readInt("Book id to return: ");
        try {
            Loan loan = loanService.returnBook(bookId);
            System.out.println("Book returned successfully -> " + loan + "\n");
        } catch (LibraryException e) {
            System.out.println("Error: " + e.getMessage() + "\n");
        }
    }

    // ---------------- گزارش‌ها ----------------

    private void printOverview() {
        List<Book> allBooks = bookService.getAll();
        long availableCount = allBooks.stream().filter(Book::isAvailable).count();

        System.out.println("\n===== LIBRARY OVERVIEW =====");
        System.out.println("Total books:      " + allBooks.size());
        System.out.println("Available books:  " + availableCount);
        System.out.println("Borrowed books:   " + (allBooks.size() - availableCount));
        System.out.println("Total members:    " + memberService.getAll().size());
        System.out.println("Active loans:     " + loanService.getActiveLoans().size());
        System.out.println("Overdue loans:    " + loanService.getOverdueLoans().size() + "\n");
    }

    private void printMemberHistory() {
        System.out.println("\n--- Borrow history of a member ---");
        int memberId = readInt("Member id: ");
        if (memberService.findById(memberId).isEmpty()) {
            System.out.println("No member found with id " + memberId + ".\n");
            return;
        }
        printLoans(loanService.getHistoryForMember(memberId));
    }

    private void printMostBorrowedBooks() {
        List<LoanService.BookPopularity> popular = loanService.getMostBorrowedBooks(3);
        if (popular.isEmpty()) {
            System.out.println("No loans recorded yet.\n");
            return;
        }
        System.out.println("\n===== MOST BORROWED BOOKS =====");
        int rank = 1;
        for (LoanService.BookPopularity item : popular) {
            System.out.printf("%d. %-40s (%d time(s))%n", rank++, item.book().getTitle(), item.timesBorrowed());
        }
        System.out.println();
    }

    // ---------------- نمایش جدولی ----------------

    private void printBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.\n");
            return;
        }
        System.out.printf("%-4s %-32s %-22s %-6s %-18s %s%n",
                "ID", "Title", "Author", "Year", "ISBN", "Status");
        System.out.println("-".repeat(96));
        for (Book book : books) {
            System.out.printf("%-4d %-32s %-22s %-6d %-18s %s%n",
                    book.getId(),
                    truncate(book.getTitle(), 32),
                    truncate(book.getAuthor(), 22),
                    book.getPublicationYear(),
                    book.getIsbn(),
                    book.isAvailable() ? "Available" : "Borrowed");
        }
        System.out.println();
    }

    private void printMembers(List<Member> members) {
        if (members.isEmpty()) {
            System.out.println("No members found.\n");
            return;
        }
        System.out.printf("%-4s %-26s %-16s %-28s %s%n",
                "ID", "Name", "Phone", "Email", "Joined");
        System.out.println("-".repeat(84));
        for (Member member : members) {
            System.out.printf("%-4d %-26s %-16s %-28s %s%n",
                    member.getId(),
                    truncate(member.getFullName(), 26),
                    member.getPhoneNumber(),
                    truncate(member.getEmail(), 28),
                    member.getJoinDate());
        }
        System.out.println();
    }

    private void printLoans(List<Loan> loanList) {
        if (loanList.isEmpty()) {
            System.out.println("No loans found.\n");
            return;
        }
        System.out.printf("%-4s %-32s %-20s %-13s %-13s %s%n",
                "ID", "Book", "Member", "Borrowed", "Due", "Status");
        System.out.println("-".repeat(98));
        for (Loan loan : loanList) {
            String status = !loan.isActive() ? "Returned " + loan.getReturnDate()
                    : loan.isOverdue() ? "OVERDUE" : "Active";
            System.out.printf("%-4d %-32s %-20s %-13s %-13s %s%n",
                    loan.getId(),
                    truncate(loan.getBook().getTitle(), 32),
                    truncate(loan.getMember().getFullName(), 20),
                    loan.getBorrowDate(),
                    loan.getDueDate(),
                    status);
        }
        System.out.println();
    }

    // ---------------- ابزارهای خواندن ورودی ----------------

    /** خواندن یک عدد صحیح؛ تا زمانی که ورودی معتبر شود تکرار می‌کند. */
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    /** خواندن یک متن که نباید خالی باشد. */
    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Value cannot be empty, please try again.");
        }
    }

    /** کوتاه کردن متن‌های بلند تا ستون‌های جدول به‌هم نریزند. */
    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}
