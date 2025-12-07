package BlueMoon.bluemoon.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.ThanhVienHo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@Repository
public class ThanhVienHoDAO {
    @Autowired
    private EntityManager entityManager;

    /**
     * SỬA LỖI: Sử dụng JPQL để tìm ThanhVienHo theo CCCD (String), tránh TypeMismatchException.
     * Tìm bản ghi đang hoạt động (ngayKetThuc IS NULL) của cư dân.
     */
    public Optional<ThanhVienHo> findCurrentByCccd(String cccd) {
        // Truy vấn theo thuộc tính cccd của đối tượng doiTuong trong ThanhVienHo
        String jpql = "SELECT tvh FROM ThanhVienHo tvh WHERE tvh.doiTuong.cccd = :cccd AND tvh.ngayKetThuc IS NULL";
        try {
             return Optional.of(entityManager.createQuery(jpql, ThanhVienHo.class)
                                            .setParameter("cccd", cccd)
                                            .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Tìm tên Chủ hộ (DoiTuong.hoVaTen) của một Hộ gia đình.
     */
    public Optional<String> findChuHoNameByHo(String maHo) {
        String jpql = "SELECT tvh.doiTuong.hoVaTen FROM ThanhVienHo tvh " +
                      "WHERE tvh.hoGiaDinh.maHo = :maHo AND tvh.laChuHo = true AND tvh.ngayKetThuc IS NULL";
        try {
            return Optional.of(entityManager.createQuery(jpql, String.class)
                                            .setParameter("maHo", maHo)
                                            .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    public Optional<DoiTuong> findChuHoByHo(String maHo){
        String jpql = "SELECT tvh.doiTuong FROM ThanhVienHo tvh " +
                    "WHERE tvh.hoGiaDinh.maHo = :maHo AND tvh.laChuHo = true AND tvh.ngayKetThuc IS NULL";
        try {
            return Optional.of(entityManager.createQuery(jpql, DoiTuong.class)
                                            .setParameter("maHo", maHo)
                                            .getSingleResult());
        } catch (NoResultException e){
            return Optional.empty();
        }
    }
    /**
     * Tìm bản ghi ThanhVienHo đang hoạt động là Chủ hộ của một Hộ gia đình.
     */
    public Optional<ThanhVienHo> findCurrentChuHoByHo(String maHo) {
        String jpql = "SELECT tvh FROM ThanhVienHo tvh " +
                      "WHERE tvh.hoGiaDinh.maHo = :maHo AND tvh.laChuHo = true AND tvh.ngayKetThuc IS NULL";
        try {
            return Optional.of(entityManager.createQuery(jpql, ThanhVienHo.class)
                                            .setParameter("maHo", maHo)
                                            .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public ThanhVienHo save(ThanhVienHo thanhVien) {
        return entityManager.merge(thanhVien);
    }

    public List<ThanhVienHo> findActiveByMaHo(String maHo) {
        String jpql = "SELECT tvh FROM ThanhVienHo tvh WHERE tvh.hoGiaDinh.maHo = :maHo AND tvh.ngayKetThuc IS NULL";
        return entityManager.createQuery(jpql, ThanhVienHo.class)
                            .setParameter("maHo", maHo)
                            .getResultList();
    }
}