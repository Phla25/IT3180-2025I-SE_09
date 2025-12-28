package BlueMoon.bluemoon.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "phan_hoi")
@DynamicUpdate
public class PhanHoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phan_hoi")
    private Integer maPhanHoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_nguoi_dung", nullable = false)
    private DoiTuong nguoiDung; // Người phản hồi (BQT, Kế toán,...)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_bao_cao", nullable = false)
    private BaoCaoSuCo baoCaoSuCo; // Liên kết với sự cố

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_nguoi_phan_hoi", nullable = false)
    private DoiTuong nguoiPhanHoi; // Người phản hồi (BQT, Kế toán,...)

    @Column(name = "noi_dung_phan_hoi", columnDefinition = "TEXT", nullable = false)
    private String noiDung;

    @Column(name = "thoi_gian_phan_hoi")
    private LocalDateTime thoiGianPhanHoi;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    // -------------------- Constructors --------------------

    public PhanHoi() {
    }

    public PhanHoi(BaoCaoSuCo baoCaoSuCo, DoiTuong nguoiPhanHoi,
                   String noiDung, LocalDateTime thoiGianPhanHoi, String ghiChu) {
        this.baoCaoSuCo = baoCaoSuCo;
        this.nguoiPhanHoi = nguoiPhanHoi;
        this.noiDung = noiDung;
        this.thoiGianPhanHoi = thoiGianPhanHoi;
        this.ghiChu = ghiChu;
    }

    // -------------------- Getter & Setter --------------------

    public Integer getMaPhanHoi() {
        return maPhanHoi;
    }

    public void setMaPhanHoi(Integer maPhanHoi) {
        this.maPhanHoi = maPhanHoi;
    }

    public BaoCaoSuCo getBaoCaoSuCo() {
        return baoCaoSuCo;
    }

    public void setBaoCaoSuCo(BaoCaoSuCo baoCaoSuCo) {
        this.baoCaoSuCo = baoCaoSuCo;
    }

    public DoiTuong getNguoiPhanHoi() {
        return nguoiPhanHoi;
    }

    public void setNguoiPhanHoi(DoiTuong nguoiPhanHoi) {
        this.nguoiPhanHoi = nguoiPhanHoi;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public LocalDateTime getThoiGianPhanHoi() {
        return thoiGianPhanHoi;
    }

    public void setThoiGianPhanHoi(LocalDateTime thoiGianPhanHoi) {
        this.thoiGianPhanHoi = thoiGianPhanHoi;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    // -------------------- Auto set thời gian phản hồi --------------------

    @PrePersist
    protected void onCreate() {
        if (this.thoiGianPhanHoi == null) {
            this.thoiGianPhanHoi = LocalDateTime.now();
        }
    }

    public DoiTuong getNguoiDung() {
        return nguoiDung;
    }

    public void setNguoiDung(DoiTuong nguoiDung) {
        this.nguoiDung = nguoiDung;
    }
}
