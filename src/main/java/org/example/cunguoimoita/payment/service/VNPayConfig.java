package org.example.cunguoimoita.payment.service;

import jakarta.servlet.ServletContext;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cấu hình kết nối cổng VNPay Sandbox v2.1.0 (HMAC-SHA512).
 * Hỗ trợ nạp cấu hình linh hoạt từ:
 *   1. context.xml: <Parameter name="vnpay.tmnCode" .../> và <Parameter name="vnpay.hashSecret" .../>
 *   2. Biến môi trường: VNP_TMN_CODE, VNP_HASH_SECRET
 *   3. File tài nguyên vnpay.properties trong classpath
 */
public class VNPayConfig {

    private static final Logger LOG = Logger.getLogger(VNPayConfig.class.getName());

    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_TMN_CODE = "CNMTDEMO";
    public static final String VNP_HASH_SECRET = "RAKHMOSUPOIKZAGACFOPURKVAARZNLOP";

    private static String tmnCode;
    private static String hashSecret;

    static {
        loadDefaults();
    }

    private static synchronized void loadDefaults() {
        // 1. Biến môi trường
        String envTmn = System.getenv("VNP_TMN_CODE");
        String envSec = System.getenv("VNP_HASH_SECRET");
        if (envTmn != null && !envTmn.isBlank()) tmnCode = envTmn.trim();
        if (envSec != null && !envSec.isBlank()) hashSecret = envSec.trim();

        // 2. System properties (-Dvnpay.tmnCode=... -Dvnpay.hashSecret=...)
        String propTmn = System.getProperty("vnpay.tmnCode");
        String propSec = System.getProperty("vnpay.hashSecret");
        if (propTmn != null && !propTmn.isBlank()) tmnCode = propTmn.trim();
        if (propSec != null && !propSec.isBlank()) hashSecret = propSec.trim();

        // 3. File vnpay.properties trong classpath (nếu có)
        try (InputStream in = VNPayConfig.class.getClassLoader().getResourceAsStream("vnpay.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String fileTmn = p.getProperty("vnpay.tmnCode");
                String fileSec = p.getProperty("vnpay.hashSecret");
                if (tmnCode == null && fileTmn != null && !fileTmn.isBlank()) tmnCode = fileTmn.trim();
                if (hashSecret == null && fileSec != null && !fileSec.isBlank()) hashSecret = fileSec.trim();
            }
        } catch (Exception e) {
            LOG.fine("Không tìm thấy vnpay.properties: " + e.getMessage());
        }

        // Mặc định sandbox demo
        if (tmnCode == null || tmnCode.isBlank()) {
            tmnCode = "CNMTDEMO";
        }
        if (hashSecret == null || hashSecret.isBlank()) {
            hashSecret = "RAKHMOSUPOIKZAGACFOPURKVAARZNLOP";
        }
    }

    /** Nạp thêm từ context.xml (web container) khi servlet khởi động. */
    public static synchronized void init(ServletContext ctx) {
        if (ctx != null) {
            String ctxTmn = ctx.getInitParameter("vnpay.tmnCode");
            String ctxSec = ctx.getInitParameter("vnpay.hashSecret");
            if (ctxTmn != null && !ctxTmn.isBlank()) tmnCode = ctxTmn.trim();
            if (ctxSec != null && !ctxSec.isBlank()) hashSecret = ctxSec.trim();
        }
    }

    public static synchronized String getTmnCode() {
        return (tmnCode != null && !tmnCode.isBlank()) ? tmnCode : "CNMTDEMO";
    }

    public static synchronized String getHashSecret() {
        return (hashSecret != null && !hashSecret.isBlank()) ? hashSecret : "RAKHMOSUPOIKZAGACFOPURKVAARZNLOP";
    }

    public static synchronized void setCredentials(String newTmnCode, String newHashSecret) {
        if (newTmnCode != null && !newTmnCode.isBlank()) tmnCode = newTmnCode.trim();
        if (newHashSecret != null && !newHashSecret.isBlank()) hashSecret = newHashSecret.trim();
    }

    /** Kiểm tra xem đã cấu hình mã merchant thật của VNPay chưa (khác mã demo rỗng). */
    public static boolean isConfigured() {
        String code = getTmnCode();
        return code != null && !code.isBlank() && !"CNMTDEMO".equalsIgnoreCase(code);
    }

    /** Tạo URL thanh toán chuyển hướng người mua sang cổng VNPay Sandbox. */
    public static String buildPaymentUrl(String orderCode, long amountVnd, String orderInfo, String ipAddress, String returnUrl) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNP_VERSION);
        vnp_Params.put("vnp_Command", VNP_COMMAND);
        vnp_Params.put("vnp_TmnCode", getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountVnd * 100)); // VNPay tính theo đơn vị đồng x100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderCode);
        vnp_Params.put("vnp_OrderInfo", orderInfo != null ? orderInfo : ("Thanh toan don hang " + orderCode));
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1");

        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(tz);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(tz);
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                first = false;
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return VNP_PAY_URL + "?" + queryUrl;
    }

    /** Kiểm tra chữ ký số trả về từ VNPay sử dụng key hiện hành. */
    public static boolean verifySignature(Map<String, String> fields, String secureHash) {
        return verifySignature(fields, secureHash, getHashSecret());
    }

    /** Kiểm tra chữ ký số trả về từ VNPay với key chỉ định (hữu ích cho unit test độc lập). */
    public static boolean verifySignature(Map<String, String> fields, String secureHash, String secretKey) {
        if (secureHash == null || secureHash.isBlank() || secretKey == null || secretKey.isBlank()) {
            return false;
        }
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                first = false;
            }
        }
        String calculated = hmacSHA512(secretKey, sb.toString());
        return calculated.equalsIgnoreCase(secureHash);
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                return "";
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
