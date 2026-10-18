package com.mediflow.pharmacy.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a user authentication login record in the database.
 * Stored in relational table `login_logs`.
 */
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String username;
    private String role;
    private LocalDateTime loginTime;
    private String loginTimeStr;
    private String ipAddress;
    private String status;

    public LoginLog() {
        this.loginTime = LocalDateTime.now();
        this.loginTimeStr = this.loginTime.format(FORMATTER);
        this.status = "SUCCESS";
    }

    public LoginLog(String username, String role, String ipAddress, String status) {
        this();
        this.username = username;
        this.role = role;
        this.ipAddress = ipAddress;
        this.status = status;
    }

    public LoginLog(Long id, String username, String role, LocalDateTime loginTime, String ipAddress, String status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.loginTime = loginTime;
        this.loginTimeStr = loginTime != null ? loginTime.format(FORMATTER) : "";
        this.ipAddress = ipAddress;
        this.status = status;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
        if (loginTime != null) {
            this.loginTimeStr = loginTime.format(FORMATTER);
        }
    }

    public String getLoginTimeStr() {
        return loginTimeStr;
    }

    public void setLoginTimeStr(String loginTimeStr) {
        this.loginTimeStr = loginTimeStr;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "LoginLog{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", loginTimeStr='" + loginTimeStr + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
