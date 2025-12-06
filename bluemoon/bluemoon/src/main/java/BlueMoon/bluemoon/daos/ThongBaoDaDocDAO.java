package BlueMoon.bluemoon.daos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.entities.ThongBaoDaDoc;

@Repository
public interface ThongBaoDaDocDAO extends JpaRepository<ThongBaoDaDoc, Integer> {
    
    // Kiểm tra xem người này đã đọc thông báo này chưa
    boolean existsByThongBaoAndNguoiDoc(ThongBao thongBao, DoiTuong nguoiDoc);

    // Đếm số lượng người đã đọc
    long countByThongBao(ThongBao thongBao);

    // Lấy danh sách người đã đọc (kèm thông tin người đọc)
    @Query("SELECT tdd FROM ThongBaoDaDoc tdd JOIN FETCH tdd.nguoiDoc WHERE tdd.thongBao.maThongBao = :maThongBao ORDER BY tdd.thoiGianDoc DESC")
    List<ThongBaoDaDoc> findByThongBaoIdWithUser(@Param("maThongBao") Integer maThongBao);
}