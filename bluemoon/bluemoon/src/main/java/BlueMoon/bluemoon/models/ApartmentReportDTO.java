package BlueMoon.bluemoon.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho báo cáo thông tin căn hộ
 */
public class ApartmentReportDTO {
    private Integer maTaiSan;
    private String tenTaiSan;
    private String loaiTaiSan;
    private String trangThai;
    private BigDecimal dienTich;
    private String viTri;
    private BigDecimal giaTri;
    private LocalDateTime ngayThem;
    
    // Thông tin hộ gia đình sở hữu
    private String maHo;
    private String tenHo;
    private String chuHo;
    private String soDienThoai;
    private String trangThaiHo;
    
    // Constructor
    public ApartmentReportDTO() {}
    
    public ApartmentReportDTO(Integer maTaiSan, String tenTaiSan, String loaiTaiSan, 
                             String trangThai, BigDecimal dienTich, String viTri, 
                             BigDecimal giaTri, LocalDateTime ngayThem,
                             String maHo, String tenHo, String chuHo, 
                             String soDienThoai, String trangThaiHo) {
        this.maTaiSan = maTaiSan;
        this.tenTaiSan = tenTaiSan;
        this.loaiTaiSan = loaiTaiSan;
        this.trangThai = trangThai;
        this.dienTich = dienTich;
        this.viTri = viTri;
        this.giaTri = giaTri;
        this.ngayThem = ngayThem;
        this.maHo = maHo;
        this.tenHo = tenHo;
        this.chuHo = chuHo;
        this.soDienThoai = soDienThoai;
        this.trangThaiHo = trangThaiHo;
    }
    
    // Getters and Setters
    public Integer getMaTaiSan() {
        return maTaiSan;
    }
    
    public void setMaTaiSan(Integer maTaiSan) {
        this.maTaiSan = maTaiSan;
    }
    
    public String getTenTaiSan() {
        return tenTaiSan;
    }
    
    public void setTenTaiSan(String tenTaiSan) {
        this.tenTaiSan = tenTaiSan;
    }
    
    public String getLoaiTaiSan() {
        return loaiTaiSan;
    }
    
    public void setLoaiTaiSan(String loaiTaiSan) {
        this.loaiTaiSan = loaiTaiSan;
    }
    
    public String getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
    
    public BigDecimal getDienTich() {
        return dienTich;
    }
    
    public void setDienTich(BigDecimal dienTich) {
        this.dienTich = dienTich;
    }
    
    public String getViTri() {
        return viTri;
    }
    
    public void setViTri(String viTri) {
        this.viTri = viTri;
    }
    
    public BigDecimal getGiaTri() {
        return giaTri;
    }
    
    public void setGiaTri(BigDecimal giaTri) {
        this.giaTri = giaTri;
    }
    
    public LocalDateTime getNgayThem() {
        return ngayThem;
    }
    
    public void setNgayThem(LocalDateTime ngayThem) {
        this.ngayThem = ngayThem;
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
    
    public String getSoDienThoai() {
        return soDienThoai;
    }
    
    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }
    
    public String getTrangThaiHo() {
        return trangThaiHo;
    }
    
    public void setTrangThaiHo(String trangThaiHo) {
        this.trangThaiHo = trangThaiHo;
    }
}
