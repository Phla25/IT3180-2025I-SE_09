package BlueMoon.bluemoon.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // Lưu ý package configs hay config tùy project của bạn
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import BlueMoon.bluemoon.configs.VNPayConfig;
import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.services.HoaDonService;
import BlueMoon.bluemoon.services.VNPayService;
import BlueMoon.bluemoon.utils.InvoiceStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PaymentController {

    @Autowired private HoaDonService hoaDonService;
    @Autowired private VNPayService vnPayService;
    @Autowired private DoiTuongDAO doiTuongDAO;

    // Đường dẫn gốc (Dùng localhost cho Mock)
    private final String BASE_URL = "http://localhost:8080";

    // =========================================================================
    // PHẦN 1: CLIENT - TẠO GIAO DỊCH VÀ NHẬN KẾT QUẢ
    // =========================================================================

    // 1. THANH TOÁN LẺ
    @GetMapping("/resident/payment/create-vnpay")
    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    public String createPayment(@RequestParam("id") Integer maHoaDon) {
        try {
            HoaDon hoaDon = hoaDonService.getHoaDonById(maHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Hóa đơn không tồn tại"));

            if (hoaDon.getTrangThai() == InvoiceStatus.da_thanh_toan) {
                return "redirect:/resident/invoices?error=paid";
            }

            String returnUrl = BASE_URL + "/resident/payment/vnpay-return";
            String paymentUrl = vnPayService.createPaymentUrl(hoaDon, returnUrl);
            
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/resident/fees?error=" + e.getMessage();
        }
    }

    // 2. THANH TOÁN GỘP (BATCH)
    @PostMapping("/resident/payment/pay-all")
    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    public String createBatchPayment(@RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                     HttpSession session) {
        try {
            if (selectedIds == null || selectedIds.isEmpty()) {
                return "redirect:/resident/fees?error=No invoices selected";
            }

            long totalAmount = 0;
            List<Integer> validIds = new ArrayList<>();

            for (Integer id : selectedIds) {
                HoaDon hd = hoaDonService.getHoaDonById(id).orElse(null);
                if (hd != null && hd.getTrangThai() != InvoiceStatus.da_thanh_toan) {
                    totalAmount += hd.getSoTien().longValue();
                    validIds.add(id);
                }
            }

            if (validIds.isEmpty()) {
                return "redirect:/resident/fees?error=Invalid invoices";
            }

            String batchTxnRef = "BATCH_" + System.currentTimeMillis();
            session.setAttribute(batchTxnRef, validIds);

            String returnUrl = BASE_URL + "/resident/payment/vnpay-return";
            String paymentUrl = vnPayService.createBatchPaymentUrl(totalAmount, batchTxnRef, returnUrl);

            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/resident/fees?error=" + e.getMessage();
        }
    }

    // 3. XỬ LÝ KẾT QUẢ TRẢ VỀ (RETURN URL)
    @GetMapping("/resident/payment/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, 
                              Principal principal,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) fields.remove("vnp_SecureHashType");
        if (fields.containsKey("vnp_SecureHash")) fields.remove("vnp_SecureHash");
        
        String signValue = VNPayConfig.hashAllFields(fields);
        
        if (signValue.equals(vnp_SecureHash)) {
            String vnp_TxnRef = request.getParameter("vnp_TxnRef");
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");

            if ("00".equals(vnp_ResponseCode)) {
                DoiTuong user = doiTuongDAO.findByCccd(principal.getName()).orElse(null);
                String maGD = "VNPAY-" + vnp_TransactionNo;

                if (vnp_TxnRef.startsWith("BATCH_")) {
                    List<Integer> ids = (List<Integer>) session.getAttribute(vnp_TxnRef);
                    if (ids != null) {
                        int count = 0;
                        for (Integer id : ids) {
                            hoaDonService.xuLyThanhToanThanhCong(id, user, maGD);
                            count++;
                        }
                        session.removeAttribute(vnp_TxnRef);
                        redirectAttributes.addFlashAttribute("successMessage", 
                            "Đã thanh toán thành công " + count + " hóa đơn! Mã GD: " + vnp_TransactionNo);
                    } else {
                        redirectAttributes.addFlashAttribute("errorMessage", 
                            "Thanh toán thành công nhưng phiên làm việc đã hết hạn.");
                    }
                } else {
                    String[] parts = vnp_TxnRef.split("_");
                    Integer maHoaDon = Integer.valueOf(parts[1]);
                    hoaDonService.xuLyThanhToanThanhCong(maHoaDon, user, maGD);
                    redirectAttributes.addFlashAttribute("successMessage", 
                        "Thanh toán thành công! Mã GD: " + vnp_TransactionNo);
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Giao dịch thất bại hoặc bị hủy bỏ. Mã lỗi: " + vnp_ResponseCode);
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi bảo mật: Chữ ký không hợp lệ!");
        }
        
        return "redirect:/resident/fees";
    }

    // =========================================================================
    // PHẦN 2: SERVER - GIẢ LẬP VNPAY (MOCK SERVER)
    // Phần này CỰC KỲ QUAN TRỌNG để chạy được Mock, đừng xóa!
    // =========================================================================

    // 4. HIỂN THỊ TRANG GIẢ LẬP
    @GetMapping("/mock-vnpay-portal")
    public String showMockPaymentPage(HttpServletRequest request, Model model) {
        // Lấy tham số từ URL
        model.addAttribute("vnp_Amount", request.getParameter("vnp_Amount"));
        model.addAttribute("vnp_TxnRef", request.getParameter("vnp_TxnRef"));
        model.addAttribute("vnp_OrderInfo", request.getParameter("vnp_OrderInfo"));
        model.addAttribute("vnp_ReturnUrl", request.getParameter("vnp_ReturnUrl"));
        
        // Trả về file HTML giao diện giả lập
        return "mock-vnpay-page"; 
    }

    // 5. XỬ LÝ NÚT XÁC NHẬN TRÊN TRANG GIẢ LẬP
    @PostMapping("/mock-vnpay-submit")
    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    public String processMockPayment(
            @RequestParam("vnp_TxnRef") String txnRef,
            @RequestParam("vnp_Amount") String amount,
            @RequestParam("vnp_ReturnUrl") String returnUrl,
            @RequestParam("responseCode") String responseCode 
    ) {
        try {
            // Giả lập tính toán của VNPay Server
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Amount", amount);
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_ResponseCode", responseCode);
            vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
            vnp_Params.put("vnp_TransactionNo", VNPayConfig.getRandomNumber(8));
            vnp_Params.put("vnp_TxnRef", txnRef);
            vnp_Params.put("vnp_PayDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            
            // Tạo chữ ký (Checksum)
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            
            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (i < fieldNames.size() - 1) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            
            String queryUrl = query.toString();
            String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            
            // Redirect về ứng dụng chính (Return URL)
            return "redirect:" + returnUrl + "?" + queryUrl;
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:" + returnUrl + "?vnp_ResponseCode=99";
        }
    }
}