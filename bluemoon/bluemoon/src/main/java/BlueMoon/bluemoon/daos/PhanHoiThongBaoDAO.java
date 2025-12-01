package BlueMoon.bluemoon.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.PhanHoiThongBao;

@Repository
public interface PhanHoiThongBaoDAO extends JpaRepository<PhanHoiThongBao, Integer> {
	List<PhanHoiThongBao> findByThongBaoMaThongBaoOrderByThoiGianGuiAsc(Integer maThongBao);
	// Trong PhanHoiThongBaoDAO.java
	// Import cần thiết: org.springframework.data.jpa.repository.Query;

	@Query("SELECT pr FROM PhanHoiThongBao pr JOIN FETCH pr.nguoiGui WHERE pr.thongBao.maThongBao = :maThongBao ORDER BY pr.thoiGianGui ASC")
	List<PhanHoiThongBao> findByThongBaoMaThongBaoWithNguoiGuiEagerly(@Param("maThongBao") Integer maThongBao);
}