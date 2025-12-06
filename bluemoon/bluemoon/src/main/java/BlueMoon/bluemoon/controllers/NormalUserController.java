package BlueMoon.bluemoon.controllers;

import java.io.IOException;
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

    @GetMapping("/resident/dashboard")
    public String residentDashboard(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", currentUser);

        // B1: Lấy thông tin Căn hộ/Hộ gia đình
        HoGiaDinhDTO canHoInfo = thanhVienHoService.getCanHoInfo(currentUser.getCccd(), currentUser.getHoVaTen());
        model.addAttribute("canHoInfo", canHoInfo);

        // B2: Lấy đối tượng HoGiaDinh (CẦN LOGIC THỰC TẾ TRONG TVH SERVICE)
        Optional<HoGiaDinh> hoGiaDinhOpt = thanhVienHoService.getHoGiaDinhByCccd(currentUser.getCccd()); 
        HoGiaDinh hoGiaDinh = hoGiaDinhOpt.orElse(null);
    
        // B3: Lấy Dữ liệu Hóa Đơn
        if (hoGiaDinh != null) {
            model.addAttribute("hoaDonStats", hoaDonService.getHoaDonStats(hoGiaDinh));
            model.addAttribute("recentHoaDon", hoaDonService.getRecentHoaDon(hoGiaDinh, 3));
        } else {
            // Tránh lỗi khi HoGiaDinh null
            model.addAttribute("hoaDonStats", new HoaDonStatsDTO()); 
            model.addAttribute("recentHoaDon", Collections.emptyList());
        }
    
        // B4: MOCK Dữ liệu còn lại (SỬA DỤNG DTO CÓ THUỘC TÍNH)
        
        // Mock DichVuStatsDTO
        // Mock DichVuStatsDTO (Bổ sung thiết lập trạng thái mặc định)
        DichVuStatsDTO dichVuStats = new DichVuStatsDTO();
        dichVuStats.setTongDichVu(0); // Đảm bảo số lượng dịch vụ là 0 khi mock
        dichVuStats.setTrangThai("Chưa đăng ký dịch vụ"); // Đặt giá trị cho trangThai
        model.addAttribute("dichVuStats", dichVuStats);
        
        // Mock SuCoStatsDTO (Sử dụng constructor có tham số)
        model.addAttribute("suCoStats", new SuCoStatsDTO(0, 0.0, 0.0));
        
        // Mock ThongBaoStatsDTO (Sử dụng constructor có tham số)
        model.addAttribute("thongBaoStats", new ThongBaoStatsDTO(0, "Không có thông báo mới")); 
        
        // Mock HoGiaDinhStatsDTO (Giả định đã tạo HoGiaDinhStatsDTO với constructor mặc định)
        model.addAttribute("hoGiaDinhStats", new HoGiaDinhDTO());

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
     * CẬP NHẬT: Xử lý yêu cầu thanh toán hóa đơn.
     * URL: /resident/fee-pay
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
            // Service cập nhật trạng thái và lưu CCCD của người yêu cầu thanh toán
            hoaDonService.markAsPaidByResident(maHoaDon, currentUser); 
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Yêu cầu thanh toán Hóa đơn #" + maHoaDon + " đã được ghi nhận. Vui lòng thực hiện chuyển khoản.");
            
            // ✨ THAY ĐỔI: Chuyển hướng người dùng đến trang chi tiết để xem QR code
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
        @GetMapping("/resident/notifications")
    public String hienThiThongBaoChoCuDan(Model model, Authentication auth) {
        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login?error=auth";
        }

        // Thêm thông tin người dùng vào model
        model.addAttribute("user", currentUser);

        // Lấy danh sách thông báo Entity từ service
        List<ThongBao> thongBaos = thongBaoService.layTatCaThongBaoMoiNhat();
        
        // ✅ CHUYỂN ĐỔI Entity sang DTO
        List<ThongBaoDTO> thongBaoDTOs = thongBaos.stream()
            .map(ThongBaoDTO::new) // Sử dụng constructor DTO
            .collect(Collectors.toList());
            
        // Truyền danh sách DTO vào model
        model.addAttribute("thongBaos", thongBaoDTOs);

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
}