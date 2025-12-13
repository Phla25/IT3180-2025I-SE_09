package BlueMoon.bluemoon.daos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.utils.InvoiceStatus;
import BlueMoon.bluemoon.utils.InvoiceType;
import BlueMoon.bluemoon.utils.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

@Repository
public class HoaDonDAO {
    @Autowired
    private EntityManager entityManager;

    public Long countAll() {
        String jpql = "SELECT COUNT(hd) FROM HoaDon hd";
        Long count = entityManager.createQuery(jpql, Long.class).getSingleResult();
        return count != null ? count : 0;
    }
    public Long countByTrangThai(InvoiceStatus trangThai) {
        String jpql = "SELECT COUNT(hd) FROM HoaDon hd WHERE hd.trangThai = :trangThai";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("trangThai", trangThai)
                .getSingleResult();
        return count != null ? count : 0;
    }
    public List<HoaDon> findAll() {
        String jpql = "SELECT hd FROM HoaDon hd";
        return entityManager.createQuery(jpql, HoaDon.class).getResultList();
    }
    public List<HoaDon> findByTrangThai(InvoiceStatus trangThai) {
        String jpql = "SELECT hd FROM HoaDon hd WHERE hd.trangThai = :trangThai";
        return entityManager.createQuery(jpql, HoaDon.class)
                .setParameter("trangThai", trangThai)
                .getResultList();
    }
    /**
     * CẬP NHẬT: Lấy danh sách hóa đơn theo Hộ Gia Đình (Cho Cư Dân)
     * Đảm bảo tải: NguoiDangKyDichVu, NguoiThanhToan
     */
    public List<HoaDon> findByHoGiaDinh(HoGiaDinh hoGiaDinh) 
    {
        String jpql = "SELECT hd FROM HoaDon hd "
                    + "LEFT JOIN FETCH hd.nguoiDangKyDichVu ndkdv "
                    + "LEFT JOIN FETCH hd.nguoiThanhToan ntt "
                    + "WHERE hd.hoGiaDinh = :hoGiaDinh "
                    + "ORDER BY hd.ngayTao DESC";
        return entityManager.createQuery(jpql, HoaDon.class)
                .setParameter("hoGiaDinh", hoGiaDinh)
                .getResultList();
    }
    public List<HoaDon> findByHoGiaDinhMaHo(String maHo) 
    {
            // Tối ưu hóa: Lọc bằng ID (maHo)
            // JOIN FETCH hd.hoGiaDinh h để tránh N+1 query khi hiển thị
            String jpql = "SELECT hd FROM HoaDon hd JOIN FETCH hd.hoGiaDinh h WHERE h.maHo = :maHo ORDER BY hd.ngayTao DESC";
            return entityManager.createQuery(jpql, HoaDon.class)
                    .setParameter("maHo", maHo)
                    .getResultList();
    }
// [SỬA 1] Hàm findById: Dùng LEFT JOIN để tìm được cả Phiếu Chi (không có hộ)
    public Optional<HoaDon> findById(Integer maHoaDon) {
        String jpql = "SELECT hd FROM HoaDon hd "
                + "LEFT JOIN FETCH hd.hoGiaDinh h " // <--- SỬA THÀNH LEFT JOIN
                + "LEFT JOIN FETCH hd.nguoiDangKyDichVu ndkdv " 
                + "LEFT JOIN FETCH hd.nguoiThanhToan ntt "
                + "WHERE hd.maHoaDon = :maHoaDon";
        try {
             HoaDon hoaDon = entityManager.createQuery(jpql, HoaDon.class)
                .setParameter("maHoaDon", maHoaDon)
                .getSingleResult();
             return Optional.of(hoaDon);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    // [SỬA 2] Hàm lấy danh sách hiển thị cho Admin/Kế toán
    public List<HoaDon> findAllWithDetails() {
        String jpql = "SELECT hd FROM HoaDon hd "
                    + "LEFT JOIN FETCH hd.hoGiaDinh h " // <--- SỬA THÀNH LEFT JOIN (Quan trọng nhất)
                    + "LEFT JOIN FETCH hd.nguoiDangKyDichVu ndkdv " 
                    + "LEFT JOIN FETCH hd.nguoiThanhToan ntt " 
                    + "ORDER BY hd.ngayTao DESC";
        return entityManager.createQuery(jpql, HoaDon.class).getResultList();
    }
    
    // [SỬA 3] Hàm lọc theo trạng thái (nếu có dùng để hiển thị list)
    public List<HoaDon> findByTrangThaiOrderByNgayTao(InvoiceStatus trangThai) {
        String jpql = "SELECT hd FROM HoaDon hd "
                    + "LEFT JOIN FETCH hd.hoGiaDinh h " // <--- SỬA THÀNH LEFT JOIN
                    + "WHERE hd.trangThai = :trangThai ORDER BY hd.ngayTao DESC";
        return entityManager.createQuery(jpql, HoaDon.class)
            .setParameter("trangThai", trangThai)
            .getResultList();
    }
    public BigDecimal sumSoTienByTrangThai(InvoiceStatus trangThai) {
        String jpql = "SELECT SUM(hd.soTien) FROM HoaDon hd WHERE hd.trangThai = :trangThai";
        BigDecimal sum = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("trangThai", trangThai)
                .getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }
    public List<HoaDon> findAllWithHoGiaDinh() {
        // Tái sử dụng phương thức findAllWithDetails mới
        return findAllWithDetails();
    }
    
    @SuppressWarnings("unchecked")
    public List<HoaDon> findByTrangThaiWithChuHo(InvoiceStatus trangThai) {
        
        // JPQL sử dụng JOIN PHỨC HỢP để tải Chủ Hộ VÀ Người Đăng Ký Dịch VỤ
        String jpql = "SELECT DISTINCT hd FROM HoaDon hd "
                + "JOIN FETCH hd.hoGiaDinh h "
                + "JOIN h.thanhVienHoList tvh " 
                + "JOIN tvh.doiTuong dt " 
                + "LEFT JOIN FETCH hd.nguoiDangKyDichVu ndkdv " // Thêm Fetch Join cho người đăng ký/người tạo
                + "WHERE tvh.laChuHo = TRUE AND hd.trangThai = :trangThai "
                + "ORDER BY hd.ngayTao DESC";

        Query query = entityManager.createQuery(jpql, HoaDon.class)
            .setParameter("trangThai", trangThai);
        
        return query.getResultList();
    }
    public BigDecimal sumSoTienByLoaiAndTrangThai(InvoiceType loaiHoaDon, InvoiceStatus trangThai) {
        String jpql = "SELECT SUM(hd.soTien) FROM HoaDon hd WHERE hd.loaiHoaDon = :loaiHoaDon AND hd.trangThai = :trangThai";
        BigDecimal sum = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("loaiHoaDon", loaiHoaDon)
                .setParameter("trangThai", trangThai)
                .getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public HoaDon save(HoaDon hoaDon) {
        return entityManager.merge(hoaDon);
    }

    public void delete(HoaDon hd) {
        entityManager.remove(hd);
    }

    /**
     * [SỬA LẠI] TÍNH TỔNG CHI (EXPENSE)
     * Logic: Dựa hoàn toàn vào VAI TRÒ của người tạo (nguoiDangKyDichVu).
     * Nếu người tạo là ADMIN hoặc KẾ TOÁN -> Đây là khoản CHI.
     */
    public BigDecimal sumTongChiByDateRange(InvoiceStatus status, LocalDateTime start, LocalDateTime end) {
        // JOIN bảng DoiTuong (u) qua nguoiDangKyDichVu
        String jpql = "SELECT SUM(h.soTien) FROM HoaDon h " +
                      "JOIN h.nguoiDangKyDichVu u " + 
                      "WHERE h.trangThai = :status " +
                      "AND h.ngayThanhToan BETWEEN :start AND :end " +
                      "AND u.vaiTro IN (:adminRoles)"; // Chỉ lấy người có quyền quản trị
        
        List<UserRole> adminRoles = Arrays.asList(UserRole.ban_quan_tri, UserRole.ke_toan);

        try {
            BigDecimal sum = entityManager.createQuery(jpql, BigDecimal.class)
                    .setParameter("status", status)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .setParameter("adminRoles", adminRoles)
                    .getSingleResult();
            return sum != null ? sum : BigDecimal.ZERO;
        } catch (NoResultException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * [SỬA LẠI] TÍNH TỔNG THU (REVENUE)
     * Logic: Ngược lại với Chi.
     * Người tạo là CƯ DÂN hoặc NULL (Hệ thống tự tạo định kỳ).
     */
    public BigDecimal sumTongThuByDateRange(InvoiceStatus status, LocalDateTime start, LocalDateTime end) {
        // LEFT JOIN để lấy cả trường hợp nguoiDangKyDichVu là NULL
        String jpql = "SELECT SUM(h.soTien) FROM HoaDon h " +
                      "LEFT JOIN h.nguoiDangKyDichVu u " +
                      "WHERE h.trangThai = :status " +
                      "AND h.ngayThanhToan BETWEEN :start AND :end " +
                      "AND u.vaiTro IN (:residentRoles)"; // Không phải Admin/Kế toán
        
        List<UserRole> residentRoles = Arrays.asList(UserRole.khong_dung_he_thong, UserRole.nguoi_dung_thuong);

        try {
            BigDecimal sum = entityManager.createQuery(jpql, BigDecimal.class)
                    .setParameter("status", status)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .setParameter("residentRoles", residentRoles)
                    .getSingleResult();
            return sum != null ? sum : BigDecimal.ZERO;
        } catch (NoResultException e) {
            return BigDecimal.ZERO;
        }
    }
}