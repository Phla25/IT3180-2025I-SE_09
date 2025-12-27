package BlueMoon.bluemoon.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DangKyDichVu;
import BlueMoon.bluemoon.entities.DichVu;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.entities.ThanhVienHo;
import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.models.ApartmentReportDTO;
import BlueMoon.bluemoon.models.DichVuStatsDTO;
import BlueMoon.bluemoon.models.HoGiaDinhDTO;
import BlueMoon.bluemoon.models.HoaDonStatsDTO;
import BlueMoon.bluemoon.models.HouseholdReportDTO;
import BlueMoon.bluemoon.models.InvoiceReportDTO;
import BlueMoon.bluemoon.models.PhanHoiThongBaoDTO;
import BlueMoon.bluemoon.models.SuCoStatsDTO;
import BlueMoon.bluemoon.models.ThongBaoDTO;
import BlueMoon.bluemoon.models.ThongBaoStatsDTO;
import BlueMoon.bluemoon.services.BaoCaoSuCoService;
import BlueMoon.bluemoon.services.DangKyDichVuService;
import BlueMoon.bluemoon.services.DichVuService;
import BlueMoon.bluemoon.services.ExportService;
import BlueMoon.bluemoon.services.HoaDonService;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.ReportService;
import BlueMoon.bluemoon.services.ThanhVienHoService;
import BlueMoon.bluemoon.services.ThongBaoService;
import BlueMoon.bluemoon.utils.InvoiceStatus;
import jakarta.servlet.http.HttpSession;

@Controller
public class NormalUserController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private ThanhVienHoService thanhVienHoService;
    @Autowired private HoaDonService hoaDonService;
    @Autowired private DichVuService dichVuService;
    @Autowired private DangKyDichVuService dangKyDichVuService;
    @Autowired private ReportService reportService;
    @Autowired private ExportService exportService;
    @Autowired private ThongBaoService thongBaoService;
    @Autowired
    private BaoCaoSuCoService baoCaoSuCoService;

    /**
     * Helper: Lấy đối tượng DoiTuong hiện tại
     * Giả sử username của principal là CCCD (đã được cấu hình trong UserDetailsService)
     */
    private DoiTuong getCurrentUser(Authentication auth) {
        String cccd = auth.getName(); // Lấy CCCD từ principal/username
        Optional<DoiTuong> userOpt = nguoiDungService.timNguoiDungThuongTheoCCCD(cccd);
        return userOpt.orElse(null); 
    }
    /**
     * Helper: Tải dữ liệu thông báo chung (Dùng cho Bell & Dropdown trên Header)
     * Có thể gọi hàm này trong các controller method khác để hiển thị chuông
     */
    private void loadNotificationData(Model model, DoiTuong currentUser) {
        List<ThongBao> thongBaos = thongBaoService.layThongBaoChoCuDan(currentUser.getCccd());
        List<ThongBaoDTO> thongBaoDTOs = thongBaos.stream().map(tb -> {
            ThongBaoDTO dto = new ThongBaoDTO(tb);
            dto.setDaDoc(thongBaoService.daDocThongBao(tb.getMaThongBao(), currentUser.getCccd()));
            return dto;
        }).collect(Collectors.toList());
        
        long unreadCount = thongBaoDTOs.stream().filter(tb -> !tb.isDaDoc()).count();

        model.addAttribute("thongBaos", thongBaoDTOs);
        model.addAttribute("unreadCount", unreadCount);
    }

    @GetMapping("/resident/dashboard")
    public String residentDashboard(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", currentUser);

        // --- 1. Load Thông báo (Cho cả Bell & Dashboard Card) ---
        // Gọi hàm helper để lấy danh sách và số lượng chưa đọc
        loadNotificationData(model, currentUser);
        
        // Lấy lại giá trị unreadCount đã được set trong loadNotificationData để dùng cho Card
        Long unreadCount = (Long) model.getAttribute("unreadCount");
        if (unreadCount == null) unreadCount = 0L;

        // Cập nhật DTO cho Card Dashboard: hiển thị số chưa đọc
        String tbStatus = unreadCount > 0 ? "Có " + unreadCount + " tin chưa đọc" : "Không có tin mới";
        model.addAttribute("thongBaoStats", new ThongBaoStatsDTO(unreadCount.intValue(), tbStatus));


        // --- 2. Lấy thông tin Căn hộ/Hộ gia đình ---
        HoGiaDinhDTO canHoInfo = thanhVienHoService.getCanHoInfo(currentUser.getCccd(), currentUser.getHoVaTen());
        model.addAttribute("canHoInfo", canHoInfo);

        Optional<HoGiaDinh> hoGiaDinhOpt = thanhVienHoService.getHoGiaDinhByCccd(currentUser.getCccd()); 
        HoGiaDinh hoGiaDinh = hoGiaDinhOpt.orElse(null);
    
        // --- 3. Lấy Dữ liệu Hóa Đơn ---
        if (hoGiaDinh != null) {
            model.addAttribute("hoaDonStats", hoaDonService.getHoaDonStats(hoGiaDinh));
            model.addAttribute("recentHoaDon", hoaDonService.getRecentHoaDon(hoGiaDinh, 3));
        } else {
            model.addAttribute("hoaDonStats", new HoaDonStatsDTO()); 
            model.addAttribute("recentHoaDon", Collections.emptyList());
        }
    
        // --- 4. Dữ liệu Dịch vụ ---
        DichVuStatsDTO dichVuStats = new DichVuStatsDTO();
        int tongDichVu = dangKyDichVuService.countDichVuDaDangKyByNguoiDung(currentUser.getCccd());
        dichVuStats.setTongDichVu(tongDichVu);
        String trangThaiDV = tongDichVu > 0 ? ("Đã đăng ký " + tongDichVu + " dịch vụ") : "Chưa đăng ký dịch vụ";
        dichVuStats.setTrangThai(trangThaiDV); 
        model.addAttribute("dichVuStats", dichVuStats);

        // --- 5. Dữ liệu Sự cố ---
        @SuppressWarnings("UnnecessaryUnboxing")
        int tongSuCo = baoCaoSuCoService.countAllSuCoByNguoiDung(currentUser.getCccd()).intValue();
        int soSuCoDaXuLy = baoCaoSuCoService.getSuCoDaXuLyTheoNguoiDung(currentUser.getCccd());
        
        // Tránh chia cho 0
        Double tyLeDaXuLy = (tongSuCo > 0) ? (double) soSuCoDaXuLy / tongSuCo * 100 : 0.0;
        
        int soSuCoDangXuLy = baoCaoSuCoService.countSuCoDangXuLyByNguoiDung(currentUser.getCccd());
        Double tyLeDangXuLy = (tongSuCo > 0) ? (double) soSuCoDangXuLy / tongSuCo * 100 : 0.0;
        
        model.addAttribute("suCoStats", new SuCoStatsDTO(tongSuCo, tyLeDaXuLy, tyLeDangXuLy));
        
        // --- 6. Dữ liệu Thống kê Hộ gia đình ---
        HoGiaDinhDTO hoGiaDinhStats = new HoGiaDinhDTO();
        hoGiaDinhStats.setTongThanhVien(thanhVienHoService.countThanhVienByHoGiaDinh(hoGiaDinh));
        hoGiaDinhStats.setMaCanHo(canHoInfo.getMaCanHo());
        hoGiaDinhStats.setChuHo(canHoInfo.getChuHo());
        model.addAttribute("hoGiaDinhStats", hoGiaDinhStats);

        return "dashboard-resident";
    }

    @GetMapping("/resident/profile")
    public String showResidentProfile(Model model, Authentication auth) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        
        if (currentUser == null) {
            // Trường hợp lỗi (ví dụ: Session hết hạn hoặc không tìm thấy user)
            return "redirect:/login?error=auth";
        }

        // 1. Thêm đối tượng user vào Model để hiển thị trong Thymeleaf
        model.addAttribute("user", currentUser);

        // 2. Trả về tên file Thymeleaf
        // Sử dụng tên mới để tránh nhầm lẫn với dashboard: profile-resident-detail.html
        return "profile-resident"; 
    }
    // NEW: Hiển thị form Cập Nhật Thông Tin Cá Nhân
    @GetMapping("/resident/profile/edit")
    public String showEditProfileForm(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", currentUser); 
        return "edit-profile-resident";
    }
    @PostMapping("/resident/profile/edit")
    public String handleEditProfile(@ModelAttribute("user") DoiTuong doiTuongCapNhat,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null || !currentUser.getCccd().equals(doiTuongCapNhat.getCccd())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng hoặc thông tin CCCD không khớp.");
            return "redirect:/resident/profile";
        }
        
        try {
            // Gọi Service để xử lý logic cập nhật thông tin
            nguoiDungService.capNhatThongTinNguoiDung(doiTuongCapNhat);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/resident/profile";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/resident/profile/edit"; // Quay lại form chỉnh sửa
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi cập nhật: " + e.getMessage());
            return "redirect:/resident/profile/edit";
        }
    }
    // Hiến thị trang đổi mật khẩu
    @GetMapping("/resident/change-password")
    public String showChangePasswordForm(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", currentUser); 
        return "change-password-resident";
    }
    // Ghi nhận, cập nhật đổi mật khẩu
    @PostMapping("/resident/change-password")
    public String handleChangePassword(@RequestParam("matKhauCu") String matKhauCu,
                                     @RequestParam("matKhauMoi") String matKhauMoi,
                                     @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/resident/profile";
        }

        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/resident/change-password";
        }
        
        try {
            // Gọi Service để xử lý logic đổi mật khẩu
            nguoiDungService.doiMatKhau(currentUser.getCccd(), matKhauCu, matKhauMoi);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại với mật khẩu mới.");
            // Chuyển hướng về trang đăng nhập sau khi đổi thành công để buộc người dùng đăng nhập lại
            return "redirect:/logout"; 
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/resident/change-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/resident/change-password";
        }
    }
    // =======================================================
    // QUẢN LÝ HÓA ĐƠN VÀ THANH TOÁN (MỚI)
    // =======================================================

    /**
     * Hiển thị danh sách tất cả hóa đơn của hộ gia đình hiện tại.
     * URL: /resident/fees
     */
    @GetMapping("/resident/fees")
    public String showResidentFees(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", currentUser);

        // 1. Lấy HoGiaDinh của người dùng
        Optional<HoGiaDinh> hoGiaDinhOpt = thanhVienHoService.getHoGiaDinhByCccd(currentUser.getCccd()); 
        HoGiaDinh hoGiaDinh = hoGiaDinhOpt.orElse(null);

        if (hoGiaDinh == null) {
            model.addAttribute("errorMessage", "Bạn chưa được liên kết với Hộ gia đình nào.");
            model.addAttribute("hoaDonList", Collections.emptyList());
        } else {
            // 2. Lấy tất cả hóa đơn của hộ gia đình (Cần thêm hàm này vào HoaDonService/DAO)
            List<HoaDon> hoaDonList = hoaDonService.getAllHoaDonByHo(hoGiaDinh); 
            model.addAttribute("hoaDonList", hoaDonList);
        }

        return "fees-resident"; // Tên file Thymeleaf mới
    }
    
    /**
     * Hiển thị chi tiết hóa đơn.
     * URL: /resident/fee-detail?id={maHoaDon}
     */
    @GetMapping("/resident/fee-detail")
    public String showFeeDetail(@RequestParam("id") Integer maHoaDon, Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", currentUser);

        Optional<HoGiaDinh> hoGiaDinhOpt = thanhVienHoService.getHoGiaDinhByCccd(currentUser.getCccd()); 
        HoGiaDinh hoGiaDinh = hoGiaDinhOpt.orElse(null);

        if (hoGiaDinh == null) {
            return "redirect:/resident/fees?error=no_household";
        }

        // 2. Lấy Hóa Đơn theo ID và Hộ gia đình (Cần thêm hàm này vào HoaDonService)
        Optional<HoaDon> hoaDonOpt = hoaDonService.getHoaDonByIdAndHo(maHoaDon, hoGiaDinh);
        
        if (hoaDonOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy Hóa đơn hoặc Hóa đơn không thuộc Hộ của bạn.");
            return "redirect:/resident/fees";
        }

        model.addAttribute("hoaDon", hoaDonOpt.get());
        
        return "fee-details-resident"; // Tên file Thymeleaf mới
    }

    /**
     * ✨ Xử lý yêu cầu thanh toán 1 hóa đơn - Chuyển sang trang chi tiết
     * URL: /resident/fee-pay (POST)
     */
    @PostMapping("/resident/fee-pay")
    public String handleFeePayment(@RequestParam("maHoaDon") Integer maHoaDon, 
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực.");
            return "redirect:/resident/fees";
        }
        
        try {
            // Cập nhật trạng thái hóa đơn sang "Chờ xác nhận"
            hoaDonService.markAsPaidByResident(maHoaDon, currentUser); 
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Yêu cầu thanh toán Hóa đơn #" + maHoaDon + " đã được ghi nhận. Vui lòng thực hiện chuyển khoản.");
            
            // ✨ Chuyển về trang chi tiết
            return "redirect:/resident/fee-detail?id=" + maHoaDon; 
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/resident/fee-detail?id=" + maHoaDon;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi thanh toán: " + e.getMessage());
            return "redirect:/resident/fee-detail?id=" + maHoaDon;
        }
    }

    /**
     * ✨ Xử lý thanh toán nhiều hóa đơn cùng lúc
     * URL: /resident/payment/pay-all (POST)
     */
    @PostMapping("/resident/payment/pay-all")
    public String handleBatchPayment(@RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                     Authentication auth,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=auth";
        }
        
        if (selectedIds == null || selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một hóa đơn để thanh toán.");
            return "redirect:/resident/fees";
        }
        
        try {
            int successCount = 0;
            BigDecimal tongTien = BigDecimal.ZERO;
            List<Integer> successIds = new ArrayList<>();
            
            // Cập nhật trạng thái từng hóa đơn
            for (Integer maHoaDon : selectedIds) {
                try {
                    HoaDon hd = hoaDonService.getHoaDonById(maHoaDon).orElse(null);
                    if (hd != null && hd.getTrangThai() != InvoiceStatus.da_thanh_toan) {
                        hoaDonService.markAsPaidByResident(maHoaDon, currentUser);
                        tongTien = tongTien.add(hd.getSoTien());
                        successIds.add(maHoaDon);
                        successCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi thanh toán hóa đơn #" + maHoaDon + ": " + e.getMessage());
                }
            }
            
            if (successCount > 0) {
                // ✨ Lưu thông tin vào session để hiển thị trên trang batch-payment-info
                session.setAttribute("batchPaymentIds", successIds);
                session.setAttribute("batchPaymentTotal", tongTien);
                session.setAttribute("batchPaymentCount", successCount);
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Đã tạo yêu cầu thanh toán cho " + successCount + " hóa đơn. Tổng tiền: " + 
                    tongTien.toString() + " ₫. Vui lòng chuyển khoản theo thông tin bên dưới.");
                
                // ✨ Chuyển sang trang hiển thị thông tin thanh toán gộp
                return "redirect:/resident/batch-payment-info";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không có hóa đơn nào được xử lý.");
                return "redirect:/resident/fees";
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/resident/fees";
        }
    }

    /**
     * ✨ Hiển thị trang thông tin thanh toán gộp
     * URL: /resident/batch-payment-info (GET)
     */
    @GetMapping("/resident/batch-payment-info")
    public String showBatchPaymentInfo(Model model, 
                                      Authentication auth,
                                      HttpSession session) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=auth";
        }
    
        // Lấy thông tin từ session
        @SuppressWarnings("unchecked")
        List<Integer> batchIds = (List<Integer>) session.getAttribute("batchPaymentIds");
        BigDecimal totalAmount = (BigDecimal) session.getAttribute("batchPaymentTotal");
        Integer count = (Integer) session.getAttribute("batchPaymentCount");
    
        if (batchIds == null || batchIds.isEmpty()) {
            return "redirect:/resident/fees?error=session_expired";
        }
    
        // Lấy danh sách hóa đơn chi tiết
        List<HoaDon> hoaDonList = new ArrayList<>();
        for (Integer id : batchIds) {
            hoaDonService.getHoaDonById(id).ifPresent(hoaDonList::add);
        }
    
        model.addAttribute("user", currentUser);
        model.addAttribute("hoaDonList", hoaDonList);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("invoiceCount", count);
        model.addAttribute("invoiceIds", batchIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", ")));
    
        return "batch-payment-info"; // ✨ Trang HTML mới
    }
    /**
     * Hiển thị danh sách Dịch vụ có thể đăng ký (Đang hoạt động)
     * URL: /resident/services
     */
    @GetMapping("/resident/services")
    public String showResidentServiceList(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        
        // 1. Lấy danh sách dịch vụ đang hoạt động
        List<DichVu> activeServices = dichVuService.getAllActiveDichVu(); // CẦN THÊM TRONG DichVuService
        
        model.addAttribute("activeServices", activeServices);
        return "service-list-resident"; 
    }
    /**
     * Hiển thị form Đăng ký Dịch vụ (GET)
     * URL: /resident/service-register-form?id={maDichVu}
     */
    @GetMapping("/resident/service-register-form")
    public String showServiceRegistrationForm(@RequestParam("id") Integer maDichVu, 
                                              Model model, 
                                              Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        model.addAttribute("user", currentUser);
        
        // 1. Lấy thông tin dịch vụ
        DichVu dichVu = dichVuService.getDichVuById(maDichVu)
            .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại."));
        
        // 2. Tạo đối tượng form mới
        DangKyDichVu dangKyYeuCau = new DangKyDichVu();
        dangKyYeuCau.setDichVu(dichVu);
        
        model.addAttribute("dichVu", dichVu);
        model.addAttribute("dangKyYeuCau", dangKyYeuCau);
        return "service-registration-resident";
    }
    /**
     * Xử lý Đăng ký Dịch vụ (POST)
     * URL: /resident/service-register-save
     */
    @PostMapping("/resident/service-register-save")
    public String handleServiceRegistration(@ModelAttribute("dangKyYeuCau") DangKyDichVu dangKyYeuCau,
                                            @RequestParam("maDichVu") Integer maDichVu, // Dùng maDichVu thay vì lấy từ object
                                            Authentication auth,
                                            RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
        
        // Thiết lập lại DichVu (do form chỉ gửi maDichVu)
        DichVu dichVu = dichVuService.getDichVuById(maDichVu)
            .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại."));
        dangKyYeuCau.setDichVu(dichVu);
        
        try {
            dangKyDichVuService.taoYeuCauDangKy(dangKyYeuCau, currentUser.getCccd());
            
            String message = dichVu.getGiaThanh().signum() > 0 
                             ? "Đăng ký dịch vụ thành công! Vui lòng thanh toán hóa đơn phí đăng ký."
                             : "Yêu cầu đăng ký dịch vụ đã được gửi, đang chờ Ban Quản Trị duyệt.";
                             
            redirectAttributes.addFlashAttribute("successMessage", message);
            return "redirect:/resident/my-services";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đăng ký dịch vụ: " + e.getMessage());
            return "redirect:/resident/service-register-form?id=" + maDichVu;
        }
    }
    
    /**
     * Hiển thị danh sách Dịch vụ đã đăng ký
     * URL: /resident/my-services
     */
    @GetMapping("/resident/my-services")
    public String showMyServices(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        model.addAttribute("user", currentUser);
        
        // Lấy danh sách dịch vụ đã đăng ký của người dùng
        List<DangKyDichVu> myRegistrations = dangKyDichVuService.getDichVuDaDangKyByCccd(currentUser.getCccd()); 
        
        model.addAttribute("myRegistrations", myRegistrations);
        return "my-services-resident"; 
    }
    
    /**
     * Chi tiết Đăng ký Dịch vụ
     * URL: /resident/service-registration-detail?id={maDangKy}
     */
    @GetMapping("/resident/service-registration-detail")
    public String showServiceRegistrationDetail(@RequestParam("id") Integer maDangKy, 
                                                Model model, 
                                                Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        model.addAttribute("user", currentUser);

        DangKyDichVu dkdv = dangKyDichVuService.getDangKyById(maDangKy)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký dịch vụ."));
        model.addAttribute("registration", dkdv);
        if (!dkdv.getNguoiDung().getCccd().equals(currentUser.getCccd())) {
            model.addAttribute("errorMessage", "Bạn không có quyền xem chi tiết đăng ký này.");
            return "redirect:/resident/my-services";
        }
        
        model.addAttribute("registration", dkdv);
        return "service-details-resident"; 
    }
    
    /**
     * Xử lý Hủy Đăng ký Dịch vụ
     * URL: /resident/service-cancel
     */
    @PostMapping("/resident/service-cancel")
    public String handleServiceCancel(@RequestParam("maDangKy") Integer maDangKy, 
                                      Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
        
        try {
            dangKyDichVuService.huyDangKyDichVu(maDangKy, currentUser.getCccd());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy yêu cầu đăng ký #" + maDangKy + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy đăng ký: " + e.getMessage());
        }
        
        return "redirect:/resident/my-services";
    }
        // ========== EXPORT REPORTS ==========
    
    /**
     * Xuất báo cáo căn hộ của cư dân ra file Excel
     */
    @GetMapping("/resident/export/apartments")
    public ResponseEntity<byte[]> exportResidentApartments(Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
            List<ApartmentReportDTO> apartments = reportService.getApartmentReportForResident(currentUser.getCccd());
            byte[] excelData = exportService.exportApartmentsToExcel(apartments);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_CanHo_CuDan_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo hóa đơn của cư dân ra file Excel
     */
    @GetMapping("/resident/export/invoices")
    public ResponseEntity<byte[]> exportResidentInvoices(Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
            List<InvoiceReportDTO> invoices = reportService.getInvoiceReportForResident(currentUser.getCccd());
            byte[] excelData = exportService.exportInvoicesToExcel(invoices);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoaDon_CuDan_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo hộ gia đình của cư dân ra file Excel
     */
    @GetMapping("/resident/export/household")
    public ResponseEntity<byte[]> exportResidentHousehold(Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
            List<HouseholdReportDTO> household = reportService.getHouseholdReportForResident(currentUser.getCccd());
            byte[] excelData = exportService.exportHouseholdsToExcel(household);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoGiaDinh_CuDan_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ========== PDF EXPORT ENDPOINTS ==========
    
    /**
     * Xuất báo cáo căn hộ của cư dân ra file PDF
     */
    @GetMapping("/resident/export/apartments/pdf")
    public ResponseEntity<byte[]> exportResidentApartmentsPdf(Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
            List<ApartmentReportDTO> apartments = reportService.getApartmentReportForResident(currentUser.getCccd());
            byte[] pdfData = exportService.exportApartmentsToPdf(apartments);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_CanHo_CuDan_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo hóa đơn của cư dân ra file PDF
     */
    @GetMapping("/resident/export/invoices/pdf")
    public ResponseEntity<byte[]> exportResidentInvoicesPdf(Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
            List<InvoiceReportDTO> invoices = reportService.getInvoiceReportForResident(currentUser.getCccd());
            byte[] pdfData = exportService.exportInvoicesToPdf(invoices);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoaDon_CuDan_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * API: Xem trước hóa đơn PDF (Inline Preview)
     * URL: /resident/invoice/preview/{maHoaDon}
     */
    // [MỚI] Hàm Preview PDF (Xem trực tiếp)
    @GetMapping("/resident/invoice/preview/{maHoaDon}")
    @SuppressWarnings("CallToPrintStackTrace")
public ResponseEntity<byte[]> previewInvoicePdf(@PathVariable Integer maHoaDon) {
    try {
        System.out.println("📄 Generating PDF preview for invoice: " + maHoaDon);
        
        // Lấy dữ liệu hóa đơn
        List<InvoiceReportDTO> invoice = reportService.getInvoiceDetailReport(maHoaDon);
        
        if (invoice.isEmpty()) {
            System.err.println("❌ Invoice not found: " + maHoaDon);
            return ResponseEntity.notFound().build();
        }
        
        System.out.println("✅ Invoice data retrieved: " + invoice.get(0).getMaHoaDon());
        
        // Tạo file PDF dạng byte[]
        byte[] pdfData = exportService.exportInvoicesToPdf(invoice);
        
        System.out.println("✅ PDF generated successfully. Size: " + pdfData.length + " bytes");
        
        // Kiểm tra nếu PDF quá nhỏ (có thể bị lỗi)
        if (pdfData.length < 100) {
            System.err.println("⚠️ Warning: PDF size too small, may be corrupted");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        
        // QUAN TRỌNG: "inline" giúp trình duyệt hiển thị file thay vì tải về
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition.inline()
                .filename("HoaDon_" + maHoaDon + ".pdf")
                .build()
        );
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
                
    } catch (IOException e) {
        System.err.println("❌ IOException when generating PDF: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.internalServerError().build();
        
    } catch (Exception e) {
        System.err.println("❌ Unexpected error: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.internalServerError().build();
    }
}
    
    /**
     * Xuất báo cáo hộ gia đình của cư dân ra file PDF
     */
    @GetMapping("/resident/export/household/pdf")
    public ResponseEntity<byte[]> exportResidentHouseholdPdf(Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
            List<HouseholdReportDTO> household = reportService.getHouseholdReportForResident(currentUser.getCccd());
            byte[] pdfData = exportService.exportHouseholdsToPdf(household);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoGiaDinh_CuDan_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ========== EXPORT DETAIL ENDPOINTS ==========
    
    /**
     * Xuất chi tiết hóa đơn ra file Excel
     */
    @GetMapping("/resident/export/invoice/{maHoaDon}")
    public ResponseEntity<byte[]> exportInvoiceDetail(@PathVariable Integer maHoaDon, Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
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
    @GetMapping("/resident/export/invoice/{maHoaDon}/pdf")
    public ResponseEntity<byte[]> exportInvoiceDetailPdf(@PathVariable Integer maHoaDon, Authentication auth) {
        try {
            DoiTuong currentUser = getCurrentUser(auth);
            if (currentUser == null) {
                return ResponseEntity.status(401).build();
            }
            
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
    // 1. API Ajax để đánh dấu đã đọc (Gọi khi click vào chuông hoặc xem chi tiết)
    @PostMapping("/resident/notifications/mark-read")
    @ResponseBody
    public ResponseEntity<String> markAsRead(@RequestParam("id") Integer maThongBao, Authentication auth) {
        thongBaoService.danhDauDaDoc(maThongBao, auth.getName());
        return ResponseEntity.ok("Success");
    }

    // 2. Cập nhật Dashboard/Trang thông báo để hiển thị chuông
    @GetMapping("/resident/notifications")
    public String hienThiThongBaoChoCuDan(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        model.addAttribute("user", currentUser);

        // Lấy danh sách
        List<ThongBao> thongBaos = thongBaoService.layThongBaoChoCuDan(currentUser.getCccd());
        
        // Convert sang DTO và check trạng thái đã đọc
        List<ThongBaoDTO> thongBaoDTOs = thongBaos.stream().map(tb -> {
            ThongBaoDTO dto = new ThongBaoDTO(tb);
            dto.setDaDoc(thongBaoService.daDocThongBao(tb.getMaThongBao(), currentUser.getCccd()));
            return dto;
        }).collect(Collectors.toList());
            
        model.addAttribute("thongBaos", thongBaoDTOs);
        
        // Đếm số chưa đọc để hiện lên Badge (Số đỏ trên chuông)
        long unreadCount = thongBaoService.demSoThongBaoChuaDoc(currentUser.getCccd());
        model.addAttribute("unreadCount", unreadCount);

        return "notifications-resident"; 
    }
    
    @GetMapping("/resident/notifications/{maThongBao}/replies")
    @ResponseBody
    public List<PhanHoiThongBaoDTO> layDanhSachPhanHoi(@PathVariable Integer maThongBao) {
        return thongBaoService.layPhanHoiTheoThongBao(maThongBao)
                .stream()
                .map(PhanHoiThongBaoDTO::new)
                .collect(Collectors.toList());
    }



    // B. Phương thức POST: Xử lý việc gửi phản hồi từ cư dân
    @PostMapping("/resident/notifications/reply")
    public String guiPhanHoi(@RequestParam("maThongBao") Integer maThongBao,
                             @RequestParam("noiDungPhanHoi") String noiDung,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {

        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/resident/notifications";
        }

        if (noiDung.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung phản hồi không được để trống.");
            return "redirect:/resident/notifications"; // hoặc quay lại trang modal
        }

        try {
            thongBaoService.themPhanHoi(maThongBao, currentUser, noiDung);
            redirectAttributes.addFlashAttribute("successMessage", "Phản hồi đã được gửi thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi gửi phản hồi.");
        }
        
        // Quay lại trang danh sách thông báo
        return "redirect:/resident/notifications"; 
    }

    //=======================================
    // BÁO CÁO SỰ CỐ CƯ DÂN (MỚI)
    //=======================================
    // 1. Xem danh sách sự cố của cư dân
    @GetMapping("/resident/incidents")
    public String showResidentIncidents(Model model, Authentication auth,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) BlueMoon.bluemoon.utils.IncidentType type,
                                        @RequestParam(required = false) BlueMoon.bluemoon.utils.PriorityLevel priority,
                                        @RequestParam(required = false) java.time.LocalDate date,
                                        @RequestParam(required = false) Integer hour) {
        
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) return "redirect:/login?error=auth";
        model.addAttribute("user", currentUser);

        // Gọi Service lọc dữ liệu thay vì lấy tất cả
        List<BaoCaoSuCo> myIncidents = baoCaoSuCoService.filterSuCoCuDan(
            currentUser.getCccd(), keyword, type, priority, date, hour
        );
        
        model.addAttribute("incidents", myIncidents);

        // Truyền lại các giá trị lọc để hiển thị trên form (giữ trạng thái)
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentType", type);
        model.addAttribute("currentPriority", priority);
        model.addAttribute("currentDate", date);
        model.addAttribute("currentHour", hour);

        // Truyền Enum để đổ vào dropdown
        model.addAttribute("incidentTypes", BlueMoon.bluemoon.utils.IncidentType.values());
        model.addAttribute("priorities", BlueMoon.bluemoon.utils.PriorityLevel.values());

        return "incidents-resident"; 
    }

    // 2. Hiển thị Form báo cáo sự cố
    @GetMapping("/resident/incident-report")
    public String showIncidentReportForm(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) return "redirect:/login?error=auth";
        model.addAttribute("user", currentUser);

        // Tạo đối tượng rỗng để binding form
        model.addAttribute("newIncident", new BaoCaoSuCo());
        
        // Truyền Enum IncidentType để hiển thị Dropdown
        model.addAttribute("incidentTypes", BlueMoon.bluemoon.utils.IncidentType.values());

        return "incident-report-resident"; // File HTML form
    }

    // 3. Xử lý Submit báo cáo
@PostMapping("/resident/incident-report")
    public String submitIncidentReport(@ModelAttribute("newIncident") BaoCaoSuCo incident,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) return "redirect:/login?error=auth";

        try {
            // CẬP NHẬT: Chỉ truyền incident và user, không cần tìm căn hộ nữa
            baoCaoSuCoService.taoBaoCaoTuCuDan(incident, currentUser);

            redirectAttributes.addFlashAttribute("successMessage", "Gửi báo cáo sự cố thành công! Đang chờ BQT phê duyệt.");
            return "redirect:/resident/incidents";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi báo cáo: " + e.getMessage());
            return "redirect:/resident/incident-report";
        }
    }
    @GetMapping("/resident/my-household")
    public String showMyApartment(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", currentUser);
        try {
            // Lấy thông tin hộ gia đình
            Optional<HoGiaDinh> hoGiaDinhOpt = thanhVienHoService.getHoGiaDinhByCccd(currentUser.getCccd());

            if (hoGiaDinhOpt.isEmpty()) {
                model.addAttribute("message", "Bạn chưa được đăng ký vào hộ gia đình nào.");
                model.addAttribute("maHo", "N/A");
                model.addAttribute("members", Collections.emptyList());
                return "my-assets-resident";
            }
            HoGiaDinh hoGiaDinh = hoGiaDinhOpt.get();
            String maHo = hoGiaDinh.getMaHo();

            // Lấy thông tin chủ hộ
            Optional<DoiTuong> chuHoOpt = thanhVienHoService.getChuHoByMaHo(maHo);
            String tenChuHo = chuHoOpt.isPresent() ? chuHoOpt.get().getHoVaTen() : "N/A";

            // Lấy danh sách thành viên bằng query trực tiếp (đã sort chủ hộ lên đầu)
            List<ThanhVienHo> members = thanhVienHoService.getActiveByMaHo(maHo);
            model.addAttribute("hoGiaDinh", hoGiaDinh);
            model.addAttribute("maHo", maHo);
            model.addAttribute("tenChuHo", tenChuHo);
            model.addAttribute("tongThanhVien", members.size());
            model.addAttribute("members", members);

        } catch (Exception e) {
            model.addAttribute("message", "Không thể tải thông tin căn hộ: " + e.getMessage());
            model.addAttribute("maHo", "N/A");
            model.addAttribute("members", Collections.emptyList());
        }

        return "my-household-resident";
    }
    
}