package BlueMoon.bluemoon.daos;

import java.util.List;  
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.utils.HouseholdStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

@Repository
public class HoGiaDinhDAO {
    @Autowired
    private EntityManager entityManager;

    // =======================================================
    // CRUD CƠ BẢN
    // =======================================================
    @Transactional
    public HoGiaDinh save(HoGiaDinh hoGiaDinh) {
        // Sử dụng merge cho cả insert và update
        return entityManager.merge(hoGiaDinh);
    }
    
    public Optional<HoGiaDinh> findById(String maHo) {
        return Optional.ofNullable(entityManager.find(HoGiaDinh.class, maHo));
    }
    
    public List<HoGiaDinh> findAll() {
        return entityManager.createQuery("SELECT hgd FROM HoGiaDinh hgd ORDER BY hgd.maHo ASC", HoGiaDinh.class).getResultList();
    }

    public Long countAll() {
        String jpql = "SELECT COUNT(hgd) FROM HoGiaDinh hgd";
        Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return count != null ? count : 0;
    }
    
    /**
     * Đếm tổng số hộ gia đình đang hoạt động
     */
    public Long countActiveHouseholds() {
        String jpql = "SELECT COUNT(hgd) FROM HoGiaDinh hgd WHERE hgd.trangThai =: trangThai";
        Long count = entityManager.createQuery(jpql, Long.class).setParameter("trangThai", HouseholdStatus.hoat_dong).getSingleResult();
        return count != null ? count : 0;
    }
    /**
     * Đếm tổng số thành viên trong 1 hộ gia đình
     */
    public Long countMembersInHousehold(String maHo) {
        String jpql = "SELECT COUNT(tvh) FROM ThanhVienHo tvh WHERE tvh.hoGiaDinh.maHo = :maHo ";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("maHo", maHo)
                .getSingleResult();
        return count != null ? count : 0;
    }
    // =======================================================
    // TRUY VẤN TÙY CHỈNH (Tìm kiếm/Lọc)
    // =======================================================
    public List<HoGiaDinh> search(String keyword) {
        String jpql = "SELECT hgd FROM HoGiaDinh hgd WHERE " +
                      "LOWER(hgd.maHo) LIKE :kw OR " +
                      "LOWER(hgd.tenHo) LIKE :kw";
        String kwParam = "%" + keyword.toLowerCase() + "%";
        return entityManager.createQuery(jpql, HoGiaDinh.class)
                            .setParameter("kw", kwParam)
                            .getResultList();
    }
    
    public boolean existsById(String maHo) {
        try {
            entityManager.createQuery("SELECT hgd.maHo FROM HoGiaDinh hgd WHERE hgd.maHo = :maHo", String.class)
                .setParameter("maHo", maHo)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    public List<HoGiaDinh> findByTrangThai(HouseholdStatus hoatDong) {
        String jpql = "SELECT hgd FROM HoGiaDinh hgd WHERE hgd.trangThai = :trangThai ORDER BY hgd.maHo ASC";
        return entityManager.createQuery(jpql, HoGiaDinh.class)
                .setParameter("trangThai", hoatDong)
                .getResultList();
    }
}
