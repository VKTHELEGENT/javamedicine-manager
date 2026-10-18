package com.mediflow.pharmacy.model;

import java.time.LocalDateTime;

/**
 * CST 205 OOPJ - Module 2: Class, Object, Constructors and Encapsulation
 * Represents a Password Reset verification code.
 */
public class ResetCode {

    private Long id;
    private String email;
    private String code;
    private LocalDateTime expires;
    private String userType; // "patient" or "faculty"
    private String username;

    // Default constructor
    public ResetCode() {
    }

    // Parameterized constructor without ID
    public ResetCode(String email, String code, LocalDateTime expires, String userType, String username) {
        this.email = email;
        this.code = code;
        this.expires = expires;
        this.userType = userType;
        this.username = username;
    }

    // Parameterized constructor with ID
    public ResetCode(Long id, String email, String code, LocalDateTime expires, String userType, String username) {
        this(email, code, expires, userType, username);
        this.id = id;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getExpires() {
        return expires;
    }

    public void setExpires(LocalDateTime expires) {
        this.expires = expires;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
