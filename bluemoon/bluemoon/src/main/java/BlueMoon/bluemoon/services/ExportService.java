package BlueMoon.bluemoon.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Import cho Excel (Apache POI)

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
// Import cho PDF (iText 7 - Đã đồng bộ với dự án của bạn)

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.LichSuRaVaoDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.LichSuRaVao;
import BlueMoon.bluemoon.models.ApartmentReportDTO;
import BlueMoon.bluemoon.models.HouseholdReportDTO;
import BlueMoon.bluemoon.models.InvoiceReportDTO;
import BlueMoon.bluemoon.models.ResidentReportDTO;
import BlueMoon.bluemoon.utils.EntryExitType;
import BlueMoon.bluemoon.utils.UserRole;

@Service
public class ExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    @Autowired
    private LichSuRaVaoDAO lichSuRaVaoDAO;
    @Autowired
    private DoiTuongDAO doiTuongDAO;

    // ==========================================
    // CÁC HÀM XUẤT EXCEL (GIỮ NGUYÊN)
    // ==========================================

    public byte[] exportApartmentsToExcel(List<ApartmentReportDTO> apartments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Căn Hộ");
            createExcelHeader(sheet, new String[]{
                "Mã Tài Sản", "Tên Tài Sản", "Loại", "Trạng Thái", "Diện Tích (m²)", 
                "Vị Trí", "Giá Trị", "Ngày Thêm", "Mã Hộ", "Tên Hộ", "Chủ Hộ"
            });
            
            int rowNum = 1;
            CellStyle dataStyle = createDataStyle(workbook);
            for (ApartmentReportDTO apt : apartments) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, apt.getMaTaiSan(), dataStyle);
                createCell(row, 1, apt.getTenTaiSan(), dataStyle);
                createCell(row, 2, apt.getLoaiTaiSan(), dataStyle);
                createCell(row, 3, apt.getTrangThai(), dataStyle);
                createCell(row, 4, apt.getDienTich(), dataStyle);
                createCell(row, 5, apt.getViTri(), dataStyle);
                createCell(row, 6, apt.getGiaTri(), dataStyle);
                createCell(row, 7, formatDateTime(apt.getNgayThem()), dataStyle);
                createCell(row, 8, apt.getMaHo(), dataStyle);
                createCell(row, 9, apt.getTenHo(), dataStyle);
                createCell(row, 10, apt.getChuHo(), dataStyle);
            }
            autoSizeColumns(sheet, 11);
            return toByteArray(workbook);
        }
    }

    public byte[] exportEntryExitToExcel(List<LichSuRaVao> logs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lịch Sử Ra Vào");
            createExcelHeader(sheet, new String[]{"ID", "Thời Gian", "Họ Tên", "CCCD", "Hoạt Động", "Cổng"});

            int rowIdx = 1;
            CellStyle dataStyle = createDataStyle(workbook);
            for (LichSuRaVao log : logs) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, log.getId(), dataStyle);
                createCell(row, 1, formatDateTime(log.getThoiGian()), dataStyle);
                createCell(row, 2, log.getCuDan() != null ? log.getCuDan().getHoVaTen() : "N/A", dataStyle);
                createCell(row, 3, log.getCuDan() != null ? log.getCuDan().getCccd() : "N/A", dataStyle);
                createCell(row, 4, log.getLoaiHoatDong(), dataStyle);
                createCell(row, 5, log.getCongKiemSoat(), dataStyle);
            }
            autoSizeColumns(sheet, 6);
            return toByteArray(workbook);
        }
    }

    public byte[] exportInvoicesToExcel(List<InvoiceReportDTO> invoices) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Hóa Đơn");
            createExcelHeader(sheet, new String[]{"Mã HĐ", "Mã Hộ", "Tên Hộ", "Chủ Hộ", "Loại HĐ", "Số Tiền", "Trạng Thái", "Ngày Tạo"});
            
            int rowNum = 1;
            CellStyle dataStyle = createDataStyle(workbook);
            for (InvoiceReportDTO inv : invoices) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, inv.getMaHoaDon(), dataStyle);
                createCell(row, 1, inv.getMaHo(), dataStyle);
                createCell(row, 2, inv.getTenHo(), dataStyle);
                createCell(row, 3, inv.getChuHo(), dataStyle);
                createCell(row, 4, inv.getLoaiHoaDon(), dataStyle);
                createCell(row, 5, inv.getSoTien(), dataStyle);
                createCell(row, 6, inv.getTrangThai(), dataStyle);
                createCell(row, 7, formatDateTime(inv.getNgayTao()), dataStyle);
            }
            autoSizeColumns(sheet, 8);
            return toByteArray(workbook);
        }
    }

    public byte[] exportHouseholdsToExcel(List<HouseholdReportDTO> households) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Hộ Gia Đình");
            createExcelHeader(sheet, new String[]{"Mã Hộ", "Tên Hộ", "Chủ Hộ", "SĐT", "Số TV", "Số CH", "Trạng Thái"});
            
            int rowNum = 1;
            CellStyle dataStyle = createDataStyle(workbook);
            for (HouseholdReportDTO h : households) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, h.getMaHo(), dataStyle);
                createCell(row, 1, h.getTenHo(), dataStyle);
                createCell(row, 2, h.getChuHo(), dataStyle);
                createCell(row, 3, h.getSoDienThoai(), dataStyle);
                createCell(row, 4, h.getSoThanhVien(), dataStyle);
                createCell(row, 5, h.getSoCanHo(), dataStyle);
                createCell(row, 6, h.getTrangThai(), dataStyle);
            }
            autoSizeColumns(sheet, 7);
            return toByteArray(workbook);
        }
    }

    public byte[] exportResidentsToExcel(List<ResidentReportDTO> residents) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Cư Dân");
            createExcelHeader(sheet, new String[]{"CCCD", "Họ Tên", "Giới Tính", "SĐT", "Email", "Mã Hộ", "Vai Trò"});
            
            int rowNum = 1;
            CellStyle dataStyle = createDataStyle(workbook);
            for (ResidentReportDTO r : residents) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, r.getCccd(), dataStyle);
                createCell(row, 1, r.getHoVaTen(), dataStyle);
                createCell(row, 2, r.getGioiTinh(), dataStyle);
                createCell(row, 3, r.getSoDienThoai(), dataStyle);
                createCell(row, 4, r.getEmail(), dataStyle);
                createCell(row, 5, r.getMaHo(), dataStyle);
                createCell(row, 6, r.getVaiTro(), dataStyle);
            }
            autoSizeColumns(sheet, 7);
            return toByteArray(workbook);
        }
    }

    // ==========================================
    // CÁC HÀM XUẤT PDF (SỬ DỤNG ITEXT 7)
    // ==========================================

    /**
     * Xuất PDF Lịch Sử Ra Vào (MỚI - iText 7)
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportEntryExitToPdf(List<LichSuRaVao> logs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            
            // Title
            Paragraph title = new Paragraph("BAO CAO LICH SU RA VAO")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold();
            document.add(title);
            document.add(new Paragraph("\n"));
            
            // Table (5 columns)
            float[] columnWidths = {3, 4, 3, 2, 3};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Headers
            String[] headers = {"Thoi Gian", "Ho Ten", "CCCD", "Hoat Dong", "Cong"};
            for (String header : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
            }
            
            // Data
            for (LichSuRaVao log : logs) {
                table.addCell(formatDateTime(log.getThoiGian()));
                table.addCell(log.getCuDan() != null ? log.getCuDan().getHoVaTen() : "N/A");
                table.addCell(log.getCuDan() != null ? log.getCuDan().getCccd() : "N/A");
                table.addCell(log.getLoaiHoatDong() != null ? log.getLoaiHoatDong().toString() : "");
                table.addCell(log.getCongKiemSoat() != null ? log.getCongKiemSoat() : "");
            }
            
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error creating PDF: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportApartmentsToPdf(List<ApartmentReportDTO> apartments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            document.add(new Paragraph("BAO CAO DANH SACH CAN HO").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1.5f, 1.5f, 1, 1.5f, 1.5f}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"Ma TS", "Ten TS", "Loai", "Trang Thai", "DT", "Vi Tri", "Gia Tri"};
            for(String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            
            for(ApartmentReportDTO a : apartments) {
                table.addCell(String.valueOf(a.getMaTaiSan()));
                table.addCell(toString(a.getTenTaiSan()));
                table.addCell(toString(a.getLoaiTaiSan()));
                table.addCell(toString(a.getTrangThai()));
                table.addCell(toString(a.getDienTich()));
                table.addCell(toString(a.getViTri()));
                table.addCell(toString(a.getGiaTri()));
            }
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch(Exception e) { throw new IOException(e); }
    }

    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportInvoicesToPdf(List<InvoiceReportDTO> invoices) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            document.add(new Paragraph("BAO CAO DANH SACH HOA DON").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 2, 2, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"Ma HD", "Ten Ho", "Loai HD", "So Tien", "Trang Thai", "Ngay Tao"};
            for(String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            
            for(InvoiceReportDTO i : invoices) {
                table.addCell(String.valueOf(i.getMaHoaDon()));
                table.addCell(toString(i.getTenHo()));
                table.addCell(toString(i.getLoaiHoaDon()));
                table.addCell(toString(i.getSoTien()));
                table.addCell(toString(i.getTrangThai()));
                table.addCell(formatDateTime(i.getNgayTao()));
            }
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch(Exception e) { throw new IOException(e); }
    }
    
    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportHouseholdsToPdf(List<HouseholdReportDTO> households) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            document.add(new Paragraph("BAO CAO HO GIA DINH").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 2, 2, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"Ma Ho", "Ten Ho", "Chu Ho", "SDT", "So TV", "Trang Thai"};
            for(String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            
            for(HouseholdReportDTO h : households) {
                table.addCell(toString(h.getMaHo()));
                table.addCell(toString(h.getTenHo()));
                table.addCell(toString(h.getChuHo()));
                table.addCell(toString(h.getSoDienThoai()));
                table.addCell(toString(h.getSoThanhVien()));
                table.addCell(toString(h.getTrangThai()));
            }
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch(Exception e) { throw new IOException(e); }
    }

    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportResidentsToPdf(List<ResidentReportDTO> residents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            document.add(new Paragraph("BAO CAO DANH SACH CU DAN").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));            
            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1.5f, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"CCCD", "Ho Ten", "Gioi Tinh", "SDT", "Ma Ho"};
            for(String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            
            for(ResidentReportDTO r : residents) {
                table.addCell(toString(r.getCccd()));
                table.addCell(toString(r.getHoVaTen()));
                table.addCell(toString(r.getGioiTinh()));
                table.addCell(toString(r.getSoDienThoai()));
                table.addCell(toString(r.getMaHo()));
            }
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch(Exception e) { throw new IOException(e); }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private void createCell(Row row, int column, Object value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(column);
        if (value == null) cell.setCellValue("");
        else if (value instanceof Number number) cell.setCellValue(number.doubleValue());
        else cell.setCellValue(value.toString());
        cell.setCellStyle(style);
    }
    
    private void createExcelHeader(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], headerStyle);
        }
    }
    
    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) sheet.autoSizeColumn(i);
    }
    
    private byte[] toByteArray(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }
    
    @SuppressWarnings("unused")
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
    
    private String toString(Object obj) {
        return obj != null ? obj.toString() : "";
    }
    /**
     * Import từ Excel
     * Cột 0: CCCD
     * Cột 1: Họ Và Tên
     * Cột 2: Ngày Sinh (dd/MM/yyyy)
     * Cột 3: Thời gian (dd/MM/yyyy HH:mm)
     * Cột 4: Loại (VAO/RA)
     * Cột 5: Cổng
     */
    public String importLichSuRaVaoFromExcel(MultipartFile file) {
        List<LichSuRaVao> listToSave = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorLog = new StringBuilder();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Bỏ qua header (row 0), bắt đầu từ row 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // 1. Đọc CCCD
                    String cccd = getCellValueAsString(row.getCell(0));
                    if (cccd == null || cccd.isEmpty()) continue; // Bỏ qua dòng trống
                    // Đọc tên
                    String hoVaTen = getCellValueAsString(row.getCell(1));
                    String ngaySinh = getCellValueAsString(row.getCell(2));
                    DateTimeFormatter dobFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    // Tìm cư dân
                    DoiTuong cuDan = doiTuongDAO.findResidentByCccd(cccd).orElse(null);
                    if (cuDan == null) {
                        DoiTuong nguoiLa = new DoiTuong();
                        nguoiLa.setCccd(cccd);
                        nguoiLa.setMatKhau("0000");
                        nguoiLa.setVaiTro(UserRole.khong_dung_he_thong);
                        nguoiLa.setHoVaTen(hoVaTen);
                        nguoiLa.setLaCuDan(false);
                        nguoiLa.setNgaySinh(dobFormatter.parse(ngaySinh, LocalDate::from));
                        cuDan = doiTuongDAO.save(nguoiLa);
                    }

                    // 2. Đọc Thời gian
                    String timeStr = getCellValueAsString(row.getCell(3));
                    LocalDateTime thoiGian;
                    try {
                        thoiGian = LocalDateTime.parse(timeStr, formatter);
                    } catch (Exception e) {
                        failCount++;
                        errorLog.append("<br>- Dòng ").append(i + 1).append(": Định dạng ngày sai (").append(timeStr).append(")");
                        continue;
                    }

                    // 3. Đọc Loại
                    String typeStr = getCellValueAsString(row.getCell(4));
                    EntryExitType type = parseEntryExitType(typeStr);

                    // 4. Đọc Cổng
                    String gate = getCellValueAsString(row.getCell(5));

                    // Tạo Entity
                    LichSuRaVao log = new LichSuRaVao();
                    log.setCuDan(cuDan);
                    log.setThoiGian(thoiGian);
                    log.setLoaiHoatDong(type);
                    log.setCongKiemSoat(gate);

                    listToSave.add(log);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    errorLog.append("<br>- Dòng ").append(i + 1).append(": Lỗi dữ liệu ").append(e.getMessage());
                }
            }

            // Lưu vào DB
            if (!listToSave.isEmpty()) {
                lichSuRaVaoDAO.saveAll(listToSave);
            }

            String result = "Thành công: " + successCount + " dòng. Thất bại: " + failCount + " dòng.";
            if (failCount > 0) {
                result += " Chi tiết lỗi: " + errorLog.toString();
            }
            return result;

        } catch (IOException e) {
            return "Lỗi đọc file Excel: " + e.getMessage();
        }
    }

    // Helper: Lấy giá trị Cell an toàn
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (((org.apache.poi.ss.usermodel.Cell) cell).getCellType()) {
            case STRING -> {
                return ((org.apache.poi.ss.usermodel.Cell) cell).getStringCellValue().trim();
            }
            case NUMERIC -> {
                // Xử lý nếu ngày tháng bị Excel format thành số
                if (DateUtil.isCellDateFormatted((org.apache.poi.ss.usermodel.Cell) cell)) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    return fmt.format(((org.apache.poi.ss.usermodel.Cell) cell).getLocalDateTimeCellValue());
                }
                return String.valueOf((long) ((org.apache.poi.ss.usermodel.Cell) cell).getNumericCellValue());
            }
            default -> {
                return "";
            }
        }
    }

    // Helper: Parse Enum
    private EntryExitType parseEntryExitType(String str) {
        if (str == null) return EntryExitType.VAO;
        str = str.trim().toUpperCase();
        if (str.equals("RA") || str.equals("OUT") || str.equals("EXIT")) {
            return EntryExitType.RA;
        }
        return EntryExitType.VAO;
    }
}