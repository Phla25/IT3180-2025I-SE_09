package BlueMoon.bluemoon.daos;

import java.util.List;
import java.util.Optional; // Cần import

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Cần import
import org.springframework.data.repository.query.Param; // Cần import
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong; // Cần import
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Repository
public interface BaoCaoSuCoDAO extends JpaRepository<BaoCaoSuCo, Integer> {

    // 1. Tự động đếm số sự cố theo trạng thái (Giữ nguyên)
    Long countByTrangThai(IncidentStatus trangThai);

    // 2. Tự động đếm số sự cố theo mức độ ưu tiên (Giữ nguyên)
    Long countByMucDoUuTien(PriorityLevel mucDo);

    // 3. Tự động tìm kiếm theo mức độ ưu tiên (Giữ nguyên)
    List<BaoCaoSuCo> findByMucDoUuTien(PriorityLevel mucDo);
    
    // 4. Lấy tất cả sự cố theo thời gian báo cáo giảm dần (Giữ nguyên)
    List<BaoCaoSuCo> findAllByOrderByThoiGianBaoCaoDesc();

    // ==================== BỔ SUNG CHO TÍNH NĂNG RESIDENT ====================

    /**
     * 5. Bổ sung: Lấy danh sách sự cố đã báo cáo bởi một DoiTuong cụ thể, 
     * sắp xếp theo thời gian báo cáo giảm dần. Dùng cho giao diện cư dân.
     */
    List<BaoCaoSuCo> findByNguoiBaoCaoOrderByThoiGianBaoCaoDesc(DoiTuong nguoiBaoCao);

    /**
     * 6. Bổ sung: Lấy chi tiết sự cố theo ID, tải ngay (JOIN FETCH) các đối tượng 
     * người báo cáo và tài sản để tránh lỗi Lazy Loading.
     */
    @Query("SELECT sc FROM BaoCaoSuCo sc LEFT JOIN FETCH sc.nguoiBaoCao LEFT JOIN FETCH sc.taiSan WHERE sc.maBaoCao = :id")
    Optional<BaoCaoSuCo> findByIdWithDetails(@Param("id") Integer id);
}