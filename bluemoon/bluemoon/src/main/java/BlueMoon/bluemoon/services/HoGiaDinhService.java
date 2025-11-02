package BlueMoon.bluemoon.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.HoGiaDinhDAO;
import BlueMoon.bluemoon.daos.TaiSanChungCuDAO;
import BlueMoon.bluemoon.daos.ThanhVienHoDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.entities.ThanhVienHo;
import BlueMoon.bluemoon.entities.ThanhVienHoID;
import BlueMoon.bluemoon.utils.AssetStatus;
import BlueMoon.bluemoon.utils.HouseholdStatus;
import BlueMoon.bluemoon.utils.TerminationReason;

@Service
public class HoGiaDinhService {

    @Autowired private HoGiaDinhDAO hoGiaDinhDAO;
    @Autowired private ThanhVienHoDAO thanhVienHoDAO;
    @Autowired private DoiTuongDAO doiTuongDAO;
    @Autowired private TaiSanChungCuDAO taiSanChungCuDAO;

    // =======================================================
    // 1. CHỨC NĂNG CƠ BẢN (CRUD)
    // =======================================================
    public List<HoGiaDinh> getAllHouseholds() {
        return hoGiaDinhDAO.findAll();
    }
    public List<HoGiaDinh> getAllHouseholds(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return hoGiaDinhDAO.search(keyword);
        }
        return hoGiaDinhDAO.findAll();
    }

    public Optional<HoGiaDinh> getHouseholdById(String maHo) {
        return hoGiaDinhDAO.findById(maHo);
    }
    /**
     * Helper: Tạo Mã Hộ (9 chữ số) duy nhất bằng cách tạo ngẫu nhiên và kiểm tra trùng lặp.
     * @return Chuỗi 9 chữ số (String).
     */
    private String generateUniqueMaHo() {
        Random random = new Random();
        String maHo;
        do {
            // Tạo số ngẫu nhiên từ 100,000,000 đến 999,999,999 (9 chữ số)
            int randomNumber = random.nextInt(900_000_000) + 100_000_000;
            maHo = String.valueOf(randomNumber);
        } while (hoGiaDinhDAO.existsById(maHo)); // Kiểm tra tính duy nhất trong DB
        
        return maHo;
    }
    /**
     * Thêm Hộ gia đình mới
     * @param hoGiaDinh Hộ gia đình cần thêm (chưa có mã hộ)
     * @param chuHoCccd CCCD của người sẽ là Chủ hộ
     * @param quanHeVoiChuHo Quan hệ của Chủ hộ với chính họ (thường là "Chủ hộ")
     * @param maTaiSan Mã Tài Sản của căn hộ muốn liên kết (có thể null) <-- THÊM THAM SỐ NÀY
     * @return HoGiaDinh đã lưu
     */
    @jakarta.transaction.Transactional
    public HoGiaDinh themHoGiaDinh(HoGiaDinh hoGiaDinh, String chuHoCccd, String quanHeVoiChuHo, Integer maTaiSan) { // <-- CẬP NHẬT
        // 1. Tạo Mã Hộ duy nhất
        if (hoGiaDinh.getMaHo() == null || hoGiaDinh.getMaHo().trim().isEmpty()) {
            hoGiaDinh.setMaHo(generateUniqueMaHo());
        } else if (hoGiaDinhDAO.existsById(hoGiaDinh.getMaHo())) {
            throw new IllegalArgumentException("Mã Hộ đã tồn tại.");
        }

        // 2. Thiết lập trạng thái mặc định
        hoGiaDinh.setNgayThanhLap(LocalDate.now());
        hoGiaDinh.setTrangThai(HouseholdStatus.hoat_dong);
        
        // 3. Lưu Hộ gia đình
        HoGiaDinh savedHo = hoGiaDinhDAO.save(hoGiaDinh);
        
        // 4. LOGIC GÁN CĂN HỘ MỚI (NEW)
        if (maTaiSan != null) {
            TaiSanChungCu canHo = taiSanChungCuDAO.findByID(maTaiSan)
                .orElseThrow(() -> new IllegalArgumentException("Mã Tài Sản Căn Hộ không hợp lệ."));
                
            if (canHo.getHoGiaDinh() != null) {
                 throw new IllegalStateException("Căn hộ đã có chủ. Vui lòng chọn căn hộ khác.");
            }
            if (canHo.getLoaiTaiSan() != BlueMoon.bluemoon.utils.AssetType.can_ho) {
                throw new IllegalArgumentException("Tài sản được chọn không phải là Căn Hộ.");
            }
            canHo.setTrangThai(AssetStatus.da_duoc_thue);
            canHo.setHoGiaDinh(savedHo); // Gán hộ mới vào căn hộ
            taiSanChungCuDAO.save(canHo); // Cập nhật căn hộ
        }

        // 5. Thêm Chủ hộ vào ThanhVienHo
        if (chuHoCccd != null && !chuHoCccd.trim().isEmpty()) {
            DoiTuong chuHo = doiTuongDAO.findResidentByCccd(chuHoCccd)
                                     .orElseThrow(() -> new IllegalArgumentException("CCCD Chủ hộ không hợp lệ."));
            
            themThanhVien(savedHo.getMaHo(), chuHo.getCccd(), true, quanHeVoiChuHo, TerminationReason.chuyen_chu_ho);
        }

        return savedHo;
    }
    
    // Hàm themHoGiaDinh cũ (không có maTaiSan) cần được giữ lại hoặc xử lý
    // Nếu bạn chỉ dùng hàm mới, hãy xóa hàm cũ hoặc thay thế nó:
    // (Bỏ qua việc xóa/thay thế hàm cũ, chỉ tập trung vào cập nhật hàm mới)

    /**
     * Cập nhật thông tin Hộ gia đình
     */
    @Transactional
    public HoGiaDinh capNhatHoGiaDinh(String maHo, HoGiaDinh hoGiaDinhCapNhat) {
        HoGiaDinh hgdHienTai = hoGiaDinhDAO.findById(maHo)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));

        hgdHienTai.setTenHo(hoGiaDinhCapNhat.getTenHo());
        hgdHienTai.setGhiChu(hoGiaDinhCapNhat.getGhiChu());
        hgdHienTai.setTrangThai(hoGiaDinhCapNhat.getTrangThai()); // Cho phép BQT thay đổi trạng thái
        
        return hoGiaDinhDAO.save(hgdHienTai);
    }

    // =======================================================
    // 2. QUẢN LÝ THÀNH VIÊN
    // =======================================================

    /**
     * Thêm thành viên vào hộ gia đình. Tự động kiểm tra và kết thúc mối quan hệ cũ.
     */
    @Transactional
    public ThanhVienHo themThanhVien(String maHo, String cccdThanhVien, Boolean laChuHo, String quanHe, TerminationReason lyDoKetThuc) {
        HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));
        DoiTuong doiTuong = doiTuongDAO.findResidentByCccd(cccdThanhVien)
                                   .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân với CCCD: " + cccdThanhVien));

        // 1. Kiểm tra và kết thúc mối quan hệ cũ (nếu có)
        thanhVienHoDAO.findCurrentByCccd(cccdThanhVien).ifPresent(thanhVienCu -> {
            thanhVienCu.setNgayKetThuc(LocalDate.now());
            thanhVienCu.setLyDoKetThuc(lyDoKetThuc);
            thanhVienHoDAO.save(thanhVienCu);
        });

        // 2. Tạo ID mới và Entity ThanhVienHo mới
        @SuppressWarnings("rawtypes")
        ThanhVienHoID id = new ThanhVienHoID(cccdThanhVien, LocalDate.now());
        ThanhVienHo tvh = new ThanhVienHo(id, doiTuong, hgd, laChuHo, quanHe);
        ThanhVienHo savedTvh = thanhVienHoDAO.save(tvh);
        return savedTvh;
    }

    /**
     * Cập nhật Chủ hộ mới. Đảm bảo Chủ hộ cũ bị set `laChuHo = false`.
     */
    @Transactional
    public void capNhatChuHo(String maHo, String cccdChuHoMoi) {
        // 1. Tìm Chủ hộ hiện tại và hạ cấp
        thanhVienHoDAO.findCurrentChuHoByHo(maHo).ifPresent(chuHoCu -> {
            if (!chuHoCu.getDoiTuong().getCccd().equals(cccdChuHoMoi)) {
                chuHoCu.setLaChuHo(false);
                thanhVienHoDAO.save(chuHoCu);
            }
        });

        // 2. Tìm thành viên mới và nâng cấp
        Optional<ThanhVienHo> tvhMoiOpt = thanhVienHoDAO.findCurrentByCccd(cccdChuHoMoi);
        if (tvhMoiOpt.isEmpty()) {
            throw new IllegalArgumentException("Thành viên mới chưa thuộc hộ nào hoặc không tồn tại.");
        }
        
        ThanhVienHo tvhMoi = tvhMoiOpt.get();
        if (!tvhMoi.getHoGiaDinh().getMaHo().equals(maHo)) {
             throw new IllegalArgumentException("Thành viên mới không thuộc Hộ gia đình này.");
        }
        
        tvhMoi.setLaChuHo(true);
        tvhMoi.setQuanHeVoiChuHo("Chủ hộ");
        thanhVienHoDAO.save(tvhMoi);
    }
    
    /**
     * Kết thúc mối quan hệ của một thành viên với hộ hiện tại (Rút hộ)
     */
    @Transactional
    public void xoaThanhVienKhoiHo(String cccdThanhVien, TerminationReason lyDo) {
        ThanhVienHo tvh = thanhVienHoDAO.findCurrentByCccd(cccdThanhVien)
            .orElseThrow(() -> new IllegalArgumentException("Thành viên không thuộc Hộ gia đình nào."));
        
        // KHÔNG được xóa nếu là Chủ hộ và hộ vẫn còn thành viên khác
        if (tvh.getLaChuHo() && hoGiaDinhDAO.countMembersInHousehold(tvh.getHoGiaDinh().getMaHo()) > 1) {
             throw new IllegalStateException("Không thể xóa Chủ hộ khi hộ còn thành viên khác. Vui lòng chuyển Chủ hộ trước.");
        }

        tvh.setNgayKetThuc(LocalDate.now());
        tvh.setLyDoKetThuc(lyDo);
        thanhVienHoDAO.save(tvh);
        
        // Tùy chọn: Xử lý nếu người đó là Chủ hộ và là thành viên cuối cùng -> Đánh dấu hộ không hoạt động
        if (tvh.getLaChuHo() && hoGiaDinhDAO.countMembersInHousehold(tvh.getHoGiaDinh().getMaHo()) == 1) {
            HoGiaDinh hgd = tvh.getHoGiaDinh();
            hgd.setTrangThai(HouseholdStatus.giai_the); // Đánh dấu hộ không hoạt động
            hoGiaDinhDAO.save(hgd);
        }
    }

    // =======================================================
    // 3. TÁCH HỘ (LOGIC PHỨC TẠP)
    // =======================================================

    /**
     * Tách một nhóm thành viên sang một hộ mới.
     * @param maHoCu Mã hộ cũ
     * @param cccdThanhVienDuocTach List CCCD của các thành viên được tách
     * @param chuHoMoiCccd CCCD của Chủ hộ mới (phải nằm trong list cccdThanhVienDuocTach)
     * @param tenHoMoi Tên của Hộ gia đình mới
     * @param maTaiSan Mã Tài Sản của căn hộ muốn liên kết (có thể null) <-- THÊM THAM SỐ NÀY
     * @return HoGiaDinh mới được tạo
     */
    @Transactional
    public HoGiaDinh tachHo(String maHoCu, List<String> cccdThanhVienDuocTach, String chuHoMoiCccd, String tenHoMoi, Integer maTaiSan) { // <-- CẬP NHẬT
        
        // 1. Kiểm tra đầu vào
        if (!cccdThanhVienDuocTach.contains(chuHoMoiCccd)) {
            throw new IllegalArgumentException("Chủ hộ mới phải nằm trong danh sách thành viên được tách.");
        }
        HoGiaDinh hgdCu = hoGiaDinhDAO.findById(maHoCu)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình cũ."));

        // 2. Tạo Hộ gia đình mới và Chủ hộ (Sử dụng hàm themHoGiaDinh mới)
        HoGiaDinh hoMoi = new HoGiaDinh();
        hoMoi.setTenHo(tenHoMoi);
        // Gọi hàm themHoGiaDinh đã cập nhật (có gán căn hộ)
        HoGiaDinh savedHoMoi = themHoGiaDinh(hoMoi, chuHoMoiCccd, "Chủ hộ", maTaiSan); // <-- GỌI HÀM CẬP NHẬT

        // 3. Xử lý các thành viên còn lại được tách
        for (String cccd : cccdThanhVienDuocTach) {
            // Chủ hộ mới đã được xử lý ở bước 2, ta chỉ cần xử lý các thành viên khác
            if (cccd.equals(chuHoMoiCccd)) continue; 
            
            Optional<ThanhVienHo> tvhCuOpt = thanhVienHoDAO.findCurrentByCccd(cccd);
            
            if (tvhCuOpt.isPresent() && tvhCuOpt.get().getHoGiaDinh().getMaHo().equals(maHoCu)) {
                ThanhVienHo tvhCu = tvhCuOpt.get();

                // a) Kết thúc tư cách thành viên ở hộ cũ
                tvhCu.setNgayKetThuc(LocalDate.now());
                tvhCu.setLyDoKetThuc(TerminationReason.tach_ho);
                thanhVienHoDAO.save(tvhCu);

                // b) Thêm vào hộ mới (Giả định quan hệ là "Thành viên")
                DoiTuong thanhVien = doiTuongDAO.findResidentByCccd(cccd)
                                          .orElseThrow(() -> new RuntimeException("Lỗi dữ liệu: Không tìm thấy CCCD " + cccd));
                
                @SuppressWarnings("rawtypes")
                ThanhVienHoID idMoi = new ThanhVienHoID(cccd, LocalDate.now());
                ThanhVienHo tvhMoi = new ThanhVienHo(idMoi, thanhVien, savedHoMoi, false, "Thành viên");
                thanhVienHoDAO.save(tvhMoi);
            }
        }
        
        // 4. Kiểm tra và xử lý Chủ hộ cũ (nếu Chủ hộ cũ bị tách)
        Optional<ThanhVienHo> chuHoCuOpt = thanhVienHoDAO.findCurrentChuHoByHo(maHoCu);
        if (chuHoCuOpt.isPresent() && cccdThanhVienDuocTach.contains(chuHoCuOpt.get().getDoiTuong().getCccd())) {
            
            if (hoGiaDinhDAO.countMembersInHousehold(maHoCu) > 0) { // Nếu còn thành viên sau khi tách
                 throw new IllegalStateException("Hộ cũ còn thành viên. Vui lòng chọn Chủ hộ mới cho Hộ cũ (" + maHoCu + ") trước khi tách Chủ hộ cũ.");
            } else {
                 // Nếu hộ cũ không còn ai, đánh dấu hộ cũ không hoạt động
                 hgdCu.setTrangThai(HouseholdStatus.giai_the);
                 hoGiaDinhDAO.save(hgdCu);
            }
        }
        
        return savedHoMoi;
    }
    // =======================================================
    // 3. LOGIC HIỂN THỊ THÔNG TIN CĂN HỘ (NEW)
    // =======================================================

    /**
     * Lấy thông tin căn hộ chính (AssetType.can_ho) mà Hộ gia đình đang sở hữu.
     * @param maHo Mã hộ gia đình
     * @return Optional<TaiSanChungCu>
     */
    public Optional<TaiSanChungCu> getApartmentByHousehold(String maHo) {
        // Kiểm tra Hộ gia đình tồn tại để tránh NPE cho DAO
        if (!hoGiaDinhDAO.findById(maHo).isPresent()) {
            return Optional.empty();
        }
        
        // Sử dụng TaiSanChungCuDAO để tìm căn hộ liên kết
        return taiSanChungCuDAO.findApartmentByHo(maHo);
    }
    @jakarta.transaction.Transactional
    public Optional<ThanhVienHo> getThanhVienHoCurrentByCccd(String cccd) {
        return thanhVienHoDAO.findCurrentByCccd(cccd);
    }
}