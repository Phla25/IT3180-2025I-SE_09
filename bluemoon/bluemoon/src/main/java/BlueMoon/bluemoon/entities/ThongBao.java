package BlueMoon.bluemoon.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import BlueMoon.bluemoon.utils.ChuKyThongBao;
import BlueMoon.bluemoon.utils.NotificationType;
import BlueMoon.bluemoon.utils.RecipientType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "thong_bao")
@DynamicUpdate
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_thong_bao")
    private Integer maThongBao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_nguoi_gui", nullable = false)
    private DoiTuong nguoiGui; // Thường là Ban Quản Trị

    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;

    @Column(name = "noi_dung_thong_bao", columnDefinition = "TEXT", nullable = false)
    private String noiDung;
    
    @OneToMany(mappedBy = "thongBao", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PhanHoiThongBao> phanHoi = new ArrayList<>();


    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thong_bao", length = 30)
    private NotificationType loaiThongBao = NotificationType.binh_thuong; // NORMAL, URGENT, SYSTEM,...

    @Enumerated(EnumType.STRING)
    @Column(name = "doi_tuong_nhan", length = 30)
    private RecipientType doiTuongNhan = RecipientType.tat_ca; // ALL_RESIDENTS, MANAGER, ACCOUNTANT,...

    @Column(name = "thoi_gian_gui")
    private LocalDateTime thoiGianGui;

    @Column(name = "trang_thai_hien_thi")
    private Boolean trangThaiHienThi = true;

    // ==================== CÁC TRƯỜNG CHO THÔNG BÁO ĐỊNH KỲ ====================
    
    @Column(name = "la_dinh_ky")
    private Boolean laDinhKy = false; // true = thông báo định kỳ, false = thông báo thường
    
    @Enumerated(EnumType.STRING)
    @Column(name = "chu_ky", length = 20)
    private ChuKyThongBao chuKy; // HANG_TUAN, HANG_THANG, HANG_NAM
    
    @Column(name = "ngay_gui_tiep_theo")
    private LocalDateTime ngayGuiTiepTheo;
    
    @Column(name = "lan_gui_cuoi_cung")
    private LocalDateTime lanGuiCuoiCung;
    
    @Column(name = "trang_thai_hoat_dong")
    private Boolean trangThaiHoatDong = true; // Cho thông báo định kỳ
    
    // Cấu hình chu kỳ tuần
    @Column(name = "thu_trong_tuan") // 1=Chủ nhật, 2=Thứ 2, ..., 7=Thứ 7
    private Integer thuTrongTuan;
    
    // Cấu hình chu kỳ tháng
    @Column(name = "ngay_trong_thang") // 1-31
    private Integer ngayTrongThang;
    
    // Cấu hình chu kỳ năm
    @Column(name = "thang_trong_nam") // 1-12
    private Integer thangTrongNam;
    
    @Column(name = "ngay_trong_nam") // 1-31
    private Integer ngayTrongNam;
    
    // Thời gian gửi
    @Column(name = "gio_gui") // 0-23
    private Integer gioGui = 9; // Mặc định 9h sáng
    
    @Column(name = "phut_gui") // 0-59
    private Integer phutGui = 0; // Mặc định 0 phút

    // -------------------- Constructors --------------------

    public ThongBao() {
    }

    public ThongBao(DoiTuong nguoiGui, String tieuDe, String noiDung,
                    NotificationType loaiThongBao, RecipientType doiTuongNhan,
                    LocalDateTime thoiGianGui, Boolean trangThaiHienThi) {
        this.nguoiGui = nguoiGui;
        this.tieuDe = tieuDe;
        this.noiDung = noiDung;
        this.loaiThongBao = loaiThongBao;
        this.doiTuongNhan = doiTuongNhan;
        this.thoiGianGui = thoiGianGui;
        this.trangThaiHienThi = trangThaiHienThi;
    }

    // -------------------- Getter & Setter --------------------

    public Integer getMaThongBao() {
        return maThongBao;
    }

    public void setMaThongBao(Integer maThongBao) {
        this.maThongBao = maThongBao;
    }

    public DoiTuong getNguoiGui() {
        return nguoiGui;
    }

    public void setNguoiGui(DoiTuong nguoiGui) {
        this.nguoiGui = nguoiGui;
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

    public NotificationType getLoaiThongBao() {
        return loaiThongBao;
    }

    public void setLoaiThongBao(NotificationType loaiThongBao) {
        this.loaiThongBao = loaiThongBao;
    }

    public RecipientType getDoiTuongNhan() {
        return doiTuongNhan;
    }

    public void setDoiTuongNhan(RecipientType doiTuongNhan) {
        this.doiTuongNhan = doiTuongNhan;
    }

    public LocalDateTime getThoiGianGui() {
        return thoiGianGui;
    }

    public void setThoiGianGui(LocalDateTime thoiGianGui) {
        this.thoiGianGui = thoiGianGui;
    }

    public Boolean getTrangThaiHienThi() {
        return trangThaiHienThi;
    }

    public void setTrangThaiHienThi(Boolean trangThaiHienThi) {
        this.trangThaiHienThi = trangThaiHienThi;
    }

    // -------------------- Auto set thời gian gửi --------------------

    @PrePersist
    protected void onCreate() {
        if (this.thoiGianGui == null) {
            this.thoiGianGui = LocalDateTime.now();
        }
    }

    public List<PhanHoiThongBao> getPhanHoi() {
        return phanHoi;
    }

    public void setPhanHoi(List<PhanHoiThongBao> phanHoi) {
        this.phanHoi = phanHoi;
    }

    // -------------------- Getter & Setter cho Thông Báo Định Kỳ --------------------

    public Boolean getLaDinhKy() {
        return laDinhKy;
    }

    public void setLaDinhKy(Boolean laDinhKy) {
        this.laDinhKy = laDinhKy;
    }

    public ChuKyThongBao getChuKy() {
        return chuKy;
    }

    public void setChuKy(ChuKyThongBao chuKy) {
        this.chuKy = chuKy;
    }

    public LocalDateTime getNgayGuiTiepTheo() {
        return ngayGuiTiepTheo;
    }

    public void setNgayGuiTiepTheo(LocalDateTime ngayGuiTiepTheo) {
        this.ngayGuiTiepTheo = ngayGuiTiepTheo;
    }

    public LocalDateTime getLanGuiCuoiCung() {
        return lanGuiCuoiCung;
    }

    public void setLanGuiCuoiCung(LocalDateTime lanGuiCuoiCung) {
        this.lanGuiCuoiCung = lanGuiCuoiCung;
    }

    public Boolean getTrangThaiHoatDong() {
        return trangThaiHoatDong;
    }

    public void setTrangThaiHoatDong(Boolean trangThaiHoatDong) {
        this.trangThaiHoatDong = trangThaiHoatDong;
    }

    public Integer getThuTrongTuan() {
        return thuTrongTuan;
    }

    public void setThuTrongTuan(Integer thuTrongTuan) {
        this.thuTrongTuan = thuTrongTuan;
    }

    public Integer getNgayTrongThang() {
        return ngayTrongThang;
    }

    public void setNgayTrongThang(Integer ngayTrongThang) {
        this.ngayTrongThang = ngayTrongThang;
    }

    public Integer getThangTrongNam() {
        return thangTrongNam;
    }

    public void setThangTrongNam(Integer thangTrongNam) {
        this.thangTrongNam = thangTrongNam;
    }

    public Integer getNgayTrongNam() {
        return ngayTrongNam;
    }

    public void setNgayTrongNam(Integer ngayTrongNam) {
        this.ngayTrongNam = ngayTrongNam;
    }

    public Integer getGioGui() {
        return gioGui;
    }

    public void setGioGui(Integer gioGui) {
        this.gioGui = gioGui;
    }

    public Integer getPhutGui() {
        return phutGui;
    }

    public void setPhutGui(Integer phutGui) {
        this.phutGui = phutGui;
    }
}