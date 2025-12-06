package BlueMoon.bluemoon.daos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.PhanHoi;

@Repository
public interface PhanHoiDAO extends JpaRepository<PhanHoi, Integer> {
    // Lấy danh sách phản hồi của một sự cố, sắp xếp từ cũ đến mới
    List<PhanHoi> findByBaoCaoSuCoOrderByThoiGianPhanHoiAsc(BaoCaoSuCo suCo);
 // Tìm tất cả phản hồi của 1 báo cáo, sắp xếp mới nhất lên đầu
    List<PhanHoi> findByBaoCaoSuCoMaBaoCaoOrderByThoiGianPhanHoiDesc(Integer maBaoCao);
}