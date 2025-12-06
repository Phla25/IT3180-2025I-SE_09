package BlueMoon.bluemoon.daos;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository; // Import quan trọng
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.IncidentType;
import BlueMoon.bluemoon.utils.PriorityLevel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

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
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs ORDER BY bcs.thoiGianBaoCao DESC"; // Sắp xếp mới nhất
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
        // Đã sửa lỗi đường dẫn attribute
        String jpql = "SELECT bcs FROM BaoCaoSuCo bcs WHERE bcs.nguoiBaoCao.cccd = :cccdNguoiBaoCao ORDER BY bcs.thoiGianBaoCao DESC";
        return entityManager.createQuery(jpql, BaoCaoSuCo.class)
                .setParameter("cccdNguoiBaoCao", cccdNguoiBaoCao)
                .getResultList();
    }

    @Transactional // BẮT BUỘC PHẢI CÓ ĐỂ LƯU VÀO DB
    public void save(BaoCaoSuCo baoCao) {
        if (baoCao.getMaBaoCao() == null) {
            entityManager.persist(baoCao);
        } else {
            entityManager.merge(baoCao);
        }
    }
    public java.util.Optional<BaoCaoSuCo> findById(Integer id) {
        BaoCaoSuCo sc = entityManager.find(BaoCaoSuCo.class, id);
        return java.util.Optional.ofNullable(sc);
    }
    /**
     * Tìm kiếm sự cố của cư dân theo nhiều tiêu chí
     */
    public List<BaoCaoSuCo> findByCuDanWithFilter(String cccd, String keyword, 
                                                  BlueMoon.bluemoon.utils.IncidentType type,
                                                  BlueMoon.bluemoon.utils.PriorityLevel priority,
                                                  LocalDateTime startDate, LocalDateTime endDate,
                                                  Integer hour) {
        
        StringBuilder sb = new StringBuilder("SELECT bcs FROM BaoCaoSuCo bcs WHERE bcs.nguoiBaoCao.cccd = :cccd");
        
        // Xây dựng câu query động
        if (keyword != null && !keyword.isEmpty()) {
            sb.append(" AND (LOWER(bcs.tieuDe) LIKE LOWER(:keyword) OR LOWER(bcs.noiDung) LIKE LOWER(:keyword))");
        }
        if (type != null) {
            sb.append(" AND bcs.loaiSuCo = :type");
        }
        if (priority != null) {
            sb.append(" AND bcs.mucDoUuTien = :priority");
        }
        if (startDate != null && endDate != null) {
            sb.append(" AND bcs.thoiGianBaoCao BETWEEN :startDate AND :endDate");
        }
        if (hour != null) {
            // Hàm HOUR() của JPQL/HQL lấy giờ từ thời gian
            sb.append(" AND HOUR(bcs.thoiGianBaoCao) = :hour");
        }
        
        sb.append(" ORDER BY bcs.thoiGianBaoCao DESC");

        // Tạo Query và gán tham số
        var query = entityManager.createQuery(sb.toString(), BaoCaoSuCo.class);
        query.setParameter("cccd", cccd);

        if (keyword != null && !keyword.isEmpty()) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        if (priority != null) {
            query.setParameter("priority", priority);
        }
        if (startDate != null && endDate != null) {
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
        }
        if (hour != null) {
            query.setParameter("hour", hour);
        }

        return query.getResultList();
    }
    /**
     * MỚI: Hàm lọc đa tiêu chí cho Admin (Đã thêm reporterName)
     */
    public List<BaoCaoSuCo> findWithFilterAdmin(String keyword, 
                                                String reporterName, // <--- THÊM MỚI
                                                IncidentStatus status,
                                                PriorityLevel priority,
                                                IncidentType type,
                                                LocalDateTime startDate, 
                                                LocalDateTime endDate,
                                                Integer hour) {
        
        StringBuilder sb = new StringBuilder("SELECT bcs FROM BaoCaoSuCo bcs WHERE 1=1");
        
        // 1. Xây dựng câu truy vấn động
        if (keyword != null && !keyword.isEmpty()) {
            sb.append(" AND (LOWER(bcs.tieuDe) LIKE LOWER(:keyword) OR LOWER(bcs.noiDung) LIKE LOWER(:keyword))");
        }
        // --- THÊM LOGIC LỌC NGƯỜI BÁO CÁO ---
        if (reporterName != null && !reporterName.isEmpty()) {
            sb.append(" AND LOWER(bcs.nguoiBaoCao.hoVaTen) LIKE LOWER(:reporterName)");
        }
        // ------------------------------------
        if (status != null) {
            sb.append(" AND bcs.trangThai = :status");
        }
        if (priority != null) {
            sb.append(" AND bcs.mucDoUuTien = :priority");
        }
        if (type != null) {
            sb.append(" AND bcs.loaiSuCo = :type");
        }
        if (startDate != null && endDate != null) {
            sb.append(" AND bcs.thoiGianBaoCao BETWEEN :startDate AND :endDate");
        }
        if (hour != null) {
            sb.append(" AND HOUR(bcs.thoiGianBaoCao) = :hour");
        }
        
        sb.append(" ORDER BY bcs.thoiGianBaoCao DESC");

        // 2. Gán tham số
        TypedQuery<BaoCaoSuCo> query = entityManager.createQuery(sb.toString(), BaoCaoSuCo.class);

        if (keyword != null && !keyword.isEmpty()) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        // --- GÁN THAM SỐ NGƯỜI BÁO CÁO ---
        if (reporterName != null && !reporterName.isEmpty()) {
            query.setParameter("reporterName", "%" + reporterName + "%");
        }
        // ---------------------------------
        if (status != null) {
            query.setParameter("status", status);
        }
        if (priority != null) {
            query.setParameter("priority", priority);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        if (startDate != null && endDate != null) {
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
        }
        if (hour != null) {
            query.setParameter("hour", hour);
        }

        return query.getResultList();
    }
}