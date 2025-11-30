package BlueMoon.bluemoon.daos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
public class TaiSanChungCuDAO {
    @Autowired
    @PersistenceContext
    private EntityManager entityManager;
    
    // =======================================================
    // 1. CÁC PHƯƠNG THỨC CRUD CƠ BẢN (Sử dụng EntityManager)
    // =======================================================

    /**
     * Lưu (hoặc cập nhật) một Entity.
     * @param taiSan Entity cần lưu.
     * @return Entity đã được quản lý (managed).
     */
    @Transactional
    public TaiSanChungCu save(TaiSanChungCu taiSan) {
        return entityManager.merge(taiSan);
    }

    /**
     * Tìm Entity bằng Khóa chính (ma_tai_san).
     */
    public Optional<TaiSanChungCu> findByID(Integer ma_tai_san) {
        TaiSanChungCu taiSan = entityManager.find(TaiSanChungCu.class, ma_tai_san);
        return Optional.ofNullable(taiSan);
    }
    
    /**
     * Tìm tất cả các Entity.
     */
    public List<TaiSanChungCu> findAll() {
        // Sử dụng JPQL để truy vấn tất cả
        return entityManager.createQuery("SELECT ts FROM TaiSanChungCu ts", TaiSanChungCu.class).getResultList();
    }
    
    /**
     * Xóa Entity.
     */
    @Transactional
    public void delete(TaiSanChungCu taiSan) {
        // Cần đảm bảo Entity đang ở trạng thái Managed trước khi remove
        if (entityManager.contains(taiSan)) {
            entityManager.remove(taiSan);
        } else {
            entityManager.remove(entityManager.merge(taiSan));
        }
    }

    // =======================================================
    // 2. CÁC PHƯƠNG THỨC TRUY VẤN TÙY CHỈNH (Sử dụng JPQL)
    // =======================================================

    /**
     * Tìm kiếm tất cả căn hộ
     */
    public List<TaiSanChungCu> findAllApartments() {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }

    /**
     * Tìm kiếm tất cả chỗ đỗ xe
     */
    public List<TaiSanChungCu> findAllParkingSpots() {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.tenTaiSan =: ten AND ts.loaiTaiSan = :loai";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("ten", "Chỗ đỗ xe")
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.tien_ich)
                            .getResultList();
    }
    /**
     * Tìm kiếm tất cả tài sản, có thể lọc theo loại tài sản.
     * Nếu loaiTaiSan là null, trả về tất cả.
     */
    public List<TaiSanChungCu> findAllAssets(BlueMoon.bluemoon.utils.AssetType loaiTaiSan) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE (:loai is null OR ts.loaiTaiSan = :loai)";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", loaiTaiSan)
                            .getResultList();
    }

    /**
     * Tìm kiếm tài sản theo tên (sử dụng LIKE).
     */
    public List<TaiSanChungCu> findAssetsByName(String keyword) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.tenTaiSan LIKE :keyword";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("keyword", "%" + keyword + "%")
                            .getResultList();
    }
    /**
     * Sửa thông tin căn hộ
     */
    public void updateApartmentInfo(Integer ma_tai_san, HoGiaDinh hoGiaDinh, String trang_thai, BigDecimal gia_tri) {
        TaiSanChungCu taiSan = entityManager.find(TaiSanChungCu.class, ma_tai_san);
        if (taiSan != null && taiSan.getLoaiTaiSan() == BlueMoon.bluemoon.utils.AssetType.can_ho) {
            taiSan.setHoGiaDinh(hoGiaDinh);
            taiSan.setTrangThai(BlueMoon.bluemoon.utils.AssetStatus.valueOf(trang_thai));
            taiSan.setGiaTri(gia_tri);
            entityManager.merge(taiSan);
        }
    }
    /**
     * Tìm kiếm căn hộ theo hộ gia đình
     */
    public List<TaiSanChungCu> findApartmentsByHousehold(HoGiaDinh hoGiaDinh) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai AND ts.hoGiaDinh = :hoGiaDinh";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .setParameter("hoGiaDinh", hoGiaDinh)
                            .getResultList();
    }
    
    /**
     * Phân loại căn hộ theo trạng thái
     */
    public List<TaiSanChungCu> findApartmentsByStatus(BlueMoon.bluemoon.utils.AssetStatus status) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai AND ts.trangThai = :trangThai";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .setParameter("trangThai", status)
                            .getResultList();
    }
    /**
     * Phân loại căn hộ theo dải diện tích
     */
    public List<TaiSanChungCu> findApartmentsByAreaRange(BigDecimal minDienTich, BigDecimal maxDienTich) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai AND ts.dienTich BETWEEN :minDienTich AND :maxDienTich";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .setParameter("minDienTich", minDienTich)
                            .setParameter("maxDienTich", maxDienTich)
                            .getResultList();
    }
    /**
     * Phân loại căn hộ theo dải giá trị
     */
    public List<TaiSanChungCu> findApartmentsByValueRange(BigDecimal minGiaTri, BigDecimal maxGiaTri) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai AND ts.giaTri BETWEEN :minGiaTri AND :maxGiaTri";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .setParameter("minGiaTri", minGiaTri)
                            .setParameter("maxGiaTri", maxGiaTri)
                            .getResultList();
    }
    // Giả định bạn đã sửa lại TaiSanChungCuDAO.java để bao gồm phương thức sau:

    /**
     * Tìm căn hộ chính (loaiTaiSan = CAN_HỌ) của một hộ gia đình.
     */
    public Optional<TaiSanChungCu> findApartmentByHo(String maHo) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.hoGiaDinh.maHo = :maHo AND ts.loaiTaiSan = :loaiCanHo";
        try {
            return Optional.of(entityManager.createQuery(jpql, TaiSanChungCu.class)
                                            .setParameter("maHo", maHo)
                                            .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                                            .setMaxResults(1) // Lấy căn hộ đầu tiên (giả định 1 hộ sở hữu 1 căn chính)
                                            .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    /**
     * Đếm tổng số căn hộ trong hệ thống
     */
    public Long countAllApartments() {
        String jpql = "SELECT COUNT(ts) FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loaiCanHo";
        Long count = entityManager.createQuery(jpql, Long.class)
                                  .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                                  .getSingleResult();
        return count != null ? count : 0;
    }
    /**
     * Đếm tổng số chỗ đỗ xe trong hệ thống
     */
    public Long countAllParkingSpots() {
        String jpql = "SELECT COUNT(ts) FROM TaiSanChungCu ts WHERE ts.tenTaiSan = :ten AND ts.loaiTaiSan = :loaiTienIch";
        Long count = entityManager.createQuery(jpql, Long.class)
                                  .setParameter("ten", "cho_do_xe")
                                  .setParameter("loaiTienIch", BlueMoon.bluemoon.utils.AssetType.tien_ich)
                                  .getSingleResult();
        return count != null ? count : 0;
    }
    /**
     * Đếm tổng số tài sản chung cư
     */
    public Long countAllAssets() {
        String jpql = "SELECT COUNT(ts) FROM TaiSanChungCu ts";
        Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return count != null ? count : 0;
    }
    /**
     * Tìm tất cả các tầng (ViTri) có căn hộ (loaiTaiSan = CAN_HỌ)
     */
    public List<String> findAllApartmentFloors() {
        String jpql = "SELECT DISTINCT ts.viTri FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai ORDER BY ts.viTri ASC";
        return entityManager.createQuery(jpql, String.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }
    /**
     * Kiểm tra xem Tên Tài Sản đã tồn tại chưa (dùng khi thêm mới)
     */
    public boolean existsByTenCanHo(String tenTaiSan) {
        // Chỉ kiểm tra cho CĂN HỘ (AssetType.can_ho)
        String jpql = "SELECT COUNT(ts) FROM TaiSanChungCu ts WHERE ts.tenTaiSan = :tenTaiSan AND ts.loaiTaiSan = :loaiCanHo";
        Long count = entityManager.createQuery(jpql, Long.class)
                                  .setParameter("tenTaiSan", tenTaiSan)
                                  .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                                  .getSingleResult();
        return count > 0;
    }
    
    /**
     * Kiểm tra xem Tên Tài Sản đã tồn tại chưa (dùng khi cập nhật, loại trừ chính nó)
     */
    public boolean existsByTenCanHoExceptId(String tenTaiSan, Integer maTaiSan) {
        String jpql = "SELECT COUNT(ts) FROM TaiSanChungCu ts WHERE ts.tenTaiSan = :tenTaiSan " +
                      "AND ts.loaiTaiSan = :loaiCanHo AND ts.maTaiSan != :maTaiSan";
        Long count = entityManager.createQuery(jpql, Long.class)
                                  .setParameter("tenTaiSan", tenTaiSan)
                                  .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                                  .setParameter("maTaiSan", maTaiSan)
                                  .getSingleResult();
        return count > 0;
    }
    
    /**
     * Tìm kiếm các căn hộ đang TRỐNG (HoGiaDinh IS NULL) theo vị trí (tầng).
     */
    public List<TaiSanChungCu> findEmptyApartmentsByFloor(String viTri) {
        String jpql = "SELECT ts FROM TaiSanChungCu ts WHERE ts.loaiTaiSan = :loai " +
                      "AND ts.hoGiaDinh IS NULL AND ts.viTri = :viTri";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .setParameter("viTri", viTri)
                            .getResultList();
    }
    /**
     * Lấy danh sách: [Tên Căn Hộ, Số lượng cư dân đang ở]
     * Ví dụ: ["A-1001", 3], ["B-0502", 2]
     * Chúng ta sẽ xử lý logic tách Tòa/Tầng ở Service.
     */
    public List<Object[]> getRawResidentCounts() {
        String jpql = "SELECT t.tenTaiSan, COUNT(tv) " +
                      "FROM TaiSanChungCu t " +
                      "JOIN t.hoGiaDinh h " +
                      "JOIN h.thanhVienHoList tv " +
                      "WHERE t.loaiTaiSan = :loai " +
                      "AND tv.ngayKetThuc IS NULL " +
                      "GROUP BY t.tenTaiSan";
        
        return entityManager.createQuery(jpql, Object[].class)
                            .setParameter("loai", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }
    // =======================================================
    // THỐNG KÊ TÀI SẢN CHUNG (KHÔNG BAO GỒM CĂN HỘ)
    // =======================================================

    /**
     * Lấy danh sách chi tiết các tài sản chung (Không phải căn hộ)
     */
    public List<TaiSanChungCu> findAllGeneralAssets() {
        String jpql = "SELECT t FROM TaiSanChungCu t WHERE t.loaiTaiSan != :loaiCanHo ORDER BY t.tenTaiSan ASC";
        return entityManager.createQuery(jpql, TaiSanChungCu.class)
                            .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }

    /**
     * Thống kê số lượng theo Loại Tài Sản (VD: Thiết bị, Tiện ích...)
     * Trả về: List<[AssetType, Long]>
     */
    public List<Object[]> countGeneralAssetsByType() {
        String jpql = "SELECT t.loaiTaiSan, COUNT(t) FROM TaiSanChungCu t " +
                      "WHERE t.loaiTaiSan != :loaiCanHo " +
                      "GROUP BY t.loaiTaiSan";
        return entityManager.createQuery(jpql, Object[].class)
                            .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }

    /**
     * Thống kê số lượng theo Trạng Thái (VD: Hoạt động, Bảo trì...)
     * Trả về: List<[AssetStatus, Long]>
     */
    public List<Object[]> countGeneralAssetsByStatus() {
        String jpql = "SELECT t.trangThai, COUNT(t) FROM TaiSanChungCu t " +
                      "WHERE t.loaiTaiSan != :loaiCanHo " +
                      "GROUP BY t.trangThai";
        return entityManager.createQuery(jpql, Object[].class)
                            .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }

    /**
     * Thống kê số lượng theo Vị Trí (Tầng/Khu vực)
     * Trả về: List<[String, Long]>
     */
    public List<Object[]> countGeneralAssetsByLocation() {
        // Group by chuỗi vị trí.
        // Lưu ý: Dữ liệu vị trí cần nhập chuẩn hóa (VD: "Tầng 1", "Sảnh A") để biểu đồ đẹp.
        String jpql = "SELECT t.viTri, COUNT(t) FROM TaiSanChungCu t " +
                      "WHERE t.loaiTaiSan != :loaiCanHo " +
                      "GROUP BY t.viTri";
        return entityManager.createQuery(jpql, Object[].class)
                            .setParameter("loaiCanHo", BlueMoon.bluemoon.utils.AssetType.can_ho)
                            .getResultList();
    }
}

