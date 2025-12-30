package BlueMoon.bluemoon.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    // CHUẨN HÓA CHUỖI (ĐỒNG BỘ VỚI FRONTEND)
    // =========================================================================
    
    private String normalizeString(String input) {
        if (input == null) return "";
        
        String str = input.replace('đ', 'd').replace('Đ', 'D');
        String nfdNormalizedString = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(nfdNormalizedString).replaceAll("");
        
        return noAccent.toUpperCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * ⭐ HÀM CHUẨN HÓA NỘI DUNG (DÙNG CHO IMPORT EXCEL)
     * Logic tương tự 100% với sanitizeContent() ở Frontend
     */
    private String sanitizeContent(String rawContent, InvoiceType type) {
        if (rawContent == null || rawContent.isEmpty()) return "";
        
        // Bước 1: Chuẩn hóa cơ bản
        String content = normalizeString(rawContent);
        
        // Bước 2: Xóa từ khóa rác
        content = content.replace("TIEN", "")
                         .replace("PHI", "")
                         .replace("THANH TOAN", "")
                         .replace("DICH VU", "");
        
        // Bước 3: Xóa mô tả thời gian trong nội dung
        content = content.replaceAll("THANG\\s*\\d{1,2}", "")
                         .replaceAll("NAM\\s*\\d{4}", "")
                         .replaceAll("\\sT\\d{1,2}\\s", " ")
                         .replaceAll("\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}", "")
                         .replaceAll("\\d{1,2}[/-]\\d{4}", "");
        
        // Bước 4: Xóa tiền tố theo loại
        if (type != null) {
            switch (type) {
                case sua_chua -> content = content.replace("SUA CHUA", "").replace("SUA", "");
                case phat -> content = content.replace("PHAT", "").replace("VI PHAM", "");
                case khac -> content = content.replace("DONG GOP", "")
                                              .replace("UNG HO", "")
                                              .replace("QUY", "");
                default -> {}
            }
        }
        
        // Bước 5: Dọn dẹp cuối cùng
        content = content.replace("-", "").trim().replaceAll("\\s+", " ");
        
        return content;
    }

    /**
     * ⭐ TẠO GHI CHÚ CHUẨN (DÙNG CHO IMPORT EXCEL)
     * Ghép chuỗi cuối cùng giống Frontend
     */
private String taoGhiChuChuanTuExcel(String rawContent, InvoiceType type, String kyThu, String ngay) {
    if (rawContent == null) return "";

    // 1. TỰ CHIẾT XUẤT NGÀY THÁNG (Nếu tham số 'ngay' hoặc 'kyThu' bị trống - do Excel thiếu cột)
    String extractedDate = "";
    if ((ngay == null || ngay.isEmpty()) && (kyThu == null || kyThu.isEmpty())) {
        // Tìm dd/mm/yyyy hoặc mm/yyyy trong xâu thô
        java.util.regex.Matcher m = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2}/\\d{4})").matcher(rawContent);
        if (m.find()) {
            extractedDate = m.group(1);
        }
    } else {
        // Nếu có tham số truyền vào (từ cột riêng), ưu tiên dùng nó
        extractedDate = (type == InvoiceType.sua_chua || type == InvoiceType.phat) ? formatDateVN(ngay) : convertKyThuToISO(kyThu);
    }

    // 2. LÀM SẠCH NỘI DUNG (Lúc này sanitizeContent sẽ xóa ngày trong xâu, nhưng ta đã lưu vào extractedDate ở trên)
    String cleanContent = sanitizeContent(rawContent, type);
    
    // 3. GHÉP LẠI THEO CHUẨN
    String prefix = "";
    if (null != type) switch (type) {
            case sua_chua -> prefix = "SUA CHUA ";
            case phat -> prefix = "PHAT ";
            case khac -> prefix = "DONG GOP ";
            default -> {
        }
        }

    // Trả về: Tiền tố + Nội dung lõi + Ngày tháng đã cứu lại được
    return (prefix + cleanContent + " " + extractedDate).trim().replaceAll("\\s+", " ");
}

    private String formatDateVN(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return String.format("%02d/%02d/%d", 
                date.getDayOfMonth(), 
                date.getMonthValue(), 
                date.getYear()
            );
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * ⭐ CHECK TRÙNG LẶP (SO SÁNH CHUỖI ĐÃ CHUẨN HÓA)
     */
    private void checkDuplicateInvoice(HoGiaDinh hoGiaDinh, InvoiceType type, String ghiChuMoi) {
        if (hoGiaDinh == null) return;
        
        String ghiChuMoiNorm = normalizeString(ghiChuMoi);
        List<HoaDon> existingInvoices = hoaDonDAO.findByHoGiaDinh(hoGiaDinh);
        
        for (HoaDon hd : existingInvoices) {
            if (hd.getLoaiHoaDon() == type) {
                String ghiChuCuNorm = normalizeString(hd.getGhiChu());
                
                if (ghiChuMoiNorm.equals(ghiChuCuNorm)) {
                    throw new IllegalArgumentException(
                        "TRÙNG LẶP: Đã tồn tại hóa đơn \"" + hd.getGhiChu() + "\""
                    );
                }
            }
        }
    }

    // =========================================================================
    // 3. CRUD OPERATIONS (SAVE - UPDATE - DELETE)
    // =========================================================================

    // =========================================================================
    // SAVE/UPDATE HÓA ĐƠN (NHẬN TỪ FRONTEND - ĐÃ CHUẨN HÓA SẴN)
    // =========================================================================
    
    @Transactional
    public HoaDon saveOrUpdateHoaDon(
        HoaDon hoaDon, 
        String maHo, 
        String nguoiDangKyCccd, 
        DoiTuong nguoiThucHien, 
        boolean isPhieuChi
    ) {
        final boolean isNewInvoice = hoaDon.getMaHoaDon() == null;
        
        if (isPhieuChi) {
            // Phiếu Chi
            hoaDon.setNguoiDangKyDichVu(nguoiThucHien);
            hoaDon.setHoGiaDinh(null);
            
            if (isNewInvoice) {
                hoaDon.setTrangThai(InvoiceStatus.da_thanh_toan);
                hoaDon.setNgayThanhToan(LocalDateTime.now());
            }
            
        } else {
            // Phiếu Thu
            if (maHo == null || maHo.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn Hộ gia đình.");
            }
            
            HoGiaDinh hoGiaDinh = hoGiaDinhService.getHouseholdById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Mã hộ không tồn tại"));
            
            hoaDon.setHoGiaDinh(hoGiaDinh);
            
            // ⭐ CHECK TRÙNG CHỈ KHI TẠO MỚI
            // Frontend đã gửi chuỗi chuẩn rồi -> Chỉ cần check trùng
            if (isNewInvoice) {
                checkDuplicateInvoice(hoGiaDinh, hoaDon.getLoaiHoaDon(), hoaDon.getGhiChu());
            }
            
            // Xử lý người đăng ký
            if (nguoiDangKyCccd != null && !nguoiDangKyCccd.isEmpty()) {
                DoiTuong nguoiDangKy = doiTuongDAO.findByCccd(nguoiDangKyCccd)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân"));
                
                boolean isMember = hoGiaDinh.getThanhVienHoList().stream()
                    .anyMatch(tvh -> tvh.getDoiTuong().getCccd().equals(nguoiDangKyCccd));
                
                if (!isMember) {
                    throw new IllegalArgumentException(
                        "Người được chọn không thuộc hộ gia đình này!"
                    );
                }
                
                hoaDon.setNguoiDangKyDichVu(nguoiDangKy);
            }
            
            // Giữ lại thông tin cũ khi update
            if (!isNewInvoice) {
                HoaDon hdOriginal = hoaDonDAO.findById(hoaDon.getMaHoaDon())
                    .orElseThrow();
                
                if (hoaDon.getNguoiThanhToan() == null) {
                    hoaDon.setNguoiThanhToan(hdOriginal.getNguoiThanhToan());
                }
                
                if (nguoiDangKyCccd == null || nguoiDangKyCccd.isEmpty()) {
                    hoaDon.setNguoiDangKyDichVu(hdOriginal.getNguoiDangKyDichVu());
                }
            }
        }
        
        // Set thông tin mặc định cho hóa đơn mới
        if (isNewInvoice) {
            if (hoaDon.getNgayTao() == null) {
                hoaDon.setNgayTao(LocalDateTime.now());
            }
            if (hoaDon.getTrangThai() == null) {
                hoaDon.setTrangThai(InvoiceStatus.chua_thanh_toan);
            }
        }
        
        return hoaDonDAO.save(hoaDon);
    }

    // =========================================================================
    // IMPORT EXCEL (CHUẨN HÓA TẠI BACKEND)
    // =========================================================================
    
    @Transactional
    public String importHoaDonFromExcel(
        org.springframework.web.multipart.MultipartFile file, 
        DoiTuong nguoiThucHien
    ) throws java.io.IOException {
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorLog = new StringBuilder();
        
        try (org.apache.poi.ss.usermodel.Workbook workbook = 
                new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            java.util.Iterator<org.apache.poi.ss.usermodel.Row> rows = sheet.iterator();
            int rowNumber = 0;
            
            while (rows.hasNext()) {
                org.apache.poi.ss.usermodel.Row currentRow = rows.next();
                
                // Bỏ qua header
                if (rowNumber == 0) { 
                    rowNumber++; 
                    continue; 
                }
                
                // Bỏ qua dòng trống
                if (currentRow.getCell(0) == null || 
                    getCellValueAsString(currentRow.getCell(0)).isEmpty()) {
                    continue;
                }
                
                try {
                    // Đọc dữ liệu từ Excel
                    String maHo = getCellValueAsString(currentRow.getCell(0));
                    String cccdThanhVien = getCellValueAsString(currentRow.getCell(1));
                    String loaiPhiStr = getCellValueAsString(currentRow.getCell(2));
                    String noiDungExcel = getCellValueAsString(currentRow.getCell(5)); // Nội dung thô
                    
                    // ⭐ QUAN TRỌNG: Giả sử Excel có thêm 2 cột:
                    // - Cột 6: Kỳ thu (mm/yyyy) hoặc để trống
                    // - Cột 7: Ngày (dd/mm/yyyy) hoặc để trống
                    String kyThuExcel = getCellValueAsString(currentRow.getCell(6)); // VD: "12/2025"
                    String ngayExcel = getCellValueAsString(currentRow.getCell(7));   // VD: "25/12/2025"
                    
                    // Chuyển đổi định dạng về ISO
                    String kyThuISO = convertKyThuToISO(kyThuExcel);   // "12/2025" -> "2025-12"
                    String ngayISO = convertNgayToISO(ngayExcel);      // "25/12/2025" -> "2025-12-25"
                    
                    BigDecimal soTien = BigDecimal.ZERO;
                    org.apache.poi.ss.usermodel.Cell amountCell = currentRow.getCell(3);
                    if (amountCell != null && 
                        amountCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        soTien = BigDecimal.valueOf(amountCell.getNumericCellValue());
                    }
                    
                    LocalDate hanThanhToan = LocalDate.now().plusDays(15);
                    org.apache.poi.ss.usermodel.Cell dateCell = currentRow.getCell(4);
                    if (dateCell != null && 
                        dateCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && 
                        org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(dateCell)) {
                        hanThanhToan = dateCell.getLocalDateTimeCellValue().toLocalDate();
                    }
                    
                    // Validate cơ bản
                    if (maHo.isEmpty()) {
                        throw new IllegalArgumentException("Mã hộ trống");
                    }
                    if (soTien.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Số tiền phải > 0");
                    }
                    
                    InvoiceType loaiHoaDon = mapToInvoiceType(loaiPhiStr);
                    
                    // ⭐ TẠO GHI CHÚ CHUẨN TỪ BACKEND (GIỐNG FRONTEND)
                    String ghiChuChuan = taoGhiChuChuanTuExcel(
                        noiDungExcel, 
                        loaiHoaDon, 
                        kyThuISO, 
                        ngayISO
                    );
                    
                    // Tạo hóa đơn
                    HoaDon hoaDon = new HoaDon();
                    hoaDon.setLoaiHoaDon(loaiHoaDon);
                    hoaDon.setSoTien(soTien);
                    hoaDon.setHanThanhToan(hanThanhToan);
                    hoaDon.setGhiChu(ghiChuChuan); // ⭐ Dùng ghi chú đã chuẩn hóa
                    
                    // Gọi hàm save (Tự động check trùng)
                    saveOrUpdateHoaDon(hoaDon, maHo, cccdThanhVien, nguoiThucHien, false);
                    
                    successCount++;
                    
                } catch (IllegalArgumentException e) {
                    errorCount++;
                    errorLog.append("Dòng ").append(rowNumber + 1)
                           .append(": ").append(e.getMessage()).append("\n");
                } catch (Exception e) {
                    errorCount++;
                    errorLog.append("Dòng ").append(rowNumber + 1)
                           .append(": Lỗi - ").append(e.getMessage()).append("\n");
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
    // HELPER METHODS
    // =========================================================================
    
    private String convertKyThuToISO(String kyThuVN) {
        // "12/2025" -> "2025-12"
        if (kyThuVN == null || kyThuVN.isEmpty()) return "";
        try {
            String[] parts = kyThuVN.split("/");
            if (parts.length == 2) {
                return parts[1] + "-" + String.format("%02d", Integer.valueOf(parts[0]));
            }
        } catch (NumberFormatException e) {}
        return "";
    }
    
    private String convertNgayToISO(String ngayVN) {
        // "25/12/2025" -> "2025-12-25"
        if (ngayVN == null || ngayVN.isEmpty()) return "";
        try {
            String[] parts = ngayVN.split("/");
            if (parts.length == 3) {
                return parts[2] + "-" + 
                       String.format("%02d", Integer.valueOf(parts[1])) + "-" + 
                       String.format("%02d", Integer.valueOf(parts[0]));
            }
        } catch (NumberFormatException e) {}
        return "";
    }
    
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING -> {
                    return cell.getStringCellValue().trim();
                }
                case NUMERIC -> {
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    long longVal = (long) cell.getNumericCellValue();
                    if (cell.getNumericCellValue() == longVal) {
                        return String.valueOf(longVal);
                    }
                    return String.valueOf(cell.getNumericCellValue());
                }
                case BOOLEAN -> {
                    return String.valueOf(cell.getBooleanCellValue());
                }
                case FORMULA -> {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                }
                default -> {
                    return "";
                }
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    private InvoiceType mapToInvoiceType(String text) {
        if (text == null) return InvoiceType.khac;
        String normalized = normalizeString(text);
        
        if (normalized.contains("DICH VU") || normalized.contains("DIEN") || 
            normalized.contains("NUOC") || normalized.contains("GUI XE") || 
            normalized.contains("VE SINH") || normalized.contains("TIEN PHI")||
            normalized.contains("PHI") || normalized.contains("DONG")){
            return InvoiceType.dich_vu;
        }
        if (normalized.contains("SUA CHUA") || normalized.contains("SUA")) {
            return InvoiceType.sua_chua;
        }
        if (normalized.contains("PHAT") || normalized.contains("VI PHAM")) {
            return InvoiceType.phat;
        }
        return InvoiceType.khac;
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
                    
                    String ghiChuCaNhan = ghiChuChuan;
                    
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
    /**
     * [MỚI] Lấy báo cáo chi tiết về các khoản đóng góp tự nguyện
     */
    public Map<String, Object> getChiTietDongGop() {
        Map<String, Object> details = new HashMap<>();
        
        // 1. Tổng quan
        Long tongSoLuotDong = hoaDonDAO.countPaidInvoicesByType(InvoiceType.khac);
        BigDecimal tongTienDongGop = hoaDonDAO.sumSoTienByLoaiAndTrangThai(InvoiceType.khac, InvoiceStatus.da_thanh_toan);
        
        details.put("tongSoLuotDong", tongSoLuotDong);
        details.put("tongTienDongGop", tongTienDongGop);
        
        // 2. Chi tiết từng quỹ
        List<Object[]> fundsRaw = hoaDonDAO.getContributionFundsStats();
        List<Map<String, Object>> funds = new ArrayList<>();
        
        for (Object[] row : fundsRaw) {
            Map<String, Object> f = new HashMap<>();
            f.put("tenQuy", row[0]);      // Ghi chú (Tên đợt)
            f.put("tongTien", row[1]);    // Tổng tiền thu được
            f.put("soNguoiDong", row[2]); // Số người đã đóng
            funds.add(f);
        }
        details.put("danhSachQuy", funds);
        
        return details;
    }
    /**
     * [MỚI] Lấy thống kê tách biệt: Bắt buộc vs Tự nguyện
     */
    public Map<String, BigDecimal> getThongKePhanLoaiThu() {
        Map<String, BigDecimal> stats = new HashMap<>();

        // 1. Định nghĩa nhóm
        List<InvoiceType> batBuocTypes = Arrays.asList(
            InvoiceType.dich_vu, 
            InvoiceType.sua_chua, 
            InvoiceType.phat
        );
        List<InvoiceType> tuNguyenTypes = Arrays.asList(
            InvoiceType.khac
        );

        // 2. Tính toán (Chỉ tính những hóa đơn ĐÃ THANH TOÁN - Doanh thu thực tế)
        BigDecimal tongBatBuoc = hoaDonDAO.sumSoTienByNhieuLoaiVaTrangThai(batBuocTypes, InvoiceStatus.da_thanh_toan);
        BigDecimal tongTuNguyen = hoaDonDAO.sumSoTienByNhieuLoaiVaTrangThai(tuNguyenTypes, InvoiceStatus.da_thanh_toan);

        // 3. Tính toán khoản CHỜ thu (Chưa thanh toán) để tham khảo
        BigDecimal choThuBatBuoc = hoaDonDAO.sumSoTienByNhieuLoaiVaTrangThai(batBuocTypes, InvoiceStatus.chua_thanh_toan);
        
        stats.put("thucThuBatBuoc", tongBatBuoc);
        stats.put("thucThuTuNguyen", tongTuNguyen);
        stats.put("choThuBatBuoc", choThuBatBuoc);

        return stats;
    }
    /**
     * [NÂNG CẤP] HÀM TẠO GHI CHÚ CHUẨN - LỌC SẠCH TỪ KHÓA THỪA
     */
    public String taoGhiChuChuan(String rawInput, InvoiceType type, String kyThuStr, String ngayStr) {
        if (rawInput == null) rawInput = "";
        
        // 1. Chuẩn hóa cơ bản (Bỏ dấu, UpperCase)
        String content = normalizeString(rawInput); 

        // 2. XÓA CÁC TỪ KHÓA "RÁC" (TIỀN, PHÍ, DỊCH VỤ...)
        // Dùng replaceAll với regex để xóa từ đứng riêng lẻ
        content = content.replace("TIEN", "")
                         .replace("PHI", "")
                         .replace("THANH TOAN", "")
                         .replace("DICH VU", "");

        // 3. XÓA CÁC MÔ TẢ THỜI GIAN TRONG NỘI DUNG (Để tránh lặp với ngày hệ thống)
        // VD: "THANG 12", "NAM 2025", "T12", "12/2025"
        content = content.replaceAll("THANG\\s*\\d{1,2}", "")  // Xóa "THANG 12"
                         .replaceAll("NAM\\s*\\d{4}", "")      // Xóa "NAM 2025"
                         .replaceAll("T\\d{1,2}", "")          // Xóa "T12"
                         .replaceAll("\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}", "") // Xóa dd/mm/yyyy
                         .replaceAll("\\d{1,2}[/-]\\d{4}", "");            // Xóa mm/yyyy

        // 4. XÓA TIỀN TỐ THEO LOẠI (Tránh lặp)
        if (type != null) {
            switch (type) {
                case sua_chua -> content = content.replace("SUA CHUA", "").replace("SUA", "");
                case phat -> content = content.replace("PHAT", "").replace("VI PHAM", "");
                case khac -> content = content.replace("DONG GOP", "").replace("UNG HO", "").replace("QUY", "");
                default -> {} 
            }
        }
        
        // 5. Dọn dẹp ký tự thừa còn sót lại (dấu gạch ngang, khoảng trắng kép)
        content = content.replace("-", "").trim().replaceAll("\\s+", " ");

        // 6. GHÉP CHUỖI CHUẨN
        String finalNote = "";
        try {
            if (null != type) switch (type) {
                case dich_vu -> {
                    if (kyThuStr == null || kyThuStr.isEmpty()) throw new IllegalArgumentException("Thiếu kỳ thu");
                    String[] parts = kyThuStr.split("-"); // yyyy-MM
                    // VD: DIEN 12/2025
                    finalNote = content + " " + parts[1] + "/" + parts[0];
                    }
                case khac -> {
                    if (kyThuStr == null || kyThuStr.isEmpty()) throw new IllegalArgumentException("Thiếu thời gian");
                    String[] parts = kyThuStr.split("-");
                    finalNote = "DONG GOP " + content + " " + parts[1] + "/" + parts[0];
                    }
                case sua_chua -> {
                    if (ngayStr == null || ngayStr.isEmpty()) throw new IllegalArgumentException("Thiếu ngày thực hiện");
                    finalNote = "SUA CHUA " + content + " " + formatDateVN(ngayStr);
                }
                case phat -> {
                    if (ngayStr == null || ngayStr.isEmpty()) throw new IllegalArgumentException("Thiếu ngày vi phạm");
                    finalNote = "PHAT " + content + " " + formatDateVN(ngayStr);
                }
                default -> {
                }
            }
        } catch (IllegalArgumentException e) {
            return content; // Fallback
        }
        
        // Trả về kết quả sạch sẽ nhất
        return finalNote.trim().replaceAll("\\s+", " ");
    }
}