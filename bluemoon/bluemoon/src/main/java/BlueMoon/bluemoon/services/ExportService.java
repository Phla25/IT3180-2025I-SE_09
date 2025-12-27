package BlueMoon.bluemoon.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

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
    
    @SuppressWarnings("unused")
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // Đường dẫn file font (BẮT BUỘC PHẢI CÓ FILE NÀY TRONG src/main/resources/fonts/)
    private static final String FONT_PATH = "fonts/times.ttf"; 

    @Autowired
    private LichSuRaVaoDAO lichSuRaVaoDAO;
    @Autowired
    private DoiTuongDAO doiTuongDAO;

    // =========================================================================
    // 1. CẤU HÌNH PHÔNG TIẾNG VIỆT CHO PDF (iText 7)
    // =========================================================================
    // Thêm vào ExportService.java

    private PdfFont getVietnameseFont() {
    try {
        // Option 1: Sử dụng font hệ thống (an toàn nhất)
        ClassPathResource resource = new ClassPathResource(FONT_PATH);
        
        if (!resource.exists()) {
            System.err.println("⚠️ Font file not found, using embedded font");
            // Fallback: Sử dụng font được nhúng sẵn trong iText
            return PdfFontFactory.createFont("Helvetica", PdfEncodings.IDENTITY_H);
        }

        InputStream is = resource.getInputStream();
        byte[] fontBytes = is.readAllBytes();
        is.close();
        
        // QUAN TRỌNG: Phải set EMBEDDED = true để nhúng font vào PDF
        return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, 
                                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        
    } catch (IOException e) {
        System.err.println("❌ Font loading error: " + e.getMessage());
        try {
            // Fallback an toàn
            return PdfFontFactory.createFont(StandardFonts.HELVETICA);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot create font", ex);
        }
    }
}

    // Sửa lại hàm exportInvoicesToPdf trong ExportService.java
// QUAN TRỌNG: Phải close Document trước khi lấy byte[]

    @SuppressWarnings("CallToPrintStackTrace")
    public byte[] exportInvoicesToPdf(List<InvoiceReportDTO> invoices) throws IOException {
    
    System.out.println("🔧 [ExportService] Starting PDF generation...");
    
    if (invoices == null || invoices.isEmpty()) {
        System.err.println("⚠️ [ExportService] Invoice list is empty!");
        throw new IOException("No invoice data provided");
    }
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PdfWriter writer = null;
    PdfDocument pdfDoc = null;
    Document document = null;
    
    try {
        // Tạo PDF Writer
        writer = new PdfWriter(baos);
        pdfDoc = new PdfDocument(writer);
        document = new Document(pdfDoc, PageSize.A4);
        
        System.out.println("✅ [ExportService] PDF Document created");
        
        PdfFont font = getVietnameseFont();
            if (font != null) {
                document.setFont(font);
                System.out.println("✅ [ExportService] Font applied to document");
            } else {
                System.err.println("⚠️ [ExportService] No font available, using default");
            }
        // TẠM THỜI BỎ FONT TIẾNG VIỆT ĐỂ TEST
        // Sau khi hoạt động, sẽ thêm lại
        // try {
        //     document.setFont(getVietnameseFont());
        // } catch (Exception e) {
        //     System.err.println("⚠️ Font error, using default font");
        // }
        
        document.setMargins(30, 30, 30, 30);
        
        InvoiceReportDTO inv = invoices.get(0);
        System.out.println("✅ [ExportService] Processing invoice: " + inv.getMaHoaDon());

        // ==================== HEADER ====================
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        headerTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell leftHeader = new Cell()
            .add(new Paragraph("BLUEMOON MANAGEMENT")
                .setBold()
                .setFontSize(16)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLUE))
            .add(new Paragraph("Address: Ha Noi, Viet Nam").setFontSize(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            
        Cell rightHeader = new Cell()
            .add(new Paragraph("SERVICE INVOICE")
                .setBold()
                .setFontSize(18)
                .setTextAlignment(TextAlignment.RIGHT))
            .add(new Paragraph("No: HD-" + inv.getMaHoaDon())
                .setFontSize(12)
                .setTextAlignment(TextAlignment.RIGHT))
            .add(new Paragraph("Date: " + formatDateTime(inv.getNgayTao()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            
        headerTable.addCell(leftHeader);
        headerTable.addCell(rightHeader);
        document.add(headerTable);
        document.add(new Paragraph("\n"));
        
        System.out.println("✅ [ExportService] Header added");
        
        // ==================== CUSTOMER INFO ====================
        Table customerTable = new Table(UnitValue.createPercentArray(new float[]{1}));
        customerTable.setWidth(UnitValue.createPercentValue(100));
        
        Cell headerCell = new Cell()
            .add(new Paragraph("PAYMENT INFORMATION").setBold()
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE))
            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
            .setPadding(5);
        customerTable.addCell(headerCell);
        
        String info = String.format("Household head: %s\nApartment: %s (%s)\nFee type: %s", 
                safeToString(inv.getChuHo()), 
                safeToString(inv.getTenHo()), 
                safeToString(inv.getMaHo()), 
                safeToString(inv.getLoaiHoaDon()));
        
        Cell infoCell = new Cell()
            .add(new Paragraph(info))
            .setPadding(10);
        customerTable.addCell(infoCell);
        
        document.add(customerTable);
        document.add(new Paragraph("\n"));

        System.out.println("✅ [ExportService] Customer info added");

        // ==================== ITEM TABLE ====================
        Table itemTable = new Table(UnitValue.createPercentArray(new float[]{1, 4, 2}));
        itemTable.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        itemTable.addHeaderCell(new Cell()
            .add(new Paragraph("No").setBold())
            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
            .setPadding(5));
        itemTable.addHeaderCell(new Cell()
            .add(new Paragraph("Description").setBold())
            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
            .setPadding(5));
        itemTable.addHeaderCell(new Cell()
            .add(new Paragraph("Amount (VND)").setBold())
            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(5));
        
        // Data row
        itemTable.addCell(new Cell()
            .add(new Paragraph("1"))
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5));
        
        String description = String.format("Payment for '%s' fee - %d/%d", 
            safeToString(inv.getLoaiHoaDon()), 
            inv.getNgayTao().getMonthValue(), 
            inv.getNgayTao().getYear());
        itemTable.addCell(new Cell()
            .add(new Paragraph(description))
            .setPadding(5));
        
        itemTable.addCell(new Cell()
            .add(new Paragraph(String.format("%,.0f", inv.getSoTien())))
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(5));
        
        // Total row
        itemTable.addCell(new Cell(1, 2)
            .add(new Paragraph("TOTAL").setBold())
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(5));
        itemTable.addCell(new Cell()
            .add(new Paragraph(String.format("%,.0f", inv.getSoTien())).setBold())
            .setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED)
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(5));
        
        document.add(itemTable);
        
        System.out.println("✅ [ExportService] Item table added");
        
        // Footer
        document.add(new Paragraph("\n\nThank you for your payment!")
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic());
        
        // ==================== ĐÓNG DOCUMENT (QUAN TRỌNG!) ====================
        document.close();
        pdfDoc.close();
        writer.close();
        
        System.out.println("✅ [ExportService] Document closed properly");
        
        byte[] result = baos.toByteArray();
        System.out.println("✅ [ExportService] PDF generation completed successfully");
        System.out.println("   Final size: " + result.length + " bytes");
        
        if (result.length < 500) {
            System.err.println("⚠️ WARNING: PDF size is too small!");
        }
        
        return result;
        
    } catch (IOException e) {
        System.err.println("❌ [ExportService] Error generating PDF:");
        System.err.println("   Type: " + e.getClass().getName());
        System.err.println("   Message: " + e.getMessage());
        e.printStackTrace();
        throw new IOException("Failed to generate PDF: " + e.getMessage(), e);
        
    } finally {
        // Đảm bảo đóng resources ngay cả khi có lỗi
        try {
            if (document != null) document.close();
            if (pdfDoc != null) pdfDoc.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            System.err.println("Error closing PDF resources: " + e.getMessage());
        }
    }
}

// Helper method để tránh null
private String safeToString(Object obj) {
    return obj != null ? obj.toString() : "N/A";
}

    // =========================================================================
    // 2. CÁC HÀM XUẤT EXCEL (Đã thêm Font Times New Roman)
    // =========================================================================

    public byte[] exportApartmentsToExcel(List<ApartmentReportDTO> apartments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Căn Hộ");
            // Header
            createExcelHeader(sheet, new String[]{
                "Mã Tài Sản", "Tên Tài Sản", "Loại", "Trạng Thái", "Diện Tích (m²)", 
                "Vị Trí", "Giá Trị", "Ngày Thêm", "Mã Hộ", "Tên Hộ", "Chủ Hộ"
            });
            
            // Data
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

    // =========================================================================
    // 3. CÁC HÀM XUẤT PDF (Đã áp dụng Font Tiếng Việt cho tất cả)
    // =========================================================================

    public byte[] exportEntryExitToPdf(List<LichSuRaVao> logs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, PageSize.A4)) {
            
            // [QUAN TRỌNG] Set font tiếng Việt
            document.setFont(getVietnameseFont());

            document.add(new Paragraph("BÁO CÁO LỊCH SỬ RA VÀO").setTextAlignment(TextAlignment.CENTER).setFontSize(18).setBold());
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 4, 3, 2, 3}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"Thời Gian", "Họ Tên", "CCCD", "Hoạt Động", "Cổng"};
            for (String header : headers) table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
            
            for (LichSuRaVao log : logs) {
                table.addCell(formatDateTime(log.getThoiGian()));
                table.addCell(toString(log.getCuDan() != null ? log.getCuDan().getHoVaTen() : "N/A"));
                table.addCell(toString(log.getCuDan() != null ? log.getCuDan().getCccd() : "N/A"));
                table.addCell(toString(log.getLoaiHoatDong()));
                table.addCell(toString(log.getCongKiemSoat()));
            }
            document.add(table);
            return baos.toByteArray();
        }
    }

    public byte[] exportApartmentsToPdf(List<ApartmentReportDTO> apartments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            
            // [QUAN TRỌNG] Set font tiếng Việt
            document.setFont(getVietnameseFont());
            
            document.add(new Paragraph("BÁO CÁO DANH SÁCH CĂN HỘ").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1.5f, 1.5f, 1, 1.5f, 1.5f}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"Mã TS", "Tên TS", "Loại", "Trạng Thái", "DT", "Vị Trí", "Giá Trị"};
            for(String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            
            for(ApartmentReportDTO a : apartments) {
                table.addCell(toString(a.getMaTaiSan()));
                table.addCell(toString(a.getTenTaiSan()));
                table.addCell(toString(a.getLoaiTaiSan()));
                table.addCell(toString(a.getTrangThai()));
                table.addCell(toString(a.getDienTich()));
                table.addCell(toString(a.getViTri()));
                table.addCell(toString(a.getGiaTri()));
            }
            document.add(table);
            return baos.toByteArray();
        }
    }

    public byte[] exportHouseholdsToPdf(List<HouseholdReportDTO> households) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            
            // [QUAN TRỌNG] Set font tiếng Việt
            document.setFont(getVietnameseFont());
            
            document.add(new Paragraph("BÁO CÁO HỘ GIA ĐÌNH").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));
            
            Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 2, 2, 2, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"Mã Hộ", "Tên Hộ", "Chủ Hộ", "SĐT", "Số TV", "Trạng Thái"};
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
            return baos.toByteArray();
        }
    }

    public byte[] exportResidentsToPdf(List<ResidentReportDTO> residents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            
            // [QUAN TRỌNG] Set font tiếng Việt
            document.setFont(getVietnameseFont());
            
            document.add(new Paragraph("BÁO CÁO DANH SÁCH CƯ DÂN").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            document.add(new Paragraph("\n"));            
            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1.5f, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            String[] headers = {"CCCD", "Họ Tên", "Giới Tính", "SĐT", "Mã Hộ"};
            for(String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            
            for(ResidentReportDTO r : residents) {
                table.addCell(toString(r.getCccd()));
                table.addCell(toString(r.getHoVaTen()));
                table.addCell(toString(r.getGioiTinh()));
                table.addCell(toString(r.getSoDienThoai()));
                table.addCell(toString(r.getMaHo()));
            }
            document.add(table);
            return baos.toByteArray();
        }
    }

    // =========================================================================
    // 4. HÀM IMPORT (GIỮ NGUYÊN)
    // =========================================================================
    public String importLichSuRaVaoFromExcel(MultipartFile file) {
        List<LichSuRaVao> listToSave = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorLog = new StringBuilder();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            DateTimeFormatter dobFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String cccd = getCellValueAsString(row.getCell(0));
                    if (cccd == null || cccd.isEmpty()) continue;
                    
                    String hoVaTen = getCellValueAsString(row.getCell(1));
                    String ngaySinh = getCellValueAsString(row.getCell(2));

                    DoiTuong cuDan = doiTuongDAO.findResidentByCccd(cccd).orElse(null);
                    if (cuDan == null) {
                        DoiTuong nguoiLa = new DoiTuong();
                        nguoiLa.setCccd(cccd);
                        nguoiLa.setMatKhau("0000");
                        nguoiLa.setVaiTro(UserRole.khong_dung_he_thong);
                        nguoiLa.setHoVaTen(hoVaTen);
                        nguoiLa.setLaCuDan(false);
                        try {
                            nguoiLa.setNgaySinh(LocalDate.parse(ngaySinh, dobFormatter));
                        } catch (Exception e) { /* Ignore date error for stranger */ }
                        cuDan = doiTuongDAO.save(nguoiLa);
                    }

                    String timeStr = getCellValueAsString(row.getCell(3));
                    LocalDateTime thoiGian = LocalDateTime.parse(timeStr, formatter);
                    EntryExitType type = parseEntryExitType(getCellValueAsString(row.getCell(4)));
                    String gate = getCellValueAsString(row.getCell(5));

                    LichSuRaVao log = new LichSuRaVao();
                    log.setCuDan(cuDan);
                    log.setThoiGian(thoiGian);
                    log.setLoaiHoatDong(type);
                    log.setCongKiemSoat(gate);
                    listToSave.add(log);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errorLog.append("<br>- Dòng ").append(i + 1).append(": ").append(e.getMessage());
                }
            }

            if (!listToSave.isEmpty()) lichSuRaVaoDAO.saveAll(listToSave);
            String result = "Thành công: " + successCount + " dòng. Thất bại: " + failCount + " dòng.";
            if (failCount > 0) result += " Lỗi: " + errorLog.toString();
            return result;

        } catch (IOException e) {
            return "Lỗi đọc file Excel: " + e.getMessage();
        }
    }

    // =========================================================================
    // 5. HELPER METHODS (Đã cập nhật Font cho Excel)
    // =========================================================================
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontName("Times New Roman"); // FIX FONT EXCEL
        font.setFontHeightInPoints((short) 12);
        
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
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setFontName("Times New Roman"); // FIX FONT EXCEL
        font.setFontHeightInPoints((short) 11);
        
        style.setFont(font);
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
    
    private String toString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING -> { return cell.getStringCellValue().trim(); }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    return fmt.format(cell.getLocalDateTimeCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            }
            default -> { return ""; }
        }
    }

    private EntryExitType parseEntryExitType(String str) {
        if (str == null) return EntryExitType.VAO;
        str = str.trim().toUpperCase();
        if (str.equals("RA") || str.equals("OUT") || str.equals("EXIT")) return EntryExitType.RA;
        return EntryExitType.VAO;
    }
}