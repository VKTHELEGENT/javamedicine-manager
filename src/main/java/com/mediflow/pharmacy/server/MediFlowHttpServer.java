package com.mediflow.pharmacy.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.mediflow.pharmacy.db.MedicineDAO;
import com.mediflow.pharmacy.db.UserDAO;
import com.mediflow.pharmacy.db.ResetCodeDAO;
import com.mediflow.pharmacy.db.LoginLogDAO;
import com.mediflow.pharmacy.model.Medicine;
import com.mediflow.pharmacy.model.User;
import com.mediflow.pharmacy.model.ResetCode;
import com.mediflow.pharmacy.model.LoginLog;
import com.mediflow.pharmacy.util.JsonUtil;
import com.mediflow.pharmacy.util.PasswordUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP Server built entirely using Java Standard Edition (com.sun.net.httpserver).
 * Serves static web frontend assets and provides REST API endpoints connected to pure JDBC.
 */
public class MediFlowHttpServer {

    private final int port;
    private HttpServer server;
    private final MedicineDAO medicineDAO;
    private final UserDAO userDAO;
    private final ResetCodeDAO resetCodeDAO;
    private final LoginLogDAO loginLogDAO;
    private final Map<String, SessionUser> sessions = new ConcurrentHashMap<>();

    public static class SessionUser {
        public final String username;
        public final String role; // "patient" or "faculty"

        public SessionUser(String username, String role) {
            this.username = username;
            this.role = role.toLowerCase();
        }
    }

    public MediFlowHttpServer(int port) {
        this.port = port;
        this.medicineDAO = new MedicineDAO();
        this.userDAO = new UserDAO();
        this.resetCodeDAO = new ResetCodeDAO();
        this.loginLogDAO = new LoginLogDAO();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // API Contexts
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/medicines", new MedicineHandler());
        server.createContext("/api/logs", new LogsHandler());

        // Static File Serving Context
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("MediFlow HTTP Server active on http://localhost:" + port + "/");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Helper Methods ---

    private String getClientIp(HttpExchange exchange) {
        String xff = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        if (exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        return "127.0.0.1";
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Role, X-User-Name");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> err = new HashMap<>();
        err.put("error", message != null ? message : "An error occurred");
        sendJsonResponse(exchange, statusCode, JsonUtil.toJson(err));
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private SessionUser authenticateSession(HttpExchange exchange) {
        // 1. Check Session Cookie
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String c : cookies) {
                String[] pair = c.trim().split("=");
                if (pair.length == 2 && "mediflow_session".equals(pair[0])) {
                    SessionUser su = sessions.get(pair[1]);
                    if (su != null) return su;
                }
            }
        }

        // 2. Check X-User-Role / X-User-Name headers sent by client
        String roleHeader = exchange.getRequestHeaders().getFirst("X-User-Role");
        String nameHeader = exchange.getRequestHeaders().getFirst("X-User-Name");
        if (roleHeader != null && !roleHeader.trim().isEmpty()) {
            return new SessionUser(nameHeader != null ? nameHeader.trim() : "user", roleHeader.trim().toLowerCase());
        }

        return null;
    }

    private boolean isFaculty(HttpExchange exchange) {
        SessionUser su = authenticateSession(exchange);
        return su != null && "faculty".equalsIgnoreCase(su.role);
    }

    // --- Handlers ---

    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            try {
                if (path.endsWith("/login") && "POST".equals(method)) {
                    handleLogin(exchange);
                } else if (path.endsWith("/register") && "POST".equals(method)) {
                    handleRegister(exchange);
                } else if (path.endsWith("/logout") && "POST".equals(method)) {
                    handleLogout(exchange);
                } else if (path.endsWith("/session") && "GET".equals(method)) {
                    handleGetSession(exchange);
                } else if (path.endsWith("/forgot-password") && "POST".equals(method)) {
                    handleForgotPassword(exchange);
                } else if (path.endsWith("/reset-password") && "POST".equals(method)) {
                    handleResetPassword(exchange);
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Auth endpoint not found.\"}");
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }

        private void handleLogin(HttpExchange exchange) throws Exception {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);
            String username = req.get("username") != null ? req.get("username").toString().trim() : null;
            String password = req.get("password") != null ? req.get("password").toString() : null;
            String role = req.get("role") != null ? req.get("role").toString().trim() : null;

            if (username == null || username.isEmpty() || password == null || password.isEmpty() || role == null || role.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Username, password and role are required.\"}");
                return;
            }

            User user = userDAO.findByUsername(username);
            if (user == null) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Invalid username or password.\"}");
                return;
            }

            if (!user.getRole().equalsIgnoreCase(role)) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Access denied for this role.\"}");
                return;
            }

            if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Invalid username or password.\"}");
                return;
            }

            String token = UUID.randomUUID().toString();
            sessions.put(token, new SessionUser(user.getUsername(), user.getRole()));
            exchange.getResponseHeaders().set("Set-Cookie", "mediflow_session=" + token + "; Path=/; HttpOnly");

            // Record login event in database login_logs table
            String clientIp = getClientIp(exchange);
            try {
                loginLogDAO.recordLogin(user.getUsername(), user.getRole(), clientIp, "SUCCESS");
            } catch (Exception logEx) {
                System.err.println("Could not record login in database: " + logEx.getMessage());
            }

            Map<String, String> resp = new HashMap<>();
            resp.put("username", user.getUsername());
            resp.put("type", user.getRole().toLowerCase());
            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }

        private void handleRegister(HttpExchange exchange) throws Exception {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);
            String username = req.get("username") != null ? req.get("username").toString().trim() : null;
            String password = req.get("password") != null ? req.get("password").toString() : null;
            String email = req.get("email") != null ? req.get("email").toString().trim() : "";
            String role = req.get("role") != null ? req.get("role").toString().trim().toUpperCase() : "PATIENT";

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Username and password are required.\"}");
                return;
            }

            if (userDAO.findByUsername(username) != null) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Username already exists.\"}");
                return;
            }

            String hashed = PasswordUtil.hashPassword(password);
            User newUser = new User(username, hashed, email, role);
            userDAO.save(newUser);

            String token = UUID.randomUUID().toString();
            sessions.put(token, new SessionUser(newUser.getUsername(), newUser.getRole()));
            exchange.getResponseHeaders().set("Set-Cookie", "mediflow_session=" + token + "; Path=/; HttpOnly");

            // Record registration event in database login_logs table
            String clientIp = getClientIp(exchange);
            try {
                loginLogDAO.recordLogin(newUser.getUsername(), newUser.getRole(), clientIp, "REGISTERED");
            } catch (Exception logEx) {
                System.err.println("Could not record registration in database: " + logEx.getMessage());
            }

            Map<String, String> resp = new HashMap<>();
            resp.put("username", newUser.getUsername());
            resp.put("type", newUser.getRole().toLowerCase());
            sendJsonResponse(exchange, 201, JsonUtil.toJson(resp));
        }

        private void handleLogout(HttpExchange exchange) throws IOException {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            if (cookieHeader != null) {
                for (String c : cookieHeader.split(";")) {
                    String[] pair = c.trim().split("=");
                    if (pair.length == 2 && "mediflow_session".equals(pair[0])) {
                        sessions.remove(pair[1]);
                    }
                }
            }
            exchange.getResponseHeaders().set("Set-Cookie", "mediflow_session=; Path=/; Max-Age=0");
            sendJsonResponse(exchange, 200, "{\"message\":\"Logged out successfully.\"}");
        }

        private void handleGetSession(HttpExchange exchange) throws IOException {
            SessionUser su = authenticateSession(exchange);
            if (su != null) {
                Map<String, String> resp = new HashMap<>();
                resp.put("username", su.username);
                resp.put("type", su.role);
                sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
            } else {
                sendJsonResponse(exchange, 401, "{\"error\":\"No active session.\"}");
            }
        }

        private void handleForgotPassword(HttpExchange exchange) throws Exception {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);
            String email = req.get("email") != null ? req.get("email").toString().trim() : null;
            String userType = req.get("userType") != null ? req.get("userType").toString().trim() : null;

            if (email == null || userType == null) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Email and user role type are required.\"}");
                return;
            }

            User user = userDAO.findByEmailAndRole(email, userType);
            if (user == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"No account found matching this email.\"}");
                return;
            }

            Random random = new Random();
            String code = String.valueOf(100000 + random.nextInt(900000));
            LocalDateTime expires = LocalDateTime.now().plusMinutes(10);

            resetCodeDAO.deleteByEmail(email);
            resetCodeDAO.save(new ResetCode(email, code, expires, userType, user.getUsername()));

            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Verification code generated.");
            resp.put("code", code);
            sendJsonResponse(exchange, 200, JsonUtil.toJson(resp));
        }

        private void handleResetPassword(HttpExchange exchange) throws Exception {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseJsonObject(body);
            String email = req.get("email") != null ? req.get("email").toString().trim() : null;
            String code = req.get("code") != null ? req.get("code").toString().trim() : null;
            String newPassword = req.get("newPassword") != null ? req.get("newPassword").toString() : null;
            String confirmPassword = req.get("confirmPassword") != null ? req.get("confirmPassword").toString() : null;

            if (email == null || code == null || newPassword == null || confirmPassword == null) {
                sendJsonResponse(exchange, 400, "{\"error\":\"All fields are required.\"}");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Passwords do not match.\"}");
                return;
            }

            ResetCode rc = resetCodeDAO.findLatestByEmail(email);
            if (rc == null) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Password reset session not found or invalid email.\"}");
                return;
            }

            if (LocalDateTime.now().isAfter(rc.getExpires())) {
                resetCodeDAO.deleteById(rc.getId());
                sendJsonResponse(exchange, 401, "{\"error\":\"Verification code has expired. Request a new one.\"}");
                return;
            }

            if (!rc.getCode().equals(code)) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Invalid verification code.\"}");
                return;
            }

            userDAO.updatePassword(rc.getUsername(), PasswordUtil.hashPassword(newPassword));
            resetCodeDAO.deleteById(rc.getId());
            sendJsonResponse(exchange, 200, "{\"message\":\"Password reset successfully. Please login with your new password.\"}");
        }
    }

    private class MedicineHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            try {
                if (path.equals("/api/medicines") || path.equals("/api/medicines/")) {
                    if ("GET".equals(method)) {
                        List<Medicine> meds = medicineDAO.getAllMedicines();
                        sendJsonResponse(exchange, 200, JsonUtil.toJson(meds));
                    } else if ("POST".equals(method)) {
                        if (!isFaculty(exchange)) {
                            sendJsonResponse(exchange, 403, "{\"error\":\"For faculty/staff access only.\"}");
                            return;
                        }
                        String body = readRequestBody(exchange);
                        Map<String, Object> req = JsonUtil.parseJsonObject(body);
                        String name = req.get("name") != null ? req.get("name").toString().trim() : null;
                        Object qtyObj = req.get("quantity");
                        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : (qtyObj != null ? Integer.parseInt(qtyObj.toString()) : 0);
                        Object availObj = req.get("available");
                        boolean avail = availObj instanceof Boolean ? (Boolean) availObj : Boolean.parseBoolean(String.valueOf(availObj));
                        String details = req.get("details") != null ? req.get("details").toString().trim() : "";

                        if (name == null || name.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Medicine name is required.\"}");
                            return;
                        }
                        Medicine saved = medicineDAO.addMedicine(new Medicine(name, qty, avail, details));
                        sendJsonResponse(exchange, 201, JsonUtil.toJson(saved));
                    } else {
                        sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed.\"}");
                    }
                } else if (path.equals("/api/medicines/search")) {
                    String searchTerm = "";
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] p = param.split("=");
                            if (p.length == 2 && "q".equals(p[0])) {
                                searchTerm = URLDecoder.decode(p[1], StandardCharsets.UTF_8);
                            }
                        }
                    }
                    List<Medicine> matches = medicineDAO.searchMedicines(searchTerm);
                    sendJsonResponse(exchange, 200, JsonUtil.toJson(matches));
                } else if (path.matches("^/api/medicines/\\d+$")) {
                    String idStr = path.substring(path.lastIndexOf('/') + 1);
                    Long id = Long.parseLong(idStr);

                    if ("GET".equals(method)) {
                        Medicine med = medicineDAO.getMedicineById(id);
                        if (med != null) {
                            sendJsonResponse(exchange, 200, JsonUtil.toJson(med));
                        } else {
                            sendJsonResponse(exchange, 404, "{\"error\":\"Medicine not found.\"}");
                        }
                    } else if ("PUT".equals(method)) {
                        if (!isFaculty(exchange)) {
                            sendJsonResponse(exchange, 403, "{\"error\":\"For faculty/staff access only.\"}");
                            return;
                        }
                        Medicine med = medicineDAO.getMedicineById(id);
                        if (med == null) {
                            sendJsonResponse(exchange, 404, "{\"error\":\"Medicine not found.\"}");
                            return;
                        }
                        String body = readRequestBody(exchange);
                        Map<String, Object> req = JsonUtil.parseJsonObject(body);
                        if (req.containsKey("name") && req.get("name") != null) {
                            med.setName(req.get("name").toString().trim());
                        }
                        if (req.containsKey("quantity") && req.get("quantity") != null) {
                            Object q = req.get("quantity");
                            med.setQuantity(q instanceof Number ? ((Number) q).intValue() : Integer.parseInt(q.toString()));
                        }
                        if (req.containsKey("available") && req.get("available") != null) {
                            Object a = req.get("available");
                            med.setAvailable(a instanceof Boolean ? (Boolean) a : Boolean.parseBoolean(a.toString()));
                        }
                        if (req.containsKey("details") && req.get("details") != null) {
                            med.setDetails(req.get("details").toString().trim());
                        }

                        Medicine updated = medicineDAO.updateMedicine(med);
                        sendJsonResponse(exchange, 200, JsonUtil.toJson(updated));
                    } else if ("DELETE".equals(method)) {
                        if (!isFaculty(exchange)) {
                            sendJsonResponse(exchange, 403, "{\"error\":\"For faculty/staff access only.\"}");
                            return;
                        }
                        boolean deleted = medicineDAO.deleteMedicine(id);
                        if (deleted) {
                            sendJsonResponse(exchange, 200, "{\"message\":\"Medicine deleted successfully from inventory.\"}");
                        } else {
                            sendJsonResponse(exchange, 404, "{\"error\":\"Medicine not found.\"}");
                        }
                    } else {
                        sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed.\"}");
                    }
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Endpoint not found.\"}");
                }
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    private class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            SessionUser su = authenticateSession(exchange);
            if (su == null) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Authentication required to access login logs.\"}");
                return;
            }

            try {
                List<LoginLog> logs;
                if ("faculty".equalsIgnoreCase(su.role)) {
                    // Faculty can view all recent system login logs
                    logs = loginLogDAO.getAllLogs(100);
                } else {
                    // Patients view their personal login session logs
                    logs = loginLogDAO.getLogsByUsername(su.username, 20);
                }
                sendJsonResponse(exchange, 200, JsonUtil.toJson(logs));
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr == null || pathStr.equals("/") || pathStr.equals("/index.html")) {
                pathStr = "/index.html";
            }

            String mimeType = "application/octet-stream";
            String lower = pathStr.toLowerCase();
            if (lower.endsWith(".html")) mimeType = "text/html; charset=UTF-8";
            else if (lower.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
            else if (lower.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";
            else if (lower.endsWith(".svg")) mimeType = "image/svg+xml";
            else if (lower.endsWith(".png")) mimeType = "image/png";
            else if (lower.endsWith(".ico")) mimeType = "image/x-icon";

            byte[] fileBytes = null;
            String cleanPath = pathStr.startsWith("/") ? pathStr.substring(1) : pathStr;

            // 1. Try filesystem candidates
            Path[] candidatePaths = new Path[]{
                    Paths.get(cleanPath),
                    Paths.get("src/main/resources/static", cleanPath),
                    Paths.get("target/classes/static", cleanPath),
                    Paths.get("static", cleanPath)
            };
            for (Path p : candidatePaths) {
                if (Files.exists(p) && !Files.isDirectory(p)) {
                    fileBytes = Files.readAllBytes(p);
                    break;
                }
            }

            // 2. Try ClassLoader resources if not found on filesystem
            if (fileBytes == null) {
                InputStream is = getClass().getResourceAsStream("/static/" + cleanPath);
                if (is == null) {
                    is = getClass().getResourceAsStream("/" + cleanPath);
                }
                if (is != null) {
                    try (InputStream in = is) {
                        fileBytes = in.readAllBytes();
                    }
                }
            }

            if (fileBytes != null) {
                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.sendResponseHeaders(200, fileBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(fileBytes);
                os.close();
            } else {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(notFound.getBytes());
                os.close();
            }
        }
    }
}
