package BlueMoon.bluemoon.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import BlueMoon.bluemoon.utils.InvoiceStatus;
import BlueMoon.bluemoon.utils.InvoiceType;
import BlueMoon.bluemoon.utils.UserRole;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "hoa_don")
@DynamicUpdate
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_hoa_don")
    private Integer maHoaDon;

    // Liên kết với hộ gia đình
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_ho", nullable = true)
    private HoGiaDinh hoGiaDinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_thanh_vien", referencedColumnName = "cccd") 
    private DoiTuong nguoiDangKyDichVu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_nguoi_thanh_toan", referencedColumnName = "cccd") // Cần thêm cột cccd_nguoi_thanh_toan vào DB
    private DoiTuong nguoiThanhToan;

    // Số tiền của hóa đơn
    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien = BigDecimal.ZERO;

    // Các liên kết khác (tùy loại hóa đơn)
    @Column(name = "ma_dich_vu")
    private Integer maDichVu;

    @Column(name = "ma_bao_cao")
    private Integer maBaoCao;

    @Column(name = "ma_tai_san")
    private Integer maTaiSan;

    // Loại hóa đơn (dịch vụ, sự cố, phí quản lý, v.v.)
    @Enumerated(EnumType.STRING)
    @Column(name = "loai_hoa_don", length = 30)
    private InvoiceType loaiHoaDon;

    // Trạng thái hóa đơn
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", length = 20)
    private InvoiceStatus trangThai = InvoiceStatus.chua_thanh_toan;

    // Ngày tạo hóa đơn
    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    // Ngày đến hạn thanh toán
    @Column(name = "han_thanh_toan")
    private LocalDate hanThanhToan;

    // Ngày thanh toán thực tế
    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    // Ghi chú hoặc mô tả chi tiết
    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    // Cập nhật thời gian sửa đổi
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    // === Constructors ===
    public HoaDon() {}

    public HoaDon(HoGiaDinh hoGiaDinh, BigDecimal soTien, InvoiceType loaiHoaDon) {
        this.hoGiaDinh = hoGiaDinh;
        this.soTien = soTien;
        this.loaiHoaDon = loaiHoaDon;
        this.trangThai = InvoiceStatus.chua_thanh_toan;
        this.ngayTao = LocalDateTime.now();
    }

    // === Getters & Setters ===
    public Integer getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(Integer maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public HoGiaDinh getHoGiaDinh() {
        return hoGiaDinh;
    }

    public void setHoGiaDinh(HoGiaDinh hoGiaDinh) {
        this.hoGiaDinh = hoGiaDinh;
    }

    public DoiTuong getNguoiDangKyDichVu(){
        return this.nguoiDangKyDichVu;
    }

    public void setNguoiDangKyDichVu(DoiTuong nguoiDangKyDichVu){
        this.nguoiDangKyDichVu = nguoiDangKyDichVu;
    }

    public DoiTuong getNguoiThanhToan(){
        return this.nguoiThanhToan;
    }

    public void setNguoiThanhToan(DoiTuong nguoiThanhToan){
        this.nguoiThanhToan = nguoiThanhToan;
    }

    public BigDecimal getSoTien() {
        return soTien;
    }

    public void setSoTien(BigDecimal soTien) {
        this.soTien = soTien;
    }

    public Integer getMaDichVu() {
        return maDichVu;
    }

    public void setMaDichVu(Integer maDichVu) {
        this.maDichVu = maDichVu;
    }

    public Integer getMaBaoCao() {
        return maBaoCao;
    }

    public void setMaBaoCao(Integer maBaoCao) {
        this.maBaoCao = maBaoCao;
    }

    public Integer getMaTaiSan() {
        return maTaiSan;
    }

    public void setMaTaiSan(Integer maTaiSan) {
        this.maTaiSan = maTaiSan;
    }

    public InvoiceType getLoaiHoaDon() {
        return loaiHoaDon;
    }

    public void setLoaiHoaDon(InvoiceType loaiHoaDon) {
        this.loaiHoaDon = loaiHoaDon;
    }

    public InvoiceStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(InvoiceStatus trangThai) {
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

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    // Cập nhật tự động ngày sửa đổi
    @PreUpdate
    public void preUpdate() {
        this.ngayCapNhat = LocalDateTime.now();
    }
    @Transient
    public boolean isPhieuChi() {
        return this.nguoiDangKyDichVu != null && 
               (this.nguoiDangKyDichVu.getVaiTro() == UserRole.ban_quan_tri || 
                this.nguoiDangKyDichVu.getVaiTro() == UserRole.ke_toan);
    }

    @Transient
    public String getLoaiGiaoDichHienThi() {
        return isPhieuChi() ? "PHIẾU CHI" : "PHIẾU THU";
    }
}
