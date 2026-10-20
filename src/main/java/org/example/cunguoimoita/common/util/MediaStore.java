package org.example.cunguoimoita.common.util;

import jakarta.servlet.http.Part;
import org.example.cunguoimoita.common.dao.MediaDAO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Chỗ duy nhất lưu ảnh tải lên: đọc lại bằng ImageIO để loại file không phải ảnh, thu nhỏ, nén JPEG, lưu vào bảng media. */
public class MediaStore {

    public static final String URL_PREFIX = "/media/";
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    /** Cạnh dài tối đa sau khi thu nhỏ */
    public static final int AVATAR_SIDE = 512;
    public static final int PHOTO_SIDE = 1600;

    private final MediaDAO mediaDAO = new MediaDAO();

    /**
     * @param maxSide cạnh dài tối đa sau khi thu nhỏ (ảnh đại diện 512, ảnh khác 1600)
     * @return đường dẫn /media/<id>
     */
    public String save(Part part, String ownerId, int maxSide) throws IOException, SQLException {
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(toJpegReady(source, maxSide), "jpg", out);
        return URL_PREFIX + mediaDAO.insert(ownerId, "image/jpeg", out.toByteArray());
    }

    /** Lỗi giữa chừng thì xoá các ảnh đã lưu để không để ảnh thừa trong database. */
    public List<String> saveAll(Iterable<Part> parts, String fieldName, int maxFiles, String ownerId)
            throws IOException, SQLException {
        List<String> urls = new ArrayList<>();
        try {
            for (Part part : parts) {
                if (!fieldName.equals(part.getName()) || part.getSize() == 0) {
                    continue;
                }
                if (urls.size() == maxFiles) {
                    throw new IllegalArgumentException("Tối đa " + maxFiles + " ảnh");
                }
                urls.add(save(part, ownerId, PHOTO_SIDE));
            }
        } catch (IOException | SQLException | RuntimeException e) {
            urls.forEach(this::delete);
            throw e;
        }
        return urls;
    }

    /** Xoá ảnh theo đường dẫn đã lưu; đường dẫn kiểu cũ (ảnh nằm trên đĩa) thì bỏ qua. */
    public void delete(String url) {
        if (!isMediaUrl(url)) {
            return;
        }
        try {
            mediaDAO.delete(url.substring(URL_PREFIX.length()));
        } catch (SQLException e) {
            e.printStackTrace();   // ảnh thừa trong bảng không ảnh hưởng người dùng
        }
    }

    public static boolean isMediaUrl(String url) {
        return url != null && url.startsWith(URL_PREFIX);
    }

    /** Thu nhỏ cạnh dài về tối đa maxSide và vẽ lên nền trắng (JPEG không có nền trong suốt). */
    private BufferedImage toJpegReady(BufferedImage src, int maxSide) {
        double scale = Math.min(1.0, (double) maxSide / Math.max(src.getWidth(), src.getHeight()));
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
