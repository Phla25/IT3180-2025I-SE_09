package BlueMoon.bluemoon.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.utils.RecipientType;

@Repository
public interface ThongBaoDAO extends JpaRepository<ThongBao, Integer> {

    // Lấy tất cả (Dùng cho Admin)
    List<ThongBao> findAllByOrderByThoiGianGuiDesc();

    // Lấy tất cả kèm thông tin người gửi (Eager Load)
    @Query("SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findAllWithNguoiGuiEagerly();

    // ✨ MỚI: Lấy thông báo cho một cư dân cụ thể
    // Logic: (Gửi cho TẤT CẢ) HOẶC (Gửi riêng cho CCCD này)
    @Query("SELECT t FROM ThongBao t WHERE (t.doiTuongNhan = :typeAll OR t.nguoiNhan.cccd = :cccd) AND t.trangThaiHienThi = true ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findForResident(@Param("cccd") String cccd, @Param("typeAll") RecipientType typeAll);
}