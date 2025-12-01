package BlueMoon.bluemoon.models;

import java.time.LocalDate;

/**
 * DTO cho báo cáo hộ gia đình
 */
public class HouseholdReportDTO {
    private String maHo;
    private String tenHo;
    private String chuHo;
    private String cccdChuHo;
    private String soDienThoai;
    private String email;
    private Integer soThanhVien;
    private Integer soCanHo;
    private String trangThai;
    private LocalDate ngayThanhLap;
    private String ghiChu;
    
    // Constructor
    public HouseholdReportDTO() {}
    
    public HouseholdReportDTO(String maHo, String tenHo, String chuHo, String cccdChuHo,
                             String soDienThoai, String email, Integer soThanhVien,
                             Integer soCanHo, String trangThai, LocalDate ngayThanhLap,
                             String ghiChu) {
        this.maHo = maHo;
        this.tenHo = tenHo;
        this.chuHo = chuHo;
        this.cccdChuHo = cccdChuHo;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.soThanhVien = soThanhVien;
        this.soCanHo = soCanHo;
        this.trangThai = trangThai;
        this.ngayThanhLap = ngayThanhLap;
        this.ghiChu = ghiChu;
    }
    
    // Getters and Setters
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
    
    public String getCccdChuHo() {
        return cccdChuHo;
    }
    
    public void setCccdChuHo(String cccdChuHo) {
        this.cccdChuHo = cccdChuHo;
    }
    
    public String getSoDienThoai() {
        return soDienThoai;
    }
    
    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getSoThanhVien() {
        return soThanhVien;
    }
    
    public void setSoThanhVien(Integer soThanhVien) {
        this.soThanhVien = soThanhVien;
    }
    
    public Integer getSoCanHo() {
        return soCanHo;
    }
    
    public void setSoCanHo(Integer soCanHo) {
        this.soCanHo = soCanHo;
    }
    
    public String getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
    
    public LocalDate getNgayThanhLap() {
        return ngayThanhLap;
    }
    
    public void setNgayThanhLap(LocalDate ngayThanhLap) {
        this.ngayThanhLap = ngayThanhLap;
    }
    
    public String getGhiChu() {
        return ghiChu;
    }
    
    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}
