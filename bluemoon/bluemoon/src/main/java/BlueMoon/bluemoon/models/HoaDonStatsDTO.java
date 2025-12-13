package BlueMoon.bluemoon.models;

import java.math.BigDecimal;

/**
 * DTO chứa thống kê hóa đơn cho Dashboard Cư Dân và Kế Toán.
 */
public class HoaDonStatsDTO {
    
    // =======================================================
    // Dữ liệu cơ bản (Dùng chung cho Cư Dân & Kế Toán)
    // =======================================================
    // Số tiền chưa thanh toán của hộ (Cho Cư Dân) hoặc Tổng số tiền chưa thu (Cho Kế Toán)
    public BigDecimal tongChuaThanhToan = BigDecimal.ZERO; 
    public String trangThai = "Tất cả đã thanh toán";
    public Integer tongHoaDonChuaThanhToan = 0; // Số hóa đơn chưa thanh toán/chưa thu
    
    // =======================================================
    // Dữ liệu mở rộng cho KẾ TOÁN (FINANCIAL FOCUS)
    // =======================================================
    public BigDecimal tongThuThangNay = BigDecimal.ZERO;
    
    // [QUAN TRỌNG] Phải có 2 trường này thì Service mới biên dịch được
    public BigDecimal tongChiThangNay = BigDecimal.ZERO; 
    public BigDecimal loiNhuanRong = BigDecimal.ZERO;

    public BigDecimal tongQuaHan = BigDecimal.ZERO;
    public Integer soHoaDonQuaHan = 0;
    public Double phanTramTangTruong = 0.0; // Biến động so với tháng trước (%)

    // === Constructors ===
    public HoaDonStatsDTO() {}
    
    public HoaDonStatsDTO(BigDecimal tongChuaThanhToan, String trangThai, Integer tongSoHoaDonChuaThanhToan){
        this.tongChuaThanhToan = tongChuaThanhToan;
        this.trangThai = trangThai;
        this.tongHoaDonChuaThanhToan = tongSoHoaDonChuaThanhToan;   
    }
}