package org.example.cunguoimoita.common.db;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool dùng chung (JNDI jdbc/cunguoimoita khai báo trong META-INF/context.xml).
 * DAO luôn dùng kèm PreparedStatement; close() trả kết nối về pool chứ không đóng hẳn.
 */
public final class ConnectionPool {

    private static final String JNDI_NAME = "java:comp/env/jdbc/cunguoimoita";

    private static ConnectionPool instance;

    private final DataSource dataSource;

    private ConnectionPool() {
        try {
            dataSource = (DataSource) new InitialContext().lookup(JNDI_NAME);
        } catch (NamingException e) {
            throw new IllegalStateException("Không tìm thấy " + JNDI_NAME
                    + ": copy META-INF/context.xml.example thành context.xml rồi sửa user/password", e);
        }
    }

    public static synchronized ConnectionPool getInstance() {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }

    /** Mượn một kết nối từ pool. */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Trả kết nối về pool (dùng khi không viết try-with-resources). */
    public void freeConnection(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
