package org.example.cunguoimoita.product.service;

import jakarta.servlet.http.Part;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Lưu ảnh bài đăng vào thư mục <catalina.base>/uploads/products (ngoài thư mục web,
 * nên deploy lại không mất ảnh). Ảnh được đọc lại bằng ImageIO rồi ghi thành JPEG mới:
 * file không phải ảnh thật sẽ bị loại, ảnh quá to được thu nhỏ cho trang tải nhanh.
 * Ảnh được phục vụ qua ProductImageServlet tại /product-image/<uuid>.jpg
 */
public class ProductImageStore {

    public static final String URL_PREFIX = "/product-image/";
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_SIDE = 1600;

    /** Thư mục chứa ảnh; tạo nếu chưa có. */
    public static Path directory() throws IOException {
        String base = System.getProperty("catalina.base");
        if (base == null || base.isBlank()) {
            throw new IOException("Tomcat chưa thiết lập catalina.base để lưu ảnh bài đăng");
        }
        Path dir = Paths.get(base, "uploads", "products").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    /** Tên file hợp lệ: đúng dạng uuid.jpg, chặn mọi kiểu ../ */
    public static boolean isValidFileName(String name) {
        return name != null && name.matches("[0-9a-f-]{36}\\.jpg");
    }

    /**
     * Lưu một ảnh người dùng tải lên.
     * @return đường dẫn ảnh để lưu vào cột products.images
     * @throws IllegalArgumentException nếu file quá lớn hoặc không phải ảnh
     */
    public String save(Part part) throws IOException {
        if (part.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Mỗi ảnh tối đa 5 MB");
        }
        BufferedImage source;
        try (InputStream in = part.getInputStream()) {
            source = ImageIO.read(in);
        }
        if (source == null) {
            throw new IllegalArgumentException("File \"" + part.getSubmittedFileName() + "\" không phải ảnh PNG/JPEG/GIF");
        }
        String fileName = UUID.randomUUID() + ".jpg";
        Path target = directory().resolve(fileName);
        ImageIO.write(toJpegReady(source), "jpg", target.toFile());
        return URL_PREFIX + fileName;
    }

    /** Xoá file ảnh của một đường dẫn đã lưu (bỏ qua nếu không phải ảnh do lớp này tạo). */
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        String name = url.substring(URL_PREFIX.length());
        if (!isValidFileName(name)) {
            return;
        }
        try {
            Files.deleteIfExists(directory().resolve(name));
        } catch (IOException e) {
            // Ảnh thừa trên đĩa không ảnh hưởng tới người dùng, chỉ ghi log
            e.printStackTrace();
        }
    }

    /** Thu nhỏ cạnh dài về tối đa MAX_SIDE và vẽ lên nền trắng (JPEG không có nền trong suốt). */
    private BufferedImage toJpegReady(BufferedImage src) {
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
