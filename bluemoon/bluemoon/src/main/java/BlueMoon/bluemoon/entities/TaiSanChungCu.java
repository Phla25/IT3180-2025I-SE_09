package BlueMoon.bluemoon.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import BlueMoon.bluemoon.utils.AssetStatus;
import BlueMoon.bluemoon.utils.AssetType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tai_san_chung_cu")
@DynamicUpdate
public class TaiSanChungCu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tai_san")
    private Integer maTaiSan;

    @Column(name = "ten_tai_san", nullable = false, length = 100)
    private String tenTaiSan;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_tai_san", nullable = false, length = 30)
    private AssetType loaiTaiSan;  // CĂN_HỘ, CHỖ_ĐỖ_XE, KHO, THIẾT_BỊ,...

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", length = 20)
    private AssetStatus trangThai;

    @Column(name = "dien_tich", precision = 10, scale = 2)
    private BigDecimal dienTich;

    @Column(name = "vi_tri", columnDefinition = "TEXT")
    private String viTri;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "gia_tri", precision = 15, scale = 2)
    private BigDecimal giaTri;

    @Column(name = "ngay_them")
    private LocalDateTime ngayThem;

    // 🔹 Mỗi tài sản thuộc một hộ (hoặc null nếu chưa đăng ký)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho")
    private HoGiaDinh hoGiaDinh;

    @PrePersist
    public void prePersist() {
        this.ngayThem = LocalDateTime.now();
    }

    // ==== Constructors ====
    public TaiSanChungCu() {}

    public TaiSanChungCu(String tenTaiSan, AssetType loaiTaiSan, BigDecimal dienTich, BigDecimal giaTri, String viTri, HoGiaDinh hoGiaDinh) {
        this.tenTaiSan = tenTaiSan;
        this.loaiTaiSan = loaiTaiSan;
        this.dienTich = dienTich;
        this.giaTri = giaTri;
        this.viTri = viTri;
        this.hoGiaDinh = hoGiaDinh;
        this.trangThai = AssetStatus.hoat_dong; // Mặc định khi tạo là hoạt động
    }

    // ==== Getters & Setters ====
    public Integer getMaTaiSan() { return maTaiSan; }
    public void setMaTaiSan(Integer maTaiSan) { this.maTaiSan = maTaiSan; }

    public String getTenTaiSan() { return tenTaiSan; }
    public void setTenTaiSan(String tenTaiSan) { this.tenTaiSan = tenTaiSan; }

    public AssetType getLoaiTaiSan() { return loaiTaiSan; }
    public void setLoaiTaiSan(AssetType loaiTaiSan) { this.loaiTaiSan = loaiTaiSan; }

    public AssetStatus getTrangThai() { return trangThai; }
    public void setTrangThai(AssetStatus trangThai) { this.trangThai = trangThai; }

    public BigDecimal getDienTich() { return dienTich; }
    public void setDienTich(BigDecimal dienTich) { this.dienTich = dienTich; }

    public String getViTri() { return viTri; }
    public void setViTri(String viTri) { this.viTri = viTri; }

    public BigDecimal getGiaTri() { return giaTri; }
    public void setGiaTri(BigDecimal giaTri) { this.giaTri = giaTri; }

    public LocalDateTime getNgayThem() { return ngayThem; }
    public void setNgayThem(LocalDateTime ngayThem) { this.ngayThem = ngayThem; }

    public HoGiaDinh getHoGiaDinh() { return hoGiaDinh; }
    public void setHoGiaDinh(HoGiaDinh hoGiaDinh) { this.hoGiaDinh = hoGiaDinh; }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
}
