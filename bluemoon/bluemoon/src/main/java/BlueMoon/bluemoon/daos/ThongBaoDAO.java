package BlueMoon.bluemoon.daos;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.utils.ChuKyThongBao;

@Repository
public interface ThongBaoDAO extends JpaRepository<ThongBao, Integer> {
    List<ThongBao> findAllByOrderByThoiGianGuiDesc();
    
    // Dùng JPQL để JOIN FETCH nguoiGui (DoiTuong)
    @Query("SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findAllWithNguoiGuiEagerly();
    
    // ==================== QUERY CHO THÔNG BÁO ĐỊNH KỲ ====================
    
    // Lấy tất cả thông báo định kỳ
    @Query("SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui WHERE t.laDinhKy = true ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findAllDinhKyWithNguoiGuiEagerly();
    
    // Lấy thông báo định kỳ đang hoạt động
    List<ThongBao> findByLaDinhKyAndTrangThaiHoatDongOrderByThoiGianGuiDesc(Boolean laDinhKy, Boolean trangThaiHoatDong);
    
    // Lấy thông báo định kỳ theo chu kỳ
    List<ThongBao> findByLaDinhKyAndChuKyAndTrangThaiHoatDongOrderByThoiGianGuiDesc(Boolean laDinhKy, ChuKyThongBao chuKy, Boolean trangThaiHoatDong);
    
    // Tìm các thông báo định kỳ cần gửi
    @Query("SELECT t FROM ThongBao t WHERE t.laDinhKy = true AND t.trangThaiHoatDong = true AND t.ngayGuiTiepTheo <= :now")
    List<ThongBao> findThongBaoDinhKyCanGui(@Param("now") LocalDateTime now);
    
    // Đếm số thông báo định kỳ đang hoạt động
    long countByLaDinhKyAndTrangThaiHoatDong(Boolean laDinhKy, Boolean trangThaiHoatDong);
    
    // Lấy thông báo thường (không phải định kỳ)
    @Query("SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui WHERE t.laDinhKy = false OR t.laDinhKy IS NULL ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findAllThuongWithNguoiGuiEagerly();
}