package BlueMoon.bluemoon.daos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import BlueMoon.bluemoon.entities.ThongBaoDaDoc;

@Repository
public interface ThongBaoDaDocDAO extends JpaRepository<ThongBaoDaDoc, Integer> {
    
    // Kiểm tra xem người này đã đọc thông báo này chưa
    boolean existsByThongBaoMaThongBaoAndNguoiDocCccd(Integer maThongBao, String cccd);

    // Đếm số người đã đọc 1 thông báo cụ thể (Dùng cho Admin)
    long countByThongBaoMaThongBao(Integer maThongBao);
}