package BlueMoon.bluemoon.daos;

import BlueMoon.bluemoon.entities.LichSuRaVao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LichSuRaVaoDAO extends JpaRepository<LichSuRaVao, Integer> {

    // 1. Giữ nguyên
    List<LichSuRaVao> findByCuDan_CccdAndThoiGianBetweenOrderByThoiGianDesc(
            String cccd, LocalDateTime start, LocalDateTime end);

    // 2. SỬA LẠI QUERY (Đơn giản hóa)
    // Lưu ý: Dùng lower() cho trường trong DB, còn tham số :keyword đã được lower ở Service
    @Query("SELECT l FROM LichSuRaVao l WHERE " +
           "(:keyword IS NULL OR lower(l.cuDan.hoVaTen) LIKE :keyword OR l.cuDan.cccd LIKE :keyword) AND " +
           "(l.thoiGian BETWEEN :startDate AND :endDate) AND " +
           "(:gate IS NULL OR l.congKiemSoat = :gate) " +
           "ORDER BY l.thoiGian DESC")
    List<LichSuRaVao> customFilter(@Param("keyword") String keyword,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("gate") String gate);

    // 3. Giữ nguyên
    @Query("SELECT DISTINCT l.congKiemSoat FROM LichSuRaVao l WHERE l.congKiemSoat IS NOT NULL")
    List<String> findDistinctCongKiemSoat();
}