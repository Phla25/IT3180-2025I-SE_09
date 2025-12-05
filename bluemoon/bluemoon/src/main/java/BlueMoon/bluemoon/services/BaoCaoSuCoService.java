package BlueMoon.bluemoon.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import BlueMoon.bluemoon.daos.BaoCaoSuCoDAO;
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Service
public class BaoCaoSuCoService {

    @Autowired
    private BaoCaoSuCoDAO suCoDAO;

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
        // Sự cố chưa tiếp nhận (moi_tiep_nhan) + Sự cố đang chờ xử lý (dang_xu_ly)
        // Để khớp với Dashboard, ta sẽ tính: Tổng - Đã hoàn thành.
        return suCoDAO.countByTrangThai(IncidentStatus.moi_tiep_nhan);
    }

    /**
     * Tính tỷ lệ sự cố đã hoàn thành trên tổng số.
     */
    public int getTyLeDaXuLy() {
        long tongSuCo = suCoDAO.countAll();
        long suCoDaXuLy = suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh);

        if (tongSuCo == 0) {
            return 0; // Tránh chia cho 0
        }
        // Tính toán tỷ lệ phần trăm
        return (int) ((suCoDaXuLy * 100) / tongSuCo);
    }

    public Long getSuCoTheoMucDo(PriorityLevel mucDo) {
        return suCoDAO.countByMucDoUuTien(mucDo);
    }

    /**
     * Lấy danh sách sự cố gần đây, sắp xếp theo ngày báo cáo.
     * Sử dụng findAll từ DAO và sắp xếp ở Service, sau đó giới hạn.
     */
    public List<BaoCaoSuCo> getRecentIncidents(int limit) {
        List<BaoCaoSuCo> allIncidents = suCoDAO.findAll();

        // Sắp xếp theo ngày báo cáo giảm dần (mới nhất lên đầu)
        allIncidents.sort(Comparator.comparing(
                (BaoCaoSuCo sc) -> sc.getThoiGianBaoCao() != null ? sc.getThoiGianBaoCao() : LocalDateTime.MIN,
                Comparator.reverseOrder()));

        // Giới hạn số lượng
        return allIncidents.size() > limit ? allIncidents.subList(0, limit) : allIncidents;
    }
}