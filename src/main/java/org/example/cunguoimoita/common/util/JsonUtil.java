package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Hỗ trợ trả JSON nhỏ cho AJAX mà không cần thêm thư viện. */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20 || ch == '<' || ch == '>' || ch == '&') {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.append('"').toString();
    }

    public static void write(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }

    public static void error(HttpServletResponse resp, int status, String message) throws IOException {
        write(resp, status, "{\"ok\":false,\"error\":" + quote(message) + "}");
    }
}
