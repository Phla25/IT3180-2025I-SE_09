package BlueMoon.bluemoon.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.ThongBao;

@Repository
public interface ThongBaoDAO extends JpaRepository<ThongBao, Integer> {
    List<ThongBao> findAllByOrderByThoiGianGuiDesc();
 // Dùng JPQL để JOIN FETCH nguoiGui (DoiTuong)
    @Query("SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui ORDER BY t.thoiGianGui DESC")
    List<ThongBao> findAllWithNguoiGuiEagerly();
}