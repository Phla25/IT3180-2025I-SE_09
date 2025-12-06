package BlueMoon.bluemoon.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.BaoCaoSuCoDAO;
import BlueMoon.bluemoon.daos.PhanHoiDAO; // [MỚI] Import DAO Phản Hồi
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.PhanHoi; // [MỚI] Import Entity Phản Hồi
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Service
@Transactional
public class BaoCaoSuCoService {

    @Autowired
    private BaoCaoSuCoDAO suCoDAO;

    @Autowired
    private PhanHoiDAO phanHoiDAO; // [MỚI] Inject DAO để lấy lịch sử

    // ==========================================================
    // 1. CÁC PHƯƠNG THỨC LẤY DỮ LIỆU (READ)
    // ==========================================================

    /**
     * Lấy sự cố theo ID, ném lỗi nếu không tìm thấy (Dùng cho API nội bộ Service)
     */
    public BaoCaoSuCo getIncidentById(Integer id) {
        return suCoDAO.findById(id) // Sửa lại findById mặc định của JPA nếu DAO chưa custom findByIdWithDetails
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố với ID: " + id));
    }

    /**
     * [MỚI] Lấy sự cố theo ID trả về Optional (Dùng cho Controller để check null)
     * Tương thích với OfficerController: suCoService.getBaoCaoTheoMa(id)
     */
    public Optional<BaoCaoSuCo> getBaoCaoTheoMa(Integer id) {
        // Ưu tiên dùng method fetch join trong DAO nếu có để lấy kèm thông tin người dùng/tài sản
        // Nếu chưa viết query custom thì dùng findById mặc định
        return suCoDAO.findById(id); 
    }

    public List<BaoCaoSuCo> getAllIncidents() {
        return suCoDAO.findAllByOrderByThoiGianBaoCaoDesc();
    }

    public List<BaoCaoSuCo> getIncidentsByNguoiBaoCao(DoiTuong nguoiBaoCao) {
        return suCoDAO.findByNguoiBaoCaoOrderByThoiGianBaoCaoDesc(nguoiBaoCao);
    }

    public List<BaoCaoSuCo> getRecentIncidents(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("thoiGianBaoCao").descending());
        return suCoDAO.findAll(pageRequest).getContent();
    }
    
    // Lọc sự cố
    public List<BaoCaoSuCo> filterIncidents(String trangThaiStr, String mucDoStr) {
        IncidentStatus status = null;
        PriorityLevel priority = null;

        if (trangThaiStr != null && !trangThaiStr.isBlank()) {
            try { status = IncidentStatus.valueOf(trangThaiStr); } catch (Exception ignored) {}
        }
        if (mucDoStr != null && !mucDoStr.isBlank()) {
            try { priority = PriorityLevel.valueOf(mucDoStr); } catch (Exception ignored) {}
        }

        if (status == null && priority == null) {
            return getAllIncidents();
        }
        
        return suCoDAO.filterIncidents(status, priority);
    }

    // [MỚI] Lấy lịch sử xử lý (Timeline) cho sự cố
    // Tương thích với OfficerController: suCoService.getLichSuXuLy(id)
    public List<PhanHoi> getLichSuXuLy(Integer maBaoCao) {
        return phanHoiDAO.findByBaoCaoSuCoMaBaoCaoOrderByThoiGianPhanHoiDesc(maBaoCao);
    }

    // ==========================================================
    // 2. CÁC PHƯƠNG THỨC CẬP NHẬT DỮ LIỆU (WRITE)
    // ==========================================================

    public BaoCaoSuCo createIncident(DoiTuong nguoiBaoCao, TaiSanChungCu taiSan, String tieuDe, String noiDung, PriorityLevel mucDo) {
        BaoCaoSuCo suCo = new BaoCaoSuCo();
        suCo.setNguoiBaoCao(nguoiBaoCao);
        suCo.setTaiSan(taiSan);
        suCo.setTieuDe(tieuDe);
        suCo.setNoiDung(noiDung);
        suCo.setMucDoUuTien(mucDo);
        suCo.setTrangThai(IncidentStatus.moi_tiep_nhan);
        suCo.setThoiGianBaoCao(LocalDateTime.now());
        
        // Khi tạo mới, có thể thêm một dòng log vào lịch sử (Tuỳ chọn)
        return suCoDAO.save(suCo);
    }

    /**
     * Cập nhật trạng thái sự cố VÀ ghi nhận vào lịch sử
     */
    public BaoCaoSuCo updateIncidentStatus(Integer id, IncidentStatus newStatus, PriorityLevel newPriority, DoiTuong nguoiCapNhat, String ghiChu) {
        BaoCaoSuCo suCo = getIncidentById(id);
        
        // Lưu lại trạng thái cũ để so sánh (nếu cần log chi tiết hơn)
        // IncidentStatus oldStatus = suCo.getTrangThai();

        suCo.setTrangThai(newStatus);
        if (newPriority != null) {
            suCo.setMucDoUuTien(newPriority);
        }
        suCo.setThoiGianCapNhat(LocalDateTime.now());
        
        BaoCaoSuCo savedSuCo = suCoDAO.save(suCo);

        // [QUAN TRỌNG] Tạo bản ghi lịch sử (Phản hồi) tự động
        PhanHoi phanHoi = new PhanHoi();
        phanHoi.setBaoCaoSuCo(savedSuCo);
        phanHoi.setNguoiPhanHoi(nguoiCapNhat);
        phanHoi.setThoiGianPhanHoi(LocalDateTime.now());
        phanHoi.setNoiDung(ghiChu != null ? ghiChu : "Đã cập nhật trạng thái thành: " + newStatus.getDbValue());
        
        phanHoiDAO.save(phanHoi);

        return savedSuCo;
    }
    
    // Method update cũ (Giữ lại để tương thích ngược nếu cần, nhưng nên overload)
    public BaoCaoSuCo updateIncidentStatus(Integer id, IncidentStatus newStatus, PriorityLevel newPriority) {
        // Gọi method mới với người cập nhật là null (hoặc xử lý logic riêng)
        return updateIncidentStatus(id, newStatus, newPriority, null, "Cập nhật hệ thống");
    }

    // ==========================================================
    // 3. THỐNG KÊ (DASHBOARD)
    // ==========================================================
    
    public Long getTongSuCo() { return suCoDAO.count(); }
    public Long getSuCoDaXuLy() { return suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh); }
    public Long getSuCoDangXuLy() { return suCoDAO.countByTrangThai(IncidentStatus.dang_xu_ly); }
    
    // [LOGIC] Sự cố chưa xử lý = Mới tiếp nhận (Dashboard hiển thị riêng Đang xử lý rồi)
    public Long getSuCoChuaXuLy() { 
        return suCoDAO.countByTrangThai(IncidentStatus.moi_tiep_nhan); 
    }
    
    public int getTyLeDaXuLy() {
        long tongSuCo = getTongSuCo();
        if (tongSuCo == 0) return 0;
        long daXuLy = getSuCoDaXuLy();
        return (int) ((daXuLy * 100) / tongSuCo);
    }

    public Long getSuCoTheoMucDo(PriorityLevel mucDo) { return suCoDAO.countByMucDoUuTien(mucDo); }
    
 // [CẬP NHẬT] Method Filter mới cho Officer Controller
    public List<BaoCaoSuCo> searchIncidents(String keyword, String assetName, String statusStr, String priorityStr, LocalDate reportDate) {
        // 1. Xử lý Status Enum
        IncidentStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = IncidentStatus.valueOf(statusStr); } catch (Exception ignored) {}
        }

        // 2. Xử lý Priority Enum
        PriorityLevel priority = null;
        if (priorityStr != null && !priorityStr.isBlank()) {
            try { priority = PriorityLevel.valueOf(priorityStr); } catch (Exception ignored) {}
        }

        // 3. Xử lý Date -> DateTime Range (Từ 00:00:00 đến 23:59:59 của ngày đó)
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        if (reportDate != null) {
            fromDate = reportDate.atStartOfDay();
            toDate = reportDate.atTime(LocalTime.MAX);
        }

        // 4. Xử lý chuỗi rỗng thành null để Query hoạt động đúng
        if (keyword != null && keyword.isBlank()) keyword = null;
        if (assetName != null && assetName.isBlank()) assetName = null;

        return suCoDAO.searchIncidents(keyword, assetName, status, priority, fromDate, toDate);
    }
}