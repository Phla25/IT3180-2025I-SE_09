package BlueMoon.bluemoon.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.PhanHoiDAO;
import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.PhanHoi;

@Service
public class PhanHoiService {

    @Autowired
    private PhanHoiDAO phanHoiDAO;

    /**
     * Lấy lịch sử phản hồi/tiến độ của một sự cố
     */
    public List<PhanHoi> getFeedbackByIncident(BaoCaoSuCo suCo) {
        return phanHoiDAO.findByBaoCaoSuCoOrderByThoiGianPhanHoiAsc(suCo);
    }

    /**
     * Admin thêm cập nhật tiến độ mới
     */
    @Transactional
    public PhanHoi addAdminUpdate(BaoCaoSuCo suCo, DoiTuong admin, String noiDung) {
        PhanHoi phanHoi = new PhanHoi();
        phanHoi.setBaoCaoSuCo(suCo);
        phanHoi.setNguoiPhanHoi(admin);
        phanHoi.setNoiDung(noiDung);
        phanHoi.setThoiGianPhanHoi(LocalDateTime.now());
        
        return phanHoiDAO.save(phanHoi);
    }
}