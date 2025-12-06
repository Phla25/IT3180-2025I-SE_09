package BlueMoon.bluemoon.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.ThongBao;

@Repository
public interface ThongBaoDAO extends JpaRepository<ThongBao, Integer> {
    List<ThongBao> findAllByOrderByThoiGianGuiDesc();
 // Dùng JPQL để JOIN FETCH nguoiGui (DoiTuong)
    @Query("SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findAllWithNguoiGuiEagerly();
    @Query("SELECT COUNT(t) FROM ThongBao t WHERE NOT EXISTS " +
            "(SELECT d FROM ThongBaoDaDoc d WHERE d.thongBao = t AND d.nguoiDoc = :nguoiDoc)")
     long countUnreadByNguoiDoc(@Param("nguoiDoc") DoiTuong nguoiDoc);
}