package BlueMoon.bluemoon.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.DichVuDAO;
import BlueMoon.bluemoon.entities.DichVu;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.utils.AssetStatus;

@Service
public class DichVuService {

    @Autowired
    private DichVuDAO dichVuDAO;

    @Autowired
    private NguoiDungService nguoiDungService; 

    // Lấy tất cả dịch vụ
    public List<DichVu> getAllDichVu() {
        return dichVuDAO.findAll();
    }

    // Lấy dịch vụ theo ID
    public Optional<DichVu> getDichVuById(Integer maDichVu) {
        return dichVuDAO.findById(maDichVu);
    }

    /**
     * Thêm mới hoặc cập nhật dịch vụ.
     * @param dichVu Đối tượng dịch vụ từ form.
     * @param banQuanTriCccd CCCD của Admin/Ban Quản Trị đang thực hiện.
     * @return Đối tượng DichVu đã được lưu.
     */
    @Transactional
    public DichVu saveOrUpdateDichVu(DichVu dichVu, String banQuanTriCccd) {
        
        // 1. Kiểm tra tính hợp lệ cơ bản
        if (dichVu.getTenDichVu() == null || dichVu.getTenDichVu().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống.");
        }
        if (dichVu.getGiaThanh() == null || dichVu.getGiaThanh().signum() < 0) {
            throw new IllegalArgumentException("Giá thành phải là một số không âm.");
        }
        if (dichVu.getLoaiDichVu() == null) {
            throw new IllegalArgumentException("Loại dịch vụ không được để trống.");
        }
        
        // 2. Lấy thông tin Ban Quản Trị
        // Sử dụng phương thức có sẵn trong AdminController (giả định NguoiDungService có hàm này)
        DoiTuong banQuanTri = nguoiDungService.timBanQuanTriTheoID(banQuanTriCccd)
                                   .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin Ban Quản Trị."));
        
        // 3. Xử lý logic cho thêm mới hoặc cập nhật
        if (dichVu.getMaDichVu() == null) {
            // THÊM MỚI
            
            // Kiểm tra trùng lặp tên
            Optional<DichVu> existingService = dichVuDAO.findByTenDichVu(dichVu.getTenDichVu());
            if (existingService.isPresent()) {
                throw new IllegalArgumentException("Tên dịch vụ '" + dichVu.getTenDichVu() + "' đã tồn tại.");
            }
            
            // Thiết lập giá trị mặc định
            dichVu.setBanQuanTri(banQuanTri);
            if (dichVu.getTrangThai() == null) {
                dichVu.setTrangThai(AssetStatus.hoat_dong); 
            }
            // dichVu.setNgayTao() được xử lý bởi @PrePersist trong Entity (DichVu.java)
            
        } else {
            // CẬP NHẬT
            DichVu existingDichVu = dichVuDAO.findById(dichVu.getMaDichVu())
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ cần cập nhật không tồn tại."));
            
            // Giữ nguyên Ban Quản Trị và Ngày Tạo ban đầu
            dichVu.setBanQuanTri(existingDichVu.getBanQuanTri());
            dichVu.setNgayTao(existingDichVu.getNgayTao());

            // Kiểm tra trùng lặp tên (ngoại trừ chính nó)
            Optional<DichVu> existingName = dichVuDAO.findByTenDichVu(dichVu.getTenDichVu());
            if (existingName.isPresent() && !existingName.get().getMaDichVu().equals(dichVu.getMaDichVu())) {
                throw new IllegalArgumentException("Tên dịch vụ '" + dichVu.getTenDichVu() + "' đã được sử dụng bởi dịch vụ khác.");
            }
        }
        
        return dichVuDAO.save(dichVu);
    }

    /**
     * Xóa mềm dịch vụ (chuyển trạng thái sang Ngừng Hoạt Động).
     * @param maDichVu Mã dịch vụ cần xóa/ngừng hoạt động.
     */
    @Transactional
    public void deleteDichVu(Integer maDichVu) {
        DichVu dichVu = dichVuDAO.findById(maDichVu)
            .orElseThrow(() -> new IllegalArgumentException("Dịch vụ cần xóa không tồn tại."));

        // Chuyển trạng thái sang ngừng hoạt động thay vì xóa cứng
        dichVu.setTrangThai(AssetStatus.ngung_hoat_dong); 
        dichVuDAO.save(dichVu);
    }
    public List<DichVu> getAllActiveDichVu() {
        return dichVuDAO.findByTrangThai(AssetStatus.hoat_dong); 
    }
}