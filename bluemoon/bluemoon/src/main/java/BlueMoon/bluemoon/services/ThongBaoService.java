package BlueMoon.bluemoon.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private PhanHoiThongBaoDAO phanHoiThongBaoDAO;

    @Autowired private ThongBaoDaDocDAO thongBaoDaDocDAO; // Inject DAO mới
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
    
    @Transactional
    public void danhDauDaDoc(Integer maThongBao, DoiTuong nguoiDoc) {
        ThongBao thongBao = thongBaoDAO.findById(maThongBao)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));

        // Kiểm tra nếu chưa đọc thì mới lưu
        if (!thongBaoDaDocDAO.existsByThongBaoAndNguoiDoc(thongBao, nguoiDoc)) {
            ThongBaoDaDoc daDoc = new ThongBaoDaDoc(thongBao, nguoiDoc);
            thongBaoDaDocDAO.save(daDoc);
        }
    }

    // [MỚI] Lấy số lượng người đã đọc
    public long laySoLuongDaDoc(Integer maThongBao) {
        ThongBao thongBao = new ThongBao(); 
        thongBao.setMaThongBao(maThongBao); // Chỉ cần set ID để tìm kiếm
        return thongBaoDaDocDAO.countByThongBao(thongBao);
    }

    // [MỚI] Lấy danh sách chi tiết người đã đọc
    public List<ThongBaoDaDoc> layDanhSachDaDoc(Integer maThongBao) {
        return thongBaoDaDocDAO.findByThongBaoIdWithUser(maThongBao);
    }
    
 // [MỚI] Lấy số lượng thông báo chưa đọc
    public long demSoThongBaoChuaDoc(DoiTuong nguoiDoc) {
        return thongBaoDAO.countUnreadByNguoiDoc(nguoiDoc);
    }



}