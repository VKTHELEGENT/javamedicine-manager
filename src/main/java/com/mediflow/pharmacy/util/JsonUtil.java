package com.mediflow.pharmacy.util;

import com.mediflow.pharmacy.model.Medicine;
import java.util.*;

/**
 * Lightweight JSON parser and serializer using standard Java collections.
 * Handles API request bodies and response payloads without external dependencies.
 */
public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escapeString((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Medicine) {
            Medicine m = (Medicine) obj;
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"id\":").append(m.getId() == null ? "null" : m.getId()).append(",");
            sb.append("\"name\":\"").append(escapeString(m.getName())).append("\",");
            sb.append("\"quantity\":").append(m.getQuantity()).append(",");
            sb.append("\"available\":").append(m.getAvailable()).append(",");
            sb.append("\"details\":\"").append(escapeString(m.getDetails())).append("\"");
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof com.mediflow.pharmacy.model.LoginLog) {
            com.mediflow.pharmacy.model.LoginLog l = (com.mediflow.pharmacy.model.LoginLog) obj;
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"id\":").append(l.getId() == null ? "null" : l.getId()).append(",");
            sb.append("\"username\":\"").append(escapeString(l.getUsername())).append("\",");
            sb.append("\"role\":\"").append(escapeString(l.getRole())).append("\",");
            sb.append("\"loginTimeStr\":\"").append(escapeString(l.getLoginTimeStr())).append("\",");
            sb.append("\"ipAddress\":\"").append(escapeString(l.getIpAddress())).append("\",");
            sb.append("\"status\":\"").append(escapeString(l.getStatus())).append("\"");
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeString(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeString(obj.toString()) + "\"";
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(hex.substring(hex.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Parses a simple JSON object string into a Map of key-value pairs.
     */
    public static Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) return map;
        String s = json.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) return map;
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) return map;

        int len = s.length();
        int i = 0;
        while (i < len) {
            // Find key start
            while (i < len && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
            if (i >= len) break;
            if (s.charAt(i) != '"') break;
            i++; // skip open quote
            int keyStart = i;
            while (i < len && s.charAt(i) != '"') {
                if (s.charAt(i) == '\\' && i + 1 < len) i += 2;
                else i++;
            }
            String key = unescapeString(s.substring(keyStart, i));
            if (i < len && s.charAt(i) == '"') i++; // skip close quote

            // Skip to colon
            while (i < len && Character.isWhitespace(s.charAt(i))) i++;
            if (i < len && s.charAt(i) == ':') i++;
            while (i < len && Character.isWhitespace(s.charAt(i))) i++;

            // Read value
            if (i >= len) break;
            char firstChar = s.charAt(i);
            Object value;
            if (firstChar == '"') {
                i++;
                int valStart = i;
                while (i < len && s.charAt(i) != '"') {
                    if (s.charAt(i) == '\\' && i + 1 < len) i += 2;
                    else i++;
                }
                value = unescapeString(s.substring(valStart, i));
                if (i < len && s.charAt(i) == '"') i++;
            } else if (firstChar == '{') {
                int open = 1;
                int valStart = i;
                i++;
                while (i < len && open > 0) {
                    if (s.charAt(i) == '{') open++;
                    else if (s.charAt(i) == '}') open--;
                    i++;
                }
                value = parseJsonObject(s.substring(valStart, i));
            } else {
                int valStart = i;
                while (i < len && s.charAt(i) != ',' && s.charAt(i) != '}') i++;
                String rawVal = s.substring(valStart, i).trim();
                if ("true".equalsIgnoreCase(rawVal)) value = Boolean.TRUE;
                else if ("false".equalsIgnoreCase(rawVal)) value = Boolean.FALSE;
                else if ("null".equalsIgnoreCase(rawVal)) value = null;
                else {
                    try {
                        if (rawVal.contains(".")) value = Double.parseDouble(rawVal);
                        else value = Long.parseLong(rawVal);
                    } catch (NumberFormatException e) {
                        value = rawVal;
                    }
                }
            }
            map.put(key, value);
        }
        return map;
    }

    private static String unescapeString(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
