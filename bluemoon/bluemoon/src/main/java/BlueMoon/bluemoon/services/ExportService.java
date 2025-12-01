package BlueMoon.bluemoon.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import BlueMoon.bluemoon.models.ApartmentReportDTO;
import BlueMoon.bluemoon.models.HouseholdReportDTO;
import BlueMoon.bluemoon.models.InvoiceReportDTO;
import BlueMoon.bluemoon.models.ResidentReportDTO;

/**
 * Service để xuất dữ liệu ra file Excel
 */
@Service
public class ExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /**
     * Xuất danh sách căn hộ ra file Excel
     */
    public byte[] exportApartmentsToExcel(List<ApartmentReportDTO> apartments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Căn Hộ");
            
            // Tạo style cho header
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Mã Tài Sản", "Tên Tài Sản", "Loại", "Trạng Thái", 
                "Diện Tích (m²)", "Vị Trí", "Giá Trị (VNĐ)", "Ngày Thêm",
                "Mã Hộ", "Tên Hộ", "Chủ Hộ", "Số Điện Thoại", "Trạng Thái Hộ"
            };
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Điền dữ liệu
            int rowNum = 1;
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
                createCell(row, 11, apt.getSoDienThoai(), dataStyle);
                createCell(row, 12, apt.getTrangThaiHo(), dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Chuyển workbook thành byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Xuất danh sách hóa đơn ra file Excel
     */
    public byte[] exportInvoicesToExcel(List<InvoiceReportDTO> invoices) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Hóa Đơn");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Mã Hóa Đơn", "Mã Hộ", "Tên Hộ", "Người Đăng Ký", "Loại Hóa Đơn",
                "Số Tiền (VNĐ)", "Trạng Thái", "Ngày Tạo", "Hạn Thanh Toán",
                "Ngày Thanh Toán", "Người Thanh Toán", "Ghi Chú"
            };
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Điền dữ liệu
            int rowNum = 1;
            for (InvoiceReportDTO invoice : invoices) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, invoice.getMaHoaDon(), dataStyle);
                createCell(row, 1, invoice.getMaHo(), dataStyle);
                createCell(row, 2, invoice.getTenHo(), dataStyle);
                createCell(row, 3, invoice.getChuHo(), dataStyle);
                createCell(row, 4, invoice.getLoaiHoaDon(), dataStyle);
                createCell(row, 5, invoice.getSoTien(), dataStyle);
                createCell(row, 6, invoice.getTrangThai(), dataStyle);
                createCell(row, 7, formatDateTime(invoice.getNgayTao()), dataStyle);
                createCell(row, 8, formatDate(invoice.getHanThanhToan()), dataStyle);
                createCell(row, 9, formatDateTime(invoice.getNgayThanhToan()), dataStyle);
                createCell(row, 10, invoice.getNguoiThanhToan(), dataStyle);
                createCell(row, 11, invoice.getGhiChu(), dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Xuất danh sách hộ gia đình ra file Excel
     */
    public byte[] exportHouseholdsToExcel(List<HouseholdReportDTO> households) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Hộ Gia Đình");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Mã Hộ", "Tên Hộ", "Chủ Hộ", "CCCD Chủ Hộ", "Số Điện Thoại",
                "Email", "Số Thành Viên", "Số Căn Hộ", "Trạng Thái", 
                "Ngày Thành Lập", "Ghi Chú"
            };
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Điền dữ liệu
            int rowNum = 1;
            for (HouseholdReportDTO household : households) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, household.getMaHo(), dataStyle);
                createCell(row, 1, household.getTenHo(), dataStyle);
                createCell(row, 2, household.getChuHo(), dataStyle);
                createCell(row, 3, household.getCccdChuHo(), dataStyle);
                createCell(row, 4, household.getSoDienThoai(), dataStyle);
                createCell(row, 5, household.getEmail(), dataStyle);
                createCell(row, 6, household.getSoThanhVien(), dataStyle);
                createCell(row, 7, household.getSoCanHo(), dataStyle);
                createCell(row, 8, household.getTrangThai(), dataStyle);
                createCell(row, 9, formatDate(household.getNgayThanhLap()), dataStyle);
                createCell(row, 10, household.getGhiChu(), dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    // Helper methods
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String string) {
            cell.setCellValue(string);
        } else if (value instanceof Integer integer) {
            cell.setCellValue(integer);
        } else if (value instanceof Long aLong) {
            cell.setCellValue(aLong);
        } else if (value instanceof Double aDouble) {
            cell.setCellValue(aDouble);
        } else if (value instanceof java.math.BigDecimal bigDecimal ) {
            cell.setCellValue(bigDecimal.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
        
        cell.setCellStyle(style);
    }
    
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }
    
    /**
     * Xuất danh sách cư dân ra file Excel
     */
    public byte[] exportResidentsToExcel(List<ResidentReportDTO> residents) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh Sách Cư Dân");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "CCCD", "Họ và Tên", "Giới Tính", "Ngày Sinh", "Số Điện Thoại",
                "Email", "Địa Chỉ Thường Trú", "Trạng Thái", "Vai Trò",
                "Mã Hộ", "Tên Hộ", "Là Chủ Hộ", "Quan Hệ Chủ Hộ"
            };
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Điền dữ liệu
            int rowNum = 1;
            for (ResidentReportDTO resident : residents) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, resident.getCccd(), dataStyle);
                createCell(row, 1, resident.getHoVaTen(), dataStyle);
                createCell(row, 2, resident.getGioiTinh(), dataStyle);
                createCell(row, 3, formatDate(resident.getNgaySinh()), dataStyle);
                createCell(row, 4, resident.getSoDienThoai(), dataStyle);
                createCell(row, 5, resident.getEmail(), dataStyle);
                createCell(row, 6, resident.getDiaChiThuongTru(), dataStyle);
                createCell(row, 7, resident.getTrangThai(), dataStyle);
                createCell(row, 8, resident.getVaiTro(), dataStyle);
                createCell(row, 9, resident.getMaHo(), dataStyle);
                createCell(row, 10, resident.getTenHo(), dataStyle);
                createCell(row, 11, resident.getLaChuHo() != null && resident.getLaChuHo() ? "Có" : "Không", dataStyle);
                createCell(row, 12, resident.getQuanHeChuHo(), dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    // ========== PDF EXPORT METHODS ==========
    
    /**
     * Xuất danh sách căn hộ ra file PDF
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportApartmentsToPdf(List<ApartmentReportDTO> apartments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Title
            Paragraph title = new Paragraph("BAO CAO DANH SACH CAN HO")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold();
            document.add(title);
            
            document.add(new Paragraph("\n"));
            
            // Table
            float[] columnWidths = {1, 2, 2, 2, 2, 2, 2, 2, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Headers
            String[] headers = {"Ma TS", "Ten TS", "Loai", "Trang Thai", "DT (m2)", 
                               "Vi Tri", "Gia Tri", "Ma Ho", "Ten Ho", "Chu Ho"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header).setBold()));
            }
            
            // Data
            for (ApartmentReportDTO apt : apartments) {
                table.addCell(String.valueOf(apt.getMaTaiSan()));
                table.addCell(apt.getTenTaiSan() != null ? apt.getTenTaiSan() : "");
                table.addCell(apt.getLoaiTaiSan() != null ? apt.getLoaiTaiSan() : "");
                table.addCell(apt.getTrangThai() != null ? apt.getTrangThai() : "");
                table.addCell(apt.getDienTich() != null ? apt.getDienTich().toString() : "");
                table.addCell(apt.getViTri() != null ? apt.getViTri() : "");
                table.addCell(apt.getGiaTri() != null ? apt.getGiaTri().toString() : "");
                table.addCell(apt.getMaHo() != null ? apt.getMaHo() : "");
                table.addCell(apt.getTenHo() != null ? apt.getTenHo() : "");
                table.addCell(apt.getChuHo() != null ? apt.getChuHo() : "");
            }
            
            document.add(table);
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error creating PDF", e);
        }
    }
    
    /**
     * Xuất danh sách hóa đơn ra file PDF
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportInvoicesToPdf(List<InvoiceReportDTO> invoices) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Title
            Paragraph title = new Paragraph("BAO CAO DANH SACH HOA DON")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold();
            document.add(title);
            
            document.add(new Paragraph("\n"));
            
            // Table
            float[] columnWidths = {1, 1.5f, 1.5f, 1.5f, 2, 2, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Headers
            String[] headers = {"Ma HD", "Ma Ho", "Ten Ho", "Chu Ho", "Loai HD", 
                               "So Tien", "Trang Thai", "Ngay Tao"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header).setBold()));
            }
            
            // Data
            for (InvoiceReportDTO invoice : invoices) {
                table.addCell(String.valueOf(invoice.getMaHoaDon()));
                table.addCell(invoice.getMaHo() != null ? invoice.getMaHo() : "");
                table.addCell(invoice.getTenHo() != null ? invoice.getTenHo() : "");
                table.addCell(invoice.getChuHo() != null ? invoice.getChuHo() : "");
                table.addCell(invoice.getLoaiHoaDon() != null ? invoice.getLoaiHoaDon() : "");
                table.addCell(invoice.getSoTien() != null ? invoice.getSoTien().toString() : "");
                table.addCell(invoice.getTrangThai() != null ? invoice.getTrangThai() : "");
                table.addCell(formatDateTime(invoice.getNgayTao()));
            }
            
            document.add(table);
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error creating PDF", e);
        }
    }
    
    /**
     * Xuất danh sách hộ gia đình ra file PDF
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportHouseholdsToPdf(List<HouseholdReportDTO> households) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Title
            Paragraph title = new Paragraph("BAO CAO DANH SACH HO GIA DINH")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold();
            document.add(title);
            
            document.add(new Paragraph("\n"));
            
            // Table
            float[] columnWidths = {1.5f, 2, 2, 2, 1.5f, 1, 1, 1.5f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Headers
            String[] headers = {"Ma Ho", "Ten Ho", "Chu Ho", "SDT", "Email", 
                               "So TV", "So CH", "Trang Thai"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header).setBold()));
            }
            
            // Data
            for (HouseholdReportDTO household : households) {
                table.addCell(household.getMaHo() != null ? household.getMaHo() : "");
                table.addCell(household.getTenHo() != null ? household.getTenHo() : "");
                table.addCell(household.getChuHo() != null ? household.getChuHo() : "");
                table.addCell(household.getSoDienThoai() != null ? household.getSoDienThoai() : "");
                table.addCell(household.getEmail() != null ? household.getEmail() : "");
                table.addCell(household.getSoThanhVien() != null ? household.getSoThanhVien().toString() : "");
                table.addCell(household.getSoCanHo() != null ? household.getSoCanHo().toString() : "");
                table.addCell(household.getTrangThai() != null ? household.getTrangThai() : "");
            }
            
            document.add(table);
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error creating PDF", e);
        }
    }
    
    /**
     * Xuất danh sách cư dân ra file PDF
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public byte[] exportResidentsToPdf(List<ResidentReportDTO> residents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Title
            Paragraph title = new Paragraph("BAO CAO DANH SACH CU DAN")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setBold();
            document.add(title);
            
            document.add(new Paragraph("\n"));
            
            // Table
            float[] columnWidths = {2, 3, 1.5f, 2, 2, 2, 1.5f, 1.5f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Headers
            String[] headers = {"CCCD", "Ho Va Ten", "Gioi Tinh", "SDT", "Email", 
                               "Ma Ho", "Ten Ho", "Chu Ho"};
            for (String header : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(header).setBold()));
            }
            
            // Data
            for (ResidentReportDTO resident : residents) {
                table.addCell(resident.getCccd() != null ? resident.getCccd() : "");
                table.addCell(resident.getHoVaTen() != null ? resident.getHoVaTen() : "");
                table.addCell(resident.getGioiTinh() != null ? resident.getGioiTinh() : "");
                table.addCell(resident.getSoDienThoai() != null ? resident.getSoDienThoai() : "");
                table.addCell(resident.getEmail() != null ? resident.getEmail() : "");
                table.addCell(resident.getMaHo() != null ? resident.getMaHo() : "");
                table.addCell(resident.getTenHo() != null ? resident.getTenHo() : "");
                table.addCell(resident.getLaChuHo() != null && resident.getLaChuHo() ? "Co" : "Khong");
            }
            
            document.add(table);
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Error creating PDF", e);
        }
    }
}
