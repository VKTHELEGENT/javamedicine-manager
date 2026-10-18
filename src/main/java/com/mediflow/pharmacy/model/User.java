package com.mediflow.pharmacy.model;

/**
 * CST 205 OOPJ - Module 2: Class, Object, Constructors and Encapsulation
 * Represents a User in the pharmacy management system (Patient or Faculty).
 */
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String role; // "PATIENT" or "FACULTY"

    // Default constructor
    public User() {
        this.role = "PATIENT";
    }

    // Parameterized constructor without ID
    public User(String username, String passwordHash, String email, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email != null ? email : "";
        this.role = role != null ? role.toUpperCase() : "PATIENT";
    }

    // Parameterized constructor with ID
    public User(Long id, String username, String passwordHash, String email, String role) {
        this(username, passwordHash, email, role); // Constructor chaining
        this.id = id;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role.toUpperCase() : "PATIENT";
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", role=" + role + ", email=" + email + "]";
    }
}
