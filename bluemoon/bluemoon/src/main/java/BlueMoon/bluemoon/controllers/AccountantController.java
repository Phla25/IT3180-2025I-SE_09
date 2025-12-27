package BlueMoon.bluemoon.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.models.HoaDonStatsDTO;
import BlueMoon.bluemoon.models.InvoiceReportDTO;
import BlueMoon.bluemoon.services.ExportService;
import BlueMoon.bluemoon.services.HoGiaDinhService;
import BlueMoon.bluemoon.services.HoaDonService;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.ReportService;
import BlueMoon.bluemoon.utils.InvoiceType;

@Controller
@RequestMapping("/accountant")
public class AccountantController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired private HoGiaDinhService hoGiaDinhService;
    @Autowired private ReportService reportService;
    @Autowired private ExportService exportService;

    /**
     * Helper: Lấy đối tượng DoiTuong hiện tại (Kế Toán)
     */
    private DoiTuong getCurrentUser(Authentication auth) {
        String id = auth.getName(); // Lấy CCCD/ID từ principal
        // SỬ DỤNG SERVICE ĐÃ CÓ
        Optional<DoiTuong> userOpt = nguoiDungService.timKeToanTheoID(id);
        return userOpt.orElse(null); 
    }

    // =======================================================
    // DASHBOARD (Giữ nguyên)
    // =======================================================
    
    @GetMapping("/dashboard")
    public String showAccountantDashboard(Model model, Authentication auth) {
        
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=notfound";
        }
        
        // 1. Thông tin người dùng (cho header và sidebar)
        model.addAttribute("user", user);

        // 2. Lấy số liệu thống kê tài chính
        HoaDonStatsDTO stats = hoaDonService.getAccountantStats();
        
        // 3. Truyền các thông số riêng lẻ vào Model 
        model.addAttribute("tongThuThangNay", stats.tongThuThangNay);
        model.addAttribute("tongChuaThu", stats.tongChuaThanhToan);
        model.addAttribute("soHoaDonChuaThu", (long) stats.tongHoaDonChuaThanhToan);
        model.addAttribute("tongQuaHan", stats.tongQuaHan);
        model.addAttribute("soHoaDonQuaHan", (long) stats.soHoaDonQuaHan);
        
        // 4. Lấy danh sách hóa đơn cần xử lý
        List<HoaDon> hoaDonChoXacNhan = hoaDonService.getHoaDonChoXacNhan(5); 
        model.addAttribute("hoaDonCanXacNhan", hoaDonChoXacNhan); // Giữ tên biến để không sửa Thymeleaf quá nhiều

        return "dashboard-accountant"; 
    }
    
    // =======================================================
    // PROFILE
    // =======================================================
    @GetMapping("/profile")
    public String showAccountantProfile(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", user);
        return "profile-accountant"; // Tên file Thymeleaf đã tạo
    }
    // =======================================================
    // PROFILE EDIT / CHANGE PASSWORD
    // =======================================================
    // Hiển thị form Đổi Mật Khẩu
    @GetMapping("/change-password")
    public String showAccountantChangePasswordForm(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth); 
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user);
        return "change-password-accountant"; 
    }

    // Xử lý POST request Đổi Mật Khẩu
    @PostMapping("/change-password")
    public String handleAccountantChangePassword(@RequestParam("matKhauCu") String matKhauCu,
                                            @RequestParam("matKhauMoi") String matKhauMoi,
                                            @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
                                            Authentication auth,
                                            RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/accountant/profile";
        }

        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/accountant/change-password";
        }
        
        try {
            nguoiDungService.doiMatKhau(currentUser.getCccd(), matKhauCu, matKhauMoi);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/logout"; 
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/accountant/change-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/accountant/change-password";
        }
    }

    // Hiển thị form Cập Nhật Thông Tin Cá Nhân
    @GetMapping("/profile/edit")
    public String showAccountantEditProfileForm(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth); 
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user); 
        // Cần import Gender
        // model.addAttribute("genders", BlueMoon.bluemoon.utils.Gender.values());
        return "edit-profile-accountant"; 
    }

    // Xử lý POST request Cập Nhật Thông Tin Cá Nhân
    @PostMapping("/profile/edit")
    public String handleAccountantEditProfile(@ModelAttribute("user") DoiTuong doiTuongCapNhat,
                                        Authentication auth,
                                        RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null || !currentUser.getCccd().equals(doiTuongCapNhat.getCccd())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/accountant/profile";
        }
        
        try {
            nguoiDungService.capNhatThongTinNguoiDung(doiTuongCapNhat);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/accountant/profile";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/accountant/profile/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi cập nhật: " + e.getMessage());
            return "redirect:/accountant/profile/edit";
        }
    }    
    // =======================================================
    // QUẢN LÝ HÓA ĐƠN (CRUD)
    // =======================================================

    @GetMapping("/fees")
    public String showAccountantFees(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        List<HoaDon> hoaDonList = hoaDonService.getAllHoaDon(); 
        model.addAttribute("hoaDonList", hoaDonList);
        return "fees-accountant";
    }
    
    @GetMapping("/fee-form")
    public String showFeeForm(@RequestParam(value = "id", required = false) Integer maHoaDon, 
                              Model model, 
                              Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        HoaDon hoaDon = (maHoaDon != null) ? 
                        hoaDonService.getHoaDonById(maHoaDon).orElse(new HoaDon()) : 
                        new HoaDon();
        
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("pageTitle", (maHoaDon != null) ? "Chỉnh Sửa Hóa Đơn #" + maHoaDon : "Tạo Hóa Đơn Mới");
        model.addAttribute("invoiceTypes", InvoiceType.values()); 
        model.addAttribute("allHo", hoGiaDinhService.getAllHouseholds());
        
        return "invoice-add-edit-accountant"; 
    }
    
    // [MỚI] API JSON cho Kế toán
    @GetMapping("/api/households/{maHo}/members")
    public ResponseEntity<List<Map<String, String>>> getHouseholdMembers(@PathVariable String maHo) {
        HoGiaDinh hgd = hoGiaDinhService.getHouseholdById(maHo).orElse(null);
        if (hgd == null) return ResponseEntity.notFound().build();

        List<Map<String, String>> members = hgd.getThanhVienHoList().stream()
            .filter(tvh -> tvh.getNgayKetThuc() == null)
            .map(tvh -> {
                Map<String, String> map = new HashMap<>();
                map.put("cccd", tvh.getDoiTuong().getCccd());
                String role = tvh.getLaChuHo() ? " (Chủ hộ)" : "";
                map.put("hoVaTen", tvh.getDoiTuong().getHoVaTen() + role);
                return map;
            })
            .toList();
        return ResponseEntity.ok(members);
    }
    /**
     * XỬ LÝ TẠO HÓA ĐƠN HÀNG LOẠT (POST)
     * URL: /accountant/fees/batch-create
     */
    @PostMapping("/fees/batch-create")
    @SuppressWarnings("CallToPrintStackTrace")
    public String handleBatchCreateFee(
            @RequestParam("noiDung") String noiDung,
            @RequestParam("soTien") BigDecimal soTien,
            @RequestParam("hanThanhToan") LocalDate hanThanhToan,
            @RequestParam("loaiHoaDon") InvoiceType loaiHoaDon,
            @RequestParam("phamVi") String phamVi, // <--- THAM SỐ MỚI (HOUSEHOLD hoặc INDIVIDUAL)
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Truyền phamVi vào Service
            int count = hoaDonService.taoHoaDonHangLoat(noiDung, soTien, hanThanhToan, loaiHoaDon, phamVi);
            
            String doiTuongStr = "INDIVIDUAL".equals(phamVi) ? "cư dân" : "hộ gia đình";
            
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Thành công! Đã tạo " + count + " phiếu thu \"" + noiDung + "\" cho các " + doiTuongStr + ".");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tạo được hóa đơn nào.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/accountant/fees";
    }

    @PostMapping("/fee-save")
    public String handleFeeSave(@ModelAttribute("hoaDon") HoaDon hoaDon, 
                                @RequestParam("maHo") String maHo, 
                                @RequestParam(value = "nguoiDangKyCccd", required = false) String nguoiDangKyCccd,
                                // 👇 THAM SỐ FORM ĐỘNG
                                @RequestParam(value = "inputNoiDung", required = false) String inputNoiDung,
                                @RequestParam(value = "inputKyThu", required = false) String inputKyThu,
                                @RequestParam(value = "inputNgay", required = false) String inputNgay,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth);

        try {
            InvoiceType type = hoaDon.getLoaiHoaDon();
            String finalGhiChu = "";
            
            if (inputNoiDung == null) inputNoiDung = "";
            String contentUpper = inputNoiDung.trim().toUpperCase();

            if (null != type) // LOGIC GHÉP CHUỖI (Copy từ AdminController sang)
            switch (type) {
                case dich_vu -> {
                    if (inputKyThu == null || inputKyThu.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn Kỳ thu.");
                    String[] parts = inputKyThu.split("-"); // yyyy-MM
                    if (parts.length == 2) finalGhiChu = String.format("%s %s/%s", contentUpper, parts[1], parts[0]);
                    }
                case khac -> {
                    if (inputKyThu == null || inputKyThu.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn thời gian.");
                    String[] parts = inputKyThu.split("-");
                    if (parts.length == 2) finalGhiChu = String.format("DONG GOP %s %s/%s", contentUpper, parts[1], parts[0]);
                    }
                case sua_chua -> {
                    if (inputNgay == null || inputNgay.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ngày.");
                    String dateStr = formatDateVN(inputNgay);
                    finalGhiChu = String.format("SUA CHUA %s %s", contentUpper, dateStr);
                    }
                case phat -> {
                    if (inputNgay == null || inputNgay.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ngày.");
                    String dateStr = formatDateVN(inputNgay);
                    finalGhiChu = String.format("PHAT %s %s", contentUpper, dateStr);
                    }
                default -> {
                }
            }

            hoaDon.setGhiChu(finalGhiChu);

            // Kế toán chỉ tạo phiếu thu (isPhieuChi = false)
            hoaDonService.saveOrUpdateHoaDon(hoaDon, maHo, nguoiDangKyCccd, currentUser, false);
            
            redirectAttributes.addFlashAttribute("successMessage", "Lưu hóa đơn thành công!");
            return "redirect:/accountant/fees";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/accountant/fee-form?id=" + (hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : "");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/accountant/fees";
        }
    }

    private String formatDateVN(String yyyyMMdd) {
        try {
            LocalDate date = LocalDate.parse(yyyyMMdd);
            return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        } catch (Exception e) { return yyyyMMdd; }
    }
    /**
     * Xử lý Xóa Hóa đơn (POST).
     * URL: /accountant/fee-delete
     */
    @GetMapping("/fee-remove-action")
    public String handleDeleteFee(@RequestParam("id") Integer maHoaDon, 
                                  RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.deleteHoaDon(maHoaDon); 
            redirectAttributes.addFlashAttribute("successMessage", "Xóa Hóa đơn #" + maHoaDon + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa Hóa đơn #" + maHoaDon + ": " + e.getMessage());
        }
        return "redirect:/accountant/fees";
    }

    // =======================================================
    // BÁO CÁO TÀI CHÍNH VÀ XÁC NHẬN THANH TOÁN
    // =======================================================
    
    /**
     * Chức năng Xác nhận Thanh Toán
     * URL: /accountant/fee-confirm
     */
    @PostMapping("/fee-confirm")
    public String handleFeeConfirm(@RequestParam("maHoaDon") Integer maHoaDon, 
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.confirmPayment(maHoaDon, getCurrentUser(auth)); 
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt thanh toán Hóa đơn #" + maHoaDon);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/accountant/fees"; 
    }
    /**
     * Chức năng Từ Chối Xác nhận Thanh Toán
     * URL: /accountant/fee-reject
     */
    @PostMapping("/fee-reject")
    public String handleFeeReject(@RequestParam("maHoaDon") Integer maHoaDon, 
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
        
        try {
            hoaDonService.rejectPayment(maHoaDon, currentUser); 
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối xác nhận thanh toán Hóa đơn #" + maHoaDon + ". Hóa đơn chuyển về trạng thái 'Chưa thanh toán'.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi từ chối xác nhận: " + e.getMessage());
        }
        return "redirect:/accountant/fees"; 
    }
    // THÊM VÀO AccountantController.java

    /**
     * ✨ MỚI: Duyệt nhiều hóa đơn cùng lúc
     * URL: POST /accountant/fee-confirm-batch
     */
    @PostMapping("/fee-confirm-batch")
    public String handleBatchFeeConfirm(@RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                        Authentication auth,
                                        RedirectAttributes redirectAttributes) {
    
        if (selectedIds == null || selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một hóa đơn để duyệt.");
            return "redirect:/accountant/fees";
        }
    
        try {
            int successCount = hoaDonService.confirmMultiplePayments(selectedIds);
        
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã duyệt thành công " + successCount + "/" + selectedIds.size() + " hóa đơn!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }
    
        return "redirect:/accountant/fees";
    }

    /**
     * ✨ MỚI: Từ chối nhiều hóa đơn cùng lúc
     * URL: POST /accountant/fee-reject-batch
     */
    @PostMapping("/fee-reject-batch")
    public String handleBatchFeeReject(@RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
    
        if (selectedIds == null || selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một hóa đơn để từ chối.");
            return "redirect:/accountant/fees";
        }
    
        try {
            int successCount = hoaDonService.rejectMultiplePayments(selectedIds, currentUser);
        
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã từ chối " + successCount + "/" + selectedIds.size() + " hóa đơn!");
        
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }
    
        return "redirect:/accountant/fees";
    }
    
    // ========== EXPORT REPORTS ==========
    
    /**
     * Xuất báo cáo danh sách hóa đơn ra file Excel
     */
    @GetMapping("/export/invoices")
    public ResponseEntity<byte[]> exportInvoices() {
        try {
            List<InvoiceReportDTO> invoices = reportService.getInvoiceReportForAccountant();
            byte[] excelData = exportService.exportInvoicesToExcel(invoices);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoaDon_KeToan_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo hóa đơn ra file PDF
     */
    @GetMapping("/export/invoices/pdf")
    public ResponseEntity<byte[]> exportInvoicesPdf() {
        try {
            List<InvoiceReportDTO> invoices = reportService.getInvoiceReportForAccountant();
            byte[] pdfData = exportService.exportInvoicesToPdf(invoices);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoaDon_KeToan_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // =======================================================
    // IMPORT EXCEL (MỚI)
    // =======================================================

    @GetMapping("/fees/import")
    public String showImportFeesPage(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        return "fees-import-accountant"; // Trỏ đến file HTML sắp tạo
    }

    @PostMapping("/fees/import")
    public String handleImportFees(@RequestParam("file") MultipartFile file,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        DoiTuong user = getCurrentUser(auth);
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file Excel.");
            return "redirect:/accountant/fees/import";
        }

        try {
            String result = hoaDonService.importHoaDonFromExcel(file, user);
            
            if (result.contains("Thất bại")) {
                redirectAttributes.addFlashAttribute("errorMessage", result);
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result);
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/accountant/fees/import";
        }
        return "redirect:/accountant/fees";
    }
    
    // ========== EXPORT DETAIL ENDPOINTS ==========
    
    /**
     * Xuất chi tiết hóa đơn ra file Excel
     */
    @GetMapping("/export/invoice/{maHoaDon}")
    public ResponseEntity<byte[]> exportInvoiceDetail(@PathVariable Integer maHoaDon) {
        try {
            List<InvoiceReportDTO> invoice = reportService.getInvoiceDetailReport(maHoaDon);
            if (invoice.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] excelData = exportService.exportInvoicesToExcel(invoice);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "ChiTiet_HoaDon_" + maHoaDon + "_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất chi tiết hóa đơn ra file PDF
     */
    @GetMapping("/export/invoice/{maHoaDon}/pdf")
    public ResponseEntity<byte[]> exportInvoiceDetailPdf(@PathVariable Integer maHoaDon) {
        try {
            List<InvoiceReportDTO> invoice = reportService.getInvoiceDetailReport(maHoaDon);
            if (invoice.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdfData = exportService.exportInvoicesToPdf(invoice);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ChiTiet_HoaDon_" + maHoaDon + "_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // =======================================================
    // 1. BÁO CÁO TÀI CHÍNH (BIỂU ĐỒ & THỐNG KÊ)
    // URL: /accountant/reports/financial
    // =======================================================
    @GetMapping("/reports/financial")
    public String showFinancialReports(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        
        // 1. Thống kê tổng quan (Cards)
        HoaDonStatsDTO stats = hoaDonService.getAccountantStats();
        model.addAttribute("stats", stats);
        
        // 2. Dữ liệu biểu đồ doanh thu 6 tháng (MỚI)
        Map<String, BigDecimal> chartDataMap = hoaDonService.getMonthlyRevenueStats();
        model.addAttribute("chartLabels", chartDataMap.keySet());
        model.addAttribute("chartData", chartDataMap.values());
        
        return "financial-report-accountant"; // Trỏ đến file HTML mới
    }

    // =======================================================
    // 2. LỊCH SỬ GIAO DỊCH (DANH SÁCH ĐÃ THANH TOÁN)
    // URL: /accountant/history/transactions
    // =======================================================
    @GetMapping("/history/transactions")
    public String showTransactionHistory(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        
        // Lấy tất cả hóa đơn đã thanh toán
        List<HoaDon> paidInvoices = hoaDonService.getAllPaidHoaDon(); 
        
        // Sắp xếp: Mới nhất lên đầu (dựa theo ngày thanh toán)
        paidInvoices.sort((h1, h2) -> {
            if (h1.getNgayThanhToan() == null) return 1;
            if (h2.getNgayThanhToan() == null) return -1;
            return h2.getNgayThanhToan().compareTo(h1.getNgayThanhToan());
        });
        
        model.addAttribute("transactions", paidInvoices);
        
        return "transaction-history-accountant"; // Trỏ đến file HTML mới
    }
}