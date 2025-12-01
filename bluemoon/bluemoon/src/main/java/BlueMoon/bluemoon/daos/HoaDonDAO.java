package BlueMoon.bluemoon.daos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.utils.InvoiceStatus;
import BlueMoon.bluemoon.utils.InvoiceType;
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
    public Optional<HoaDon> findById(Integer maHoaDon) {
        // Sử dụng Integer cho maHoaDon
        String jpql = "SELECT hd FROM HoaDon hd "
                + "JOIN FETCH hd.hoGiaDinh h " 
                // SỬA: Dùng tên thuộc tính mới cho Người tạo HĐ/Người đăng ký
                + "LEFT JOIN FETCH hd.nguoiDangKyDichVu ndkdv " 
                // SỬA: Dùng tên thuộc tính mới cho Người Thanh Toán/Xác nhận
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
    
    // PHƯƠNG THỨC MỚI ĐỂ GIẢI QUYẾT LỖI BIÊN DỊCH
    public List<HoaDon> findByTrangThaiOrderByNgayTao(InvoiceStatus trangThai) {
        // Tạm thời sử dụng truy vấn đơn giản nhất
        String jpql = "SELECT hd FROM HoaDon hd JOIN FETCH hd.hoGiaDinh h WHERE hd.trangThai = :trangThai ORDER BY hd.ngayTao DESC";
        return entityManager.createQuery(jpql, HoaDon.class)
            .setParameter("trangThai", trangThai)
            .getResultList();
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
     * CẬP NHẬT: Lấy tất cả hóa đơn với Fetch Join (Cho Admin/Kế toán)
     * Đảm bảo tải: HoGiaDinh, NguoiDangKyDichVu, NguoiThanhToan
     */
    public List<HoaDon> findAllWithDetails() {
        String jpql = "SELECT hd FROM HoaDon hd "
                    + "JOIN FETCH hd.hoGiaDinh h " 
                    + "LEFT JOIN FETCH hd.nguoiDangKyDichVu ndkdv " // Người đăng ký dịch vụ/Người tạo hóa đơn
                    + "LEFT JOIN FETCH hd.nguoiThanhToan ntt " // Người thanh toán/Xác nhận
                    + "ORDER BY hd.ngayTao DESC";
        return entityManager.createQuery(jpql, HoaDon.class).getResultList();
    }
    
}