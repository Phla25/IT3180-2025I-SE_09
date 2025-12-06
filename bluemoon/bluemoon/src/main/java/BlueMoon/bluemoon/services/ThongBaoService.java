package BlueMoon.bluemoon.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.PhanHoiThongBaoDAO;
import BlueMoon.bluemoon.daos.ThongBaoDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.PhanHoiThongBao;
import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.utils.RecipientType;

@Service
public class ThongBaoService {

    @Autowired
    private ThongBaoDAO thongBaoDAO;

    @Autowired
    private DoiTuongDAO doiTuongDAO;

    @Autowired
    private PhanHoiThongBaoDAO phanHoiThongBaoDAO;

    /**
     * Lấy danh sách thông báo cho cư dân
     * Trạng thái đọc/chưa đọc sẽ được quản lý ở frontend (localStorage)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> layThongBaoChoCuDan(String cccd) {
        // Lấy tất cả thông báo dành cho cư dân
        List<ThongBao> thongBaoList = thongBaoDAO.findAllWithNguoiGuiEagerly();
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ThongBao tb : thongBaoList) {
            // Chỉ lấy thông báo hiển thị và dành cho tất cả hoặc cư dân
            if (tb.getTrangThaiHienThi() != null && tb.getTrangThaiHienThi() 
                && (tb.getDoiTuongNhan() == RecipientType.tat_ca || tb.getDoiTuongNhan() == RecipientType.chu_ho)) {
                
                Map<String, Object> item = new HashMap<>();
                item.put("maThongBao", tb.getMaThongBao());
                item.put("tieuDe", tb.getTieuDe());
                item.put("noiDung", tb.getNoiDung());
                item.put("thoiGianGui", tb.getThoiGianGui());
                item.put("loaiThongBao", tb.getLoaiThongBao() != null ? tb.getLoaiThongBao().name() : "binh_thuong");
                
                // Thông tin người gửi
                if (tb.getNguoiGui() != null) {
                    item.put("nguoiGui", tb.getNguoiGui().getHoVaTen());
                }
                
                result.add(item);
            }
        }
        
        return result;
    }

    /**
     * Lấy chi tiết thông báo kèm phản hồi
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> layChiTietThongBao(Integer maThongBao, String cccd) {
        Optional<ThongBao> tbOpt = thongBaoDAO.findById(maThongBao);
        
        if (tbOpt.isEmpty()) {
            return Optional.empty();
        }
        
        ThongBao tb = tbOpt.get();
        
        // Kiểm tra quyền xem
        if (tb.getTrangThaiHienThi() == null || !tb.getTrangThaiHienThi()) {
            return Optional.empty();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("maThongBao", tb.getMaThongBao());
        result.put("tieuDe", tb.getTieuDe());
        result.put("noiDung", tb.getNoiDung());
        result.put("thoiGianGui", tb.getThoiGianGui());
        result.put("loaiThongBao", tb.getLoaiThongBao() != null ? tb.getLoaiThongBao().name() : "binh_thuong");
        
        // Thông tin người gửi
        if (tb.getNguoiGui() != null) {
            result.put("nguoiGui", tb.getNguoiGui().getHoVaTen());
        }
        
        // Lấy danh sách phản hồi
        List<PhanHoiThongBao> phanHoiList = phanHoiThongBaoDAO.findByThongBaoMaThongBaoWithNguoiGuiEagerly(maThongBao);
        List<Map<String, Object>> phanHoiData = new ArrayList<>();
        
        for (PhanHoiThongBao ph : phanHoiList) {
            Map<String, Object> phItem = new HashMap<>();
            phItem.put("noiDung", ph.getNoiDung());
            phItem.put("thoiGianGui", ph.getThoiGianGui());
            if (ph.getNguoiGui() != null) {
                phItem.put("nguoiGui", ph.getNguoiGui().getHoVaTen());
            }
            phanHoiData.add(phItem);
        }
        
        result.put("phanHoi", phanHoiData);
        
        return Optional.of(result);
    }

    /**
     * Gửi phản hồi cho thông báo
     */
    @Transactional
    public void guiPhanHoi(Integer maThongBao, String cccd, String noiDung) {
        Optional<ThongBao> tbOpt = thongBaoDAO.findById(maThongBao);
        Optional<DoiTuong> nguoiGuiOpt = doiTuongDAO.findByCccd(cccd);
        
        if (tbOpt.isEmpty() || nguoiGuiOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông báo hoặc người dùng");
        }
        
        ThongBao tb = tbOpt.get();
        DoiTuong nguoiGui = nguoiGuiOpt.get();
        
        PhanHoiThongBao phanHoi = new PhanHoiThongBao(tb, nguoiGui, noiDung);
        phanHoiThongBaoDAO.save(phanHoi);
    }

    /**
     * Lấy tất cả thông báo mới nhất (cho admin)
     */
    @Transactional(readOnly = true)
    public List<ThongBao> layTatCaThongBaoMoiNhat() {
        return thongBaoDAO.findAllWithNguoiGuiEagerly();
    }

    /**
     * Tạo và gửi thông báo mới
     */
    @Transactional
    public void taoVaGuiThongBao(String tieuDe, String noiDung, DoiTuong nguoiGui) {
        ThongBao thongBao = new ThongBao();
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setNguoiGui(nguoiGui);
        thongBao.setDoiTuongNhan(RecipientType.tat_ca);
        thongBao.setTrangThaiHienThi(true);
        
        thongBaoDAO.save(thongBao);
    }

    /**
     * Lấy danh sách phản hồi theo thông báo
     */
    @Transactional(readOnly = true)
    public List<PhanHoiThongBao> layPhanHoiTheoThongBao(Integer maThongBao) {
        return phanHoiThongBaoDAO.findByThongBaoMaThongBaoWithNguoiGuiEagerly(maThongBao);
    }

    /**
     * Thêm phản hồi vào thông báo
     */
    @Transactional
    public void themPhanHoi(Integer maThongBao, DoiTuong nguoiGui, String noiDung) {
        Optional<ThongBao> thongBaoOpt = thongBaoDAO.findById(maThongBao);
        
        if (thongBaoOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông báo");
        }
        
        PhanHoiThongBao phanHoi = new PhanHoiThongBao(thongBaoOpt.get(), nguoiGui, noiDung);
        phanHoiThongBaoDAO.save(phanHoi);
    }
}
