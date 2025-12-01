package BlueMoon.bluemoon.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import BlueMoon.bluemoon.daos.PhanHoiThongBaoDAO;
import BlueMoon.bluemoon.models.PhanHoiThongBaoDTO; // SỬ DỤNG DTO BẠN CUNG CẤP

@Service
public class PhanHoiThongBaoService {

    @Autowired
    private PhanHoiThongBaoDAO phanHoiThongBaoDAO;

    /**
     * Lấy tất cả phản hồi cho một thông báo và chuyển đổi sang DTO.
     * Sử dụng Fetch Join trong DAO để tối ưu hiệu suất.
     * @param maThongBao Mã thông báo cần xem phản hồi.
     * @return Danh sách PhanHoiThongBaoDTO.
     */
    public List<PhanHoiThongBaoDTO> getRepliesByMaThongBao(Integer maThongBao) {
        
        // Gọi DAO sử dụng Fetch Join để lấy dữ liệu NguoiGui
        return phanHoiThongBaoDAO.findByThongBaoMaThongBaoWithNguoiGuiEagerly(maThongBao)
                .stream()
                // Chuyển đổi Entity sang DTO bằng Constructor của DTO
                .map(PhanHoiThongBaoDTO::new)
                .collect(Collectors.toList());
    }
}