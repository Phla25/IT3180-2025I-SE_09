package BlueMoon.bluemoon.services;

import BlueMoon.bluemoon.daos.LichSuRaVaoDAO;
import BlueMoon.bluemoon.entities.LichSuRaVao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LichSuRaVaoService {

    @Autowired
    private LichSuRaVaoDAO lichSuRaVaoDAO;
    @Autowired
    private ExportService exportService;

    // Cho Cư Dân: Xem lịch sử của chính mình
    public List<LichSuRaVao> getHistoryByPerson(String cccd, LocalDateTime start, LocalDateTime end) {
        return lichSuRaVaoDAO.findByCuDan_CccdAndThoiGianBetweenOrderByThoiGianDesc(cccd, start, end);
    }

    // Cho Admin: Xem và lọc toàn bộ (Xử lý LocalDate -> LocalDateTime tại đây)
    public List<LichSuRaVao> getAllLogs(String keyword, LocalDate date, String gate) {
        LocalDateTime start;
        LocalDateTime end;

        if (date != null) {
            start = date.atStartOfDay(); 
            end = date.atTime(23, 59, 59);
        } else {
            start = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
            end = LocalDateTime.of(2100, 12, 31, 23, 59, 59);
        }
        
        // XỬ LÝ KEYWORD TẠI ĐÂY (Thêm % hai đầu)
        if (keyword != null && !keyword.trim().isEmpty()) {
            keyword = "%" + keyword.trim().toLowerCase() + "%"; // Chuyển thường và thêm %
        } else {
            keyword = null;
        }

        if (gate != null && (gate.trim().isEmpty() || gate.equals(""))) {
            gate = null;
        }

        return lichSuRaVaoDAO.customFilter(keyword, start, end, gate);
    }

    // Lấy danh sách các cổng
    public List<String> getAllGates() {
        return lichSuRaVaoDAO.findDistinctCongKiemSoat();
    }

    public String importLichSuRaVaoFromExcel(MultipartFile file) {
        return exportService.importLichSuRaVaoFromExcel(file);
    }
}