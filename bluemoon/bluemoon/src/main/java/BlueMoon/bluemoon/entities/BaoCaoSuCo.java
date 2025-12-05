package BlueMoon.bluemoon.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bao_cao_su_co")
@DynamicUpdate
public class BaoCaoSuCo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_bao_cao")
    private Integer maBaoCao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_nguoi_bao_cao", nullable = false)
    private DoiTuong nguoiBaoCao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_tai_san", nullable = false)
    private TaiSanChungCu taiSan;

    @Column(name = "tieu_de", length = 255)
    private String tieuDe;

    @Column(name = "mo_ta_su_co", columnDefinition = "TEXT")
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do_uu_tien", length = 20)
    private PriorityLevel mucDoUuTien = PriorityLevel.binh_thuong;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", length = 20)
    private IncidentStatus trangThai = IncidentStatus.moi_tiep_nhan;

    @Column(name = "thoi_gian_bao_cao")
    private LocalDateTime thoiGianBaoCao = LocalDateTime.now();

    @Column(name = "thoi_gian_cap_nhat")
    private LocalDateTime thoiGianCapNhat;

    @Column(name = "chi_phi_xu_ly", precision = 15, scale = 2)
    private BigDecimal chiPhiXuLy = BigDecimal.ZERO;

    // ✅ Constructors
    public BaoCaoSuCo() {
    }

    public BaoCaoSuCo(DoiTuong nguoiBaoCao, TaiSanChungCu taiSan, String tieuDe, String noiDung,
            PriorityLevel mucDoUuTien, IncidentStatus trangThai) {
        this.nguoiBaoCao = nguoiBaoCao;
        this.taiSan = taiSan;
        this.tieuDe = tieuDe;
        this.noiDung = noiDung;
        this.mucDoUuTien = mucDoUuTien;
        this.trangThai = trangThai;
    }

    // ✅ Getters & Setters
    public Integer getMaBaoCao() {
        return maBaoCao;
    }

    public void setMaBaoCao(Integer maBaoCao) {
        this.maBaoCao = maBaoCao;
    }

    public DoiTuong getNguoiBaoCao() {
        return nguoiBaoCao;
    }

    public void setNguoiBaoCao(DoiTuong nguoiBaoCao) {
        this.nguoiBaoCao = nguoiBaoCao;
    }

    public TaiSanChungCu getTaiSan() {
        return taiSan;
    }

    public void setTaiSan(TaiSanChungCu taiSan) {
        this.taiSan = taiSan;
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

    public PriorityLevel getMucDoUuTien() {
        return mucDoUuTien;
    }

    public void setMucDoUuTien(PriorityLevel mucDoUuTien) {
        this.mucDoUuTien = mucDoUuTien;
    }

    public IncidentStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(IncidentStatus trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getThoiGianBaoCao() {
        return thoiGianBaoCao;
    }

    public void setThoiGianBaoCao(LocalDateTime thoiGianBaoCao) {
        this.thoiGianBaoCao = thoiGianBaoCao;
    }

    public LocalDateTime getThoiGianCapNhat() {
        return thoiGianCapNhat;
    }

    public void setThoiGianCapNhat(LocalDateTime thoiGianCapNhat) {
        this.thoiGianCapNhat = thoiGianCapNhat;
    }

    public BigDecimal getChiPhiXuLy() {
        return chiPhiXuLy;
    }

    public void setChiPhiXuLy(BigDecimal chiPhiXuLy) {
        this.chiPhiXuLy = chiPhiXuLy;
    }
}
