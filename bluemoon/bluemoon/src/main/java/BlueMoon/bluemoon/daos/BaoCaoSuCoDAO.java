package BlueMoon.bluemoon.daos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;
import jakarta.persistence.EntityManager;

@Repository
public class BaoCaoSuCoDAO {
    @Autowired
    private EntityManager entityManager;

    public Long countAll() {
        String jpql = "SELECT COUNT(bcs) FROM BaoCaoSuCo bcs";
        Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return count != null ? count : 0;
    }

    public Long countByTrangThai(IncidentStatus trangThai) {
        String jpql = "SELECT COUNT(bcs) FROM BaoCaoSuCo bcs WHERE bcs.trangThai = :trangThai";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("trangThai", trangThai)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public Long countByMucDoUuTien(PriorityLevel mucDoUuTien) {
        String jpql = "SELECT COUNT(bcs) FROM BaoCaoSuCo bcs WHERE bcs.mucDoUuTien = :mucDoUuTien";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("mucDoUuTien", mucDoUuTien)
                .getSingleResult();
        return count != null ? count : 0;
    }

    public List<BaoCaoSuCo> findAll() {
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs";
        return entityManager.createQuery(jpql, BaoCaoSuCo.class).getResultList();
    }

    public List<BaoCaoSuCo> findByTrangThai(IncidentStatus trangThai) {
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs WHERE bcs.trangThai = :trangThai";
        return entityManager.createQuery(jpql, BaoCaoSuCo.class)
                .setParameter("trangThai", trangThai)
                .getResultList();
    }

    public List<BaoCaoSuCo> findByMucDoUuTien(PriorityLevel mucDoUuTien) {
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs WHERE bcs.mucDoUuTien = :mucDoUuTien";
        return entityManager.createQuery(jpql, BaoCaoSuCo.class)
                .setParameter("mucDoUuTien", mucDoUuTien)
                .setMaxResults(5)
                .getResultList();
    }

    public List<BaoCaoSuCo> findByNguoiBaoCao(String cccdNguoiBaoCao) {
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs WHERE bcs.cccdNguoiBaoCao = :cccdNguoiBaoCao";
        return entityManager.createQuery(jpql, BaoCaoSuCo.class)
                .setParameter("cccdNguoiBaoCao", cccdNguoiBaoCao)
                .getResultList();
    }

    /**
     * Lấy danh sách sự cố sắp xếp theo mức độ ưu tiên (cao → bình thường → thấp)
     * và thời gian báo cáo mới nhất
     */
    public List<BaoCaoSuCo> findAllOrderedByPriority() {
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs " +
                "ORDER BY " +
                "CASE bcs.mucDoUuTien " +
                "  WHEN BlueMoon.bluemoon.utils.PriorityLevel.cao THEN 1 " +
                "  WHEN BlueMoon.bluemoon.utils.PriorityLevel.binh_thuong THEN 2 " +
                "  WHEN BlueMoon.bluemoon.utils.PriorityLevel.thap THEN 3 " +
                "  ELSE 4 " +
                "END, " +
                "bcs.thoiGianBaoCao DESC";
        return entityManager.createQuery(jpql, BaoCaoSuCo.class)
                .setMaxResults(10)
                .getResultList();
    }
}
