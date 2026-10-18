package com.mediflow.pharmacy.model;

/**
 * CST 205 OOPJ - Module 2: Class, Object, Constructors and Encapsulation
 * Represents a Medicine entity in the pharmacy inventory.
 */
public class Medicine {

    // Instance variables (Encapsulation)
    private Long id;
    private String name;
    private Integer quantity;
    private Boolean available;
    private String details;

    // Default constructor
    public Medicine() {
        this.quantity = 0;
        this.available = false;
        this.details = "";
    }

    // Parameterized constructor without ID (used when creating a new record before DB auto-generation)
    public Medicine(String name, Integer quantity, Boolean available, String details) {
        this.name = name;
        this.quantity = quantity;
        this.available = available;
        this.details = details != null ? details : "";
    }

    // Parameterized constructor with ID (used when reading from JDBC ResultSet)
    public Medicine(Long id, String name, Integer quantity, Boolean available, String details) {
        this(name, quantity, available, details); // Constructor chaining using this()
        this.id = id;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "Medicine[id=" + id + ", name=" + name + ", quantity=" + quantity + ", available=" + available + "]";
    }
}
