package BlueMoon.bluemoon.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.PhanHoiThongBao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@Repository
public class PhanHoiThongBaoDAO {
    
    @Autowired
    @PersistenceContext
    private EntityManager entityManager;
    
    // =======================================================
    // 1. CÁC PHƯƠNG THỨC CRUD CƠ BẢN
    // =======================================================
    
    /**
     * Lưu (hoặc cập nhật) một Entity.
     */
    @Transactional
    public PhanHoiThongBao save(PhanHoiThongBao phanHoi) {
        if (phanHoi.getMaPhanHoi() == null) {
            entityManager.persist(phanHoi);
            return phanHoi;
        } else {
            return entityManager.merge(phanHoi);
        }
    }
    
    /**
     * Tìm Entity bằng Khóa chính (maPhanHoi).
     */
    public Optional<PhanHoiThongBao> findById(Integer maPhanHoi) {
        PhanHoiThongBao phanHoi = entityManager.find(PhanHoiThongBao.class, maPhanHoi);
        return Optional.ofNullable(phanHoi);
    }
    
    /**
     * Xóa Entity.
     */
    @Transactional
    public void delete(PhanHoiThongBao phanHoi) {
        if (entityManager.contains(phanHoi)) {
            entityManager.remove(phanHoi);
        } else {
            entityManager.remove(entityManager.merge(phanHoi));
        }
    }
    
    /**
     * Tìm tất cả phản hồi theo mã thông báo, sắp xếp theo thời gian gửi tăng dần.
     */
    public List<PhanHoiThongBao> findByThongBaoMaThongBaoOrderByThoiGianGuiAsc(Integer maThongBao) {
        String jpql = "SELECT pr FROM PhanHoiThongBao pr WHERE pr.thongBao.maThongBao = :maThongBao ORDER BY pr.thoiGianGui ASC";
        TypedQuery<PhanHoiThongBao> query = entityManager.createQuery(jpql, PhanHoiThongBao.class);
        query.setParameter("maThongBao", maThongBao);
        return query.getResultList();
    }
    
    /**
     * Tìm tất cả phản hồi theo mã thông báo với JOIN FETCH nguoiGui, sắp xếp theo thời gian gửi tăng dần.
     */
    public List<PhanHoiThongBao> findByThongBaoMaThongBaoWithNguoiGuiEagerly(Integer maThongBao) {
        String jpql = "SELECT pr FROM PhanHoiThongBao pr JOIN FETCH pr.nguoiGui WHERE pr.thongBao.maThongBao = :maThongBao ORDER BY pr.thoiGianGui ASC";
        TypedQuery<PhanHoiThongBao> query = entityManager.createQuery(jpql, PhanHoiThongBao.class);
        query.setParameter("maThongBao", maThongBao);
        return query.getResultList();
    }
}
