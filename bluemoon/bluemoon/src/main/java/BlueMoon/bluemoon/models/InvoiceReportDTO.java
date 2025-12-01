package BlueMoon.bluemoon.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO cho báo cáo hóa đơn
 */
public class InvoiceReportDTO {
    private Integer maHoaDon;
    private String maHo;
    private String tenHo;
    private String chuHo;
    private String loaiHoaDon;
    private BigDecimal soTien;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDate hanThanhToan;
    private LocalDateTime ngayThanhToan;
    private String nguoiThanhToan;
    private String ghiChu;
    
    // Constructor
    public InvoiceReportDTO() {}
    
    public InvoiceReportDTO(Integer maHoaDon, String maHo, String tenHo, String chuHo,
                           String loaiHoaDon, BigDecimal soTien, String trangThai,
                           LocalDateTime ngayTao, LocalDate hanThanhToan, 
                           LocalDateTime ngayThanhToan, String nguoiThanhToan, 
                           String ghiChu) {
        this.maHoaDon = maHoaDon;
        this.maHo = maHo;
        this.tenHo = tenHo;
        this.chuHo = chuHo;
        this.loaiHoaDon = loaiHoaDon;
        this.soTien = soTien;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
        this.hanThanhToan = hanThanhToan;
        this.ngayThanhToan = ngayThanhToan;
        this.nguoiThanhToan = nguoiThanhToan;
        this.ghiChu = ghiChu;
    }
    
    // Getters and Setters
    public Integer getMaHoaDon() {
        return maHoaDon;
    }
    
    public void setMaHoaDon(Integer maHoaDon) {
        this.maHoaDon = maHoaDon;
    }
    
    public String getMaHo() {
        return maHo;
    }
    
    public void setMaHo(String maHo) {
        this.maHo = maHo;
    }
    
    public String getTenHo() {
        return tenHo;
    }
    
    public void setTenHo(String tenHo) {
        this.tenHo = tenHo;
    }
    
    public String getChuHo() {
        return chuHo;
    }
    
    public void setChuHo(String chuHo) {
        this.chuHo = chuHo;
    }
    
    public String getLoaiHoaDon() {
        return loaiHoaDon;
    }
    
    public void setLoaiHoaDon(String loaiHoaDon) {
        this.loaiHoaDon = loaiHoaDon;
    }
    
    public BigDecimal getSoTien() {
        return soTien;
    }
    
    public void setSoTien(BigDecimal soTien) {
        this.soTien = soTien;
    }
    
    public String getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
    
    public LocalDateTime getNgayTao() {
        return ngayTao;
    }
    
    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
    
    public LocalDate getHanThanhToan() {
        return hanThanhToan;
    }
    
    public void setHanThanhToan(LocalDate hanThanhToan) {
        this.hanThanhToan = hanThanhToan;
    }
    
    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }
    
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        this.ngayThanhToan = ngayThanhToan;
    }
    
    public String getNguoiThanhToan() {
        return nguoiThanhToan;
    }
    
    public void setNguoiThanhToan(String nguoiThanhToan) {
        this.nguoiThanhToan = nguoiThanhToan;
    }
    
    public String getGhiChu() {
        return ghiChu;
    }
    
    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}
