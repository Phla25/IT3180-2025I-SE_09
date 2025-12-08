// File: src/main/java/BlueMoon/bluemoon/entities/LichSuRaVao.java
package BlueMoon.bluemoon.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import BlueMoon.bluemoon.utils.EntryExitType;

@Entity
@Table(name = "lich_su_ra_vao")
public class LichSuRaVao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ma_cu_dan", referencedColumnName = "cccd")
    private DoiTuong cuDan;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_hoat_dong") // VAO hoặc RA
    private EntryExitType loaiHoatDong; 
    
    @Column(name = "cong_kiem_soat") // Ví dụ: Cổng A, Cổng B, Thang máy T1
    private String congKiemSoat;

    @Column(name = "hinh_anh_checkin") // Đường dẫn ảnh chụp camera (nếu có)
    private String hinhAnh;

    // Getters and Setters
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public DoiTuong getCuDan() {
        return cuDan;
    }
    public void setCuDan(DoiTuong cuDan) {
        this.cuDan = cuDan;
    }
    public LocalDateTime getThoiGian() {
        return thoiGian;
    }
    public void setThoiGian(LocalDateTime thoiGian) {
        this.thoiGian = thoiGian;
    }
    public EntryExitType getLoaiHoatDong() {
        return loaiHoatDong;
    }
    public void setLoaiHoatDong(EntryExitType loaiHoatDong) {
        this.loaiHoatDong = loaiHoatDong;
    }
    public String getCongKiemSoat() {
        return congKiemSoat;
    }
    public void setCongKiemSoat(String congKiemSoat) {
        this.congKiemSoat = congKiemSoat;
    }
    public String getHinhAnh() {
        return hinhAnh;
    }
    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }
}