package BlueMoon.bluemoon.configs;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class VNPayConfig {
    
    // --- 1. CẤU HÌNH GIẢ LẬP (MOCK) ---
    // Thay vì https://sandbox.vnpayment.vn..., ta trỏ về đường dẫn nội bộ
    public static String vnp_PayUrl = "/mock-vnpay-portal"; 
    
    public static String vnp_ApiUrl = "https://vv3rq1lx-8080.asse.devtunnels.ms/api/mock-transaction";

    // --- 2. MÃ TEST TỰ QUY ĐỊNH (Dùng gì cũng được, miễn là thống nhất) ---
    public static String vnp_TmnCode = "TEST_DOAN"; 
    public static String vnp_HashSecret = "MYSECRETKEY1234567890MYSECRETKEY"; 

    // --- 3. CÁC HÀM TIỆN ÍCH (GIỮ NGUYÊN KHÔNG SỬA) ---
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (IllegalStateException | NullPointerException | InvalidKeyException | NoSuchAlgorithmException ex) { return ""; }
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
    
    public static String getIpAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    public static String hashAllFields(Map fields) {
        List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) sb.append("&");
        }
        return hmacSHA512(vnp_HashSecret, sb.toString());
    }
}