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
import BlueMoon.bluemoon.utils.AccountStatus;
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
        // 1. Kiểm tra dữ liệu đầu vào
        HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình đích: " + maHo));
            
        DoiTuong doiTuong = doiTuongDAO.findResidentByCccd(cccdThanhVien)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân với CCCD: " + cccdThanhVien));

        // 2. Kiểm tra trạng thái hiện tại của người này
        Optional<ThanhVienHo> tvhCuOpt = thanhVienHoDAO.findCurrentByCccd(cccdThanhVien);
        
        if (tvhCuOpt.isPresent()) {
            ThanhVienHo tvhCu = tvhCuOpt.get();
            
            // Nếu người này đang ở hộ khác
            if (!tvhCu.getHoGiaDinh().getMaHo().equals(maHo)) {
                // >>> CHẶN TUYỆT ĐỐI <<<
                throw new IllegalStateException("Cư dân này đang thuộc hộ gia đình: " 
                    + tvhCu.getHoGiaDinh().getTenHo() 
                    + " (Mã: " + tvhCu.getHoGiaDinh().getMaHo() + "). "
                    + "Vui lòng thực hiện thủ tục Rút Hộ (Xóa thành viên) khỏi hộ cũ trước khi thêm vào hộ mới.");
            } else {
                throw new IllegalArgumentException("Thành viên này ĐÃ CÓ trong hộ gia đình này rồi.");
            }
        }

        // 3. Nếu chưa thuộc hộ nào (tvhCuOpt rỗng) -> Tạo quan hệ mới
        @SuppressWarnings("rawtypes")
        ThanhVienHoID id = new ThanhVienHoID(cccdThanhVien, LocalDate.now());
        ThanhVienHo tvh = new ThanhVienHo(id, doiTuong, hgd, laChuHo, quanHe);
        
        return thanhVienHoDAO.save(tvh);
    }

    /**
     * Cập nhật Chủ hộ mới. Đảm bảo Chủ hộ cũ bị set `laChuHo = false`.
     */
    @Transactional
    public void capNhatChuHo(String maHo, String cccdChuHoMoi, String quanHeVoiChuHo) {
        // 1. Tìm Chủ hộ hiện tại và hạ cấp
        thanhVienHoDAO.findCurrentChuHoByHo(maHo).ifPresent(chuHoCu -> {
            if (!chuHoCu.getDoiTuong().getCccd().equals(cccdChuHoMoi)) {
                chuHoCu.setLaChuHo(false);
                chuHoCu.setQuanHeVoiChuHo(quanHeVoiChuHo);
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
        
        String maHo = tvh.getHoGiaDinh().getMaHo();
        
        // 1. Kiểm tra logic Chủ hộ
        if (tvh.getLaChuHo()) {
            // Đếm số thành viên ĐANG HOẠT ĐỘNG còn lại trong hộ
            long soThanhVienConLai = thanhVienHoDAO.countActiveByHoGiaDinh(maHo);
            
            if (soThanhVienConLai > 1) {
                 throw new IllegalStateException("KHÔNG THỂ XÓA CHỦ HỘ: Hộ gia đình vẫn còn thành viên khác. Vui lòng chuyển quyền Chủ hộ cho người khác trước khi xóa.");
            }
            
            // Nếu chỉ còn 1 mình chủ hộ -> Cho phép xóa và đánh dấu hộ giải thể
            HoGiaDinh hgd = tvh.getHoGiaDinh();
            hgd.setTrangThai(HouseholdStatus.giai_the);
            hoGiaDinhDAO.save(hgd);
            
            // Giải phóng căn hộ (nếu có) để người khác thuê
            taiSanChungCuDAO.findApartmentByHo(maHo).ifPresent(canHo -> {
                canHo.setHoGiaDinh(null);
                canHo.setTrangThai(BlueMoon.bluemoon.utils.AssetStatus.hoat_dong); // Trả về trạng thái trống
                taiSanChungCuDAO.save(canHo);
            });
        }

        // 2. Thực hiện xóa (kết thúc)
        tvh.setNgayKetThuc(LocalDate.now());
        tvh.setLyDoKetThuc(lyDo);
        thanhVienHoDAO.save(tvh);
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
    public HoGiaDinh tachHo(String maHoCu, List<String> cccdThanhVienDuocTach, String chuHoMoiCccd, String tenHoMoi, Integer maTaiSan) {
    
        // 1. Kiểm tra đầu vào
        if (!cccdThanhVienDuocTach.contains(chuHoMoiCccd)) {
            throw new IllegalArgumentException("Chủ hộ mới phải nằm trong danh sách thành viên được tách.");
        }
    
        @SuppressWarnings("unused")
        HoGiaDinh hgdCu = hoGiaDinhDAO.findById(maHoCu)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình cũ."));

        // 2. 🚫 KHÔNG CHO PHÉP TÁCH CHỦ HỘ CŨ
        Optional<ThanhVienHo> chuHoCuOpt = thanhVienHoDAO.findCurrentChuHoByHo(maHoCu);
        if (chuHoCuOpt.isPresent()) {
            String cccdChuHoCu = chuHoCuOpt.get().getDoiTuong().getCccd();
            if (cccdThanhVienDuocTach.contains(cccdChuHoCu)) {
                throw new IllegalArgumentException(
                    "Không thể tách Chủ hộ cũ. Vui lòng chuyển Chủ hộ trước hoặc chỉ tách thành viên khác."
                );
            }
        }
    
        // 3. Kết thúc mối quan hệ cũ của các thành viên được tách
        for (String cccd : cccdThanhVienDuocTach) {
            Optional<ThanhVienHo> tvhCuOpt = thanhVienHoDAO.findCurrentByCccd(cccd);

            if (tvhCuOpt.isPresent() && tvhCuOpt.get().getHoGiaDinh().getMaHo().equals(maHoCu)) {
                ThanhVienHo tvhCu = tvhCuOpt.get();
                tvhCu.setNgayKetThuc(LocalDate.now());
                tvhCu.setLyDoKetThuc(TerminationReason.tach_ho);
                thanhVienHoDAO.save(tvhCu);
            }
        }

        // 4. Tạo Hộ gia đình mới
        HoGiaDinh hoMoi = new HoGiaDinh();
        hoMoi.setMaHo(generateUniqueMaHo());
        hoMoi.setTenHo(tenHoMoi);
        hoMoi.setNgayThanhLap(LocalDate.now());
        hoMoi.setTrangThai(HouseholdStatus.hoat_dong);
        HoGiaDinh savedHoMoi = hoGiaDinhDAO.save(hoMoi);
    
        // 5. Gán căn hộ cho hộ mới (nếu có)
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
            canHo.setHoGiaDinh(savedHoMoi);
            taiSanChungCuDAO.save(canHo);
        }
    
        // 6. Thêm thành viên vào hộ mới
        for (String cccd : cccdThanhVienDuocTach) {
            DoiTuong thanhVien = doiTuongDAO.findResidentByCccd(cccd)
                .orElseThrow(() -> new RuntimeException("Lỗi dữ liệu: Không tìm thấy CCCD " + cccd));
        
            boolean laChuHo = cccd.equals(chuHoMoiCccd);
            String quanHe = laChuHo ? "Chủ hộ" : "Thành viên";
        
            @SuppressWarnings("rawtypes")
            ThanhVienHoID idMoi = new ThanhVienHoID(cccd, LocalDate.now());
            ThanhVienHo tvhMoi = new ThanhVienHo(idMoi, thanhVien, savedHoMoi, laChuHo, quanHe);
            thanhVienHoDAO.save(tvhMoi);
        }
    
        return savedHoMoi;
    }
    /**
     * Cập nhật thông tin Hộ gia đình và đổi Căn hộ (nếu chọn).
     */
    @Transactional
    public void capNhatHoGiaDinh(String maHo, HoGiaDinh thongTinMoi, Integer maCanHoMoi) {
        HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình: " + maHo));

        // 1. Cập nhật thông tin cơ bản
        hgd.setTenHo(thongTinMoi.getTenHo());
        hgd.setTrangThai(thongTinMoi.getTrangThai());
        hgd.setGhiChu(thongTinMoi.getGhiChu());
        
        hoGiaDinhDAO.save(hgd);

        // 2. Xử lý đổi Căn hộ (Nếu có chọn căn hộ mới)
        if (maCanHoMoi != null) {
            // A. Tìm căn hộ cũ (nếu có) và gỡ bỏ
            taiSanChungCuDAO.findApartmentByHo(maHo).ifPresent(canHoCu -> {
                // Nếu căn mới khác căn cũ thì mới gỡ
                if (!canHoCu.getMaTaiSan().equals(maCanHoMoi)) {
                    canHoCu.setHoGiaDinh(null);
                    canHoCu.setTrangThai(BlueMoon.bluemoon.utils.AssetStatus.hoat_dong); // Trả về trạng thái trống
                    taiSanChungCuDAO.save(canHoCu);
                }
            });

            // B. Gán căn hộ mới
            TaiSanChungCu canHoMoi = taiSanChungCuDAO.findByID(maCanHoMoi)
                .orElseThrow(() -> new IllegalArgumentException("Căn hộ mới không tồn tại."));
            
            // Kiểm tra lại xem căn mới có ai nhanh tay thuê mất chưa
            if (canHoMoi.getHoGiaDinh() != null && !canHoMoi.getHoGiaDinh().getMaHo().equals(maHo)) {
                throw new IllegalStateException("Căn hộ này vừa được người khác chọn. Vui lòng chọn căn khác.");
            }

            canHoMoi.setHoGiaDinh(hgd);
            canHoMoi.setTrangThai(BlueMoon.bluemoon.utils.AssetStatus.da_duoc_thue);
            taiSanChungCuDAO.save(canHoMoi);
        }
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
        // =======================================================
    // 4. THỐNG KÊ HỘ GIA ĐÌNH
    // =======================================================

    /**
     * Lấy dữ liệu thống kê hộ gia đình
     */
    public java.util.Map<String, Object> getHouseholdStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 1. Lấy tất cả hộ gia đình đang hoạt động
        List<HoGiaDinh> allHouseholds = hoGiaDinhDAO.findAll().stream()
                .filter(h -> h.getTrangThai() == HouseholdStatus.hoat_dong)
                .collect(java.util.stream.Collectors.toList());

        // 2. Thống kê theo tầng (dựa trên căn hộ) - lấy từ viTri
        java.util.Map<String, Long> floorStats = new java.util.HashMap<>();
        for (HoGiaDinh hgd : allHouseholds) {
            Optional<TaiSanChungCu> apartment = getApartmentByHousehold(hgd.getMaHo());
            if (apartment.isPresent() && apartment.get().getViTri() != null) {
                String viTri = apartment.get().getViTri();
                // Trích xuất số tầng từ chuỗi viTri (ví dụ: "Tầng 5, Tòa A" -> "Tầng 5")
                String floor = extractFloorFromViTri(viTri);
                if (floor != null) {
                    floorStats.put(floor, floorStats.getOrDefault(floor, 0L) + 1);
                }
            }
        }

        List<String> floorLabels = new java.util.ArrayList<>(floorStats.keySet());
        floorLabels.sort((a, b) -> {
            try {
                int floorA = Integer.parseInt(a.replace("Tầng ", ""));
                int floorB = Integer.parseInt(b.replace("Tầng ", ""));
                return Integer.compare(floorA, floorB);
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });
        List<Long> floorData = floorLabels.stream()
                .map(floorStats::get)
                .collect(java.util.stream.Collectors.toList());

        stats.put("householdFloorLabels", floorLabels);
        stats.put("householdFloorData", floorData);

        // 3. Tính trung bình số thành viên mỗi hộ
        long totalMembers = 0;
        for (HoGiaDinh hgd : allHouseholds) {
            // Đếm số thành viên hiện tại (ngayKetThuc == null)
            long memberCount = hgd.getThanhVienHoList().stream()
                    .filter(tv -> tv.getNgayKetThuc() == null)
                    .count();
            totalMembers += memberCount;
        }

        double avgMembers = allHouseholds.isEmpty() ? 0 : (double) totalMembers / allHouseholds.size();
        stats.put("averageMembers", Math.round(avgMembers * 10.0) / 10.0); // Làm tròn 1 chữ số
        stats.put("totalHouseholds", (long) allHouseholds.size());
        stats.put("totalMembers", totalMembers);

        // 4. Phân bố số thành viên (1 người, 2-3 người, 4-5 người, 6+ người)
        long single = 0; // 1 người
        long small = 0; // 2-3 người
        long medium = 0; // 4-5 người
        long large = 0; // 6+ người

        for (HoGiaDinh hgd : allHouseholds) {
            long memberCount = hgd.getThanhVienHoList().stream()
                    .filter(tv -> tv.getNgayKetThuc() == null)
                    .count();

            if (memberCount == 1)
                single++;
            else if (memberCount <= 3)
                small++;
            else if (memberCount <= 5)
                medium++;
            else
                large++;
        }

        List<String> sizeLabels = java.util.List.of("1 người", "2-3 người", "4-5 người", "6+ người");
        List<Long> sizeData = java.util.List.of(single, small, medium, large);

        stats.put("householdSizeLabels", sizeLabels);
        stats.put("householdSizeData", sizeData);

        return stats;
    }

    /**
     * Trích xuất tầng từ chuỗi viTri (ví dụ: "Tầng 5, Tòa A" -> "Tầng 5")
     */
    private String extractFloorFromViTri(String viTri) {
        if (viTri == null)
            return null;

        // Tìm pattern "Tầng X" hoặc "tầng X"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[Tt]ầng\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(viTri);

        if (matcher.find()) {
            return "Tầng " + matcher.group(1);
        }
        return null;
    }
    // =======================================================
    // 5. GIẢI THỂ HỘ GIA ĐÌNH (MỚI)
    // =======================================================

    /**
     * Trường hợp 1: Giải thể toàn bộ hộ (Ví dụ: Cả hộ chuyển đi nơi khác)
     * - Kết thúc tất cả thành viên.
     * - Trả căn hộ về trạng thái trống.
     * - Đổi trạng thái hộ sang 'giai_the'.
     */
    @Transactional
    public void giaiTheHoGiaDinh(String maHo, String lyDoChiTiet) {
        HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình: " + maHo));

        // 1. Kết thúc tất cả thành viên đang hoạt động
        List<ThanhVienHo> activeMembers = thanhVienHoDAO.findActiveByMaHo(maHo);
        for (ThanhVienHo tvh : activeMembers) {
            tvh.setNgayKetThuc(LocalDate.now());
            // Nếu lý do không được truyền, mặc định là chuyển hộ
            tvh.setLyDoKetThuc(TerminationReason.chuyen_di); 
            thanhVienHoDAO.save(tvh);
            
            // Cập nhật trạng thái Cư dân thành "Rời đi" (nếu chưa chết)
            DoiTuong cuDan = tvh.getDoiTuong();
            if (cuDan.getTrangThaiDanCu() != BlueMoon.bluemoon.utils.ResidentStatus.da_chet) {
                cuDan.setTrangThaiDanCu(BlueMoon.bluemoon.utils.ResidentStatus.roi_di);
                doiTuongDAO.save(cuDan);
            }
        }

        // 2. Trả Căn hộ (nếu có)
        taiSanChungCuDAO.findApartmentByHo(maHo).ifPresent(canHo -> {
            canHo.setHoGiaDinh(null);
            canHo.setTrangThai(BlueMoon.bluemoon.utils.AssetStatus.hoat_dong); // Trạng thái 'Trống'
            taiSanChungCuDAO.save(canHo);
        });

        // 3. Cập nhật trạng thái Hộ
        hgd.setTrangThai(HouseholdStatus.giai_the);
        // Có thể lưu lý do vào ghi chú
        String ghiChuCu = hgd.getGhiChu() != null ? hgd.getGhiChu() : "";
        hgd.setGhiChu(ghiChuCu + " | [Đã giải thể ngày " + LocalDate.now() + ": " + lyDoChiTiet + "]");
        
        hoGiaDinhDAO.save(hgd);
    }

    /**
     * Trường hợp 2: Báo tử thành viên.
     * - Nếu là người cuối cùng -> Tự động giải thể hộ.
     */
    @Transactional
    public void baoTuThanhVien(String cccd) {
        // 1. Tìm thành viên
        ThanhVienHo tvh = thanhVienHoDAO.findCurrentByCccd(cccd)
            .orElseThrow(() -> new IllegalArgumentException("Không Thấy Thành Viên"));

        // 2. Cập nhật trạng thái Cư dân -> Đã chết
        DoiTuong cuDan = tvh.getDoiTuong();
        cuDan.setTrangThaiDanCu(BlueMoon.bluemoon.utils.ResidentStatus.da_chet);
        doiTuongDAO.save(cuDan);

        // 3. Kết thúc quan hệ thành viên
        tvh.setNgayKetThuc(LocalDate.now());
        tvh.setLyDoKetThuc(TerminationReason.qua_doi);
        thanhVienHoDAO.save(tvh);
        HoGiaDinh hgd = tvh.getHoGiaDinh();
        String maHo = hgd.getMaHo();
        // 4. Kiểm tra xem còn ai trong hộ không?
        long soThanhVienConLai = thanhVienHoDAO.countActiveByHoGiaDinh(maHo);

        if (soThanhVienConLai == 0) {
            // Nếu không còn ai -> Giải thể hộ luôn
            giaiTheHoGiaDinh(maHo, "Tự động giải thể do thành viên cuối cùng đã qua đời.");
        } else if (tvh.getLaChuHo()) {
            // Nếu còn người nhưng người chết là Chủ hộ -> Yêu cầu chuyển chủ hộ (hoặc xử lý logic tạm thời)
            // Ở đây ta có thể ném Exception nhắc nhở hoặc tự động gán người khác (tùy nghiệp vụ)
            // Hiện tại ta để hộ tồn tại nhưng không có chủ hộ active (cần Admin vào sửa thủ công)
        }
    }
// =======================================================
    // XỬ LÝ BIẾN ĐỘNG (CHẾT HOẶC RỜI ĐI) TỪ DANH SÁCH CƯ DÂN
    // =======================================================

    /**
     * Hàm xử lý chung khi Admin set trạng thái cư dân là "Đã chết" hoặc "Rời đi".
     * @param cccd CCCD của người bị đổi trạng thái
     * @param lyDoKetThuc Lý do kết thúc trong bảng ThanhVienHo (qua_doi hoặc chuyen_ho)
     */
    @Transactional
    public String xuLyBienDongThanhVien(String cccd, TerminationReason lyDoKetThuc) {
        // 1. Tìm xem người này đang ở hộ nào
        Optional<ThanhVienHo> tvhOpt = thanhVienHoDAO.findCurrentByCccd(cccd);

        if (tvhOpt.isEmpty()) {
            return "Đã cập nhật trạng thái cư dân (Người này không thuộc hộ gia đình nào).";
        }

        ThanhVienHo tvh = tvhOpt.get();
        String maHo = tvh.getHoGiaDinh().getMaHo();
        String tenCuDan = tvh.getDoiTuong().getHoVaTen();

        // 2. Kết thúc tư cách thành viên của người này trong hộ
        tvh.setNgayKetThuc(LocalDate.now());
        tvh.setLyDoKetThuc(lyDoKetThuc);
        tvh.setLaChuHo(false);
        thanhVienHoDAO.save(tvh);
        Optional<DoiTuong> doiTuong = doiTuongDAO.findByCccd(cccd);
        DoiTuong dt = doiTuong.get();
        dt.setLaCuDan(false);
        dt.setTrangThaiTaiKhoan(AccountStatus.tam_ngung);
        doiTuongDAO.save(dt);
        // 3. Kiểm tra số thành viên CÒN LẠI trong hộ
        long soThanhVienConLai = thanhVienHoDAO.countActiveByHoGiaDinh(maHo);

        if (soThanhVienConLai == 0) {
            // TRƯỜNG HỢP 1: Hết người -> Tự động giải thể
            String lyDoGiaiThe = (lyDoKetThuc == TerminationReason.qua_doi) 
                ? "Tự động giải thể do thành viên cuối cùng qua đời." 
                : "Tự động giải thể do thành viên cuối cùng rời đi.";
                
            giaiTheHoGiaDinh(maHo, lyDoGiaiThe);
            
            return "Cư dân " + tenCuDan + " là thành viên cuối cùng. Hộ gia đình " + maHo + " đã được TỰ ĐỘNG GIẢI THỂ.";
        } else {
            // TRƯỜNG HỢP 2: Vẫn còn người
            if (tvh.getLaChuHo()) {
                return "Cảnh báo: " + tenCuDan + " là CHỦ HỘ. Vui lòng vào chi tiết hộ để chuyển quyền chủ hộ cho thành viên khác.";
            } else {
                return "Đã cập nhật danh sách thành viên hộ " + maHo + ".";
            }
        }
    }
}