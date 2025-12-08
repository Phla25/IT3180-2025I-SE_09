package BlueMoon.bluemoon.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.PhanHoiThongBaoDAO;
import BlueMoon.bluemoon.daos.ThongBaoDAO;
import BlueMoon.bluemoon.daos.ThongBaoDaDocDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.PhanHoiThongBao;
import BlueMoon.bluemoon.entities.ThongBao;
import BlueMoon.bluemoon.entities.ThongBaoDaDoc;
import BlueMoon.bluemoon.models.PhanHoiThongBaoDTO;

@Service
public class ThongBaoService {

    @Autowired
    private ThongBaoDAO thongBaoDAO;

    @Autowired private ThongBaoDaDocDAO thongBaoDaDocDAO;
    @Autowired private CuDanService cuDanService;

    @Autowired
    private PhanHoiThongBaoDAO phanHoiThongBaoDAO;

    @Transactional
    public ThongBao taoVaGuiThongBao(String tieuDe, String noiDung, DoiTuong nguoiTao) {
        ThongBao thongBao = new ThongBao();
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setNguoiGui(nguoiTao);
        thongBao.setThoiGianGui(LocalDateTime.now());
        return thongBaoDAO.save(thongBao);
    }

    public List<ThongBao> layTatCaThongBaoMoiNhat() {
        return thongBaoDAO.findAllByOrderByThoiGianGuiDesc();
    }

    public Optional<ThongBao> layThongBaoTheoMa(Integer maThongBao) {
        return thongBaoDAO.findById(maThongBao);
    }

    @Transactional
    public PhanHoiThongBao themPhanHoi(Integer maThongBao, DoiTuong nguoiGui, String noiDung) {
        Optional<ThongBao> thongBaoOpt = layThongBaoTheoMa(maThongBao);

        if (thongBaoOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông báo với mã: " + maThongBao);
        }

        PhanHoiThongBao phanHoi = new PhanHoiThongBao();
        phanHoi.setThongBao(thongBaoOpt.get()); // ✅ sửa ở đây
        phanHoi.setNguoiGui(nguoiGui);
        phanHoi.setNoiDung(noiDung);

        return phanHoiThongBaoDAO.save(phanHoi);
    }

 // Trong ThongBaoService.java

    public List<PhanHoiThongBao> layPhanHoiTheoThongBao(Integer maThongBao) {
        // Sửa để gọi phương thức FETCH JOIN
        return phanHoiThongBaoDAO.findByThongBaoMaThongBaoWithNguoiGuiEagerly(maThongBao); 
    }
    
    public List<PhanHoiThongBao> getPhanHoiByThongBao(Integer maThongBao) {
        return phanHoiThongBaoDAO.findByThongBaoMaThongBaoWithNguoiGuiEagerly(maThongBao);
    }

    @Transactional(readOnly = true)
    public List<PhanHoiThongBaoDTO> getPhanHoiDTOByThongBao(Integer maThongBao) {
        List<PhanHoiThongBao> list = phanHoiThongBaoDAO
            .findByThongBaoMaThongBaoWithNguoiGuiEagerly(maThongBao);

        return list.stream()
            .map(ph -> new PhanHoiThongBaoDTO(ph))
            .collect(Collectors.toList());
    }

    @Autowired private DoiTuongDAO doiTuongDAO; // Inject thêm

    // Helper: Lấy đại diện BQT để làm người gửi hệ thống
    private DoiTuong getSystemSender() {
        // Tìm người đầu tiên có vai trò ban_quan_tri
        return doiTuongDAO.findAll().stream()
                .filter(u -> u.getVaiTro() == BlueMoon.bluemoon.utils.UserRole.ban_quan_tri)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Hệ thống cần ít nhất 1 tài khoản Ban Quản Trị để gửi thông báo tự động."));
    }

    /**
     * Gửi thông báo đến TOÀN BỘ cư dân (Broadcast)
     */
    @Transactional
    public void guiThongBaoHeThongDenTatCa(String tieuDe, String noiDung) {
        ThongBao thongBao = new ThongBao();
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setNguoiGui(getSystemSender()); // Người gửi là BQT
        thongBao.setNguoiNhan(null); // Null nghĩa là gửi tất cả
        thongBao.setDoiTuongNhan(BlueMoon.bluemoon.utils.RecipientType.tat_ca);
        thongBao.setLoaiThongBao(BlueMoon.bluemoon.utils.NotificationType.quan_trong); // Hoặc urgent/normal tùy logic
        thongBao.setThoiGianGui(LocalDateTime.now());
        thongBao.setTrangThaiHienThi(true);
        
        thongBaoDAO.save(thongBao);
    }

    /**
     * Gửi thông báo đến MỘT cá nhân cụ thể
     */
    @Transactional
    public void guiThongBaoCaNhan(DoiTuong nguoiNhan, String tieuDe, String noiDung) {
        ThongBao thongBao = new ThongBao();
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setNguoiGui(getSystemSender());
        thongBao.setNguoiNhan(nguoiNhan); // Gán người nhận cụ thể
        // Giả sử RecipientType có giá trị 'ca_nhan' hoặc 'cu_dan', nếu không có thì set null hoặc logic tương ứng
        thongBao.setDoiTuongNhan(BlueMoon.bluemoon.utils.RecipientType.ca_nhan); // Tạm thời set tat_ca nhưng có nguoiNhan cụ thể
        // Tốt nhất nên thêm giá trị CA_NHAN vào Enum RecipientType
        
        thongBao.setLoaiThongBao(BlueMoon.bluemoon.utils.NotificationType.binh_thuong);
        thongBao.setThoiGianGui(LocalDateTime.now());
        thongBao.setTrangThaiHienThi(true);

        thongBaoDAO.save(thongBao);
    }
    public List<ThongBao> layThongBaoChoCuDan(String cccd) {
        return thongBaoDAO.findForResident(cccd, BlueMoon.bluemoon.utils.RecipientType.tat_ca);
    }
    // 1. Đếm số thông báo chưa đọc cho icon chuông (Cư dân)
    public long demSoThongBaoChuaDoc(String cccd) {
        return thongBaoDAO.countUnreadForResident(cccd, BlueMoon.bluemoon.utils.RecipientType.tat_ca);
    }

    // 2. Đánh dấu một thông báo là đã đọc (Khi cư dân click vào xem)
    @Transactional
    public void danhDauDaDoc(Integer maThongBao, String cccd) {
        // Kiểm tra xem đã đọc chưa, nếu chưa thì mới lưu
        if (!thongBaoDaDocDAO.existsByThongBaoMaThongBaoAndNguoiDocCccd(maThongBao, cccd)) {
            ThongBao tb = thongBaoDAO.findById(maThongBao).orElse(null);
            DoiTuong nguoiDoc = doiTuongDAO.findByCccd(cccd).orElse(null); // Giả sử bạn có DAO này inject vào Service
            
            if (tb != null && nguoiDoc != null) {
                ThongBaoDaDoc daDoc = new ThongBaoDaDoc(tb, nguoiDoc);
                thongBaoDaDocDAO.save(daDoc);
            }
        }
    }

    // 3. Kiểm tra trạng thái đã đọc của 1 thông báo cụ thể (để hiển thị UI đậm/nhạt)
    public boolean daDocThongBao(Integer maThongBao, String cccd) {
        return thongBaoDaDocDAO.existsByThongBaoMaThongBaoAndNguoiDocCccd(maThongBao, cccd);
    }

    // 4. Thống kê số người CHƯA ĐỌC cho Admin
    public long demSoNguoiChuaDoc(ThongBao thongBao) {
        if (thongBao.getDoiTuongNhan() == BlueMoon.bluemoon.utils.RecipientType.ca_nhan) {
            // Nếu gửi cá nhân: 1 - số người đã đọc (0 hoặc 1)
            long daDoc = thongBaoDaDocDAO.countByThongBaoMaThongBao(thongBao.getMaThongBao());
            return daDoc > 0 ? 0 : 1;
        } else {
            // Nếu gửi tất cả: Tổng cư dân - số người đã đọc
            long tongCuDan = cuDanService.layDanhSachCuDan().size();
            long daDoc = thongBaoDaDocDAO.countByThongBaoMaThongBao(thongBao.getMaThongBao());
            return (tongCuDan - daDoc) < 0 ? 0 : (tongCuDan - daDoc);
        }
    }
}