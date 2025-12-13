package BlueMoon.bluemoon.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.utils.AccountStatus;
import BlueMoon.bluemoon.utils.ResidentStatus;
import BlueMoon.bluemoon.utils.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

// Sử dụng @Repository để cho Spring quản lý DAO này
@Repository
public class DoiTuongDAO {

    // Tiêm EntityManager để truy vấn thủ công
    @Autowired
    @PersistenceContext
    private EntityManager entityManager;

    // =======================================================
    // 1. CÁC PHƯƠNG THỨC CRUD CƠ BẢN (Sử dụng EntityManager)
    // =======================================================

    /**
     * Lưu (hoặc cập nhật) một Entity.
     * @param doiTuong Entity cần lưu.
     * @return Entity đã được quản lý (managed).
     */
    @Transactional
    public DoiTuong save(DoiTuong doiTuong) {
        return entityManager.merge(doiTuong);
    }

    /**
     * Tìm Entity bằng Khóa chính (CCCD).
     */
    public Optional<DoiTuong> findByCccd(String cccd) {
        DoiTuong doiTuong = entityManager.find(DoiTuong.class, cccd);
        return Optional.ofNullable(doiTuong);
    }
    public DoiTuong timCuDanBangCccd(String cccd) {
        DoiTuong doiTuong = entityManager.find(DoiTuong.class, cccd);
        return doiTuong;
    }
    /**
     * Tìm tất cả các Entity.
     */
    public List<DoiTuong> findAll() {
        // Sử dụng JPQL để truy vấn tất cả
        return entityManager.createQuery("SELECT d FROM DoiTuong d ORDER BY d.hoVaTen ASC", DoiTuong.class).getResultList();
    }
    
    /**
     * Xóa Entity.
     */
    @Transactional
    public void delete(DoiTuong doiTuong) {
        // Cần đảm bảo Entity đang ở trạng thái Managed trước khi remove
        if (entityManager.contains(doiTuong)) {
            entityManager.remove(doiTuong);
        } else {
            entityManager.remove(entityManager.merge(doiTuong));
        }
    }

    // =======================================================
    // 2. CÁC PHƯƠNG THỨC TRUY VẤN TÙY CHỈNH (Sử dụng JPQL)
    // =======================================================

    /**
     * Tìm Entity bằng Email.
     */
    public Optional<DoiTuong> findByEmail(String email) {
        String jpql = "SELECT d FROM DoiTuong d WHERE d.email = :email ORDER BY d.hoVaTen ASC";
        try {
            return Optional.of(
                entityManager.createQuery(jpql, DoiTuong.class)
                    .setParameter("email", email)
                    .getSingleResult()
            );
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Tìm tất cả người dùng với vai trò cụ thể.
     */
    public List<DoiTuong> findByVaiTro(UserRole vaiTro) {
        String jpql = "SELECT d FROM DoiTuong d WHERE d.vaiTro = :vaiTro ORDER BY d.hoVaTen ASC";
        return entityManager.createQuery(jpql, DoiTuong.class)
            .setParameter("vaiTro", vaiTro)
            .getResultList();
    }

    /**
     * Tìm Dân cư đang ở chung cư.
     */
    public List<DoiTuong> findResidentsInComplex(ResidentStatus trangThaiDanCu) {
        String jpql = "SELECT d FROM DoiTuong d WHERE d.laCuDan = true AND d.trangThaiDanCu = :trangThai ORDER BY d.hoVaTen ASC";
        return entityManager.createQuery(jpql, DoiTuong.class)
            .setParameter("trangThai", trangThaiDanCu)
            .getResultList();
    }
    /**
     * Tìm kiếm tài khoản cư dân theo CCCD
     */
    public Optional<DoiTuong> timNguoiDungThuongTheoCCCD(String cccd){
        String jpql = "SELECT d FROM DoiTuong d WHERE d.cccd = :cccd AND d.vaiTro = :vaiTro ORDER BY d.hoVaTen ASC";
        try {
            DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class)
                    .setParameter("cccd", cccd)
                    .setParameter("vaiTro", UserRole.nguoi_dung_thuong)
                    .getSingleResult();
            return Optional.of(doiTuong);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    /** 
     * Tìm cư dân theo CCCD 
    */
    public Optional<DoiTuong> findResidentByCccd(String cccd) {
        String jpql = "SELECT d FROM DoiTuong d WHERE d.cccd = :cccd AND d.laCuDan = true ORDER BY d.hoVaTen ASC";
        try {
            DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class)
                    .setParameter("cccd", cccd)
                    .getSingleResult();
            return Optional.of(doiTuong);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Tìm người dùng bằng reset token
     */
    public Optional<DoiTuong> findByResetToken(String token) {
        String jpql = "SELECT d FROM DoiTuong d WHERE d.resetToken = :token";
        try {
            DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(doiTuong);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
	/**
	 * Tìm kiếm cư dân theo cccd, họ tên, tuổi, địa chỉ, giới tính, sđt, chủ hộ
	 */
	public List<DoiTuong> searchResidentsAndFilter(String keyword, ResidentStatus residentStatus,
			AccountStatus accountStatus) {

// 1. Khởi tạo truy vấn cơ bản: Lấy tất cả các đối tượng được đánh dấu là Cư Dân
		StringBuilder jpql = new StringBuilder("SELECT d FROM DoiTuong d WHERE d.laCuDan = true");

// 2. Thêm điều kiện lọc theo Trạng Thái Dân Cư (nếu có)
		if (residentStatus != null) {
			jpql.append(" AND d.trangThaiDanCu = :rStatus");
		}

// 3. Thêm điều kiện lọc theo Trạng Thái Tài Khoản (nếu có)
		if (accountStatus != null) {
			jpql.append(" AND d.trangThaiTaiKhoan = :aStatus");
		}

// 4. Thêm điều kiện tìm kiếm theo Từ Khóa (nếu có)
		if (keyword != null && !keyword.trim().isEmpty()) {
			jpql.append(" AND (LOWER(d.cccd) LIKE :kw OR " + "LOWER(d.hoVaTen) LIKE :kw OR "
					+ "LOWER(d.soDienThoai) LIKE :kw OR " + "LOWER(d.email) LIKE :kw)"); // Tìm kiếm trên CCCD, Họ tên,
																							// SĐT và Email
		}

// 5. Chuẩn bị truy vấn và thiết lập tham số
		TypedQuery<DoiTuong> query = entityManager.createQuery(jpql.append(" ORDER BY d.hoVaTen ASC").toString(), DoiTuong.class);

// Thiết lập tham số cho Trạng Thái Dân Cư
		if (residentStatus != null) {
			query.setParameter("rStatus", residentStatus);
		}

// Thiết lập tham số cho Trạng Thái Tài Khoản
		if (accountStatus != null) {
			query.setParameter("aStatus", accountStatus);
		}

// Thiết lập tham số cho Từ Khóa
		if (keyword != null && !keyword.trim().isEmpty()) {
			String kwParam = "%" + keyword.toLowerCase() + "%";
			query.setParameter("kw", kwParam);
		}

// 6. Thực thi và trả về danh sách kết quả
		return query.getResultList();
	}

	public List<DoiTuong> searchResidents(String keyword) {
		String jpql = "SELECT d FROM DoiTuong d WHERE d.laCuDan = true AND (" + "LOWER(d.cccd) LIKE :kw OR "
				+ "LOWER(d.hoVaTen) LIKE :kw OR " + "CAST(d.ngaySinh AS string) LIKE :kw OR "
				+ "LOWER(d.queQuan) LIKE :kw OR " + "LOWER(d.gioiTinh) LIKE :kw OR " + "LOWER(d.soDienThoai) LIKE :kw"
				+ ") ORDER BY d.hoVaTen ASC";
		String kwParam = "%" + keyword.toLowerCase() + "%";
		return entityManager.createQuery(jpql, DoiTuong.class).setParameter("kw", kwParam).getResultList();
	}

	public Optional<DoiTuong> findByEmailandCccd(String cccd, String email) {
		String jpql = "SELECT d FROM DoiTuong d WHERE d.cccd = :cccd AND d.email = :email";
		try {
			DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class).setParameter("cccd", cccd)
					.setParameter("email", email).getSingleResult();
			return Optional.of(doiTuong);
		} catch (NoResultException e) {
			return Optional.empty();
		}
	}

	/**
	 * Tìm ban quản trị theo id
	 */
	public Optional<DoiTuong> findAdminById(String id) {
		String jpql = "SELECT d FROM DoiTuong d WHERE d.cccd = :id AND d.vaiTro = :vaiTro";
		try {
			DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class).setParameter("id", id)
					.setParameter("vaiTro", UserRole.ban_quan_tri).getSingleResult();
			return Optional.of(doiTuong);
		} catch (NoResultException e) {
			return Optional.empty();
		}
	}

	/**
	 * Đếm tổng số cư dân trong hệ thống
	 */
	public Long countResidents() {
		String jpql = "SELECT COUNT(d) FROM DoiTuong d WHERE d.laCuDan = true";
		Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
		return count != null ? count : 0;
	}

	/**
	 * Tìm cơ quan chức năng theo id
	 */
	public Optional<DoiTuong> findOfficerById(String id) {
		String jpql = "SELECT d FROM DoiTuong d WHERE d.cccd = :id AND d.vaiTro = :vaiTro";
		try {
			DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class).setParameter("id", id)
					.setParameter("vaiTro", UserRole.co_quan_chuc_nang).getSingleResult();
			return Optional.of(doiTuong);
		} catch (NoResultException e) {
			return Optional.empty();
		}
	}

	/**
	 * Tìm kế toán theo id
	 */
	public Optional<DoiTuong> findAccountantById(String id) {
		String jpql = "SELECT d FROM DoiTuong d WHERE d.cccd = :id AND d.vaiTro = :vaiTro";
		try {
			DoiTuong doiTuong = entityManager.createQuery(jpql, DoiTuong.class).setParameter("id", id)
					.setParameter("vaiTro", UserRole.ke_toan).getSingleResult();
			return Optional.of(doiTuong);
		} catch (NoResultException e) {
			return Optional.empty();
		}
	}
    /**
     * Thống kê cư dân theo Giới tính
     */
    public List<Object[]> countResidentsByGender() {
        String jpql = "SELECT d.gioiTinh, COUNT(d) FROM DoiTuong d WHERE d.laCuDan = true GROUP BY d.gioiTinh";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    /**
     * Thống kê cư dân theo Trạng thái cư trú (Thường trú/Tạm trú...)
     */
    public List<Object[]> countResidentsByStatus() {
        String jpql = "SELECT d.trangThaiDanCu, COUNT(d) FROM DoiTuong d WHERE d.laCuDan = true GROUP BY d.trangThaiDanCu";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }
}