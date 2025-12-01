package BlueMoon.bluemoon.models;



import java.time.LocalDateTime;

import BlueMoon.bluemoon.entities.PhanHoiThongBao;

public class PhanHoiThongBaoDTO {
    private String noiDung;
    private final LocalDateTime thoiGianGui;
    private final String hoVaTenNguoiGui;
    private final String vaiTroNguoiGui;

    public PhanHoiThongBaoDTO(PhanHoiThongBao ph) {
        this.noiDung = ph.getNoiDung();
        this.thoiGianGui = ph.getThoiGianGui();
        this.hoVaTenNguoiGui = ph.getNguoiGui().getHoVaTen();
        this.vaiTroNguoiGui = ph.getNguoiGui().getVaiTro().toString();
    }
    
    public String getNoiDung() {
        return noiDung;
    }

    public LocalDateTime getThoiGianGui() {
        return thoiGianGui;
    }

    public String getHoVaTenNguoiGui() {
        return hoVaTenNguoiGui;
    }

    public String getVaiTroNguoiGui() {
        return vaiTroNguoiGui;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }
}