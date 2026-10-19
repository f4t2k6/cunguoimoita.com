package org.example.cunguoimoita.account.service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Đọc các trường scalar cần thiết từ JSON OAuth mà không thêm framework. */
final class OAuthJson {
    private OAuthJson() { }

    static String string(String json, String field) throws IOException {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field)
                + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescape(matcher.group(1));
    }

    static boolean bool(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field)
                + "\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE).matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String unescape(String value) throws IOException {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '\\') {
                result.append(ch);
                continue;
            }
            if (++i >= value.length()) {
                throw new IOException("JSON OAuth không hợp lệ");
            }
            char escaped = value.charAt(i);
            switch (escaped) {
                case '"' -> result.append('"');
                case '\\' -> result.append('\\');
                case '/' -> result.append('/');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (i + 4 >= value.length()) {
                        throw new IOException("JSON OAuth không hợp lệ");
                    }
                    try {
                        result.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                    } catch (NumberFormatException e) {
                        throw new IOException("JSON OAuth không hợp lệ", e);
                    }
                    i += 4;
                }
                default -> throw new IOException("JSON OAuth không hợp lệ");
            }
        }
        return result.toString();
    }
}
