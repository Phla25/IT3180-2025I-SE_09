package BlueMoon.bluemoon.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.HoaDonDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.models.HoaDonStatsDTO;
import BlueMoon.bluemoon.utils.InvoiceStatus;
import BlueMoon.bluemoon.utils.InvoiceType;
import jakarta.transaction.Transactional;

@Service
public class HoaDonService {
    @Autowired private DoiTuongDAO doiTuongDAO;
    @Autowired private HoaDonDAO hoaDonDAO;
    @Autowired private HoGiaDinhService hoGiaDinhService;
    @Autowired@SuppressWarnings("unused")
    private ThanhVienHoService thanhVienHoService;

    /**
     * Lấy thông tin thống kê hóa đơn chính cho dashboard cư dân (DỮ LIỆU THẬT).
     */
    public HoaDonStatsDTO getHoaDonStats(HoGiaDinh hoGiaDinh) {
        HoaDonStatsDTO stats = new HoaDonStatsDTO();
        
        if (hoGiaDinh == null) {
            return stats;
        }
        
        // 1. Lấy tất cả hóa đơn của hộ này
        List<HoaDon> invoices = getAllHoaDonByHo(hoGiaDinh); 
        
        // 2. Lọc các hóa đơn Chưa thanh toán hoặc Quá hạn
        List<HoaDon> pendingInvoices = invoices.stream()
            .filter(hd -> hd.getTrangThai() == InvoiceStatus.chua_thanh_toan || hd.getTrangThai() == InvoiceStatus.qua_han)
            .collect(Collectors.toList());
            
        // 3. Tính tổng tiền
        BigDecimal tongChuaThanhToan = pendingInvoices.stream()
            .map(HoaDon::getSoTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add); // Cộng dồn số tiền
            
        int soLuong = pendingInvoices.size();
        
        // 4. Gán vào DTO
        stats.tongChuaThanhToan = tongChuaThanhToan;
        stats.tongHoaDonChuaThanhToan = soLuong;

        if (soLuong > 0) {
            stats.trangThai = soLuong + " hóa đơn chưa thanh toán";
        } else {
             stats.trangThai = "Đã thanh toán hết";
        }
        
        return stats;
    }
    /**
     * Lấy danh sách hóa đơn gần đây nhất của một hộ gia đình.
     */
    public List<HoaDon> getRecentHoaDon(HoGiaDinh hoGiaDinh, int limit) {
        if (hoGiaDinh == null) {
                return Collections.emptyList();
            }
            
            List<HoaDon> list = hoaDonDAO.findByHoGiaDinh(hoGiaDinh);
            
            // Sắp xếp theo ngày tạo (mới nhất lên đầu)
            list.sort(Comparator.comparing(HoaDon::getNgayTao, Comparator.reverseOrder()));
            
            return list.size() > limit ? list.subList(0, limit) : list;
    }
        
    /**
     * Lấy danh sách hóa đơn cần Kế toán xử lý/xác nhận.
     */
    public List<HoaDon> getHoaDonCanXacNhan(InvoiceStatus trangThai, int limit) {
        // Gọi phương thức DAO có JOIN phức hợp và sắp xếp
        List<HoaDon> list = hoaDonDAO.findByTrangThaiWithChuHo(trangThai);
        
        // Giới hạn số lượng ở tầng Service
        return list.size() > limit ? list.subList(0, limit) : list;
    }
        
    // =======================================================
    // CHỨC NĂNG 1: BÁO CÁO TÀI CHÍNH (THỐNG KÊ TỔNG QUAN)
    // =======================================================
    public HoaDonStatsDTO getAccountantStats() {   
        HoaDonStatsDTO stats = new HoaDonStatsDTO();
        
        // 1. Xác định thời gian tháng hiện tại (Từ ngày 1 đến cuối tháng)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(23, 59, 59);

        // 2. Tính TỔNG THU THÁNG NAY (Chỉ tính tiền về trong tháng này)
        BigDecimal tongThu = hoaDonDAO.sumTongThuByDateRange(
            InvoiceStatus.da_thanh_toan, startOfMonth, endOfMonth
        );
        
        // 3. Tính TỔNG CHI THÁNG NAY (Chỉ tính tiền chi trong tháng này)
        BigDecimal tongChi = hoaDonDAO.sumTongChiByDateRange(
            InvoiceStatus.da_thanh_toan, startOfMonth, endOfMonth
        );

        // 4. Tính CÔNG NỢ (Tổng số tiền chưa thu được từ trước tới nay)
        // Dùng hàm cũ sumSoTienByTrangThai (tính all time) cho trạng thái chưa thanh toán
        BigDecimal tongChuaThu = hoaDonDAO.sumSoTienByTrangThai(InvoiceStatus.chua_thanh_toan);
        
        // 5. Tính NỢ QUÁ HẠN
        List<HoaDon> pendingInvoices = hoaDonDAO.findByTrangThai(InvoiceStatus.chua_thanh_toan);
        BigDecimal tongQuaHan = pendingInvoices.stream()
            .filter(hd -> hd.getHanThanhToan() != null && hd.getHanThanhToan().isBefore(now.toLocalDate()))
            .map(HoaDon::getSoTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gán dữ liệu vào DTO
        stats.tongThuThangNay = tongThu; 
        stats.tongChiThangNay = tongChi; 
        stats.loiNhuanRong = tongThu.subtract(tongChi);
        stats.tongChuaThanhToan = tongChuaThu;
        stats.tongQuaHan = tongQuaHan;
        
        return stats;
    }

    // =======================================================
    // CHỨC NĂNG 2: BIỂU ĐỒ DOANH THU (6 THÁNG GẦN NHẤT)
    // =======================================================
    public Map<String, BigDecimal> getMonthlyRevenueStats() {
        Map<String, BigDecimal> stats = new LinkedHashMap<>(); // Giữ thứ tự chèn
        LocalDate today = LocalDate.now();

        // Lặp 6 tháng gần nhất (bao gồm tháng hiện tại)
        for (int i = 5; i >= 0; i--) {
            LocalDate date = today.minusMonths(i);
            
            // Tính ngày đầu tháng và cuối tháng của tháng đó
            LocalDateTime start = date.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59);
            
            // Gọi DAO để tính tổng thu của tháng đó
            BigDecimal revenue = hoaDonDAO.sumTongThuByDateRange(
                InvoiceStatus.da_thanh_toan, start, end
            );
            
            // Tạo key hiển thị (Ví dụ: "Tháng 12/2024")
            String key = "Tháng " + date.getMonthValue() + "/" + date.getYear();
            stats.put(key, revenue);
        }
        return stats;
    }

    // =======================================================
    // CHỨC NĂNG 3: LỊCH SỬ GIAO DỊCH
    // =======================================================
    public List<HoaDon> getTransactionHistory() {
        // Lấy tất cả hóa đơn đã thanh toán
        List<HoaDon> list = hoaDonDAO.findByTrangThai(InvoiceStatus.da_thanh_toan);
        
        // Sắp xếp: Giao dịch mới nhất lên đầu (dựa theo ngày thanh toán thực tế)
        list.sort((h1, h2) -> {
            LocalDateTime d1 = h1.getNgayThanhToan();
            LocalDateTime d2 = h2.getNgayThanhToan();
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1); // Giảm dần
        });
        
        return list;
    }
    /**
     * Lấy tất cả hóa đơn của một hộ gia đình.
     */
    public List<HoaDon> getAllHoaDonByHo(HoGiaDinh hoGiaDinh) {
        if (hoGiaDinh == null) {
            return Collections.emptyList();
        }
        // Dùng DAO có sẵn
        return hoaDonDAO.findByHoGiaDinh(hoGiaDinh);
    }
    
    /**
     * Lấy một hóa đơn theo ID và Hộ gia đình (đảm bảo bảo mật).
     */
    public Optional<HoaDon> getHoaDonByIdAndHo(Integer maHoaDon, HoGiaDinh hoGiaDinh) {
        // Cần thêm hàm này vào HoaDonDAO nếu chưa có
        // Tạm thời lấy tất cả hóa đơn của hộ và lọc (Kém hiệu quả, nên dùng DAO)
        return hoaDonDAO.findByHoGiaDinh(hoGiaDinh).stream()
            .filter(hd -> hd.getMaHoaDon().equals(maHoaDon))
            .findFirst();
    }

    /**
     * CẬP NHẬT: Mock chức năng thanh toán của Cư Dân.
     * Lưu thông tin người thực hiện thanh toán vào trường MỚI.
     */
    @Transactional 
    public void markAsPaidByResident(Integer maHoaDon, DoiTuong nguoiThanhToan) {
        // ... (Tìm HoaDon) ...
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        
        if (hd.getTrangThai() == InvoiceStatus.da_thanh_toan) {
            throw new IllegalArgumentException("Hóa đơn #" + maHoaDon + " đã được thanh toán.");
        }
        if (hd.getTrangThai() == InvoiceStatus.cho_xac_nhan) {
            throw new IllegalArgumentException("Hóa đơn #" + maHoaDon + " đang chờ Kế toán xác nhận. Không thể thanh toán lại.");
        }
        
        hd.setTrangThai(InvoiceStatus.cho_xac_nhan);
        // ✨ LƯU NGƯỜI THANH TOÁN VÀO TRƯỜNG MỚI
        hd.setNguoiThanhToan(nguoiThanhToan); 
        // Giữ nguyên nguoiThucHienGiaoDich (người tạo ra hóa đơn)
    }
    /**
     * CẬP NHẬT: Lưu hoặc Cập nhật Hóa đơn (Chức năng CRUD) bởi Admin/Kế toán.
     * * Quy tắc logic mới cho nguoiDangKyDichVu:
     * 1. TẠO MỚI (bởi Admin/Kế toán): nguoiDangKyDichVu = Chủ hộ.
     * 2. CẬP NHẬT: Giữ lại nguoiDangKyDichVu cũ (có thể là Chủ hộ hoặc người đăng ký dịch vụ).
     * 3. HỆ THỐNG TỰ SINH (Đăng ký DV): logic này nằm trong DangKyDichVuService, không bị ảnh hưởng.
     * * @param hoaDon Đối tượng HoaDon được bind từ form.
     * @param maHo Mã Hộ gia đình được chọn từ form.
     * @param nguoiThucHien Đối tượng Admin/Kế toán đang thực hiện thao tác.
     * @return HoaDon đã được lưu.
     */
    /**
     * [QUAN TRỌNG] Lưu Hóa đơn và gán Người Đứng Tên (DoiTuong)
     * Cập nhật thêm tham số isPhieuChi
     */
    @Transactional
    public HoaDon saveOrUpdateHoaDon(HoaDon hoaDon, String maHo, String nguoiDangKyCccd, DoiTuong nguoiThucHien, boolean isPhieuChi) {
    
        final boolean isNewInvoice = hoaDon.getMaHoaDon() == null;
    
        if (isPhieuChi) {
            // LOGIC PHIẾU CHI
            // 1. Người đứng tên hóa đơn là chính Admin/Kế toán đang login
            hoaDon.setNguoiDangKyDichVu(nguoiThucHien);
            
            // 2. Gán vào Hộ BQT hoặc để null (nếu DB cho phép null)
            // Vì entity HoaDon của bạn có: @JoinColumn(name = "ma_ho", nullable = true) -> Có thể để null
            hoaDon.setHoGiaDinh(null); 
            
            // 3. Set trạng thái là đã thanh toán luôn (thường phiếu chi là chi ngay)
            if (isNewInvoice) {
                hoaDon.setTrangThai(InvoiceStatus.da_thanh_toan); 
                hoaDon.setNgayThanhToan(LocalDateTime.now());
            }
        } else {
            // LOGIC PHIẾU THU (Logic cũ)
            if (maHo == null || maHo.isEmpty()) {
                 throw new IllegalArgumentException("Vui lòng chọn Hộ gia đình cho phiếu thu.");
            }
            
            Optional<HoGiaDinh> hoGiaDinhopt = hoGiaDinhService.getHouseholdById(maHo);
            HoGiaDinh hoGiaDinh = hoGiaDinhopt.orElseThrow(() -> new IllegalArgumentException("Mã hộ không tồn tại"));
            hoaDon.setHoGiaDinh(hoGiaDinh);

            // Xử lý người đứng tên (như cũ)
            if (nguoiDangKyCccd != null && !nguoiDangKyCccd.isEmpty()) {
                DoiTuong nguoiDangKy = doiTuongDAO.findByCccd(nguoiDangKyCccd)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân"));
                
                // Check thành viên...
                boolean isMember = hoGiaDinh.getThanhVienHoList().stream()
                    .anyMatch(tvh -> tvh.getDoiTuong().getCccd().equals(nguoiDangKyCccd));

                if (!isMember) {
                    throw new IllegalArgumentException("Người được chọn không thuộc hộ gia đình này!");
                }
                hoaDon.setNguoiDangKyDichVu(nguoiDangKy);
            }
            
            // Logic cập nhật cũ...
            if (!isNewInvoice) {
                HoaDon hdOriginal = hoaDonDAO.findById(hoaDon.getMaHoaDon()) // Dùng findById thay vì stream lọc
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hóa đơn gốc."));
                
                if (hoaDon.getNguoiThanhToan() == null) hoaDon.setNguoiThanhToan(hdOriginal.getNguoiThanhToan());
                if (nguoiDangKyCccd == null || nguoiDangKyCccd.isEmpty()) {
                    hoaDon.setNguoiDangKyDichVu(hdOriginal.getNguoiDangKyDichVu());
                }
            }
        }

        // 4. Thiết lập mặc định chung
        if (isNewInvoice) {
            if (hoaDon.getNgayTao() == null) hoaDon.setNgayTao(LocalDateTime.now());
            if (hoaDon.getTrangThai() == null) hoaDon.setTrangThai(InvoiceStatus.chua_thanh_toan);
        }
    
        return hoaDonDAO.save(hoaDon);
    }
    
    // Giữ lại hàm cũ (Overload) để tương thích ngược (nếu cần)
    @Transactional
    public HoaDon saveOrUpdateHoaDon(HoaDon hoaDon, String maHo, String nguoiDangKyCccd, DoiTuong nguoiThucHien) {
        return saveOrUpdateHoaDon(hoaDon, maHo, nguoiDangKyCccd, nguoiThucHien, false); // Mặc định là Thu
    }
    /**
     * Lấy danh sách hóa đơn đang ở trạng thái chờ xác nhận.
     */
    public List<HoaDon> getHoaDonChoXacNhan(int limit) {
        List<HoaDon> list = hoaDonDAO.findByTrangThaiWithChuHo(InvoiceStatus.cho_xac_nhan);
        // Sắp xếp theo ngày tạo (mới nhất lên đầu)
        list.sort(Comparator.comparing(HoaDon::getNgayTao, Comparator.reverseOrder()));
        return list.size() > limit ? list.subList(0, limit) : list;
    }
    /**
     * Lấy tất cả hóa đơn cho Kế Toán/Admin.
     */
    public List<HoaDon> getAllHoaDon() {
        return hoaDonDAO.findAllWithHoGiaDinh(); // Dùng hàm có FETCH JOIN
    }
    
    /**
     * Hàm lấy Hóa đơn theo ID (cho Admin/Accountant)
     */
    public Optional<HoaDon> getHoaDonById(Integer maHoaDon) {
         return hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon))
            .findFirst();
    }
    /**
     * Xóa Hóa đơn.
     */
    @Transactional
    public void deleteHoaDon(Integer maHoaDon) {
        // Cần thêm hàm findById vào HoaDonDAO nếu chưa có
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
                             .filter(h -> h.getMaHoaDon().equals(maHoaDon))
                             .findFirst()
                             .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn cần xóa."));
        // Cần thêm hàm delete vào HoaDonDAO
        hoaDonDAO.delete(hd); 
    }

    /**
     * Kế Toán Xác nhận Thanh toán Hóa đơn.
     */
    @Transactional
    public void confirmPayment(Integer maHoaDon, DoiTuong nguoiXacNhan) {
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        
        if (hd.getTrangThai() == InvoiceStatus.da_thanh_toan) {
            throw new IllegalArgumentException("Hóa đơn đã được thanh toán trước đó.");
        }
        
        // Chuyển trạng thái sang đã thanh toán
        hd.setTrangThai(InvoiceStatus.da_thanh_toan);
        hd.setNgayThanhToan(LocalDateTime.now());
    }
    /**
     * [CẬP NHẬT] Import Hóa Đơn từ Excel
     * Logic mới: 
     * - Có CCCD => Gán cho thành viên đó (nếu hợp lệ).
     * - Không CCCD => Để NULL (Hóa đơn chung của hộ), KHÔNG tự gán chủ hộ.
     */
    @Transactional
    public String importHoaDonFromExcel(MultipartFile file, DoiTuong nguoiThucHien) throws IOException {
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorLog = new StringBuilder();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // Bỏ qua dòng tiêu đề
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                // Bỏ qua dòng trống
                if (currentRow.getCell(0) == null || getCellValueAsString(currentRow.getCell(0)).isEmpty()) {
                    continue; 
                }

                try {
                    // --- 1. ĐỌC DỮ LIỆU ---
                    String maHo = getCellValueAsString(currentRow.getCell(0));       // Cột 0: Mã Hộ
                    String cccdThanhVien = getCellValueAsString(currentRow.getCell(1)); // Cột 1: CCCD (Có thể rỗng)
                    String loaiPhiStr = getCellValueAsString(currentRow.getCell(2));    // Cột 2: Loại phí
                    
                    // Cột 3: Số tiền
                    BigDecimal soTien = BigDecimal.ZERO;
                    Cell amountCell = currentRow.getCell(3);
                    if (amountCell != null && amountCell.getCellType() == CellType.NUMERIC) {
                        soTien = BigDecimal.valueOf(amountCell.getNumericCellValue());
                    }

                    // Cột 4: Hạn thanh toán
                    LocalDate hanThanhToan = LocalDate.now().plusDays(15);
                    Cell dateCell = currentRow.getCell(4);
                    if (dateCell != null) {
                        try {
                            if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                                hanThanhToan = dateCell.getLocalDateTimeCellValue().toLocalDate();
                            }
                        } catch (Exception ex) { /* Dùng ngày mặc định */ }
                    }

                    String ghiChu = getCellValueAsString(currentRow.getCell(5)); // Cột 5: Ghi chú

                    // --- 2. VALIDATE & XỬ LÝ ---
                    
                    // Validate cơ bản
                    if (maHo.isEmpty()) throw new IllegalArgumentException("Mã hộ không được để trống");
                    if (soTien.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0");

                    // Tìm Hộ Gia Đình
                    HoGiaDinh hoGiaDinh = hoGiaDinhService.getHouseholdById(maHo)
                        .orElseThrow(() -> new IllegalArgumentException("Mã hộ '" + maHo + "' không tồn tại"));

                    // Map Enum Loại phí
                    InvoiceType loaiHoaDon = mapToInvoiceType(loaiPhiStr);

                    // Khởi tạo hóa đơn
                    HoaDon hoaDon = new HoaDon();
                    hoaDon.setHoGiaDinh(hoGiaDinh); // Gán hộ
                    hoaDon.setLoaiHoaDon(loaiHoaDon);
                    hoaDon.setSoTien(soTien);
                    hoaDon.setHanThanhToan(hanThanhToan);
                    hoaDon.setGhiChu(ghiChu);
                    
                    // Thiết lập thông tin mặc định
                    hoaDon.setNgayTao(LocalDateTime.now());
                    hoaDon.setTrangThai(InvoiceStatus.chua_thanh_toan);

                    // --- 3. XỬ LÝ NGƯỜI ĐỨNG TÊN (LOGIC MỚI) ---
                    if (cccdThanhVien != null && !cccdThanhVien.isEmpty()) {
                        // Nếu Excel CÓ nhập CCCD -> Bắt buộc phải tìm thấy và phải thuộc hộ
                        DoiTuong thanhVien = doiTuongDAO.findByCccd(cccdThanhVien)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân với CCCD: " + cccdThanhVien));
                        
                        boolean isMember = hoGiaDinh.getThanhVienHoList().stream()
                            .anyMatch(tvh -> tvh.getDoiTuong().getCccd().equals(cccdThanhVien)); // Có thể bỏ check ngày kết thúc nếu muốn thu phí người cũ
                        
                        if (!isMember) {
                            throw new IllegalArgumentException("Cư dân " + cccdThanhVien + " không thuộc hộ " + maHo);
                        }
                        
                        hoaDon.setNguoiDangKyDichVu(thanhVien);
                    } else {
                        // Nếu Excel KHÔNG nhập CCCD -> Để NULL (Hóa đơn chung của hộ)
                        hoaDon.setNguoiDangKyDichVu(null);
                    }

                    // --- 4. LƯU TRỰC TIẾP ---
                    // Dùng hoaDonDAO.save trực tiếp để tránh logic "tự gán chủ hộ" trong hàm saveOrUpdateHoaDon
                    hoaDonDAO.save(hoaDon);
                    
                    successCount++;

                } catch (IllegalArgumentException e) {
                    errorCount++;
                    String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Lỗi dữ liệu";
                    errorLog.append("Dòng ").append(rowNumber + 1).append(": ").append(errorMsg).append("\n");
                }
                rowNumber++;
            }
        }
        

        // Trả về báo cáo
        StringBuilder result = new StringBuilder();
        result.append("Kết quả Import: Thành công ").append(successCount).append(" hóa đơn.");
        if (errorCount > 0) {
            result.append(" Thất bại ").append(errorCount).append(" dòng.\n");
            result.append("Chi tiết lỗi:\n").append(errorLog.toString());
        }
        return result.toString();
    }
    // =========================================================================
    // CÁC HÀM PHỤ TRỢ (HELPER) CHO VIỆC ĐỌC EXCEL
    // =========================================================================

    /**
     * Hàm chuyển đổi giá trị của ô Excel sang String một cách an toàn.
     * Xử lý được cả ô Text, ô Số, ô Công thức, ô Boolean.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    // Xử lý ngày tháng
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    // Xử lý số (tránh lỗi hiển thị số khoa học như 1.23E9)
                    long longVal = (long) cell.getNumericCellValue();
                    if (cell.getNumericCellValue() == longVal) {
                        return String.valueOf(longVal); // Trả về số nguyên nếu không có phần thập phân
                    }
                    return String.valueOf(cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    // Nếu là công thức, cố gắng lấy giá trị kết quả
                    try {
                        return cell.getStringCellValue();
                    } catch (IllegalStateException e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                case BLANK:
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Hàm map từ tên tiếng Việt trong Excel sang Enum hệ thống
     */
    private InvoiceType mapToInvoiceType(String text) {
        if (text == null) return InvoiceType.khac;
        String normalized = text.trim().toLowerCase();
        
        if (normalized.contains("quản lý")) return InvoiceType.khac;
        if (normalized.contains("dịch vụ")) return InvoiceType.dich_vu;
        if (normalized.contains("điện")) return InvoiceType.dich_vu;
        if (normalized.contains("nước")) return InvoiceType.dich_vu;
        if (normalized.contains("gửi xe")) return InvoiceType.dich_vu;
        if (normalized.contains("vệ sinh")) return InvoiceType.dich_vu;
        if (normalized.contains("đóng góp")) return InvoiceType.khac;
        
        return InvoiceType.khac;
    }
    
    /**
     * Lấy tất cả hóa đơn đã thanh toán (Lịch sử giao dịch).
     */
    public List<HoaDon> getAllPaidHoaDon() {
        return hoaDonDAO.findByTrangThai(InvoiceStatus.da_thanh_toan);
    }
    /**
     * Kế Toán Từ Chối Xác nhận Thanh toán Hóa đơn.
     * Chuyển trạng thái từ CHỜ XÁC NHẬN về CHƯA THANH TOÁN.
     */
    @Transactional
    public void rejectPayment(Integer maHoaDon, DoiTuong nguoiThucHien) {
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        
        // Chỉ từ chối khi hóa đơn ở trạng thái CHỜ XÁC NHẬN
        if (hd.getTrangThai() != InvoiceStatus.cho_xac_nhan) {
            throw new IllegalArgumentException("Chỉ có thể từ chối hóa đơn đang ở trạng thái Chờ Xác Nhận. Trạng thái hiện tại: " + hd.getTrangThai().getDbValue());
        }
        
        // Chuyển trạng thái về chưa thanh toán
        hd.setTrangThai(InvoiceStatus.chua_thanh_toan);
        hd.setNgayThanhToan(null);
        hd.setNguoiThanhToan(null); // Xóa thông tin người đã thanh toán (cư dân)
        
        hoaDonDAO.save(hd);
    }
    /**
     * Xử lý khi nhận được phản hồi THÀNH CÔNG từ VNPay/Cổng thanh toán.
     * Cập nhật trạng thái hóa đơn và ghi nhận giao dịch.
     */
    @Transactional
    public void xuLyThanhToanThanhCong(Integer maHoaDon, DoiTuong nguoiThanhToan, String maGiaoDichNganHang) {
        
        // 1. Tìm hóa đơn
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));

        // 2. Kiểm tra trạng thái để tránh xử lý lặp lại
        if (hd.getTrangThai() == InvoiceStatus.da_thanh_toan) {
            return; 
        }

        // 3. Cập nhật trạng thái
        hd.setTrangThai(InvoiceStatus.da_thanh_toan);
        hd.setNgayThanhToan(LocalDateTime.now());
        
        // 4. Ghi nhận người thanh toán (Cư dân thực hiện)
        hd.setNguoiThanhToan(nguoiThanhToan);
        
        // 5. Ghi chú mã giao dịch vào hóa đơn để đối soát sau này
        String ghiChuHienTai = (hd.getGhiChu() == null) ? "" : hd.getGhiChu();
        String noiDungGiaoDich = " | Thanh toán Online qua VNPay. Mã GD: " + maGiaoDichNganHang;
        
        // Tránh ghi chú quá dài nếu update nhiều lần
        if (ghiChuHienTai.length() + noiDungGiaoDich.length() < 255) {
            hd.setGhiChu(ghiChuHienTai + noiDungGiaoDich);
        } else {
            hd.setGhiChu(noiDungGiaoDich); // Ghi đè nếu quá dài
        }
        
        // 6. Lưu vào DB
        hoaDonDAO.save(hd);
    }
    /**
     * Duyệt nhiều hóa đơn cùng lúc (Batch Confirmation)
     * @param maHoaDonList Danh sách ID hóa đơn cần duyệt
     * @return Số lượng hóa đơn được duyệt thành công
     */
    @Transactional
    public int confirmMultiplePayments(List<Integer> maHoaDonList) {
        int successCount = 0;
    
        for (Integer maHoaDon : maHoaDonList) {
            try {
                HoaDon hd = hoaDonDAO.findById(maHoaDon)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + maHoaDon));

                // Chỉ duyệt những hóa đơn đang ở trạng thái "Chờ xác nhận"
                if (hd.getTrangThai() == InvoiceStatus.cho_xac_nhan) {
                    hd.setTrangThai(InvoiceStatus.da_thanh_toan);
                    hd.setNgayThanhToan(LocalDateTime.now());
                    hoaDonDAO.save(hd);
                    successCount++;
                }
            } catch (Exception e) {
                // Log lỗi nhưng tiếp tục duyệt các hóa đơn khác
                System.err.println("Lỗi duyệt hóa đơn #" + maHoaDon + ": " + e.getMessage());
            }
        }
    
        return successCount;
    }

    /**
     * Từ chối nhiều hóa đơn cùng lúc
     * @param maHoaDonList Danh sách ID hóa đơn cần từ chối
     * @param nguoiXacNhan Kế toán/Admin thực hiện
     * @return Số lượng hóa đơn bị từ chối
     */
    @Transactional
    public int rejectMultiplePayments(List<Integer> maHoaDonList, DoiTuong nguoiXacNhan) {
        int successCount = 0;
    
        for (Integer maHoaDon : maHoaDonList) {
            try {
                HoaDon hd = hoaDonDAO.findById(maHoaDon)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + maHoaDon));

                if (hd.getTrangThai() == InvoiceStatus.cho_xac_nhan) {
                    // Chuyển về trạng thái "Chưa thanh toán"
                    if (hd.getHanThanhToan() != null && hd.getHanThanhToan().isBefore(LocalDate.now())) {
                        hd.setTrangThai(InvoiceStatus.qua_han);
                    } else {
                        hd.setTrangThai(InvoiceStatus.chua_thanh_toan);
                    }
                    hd.setNguoiThanhToan(null); // Xóa người thanh toán
                    hoaDonDAO.save(hd);
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("Lỗi từ chối hóa đơn #" + maHoaDon + ": " + e.getMessage());
            }
        }
    
        return successCount;
    }
    
}