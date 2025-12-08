package BlueMoon.bluemoon.models; // Đặt trong thư mục models/dto nếu bạn có

import java.time.LocalDateTime;

import BlueMoon.bluemoon.entities.ThongBao;

public class ThongBaoDTO {
    
    private Integer maThongBao;
    private String tieuDe;
    private String noiDung;
    private LocalDateTime thoiGianGui;
    private boolean daDoc; // Cho cư dân
    private long soNguoiChuaDoc; // Cho Admin
    
    // -------------------- Constructors --------------------
    
    public ThongBaoDTO() {
    }

    // Constructor để dễ dàng chuyển đổi từ Entity
    public ThongBaoDTO(ThongBao tb) {
        this.maThongBao = tb.getMaThongBao();
        this.tieuDe = tb.getTieuDe();
        this.noiDung = tb.getNoiDung();
        this.thoiGianGui = tb.getThoiGianGui();
    }

    // -------------------- Getters và Setters --------------------
    // Bạn phải có đầy đủ Getters và Setters cho các trường trên
    
    public Integer getMaThongBao() {
        return maThongBao;
    }

    public void setMaThongBao(Integer maThongBao) {
        this.maThongBao = maThongBao;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public LocalDateTime getThoiGianGui() {
        return thoiGianGui;
    }

    public void setThoiGianGui(LocalDateTime thoiGianGui) {
        this.thoiGianGui = thoiGianGui;
    }

    public long getSoNguoiChuaDoc() {
        return soNguoiChuaDoc;
    }

    public void setSoNguoiChuaDoc(long soNguoiChuaDoc) {
        this.soNguoiChuaDoc = soNguoiChuaDoc;
    }

    public boolean isDaDoc() {
        return daDoc;
    }

    public void setDaDoc(boolean daDoc) {
        this.daDoc = daDoc;
    }
}