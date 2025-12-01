package BlueMoon.bluemoon.models;

import java.time.LocalDate;

/**
 * DTO cho báo cáo cư dân
 */
public class ResidentReportDTO {
    private String cccd;
    private String hoVaTen;
    private String gioiTinh;
    private LocalDate ngaySinh;
    private String soDienThoai;
    private String email;
    private String diaChiThuongTru;
    private String trangThai;
    private String vaiTro;
    
    // Thông tin hộ gia đình
    private String maHo;
    private String tenHo;
    private Boolean laChuHo;
    private String quanHeChuHo;
    
    // Constructor
    public ResidentReportDTO() {}
    
    public ResidentReportDTO(String cccd, String hoVaTen, String gioiTinh, 
                            LocalDate ngaySinh, String soDienThoai, String email,
                            String diaChiThuongTru, String trangThai, String vaiTro,
                            String maHo, String tenHo, Boolean laChuHo, 
                            String quanHeChuHo) {
        this.cccd = cccd;
        this.hoVaTen = hoVaTen;
        this.gioiTinh = gioiTinh;
        this.ngaySinh = ngaySinh;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.diaChiThuongTru = diaChiThuongTru;
        this.trangThai = trangThai;
        this.vaiTro = vaiTro;
        this.maHo = maHo;
        this.tenHo = tenHo;
        this.laChuHo = laChuHo;
        this.quanHeChuHo = quanHeChuHo;
    }
    
    // Getters and Setters
    public String getCccd() {
        return cccd;
    }
    
    public void setCccd(String cccd) {
        this.cccd = cccd;
    }
    
    public String getHoVaTen() {
        return hoVaTen;
    }
    
    public void setHoVaTen(String hoVaTen) {
        this.hoVaTen = hoVaTen;
    }
    
    public String getGioiTinh() {
        return gioiTinh;
    }
    
    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }
    
    public LocalDate getNgaySinh() {
        return ngaySinh;
    }
    
    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
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
    
    public String getDiaChiThuongTru() {
        return diaChiThuongTru;
    }
    
    public void setDiaChiThuongTru(String diaChiThuongTru) {
        this.diaChiThuongTru = diaChiThuongTru;
    }
    
    public String getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
    
    public String getVaiTro() {
        return vaiTro;
    }
    
    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
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
    
    public Boolean getLaChuHo() {
        return laChuHo;
    }
    
    public void setLaChuHo(Boolean laChuHo) {
        this.laChuHo = laChuHo;
    }
    
    public String getQuanHeChuHo() {
        return quanHeChuHo;
    }
    
    public void setQuanHeChuHo(String quanHeChuHo) {
        this.quanHeChuHo = quanHeChuHo;
    }
}
