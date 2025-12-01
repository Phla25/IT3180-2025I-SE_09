package BlueMoon.bluemoon.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.DangKyDichVu;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@Repository
public class DangKyDichVuDAO {
    @Autowired
    private EntityManager entityManager;

    /**
     * Đếm tổng số dịch vụ đăng ký bởi cư dân trong hệ thống
     */
    public Long countAll() {
        String jpql = "SELECT COUNT(dkdv) FROM DangKyDichVu dkdv";
        Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return count != null ? count : 0;
    }
    /** 
     * Đếm Tổng số dịch vụ đăng ký bởi cư dân cụ thể
     */
    public Long countByCccd(String cccd) {
        String jpql = "SELECT COUNT(dkdv) FROM DangKyDichVu dkdv WHERE dkdv.nguoiDung.cccd = :cccd";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("cccd", cccd)
                .getSingleResult();
        return count != null ? count : 0;
    }
    public DangKyDichVu save(DangKyDichVu dkdv){
        if (dkdv.getMaDangKy() == null) {
            entityManager.persist(dkdv);
            return dkdv;
        } else {
            // Cập nhật
            return entityManager.merge(dkdv);
        }
    }
    public List<DangKyDichVu> findAllByNguoiDungCccd(String cccd) {
        String jpql = "SELECT dkdv FROM DangKyDichVu dkdv WHERE dkdv.nguoiDung.cccd =: cccd";
        try {
            return (List<DangKyDichVu>) entityManager.createQuery(jpql, DangKyDichVu.class)
                                .setParameter("cccd", cccd).getResultList();
        } catch (NoResultException e){
            return null;
        }
    }

    public Optional<DangKyDichVu> findById(Integer maDangKy) {
        DangKyDichVu dkdv = entityManager.find(DangKyDichVu.class, maDangKy);
        return Optional.ofNullable(dkdv);
    }

    public List<DangKyDichVu> findAll() {
        return entityManager.createQuery("SELECT dkdv FROM DangKyDichVu dkdv", DangKyDichVu.class).getResultList();
    }
}
