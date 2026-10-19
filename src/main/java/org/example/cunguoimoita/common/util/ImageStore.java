package org.example.cunguoimoita.common.util;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lưu ảnh người dùng gửi lên (ảnh trong chat, ảnh bằng chứng khiếu nại, ảnh báo cáo vi phạm)
 * vào thư mục <catalina.base>/uploads/<folder>, ngoài thư mục web nên deploy lại không mất ảnh.
 * Ảnh được đọc lại bằng ImageIO rồi ghi thành JPEG mới: file không phải ảnh bị loại, ảnh to được thu nhỏ.
 * Ảnh được phục vụ qua UploadImageServlet tại /uploads/<folder>/<uuid>.jpg
 * (cách làm giống ProductImageStore của bài đăng, tách riêng để không đụng vào phần F2).
 */
public class ImageStore {

    /** Các thư mục được phép, cũng là phần đường dẫn sau /uploads/ */
    public static final Set<String> FOLDERS = Set.of("chat", "disputes", "reports");
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_SIDE = 1600;

    private final String folder;

    public ImageStore(String folder) {
        if (!FOLDERS.contains(folder)) {
            throw new IllegalArgumentException("Thư mục ảnh không hợp lệ: " + folder);
        }
        this.folder = folder;
    }

    /** Thư mục chứa ảnh của một loại; tạo nếu chưa có. */
    public static Path directory(String folder) throws IOException {
        String base = System.getProperty("catalina.base");
        if (base == null || base.isBlank()) {
            throw new IOException("Tomcat chưa thiết lập catalina.base để lưu ảnh");
        }
        Path dir = Paths.get(base, "uploads", folder).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    /** Tên file hợp lệ: đúng dạng uuid.jpg, chặn mọi kiểu ../ */
    public static boolean isValidFileName(String name) {
        return name != null && name.matches("[0-9a-f-]{36}\\.jpg");
    }

    /**
     * Lưu một ảnh.
     * @return đường dẫn ảnh (vd /uploads/chat/<uuid>.jpg) để lưu vào DB
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
        ImageIO.write(toJpegReady(source), "jpg", directory(folder).resolve(fileName).toFile());
        return "/uploads/" + folder + "/" + fileName;
    }

    /**
     * Lưu mọi ảnh của một ô chọn nhiều file (input type=file multiple), bỏ qua ô trống.
     * @return danh sách đường dẫn, tối đa maxFiles ảnh
     */
    public List<String> saveAll(Iterable<Part> parts, String fieldName, int maxFiles) throws IOException {
        List<String> urls = new ArrayList<>();
        for (Part part : parts) {
            if (!fieldName.equals(part.getName()) || part.getSize() == 0) {
                continue;
            }
            if (urls.size() == maxFiles) {
                throw new IllegalArgumentException("Tối đa " + maxFiles + " ảnh");
            }
            urls.add(save(part));
        }
        return urls;
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
