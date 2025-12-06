package BlueMoon.bluemoon.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
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

import BlueMoon.bluemoon.daos.BaoCaoSuCoDAO;
import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.HoGiaDinhDAO;
import BlueMoon.bluemoon.daos.HoaDonDAO;
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DangKyDichVu;
import BlueMoon.bluemoon.entities.DichVu;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.entities.ThanhVienHo;
import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.models.ApartmentReportDTO;
import BlueMoon.bluemoon.models.HouseholdReportDTO;
import BlueMoon.bluemoon.models.InvoiceReportDTO;
import BlueMoon.bluemoon.models.PhanHoiThongBaoDTO;
import BlueMoon.bluemoon.models.ResidentReportDTO;
import BlueMoon.bluemoon.services.CuDanService;
import BlueMoon.bluemoon.services.DangKyDichVuService;
import BlueMoon.bluemoon.services.DichVuService;
import BlueMoon.bluemoon.services.ExportService;
import BlueMoon.bluemoon.services.HoGiaDinhService;
import BlueMoon.bluemoon.services.HoaDonService;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.PhanHoiThongBaoService;
import BlueMoon.bluemoon.services.ReportService;
import BlueMoon.bluemoon.services.TaiSanChungCuService;
import BlueMoon.bluemoon.services.ThongBaoService;
import BlueMoon.bluemoon.utils.AccountStatus;
import BlueMoon.bluemoon.utils.Gender;
import BlueMoon.bluemoon.utils.HouseholdStatus;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.InvoiceStatus;
import BlueMoon.bluemoon.utils.InvoiceType;
import BlueMoon.bluemoon.utils.PriorityLevel;
import BlueMoon.bluemoon.utils.ResidentStatus;
import BlueMoon.bluemoon.utils.TerminationReason;
import BlueMoon.bluemoon.utils.UserRole;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private HoGiaDinhService hoGiaDinhService;
    @Autowired
    private DichVuService dichVuService;

    private DoiTuong getCurrentUser(Authentication auth) {
        String id = auth.getName();
        Optional<DoiTuong> userOpt = nguoiDungService.timBanQuanTriTheoID(id);
        return userOpt.orElse(null);
    }

    @Autowired
    private CuDanService cuDanService;
    @Autowired
    private HoGiaDinhDAO hoGiaDinhDAO;
    @Autowired
    private BaoCaoSuCoDAO suCoDAO;
    @Autowired
    private HoaDonDAO hoaDonDAO;
    @Autowired
    private TaiSanChungCuService taiSanChungCuService;
    @Autowired
    private HoaDonService hoaDonService;
    @Autowired
    private DangKyDichVuService dangKyDichVuService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ExportService exportService;
    @Autowired
    private ThongBaoService thongBaoService;
    @Autowired
    private DoiTuongDAO doiTuongDAO;
    @Autowired
    private PhanHoiThongBaoService phanHoiThongBaoService;

    @SuppressWarnings("unchecked")
    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model, Authentication auth) {

        // 1. Xác thực người dùng
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=notfound";
        }
        model.addAttribute("user", user);

        // 2. Thống kê số liệu tổng quan (Cards)
        model.addAttribute("tongCuDan", cuDanService.layDanhSachCuDan().size());
        model.addAttribute("tongHoGiaDinh", hoGiaDinhDAO.countAll());

        long suCoChuaXuLy = suCoDAO.countByTrangThai(IncidentStatus.moi_tiep_nhan);
        long suCoDangXuLy = suCoDAO.countByTrangThai(IncidentStatus.dang_xu_ly);
        model.addAttribute("suCoChuaXuLy", suCoChuaXuLy + suCoDangXuLy);

        BigDecimal tongThu = hoaDonDAO.sumSoTienByTrangThai(InvoiceStatus.da_thanh_toan);
        model.addAttribute("doanhThuThang", tongThu);

        // 3. Thống kê tỷ lệ (Progress bars)
        long tongSuCo = suCoDAO.countAll();
        long suCoDaXuLy = suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh);
        int tyLeSuCoDaXuLy = (tongSuCo > 0) ? (int) ((suCoDaXuLy * 100) / tongSuCo) : 0;
        model.addAttribute("tyLeSuCoDaXuLy", tyLeSuCoDaXuLy);

        // (Giả lập các tỷ lệ khác nếu chưa có logic tính toán)
        model.addAttribute("tyLeThuPhi", 78);
        model.addAttribute("tyLeCanHoDaBan", 92);

        // 4. Danh sách sự cố sắp xếp theo mức độ ưu tiên (cao → thấp)
        List<BaoCaoSuCo> suCoCanXuLy = suCoDAO.findAllOrderedByPriority();
        model.addAttribute("suCoCanXuLy", suCoCanXuLy);

        // ========================================================
        // 5. XỬ LÝ DỮ LIỆU BIỂU ĐỒ (LOGIC MỚI)
        // ========================================================
        Map<String, Object> chartData = taiSanChungCuService.getChartData();

        // A. Dữ liệu Tầng
        Map<Integer, Long> floorMap = (Map<Integer, Long>) chartData.get("floorStats");
        List<String> floorLabels = new ArrayList<>();
        List<Long> floorData = new ArrayList<>();

        for (Map.Entry<Integer, Long> entry : floorMap.entrySet()) {
            floorLabels.add("Tầng " + entry.getKey());
            floorData.add(entry.getValue());
        }

        // B. Dữ liệu Tòa
        Map<String, Long> buildingMap = (Map<String, Long>) chartData.get("buildingStats");
        List<String> buildingLabels = new ArrayList<>();
        List<Long> buildingData = new ArrayList<>();

        for (Map.Entry<String, Long> entry : buildingMap.entrySet()) {
            buildingLabels.add("Tòa " + entry.getKey());
            buildingData.add(entry.getValue());
        }

        model.addAttribute("floorLabels", floorLabels);
        model.addAttribute("floorData", floorData);
        model.addAttribute("buildingLabels", buildingLabels);
        model.addAttribute("buildingData", buildingData);
        // ========================================================

        return "dashboard-admin";
    }

    @GetMapping("/profile")
    public String showAdminProfile(Model model, Authentication auth) {

        // 1. Lấy thông tin người dùng đang đăng nhập (Ban Quản Trị)
        DoiTuong user = getCurrentUser(auth);

        if (user == null) {
            // Trường hợp lỗi (ví dụ: Session hết hạn hoặc không tìm thấy user)
            return "redirect:/login?error=auth";
        }

        // 2. Thêm đối tượng user vào Model để hiển thị trong Thymeleaf
        model.addAttribute("user", user);

        // 3. Trả về tên file Thymeleaf (profile.html trong thư mục template/admin/)
        return "profile-admin"; // Hoặc "admin/profile" tùy theo cấu trúc thư mục của bạn
    }
    // Trong AdminController.java

    // @Autowired private CuDanService cuDanService; // Đã có

    @GetMapping("/resident-list")
    public String showResidentList(Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ResidentStatus trangThaiDanCu,
            @RequestParam(required = false) AccountStatus accountStatus,
            Authentication auth) {

        // 1. Lấy thông tin người dùng đang đăng nhập (header)
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user);

        // 2. Lấy danh sách đối tượng (có áp dụng tìm kiếm/lọc)
        // Nếu có tham số tìm kiếm, gọi hàm lọc; nếu không, lấy tất cả.
        List<DoiTuong> danhSachDoiTuong;
        if (keyword != null || trangThaiDanCu != null || accountStatus != null) {
            danhSachDoiTuong = cuDanService.timKiemvaLoc(keyword, trangThaiDanCu, accountStatus);
        } else {
            danhSachDoiTuong = cuDanService.layDanhSachCuDan();
        }

        // 3. Thêm danh sách vào Model
        model.addAttribute("residents", danhSachDoiTuong);

        // 4. (Tùy chọn) Thêm thông tin phân trang
        model.addAttribute("totalResidents", danhSachDoiTuong.size()); // Giả định không phân trang

        model.addAttribute("accountStatuses", AccountStatus.values());
        return "residents"; // Giả định tên file Thymeleaf là residents-list.html
    }

    /**
     * HIỂN THỊ FORM (GET)
     * Đường dẫn: /admin/resident-add
     */
    @GetMapping("/resident-add")
    public String showAddResidentForm(Model model, Authentication auth) {
        // Lấy user cho header
        model.addAttribute("user", getCurrentUser(auth));

        // Cần truyền một đối tượng rỗng để binding form (th:object)
        model.addAttribute("newResident", new DoiTuong());

        // Truyền các giá trị Enum để hiển thị trong dropdown (nếu cần)
        model.addAttribute("genders", Gender.values());

        return "resident-add"; // Tên file Thymeleaf: resident-add.html
    }

    @PostMapping("/resident-add")
    public String addNewResident(@ModelAttribute("newResident") DoiTuong newResident,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. Service phải tự động thiết lập các trường mặc định:
            // matKhau (random), vaiTro (khong_dung_he_thong), laCuDan (true), v.v.
            // (CuDanService.themCuDan của bạn đã xử lý phần lớn, nhưng cần đảm bảo matKhau
            // được tạo)

            // 2. Tùy chọn: Đặt Email và SĐT thành null nếu không được nhập, nhưng hiện tại
            // form yêu cầu BẮT BUỘC.
            // Nếu muốn không bắt buộc, ta phải chỉnh form. Hiện tại, hãy để Service xử lý.

            DoiTuong savedResident = cuDanService.themCuDan(newResident);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã thêm thông tin cư dân " + savedResident.getHoVaTen() + " thành công.");

            return "redirect:/admin/resident-list";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/resident-add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi lưu: " + e.getMessage());
            return "redirect:/admin/resident-add";
        }
    }

    /**
     * XỬ LÝ XÓA MỀM (Chuyển đi/Mất)
     * Đường dẫn: /admin/resident-delete
     * Phương thức: POST (hoặc GET đơn giản)
     */
    @GetMapping("/resident-delete") // Dùng GET cho đơn giản với link href/redirect
    public String deleteResident(@RequestParam String cccd,
            @RequestParam ResidentStatus lyDo,
            RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra lý do hợp lệ (chỉ chấp nhận roi_di hoặc da_chet)
            if (lyDo != ResidentStatus.roi_di && lyDo != ResidentStatus.da_chet) {
                throw new IllegalArgumentException("Lý do xóa không hợp lệ.");
            }

            cuDanService.xoaCuDan(cccd, lyDo);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật trạng thái cư dân " + cccd + " thành công (Lý do: " + lyDo.getDbValue() + ").");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa cư dân: " + e.getMessage());
        }
        return "redirect:/admin/resident-list";
    }
    // Trong AdminController.java

    // 1. GET: Hiển thị Form với Dữ liệu Cũ
    @GetMapping("/resident-edit")
    public String showEditResidentForm(@RequestParam String cccd, Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user);

        // Lấy thông tin cư dân cần chỉnh sửa
        Optional<DoiTuong> residentOpt = cuDanService.timCuDanTheoCCCD(cccd);

        if (residentOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy cư dân với CCCD: " + cccd);
            return "redirect:/admin/resident-list";
        }

        DoiTuong residentToEdit = residentOpt.get();

        // Đặt đối tượng vào model để binding form (th:object)
        model.addAttribute("resident", residentToEdit);

        // Truyền Enum (cho dropdown)
        model.addAttribute("genders", Gender.values());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("accountStatuses", AccountStatus.values());
        model.addAttribute("residentStatuses", ResidentStatus.values());

        return "resident-edit";
    }

    // 2. POST: Xử lý Cập nhật
    @PostMapping("/resident-edit")
    public String updateResident(@ModelAttribute("resident") DoiTuong residentCapNhat,
            @RequestParam(value = "matKhauMoi", required = false) String matKhauMoi,
            RedirectAttributes redirectAttributes) {
        try {
            // Lấy CCCD từ đối tượng form đã bind (CCCD là trường cố định)
            String cccd = residentCapNhat.getCccd();

            // 1. Cập nhật thông tin cơ bản (họ tên, ngày sinh, giới tính, SĐT, email, etc.)
            cuDanService.capNhatCuDan(cccd, residentCapNhat);

            // 2. Xử lý đổi mật khẩu (nếu có)
            if (matKhauMoi != null && !matKhauMoi.trim().isEmpty()) {
                // LƯU Ý: Cần có phương thức đổi mật khẩu cho Admin trong NguoiDungService
                // (Admin bỏ qua mật khẩu cũ)
                nguoiDungService.resetMatKhau(cccd, matKhauMoi); // Giả sử hàm này được overloaded hoặc sử dụng cho
                                                                 // Admin
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật thông tin cư dân " + residentCapNhat.getHoVaTen() + " thành công!");

            return "redirect:/admin/resident-list";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
            return "redirect:/admin/resident-edit?cccd=" + residentCapNhat.getCccd();
        }
    }

    /**
     * HIỂN THỊ CHI TIẾT CƯ DÂN (GET)
     * Đường dẫn: /admin/resident-details?cccd={cccd}
     */
    @GetMapping("/resident-details")
    @Transactional // Đảm bảo Lazy Loading của ThanhVienHo hoạt động
    public String showResidentDetails(@RequestParam("cccd") String cccd, Model model, Authentication auth) {
        DoiTuong userAdmin = getCurrentUser(auth);
        if (userAdmin == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", userAdmin);

        // 1. Lấy thông tin cư dân chính
        DoiTuong resident = cuDanService.timCuDanTheoCCCD(cccd)
                .orElse(null);

        if (resident == null) {
            model.addAttribute("errorMessage", "Không tìm thấy cư dân với CCCD: " + cccd);
            return "redirect:/admin/resident-list";
        }
        model.addAttribute("resident", resident);

        // 2. Lấy mối quan hệ hộ gia đình hiện tại (ThanhVienHo)
        // Cần inject ThanhVienHoService
        // ⚠️ GIẢ ĐỊNH: Bạn sẽ inject ThanhVienHoService vào Controller này.
        // @Autowired private ThanhVienHoService thanhVienHoService;

        // Dùng HoGiaDinhService để lấy thông tin liên quan đến hộ (nếu có)
        Optional<ThanhVienHo> tvhOpt = hoGiaDinhService.getThanhVienHoCurrentByCccd(cccd);

        if (tvhOpt.isPresent()) {
            ThanhVienHo tvh = tvhOpt.get();
            model.addAttribute("currentHousehold", tvh.getHoGiaDinh()); // Hộ gia đình
            model.addAttribute("memberRelation", tvh); // Chi tiết quan hệ (Chủ hộ, Quan hệ, Ngày bắt đầu)

            // Lấy thông tin căn hộ (nếu có)
            Optional<TaiSanChungCu> apartmentOpt = hoGiaDinhService
                    .getApartmentByHousehold(tvh.getHoGiaDinh().getMaHo());
            model.addAttribute("apartment", apartmentOpt.orElse(null));
        } else {
            model.addAttribute("currentHousehold", null);
            model.addAttribute("memberRelation", null);
            model.addAttribute("apartment", null);
        }

        return "resident-details"; // Trỏ đến file Thymeleaf mới
    }

    // =======================================================
    // PROFILE EDIT / CHANGE PASSWORD
    // =======================================================

    // Hiển thị form Đổi Mật Khẩu
    @GetMapping("/change-password")
    public String showAdminChangePasswordForm(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user);
        return "change-password-admin";
    }

    // Xử lý POST request Đổi Mật Khẩu
    @PostMapping("/change-password")
    public String handleAdminChangePassword(@RequestParam("matKhauCu") String matKhauCu,
            @RequestParam("matKhauMoi") String matKhauMoi,
            @RequestParam("xacNhanMatKhau") String xacNhanMatKhau,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/admin/profile";
        }

        if (!matKhauMoi.equals(xacNhanMatKhau)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/admin/change-password";
        }

        try {
            nguoiDungService.doiMatKhau(currentUser.getCccd(), matKhauCu, matKhauMoi);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/logout";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/change-password";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/change-password";
        }
    }

    // Hiển thị form Cập Nhật Thông Tin Cá Nhân
    @GetMapping("/profile/edit")
    public String showAdminEditProfileForm(Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=auth";
        }
        model.addAttribute("user", user);
        model.addAttribute("genders", Gender.values()); // Để hiển thị Enum giới tính
        return "edit-profile-admin";
    }

    // Xử lý POST request Cập Nhật Thông Tin Cá Nhân
    @PostMapping("/profile/edit")
    public String handleAdminEditProfile(@ModelAttribute("user") DoiTuong doiTuongCapNhat,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        DoiTuong currentUser = getCurrentUser(auth);
        if (currentUser == null || !currentUser.getCccd().equals(doiTuongCapNhat.getCccd())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực người dùng.");
            return "redirect:/admin/profile";
        }

        try {
            // Sử dụng hàm đã thêm trong NguoiDungService
            nguoiDungService.capNhatThongTinNguoiDung(doiTuongCapNhat);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/admin/profile";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/profile/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi cập nhật: " + e.getMessage());
            return "redirect:/admin/profile/edit";
        }
    }
    // =======================================================
    // QUẢN LÝ HỘ GIA ĐÌNH
    // =======================================================

    /**
     * Hiển thị danh sách hộ gia đình
     */
    @GetMapping("/household-list")
    public String showHouseholdList(Model model,
            @RequestParam(required = false) String keyword,
            Authentication auth) {

        model.addAttribute("user", getCurrentUser(auth));

        List<HoGiaDinh> households = hoGiaDinhService.getAllHouseholds(keyword);

        model.addAttribute("households", households);
        model.addAttribute("keyword", keyword); // Để giữ lại giá trị tìm kiếm trên form

        return "household-list"; // Tên file Thymeleaf: household-list.html
    }

    /**
     * Hiển thị form thêm hộ gia đình mới (GET)
     */
    @GetMapping("/household-add")
    public String showAddHouseholdForm(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("newHousehold", new HoGiaDinh());
        model.addAttribute("householdStatuses", HouseholdStatus.values());
        List<String> floors = taiSanChungCuService.getAllApartmentFloors();
        model.addAttribute("apartmentFloors", floors);

        // Thêm DTO hoặc RequestParam để nhập CCCD Chủ hộ
        // Giả định dùng RequestParam: chuHoCccd và quanHe

        return "household-add"; // Tên file Thymeleaf: household-add.html
    }

    /**
     * Xử lý thêm hộ gia đình mới (POST)
     */
    @PostMapping("/household-add")
    public String handleAddHousehold(@ModelAttribute("newHousehold") HoGiaDinh hoGiaDinh,
            @RequestParam("chuHoCccd") String chuHoCccd,
            @RequestParam(value = "quanHeVoiChuHo", defaultValue = "Chủ hộ") String quanHe,
            @RequestParam(value = "maCanHoLienKet", required = false) Integer maTaiSan, // <-- THAM SỐ MỚI
            RedirectAttributes redirectAttributes) {
        try {
            // Cập nhật hàm service: truyền thêm maTaiSan
            hoGiaDinhService.themHoGiaDinh(hoGiaDinh, chuHoCccd, quanHe, maTaiSan);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm Hộ gia đình " + hoGiaDinh.getTenHo() + " và gán Căn hộ thành công!");
            return "redirect:/admin/household-list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/household-add";
        }
    }

    /**
     * XỬ LÝ REST API: Lấy danh sách Căn hộ trống theo Tầng
     * URL: GET /admin/apartments/empty-by-floor?viTri=T1
     * TRẢ VỀ: List<Map<String, Object>> {maTaiSan, tenHienThi}
     */
    @GetMapping("/apartments/empty-by-floor")
    public ResponseEntity<List<Map<String, Object>>> getEmptyApartmentsByFloor(@RequestParam("viTri") String viTri) {
        if (viTri == null || viTri.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<TaiSanChungCu> apartments = taiSanChungCuService.getEmptyApartmentsByFloor(viTri);

        // Chuyển đổi List<TaiSanChungCu> sang List<Map<String, Object>> chỉ chứa
        // MaTaiSan và TenTaiSan
        List<Map<String, Object>> simpleApartments = apartments.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("maTaiSan", a.getMaTaiSan());
                    // Hiển thị Tên Căn Hộ và Diện Tích để dễ nhận biết hơn
                    map.put("tenHienThi", a.getTenTaiSan() + " (DT: " + a.getDienTich() + "m2)");
                    return map;
                })
                .toList();

        // Trả về danh sách Căn hộ trống dưới dạng JSON
        return ResponseEntity.ok(simpleApartments);
    }

    /**
     * Xem chi tiết hộ gia đình, danh sách thành viên và căn hộ đang ở (GET)
     */
    @GetMapping("/household-detail")
    @Transactional // Cần thêm @Transactional để đảm bảo lazy loading List<ThanhVienHo> hoạt động
    public String showHouseholdDetail(@RequestParam("maHo") String maHo, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        HoGiaDinh hgd = hoGiaDinhService.getHouseholdById(maHo)
                .orElse(null);

        if (hgd == null) {
            model.addAttribute("errorMessage", "Không tìm thấy Hộ gia đình.");
            return "redirect:/admin/household-list";
        }

        // 1. Tải danh sách thành viên đang hoạt động
        // Dùng Lazy loading List<ThanhVienHo> trên Entity HGD (Cần @Transactional)
        List<ThanhVienHo> thanhVienList = hgd.getThanhVienHoList().stream()
                .filter(tvh -> tvh.getNgayKetThuc() == null)
                .toList();

        // 2. Lấy thông tin căn hộ chính
        Optional<TaiSanChungCu> apartmentOpt = hoGiaDinhService.getApartmentByHousehold(maHo);

        // 3. Thêm dữ liệu vào Model
        model.addAttribute("household", hgd);
        model.addAttribute("members", thanhVienList);
        model.addAttribute("apartment", apartmentOpt.orElse(null)); // Thêm căn hộ (hoặc null)
        model.addAttribute("terminationReasons", TerminationReason.values());

        return "household-details";
    }

    /**
     * HIỂN THỊ FORM TÁCH HỘ (GET)
     * Đường dẫn: /admin/household-split
     * Yêu cầu: maHo (Hộ cũ)
     */
    @GetMapping("/household-split")
    @Transactional
    public String showSplitHouseholdForm(@RequestParam("maHo") String maHoCu, Model model, Authentication auth) {
        DoiTuong user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login?error=auth";
        }

        HoGiaDinh hgdCu = hoGiaDinhService.getHouseholdById(maHoCu)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình cũ."));

        // Lấy danh sách thành viên hiện tại của hộ cũ
        List<DoiTuong> members = hgdCu.getThanhVienHoList().stream()
                .filter(tvh -> tvh.getNgayKetThuc() == null)
                .map(ThanhVienHo::getDoiTuong)
                .toList();
        List<String> floors = taiSanChungCuService.getAllApartmentFloors();
        model.addAttribute("user", user);
        model.addAttribute("household", hgdCu);
        model.addAttribute("members", members); // Danh sách thành viên để chọn
        model.addAttribute("newHousehold", new HoGiaDinh()); // DTO cho thông tin Hộ mới
        model.addAttribute("apartmentFloors", floors);
        return "household-split"; // Tên file Thymeleaf mới
    }

    /**
     * XỬ LÝ TÁCH HỘ (POST)
     * Đường dẫn: /admin/household-split
     */
    @PostMapping("/household-split")
    public String handleSplitHousehold(@RequestParam("maHoCu") String maHoCu,
            @RequestParam("tenHoMoi") String tenHoMoi,
            @RequestParam("chuHoMoiCccd") String chuHoMoiCccd,
            @RequestParam("cccdDuocTach") List<String> cccdThanhVienDuocTach, // List CCCD được chọn
            @RequestParam(value = "maCanHoLienKet", required = false) Integer maTaiSan, // <-- THAM SỐ MỚI
            RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra số lượng thành viên tối thiểu
            if (cccdThanhVienDuocTach.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một thành viên để tách hộ.");
            }

            // Gọi logic Service Tách Hộ (truyền thêm maTaiSan)
            HoGiaDinh hoGiaDinhMoi = hoGiaDinhService.tachHo(
                    maHoCu,
                    cccdThanhVienDuocTach,
                    chuHoMoiCccd,
                    tenHoMoi,
                    maTaiSan // <-- TRUYỀN THAM SỐ MỚI
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Tách hộ thành công! Đã tạo Hộ gia đình mới: " + hoGiaDinhMoi.getTenHo());

            return "redirect:/admin/household-detail?maHo=" + hoGiaDinhMoi.getMaHo();

        } catch (IllegalStateException e) {
            // Lỗi khi Chủ hộ cũ bị tách nhưng hộ cũ vẫn còn thành viên
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/household-detail?maHo=" + maHoCu;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tách hộ: " + e.getMessage());
            return "redirect:/admin/household-split?maHo=" + maHoCu; // Quay lại form Tách Hộ
        }
    }

    /**
     * HIỂN THỊ FORM THÊM THÀNH VIÊN (GET)
     * Đường dẫn: /admin/household-member-add
     * Yêu cầu: maHo (Mã hộ cần thêm)
     */
    @GetMapping("/household-member-add")
    public String showAddMemberForm(@RequestParam("maHo") String maHo, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        // Kiểm tra hộ gia đình tồn tại
        HoGiaDinh hgd = hoGiaDinhService.getHouseholdById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));

        model.addAttribute("household", hgd);
        // Truyền DTO/Form object nếu cần, nhưng ở đây ta dùng @RequestParam đơn giản
        // hơn

        return "household-member-add"; // Trỏ đến file Thymeleaf mới
    }

    /**
     * XỬ LÝ THÊM THÀNH VIÊN (POST)
     * Đường dẫn: /admin/household-member-add
     */
    @PostMapping("/household-member-add")
    public String handleAddMember(@RequestParam("maHo") String maHo,
            @RequestParam("cccdThanhVien") String cccdThanhVien,
            @RequestParam("quanHe") String quanHe,
            RedirectAttributes redirectAttributes) {
        try {
            // Mặc định laChuHo = false khi thêm mới
            // LyDoKetThuc = chuyen_ho (sẽ được ghi đè nếu thành viên này đang ở hộ khác)
            hoGiaDinhService.themThanhVien(maHo, cccdThanhVien, false, quanHe, null);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã thêm thành viên CCCD " + cccdThanhVien + " vào Hộ " + maHo + " thành công.");

            return "redirect:/admin/household-detail?maHo=" + maHo;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/household-member-add?maHo=" + maHo;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi thêm thành viên: " + e.getMessage());
            return "redirect:/admin/household-member-add?maHo=" + maHo;
        }
    }

    /**
     * HIỂN THỊ FORM CHUYỂN CHỦ HỘ (GET)
     * Đường dẫn: /admin/household-change-owner
     * Yêu cầu: maHo (Mã hộ cần chuyển Chủ hộ)
     */
    @GetMapping("/household-change-owner")
    @Transactional // Cần load danh sách thành viên
    public String showChangeChuHoForm(@RequestParam("maHo") String maHo, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        HoGiaDinh hgd = hoGiaDinhService.getHouseholdById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));

        // Lấy danh sách thành viên hiện tại (trừ Chủ hộ hiện tại)
        List<DoiTuong> members = hgd.getThanhVienHoList().stream()
                .filter(tvh -> tvh.getNgayKetThuc() == null && !tvh.getLaChuHo())
                .map(ThanhVienHo::getDoiTuong)
                .toList();

        model.addAttribute("household", hgd);
        model.addAttribute("members", members);

        return "household-change-owner"; // Trỏ đến file Thymeleaf mới
    }

    /**
     * XỬ LÝ CHUYỂN CHỦ HỘ (POST)
     * Đường dẫn: /admin/household-change-ch
     */
    @PostMapping("/household-change-owner")
    public String handleChangeChuHo(@RequestParam("maHo") String maHo,
            @RequestParam("cccdChuHoMoi") String cccdChuHoMoi,
            RedirectAttributes redirectAttributes) {
        try {
            hoGiaDinhService.capNhatChuHo(maHo, cccdChuHoMoi);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển Chủ hộ cho Hộ " + maHo + " thành công (CCCD mới: " + cccdChuHoMoi + ").");

            return "redirect:/admin/household-detail?maHo=" + maHo;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/household-change-owner?maHo=" + maHo;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi chuyển Chủ hộ: " + e.getMessage());
            return "redirect:/admin/household-change-owner?maHo=" + maHo;
        }
    }
    // =======================================================
    // QUẢN LÝ CĂN HỘ (APARTMENTS)
    // =======================================================

    /**
     * Hiển thị danh sách Căn Hộ (GET) có hỗ trợ phân loại.
     */
    @GetMapping("/apartment-list")
    public String showApartmentList(
            Model model,
            Authentication auth,
            @RequestParam(required = false) String keyword, // Giả định có tìm kiếm theo tên
            @RequestParam(required = false) BlueMoon.bluemoon.utils.AssetStatus status,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) BigDecimal minValue,
            @RequestParam(required = false) BigDecimal maxValue) {
        model.addAttribute("user", getCurrentUser(auth));

        // Lấy danh sách căn hộ dựa trên các bộ lọc
        List<TaiSanChungCu> apartments;

        // Chú ý: Hiện tại DAO chỉ hỗ trợ lọc đơn (Area, Value, Status).
        // Ta ưu tiên lọc phức tạp hơn trước.
        if (minArea != null && maxArea != null && minArea.compareTo(maxArea) <= 0) {
            apartments = taiSanChungCuService.getApartmentsByAreaRange(minArea, maxArea);
        } else if (minValue != null && maxValue != null && minValue.compareTo(maxValue) <= 0) {
            apartments = taiSanChungCuService.getApartmentsByValueRange(minValue, maxValue);
        } else if (status != null) {
            apartments = taiSanChungCuService.getApartmentsByStatus(status);
        } else {
            // Nếu không có bộ lọc nào, trả về tất cả
            apartments = taiSanChungCuService.getAllApartments();
        }

        // Nếu có keyword, cần thêm logic tìm kiếm theo tên/mã thủ công tại đây nếu
        // Service không hỗ trợ
        // (Bỏ qua logic keyword để tập trung vào phân loại chính)

        model.addAttribute("apartments", apartments);
        // Lưu trữ các giá trị lọc để giữ lại trên form
        model.addAttribute("currentStatus", status);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        model.addAttribute("minValue", minValue);
        model.addAttribute("maxValue", maxValue);
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());

        return "apartment-list-admin";
    }

    /**
     * Hiển thị form thêm Căn Hộ mới (GET)
     */
    @GetMapping("/apartment-add")
    public String showAddApartmentForm(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("newApartment", new TaiSanChungCu());
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());
        model.addAttribute("households", hoGiaDinhService.getAllHouseholds()); // Lấy danh sách Hộ gia đình để liên kết

        return "apartment-add";
    }

    /**
     * Xử lý thêm Căn Hộ mới (POST)
     */
    @PostMapping("/apartment-add")
    public String handleAddApartment(@ModelAttribute("newApartment") TaiSanChungCu apartment,
            @RequestParam(value = "maHoLienKet", required = false) String maHoLienKet,
            RedirectAttributes redirectAttributes) {
        try {
            taiSanChungCuService.themCanHo(apartment, maHoLienKet);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm Căn hộ " + apartment.getTenTaiSan() + " thành công!");
            return "redirect:/admin/apartment-list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/apartment-add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/apartment-add";
        }
    }

    /**
     * Hiển thị form chỉnh sửa Căn Hộ (GET)
     */
    @GetMapping("/apartment-edit")
    public String showEditApartmentForm(@RequestParam("maTaiSan") Integer maTaiSan, Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        TaiSanChungCu apartmentToEdit = taiSanChungCuService.getApartmentById(maTaiSan)
                .orElse(null);

        if (apartmentToEdit == null) {
            model.addAttribute("errorMessage", "Không tìm thấy Căn hộ.");
            return "redirect:/admin/apartment-list";
        }

        model.addAttribute("apartment", apartmentToEdit);
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());
        model.addAttribute("households", hoGiaDinhService.getAllHouseholds());

        return "apartment-edit";
    }

    /**
     * Xử lý cập nhật Căn Hộ (POST)
     */
    @PostMapping("/apartment-edit")
    public String handleEditApartment(@ModelAttribute("apartment") TaiSanChungCu apartment,
            @RequestParam("maTaiSan") Integer maTaiSan,
            @RequestParam(value = "maHoLienKet", required = false) String maHoLienKet,
            RedirectAttributes redirectAttributes) {
        try {
            taiSanChungCuService.capNhatCanHo(maTaiSan, apartment, maHoLienKet);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật Căn hộ " + apartment.getTenTaiSan() + " thành công!");
            return "redirect:/admin/apartment-list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/apartment-edit?maTaiSan=" + maTaiSan;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/apartment-edit?maTaiSan=" + maTaiSan;
        }
    }

    /**
     * Xử lý xóa Căn Hộ (GET cho đơn giản)
     */
    @GetMapping("/apartment-delete")
    public String handleDeleteApartment(@RequestParam("maTaiSan") Integer maTaiSan,
            RedirectAttributes redirectAttributes) {
        try {
            taiSanChungCuService.xoaCanHo(maTaiSan);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa căn hộ Mã " + maTaiSan + " thành công.");
            return "redirect:/admin/apartment-list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/apartment-list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi xóa: " + e.getMessage());
            return "redirect:/admin/apartment-list";
        }
    }

    /**
     * Xem chi tiết Căn Hộ (GET)
     */
    @GetMapping("/apartment-details")
    public String showAdminApartmentDetails(@RequestParam("maTaiSan") Integer maTaiSan, Model model,
            Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        TaiSanChungCu apartment = taiSanChungCuService.getApartmentById(maTaiSan)
                .orElse(null);

        if (apartment == null) {
            model.addAttribute("errorMessage", "Không tìm thấy Căn hộ với Mã Tài Sản: " + maTaiSan);
            return "redirect:/admin/apartment-list";
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

        return "apartment-details-admin";
    }
    // =======================================================
    // QUẢN LÝ TẤT CẢ TÀI SẢN (GENERAL ASSETS) - Bao gồm cả Căn Hộ
    // =======================================================

    /**
     * Hiển thị danh sách TẤT CẢ Tài Sản (GET) có hỗ trợ lọc theo loại và tìm kiếm.
     * URL: /admin/general-asset-list
     */
    @GetMapping("/general-asset-list")
    public String showGeneralAssetList(
            Model model,
            Authentication auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BlueMoon.bluemoon.utils.AssetType loaiTaiSan // Lọc theo loại
    ) {
        model.addAttribute("user", getCurrentUser(auth));

        // Lấy danh sách tài sản dựa trên các bộ lọc
        List<TaiSanChungCu> assets;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Nếu có keyword, tìm kiếm theo tên, bỏ qua loaiTaiSan (hoặc bạn có thể thêm
            // logic lọc kép)
            assets = taiSanChungCuService.findAssetsByFilters(keyword, null);
        } else {
            // Nếu không có keyword, lọc theo loại tài sản (loaiTaiSan = null sẽ lấy tất cả)
            assets = taiSanChungCuService.getAllAssets(loaiTaiSan);
        }

        model.addAttribute("assets", assets);
        // Lưu trữ các giá trị lọc để giữ lại trên form
        model.addAttribute("currentAssetType", loaiTaiSan);
        model.addAttribute("keyword", keyword);
        model.addAttribute("assetTypes", BlueMoon.bluemoon.utils.AssetType.values()); // Cần hiển thị tất cả loại
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());

        return "general-asset-list"; // Tên file Thymeleaf mới
    }

    /**
     * Hiển thị form thêm Tài Sản Chung mới (GET)
     * URL: /admin/general-asset-add
     */
    @GetMapping("/general-asset-add")
    public String showAddGeneralAssetForm(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("newAsset", new TaiSanChungCu());
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());
        model.addAttribute("assetTypes", BlueMoon.bluemoon.utils.AssetType.values());
        // Lấy danh sách hộ gia đình để liên kết
        model.addAttribute("households", hoGiaDinhService.getAllHouseholds());

        return "general-asset-add";
    }

    /**
     * Xử lý thêm Tài Sản Chung mới (POST)
     * URL: /admin/general-asset-add
     */
    @PostMapping("/general-asset-add")
    public String handleAddGeneralAsset(@ModelAttribute("newAsset") TaiSanChungCu asset,
            @RequestParam(value = "maHoLienKet", required = false) String maHoLienKet,
            RedirectAttributes redirectAttributes) {
        try {
            TaiSanChungCu savedAsset = taiSanChungCuService.themTaiSanChung(asset, maHoLienKet);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm Tài sản " + savedAsset.getTenTaiSan() + " (Mã: " + savedAsset.getMaTaiSan()
                            + ") thành công!");
            return "redirect:/admin/general-asset-list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/general-asset-add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/general-asset-add";
        }
    }

    /**
     * Hiển thị form chỉnh sửa Tài Sản Chung (GET)
     * URL: /admin/general-asset-edit?maTaiSan={id}
     */
    @GetMapping("/general-asset-edit")
    public String showEditGeneralAssetForm(@RequestParam("maTaiSan") Integer maTaiSan, Model model,
            Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        TaiSanChungCu assetToEdit = taiSanChungCuService.getAssetById(maTaiSan)
                .orElse(null);

        if (assetToEdit == null) {
            model.addAttribute("errorMessage", "Không tìm thấy Tài Sản.");
            return "redirect:/admin/general-asset-list";
        }

        model.addAttribute("asset", assetToEdit);
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());
        model.addAttribute("assetTypes", BlueMoon.bluemoon.utils.AssetType.values());
        model.addAttribute("households", hoGiaDinhService.getAllHouseholds());

        return "general-asset-edit";
    }

    /**
     * Xử lý cập nhật Tài Sản Chung (POST)
     * URL: /admin/general-asset-edit
     */
    @PostMapping("/general-asset-edit")
    public String handleEditGeneralAsset(@ModelAttribute("asset") TaiSanChungCu asset,
            @RequestParam("maTaiSan") Integer maTaiSan,
            @RequestParam(value = "maHoLienKet", required = false) String maHoLienKet,
            RedirectAttributes redirectAttributes) {
        try {
            taiSanChungCuService.capNhatTaiSanChung(maTaiSan, asset, maHoLienKet);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật Tài sản Mã " + maTaiSan + " thành công!");
            return "redirect:/admin/general-asset-list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/general-asset-edit?maTaiSan=" + maTaiSan;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/general-asset-edit?maTaiSan=" + maTaiSan;
        }
    }

    /**
     * Xử lý xóa Tài Sản Chung (GET cho đơn giản)
     * URL: /admin/general-asset-delete?maTaiSan={id}
     */
    @GetMapping("/general-asset-delete")
    public String handleDeleteGeneralAsset(@RequestParam("maTaiSan") Integer maTaiSan,
            RedirectAttributes redirectAttributes) {
        try {
            taiSanChungCuService.xoaTaiSanChung(maTaiSan);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài sản Mã " + maTaiSan + " thành công.");
            return "redirect:/admin/general-asset-list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/general-asset-list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi xóa: " + e.getMessage());
            return "redirect:/admin/general-asset-list";
        }
    }

    /**
     * Xem chi tiết Tài Sản Chung (GET)
     * URL: /admin/general-asset-details?maTaiSan={id}
     */
    @GetMapping("/general-asset-details")
    public String showAdminGeneralAssetDetails(@RequestParam("maTaiSan") Integer maTaiSan, Model model,
            Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        TaiSanChungCu asset = taiSanChungCuService.getAssetById(maTaiSan)
                .orElse(null);

        if (asset == null) {
            model.addAttribute("errorMessage", "Không tìm thấy Tài Sản với Mã Tài Sản: " + maTaiSan);
            return "redirect:/admin/general-asset-list";
        }

        model.addAttribute("asset", asset);

        // Tùy chọn: Thêm danh sách thành viên hộ liên kết (nếu có)
        if (asset.getHoGiaDinh() != null) {
            List<BlueMoon.bluemoon.entities.ThanhVienHo> members = asset.getHoGiaDinh().getThanhVienHoList().stream()
                    .filter(tvh -> tvh.getNgayKetThuc() == null)
                    .toList();
            model.addAttribute("members", members);
        } else {
            model.addAttribute("members", List.of());
        }

        return "general-asset-details";
    }
    // =======================================================
    // QUẢN LÝ HÓA ĐƠN (CRUD)
    // =======================================================

    // =======================================================
    // [CẬP NHẬT] QUẢN LÝ HÓA ĐƠN VỚI CHỨC NĂNG CHỌN THÀNH VIÊN
    // =======================================================

    /**
     * Admin list: URL: /admin/fees
     */
    @GetMapping("/fees")
    public String showAdminFees(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        List<HoaDon> hoaDonList = hoaDonService.getAllHoaDon();
        model.addAttribute("hoaDonList", hoaDonList);
        return "fees-admin";
    }

    /**
     * Admin form: URL: /admin/fee-form
     */
    @GetMapping("/fee-form")
    public String showAdminFeeForm(@RequestParam(value = "id", required = false) Integer maHoaDon,
            Model model,
            Authentication auth) {

        model.addAttribute("user", getCurrentUser(auth));
        HoaDon hoaDon = (maHoaDon != null) ? hoaDonService.getHoaDonById(maHoaDon).orElse(new HoaDon()) : new HoaDon();

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("pageTitle", (maHoaDon != null) ? "Chỉnh Sửa Hóa Đơn #" + maHoaDon : "Tạo Hóa Đơn Mới");
        model.addAttribute("invoiceTypes", InvoiceType.values());
        List<HoGiaDinh> allHo = hoGiaDinhService.getAllHouseholds();
        model.addAttribute("allHo", allHo);

        return "invoice-add-edit-admin";
    }

    /**
     * API JSON: Trả về danh sách thành viên của một hộ cụ thể.
     * Giao diện sẽ gọi cái này khi người dùng chọn Hộ.
     */
    @GetMapping("/api/households/{maHo}/members")
    public ResponseEntity<List<Map<String, String>>> getHouseholdMembers(@PathVariable String maHo) {
        // 1. Tìm hộ gia đình
        HoGiaDinh hgd = hoGiaDinhService.getHouseholdById(maHo).orElse(null);

        if (hgd == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Lấy danh sách thành viên ĐANG Ở (ngayKetThuc == null)
        List<Map<String, String>> members = hgd.getThanhVienHoList().stream()
                .filter(tvh -> tvh.getNgayKetThuc() == null)
                .map(tvh -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("cccd", tvh.getDoiTuong().getCccd());

                    // Đánh dấu ai là Chủ hộ để dễ nhìn
                    String role = tvh.getLaChuHo() ? " (Chủ hộ)" : "";
                    map.put("hoVaTen", tvh.getDoiTuong().getHoVaTen() + role);

                    return map;
                })
                .toList();

        return ResponseEntity.ok(members);
    }

    /**
     * Admin delete: URL: /admin/fee-delete (Tái sử dụng logic Service)
     */
    @PostMapping("/fee-delete")
    public String handleAdminDeleteFee(@RequestParam("id") Integer maHoaDon,
            RedirectAttributes redirectAttributes) {
        try {
            hoaDonService.deleteHoaDon(maHoaDon);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa Hóa đơn #" + maHoaDon + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa: " + e.getMessage());
        }
        return "redirect:/admin/fees";
    }
    // =======================================================
    // QUẢN LÝ DỊCH VỤ (SERVICES)
    // =======================================================

    /**
     * HIỂN THỊ DANH SÁCH DỊCH VỤ (GET)
     * URL: /admin/service-list
     */
    @GetMapping("/service-list")
    public String showServiceList(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        // 1. Lấy danh sách tất cả dịch vụ
        List<DichVu> dichVuList = dichVuService.getAllDichVu();

        model.addAttribute("services", dichVuList);
        model.addAttribute("serviceTypes", BlueMoon.bluemoon.utils.ServiceType.values());
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());

        return "service-list-admin"; // Trỏ đến file Thymeleaf mới
    }

    // -------------------------------------------------------

    /**
     * HIỂN THỊ FORM THÊM/SỬA DỊCH VỤ (GET)
     * URL: /admin/service-form?id={id}
     */
    @GetMapping("/service-form")
    public String showServiceForm(@RequestParam(value = "id", required = false) Integer maDichVu,
            Model model,
            Authentication auth) {

        model.addAttribute("user", getCurrentUser(auth));
        DichVu dichVu = new DichVu();
        String pageTitle = "Tạo Dịch Vụ Mới";

        if (maDichVu != null) {
            // Chế độ chỉnh sửa
            dichVu = dichVuService.getDichVuById(maDichVu)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Dịch Vụ Mã: " + maDichVu));
            pageTitle = "Chỉnh Sửa Dịch Vụ #" + maDichVu;
        }

        model.addAttribute("dichVu", dichVu);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("serviceTypes", BlueMoon.bluemoon.utils.ServiceType.values());
        model.addAttribute("assetStatuses", BlueMoon.bluemoon.utils.AssetStatus.values());
        // Thêm Admin/Ban Quản Trị để chọn người phụ trách (nếu cần, tạm bỏ qua)
        // model.addAttribute("adminList",
        // nguoiDungService.getUsersByRole(UserRole.ADMIN));

        return "service-add-edit-admin"; // Trỏ đến file Thymeleaf mới
    }

    // -------------------------------------------------------

    /**
     * XỬ LÝ LƯU DỊCH VỤ (POST)
     * URL: /admin/service-save
     */
    @PostMapping("/service-save")
    public String handleServiceSave(@ModelAttribute("dichVu") DichVu dichVu,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        DoiTuong currentUser = getCurrentUser(auth); // Admin là người tạo/cập nhật

        try {
            // Tên Ban Quản Trị (Admin) phải được lấy từ currentUser (CCCD)
            DichVu savedDichVu = dichVuService.saveOrUpdateDichVu(dichVu, currentUser.getCccd());

            String message = (savedDichVu.getMaDichVu() == null)
                    ? "Tạo mới Dịch vụ thành công!"
                    : "Cập nhật Dịch vụ #" + savedDichVu.getMaDichVu() + " thành công!";

            redirectAttributes.addFlashAttribute("successMessage", message);
            return "redirect:/admin/service-list";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Quay lại form với ID (nếu có)
            String idParam = (dichVu.getMaDichVu() != null) ? "?id=" + dichVu.getMaDichVu() : "";
            return "redirect:/admin/service-form" + idParam;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi lưu: " + e.getMessage());
            return "redirect:/admin/service-list";
        }
    }

    // -------------------------------------------------------

    /**
     * XỬ LÝ XÓA DỊCH VỤ (GET/Chuyển trạng thái)
     * URL: /admin/service-delete?id={id}
     */
    @GetMapping("/service-delete")
    public String handleServiceDelete(@RequestParam("id") Integer maDichVu,
            RedirectAttributes redirectAttributes) {
        try {
            // Giả định logic xóa/chuyển trạng thái nằm trong Service
            dichVuService.deleteDichVu(maDichVu); // Thực tế nên là thay đổi trạng thái sang KHÔNG HOẠT ĐỘNG
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xóa (hoặc chuyển trạng thái) Dịch vụ #" + maDichVu + " thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống khi xóa: " + e.getMessage());
        }
        return "redirect:/admin/service-list";
    }

    @GetMapping("/service-registrations")
    public String showServiceRegistrations(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        // 1. Lấy danh sách tất cả đăng ký dịch vụ
        List<DangKyDichVu> allRegistrations = dangKyDichVuService.getAllDangKyDichVu(); // CẦN THÊM TRONG
                                                                                        // DangKyDichVuService

        model.addAttribute("allRegistrations", allRegistrations);
        model.addAttribute("registrationStatuses", BlueMoon.bluemoon.utils.RegistrationStatus.values());

        return "service-registrations-admin"; // Trỏ đến file Thymeleaf mới
    }
    // ========== EXPORT REPORTS ==========

    /**
     * Xuất báo cáo danh sách căn hộ ra file Excel
     */
    @GetMapping("/export/apartments")
    public ResponseEntity<byte[]> exportApartments() {
        try {
            List<ApartmentReportDTO> apartments = reportService.getApartmentReportForAdmin();
            byte[] excelData = exportService.exportApartmentsToExcel(apartments);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_CanHo_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo TẤT CẢ tài sản ra file Excel (không có bộ lọc)
     * URL: GET /admin/export/assets (không có query parameter)
     */
    @GetMapping(value = "/export/assets", params = {})
    public ResponseEntity<byte[]> exportAllAssets() {
        try {
            List<ApartmentReportDTO> assets = reportService.getAllAssetsReportForAdmin();
            byte[] excelData = exportService.exportApartmentsToExcel(assets);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_TatCaTaiSan_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo TẤT CẢ tài sản ra file PDF
     */
    @GetMapping("/export/assets/pdf")
    public ResponseEntity<byte[]> exportAllAssetsPdf() {
        try {
            List<ApartmentReportDTO> assets = reportService.getAllAssetsReportForAdmin();
            byte[] pdfData = exportService.exportApartmentsToPdf(assets);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_TatCaTaiSan_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo danh sách hóa đơn ra file Excel
     */
    @GetMapping("/export/invoices")
    public ResponseEntity<byte[]> exportInvoices() {
        try {
            List<InvoiceReportDTO> invoices = reportService.getInvoiceReportForAdmin();
            byte[] excelData = exportService.exportInvoicesToExcel(invoices);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_HoaDon_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // =======================================================
    // IMPORT EXCEL (MỚI)
    // =======================================================

    /**
     * 1. Hiển thị trang Import Excel
     * URL: /admin/fees/import
     */
    @GetMapping("/fees/import")
    public String showImportFeesPage(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));
        return "fees-import-admin"; // Trỏ đến file HTML sắp tạo
    }

    /**
     * 2. Xử lý Upload File Excel
     * URL: /admin/fees/import
     */
    @PostMapping("/fees/import")
    @SuppressWarnings({ "CallToPrintStackTrace", "UseSpecificCatch" })
    public String handleImportFees(@RequestParam("file") MultipartFile file,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        DoiTuong user = getCurrentUser(auth);

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file Excel.");
            return "redirect:/admin/fees/import";
        }

        try {
            // Gọi Service xử lý đọc file và lưu DB
            String result = hoaDonService.importHoaDonFromExcel(file, user);

            // Kiểm tra kết quả để hiển thị màu thông báo phù hợp
            if (result.contains("Thất bại") || result.contains("Lỗi")) {
                // Nếu có lỗi dòng nào đó, hiện thông báo dạng cảnh báo/lỗi
                redirectAttributes.addFlashAttribute("errorMessage", result);
            } else {
                redirectAttributes.addFlashAttribute("successMessage", result);
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/fees/import";
        }

        // Thành công thì quay về danh sách hóa đơn
        return "redirect:/admin/fees";
    }

    /**
     * Xuất báo cáo danh sách hộ gia đình ra file Excel
     */
    @GetMapping("/export/households")
    public ResponseEntity<byte[]> exportHouseholds() {
        try {
            List<HouseholdReportDTO> households = reportService.getHouseholdReportForAdmin();
            byte[] excelData = exportService.exportHouseholdsToExcel(households);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_HoGiaDinh_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo tài sản chung ra file Excel (có bộ lọc theo loại)
     * URL: GET /admin/export/assets?assetType=can_ho
     * 
     * @param assetType Loại tài sản (can_ho, tai_san_chung, etc.) - không bắt buộc
     */
    @GetMapping(value = "/export/assets", params = "assetType")
    public ResponseEntity<byte[]> exportAssets(@RequestParam(required = false) String assetType) {
        try {
            BlueMoon.bluemoon.utils.AssetType type = null;
            if (assetType != null && !assetType.isEmpty()) {
                type = BlueMoon.bluemoon.utils.AssetType.valueOf(assetType);
            }

            List<ApartmentReportDTO> assets = reportService.getAssetReportForAdmin(type);
            byte[] excelData = exportService.exportApartmentsToExcel(assets);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_TaiSan_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== PDF EXPORT ENDPOINTS ==========

    /**
     * Xuất báo cáo căn hộ ra file PDF
     */
    @GetMapping("/export/apartments/pdf")
    public ResponseEntity<byte[]> exportApartmentsPdf() {
        try {
            List<ApartmentReportDTO> apartments = reportService.getApartmentReportForAdmin();
            byte[] pdfData = exportService.exportApartmentsToPdf(apartments);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_CanHo_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
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
            List<InvoiceReportDTO> invoices = reportService.getInvoiceReportForAdmin();
            byte[] pdfData = exportService.exportInvoicesToPdf(invoices);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_HoaDon_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo hộ gia đình ra file PDF
     */
    @GetMapping("/export/households/pdf")
    public ResponseEntity<byte[]> exportHouseholdsPdf() {
        try {
            List<HouseholdReportDTO> households = reportService.getHouseholdReportForAdmin();
            byte[] pdfData = exportService.exportHouseholdsToPdf(households);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_HoGiaDinh_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo cư dân ra file Excel
     */
    @GetMapping("/export/residents")
    public ResponseEntity<byte[]> exportResidents() {
        try {
            List<ResidentReportDTO> residents = reportService.getResidentReportForAdmin();
            byte[] excelData = exportService.exportResidentsToExcel(residents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "BaoCao_CuDan_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báo cáo cư dân ra file PDF
     */
    @GetMapping("/export/residents/pdf")
    public ResponseEntity<byte[]> exportResidentsPdf() {
        try {
            List<ResidentReportDTO> residents = reportService.getResidentReportForAdmin();
            byte[] pdfData = exportService.exportResidentsToPdf(residents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "BaoCao_CuDan_" + System.currentTimeMillis() + ".pdf");

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
            headers.setContentDispositionFormData("attachment",
                    "ChiTiet_CanHo_" + maTaiSan + "_" + System.currentTimeMillis() + ".xlsx");

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
            headers.setContentDispositionFormData("attachment",
                    "ChiTiet_CanHo_" + maTaiSan + "_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất chi tiết cư dân ra file Excel
     */
    @GetMapping("/export/resident/{cccd}")
    public ResponseEntity<byte[]> exportResidentDetail(@PathVariable String cccd) {
        try {
            List<ResidentReportDTO> resident = reportService.getResidentDetailReport(cccd);
            if (resident.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] excelData = exportService.exportResidentsToExcel(resident);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "ChiTiet_CuDan_" + cccd + "_" + System.currentTimeMillis() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất chi tiết cư dân ra file PDF
     */
    @GetMapping("/export/resident/{cccd}/pdf")
    public ResponseEntity<byte[]> exportResidentDetailPdf(@PathVariable String cccd) {
        try {
            List<ResidentReportDTO> resident = reportService.getResidentDetailReport(cccd);
            if (resident.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdfData = exportService.exportResidentsToPdf(resident);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "ChiTiet_CuDan_" + cccd + "_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

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
            headers.setContentDispositionFormData("attachment",
                    "ChiTiet_HoaDon_" + maHoaDon + "_" + System.currentTimeMillis() + ".xlsx");

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
            headers.setContentDispositionFormData("attachment",
                    "ChiTiet_HoaDon_" + maHoaDon + "_" + System.currentTimeMillis() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // THÔNG BÁO
    // 📨 Hiển thị danh sách thông báo
    @GetMapping("/notifications")
    public String hienThiThongBao(Model model, Principal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        List<ThongBao> thongBaos = thongBaoService.layTatCaThongBaoMoiNhat();

        // ✅ LỌC THEO TÌM KIẾM VÀ NGÀY THÁNG
        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordLower = keyword.toLowerCase().trim();
            thongBaos = thongBaos.stream()
                    .filter(tb -> tb.getTieuDe().toLowerCase().contains(keywordLower) ||
                            tb.getNoiDung().toLowerCase().contains(keywordLower))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                java.time.LocalDate startDate = java.time.LocalDate.parse(fromDate);
                thongBaos = thongBaos.stream()
                        .filter(tb -> !tb.getThoiGianGui().toLocalDate().isBefore(startDate))
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                // Bỏ qua nếu format ngày không hợp lệ
            }
        }

        if (toDate != null && !toDate.isEmpty()) {
            try {
                java.time.LocalDate endDate = java.time.LocalDate.parse(toDate);
                thongBaos = thongBaos.stream()
                        .filter(tb -> !tb.getThoiGianGui().toLocalDate().isAfter(endDate))
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                // Bỏ qua nếu format ngày không hợp lệ
            }
        }

        model.addAttribute("thongBaos", thongBaos);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        // Lấy thông tin người đang đăng nhập
        DoiTuong user = null;
        if (principal != null) {
            user = doiTuongDAO.findByCccd(principal.getName()).orElse(null);
        }
        model.addAttribute("user", user);

        return "notification-admin";
    }

    // Gửi thông báo mới
    @PostMapping("/notifications/send")
    public String guiThongBao(
            @RequestParam("tieuDe") String tieuDe,
            @RequestParam("noiDung") String noiDung,
            Principal principal) {
        // Lấy người gửi thật từ tài khoản đang đăng nhập
        DoiTuong nguoiTao = null;
        if (principal != null) {
            nguoiTao = doiTuongDAO.findByCccd(principal.getName()).orElse(null);
        }

        // Nếu không có người đăng nhập (trường hợp test), dùng giả lập
        if (nguoiTao == null) {
            nguoiTao = new DoiTuong();
            nguoiTao.setCccd("BQT");
            nguoiTao.setHoVaTen("Ban Quản Trị");
        }

        // Gọi service để lưu thông báo
        thongBaoService.taoVaGuiThongBao(tieuDe, noiDung, nguoiTao);

        return "redirect:/admin/notifications?success=true";
    }

    /**
     * Endpoint REST API: Lấy danh sách phản hồi của một thông báo
     * URL: GET /admin/notifications/{maThongBao}/replies
     */
    @GetMapping("/notifications/{maThongBao}/replies")
    public ResponseEntity<List<PhanHoiThongBaoDTO>> getNotificationReplies(@PathVariable Integer maThongBao) {

        // 1. Lấy dữ liệu từ Service
        List<PhanHoiThongBaoDTO> replies = phanHoiThongBaoService.getRepliesByMaThongBao(maThongBao);

        // 2. Trả về JSON
        // Nếu không có phản hồi, trả về List rỗng (status 200)
        return ResponseEntity.ok(replies);
    }

    /**
     * Hiển thị trang báo cáo thống kê tổng hợp
     * URL: /admin/reports
     */
    @GetMapping("/reports") // Endpoint chung cho trang báo cáo
    public String showUnifiedReport(Model model, Authentication auth) {
        model.addAttribute("user", getCurrentUser(auth));

        // === PHẦN 1: DỮ LIỆU TÀI SẢN CHUNG ===
        Map<String, Object> assetStats = taiSanChungCuService.getGeneralAssetStatistics();
        model.addAttribute("assetTypeLabels", assetStats.get("typeLabels"));
        model.addAttribute("assetTypeData", assetStats.get("typeData"));
        model.addAttribute("assetStatusLabels", assetStats.get("statusLabels"));
        model.addAttribute("assetStatusData", assetStats.get("statusData"));
        model.addAttribute("assetLocationLabels", assetStats.get("locationLabels"));
        model.addAttribute("assetLocationData", assetStats.get("locationData"));

        // Danh sách bảng tài sản
        model.addAttribute("assetList", taiSanChungCuService.getGeneralAssetListReport());

        // === PHẦN 2: DỮ LIỆU CƯ DÂN ===
        // A. Thống kê theo Tòa/Tầng (Lấy từ TaiSanChungCuService cũ)
        Map<String, Object> buildingStats = taiSanChungCuService.getChartData(); // Hàm cũ bạn đã viết

        // Xử lý dữ liệu Tầng
        Map<Integer, Long> floorMap = (Map<Integer, Long>) buildingStats.get("floorStats");
        List<String> floorLabels = new ArrayList<>();
        List<Long> floorData = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : floorMap.entrySet()) {
            floorLabels.add("Tầng " + e.getKey());
            floorData.add(e.getValue());
        }
        model.addAttribute("floorLabels", floorLabels);
        model.addAttribute("floorData", floorData);

        // B. Thống kê Giới tính & Trạng thái (Lấy từ CuDanService mới)
        Map<String, Object> residentStats = cuDanService.getResidentStatistics();
        model.addAttribute("genderLabels", residentStats.get("genderLabels"));
        model.addAttribute("genderData", residentStats.get("genderData"));
        model.addAttribute("resStatusLabels", residentStats.get("resStatusLabels"));
        model.addAttribute("resStatusData", residentStats.get("resStatusData"));
        model.addAttribute("ageLabels", residentStats.get("ageLabels"));
        model.addAttribute("ageData", residentStats.get("ageData"));

        // === PHẦN 3: DỮ LIỆU HỘ GIA ĐÌNH ===
        Map<String, Object> householdStats = hoGiaDinhService.getHouseholdStatistics();
        model.addAttribute("householdFloorLabels", householdStats.get("householdFloorLabels"));
        model.addAttribute("householdFloorData", householdStats.get("householdFloorData"));
        model.addAttribute("averageMembers", householdStats.get("averageMembers"));
        model.addAttribute("totalHouseholds", householdStats.get("totalHouseholds"));
        model.addAttribute("totalMembers", householdStats.get("totalMembers"));
        model.addAttribute("householdSizeLabels", householdStats.get("householdSizeLabels"));
        model.addAttribute("householdSizeData", householdStats.get("householdSizeData"));

        return "reports-dashboard"; // Tên file HTML mới
    }

    /**
     * TEST ENDPOINT: Kiểm tra dữ liệu sự cố trong database
     * URL: /admin/test-incidents
     */
    @GetMapping("/test-incidents")
    @ResponseBody
    public String testIncidents() {
        StringBuilder result = new StringBuilder();
        result.append("<html><head><meta charset='UTF-8'><title>Kiểm Tra Dữ Liệu Sự Cố</title></head><body>");
        result.append("<h1>📊 KIỂM TRA DỮ LIỆU SỰ CỐ TRONG DATABASE</h1>");
        result.append("<hr>");

        // 1. Thống kê tổng quan
        result.append("<h2>1. Thống Kê Tổng Quan</h2>");
        result.append("<table border='1' cellpadding='10' style='border-collapse: collapse;'>");
        result.append("<tr style='background: #4F46E5; color: white;'>");
        result.append("<th>Tiêu Chí</th><th>Số Lượng</th></tr>");

        Long totalIncidents = suCoDAO.countAll();
        result.append("<tr><td><strong>Tổng số sự cố</strong></td><td>").append(totalIncidents).append("</td></tr>");

        Long newIncidents = suCoDAO.countByTrangThai(IncidentStatus.moi_tiep_nhan);
        result.append("<tr><td>Mới tiếp nhận</td><td>").append(newIncidents).append("</td></tr>");

        Long processingIncidents = suCoDAO.countByTrangThai(IncidentStatus.dang_xu_ly);
        result.append("<tr><td>Đang xử lý</td><td>").append(processingIncidents).append("</td></tr>");

        Long completedIncidents = suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh);
        result.append("<tr><td>Đã hoàn thành</td><td>").append(completedIncidents).append("</td></tr>");

        Long highPriority = suCoDAO.countByMucDoUuTien(PriorityLevel.cao);
        result.append("<tr style='background: #fee;'><td><strong>Ưu tiên CAO</strong></td><td><strong>")
                .append(highPriority).append("</strong></td></tr>");

        Long normalPriority = suCoDAO.countByMucDoUuTien(PriorityLevel.binh_thuong);
        result.append("<tr><td>Ưu tiên BÌNH THƯỜNG</td><td>").append(normalPriority).append("</td></tr>");

        Long lowPriority = suCoDAO.countByMucDoUuTien(PriorityLevel.thap);
        result.append("<tr><td>Ưu tiên THẤP</td><td>").append(lowPriority).append("</td></tr>");

        result.append("</table>");

        // 2. Danh sách sự cố ưu tiên cao
        result.append("<h2>2. Chi Tiết Sự Cố Ưu Tiên CAO (Hiển thị trên Dashboard)</h2>");
        List<BaoCaoSuCo> highPriorityIncidents = suCoDAO.findByMucDoUuTien(PriorityLevel.cao);

        if (highPriorityIncidents.isEmpty()) {
            result.append("<p style='color: green;'>✅ Không có sự cố ưu tiên cao cần xử lý gấp.</p>");
        } else {
            result.append("<table border='1' cellpadding='10' style='border-collapse: collapse; width: 100%;'>");
            result.append("<tr style='background: #EF4444; color: white;'>");
            result.append(
                    "<th>Mã</th><th>Tiêu Đề</th><th>Người Báo Cáo</th><th>Vị Trí</th><th>Trạng Thái</th><th>Thời Gian</th></tr>");

            for (BaoCaoSuCo incident : highPriorityIncidents) {
                result.append("<tr>");
                result.append("<td>").append(incident.getMaBaoCao()).append("</td>");
                result.append("<td><strong>").append(incident.getTieuDe()).append("</strong></td>");
                result.append("<td>")
                        .append(incident.getNguoiBaoCao() != null ? incident.getNguoiBaoCao().getHoVaTen() : "N/A")
                        .append("</td>");
                result.append("<td>").append(incident.getTaiSan() != null ? incident.getTaiSan().getViTri() : "N/A")
                        .append("</td>");
                result.append("<td>")
                        .append(incident.getTrangThai() != null ? incident.getTrangThai().getDbValue() : "N/A")
                        .append("</td>");
                result.append("<td>").append(incident.getThoiGianBaoCao()).append("</td>");
                result.append("</tr>");
            }
            result.append("</table>");
        }

        // 3. Tất cả sự cố trong database
        result.append("<h2>3. Tất Cả Sự Cố Trong Database</h2>");
        List<BaoCaoSuCo> allIncidents = suCoDAO.findAll();

        if (allIncidents.isEmpty()) {
            result.append("<p style='color: red;'>❌ Database không có dữ liệu sự cố nào!</p>");
            result.append("<p><strong>Hướng dẫn thêm dữ liệu test:</strong></p>");
            result.append(
                    "<pre>INSERT INTO bao_cao_su_co (cccd_nguoi_bao_cao, ma_tai_san, tieu_de, mo_ta_su_co, muc_do_uu_tien, trang_thai, thoi_gian_bao_cao) VALUES\n");
            result.append(
                    "('123456789012', 1, 'Hỏng thang máy Tòa A', 'Thang máy tầng 5 bị kẹt', 'cao', 'moi_tiep_nhan', NOW()),\n");
            result.append(
                    "('123456789012', 2, '[Chỗ đỗ xe] Quá tải', 'Chỗ đỗ xe tầng hầm đầy', 'cao', 'moi_tiep_nhan', NOW());</pre>");
        } else {
            result.append("<p>Tổng cộng: <strong>").append(allIncidents.size()).append("</strong> sự cố</p>");
            result.append("<table border='1' cellpadding='10' style='border-collapse: collapse; width: 100%;'>");
            result.append("<tr style='background: #4F46E5; color: white;'>");
            result.append(
                    "<th>Mã</th><th>Tiêu Đề</th><th>Mô Tả</th><th>Ưu Tiên</th><th>Trạng Thái</th><th>Người BC</th><th>Tài Sản</th><th>Thời Gian</th></tr>");

            for (BaoCaoSuCo incident : allIncidents) {
                String bgColor = "";
                if (incident.getMucDoUuTien() == PriorityLevel.cao) {
                    bgColor = "background: #fee;";
                }
                result.append("<tr style='").append(bgColor).append("'>");
                result.append("<td>").append(incident.getMaBaoCao()).append("</td>");
                result.append("<td><strong>").append(incident.getTieuDe()).append("</strong></td>");
                result.append("<td>").append(incident.getNoiDung() != null ? incident.getNoiDung() : "")
                        .append("</td>");
                result.append("<td>")
                        .append(incident.getMucDoUuTien() != null ? incident.getMucDoUuTien().getDbValue() : "N/A")
                        .append("</td>");
                result.append("<td>")
                        .append(incident.getTrangThai() != null ? incident.getTrangThai().getDbValue() : "N/A")
                        .append("</td>");
                result.append("<td>")
                        .append(incident.getNguoiBaoCao() != null ? incident.getNguoiBaoCao().getHoVaTen() : "N/A")
                        .append("</td>");
                result.append("<td>")
                        .append(incident.getTaiSan() != null
                                ? incident.getTaiSan().getTenTaiSan() + " (" + incident.getTaiSan().getViTri() + ")"
                                : "N/A")
                        .append("</td>");
                result.append("<td>").append(incident.getThoiGianBaoCao()).append("</td>");
                result.append("</tr>");
            }
            result.append("</table>");
        }

        // 4. Thông tin bảng database
        result.append("<h2>4. Thông Tin Cấu Trúc Bảng</h2>");
        result.append("<p><strong>Tên bảng:</strong> <code>bao_cao_su_co</code></p>");
        result.append("<p><strong>Các cột:</strong></p>");
        result.append("<ul>");
        result.append("<li><code>ma_bao_cao</code> - ID tự động tăng (Primary Key)</li>");
        result.append("<li><code>cccd_nguoi_bao_cao</code> - CCCD người báo cáo (Foreign Key → doi_tuong)</li>");
        result.append("<li><code>ma_tai_san</code> - Mã tài sản bị sự cố (Foreign Key → tai_san_chung_cu)</li>");
        result.append("<li><code>tieu_de</code> - Tiêu đề sự cố (VARCHAR 255)</li>");
        result.append("<li><code>mo_ta_su_co</code> - Mô tả chi tiết (TEXT)</li>");
        result.append("<li><code>muc_do_uu_tien</code> - Mức độ: 'cao', 'binh_thuong', 'thap'</li>");
        result.append(
                "<li><code>trang_thai</code> - Trạng thái: 'moi_tiep_nhan', 'dang_xu_ly', 'da_hoan_thanh', 'da_huy'</li>");
        result.append("<li><code>thoi_gian_bao_cao</code> - Thời gian tạo (TIMESTAMP)</li>");
        result.append("<li><code>thoi_gian_cap_nhat</code> - Thời gian cập nhật (TIMESTAMP)</li>");
        result.append("<li><code>chi_phi_xu_ly</code> - Chi phí xử lý (DECIMAL)</li>");
        result.append("</ul>");

        result.append("<hr>");
        result.append("<p><a href='/admin/dashboard'>← Quay lại Dashboard</a></p>");
        result.append("</body></html>");

        return result.toString();
    }
}