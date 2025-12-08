package BlueMoon.bluemoon.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.LichSuRaVao;
import BlueMoon.bluemoon.entities.TaiSanChungCu; // Import TaiSanChungCu
import BlueMoon.bluemoon.entities.ThanhVienHo;
import BlueMoon.bluemoon.models.ApartmentReportDTO;
import BlueMoon.bluemoon.models.ResidentReportDTO;
import BlueMoon.bluemoon.services.BaoCaoSuCoService;
import BlueMoon.bluemoon.services.ExportService;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.ReportService;
import BlueMoon.bluemoon.services.TaiSanChungCuService; // Import TaiSanChungCuService
import BlueMoon.bluemoon.utils.Gender; // Cần thiết nếu dùng Enum trực tiếp
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;
import BlueMoon.bluemoon.services.CuDanService;
import BlueMoon.bluemoon.services.HoGiaDinhService;
import BlueMoon.bluemoon.services.LichSuRaVaoService;

@Controller
@RequestMapping("/officer")
public class OfficerController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired 
    private CuDanService cuDanService;

    @Autowired
    private HoGiaDinhService hoGiaDinhService;
    
    @Autowired
    private BaoCaoSuCoService suCoService; 
    
    // THÊM INJECT SERVICE CĂN HỘ
    @Autowired
    private TaiSanChungCuService taiSanChungCuService; 
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ExportService exportService; 
    
    @Autowired
    private LichSuRaVaoService lichSuRaVaoService;
    /**
     * Helper: Lấy đối tượng DoiTuong hiện tại (Cơ Quan Chức Năng)
     */
    private DoiTuong getCurrentUser(Authentication auth) {
        String id = auth.getName(); 
        Optional<DoiTuong> userOpt = nguoiDungService.timCoQuanChucNangTheoID(id);
        return userOpt.orElse(null); 
    }

    // =======================================================
    // DASHBOARD
    // =======================================================
    
    @GetMapping("/dashboard")
    public String showOfficerDashboard(Model model, Authentication auth) {
        
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=notfound";
        }
        
        // 1. Thông tin người dùng (cho header và sidebar)
        model.addAttribute("user", user);

        // 2. Lấy Thống kê chung về Sự Cố (Sử dụng Service thực tế)
        Long tongSuCo = suCoService.getTongSuCo();
        Long suCoDaXuLy = suCoService.getSuCoDaXuLy();
        Long suCoDangXuLy = suCoService.getSuCoDangXuLy();
        // Lấy sự cố mới tiếp nhận, theo logic của Service mới
        // Cần đảm bảo logic thống kê trên dashboard-officer.html khớp với logic Service.
        // Dựa vào file HTML, 'suCoChuaXuLy' được hiển thị riêng.
        // Ta dùng: suCoChuaXuLy = moi_tiep_nhan (như định nghĩa trong Service)
        Long suCoMoiTiepNhan = suCoService.getSuCoChuaXuLy();

        // 3. Thống kê theo Mức độ Ưu tiên
        Long suCoCao = suCoService.getSuCoTheoMucDo(PriorityLevel.cao);
        Long suCoTrungBinh = suCoService.getSuCoTheoMucDo(PriorityLevel.binh_thuong); // Dùng binh_thuong thay vì trung_binh
        Long suCoThap = suCoService.getSuCoTheoMucDo(PriorityLevel.thap);
        
        // 4. Danh sách Sự Cố Gần Đây (Lấy 5 bản ghi mới nhất)
        List<BaoCaoSuCo> danhSachSuCo = suCoService.getRecentIncidents(5); 

        // 5. Truyền dữ liệu vào Model
        model.addAttribute("tongSuCo", tongSuCo);
        model.addAttribute("suCoDaXuLy", suCoDaXuLy);
        model.addAttribute("suCoDangXuLy", suCoDangXuLy);
        model.addAttribute("suCoChuaXuLy", suCoMoiTiepNhan); // Đã sửa để khớp với Service: chỉ là "mới tiếp nhận"
        
        model.addAttribute("suCoCao", suCoCao);
        model.addAttribute("suCoTrungBinh", suCoTrungBinh);
        model.addAttribute("suCoThap", suCoThap);

        model.addAttribute("danhSachSuCo", danhSachSuCo);

        return "dashboard-officer"; 
    }
    
    // =======================================================
    // QUẢN LÝ CĂN HỘ (CHỈ XEM)
    // =======================================================
    
    // Trong OfficerController.java, thay thế phương thức showOfficerApartmentList

    /**
     * Hiển thị danh sách Căn Hộ cho Cơ Quan Chức Năng (GET) có hỗ trợ phân loại.
     */
    @GetMapping("/apartment-list") 
    public String showOfficerApartmentList(
            Model model, 
            Authentication auth,
            @RequestParam(required = false) BlueMoon.bluemoon.utils.AssetStatus status,
            @RequestParam(required = false) java.math.BigDecimal minArea,
            @RequestParam(required = false) java.math.BigDecimal maxArea,
            @RequestParam(required = false) java.math.BigDecimal minValue,
            @RequestParam(required = false) java.math.BigDecimal maxValue
        ) {
        
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=notfound";
        }
        
        model.addAttribute("user", user);

        // Lấy danh sách căn hộ dựa trên các bộ lọc
        List<TaiSanChungCu> apartments;
        
        if (minArea != null && maxArea != null && minArea.compareTo(maxArea) <= 0) {
            apartments = taiSanChungCuService.getApartmentsByAreaRange(minArea, maxArea);
        } else if (minValue != null && maxValue != null && minValue.compareTo(maxValue) <= 0) {
            apartments = taiSanChungCuService.getApartmentsByValueRange(minValue, maxValue);
        } else if (status != null) {
            apartments = taiSanChungCuService.getApartmentsByStatus(status);
        } else {
            apartments = taiSanChungCuService.getAllApartments();
        }
        
        model.addAttribute("apartments", apartments);
        // Lưu trữ các giá trị lọc để giữ lại trên form
        model.addAttribute("currentStatus", status);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        model.addAttribute("minValue", minValue);
        model.addAttribute("maxValue", maxValue);
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());
        
        return "apartment-list-officer"; // Trỏ đến file HTML dành riêng cho Officer
    }
    /**
     * Hiển thị chi tiết Căn Hộ cho Cơ Quan Chức Năng (GET)
     */
    @GetMapping("/apartment-details")
    public String showOfficerApartmentDetails(@RequestParam("maTaiSan") Integer maTaiSan, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        TaiSanChungCu apartment = taiSanChungCuService.getApartmentById(maTaiSan)
            .orElse(null);
        
        if (apartment == null) {
            model.addAttribute("errorMessage", "Không tìm thấy Căn hộ với Mã Tài Sản: " + maTaiSan);
            return "redirect:/officer/apartment-list";
        }

        model.addAttribute("apartment", apartment);
    
        // Tùy chọn: Thêm danh sách thành viên hộ liên kết (nếu có)
        if (apartment.getHoGiaDinh() != null) {
            List<ThanhVienHo> members = apartment.getHoGiaDinh().getThanhVienHoList().stream()
                .filter(tvh -> tvh.getNgayKetThuc() == null) 
                .toList();
            model.addAttribute("members", members);
        } else {
             model.addAttribute("members", List.of());
        }

        return "apartment-details-officer";
    }
    
    // =======================================================
    // PROFILE
    // =======================================================
    @GetMapping("/profile")
    public String showOfficerProfile(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", user);
        return "profile-officer"; // Tên file Thymeleaf đã tạo
    }
    // Hiển thị form Đổi Mật Khẩu
    @GetMapping("/change-password")
    public String showOfficerChangePasswordForm(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth); 
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user);
        return "change-password-officer"; 
    }

    // Xử lý POST request Đổi Mật Khẩu
    @PostMapping("/change-password")
    public String handleOfficerChangePassword(@RequestParam("matKhauCu") String matKhauCu,
                                            @RequestParam("matKhauMoi") String matKhauMoi,
                                            @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
                                            Authentication auth,
                                            RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/officer/profile";
        }

        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/officer/change-password";
        }
        
        try {
            nguoiDungService.doiMatKhau(currentUser.getCccd(), matKhauCu, matKhauMoi);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/logout"; 
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/officer/change-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/officer/change-password";
        }
    }

    // Hiển thị form Cập Nhật Thông Tin Cá Nhân
    @GetMapping("/profile/edit")
    public String showOfficerEditProfileForm(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth); 
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user); 
        model.addAttribute("genders", Gender.values());
        return "edit-profile-officer"; 
    }

    // Xử lý POST request Cập Nhật Thông Tin Cá Nhân
    @PostMapping("/profile/edit")
    public String handleOfficerEditProfile(@ModelAttribute("user") DoiTuong doiTuongCapNhat,
                                        Authentication auth,
                                        RedirectAttributes redirectAttributes) {
        
        DoiTuong currentUser = getCurrentUser(auth); 
        if (currentUser == null || !currentUser.getCccd().equals(doiTuongCapNhat.getCccd())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/officer/profile";
        }
        
        try {
            nguoiDungService.capNhatThongTinNguoiDung(doiTuongCapNhat);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/officer/profile";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/officer/profile/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi cập nhật: " + e.getMessage());
            return "redirect:/officer/profile/edit";
        }
    }
    // ========== EXPORT REPORTS ==========
    
    /**
     * Xuất báo cáo danh sách căn hộ ra file Excel
     */
    @GetMapping("/export/apartments")
    public ResponseEntity<byte[]> exportApartments() {
        try {
            List<ApartmentReportDTO> apartments = reportService.getApartmentReportForOfficer();
            byte[] excelData = exportService.exportApartmentsToExcel(apartments);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_CanHo_CQCN_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo danh sách cư dân ra file Excel
     */
    @GetMapping("/export/residents")
    public ResponseEntity<byte[]> exportResidents() {
        try {
            List<ResidentReportDTO> residents = reportService.getResidentReportForOfficer();
            byte[] excelData = exportService.exportResidentsToExcel(residents);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_CuDan_CQCN_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo danh sách căn hộ ra file PDF
     */
    @GetMapping("/export/apartments/pdf")
    public ResponseEntity<byte[]> exportApartmentsPdf() {
        try {
            List<ApartmentReportDTO> apartments = reportService.getApartmentReportForOfficer();
            byte[] pdfData = exportService.exportApartmentsToPdf(apartments);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_CanHo_CQCN_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất báo cáo danh sách cư dân ra file PDF
     */
    @GetMapping("/export/residents/pdf")
    public ResponseEntity<byte[]> exportResidentsPdf() {
        try {
            List<ResidentReportDTO> residents = reportService.getResidentReportForOfficer();
            byte[] pdfData = exportService.exportResidentsToPdf(residents);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_CuDan_CQCN_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ========== EXPORT DETAIL ENDPOINTS ==========
    
    /**
     * Xuất chi tiết căn hộ ra file Excel
     */
    @GetMapping("/export/apartment/{maTaiSan}")
    public ResponseEntity<byte[]> exportApartmentDetail(@PathVariable Integer maTaiSan) {
        try {
            List<ApartmentReportDTO> apartment = reportService.getApartmentDetailReport(maTaiSan);
            if (apartment.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] excelData = exportService.exportApartmentsToExcel(apartment);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "ChiTiet_CanHo_" + maTaiSan + "_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Xuất chi tiết căn hộ ra file PDF
     */
    @GetMapping("/export/apartment/{maTaiSan}/pdf")
    public ResponseEntity<byte[]> exportApartmentDetailPdf(@PathVariable Integer maTaiSan) {
        try {
            List<ApartmentReportDTO> apartment = reportService.getApartmentDetailReport(maTaiSan);
            if (apartment.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdfData = exportService.exportApartmentsToPdf(apartment);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ChiTiet_CanHo_" + maTaiSan + "_" + System.currentTimeMillis() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * Xem Sự Cố - Cơ Quan Chức Năng
     */
    // 1. Hiển thị danh sách sự cố (CÓ BỘ LỌC)
    @GetMapping("/incidents")
    public String showOfficerIncidents(Model model, 
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String reporterName, // Lọc người báo
                                     @RequestParam(required = false) String assetName, // Lọc theo tên tài sản (Nếu Service hỗ trợ, nếu chưa thì tạm bỏ qua hoặc dùng keyword)
                                     @RequestParam(required = false) IncidentStatus status,
                                     @RequestParam(required = false) PriorityLevel priority,
                                     @RequestParam(required = false) java.time.LocalDate reportDate,
                                     @RequestParam(required = false) Integer reportHour,
                                     Authentication auth) {
        
        DoiTuong user = getCurrentUser(auth);
        if (user == null) return "redirect:/login?error=notfound";
        model.addAttribute("user", user);

        // Gọi Service lọc dữ liệu (Tái sử dụng hàm của Admin)
        // Lưu ý: Nếu muốn lọc theo "Tên tài sản" riêng biệt, bạn cần nâng cấp Service. 
        // Ở đây ta tạm dùng keyword để tìm chung cho cả Tiêu đề/Nội dung.
        List<BaoCaoSuCo> suCoList = suCoService.filterSuCoAdmin(
            keyword, reporterName, status, priority, null, reportDate, reportHour
        );
        
        model.addAttribute("danhSachSuCo", suCoList);
        
        // Truyền lại giá trị để giữ trạng thái Form
        model.addAttribute("keyword", keyword);
        model.addAttribute("reporterName", reporterName);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("reportDate", reportDate);
        model.addAttribute("reportHour", reportHour);

        // Enum cho Dropdown
        model.addAttribute("incidentStatuses", IncidentStatus.values());
        model.addAttribute("priorityLevels", PriorityLevel.values());

        return "incident-officer"; 
    }

    // 2. API Lấy chi tiết sự cố cho Modal (QUAN TRỌNG: Trả về Fragment HTML)
    @GetMapping("/incidents/detail/{id}")
    public String getIncidentDetailForOfficer(@PathVariable Integer id, Model model) {
        // SỬA LẠI ĐOẠN NÀY: Không cần ép kiểu (BaoCaoSuCo) hay (Optional) nữa
        BaoCaoSuCo suCo = suCoService.getSuCoById(id).orElse(null);
        
        model.addAttribute("suCo", suCo);
        
        // Trả về Fragment
        return "incident-officer :: detailContent";
    }

    // =======================================================
    // BÁO CÁO THỐNG KÊ TỔNG HỢP
    // =======================================================

    /**
     * Hiển thị trang báo cáo thống kê tổng hợp cho Cơ quan chức năng
     * URL: /officer/reports
     */
    @GetMapping("/reports")
    public String showUnifiedReport(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        // === PHẦN 1: DỮ LIỆU TÀI SẢN CHUNG ===
        java.util.Map<String, Object> assetStats = taiSanChungCuService.getGeneralAssetStatistics();
        model.addAttribute("assetTypeLabels", assetStats.get("typeLabels"));
        model.addAttribute("assetTypeData", assetStats.get("typeData"));
        model.addAttribute("assetStatusLabels", assetStats.get("statusLabels"));
        model.addAttribute("assetStatusData", assetStats.get("statusData"));
        model.addAttribute("assetLocationLabels", assetStats.get("locationLabels"));
        model.addAttribute("assetLocationData", assetStats.get("locationData"));

        // Danh sách bảng tài sản
        model.addAttribute("assetList", taiSanChungCuService.getGeneralAssetListReport());

        // === PHẦN 2: DỮ LIỆU CƯ DÂN ===
        // A. Thống kê theo Tòa/Tầng
        java.util.Map<String, Object> buildingStats = taiSanChungCuService.getChartData();

        // Xử lý dữ liệu Tầng
        java.util.Map<Integer, Long> floorMap = (java.util.Map<Integer, Long>) buildingStats.get("floorStats");
        java.util.List<String> floorLabels = new java.util.ArrayList<>();
        java.util.List<Long> floorData = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, Long> e : floorMap.entrySet()) {
            floorLabels.add("Tầng " + e.getKey());
            floorData.add(e.getValue());
        }
        model.addAttribute("floorLabels", floorLabels);
        model.addAttribute("floorData", floorData);

        // B. Thống kê Giới tính & Trạng thái
        java.util.Map<String, Object> residentStats = cuDanService.getResidentStatistics();
        model.addAttribute("genderLabels", residentStats.get("genderLabels"));
        model.addAttribute("genderData", residentStats.get("genderData"));
        model.addAttribute("resStatusLabels", residentStats.get("resStatusLabels"));
        model.addAttribute("resStatusData", residentStats.get("resStatusData"));
        model.addAttribute("ageLabels", residentStats.get("ageLabels"));
        model.addAttribute("ageData", residentStats.get("ageData"));

        // === PHẦN 3: DỮ LIỆU HỘ GIA ĐÌNH ===
        java.util.Map<String, Object> householdStats = hoGiaDinhService.getHouseholdStatistics();
        model.addAttribute("householdFloorLabels", householdStats.get("householdFloorLabels"));
        model.addAttribute("householdFloorData", householdStats.get("householdFloorData"));
        model.addAttribute("averageMembers", householdStats.get("averageMembers"));
        model.addAttribute("totalHouseholds", householdStats.get("totalHouseholds"));
        model.addAttribute("totalMembers", householdStats.get("totalMembers"));
        model.addAttribute("householdSizeLabels", householdStats.get("householdSizeLabels"));
        model.addAttribute("householdSizeData", householdStats.get("householdSizeData"));

        return "reports-dashboard-officer";
    }
    // =======================================================
    // TRA CỨU LỊCH SỬ RA VÀO (MỚI)
    // =======================================================

    /**
     * Hiển thị danh sách ra vào cho Cơ Quan Chức Năng
     * URL: /officer/entry-exit-logs
     */
    @GetMapping("/entry-exit-logs")
    public String showOfficerEntryExitLogs(Model model, Authentication auth,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) LocalDate date,
                                           @RequestParam(required = false) String gate) {
        
        DoiTuong user = getCurrentUser(auth);
        if (user == null) return "redirect:/login?error=notfound";
        model.addAttribute("user", user);

        // Sử dụng chung hàm lọc của Service như Admin
        List<LichSuRaVao> logs = lichSuRaVaoService.getAllLogs(keyword, date, gate);

        model.addAttribute("logs", logs);
        
        // Truyền lại params để giữ form search
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentDate", date);
        model.addAttribute("currentGate", gate);
        
        model.addAttribute("gates", lichSuRaVaoService.getAllGates()); 

        return "entry-exit-log-officer"; // File HTML riêng cho Officer
    }
}