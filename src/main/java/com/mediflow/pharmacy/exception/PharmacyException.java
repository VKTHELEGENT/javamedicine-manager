package com.mediflow.pharmacy.exception;

/**
 * CST 205 OOPJ - Module 3: User Defined Exception
 * Custom exception class representing pharmacy business logic and database errors.
 */
public class PharmacyException extends Exception {

    private static final long serialVersionUID = 1L;

    // Default constructor
    public PharmacyException() {
        super("An error occurred in the Pharmacy system.");
    }

    // Parameterized constructor invoking superclass constructor
    public PharmacyException(String message) {
        super(message);
    }

    // Parameterized constructor with cause
    public PharmacyException(String message, Throwable cause) {
        super(message, cause);
    }
}
