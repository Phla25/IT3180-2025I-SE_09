package BlueMoon.bluemoon.daos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.utils.ChuKyThongBao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@Repository
public class ThongBaoDAO {
    
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
    public ThongBao save(ThongBao thongBao) {
        if (thongBao.getMaThongBao() == null) {
            entityManager.persist(thongBao);
            return thongBao;
        } else {
            return entityManager.merge(thongBao);
        }
    }
    
    /**
     * Tìm Entity bằng Khóa chính (maThongBao).
     */
    public Optional<ThongBao> findById(Integer maThongBao) {
        ThongBao thongBao = entityManager.find(ThongBao.class, maThongBao);
        return Optional.ofNullable(thongBao);
    }
    
    /**
     * Xóa Entity.
     */
    @Transactional
    public void delete(ThongBao thongBao) {
        if (entityManager.contains(thongBao)) {
            entityManager.remove(thongBao);
        } else {
            entityManager.remove(entityManager.merge(thongBao));
        }
    }
    
    /**
     * Tìm tất cả thông báo, sắp xếp theo thời gian gửi giảm dần.
     */
    public List<ThongBao> findAllByOrderByThoiGianGuiDesc() {
        String jpql = "SELECT t FROM ThongBao t ORDER BY t.thoiGianGui DESC";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        return query.getResultList();
    }
    
    /**
     * Lấy tất cả thông báo với JOIN FETCH nguoiGui (DoiTuong).
     */
    public List<ThongBao> findAllWithNguoiGuiEagerly() {
        String jpql = "SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui ORDER BY t.thoiGianGui DESC";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        return query.getResultList();
    }
    
    // ==================== QUERY CHO THÔNG BÁO ĐỊNH KỲ ====================
    
    /**
     * Lấy tất cả thông báo định kỳ với JOIN FETCH nguoiGui.
     */
    public List<ThongBao> findAllDinhKyWithNguoiGuiEagerly() {
        String jpql = "SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui WHERE t.laDinhKy = true ORDER BY t.thoiGianGui DESC";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        return query.getResultList();
    }
    
    /**
     * Lấy thông báo định kỳ đang hoạt động.
     */
    public List<ThongBao> findByLaDinhKyAndTrangThaiHoatDongOrderByThoiGianGuiDesc(Boolean laDinhKy, Boolean trangThaiHoatDong) {
        String jpql = "SELECT t FROM ThongBao t WHERE t.laDinhKy = :laDinhKy AND t.trangThaiHoatDong = :trangThaiHoatDong ORDER BY t.thoiGianGui DESC";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        query.setParameter("laDinhKy", laDinhKy);
        query.setParameter("trangThaiHoatDong", trangThaiHoatDong);
        return query.getResultList();
    }
    
    /**
     * Lấy thông báo định kỳ theo chu kỳ.
     */
    public List<ThongBao> findByLaDinhKyAndChuKyAndTrangThaiHoatDongOrderByThoiGianGuiDesc(Boolean laDinhKy, ChuKyThongBao chuKy, Boolean trangThaiHoatDong) {
        String jpql = "SELECT t FROM ThongBao t WHERE t.laDinhKy = :laDinhKy AND t.chuKy = :chuKy AND t.trangThaiHoatDong = :trangThaiHoatDong ORDER BY t.thoiGianGui DESC";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        query.setParameter("laDinhKy", laDinhKy);
        query.setParameter("chuKy", chuKy);
        query.setParameter("trangThaiHoatDong", trangThaiHoatDong);
        return query.getResultList();
    }
    
    /**
     * Tìm các thông báo định kỳ cần gửi.
     */
    public List<ThongBao> findThongBaoDinhKyCanGui(LocalDateTime now) {
        String jpql = "SELECT t FROM ThongBao t WHERE t.laDinhKy = true AND t.trangThaiHoatDong = true AND t.ngayGuiTiepTheo <= :now";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        query.setParameter("now", now);
        return query.getResultList();
    }
    
    /**
     * Đếm số thông báo định kỳ đang hoạt động.
     */
    public long countByLaDinhKyAndTrangThaiHoatDong(Boolean laDinhKy, Boolean trangThaiHoatDong) {
        String jpql = "SELECT COUNT(t) FROM ThongBao t WHERE t.laDinhKy = :laDinhKy AND t.trangThaiHoatDong = :trangThaiHoatDong";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("laDinhKy", laDinhKy);
        query.setParameter("trangThaiHoatDong", trangThaiHoatDong);
        Long count = query.getSingleResult();
        return count != null ? count : 0L;
    }
    
    /**
     * Lấy thông báo thường (không phải định kỳ) với JOIN FETCH nguoiGui.
     */
    public List<ThongBao> findAllThuongWithNguoiGuiEagerly() {
        String jpql = "SELECT t FROM ThongBao t JOIN FETCH t.nguoiGui WHERE t.laDinhKy = false OR t.laDinhKy IS NULL ORDER BY t.thoiGianGui DESC";
        TypedQuery<ThongBao> query = entityManager.createQuery(jpql, ThongBao.class);
        return query.getResultList();
    }
}
