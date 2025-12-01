package BlueMoon.bluemoon.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.DichVu;
import BlueMoon.bluemoon.utils.AssetStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

@Repository
public class DichVuDAO {
    @Autowired
    private EntityManager entityManager;
    
    /**
     * Lưu hoặc cập nhật một đối tượng DichVu
     */
    public DichVu save(DichVu dichVu) {
        if (dichVu.getMaDichVu() == null) {
            // Thêm mới
            entityManager.persist(dichVu);
            return dichVu;
        } else {
            // Cập nhật
            return entityManager.merge(dichVu);
        }
    }
    
    /**
     * Tìm dịch vụ theo ID
     */
    public Optional<DichVu> findById(Integer maDichVu) {
        DichVu dichVu = entityManager.find(DichVu.class, maDichVu);
        return Optional.ofNullable(dichVu);
    }

    /**
     * Lấy tất cả dịch vụ
     */
    public List<DichVu> findAll() {
        return entityManager.createQuery("SELECT dv FROM DichVu dv", DichVu.class).getResultList();
    }
    
    /**
     * Đếm tổng số dịch vụ trong hệ thống
     */
    public Long countAll() {
        String jpql = "SELECT COUNT(dv) FROM DichVu dv";
        Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return count != null ? count : 0;
    }
    
    /**
     * Tìm dịch vụ theo tên (Trả về một List)
     */
    public Optional<DichVu> findByTenDichVu(String tenDichVu) {
        String jpql = "SELECT dv FROM DichVu dv WHERE dv.tenDichVu = :tenDichVu";
        try {
            TypedQuery<DichVu> query = entityManager.createQuery(jpql, DichVu.class)
                    .setParameter("tenDichVu", tenDichVu);
            // Giả định tên là UNIQUE, lấy 1 kết quả đầu tiên
            return Optional.of(query.getSingleResult()); 
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    // Phương thức xóa (Nếu thực sự cần xóa cứng)
    public void delete(DichVu dichVu) {
        if (entityManager.contains(dichVu)) {
            entityManager.remove(dichVu);
        } else {
            entityManager.remove(entityManager.merge(dichVu));
        }
    }


    public List<DichVu> findByTrangThai(AssetStatus assetStatus) {
        String jpql = "SELECT dv FROM DichVu dv WHERE dv.trangThai =:trangThai";
        try {
            return (List<DichVu>) entityManager.createQuery(jpql, DichVu.class)
                                .setParameter("trangThai", assetStatus).getResultList();
        } catch (NoResultException e){
            return null;
        }
    }
}