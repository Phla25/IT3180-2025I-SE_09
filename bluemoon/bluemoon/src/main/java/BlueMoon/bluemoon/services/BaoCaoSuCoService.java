package BlueMoon.bluemoon.services;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import BlueMoon.bluemoon.daos.BaoCaoSuCoDAO;
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.IncidentType;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Service
public class BaoCaoSuCoService {

    @Autowired
    private BaoCaoSuCoDAO suCoDAO; // Chỉ giữ 1 biến DAO duy nhất

    public Long getTongSuCo() {
        return suCoDAO.countAll();
    }

    public Long getSuCoDaXuLy() {
        return suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh);
    }

    public Long getSuCoDangXuLy() {
        return suCoDAO.countByTrangThai(IncidentStatus.dang_xu_ly);
    }
    
    public Long getSuCoChuaXuLy() {
        return suCoDAO.countByTrangThai(IncidentStatus.moi_tiep_nhan);
    }

    public int getTyLeDaXuLy() {
        long tongSuCo = suCoDAO.countAll();
        long suCoDaXuLy = suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh);
        return (tongSuCo > 0) ? (int) ((suCoDaXuLy * 100) / tongSuCo) : 0;
    }
    
    public Long getSuCoTheoMucDo(PriorityLevel mucDo) {
        return suCoDAO.countByMucDoUuTien(mucDo);
    }

    public List<BaoCaoSuCo> getRecentIncidents(int limit) {
        List<BaoCaoSuCo> allIncidents = suCoDAO.findAll();
        // Sắp xếp giảm dần theo mã báo cáo (hoặc thời gian nếu có) để lấy mới nhất
        allIncidents.sort(Comparator.comparing(BaoCaoSuCo::getMaBaoCao).reversed());
        return allIncidents.size() > limit ? allIncidents.subList(0, limit) : allIncidents;
    }

    public List<BaoCaoSuCo> getSuCoByNguoiDung(String cccd) {
        return suCoDAO.findByNguoiBaoCao(cccd);
    }

    // --- CÁC HÀM TẠO SỰ CỐ (ĐÃ SỬA) ---

    @Transactional // Đảm bảo lưu dữ liệu an toàn
    public void taoBaoCaoTuCuDan(BaoCaoSuCo baoCao, DoiTuong nguoiBaoCao) {
        baoCao.setNguoiBaoCao(nguoiBaoCao);
        baoCao.setTaiSan(null); 
        baoCao.setThoiGianBaoCao(java.time.LocalDateTime.now());
        baoCao.setTrangThai(IncidentStatus.cho_phe_duyet); // Cư dân -> Chờ duyệt
        
        applyPriorityLogic(baoCao); // Chỉ set logic ưu tiên

        suCoDAO.save(baoCao); // Lưu 1 lần duy nhất
    }

    @Transactional // Đảm bảo lưu dữ liệu an toàn
    public void taoBaoCaoTuAdmin(BaoCaoSuCo baoCao, DoiTuong admin) {
        baoCao.setNguoiBaoCao(admin);
        baoCao.setTaiSan(null);
        baoCao.setThoiGianBaoCao(java.time.LocalDateTime.now());
        baoCao.setTrangThai(IncidentStatus.moi_tiep_nhan); // Admin -> Mới tiếp nhận
        
        applyPriorityLogic(baoCao); // Chỉ set logic ưu tiên

        suCoDAO.save(baoCao); // Lưu 1 lần duy nhất
    }

    // Hàm này chỉ xử lý logic, KHÔNG gọi DAO.save()
    private void applyPriorityLogic(BaoCaoSuCo baoCao) {
        if (baoCao.getLoaiSuCo() != null) {
            switch (baoCao.getLoaiSuCo()) {
                case an_ninh -> baoCao.setMucDoUuTien(PriorityLevel.khan_cap);
                case mat_do -> baoCao.setMucDoUuTien(PriorityLevel.cao);
                case dien, nuoc -> baoCao.setMucDoUuTien(PriorityLevel.binh_thuong);
                default -> baoCao.setMucDoUuTien(PriorityLevel.thap);
            }
        } else {
            baoCao.setMucDoUuTien(PriorityLevel.thap);
        }
    }
    public List<BaoCaoSuCo> filterSuCoCuDan(String cccd, String keyword, 
                                            BlueMoon.bluemoon.utils.IncidentType type,
                                            BlueMoon.bluemoon.utils.PriorityLevel priority,
                                            LocalDate date, Integer hour) {
        
        java.time.LocalDateTime startDateTime = null;
        java.time.LocalDateTime endDateTime = null;

        // Nếu người dùng chọn ngày, ta tìm trong khoảng từ đầu ngày đến cuối ngày đó
        if (date != null) {
            startDateTime = date.atStartOfDay();
            endDateTime = date.atTime(23, 59, 59);
        }

        return suCoDAO.findByCuDanWithFilter(cccd, keyword, type, priority, startDateTime, endDateTime, hour);
    }
    public List<BaoCaoSuCo> filterSuCoAdmin(String keyword, 
                                            String reporterName, // <--- THÊM MỚI
                                            IncidentStatus status,
                                            PriorityLevel priority,
                                            IncidentType type,
                                            LocalDate date, 
                                            Integer hour) {
        
        java.time.LocalDateTime startDateTime = null;
        java.time.LocalDateTime endDateTime = null;

        if (date != null) {
            startDateTime = date.atStartOfDay();
            endDateTime = date.atTime(23, 59, 59);
        }

        // Truyền thêm reporterName vào DAO
        return suCoDAO.findWithFilterAdmin(keyword, reporterName, status, priority, type, startDateTime, endDateTime, hour);
    }

    public Optional<BaoCaoSuCo> getSuCoById(Integer id) {
        return suCoDAO.findById(id);
    }

    public Integer countAllSuCoByNguoiDung(String cccd) {
        return suCoDAO.countByNguoiBaoCao(cccd);
    }

    public Integer getSuCoDaXuLyTheoNguoiDung(String cccd) {
        return suCoDAO.sumSoSuCoByNguoiBaoCaoAndTrangThai(cccd, IncidentStatus.da_hoan_thanh);
    }

    public int countSuCoDangXuLyByNguoiDung(String cccd) {
        return suCoDAO.sumSoSuCoByNguoiBaoCaoAndTrangThai(cccd, IncidentStatus.dang_xu_ly);
    }
}