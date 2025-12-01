package BlueMoon.bluemoon.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import BlueMoon.bluemoon.daos.BaoCaoSuCoDAO;
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.utils.AssetType;
import BlueMoon.bluemoon.utils.IncidentStatus;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Service
public class BaoCaoSuCoService {

    @Autowired
    private BaoCaoSuCoDAO suCoDAO;

    // ==============================
    // 1. Tạo sự cố mới
    // ==============================
    public BaoCaoSuCo createIncident(DoiTuong nguoiBaoCao,
            AssetType loaiTaiSan,
            String tieuDe,
            String noiDung,
            PriorityLevel mucDo) {
BaoCaoSuCo suCo = new BaoCaoSuCo();
suCo.setNguoiBaoCao(nguoiBaoCao);
suCo.setLoaiTaiSan(loaiTaiSan);
suCo.setTieuDe(tieuDe);
suCo.setNoiDung(noiDung);
suCo.setMucDoUuTien(mucDo);
suCo.setTrangThai(IncidentStatus.moi_tiep_nhan);
suCo.setThoiGianBaoCao(LocalDateTime.now());
return suCoDAO.save(suCo);
}



    // ==============================
    // 2. Lấy chi tiết sự cố
    // ==============================
    public BaoCaoSuCo getIncidentById(Integer id) {
        return suCoDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố với ID: " + id));
    }

    // ==============================
    // 3. Lấy danh sách sự cố
    // ==============================
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

    // ==============================
    // 4. Cập nhật trạng thái
    // ==============================
    public BaoCaoSuCo updateIncidentStatus(Integer id, IncidentStatus newStatus, PriorityLevel newPriority) {
        Optional<BaoCaoSuCo> optionalSuCo = suCoDAO.findById(id);
        if (optionalSuCo.isPresent()) {
            BaoCaoSuCo suCo = optionalSuCo.get();
            suCo.setTrangThai(newStatus);
            suCo.setMucDoUuTien(newPriority);
            suCo.setThoiGianCapNhat(LocalDateTime.now());
            return suCoDAO.save(suCo);
        } else {
            throw new RuntimeException("Không tìm thấy sự cố để cập nhật với ID: " + id);
        }
    }

    // ==============================
    // 5. Thống kê
    // ==============================
    public Long getTongSuCo() {
        return suCoDAO.count();
    }

    public Long getSuCoDaXuLy() {
        return suCoDAO.countByTrangThai(IncidentStatus.da_hoan_thanh);
    }

    public Long getSuCoDangXuLy() {
        return suCoDAO.countByTrangThai(IncidentStatus.dang_xu_ly);
    }

    public Long getSuCoChuaXuLy() {
        return suCoDAO.countByTrangThai(IncidentStatus.moi_tiep_nhan)
                + suCoDAO.countByTrangThai(IncidentStatus.dang_xu_ly);
    }

    public int getTyLeDaXuLy() {
        long tongSuCo = getTongSuCo();
        if (tongSuCo == 0) return 0;
        long daXuLy = getSuCoDaXuLy();
        return (int) ((daXuLy * 100) / tongSuCo);
    }

    public Long getSuCoTheoMucDo(PriorityLevel mucDo) {
        return suCoDAO.countByMucDoUuTien(mucDo);
    }
}
