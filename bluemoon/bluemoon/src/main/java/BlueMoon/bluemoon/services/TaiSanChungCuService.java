package BlueMoon.bluemoon.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.HoGiaDinhDAO;
import BlueMoon.bluemoon.daos.TaiSanChungCuDAO;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.utils.AssetType;

@Service
public class TaiSanChungCuService {

    // Inject DAO để tương tác với cơ sở dữ liệu
    @Autowired private TaiSanChungCuDAO taiSanChungCuDAO;
    @Autowired private HoGiaDinhDAO hoGiaDinhDAO; // Cần để tìm HoGiaDinh khi liên kết

    // =======================================================
    // 1. CHỨC NĂNG READ (Đọc)
    // =======================================================
     /**
     * Lấy danh sách tất cả các Tài sản Chung Cư (bao gồm cả căn hộ).
     * @param loaiTaiSan Loại tài sản cần lọc (null để lấy tất cả).
     * @return List<TaiSanChungCu>
     */
    public List<TaiSanChungCu> getAllAssets(AssetType loaiTaiSan) {
        return taiSanChungCuDAO.findAllAssets(loaiTaiSan);
    }

    /**
     * Lấy một Tài Sản Chung Cư theo Mã Tài Sản.
     */
    public Optional<TaiSanChungCu> getAssetById(Integer maTaiSan) {
        // Có thể dùng findByID chung từ DAO
        return taiSanChungCuDAO.findByID(maTaiSan); 
    }

    /**
     * Tìm kiếm tài sản theo tên hoặc loại (sử dụng cho Controller).
     */
    public List<TaiSanChungCu> findAssetsByFilters(String keyword, AssetType loaiTaiSan) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return taiSanChungCuDAO.findAssetsByName(keyword);
        }
        return taiSanChungCuDAO.findAllAssets(loaiTaiSan);
    }
    /**
     * Lấy danh sách tất cả các tầng có căn hộ.
     * @return List<String> danh sách tầng.
     */
    public List<String> getAllApartmentFloors() {
        return taiSanChungCuDAO.findAllApartmentFloors();
    }

    /**
     * Lấy danh sách căn hộ trống theo tầng.
     * @param viTri Vị trí (Tầng) cần tìm.
     * @return List<TaiSanChungCu> danh sách căn hộ trống.
     */
    public List<TaiSanChungCu> getEmptyApartmentsByFloor(String viTri) {
        return taiSanChungCuDAO.findEmptyApartmentsByFloor(viTri);
    }
    /**
     * Lấy danh sách tất cả các Căn hộ (loaiTaiSan = can_ho).
     * @return List<TaiSanChungCu> là danh sách các căn hộ.
     */
    public List<TaiSanChungCu> getAllApartments() {
        return taiSanChungCuDAO.findAllApartments();
    }

    /**
     * Lấy một Tài Sản Chung Cư (Căn hộ) theo Mã Tài Sản.
     */
    public Optional<TaiSanChungCu> getApartmentById(Integer maTaiSan) {
        return taiSanChungCuDAO.findByID(maTaiSan);
    }
    /**
     * Phân loại căn hộ theo Trạng thái.
     * @param status Trạng thái cần lọc.
     * @return List<TaiSanChungCu>.
     */
    public List<TaiSanChungCu> getApartmentsByStatus(BlueMoon.bluemoon.utils.AssetStatus status) {
        return taiSanChungCuDAO.findApartmentsByStatus(status);
    }
    
    /**
     * Phân loại căn hộ theo dải Diện tích.
     * @param minDienTich Diện tích tối thiểu.
     * @param maxDienTich Diện tích tối đa.
     * @return List<TaiSanChungCu>.
     */
    public List<TaiSanChungCu> getApartmentsByAreaRange(java.math.BigDecimal minDienTich, java.math.BigDecimal maxDienTich) {
        return taiSanChungCuDAO.findApartmentsByAreaRange(minDienTich, maxDienTich);
    }

    /**
     * Phân loại căn hộ theo dải Giá trị.
     * @param minGiaTri Giá trị tối thiểu.
     * @param maxGiaTri Giá trị tối đa.
     * @return List<TaiSanChungCu>.
     */
    public List<TaiSanChungCu> getApartmentsByValueRange(java.math.BigDecimal minGiaTri, java.math.BigDecimal maxGiaTri) {
        return taiSanChungCuDAO.findApartmentsByValueRange(minGiaTri, maxGiaTri);
    }

    /**
     * Lấy danh sách căn hộ có thể lọc theo nhiều tiêu chí (Sử dụng cho Controller).
     * @param status Trạng thái (Optional).
     * @param minArea Diện tích nhỏ nhất (Optional).
     * @param maxArea Diện tích lớn nhất (Optional).
     * @return List<TaiSanChungCu> kết quả.
     */
    public List<TaiSanChungCu> findApartmentsByFilters(BlueMoon.bluemoon.utils.AssetStatus status, java.math.BigDecimal minArea, java.math.BigDecimal maxArea, java.math.BigDecimal minValue, java.math.BigDecimal maxValue) {
        // Đây là cách đơn giản để xử lý lọc: nếu không có tham số nào, trả về tất cả.
        // Đối với logic lọc phức tạp hơn (kết hợp nhiều trường), cần viết JPQL động trong DAO.
        
        // Vì DAO hiện tại chỉ có các hàm lọc đơn, ta sẽ cần thêm logic kết hợp trong DAO hoặc Service, 
        // nhưng tạm thời, ta sẽ chỉ hỗ trợ lọc đơn giản (hoặc kết hợp nếu cả min/max đều có).
        
        if (minArea != null && maxArea != null) {
            return taiSanChungCuDAO.findApartmentsByAreaRange(minArea, maxArea);
        }
        if (minValue != null && maxValue != null) {
            return taiSanChungCuDAO.findApartmentsByValueRange(minValue, maxValue);
        }
        if (status != null) {
            return taiSanChungCuDAO.findApartmentsByStatus(status);
        }
        
        // Nếu không có bộ lọc nào được áp dụng, trả về tất cả căn hộ
        return taiSanChungCuDAO.findAllApartments();
    }
    
    // =======================================================
    // 2. CHỨC NĂNG CREATE (Tạo)
    // =======================================================
    /**
     * Thêm một Tài Sản Chung Cư mới (áp dụng cho mọi loại tài sản).
     * @param taiSanMoi Entity TaiSanChungCu chứa thông tin tài sản.
     * @param maHo Mã Hộ gia đình liên kết (có thể là null hoặc chuỗi rỗng).
     * @return TaiSanChungCu đã được lưu.
     */
    @Transactional
    public TaiSanChungCu themTaiSanChung(TaiSanChungCu taiSanMoi, String maHo) {
        // 1. Kiểm tra và thiết lập Hộ gia đình liên kết
        if (maHo != null && !maHo.trim().isEmpty()) {
            HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));

            taiSanMoi.setHoGiaDinh(hgd);
        } else {
            taiSanMoi.setHoGiaDinh(null);
        }   

        // 2. Loại tài sản phải được set từ Controller/Form (taiSanMoi đã có)
        if (taiSanMoi.getLoaiTaiSan() == null) {
            throw new IllegalArgumentException("Loại Tài Sản không được để trống.");
        }
    
        // 3. Lưu
        return taiSanChungCuDAO.save(taiSanMoi);
    }
    /**
     * Thêm một Căn hộ mới.
     * @param canHoMoi Entity TaiSanChungCu chứa thông tin căn hộ (TenTaiSan, DienTich, GiaTri, TrangThai, ViTri).
     * @param maHo Mã Hộ gia đình liên kết (có thể là null hoặc chuỗi rỗng).
     * @return TaiSanChungCu đã được lưu.
     */
    @Transactional
    public TaiSanChungCu themCanHo(TaiSanChungCu canHoMoi, String maHo) {
        // KIỂM TRA NGHIỆP VỤ: Tên căn hộ phải là duy nhất (NEW)
        if (taiSanChungCuDAO.existsByTenCanHo(canHoMoi.getTenTaiSan())) {
            throw new IllegalArgumentException("Lỗi: Tên Căn hộ '" + canHoMoi.getTenTaiSan() + "' đã tồn tại. Vui lòng chọn tên khác.");
        }
        // 1. Kiểm tra và thiết lập Hộ gia đình liên kết
        if (maHo != null && !maHo.trim().isEmpty()) {
            HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));
            
            // Logic nghiệp vụ: Cần đảm bảo hộ này chưa sở hữu căn hộ chính nào khác nếu cần. 
            // Ở đây, ta chỉ kiểm tra Hộ có tồn tại hay không.
            canHoMoi.setHoGiaDinh(hgd);
        } else {
            canHoMoi.setHoGiaDinh(null);
        }

        // 2. Thiết lập loại tài sản mặc định là CĂN_HỘ
        canHoMoi.setLoaiTaiSan(AssetType.can_ho); 
        
        // 3. Lưu
        return taiSanChungCuDAO.save(canHoMoi);
    }

    // =======================================================
    // 3. CHỨC NĂNG UPDATE (Cập nhật)
    // =======================================================
    /**
     * Cập nhật thông tin Tài Sản Chung Cư (áp dụng cho mọi loại tài sản).
     * @param maTaiSan Mã tài sản cần cập nhật.
     * @param taiSanCapNhat Entity TaiSanChungCu chứa dữ liệu mới.
     * @param maHo Mã Hộ gia đình mới liên kết (có thể là null để gỡ liên kết).
     * @return TaiSanChungCu đã được cập nhật.
     */
    @Autowired private ThongBaoService thongBaoService; // Inject thêm

    @Transactional
    public TaiSanChungCu capNhatTaiSanChung(Integer maTaiSan, TaiSanChungCu taiSanCapNhat, String maHo) {
        TaiSanChungCu taiSanHienTai = taiSanChungCuDAO.findByID(maTaiSan)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Tài Sản với Mã Tài Sản: " + maTaiSan));

        // Lưu trạng thái cũ để so sánh
        BlueMoon.bluemoon.utils.AssetStatus trangThaiCu = taiSanHienTai.getTrangThai();

        // 1. Cập nhật thông tin cơ bản
        taiSanHienTai.setTenTaiSan(taiSanCapNhat.getTenTaiSan());
        taiSanHienTai.setLoaiTaiSan(taiSanCapNhat.getLoaiTaiSan());
        taiSanHienTai.setTrangThai(taiSanCapNhat.getTrangThai()); // Trạng thái mới
        taiSanHienTai.setDienTich(taiSanCapNhat.getDienTich());
        taiSanHienTai.setGiaTri(taiSanCapNhat.getGiaTri());
        taiSanHienTai.setViTri(taiSanCapNhat.getViTri());
        taiSanHienTai.setMoTa(taiSanCapNhat.getMoTa());
    
        // 2. Cập nhật Hộ gia đình liên kết
        if (maHo != null && !maHo.trim().isEmpty()) {
            HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));
            taiSanHienTai.setHoGiaDinh(hgd);
        } else {
             taiSanHienTai.setHoGiaDinh(null); 
        }

        TaiSanChungCu savedAsset = taiSanChungCuDAO.save(taiSanHienTai);

        // === LOGIC THÔNG BÁO: Nếu trạng thái thay đổi -> Báo toàn bộ cư dân ===
        if (trangThaiCu != savedAsset.getTrangThai()) {
            String tieuDe = "Thông báo tài sản: " + savedAsset.getTenTaiSan() +" đã " + savedAsset.getTrangThai().getDbValue();
            String noiDung = "Tài sản '" + savedAsset.getTenTaiSan() + "' (" + savedAsset.getViTri() + ") " +
                             "đã chuyển trạng thái từ " + trangThaiCu.getDbValue() + 
                             " sang " + savedAsset.getTrangThai().getDbValue() + ".";
            
            // Gọi service gửi thông báo (chạy ngầm hoặc trực tiếp)
            try {
                thongBaoService.guiThongBaoHeThongDenTatCa(tieuDe, noiDung);
            } catch (Exception e) {
                System.err.println("Lỗi gửi thông báo tự động: " + e.getMessage());
                // Không throw exception để tránh rollback giao dịch cập nhật tài sản
            }
        }

        return savedAsset;
    }
    /**
     * Cập nhật thông tin Căn hộ.
     * @param maTaiSan Mã căn hộ cần cập nhật.
     * @param canHoCapNhat Entity TaiSanChungCu chứa dữ liệu mới.
     * @param maHo Mã Hộ gia đình mới liên kết (có thể là null để gỡ liên kết).
     * @return TaiSanChungCu đã được cập nhật.
     */
    @Transactional
    public TaiSanChungCu capNhatCanHo(Integer maTaiSan, TaiSanChungCu canHoCapNhat, String maHo) {
        TaiSanChungCu canHoHienTai = taiSanChungCuDAO.findByID(maTaiSan)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Căn hộ với Mã Tài Sản: " + maTaiSan));
                // KIỂM TRA NGHIỆP VỤ: Tên căn hộ phải là duy nhất (khi cập nhật) (NEW)
        if (taiSanChungCuDAO.existsByTenCanHoExceptId(canHoCapNhat.getTenTaiSan(), maTaiSan)) {
             throw new IllegalArgumentException("Lỗi: Tên Căn hộ '" + canHoCapNhat.getTenTaiSan() + "' đã được sử dụng cho căn hộ khác.");
        }
        // Đảm bảo không thay đổi loại tài sản
        if (canHoHienTai.getLoaiTaiSan() != AssetType.can_ho) {
             throw new IllegalStateException("Loại tài sản không phải là Căn hộ. Không thể cập nhật qua chức năng này.");
        }

        // 1. Cập nhật thông tin cơ bản
        canHoHienTai.setTenTaiSan(canHoCapNhat.getTenTaiSan());
        canHoHienTai.setTrangThai(canHoCapNhat.getTrangThai());
        canHoHienTai.setDienTich(canHoCapNhat.getDienTich());
        canHoHienTai.setGiaTri(canHoCapNhat.getGiaTri());
        canHoHienTai.setViTri(canHoCapNhat.getViTri());
        canHoHienTai.setMoTa(canHoCapNhat.getMoTa());
        // 2. Cập nhật Hộ gia đình liên kết
        if (maHo != null && !maHo.trim().isEmpty()) {
            HoGiaDinh hgd = hoGiaDinhDAO.findById(maHo)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hộ gia đình với Mã Hộ: " + maHo));
            canHoHienTai.setHoGiaDinh(hgd);
        } else {
             canHoHienTai.setHoGiaDinh(null); // Gỡ liên kết
        }

        return taiSanChungCuDAO.save(canHoHienTai);
    }

    // =======================================================
    // 4. CHỨC NĂNG DELETE (Xóa)
    // =======================================================
    /**
     * Xóa vật lý một Tài Sản Chung Cư khỏi hệ thống.
     * @param maTaiSan Mã tài sản cần xóa.
     */
    @Transactional
    public void xoaTaiSanChung(Integer maTaiSan) {
        TaiSanChungCu taiSan = taiSanChungCuDAO.findByID(maTaiSan)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Tài Sản với Mã Tài Sản: " + maTaiSan));
    
        // ... (Thêm logic kiểm tra ràng buộc nghiệp vụ trước khi xóa) ...
    
        taiSanChungCuDAO.delete(taiSan);
    }
    /**
     * Xóa vật lý một Căn hộ khỏi hệ thống.
     * @param maTaiSan Mã căn hộ cần xóa.
     */
    @Transactional
    public void xoaCanHo(Integer maTaiSan) {
        TaiSanChungCu canHo = taiSanChungCuDAO.findByID(maTaiSan)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Căn hộ với Mã Tài Sản: " + maTaiSan));
        
        // Thêm logic kiểm tra ràng buộc nghiệp vụ trước khi xóa (ví dụ: không được xóa nếu có hóa đơn hoặc sự cố đang mở liên quan)
        // ... (Logic kiểm tra) ...
        
        taiSanChungCuDAO.delete(canHo);
    }
    public Map<String, Object> getChartData() {
        // 1. Lấy dữ liệu thô từ DB
        List<Object[]> rawData = taiSanChungCuDAO.getRawResidentCounts();

        // Map lưu trữ kết quả cộng dồn
        // Dùng TreeMap để tự động sắp xếp (Tầng 1, 2... Tòa A, B...)
        Map<Integer, Long> floorStats = new java.util.TreeMap<>();
        Map<String, Long> buildingStats = new java.util.TreeMap<>();

        for (Object[] row : rawData) {
            String tenCanHo = (String) row[0]; // VD: "A-1001"
            Long soLuong = (Long) row[1];      // VD: 3 người

            if (tenCanHo == null || soLuong == 0) continue;

            // --- LOGIC XỬ LÝ CHUỖI ---
            try {
                // Giả định định dạng: [Tòa]-[Số] (VD: A-1001) hoặc [Tòa][Số] (VD: A1001)
                
                // 1. Tách Tòa: Lấy ký tự đầu tiên hoặc phần trước dấu "-"
                String toa = "";
                String phanSo = "";

                if (tenCanHo.contains("-")) {
                    String[] parts = tenCanHo.split("-");
                    toa = parts[0].trim(); // "A"
                    if (parts.length > 1) phanSo = parts[1].trim(); // "1001"
                } else {
                    // Trường hợp viết liền A1001 -> Tách chữ cái đầu
                    toa = tenCanHo.substring(0, 1);
                    phanSo = tenCanHo.substring(1);
                }

                // 2. Tính Tầng: Lấy phần số chia cho 100
                // VD: 1001 / 100 = 10 (Tầng 10)
                // VD: 502 / 100 = 5 (Tầng 5)
                if (!phanSo.isEmpty() && phanSo.matches("\\d+")) {
                    int soPhong = Integer.parseInt(phanSo);
                    int tang = soPhong / 100; 
                    
                    // Cộng dồn vào Map Tầng
                    floorStats.put(tang, floorStats.getOrDefault(tang, 0L) + soLuong);
                }

                // 3. Cộng dồn vào Map Tòa
                buildingStats.put(toa, buildingStats.getOrDefault(toa, 0L) + soLuong);

            } catch (NumberFormatException e) {
                System.err.println("Lỗi parse tên căn hộ: " + tenCanHo);
                // Có thể log lại hoặc bỏ qua căn hộ đặt tên sai quy tắc
            }
        }

        // Đóng gói kết quả trả về
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("floorStats", floorStats);
        result.put("buildingStats", buildingStats);
        return result;
    }
    // =======================================================
    // LOGIC BÁO CÁO THỐNG KÊ
    // =======================================================

    /**
     * Lấy dữ liệu cho các biểu đồ thống kê tài sản chung
     */
    public Map<String, Object> getGeneralAssetStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Thống kê theo Loại
        List<Object[]> typeRaw = taiSanChungCuDAO.countGeneralAssetsByType();
        List<String> typeLabels = new ArrayList<>();
        List<Long> typeData = new ArrayList<>();
        for (Object[] row : typeRaw) {
            BlueMoon.bluemoon.utils.AssetType type = (BlueMoon.bluemoon.utils.AssetType) row[0];
            typeLabels.add(type.getDbValue()); // Lấy tên hiển thị của Enum
            typeData.add((Long) row[1]);
        }
        stats.put("typeLabels", typeLabels);
        stats.put("typeData", typeData);

        // 2. Thống kê theo Trạng Thái
        List<Object[]> statusRaw = taiSanChungCuDAO.countGeneralAssetsByStatus();
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusData = new ArrayList<>();
        for (Object[] row : statusRaw) {
            BlueMoon.bluemoon.utils.AssetStatus status = (BlueMoon.bluemoon.utils.AssetStatus) row[0];
            statusLabels.add(status.getDbValue());
            statusData.add((Long) row[1]);
        }
        stats.put("statusLabels", statusLabels);
        stats.put("statusData", statusData);

        // 3. Thống kê theo Vị Trí
        List<Object[]> locationRaw = taiSanChungCuDAO.countGeneralAssetsByLocation();
        List<String> locationLabels = new ArrayList<>();
        List<Long> locationData = new ArrayList<>();
        for (Object[] row : locationRaw) {
            String loc = (String) row[0];
            locationLabels.add(loc);
            locationData.add((Long) row[1]);
        }
        stats.put("locationLabels", locationLabels);
        stats.put("locationData", locationData);

        return stats;
    }

    /**
     * Lấy danh sách chi tiết để hiển thị bảng
     */
    public List<TaiSanChungCu> getGeneralAssetListReport() {
        return taiSanChungCuDAO.findAllGeneralAssets();
    }
    /**
     * Lấy danh sách các TÒA NHÀ có căn hộ trống.
     * Logic: Quét toàn bộ căn trống, cắt chuỗi lấy ký tự đầu (A, B...).
     */
    public List<String> getAvailableBuildings() {
        List<TaiSanChungCu> emptyApts = taiSanChungCuDAO.findAllEmptyApartments();
        Set<String> buildings = new HashSet<>();

        for (TaiSanChungCu apt : emptyApts) {
            String name = apt.getTenTaiSan(); // VD: "A-101"
            if (name.contains("-")) {
                String building = name.split("-")[0]; // Lấy "A"
                buildings.add(building);
            }
        }
        return new ArrayList<>(buildings).stream().sorted().collect(Collectors.toList());
    }

    /**
     * Lấy danh sách TẦNG có căn hộ trống thuộc Tòa cụ thể.
     * Logic: A-101 -> Tầng 1, A-1205 -> Tầng 12.
     */
    @SuppressWarnings("UnnecessaryContinue")
    public List<Integer> getAvailableFloorsByBuilding(String building) {
        List<TaiSanChungCu> emptyApts = taiSanChungCuDAO.findAllEmptyApartments();
        Set<Integer> floors = new HashSet<>();

        for (TaiSanChungCu apt : emptyApts) {
            String name = apt.getTenTaiSan(); // VD: "A-101"
            if (name.startsWith(building + "-")) {
                try {
                    String numberPart = name.split("-")[1]; // "101"
                    int roomNum = Integer.parseInt(numberPart);
                    int floor = roomNum / 100; // 101/100 = 1
                    if (floor == 0) floor = 1; // Xử lý tầng trệt nếu cần
                    floors.add(floor);
                } catch (NumberFormatException e) { continue; }
            }
        }
        return new ArrayList<>(floors).stream().sorted().collect(Collectors.toList());
    }

    /**
     * Lấy danh sách CĂN HỘ trống thuộc Tòa và Tầng cụ thể.
     */
    @SuppressWarnings("UnnecessaryContinue")
    public List<TaiSanChungCu> getEmptyApartmentsByBuildingAndFloor(String building, Integer floor) {
        List<TaiSanChungCu> emptyApts = taiSanChungCuDAO.findAllEmptyApartments();
        List<TaiSanChungCu> result = new ArrayList<>();

        for (TaiSanChungCu apt : emptyApts) {
            String name = apt.getTenTaiSan(); // VD: "A-101"
            try {
                if (name.startsWith(building + "-")) {
                    String numberPart = name.split("-")[1];
                    int roomNum = Integer.parseInt(numberPart);
                    int calculatedFloor = roomNum / 100;
                    if (calculatedFloor == 0) calculatedFloor = 1;

                    if (calculatedFloor == floor) {
                        result.add(apt);
                    }
                }
            } catch (NumberFormatException e) { continue; }
        }
        return result;
    }
    
}