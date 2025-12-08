package BlueMoon.bluemoon.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.DangKyDichVuDAO;
import BlueMoon.bluemoon.daos.HoaDonDAO;
import BlueMoon.bluemoon.entities.DangKyDichVu;
import BlueMoon.bluemoon.entities.DichVu;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.utils.InvoiceType;
import BlueMoon.bluemoon.utils.RegistrationStatus;

@Service
public class DangKyDichVuService {

    @Autowired private DangKyDichVuDAO dangKyDichVuDAO;
    @Autowired private HoaDonDAO hoaDonDAO;
    @Autowired private DichVuService dichVuService;
    @Autowired private ThanhVienHoService thanhVienHoService;
    @Autowired private CuDanService cuDanService;

    // Cần thêm DangKyDichVuDAO để hỗ trợ save/findById.
    // Giả định DangKyDichVuDAO có các phương thức cơ bản: save(), findById(), findByNguoiDungAndDichVu()

    /**
     * Tạo yêu cầu đăng ký dịch vụ và tạo hóa đơn thanh toán lần đầu (nếu cần).
     * @param dangKyYeuCau DTO/Entity DangKyDichVu từ form.
     * @param cccdNguoiDangKy CCCD của cư dân.
     * @return DangKyDichVu đã được lưu.
     */
    @Transactional
    public DangKyDichVu taoYeuCauDangKy(DangKyDichVu dangKyYeuCau, String cccdNguoiDangKy) {
        
        // 1. Kiểm tra Dịch vụ và Người dùng
        DoiTuong nguoiDangKy = cuDanService.timCuDanTheoCCCD(cccdNguoiDangKy)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin cư dân."));
        
        DichVu dichVu = dichVuService.getDichVuById(dangKyYeuCau.getDichVu().getMaDichVu())
            .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại hoặc đã ngừng hoạt động."));

        // 2. Kiểm tra xem cư dân có thuộc hộ gia đình không (cần cho việc tạo Hóa đơn)
        Optional<HoGiaDinh> hoGiaDinhOpt = thanhVienHoService.getHoGiaDinhByCccd(cccdNguoiDangKy);
        @SuppressWarnings("unused")
        HoGiaDinh hoGiaDinh = hoGiaDinhOpt
            .orElseThrow(() -> new IllegalStateException("Cư dân phải thuộc một hộ gia đình để đăng ký dịch vụ."));
        
        // 3. Thiết lập thông tin cho đối tượng DangKyDichVu
        dangKyYeuCau.setNguoiDung(nguoiDangKy);
        dangKyYeuCau.setDichVu(dichVu); // Đảm bảo đối tượng DichVu được set đầy đủ
        dangKyYeuCau.setTrangThai(RegistrationStatus.dang_su_dung);
        dangKyYeuCau.setNgayDangKy(LocalDateTime.now());
        // Giả định các trường Ngày Bắt Đầu/Kết Thúc đã được set từ form nếu cần

        // 4. Lưu Yêu cầu đăng ký
        // Cần phương thức save() trong DangKyDichVuDAO
        DangKyDichVu savedDangKy = dangKyDichVuDAO.save(dangKyYeuCau);
        
        // 5. TÙY CHỌN: Tạo Hóa đơn ngay (thanh toán lần đầu)
        // Nếu là dịch vụ không định kỳ (ví dụ: Phí đăng ký 1 lần)
        if (dichVu.getGiaThanh().compareTo(BigDecimal.ZERO) > 0) {
            
            // Hóa đơn này là Hóa đơn dịch vụ đầu tiên/thanh toán phí đăng ký.
            HoaDon hoaDon = new HoaDon(hoGiaDinh, dichVu.getGiaThanh(), InvoiceType.dich_vu);
            hoaDon.setMaDichVu(dichVu.getMaDichVu());
            // Người đứng tên hóa đơn là người đăng ký dịch vụ (cccd_thanh_vien)
            hoaDon.setNguoiDangKyDichVu(nguoiDangKy); 
            hoaDon.setGhiChu("Phí đăng ký dịch vụ: " + dichVu.getTenDichVu());
            // Mặc định trạng thái là CHƯA THANH TOÁN
            
            hoaDonDAO.save(hoaDon); // Cần tạo hàm save(HoaDon) trong HoaDonService
        }


        return savedDangKy;
    }
    
    // Cần bổ sung DangKyDichVuDAO với hàm save
    // Cần bổ sung DangKyDichVuDAO với hàm findAllByNguoiDungCccd(String cccd)
    
    // Giả định có DangKyDichVuDAO.save(DangKyDichVu)
    // Giả định có DangKyDichVuDAO.findAllByNguoiDungCccd(String cccd)
    
    // Lưu ý: Cần update DangKyDichVuDAO
    // Đảm bảo DangKyDichVuDAO có phương thức save(DangKyDichVu)
    public DangKyDichVu save(DangKyDichVu dkdv) {
        // Giả định DangKyDichVuDAO.save đã tồn tại
        return dangKyDichVuDAO.save(dkdv);
    }
    public List<DangKyDichVu> getDichVuDaDangKyByCccd(String cccd) {
        return dangKyDichVuDAO.findAllByNguoiDungCccd(cccd);
    }
    public Optional<DangKyDichVu> getDangKyById(Integer maDangKy) {
        // Giả định DangKyDichVuDAO có findByIdWithDetails(Integer maDangKy) để fetch join
        return dangKyDichVuDAO.findById(maDangKy);
    }
    /**
     * Hủy đăng ký dịch vụ (Chuyển trạng thái sang da_huy).
     * Cư dân chỉ có thể hủy đăng ký nếu trạng thái là cho_duyet.
     */
    @Transactional
    public void huyDangKyDichVu(Integer maDangKy, String cccdNguoiDung) {
        DangKyDichVu dkdv = dangKyDichVuDAO.findById(maDangKy)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đăng ký."));
        
        // 1. Kiểm tra người dùng có phải là người đăng ký không
        if (!dkdv.getNguoiDung().getCccd().equals(cccdNguoiDung)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy yêu cầu đăng ký này.");
        }

        // 2. Kiểm tra trạng thái hợp lệ để hủy
        if (dkdv.getTrangThai() == RegistrationStatus.da_huy || dkdv.getTrangThai() == RegistrationStatus.da_ket_thuc) {
            throw new IllegalArgumentException("Yêu cầu này không thể hủy được nữa (Trạng thái hiện tại: " + dkdv.getTrangThai().name() + ").");
        }
        
        // Chỉ cho phép cư dân hủy khi đang chờ duyệt
        if (dkdv.getTrangThai() != RegistrationStatus.cho_duyet) {
            throw new IllegalArgumentException("Yêu cầu đã được duyệt, vui lòng liên hệ Ban Quản Trị để hủy.");
        }
        
        // 3. Thực hiện hủy
        dkdv.setTrangThai(RegistrationStatus.da_huy);
        dkdv.setGhiChu((dkdv.getGhiChu() != null ? dkdv.getGhiChu() + "\n" : "") + "Yêu cầu bị hủy bởi cư dân vào: " + LocalDateTime.now());
        dangKyDichVuDAO.save(dkdv);
    }
    
    // NEW: Lấy tất cả đăng ký dịch vụ (cho Admin)
    public List<DangKyDichVu> getAllDangKyDichVu() {
        // Giả định DangKyDichVuDAO có phương thức findAllWithDetails()
        return dangKyDichVuDAO.findAll();
    }

    public int countDichVuDaDangKyByNguoiDung(String cccd) {
        return dangKyDichVuDAO.countByNguoiDungCccd(cccd);
    }
}