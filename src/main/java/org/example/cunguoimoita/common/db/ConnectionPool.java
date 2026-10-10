package org.example.cunguoimoita.common.db;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool dùng chung cho cả nhóm.
 * Pool do Tomcat quản lý (DBCP), khai báo trong src/main/webapp/META-INF/context.xml với tên jdbc/cunguoimoita.
 *
 * Cách dùng trong DAO (luôn kèm PreparedStatement):
 *   try (Connection c = ConnectionPool.getInstance().getConnection();
 *        PreparedStatement ps = c.prepareStatement("SELECT ... WHERE id = ?")) { ... }
 * close() (try-with-resources tự gọi) không đóng hẳn kết nối mà trả nó về pool.
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
