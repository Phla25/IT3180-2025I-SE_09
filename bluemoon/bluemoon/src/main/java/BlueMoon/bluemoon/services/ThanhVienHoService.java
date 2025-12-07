package BlueMoon.bluemoon.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // THÊM IMPORT NÀY

import BlueMoon.bluemoon.daos.TaiSanChungCuDAO;
import BlueMoon.bluemoon.daos.ThanhVienHoDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.entities.ThanhVienHo;
import BlueMoon.bluemoon.models.HoGiaDinhDTO;

@Service
public class ThanhVienHoService {

    @Autowired private ThanhVienHoDAO thanhVienHoDAO;
    @Autowired private TaiSanChungCuDAO taiSanChungCuDAO;

    /**
     * Lấy thông tin chi tiết căn hộ của cư dân, sử dụng HoGiaDinhDTO.
     */
    @Transactional // <--- SỬA LỖI: Thêm @Transactional để cho phép Lazy Loading
    public HoGiaDinhDTO getCanHoInfo(String cccd, String hoVaTenNguoiDung) {
        // 1. Tìm mối quan hệ ThanhVienHo hiện tại của người dùng
        Optional<ThanhVienHo> tvhOpt = thanhVienHoDAO.findCurrentByCccd(cccd);

        if (tvhOpt.isEmpty()) {
            // Trường hợp 1: Người dùng chưa được gán vào hộ gia đình nào
            return new HoGiaDinhDTO("N/A", hoVaTenNguoiDung, "Chưa thuộc hộ nào");
        }

        HoGiaDinh hoGiaDinh = tvhOpt.get().getHoGiaDinh();
        // DÒNG NÀY (hoGiaDinh.getMaHo()) giờ an toàn trong Transaction
        String maHo = hoGiaDinh.getMaHo();
        
        // 2. Tìm Chủ hộ (Dùng findChuHoNameByHo)
        String tenChuHo = thanhVienHoDAO.findChuHoNameByHo(maHo).orElse(hoVaTenNguoiDung + " (Chủ hộ chưa xác định)");

        // 3. Tìm Căn hộ chính (TaiSanChungCu) thuộc Hộ gia đình này
        Optional<TaiSanChungCu> canHoOpt = taiSanChungCuDAO.findApartmentByHo(maHo); 

        if (canHoOpt.isEmpty()) {
            // Trường hợp 2: Có hộ nhưng chưa có căn hộ được gán
             return new HoGiaDinhDTO("Chưa gán", tenChuHo, "Không có căn hộ liên kết");
        }
        
        TaiSanChungCu canHo = canHoOpt.get();
        
        // 4. Trả về DTO hoàn chỉnh
        return new HoGiaDinhDTO(
            canHo.getTenTaiSan(), 
            tenChuHo,
            canHo.getTrangThai().getDbValue() // Lấy giá trị DB của Enum
        );
    }
    public Optional<HoGiaDinh> getHoGiaDinhByCccd(String cccd) {
        Optional<ThanhVienHo> tvhOpt = thanhVienHoDAO.findCurrentByCccd(cccd);
        return tvhOpt.map(ThanhVienHo::getHoGiaDinh);
    }
    public Optional<ThanhVienHo> getThanhVienHoByCccd(String cccd){
        return thanhVienHoDAO.findCurrentByCccd(cccd);
    }
    public Optional<DoiTuong> getChuHoByMaHo(String maHo){
        return thanhVienHoDAO.findChuHoByHo(maHo);
    }

    public List<ThanhVienHo> getActiveByMaHo(String maHo) {
        return thanhVienHoDAO.findActiveByMaHo(maHo);
    }
}