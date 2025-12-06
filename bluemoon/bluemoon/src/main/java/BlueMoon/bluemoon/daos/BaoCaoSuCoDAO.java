package BlueMoon.bluemoon.daos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Repository
public interface BaoCaoSuCoDAO extends JpaRepository<BaoCaoSuCo, Integer> {

    Long countByTrangThai(IncidentStatus trangThai);
    Long countByMucDoUuTien(PriorityLevel mucDo);
    List<BaoCaoSuCo> findByMucDoUuTien(PriorityLevel mucDo);
    List<BaoCaoSuCo> findAllByOrderByThoiGianBaoCaoDesc();
    
    List<BaoCaoSuCo> findByNguoiBaoCaoOrderByThoiGianBaoCaoDesc(DoiTuong nguoiBaoCao);

    // JOIN FETCH để tránh lỗi Lazy Loading khi xem chi tiết
    @Query("SELECT sc FROM BaoCaoSuCo sc LEFT JOIN FETCH sc.nguoiBaoCao LEFT JOIN FETCH sc.taiSan WHERE sc.maBaoCao = :id")
    Optional<BaoCaoSuCo> findByIdWithDetails(@Param("id") Integer id);

    // [MỚI] Tìm kiếm động: Lọc ngay tại Database thay vì Java Stream
    @Query("SELECT sc FROM BaoCaoSuCo sc " +
           "WHERE (:trangThai IS NULL OR sc.trangThai = :trangThai) " +
           "AND (:mucDo IS NULL OR sc.mucDoUuTien = :mucDo) " +
           "ORDER BY sc.thoiGianBaoCao DESC")
    List<BaoCaoSuCo> filterIncidents(@Param("trangThai") IncidentStatus trangThai, 
                                     @Param("mucDo") PriorityLevel mucDo);
    
 // [ĐÃ SỬA LỖI LOWER(BYTEA)]
    // Sử dụng CAST(sc.noiDung AS string) để ép kiểu dữ liệu nhị phân sang chuỗi trước khi tìm kiếm
 // [SỬA LẠI: BỎ CAST VÌ DB ĐÃ LÀ TEXT]
    @Query("SELECT sc FROM BaoCaoSuCo sc WHERE " +
    	       "(:keyword IS NULL OR LOWER(sc.tieuDe) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(sc.noiDung) LIKE LOWER(CONCAT('%', :keyword, '%'))) " + // Đã bỏ CAST
    	       "AND (:assetName IS NULL OR LOWER(sc.taiSan.tenTaiSan) LIKE LOWER(CONCAT('%', :assetName, '%'))) " +
    	       "AND (:trangThai IS NULL OR sc.trangThai = :trangThai) " +
    	       "AND (:mucDo IS NULL OR sc.mucDoUuTien = :mucDo) " +
    	       "AND (:fromDate IS NULL OR sc.thoiGianBaoCao >= :fromDate) " +
    	       "AND (:toDate IS NULL OR sc.thoiGianBaoCao <= :toDate) " +
    	       "ORDER BY sc.thoiGianBaoCao DESC")
    	List<BaoCaoSuCo> searchIncidents(
    	        @Param("keyword") String keyword,
    	        @Param("assetName") String assetName,
    	        @Param("trangThai") IncidentStatus trangThai,
    	        @Param("mucDo") PriorityLevel mucDo,
    	        @Param("fromDate") LocalDateTime fromDate,
    	        @Param("toDate") LocalDateTime toDate
    	);
}