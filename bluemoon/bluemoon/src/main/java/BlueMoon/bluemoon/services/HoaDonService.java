package BlueMoon.bluemoon.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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
import BlueMoon.bluemoon.entities.ThanhVienHo;
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

    // =========================================================================
    // 1. CẤU HÌNH REGEX (LUẬT CÚ PHÁP CHẶT CHẼ)
    // =========================================================================
    
    // 1. Dịch vụ: [TÊN] [mm/yyyy] -> VD: DIEN 10/2025
    private static final String REGEX_DICH_VU = "^[A-Z0-9_ ]+ (0[1-9]|1[0-2])/\\d{4}$";

    // 2. Sửa chữa: SUA CHUA [Nội dung] [dd/mm/yyyy] -> VD: SUA CHUA BONG DEN 25/10/2025
    private static final String REGEX_SUA_CHUA = "^SUA CHUA .+ (0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$";

    // 3. Phạt: PHAT [Nội dung] [dd/mm/yyyy] -> VD: PHAT DO XE SAI 25/10/2025
    private static final String REGEX_PHAT = "^PHAT .+ (0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$";

    // 4. Khác/Đóng góp: DONG GOP [Tên quỹ] [mm/yyyy] -> VD: DONG GOP VACXIN 10/2025
    private static final String REGEX_KHAC = "^DONG GOP .+ (0[1-9]|1[0-2])/\\d{4}$";

    // =========================================================================
    // 2. HELPER METHODS (XỬ LÝ CHUỖI & CHECK TRÙNG)
    // =========================================================================

    private String normalizeString(String input) {
        if (input == null) return "";
        // Chuyển về chữ hoa, bỏ dấu tiếng Việt, bỏ khoảng trắng thừa
        String nfdNormalizedString = Normalizer.normalize(input, Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(nfdNormalizedString).replaceAll("");
        return noAccent.toUpperCase().trim().replaceAll("\\s+", " "); 
    }

    /**
     * Kiểm tra cú pháp ghi chú (Strict Validation)
     */
    @SuppressWarnings("Unused")
    private void validateGhiChuSyntax(InvoiceType type, String ghiChu) {
        if (ghiChu == null || ghiChu.trim().isEmpty()) {
            throw new IllegalArgumentException("Ghi chú không được để trống.");
        }

        String noteNorm = normalizeString(ghiChu); 
        boolean isValid = false;
        String mauChuan = "";

        if (null == type) { // InvoiceType.khac -> Coi là Đóng góp
            isValid = noteNorm.matches(REGEX_KHAC);
            mauChuan = "DONG GOP [TÊN QUỸ] mm/yyyy";
        } 
        else switch (type) {
            case dich_vu -> {
                isValid = noteNorm.matches(REGEX_DICH_VU);
                mauChuan = "TÊN_DV mm/yyyy (VD: DIEN 10/2025)";
            }
            case sua_chua -> {
                isValid = noteNorm.matches(REGEX_SUA_CHUA);
                mauChuan = "SUA CHUA [NỘI DUNG] dd/mm/yyyy";
            }
            case phat -> {
                isValid = noteNorm.matches(REGEX_PHAT);
                mauChuan = "PHAT [LÝ DO] dd/mm/yyyy";
            }
            default -> {
                // InvoiceType.khac -> Coi là Đóng góp
                isValid = noteNorm.matches(REGEX_KHAC);
                mauChuan = "DONG GOP [TÊN QUỸ] mm/yyyy";
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException(
                "Sai cú pháp ghi chú! Loại '" + type + "' yêu cầu chuẩn: " + mauChuan + 
                ". (Giá trị nhập: " + ghiChu + ")"
            );
        }
    }

    /**
     * Check trùng lặp dựa trên chuỗi chính xác 100%
     */
    private void checkDuplicateStrict(HoGiaDinh hgd, InvoiceType type, String ghiChuMoi) {
        String noteMoiNorm = normalizeString(ghiChuMoi);
        List<HoaDon> existingInvoices = hoaDonDAO.findByHoGiaDinh(hgd);

        for (HoaDon hd : existingInvoices) {
            if (hd.getLoaiHoaDon() == type) { // Chỉ so sánh cùng loại
                String noteCuNorm = normalizeString(hd.getGhiChu());
                // So sánh chuỗi tuyệt đối
                if (noteMoiNorm.equals(noteCuNorm)) {
                    throw new IllegalArgumentException(
                        "TRÙNG LẶP: Đã tồn tại hóa đơn trong hệ thống: \"" + hd.getGhiChu() + "\""
                    );
                }
            }
        }
    }

    // =========================================================================
    // 3. CRUD OPERATIONS (SAVE - UPDATE - DELETE)
    // =========================================================================

    @Transactional
    public HoaDon saveOrUpdateHoaDon(HoaDon hoaDon, String maHo, String nguoiDangKyCccd, DoiTuong nguoiThucHien, boolean isPhieuChi) {
    
        final boolean isNewInvoice = hoaDon.getMaHoaDon() == null;
    
        if (isPhieuChi) {
            hoaDon.setNguoiDangKyDichVu(nguoiThucHien);
            hoaDon.setHoGiaDinh(null); 
            if (isNewInvoice) {
                hoaDon.setTrangThai(InvoiceStatus.da_thanh_toan); 
                hoaDon.setNgayThanhToan(LocalDateTime.now());
            }
        } else {
            // PHIẾU THU
            if (maHo == null || maHo.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn Hộ gia đình.");
            
            HoGiaDinh hoGiaDinh = hoGiaDinhService.getHouseholdById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Mã hộ không tồn tại"));
            hoaDon.setHoGiaDinh(hoGiaDinh);

            // [VALIDATE & CHECK TRÙNG] -> Chỉ khi tạo mới
            if (isNewInvoice) {
                validateGhiChuSyntax(hoaDon.getLoaiHoaDon(), hoaDon.getGhiChu());
                checkDuplicateStrict(hoGiaDinh, hoaDon.getLoaiHoaDon(), hoaDon.getGhiChu());
            }

            if (nguoiDangKyCccd != null && !nguoiDangKyCccd.isEmpty()) {
                DoiTuong nguoiDangKy = doiTuongDAO.findByCccd(nguoiDangKyCccd)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân"));
                boolean isMember = hoGiaDinh.getThanhVienHoList().stream()
                    .anyMatch(tvh -> tvh.getDoiTuong().getCccd().equals(nguoiDangKyCccd));
                if (!isMember) throw new IllegalArgumentException("Người được chọn không thuộc hộ gia đình này!");
                hoaDon.setNguoiDangKyDichVu(nguoiDangKy);
            }
            
            if (!isNewInvoice) {
                HoaDon hdOriginal = hoaDonDAO.findById(hoaDon.getMaHoaDon()).orElseThrow();
                if (hoaDon.getNguoiThanhToan() == null) hoaDon.setNguoiThanhToan(hdOriginal.getNguoiThanhToan());
                if (nguoiDangKyCccd == null || nguoiDangKyCccd.isEmpty()) hoaDon.setNguoiDangKyDichVu(hdOriginal.getNguoiDangKyDichVu());
            }
        }

        if (isNewInvoice) {
            if (hoaDon.getNgayTao() == null) hoaDon.setNgayTao(LocalDateTime.now());
            if (hoaDon.getTrangThai() == null) hoaDon.setTrangThai(InvoiceStatus.chua_thanh_toan);
        }
    
        return hoaDonDAO.save(hoaDon);
    }
    
    // Wrapper để tương thích ngược
    @Transactional
    public HoaDon saveOrUpdateHoaDon(HoaDon hoaDon, String maHo, String nguoiDangKyCccd, DoiTuong nguoiThucHien) {
        return saveOrUpdateHoaDon(hoaDon, maHo, nguoiDangKyCccd, nguoiThucHien, false); 
    }

    @Transactional
    public void deleteHoaDon(Integer maHoaDon) {
        HoaDon hd = hoaDonDAO.findById(maHoaDon)
                             .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn cần xóa."));
        hoaDonDAO.delete(hd); 
    }

    public List<HoaDon> getAllHoaDon() { return hoaDonDAO.findAllWithHoGiaDinh(); }
    
    public Optional<HoaDon> getHoaDonById(Integer maHoaDon) {
         return hoaDonDAO.findAllWithHoGiaDinh().stream().filter(h -> h.getMaHoaDon().equals(maHoaDon)).findFirst();
    }

    // =========================================================================
    // 4. IMPORT EXCEL
    // =========================================================================
    
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
                if (rowNumber == 0) { rowNumber++; continue; }
                if (currentRow.getCell(0) == null || getCellValueAsString(currentRow.getCell(0)).isEmpty()) continue;

                try {
                    String maHo = getCellValueAsString(currentRow.getCell(0));
                    String cccdThanhVien = getCellValueAsString(currentRow.getCell(1));
                    String loaiPhiStr = getCellValueAsString(currentRow.getCell(2));
                    String ghiChu = getCellValueAsString(currentRow.getCell(5)); // Ghi chú từ Excel (Phải đúng chuẩn)
                    
                    BigDecimal soTien = BigDecimal.ZERO;
                    Cell amountCell = currentRow.getCell(3);
                    if (amountCell != null && amountCell.getCellType() == CellType.NUMERIC) {
                        soTien = BigDecimal.valueOf(amountCell.getNumericCellValue());
                    }

                    LocalDate hanThanhToan = LocalDate.now().plusDays(15);
                    Cell dateCell = currentRow.getCell(4);
                    if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                        hanThanhToan = dateCell.getLocalDateTimeCellValue().toLocalDate();
                    }

                    if (maHo.isEmpty()) throw new IllegalArgumentException("Mã hộ trống");
                    if (soTien.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền phải > 0");

                    InvoiceType loaiHoaDon = mapToInvoiceType(loaiPhiStr);
                    HoaDon hoaDon = new HoaDon();
                    hoaDon.setLoaiHoaDon(loaiHoaDon);
                    hoaDon.setSoTien(soTien);
                    hoaDon.setHanThanhToan(hanThanhToan);
                    hoaDon.setGhiChu(ghiChu); 
                    
                    // GỌI HÀM SAVE CHUNG -> Tự động Validate cú pháp & Check trùng
                    saveOrUpdateHoaDon(hoaDon, maHo, cccdThanhVien, nguoiThucHien, false);

                    successCount++;

                } catch (IllegalArgumentException e) {
                    errorCount++;
                    errorLog.append("Dòng ").append(rowNumber + 1).append(": ").append(e.getMessage()).append("\n");
                } catch (Exception e) {
                    errorCount++;
                    errorLog.append("Dòng ").append(rowNumber + 1).append(": Lỗi - ").append(e.getMessage()).append("\n");
                }
                rowNumber++;
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("Kết quả Import: Thành công ").append(successCount);
        if (errorCount > 0) {
            result.append(", Thất bại ").append(errorCount).append(" dòng.\n");
            result.append("Chi tiết lỗi:\n").append(errorLog.toString());
        }
        return result.toString();
    }

    // =========================================================================
    // 5. CÁC HÀM CŨ & THỐNG KÊ (ĐÃ KHÔI PHỤC ĐẦY ĐỦ)
    // =========================================================================

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING -> {
                    return cell.getStringCellValue().trim();
                }
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    long longVal = (long) cell.getNumericCellValue();
                    if (cell.getNumericCellValue() == longVal) return String.valueOf(longVal);
                    return String.valueOf(cell.getNumericCellValue());
                }
                case BOOLEAN -> {
                    return String.valueOf(cell.getBooleanCellValue());
                }
                case FORMULA -> {
                    // Thử lấy giá trị String trước, nếu lỗi thì lấy Numeric
                    try { return cell.getStringCellValue(); } catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
                }
                default -> {
                    return "";
                }
            }
        } catch (Exception e) { return ""; }
    }

    private InvoiceType mapToInvoiceType(String text) {
        if (text == null) return InvoiceType.khac;
        String normalized = normalizeString(text);
        if (normalized.contains("DICH VU") || normalized.contains("DIEN") || normalized.contains("NUOC") || normalized.contains("GUI XE") || normalized.contains("VE SINH")) return InvoiceType.dich_vu;
        if (normalized.contains("SUA CHUA")) return InvoiceType.sua_chua;
        if (normalized.contains("PHAT")) return InvoiceType.phat;
        return InvoiceType.khac;
    }

    public HoaDonStatsDTO getHoaDonStats(HoGiaDinh hoGiaDinh) {
        HoaDonStatsDTO stats = new HoaDonStatsDTO();
        if (hoGiaDinh == null) return stats;
        List<HoaDon> invoices = getAllHoaDonByHo(hoGiaDinh); 
        List<HoaDon> pendingInvoices = invoices.stream()
            .filter(hd -> hd.getTrangThai() == InvoiceStatus.chua_thanh_toan || hd.getTrangThai() == InvoiceStatus.qua_han)
            .collect(Collectors.toList());
        BigDecimal tongChuaThanhToan = pendingInvoices.stream().map(HoaDon::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        int soLuong = pendingInvoices.size();
        stats.tongChuaThanhToan = tongChuaThanhToan;
        stats.tongHoaDonChuaThanhToan = soLuong;
        stats.trangThai = soLuong > 0 ? soLuong + " hóa đơn chưa thanh toán" : "Đã thanh toán hết";
        return stats;
    }

    public List<HoaDon> getRecentHoaDon(HoGiaDinh hoGiaDinh, int limit) {
        if (hoGiaDinh == null) return Collections.emptyList();
        List<HoaDon> list = hoaDonDAO.findByHoGiaDinh(hoGiaDinh);
        list.sort(Comparator.comparing(HoaDon::getNgayTao, Comparator.reverseOrder()));
        return list.size() > limit ? list.subList(0, limit) : list;
    }
    
    public List<HoaDon> getHoaDonCanXacNhan(InvoiceStatus trangThai, int limit) {
        List<HoaDon> list = hoaDonDAO.findByTrangThaiWithChuHo(trangThai);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    public HoaDonStatsDTO getAccountantStats() {   
        HoaDonStatsDTO stats = new HoaDonStatsDTO();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(23, 59, 59);
        BigDecimal tongThu = hoaDonDAO.sumTongThuByDateRange(InvoiceStatus.da_thanh_toan, startOfMonth, endOfMonth);
        BigDecimal tongChi = hoaDonDAO.sumTongChiByDateRange(InvoiceStatus.da_thanh_toan, startOfMonth, endOfMonth);
        BigDecimal tongChuaThu = hoaDonDAO.sumSoTienByTrangThai(InvoiceStatus.chua_thanh_toan);
        List<HoaDon> pendingInvoices = hoaDonDAO.findByTrangThai(InvoiceStatus.chua_thanh_toan);
        BigDecimal tongQuaHan = pendingInvoices.stream()
            .filter(hd -> hd.getHanThanhToan() != null && hd.getHanThanhToan().isBefore(now.toLocalDate()))
            .map(HoaDon::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.tongThuThangNay = tongThu; 
        stats.tongChiThangNay = tongChi; 
        stats.loiNhuanRong = tongThu.subtract(tongChi);
        stats.tongChuaThanhToan = tongChuaThu;
        stats.tongQuaHan = tongQuaHan;
        return stats;
    }

    public Map<String, BigDecimal> getMonthlyRevenueStats() {
        Map<String, BigDecimal> stats = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = today.minusMonths(i);
            LocalDateTime start = date.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59);
            BigDecimal revenue = hoaDonDAO.sumTongThuByDateRange(InvoiceStatus.da_thanh_toan, start, end);
            String key = "Tháng " + date.getMonthValue() + "/" + date.getYear();
            stats.put(key, revenue);
        }
        return stats;
    }

    public List<HoaDon> getTransactionHistory() {
        List<HoaDon> list = hoaDonDAO.findByTrangThai(InvoiceStatus.da_thanh_toan);
        list.sort((h1, h2) -> {
            LocalDateTime d1 = h1.getNgayThanhToan();
            LocalDateTime d2 = h2.getNgayThanhToan();
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });
        return list;
    }

    public List<HoaDon> getAllHoaDonByHo(HoGiaDinh hoGiaDinh) {
        if (hoGiaDinh == null) return Collections.emptyList();
        return hoaDonDAO.findByHoGiaDinh(hoGiaDinh);
    }
    
    public Optional<HoaDon> getHoaDonByIdAndHo(Integer maHoaDon, HoGiaDinh hoGiaDinh) {
        return hoaDonDAO.findByHoGiaDinh(hoGiaDinh).stream()
            .filter(hd -> hd.getMaHoaDon().equals(maHoaDon))
            .findFirst();
    }

    @Transactional 
    public void markAsPaidByResident(Integer maHoaDon, DoiTuong nguoiThanhToan) {
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        if (hd.getTrangThai() == InvoiceStatus.da_thanh_toan) throw new IllegalArgumentException("Hóa đơn #" + maHoaDon + " đã được thanh toán.");
        if (hd.getTrangThai() == InvoiceStatus.cho_xac_nhan) throw new IllegalArgumentException("Hóa đơn đang chờ xác nhận.");
        hd.setTrangThai(InvoiceStatus.cho_xac_nhan);
        hd.setNguoiThanhToan(nguoiThanhToan); 
    }

    public List<HoaDon> getHoaDonChoXacNhan(int limit) {
        List<HoaDon> list = hoaDonDAO.findByTrangThaiWithChuHo(InvoiceStatus.cho_xac_nhan);
        list.sort(Comparator.comparing(HoaDon::getNgayTao, Comparator.reverseOrder()));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @Transactional
    public void confirmPayment(Integer maHoaDon, DoiTuong nguoiXacNhan) {
        // 1. Tìm hóa đơn (Nên dùng findById cho nhanh thay vì stream cả list)
        HoaDon hd = hoaDonDAO.findById(maHoaDon)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
            
        // 2. Kiểm tra hợp lệ
        if (hd.getTrangThai() == InvoiceStatus.da_thanh_toan) {
            throw new IllegalArgumentException("Hóa đơn đã được thanh toán trước đó.");
        }
        
        // 3. Cập nhật thông tin
        hd.setTrangThai(InvoiceStatus.da_thanh_toan);
        hd.setNgayThanhToan(LocalDateTime.now());
        
        // [QUAN TRỌNG] Lưu lại thay đổi xuống DB
        hoaDonDAO.save(hd); 
    }
    public List<HoaDon> getAllPaidHoaDon() { return hoaDonDAO.findByTrangThai(InvoiceStatus.da_thanh_toan); }

    @Transactional
    public void rejectPayment(Integer maHoaDon, DoiTuong nguoiThucHien) {
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        if (hd.getTrangThai() != InvoiceStatus.cho_xac_nhan) throw new IllegalArgumentException("Chỉ có thể từ chối hóa đơn đang chờ xác nhận.");
        hd.setTrangThai(InvoiceStatus.chua_thanh_toan);
        hd.setNgayThanhToan(null);
        hd.setNguoiThanhToan(null); 
        hoaDonDAO.save(hd);
    }

    @Transactional
    public void xuLyThanhToanThanhCong(Integer maHoaDon, DoiTuong nguoiThanhToan, String maGiaoDichNganHang) {
        HoaDon hd = hoaDonDAO.findAllWithHoGiaDinh().stream()
            .filter(h -> h.getMaHoaDon().equals(maHoaDon)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        if (hd.getTrangThai() == InvoiceStatus.da_thanh_toan) return; 
        hd.setTrangThai(InvoiceStatus.da_thanh_toan);
        hd.setNgayThanhToan(LocalDateTime.now());
        hd.setNguoiThanhToan(nguoiThanhToan);
        String ghiChuHienTai = (hd.getGhiChu() == null) ? "" : hd.getGhiChu();
        String noiDungGiaoDich = " | Thanh toán Online qua VNPay. Mã GD: " + maGiaoDichNganHang;
        if (ghiChuHienTai.length() + noiDungGiaoDich.length() < 255) hd.setGhiChu(ghiChuHienTai + noiDungGiaoDich);
        else hd.setGhiChu(noiDungGiaoDich); 
        hoaDonDAO.save(hd);
    }

    @Transactional
    public int confirmMultiplePayments(List<Integer> maHoaDonList) {
        int successCount = 0;
        for (Integer maHoaDon : maHoaDonList) {
            try {
                HoaDon hd = hoaDonDAO.findById(maHoaDon).orElseThrow(() -> new IllegalArgumentException("Lỗi ID"));
                if (hd.getTrangThai() == InvoiceStatus.cho_xac_nhan) {
                    hd.setTrangThai(InvoiceStatus.da_thanh_toan);
                    hd.setNgayThanhToan(LocalDateTime.now());
                    hoaDonDAO.save(hd);
                    successCount++;
                }
            } catch (Exception e) {}
        }
        return successCount;
    }

    @Transactional
    public int rejectMultiplePayments(List<Integer> maHoaDonList, DoiTuong nguoiXacNhan) {
        int successCount = 0;
        for (Integer maHoaDon : maHoaDonList) {
            try {
                HoaDon hd = hoaDonDAO.findById(maHoaDon).orElseThrow(() -> new IllegalArgumentException("Lỗi ID"));
                if (hd.getTrangThai() == InvoiceStatus.cho_xac_nhan) {
                    if (hd.getHanThanhToan() != null && hd.getHanThanhToan().isBefore(LocalDate.now())) {
                        hd.setTrangThai(InvoiceStatus.qua_han);
                    } else {
                        hd.setTrangThai(InvoiceStatus.chua_thanh_toan);
                    }
                    hd.setNguoiThanhToan(null);
                    hoaDonDAO.save(hd);
                    successCount++;
                }
            } catch (Exception e) {}
        }
        return successCount;
    }
    // =========================================================================
    // 6. TẠO HÓA ĐƠN HÀNG LOẠT (BATCH CREATE)
    // =========================================================================
    
    @Transactional
    public int taoHoaDonHangLoat(String noiDung, BigDecimal soTien, LocalDate hanThanhToan, InvoiceType loaiHoaDon, String phamVi) { // Thêm người tạo nếu cần log
        
        List<HoaDon> danhSachHoaDonMoi = new ArrayList<>();
        String thangNam = LocalDate.now().getMonthValue() + "/" + LocalDate.now().getYear();
        
        // Chuẩn hóa nội dung
        String noiDungUpper = normalizeString(noiDung);
        String ghiChuChuan = (loaiHoaDon == InvoiceType.khac) 
                             ? "DONG GOP " + noiDungUpper + " " + thangNam 
                             : noiDungUpper + " " + thangNam;

        // ---------------------------------------------------------
        // TRƯỜNG HỢP 1: ÁP DỤNG CHO TẤT CẢ CƯ DÂN (THEO ĐẦU NGƯỜI)
        // ---------------------------------------------------------
        if ("INDIVIDUAL".equals(phamVi)) {
            // Lấy tất cả thành viên đang ở (Active)
            List<ThanhVienHo> tatCaThanhVien = thanhVienHoService.getAllActiveMembers();
            
            for (ThanhVienHo tv : tatCaThanhVien) {
                try {
                    // [BỔ SUNG] CHECK TRÙNG LẶP CHO CÁ NHÂN
                    // Kiểm tra xem trong Hộ của người này, đã có hóa đơn nào:
                    // 1. Cùng nội dung ghi chú (ghiChuChuan + Tên)
                    // 2. Cùng số tiền
                    // 3. Người đứng tên chính là người này
                    
                    String ghiChuCaNhan = ghiChuChuan + " (" + tv.getDoiTuong().getHoVaTen() + ")";
                    
                    List<HoaDon> hoaDonCuaHo = hoaDonDAO.findByHoGiaDinh(tv.getHoGiaDinh());
                    boolean daTonTai = hoaDonCuaHo.stream().anyMatch(hd -> 
                        normalizeString(hd.getGhiChu()).equals(normalizeString(ghiChuCaNhan))
                        && hd.getLoaiHoaDon() == loaiHoaDon
                        && hd.getNguoiDangKyDichVu() != null 
                        && hd.getNguoiDangKyDichVu().getCccd().equals(tv.getDoiTuong().getCccd())
                    );

                    if (daTonTai) continue; // Bỏ qua nếu đã có

                    // Tạo hóa đơn
                    HoaDon hd = new HoaDon();
                    hd.setHoGiaDinh(tv.getHoGiaDinh());
                    hd.setLoaiHoaDon(loaiHoaDon);
                    hd.setSoTien(soTien);
                    hd.setHanThanhToan(hanThanhToan);
                    hd.setGhiChu(ghiChuCaNhan); // Ghi chú có tên riêng
                    hd.setNgayTao(LocalDateTime.now());
                    hd.setTrangThai(InvoiceStatus.chua_thanh_toan);
                    hd.setNguoiDangKyDichVu(tv.getDoiTuong()); // Gán cho cư dân cụ thể

                    danhSachHoaDonMoi.add(hd);
                } catch (Exception e) {
                    System.err.println("Lỗi tạo cho cư dân " + tv.getDoiTuong().getCccd());
                }
            }

        } 
        // ---------------------------------------------------------
        // TRƯỜNG HỢP 2: ÁP DỤNG THEO HỘ (CHỦ HỘ ĐẠI DIỆN)
        // ---------------------------------------------------------
        else {
            List<HoGiaDinh> danhSachHo = hoGiaDinhService.getAllActiveHouseholds();
            
            for (HoGiaDinh ho : danhSachHo) {
                try {
                    // Check trùng cho Hộ (Logic cũ)
                    List<HoaDon> hoaDonCuaHo = hoaDonDAO.findByHoGiaDinh(ho);
                    boolean daTonTai = hoaDonCuaHo.stream().anyMatch(hd -> 
                        normalizeString(hd.getGhiChu()).equals(normalizeString(ghiChuChuan)) 
                        && hd.getLoaiHoaDon() == loaiHoaDon
                        && hd.getSoTien().compareTo(soTien) == 0
                    );
                    
                    if (daTonTai) continue;

                    DoiTuong chuHo = null;
                    if (ho.getThanhVienHoList() != null) {
                        chuHo = ho.getThanhVienHoList().stream()
                            .filter(tv -> Boolean.TRUE.equals(tv.getLaChuHo()) && tv.getNgayKetThuc() == null)
                            .map(ThanhVienHo::getDoiTuong).findFirst().orElse(null);
                    }

                    HoaDon hd = new HoaDon();
                    hd.setHoGiaDinh(ho);
                    hd.setLoaiHoaDon(loaiHoaDon);
                    hd.setSoTien(soTien);
                    hd.setHanThanhToan(hanThanhToan);
                    hd.setGhiChu(ghiChuChuan);
                    hd.setNgayTao(LocalDateTime.now());
                    hd.setTrangThai(InvoiceStatus.chua_thanh_toan);
                    hd.setNguoiDangKyDichVu(chuHo); 

                    danhSachHoaDonMoi.add(hd);
                } catch (Exception e) {
                    System.err.println("Lỗi tạo cho hộ " + ho.getMaHo());
                }
            }
        }

        // Lưu tất cả
        if (!danhSachHoaDonMoi.isEmpty()) {
            hoaDonDAO.saveAll(danhSachHoaDonMoi);
        }
        
        return danhSachHoaDonMoi.size();
    }
}