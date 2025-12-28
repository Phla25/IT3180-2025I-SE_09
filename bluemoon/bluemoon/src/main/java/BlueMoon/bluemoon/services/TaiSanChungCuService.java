package BlueMoon.bluemoon.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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
    
        if (taiSanMoi.getLoaiTaiSan() == AssetType.can_ho){
            if (taiSanChungCuDAO.existsByTenCanHo(taiSanMoi.getTenTaiSan())) {
            throw new IllegalArgumentException("Lỗi: Tên Căn hộ '" + taiSanMoi.getTenTaiSan() + "' đã tồn tại. Vui lòng chọn tên khác.");
        }
        }
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
        validateTenCanHo(canHoMoi.getTenTaiSan());
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
    // =======================================================
    // [MỚI] HÀM KIỂM TRA LOGIC TÊN CĂN HỘ & SỐ TẦNG
    // =======================================================
    private void validateTenCanHo(String tenCanHo) {
        // 1. Kiểm tra định dạng cơ bản (VD: A-101, P-3001)
        if (tenCanHo == null || !tenCanHo.contains("-")) {
            throw new IllegalArgumentException("Tên căn hộ không hợp lệ. Định dạng chuẩn: [Ký tự]-[Số Phòng] (VD: A-1005)");
        }

        String[] parts = tenCanHo.split("-");
        String prefix = parts[0].toUpperCase(); // Chữ cái đầu (A, K, P...)
        String numberPart = parts[1];

        // 2. Tính toán số tầng từ số phòng
        int floor;
        try {
            int roomNum = Integer.parseInt(numberPart);
            floor = roomNum / 100; // VD: 1005 / 100 = 10
            if (floor == 0) floor = 1; // VD: 001 -> Tầng 1
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số phòng phải là ký tự số.");
        }

        // 3. Kiểm tra giới hạn 30 tầng
        if (floor > 30) {
            throw new IllegalArgumentException("Lỗi: Chung cư BlueMoon chỉ có tối đa 30 tầng. Bạn đang nhập tầng " + floor);
        }

        // 4. Kiểm tra quy tắc đặt tên (Prefix) theo tầng
        switch (prefix) {
            case "K" -> {
                // Kiot
                if (floor != 1)
                    throw new IllegalArgumentException("Tiền tố 'K' (Kiot) chỉ dành cho Tầng 1.");
            }
            
            case "D" -> {
                // Tầng Đế (Thương mại/Văn phòng) - Tầng 2 đến 5
                if (floor < 2 || floor > 5)
                    throw new IllegalArgumentException("Tiền tố 'D' (Tầng Đế) chỉ dành cho Tầng 2 đến Tầng 5.");
            }

            case "A" -> {
                // Căn hộ nhà ở - Tầng 2 đến 29 (Gộp cả đế nếu muốn, hoặc chỉ 6-29)
                // Đề bài: 4 tầng đế + 24 tầng nhà ở = Tầng 2 -> 29
                if (floor < 2 || floor > 29) 
                    throw new IllegalArgumentException("Tiền tố 'A' (Căn hộ) dành cho các tầng từ 2 đến 29.");
            }

            case "P" -> {
                // Penthouse
                if (floor != 30)
                    throw new IllegalArgumentException("Tiền tố 'P' (Penthouse) chỉ dành cho Tầng 30.");
            }

            default -> throw new IllegalArgumentException("Tiền tố không hợp lệ. Vui lòng dùng: K (Kiot), D (Đế), A (Căn hộ), P (Penthouse).");
        }
    }
    @Transactional
    public TaiSanChungCu capNhatTaiSanChung(Integer maTaiSan, TaiSanChungCu taiSanCapNhat, String maHo) {
        TaiSanChungCu taiSanHienTai = taiSanChungCuDAO.findByID(maTaiSan)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Tài Sản với Mã Tài Sản: " + maTaiSan));
        if (taiSanCapNhat.getLoaiTaiSan() == AssetType.can_ho){
            if (taiSanChungCuDAO.existsByTenCanHo(taiSanCapNhat.getTenTaiSan())) {
            throw new IllegalArgumentException("Lỗi: Tên Căn hộ '" + taiSanCapNhat.getTenTaiSan() + "' đã tồn tại. Vui lòng chọn tên khác.");
        }
    }
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
        validateTenCanHo(canHoCapNhat.getTenTaiSan());
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
        Map<String, Object> result = new HashMap<>();
        
        // 1. Lấy dữ liệu thô từ Database
        List<Object[]> rawData = taiSanChungCuDAO.getRawResidentCounts(); 
        
        // Dùng TreeMap để tự động sắp xếp key (Tầng 1 -> 30)
        Map<Integer, Long> floorStats = new TreeMap<>();
        Map<String, Long> sectionStats = new TreeMap<>(); // Thống kê theo phân khu

        // KHỞI TẠO DỮ LIỆU MẶC ĐỊNH
        // 1. Khởi tạo đủ 30 tầng (để biểu đồ luôn hiện đủ trục X)
        for (int i = 1; i <= 30; i++) {
            floorStats.put(i, 0L);
        }

        // 2. Khởi tạo 4 phân khu chức năng
        sectionStats.put("Kiot (T1)", 0L);
        sectionStats.put("Tầng Đế (T2-T5)", 0L);
        sectionStats.put("Căn Hộ (T6-T29)", 0L);
        sectionStats.put("Penthouse (T30)", 0L);

        // XỬ LÝ DỮ LIỆU
        for (Object[] row : rawData) {
            try {
                String aptName = (String) row[0]; // VD: "A-0601"
                Long count = (Long) row[1];

                if (aptName == null || !aptName.contains("-")) continue;

                // Tách lấy số phòng (VD: 0601 -> Tầng 6)
                String numberPart = aptName.split("-")[1]; 
                int roomNum = Integer.parseInt(numberPart);
                int floor = roomNum / 100;    
                if (floor == 0) floor = 1; 

                // Nếu tầng nằm ngoài phạm vi 30 tầng -> Bỏ qua
                if (floor > 30) continue;

                // Cộng dồn vào Tầng
                floorStats.put(floor, floorStats.get(floor) + count);

                // Cộng dồn vào Phân Khu
                if (floor == 1) {
                    sectionStats.put("Kiot (T1)", sectionStats.get("Kiot (T1)") + count);
                } else if (floor >= 2 && floor <= 5) {
                    sectionStats.put("Tầng Đế (T2-T5)", sectionStats.get("Tầng Đế (T2-T5)") + count);
                } else if (floor >= 6 && floor <= 29) {
                    sectionStats.put("Căn Hộ (T6-T29)", sectionStats.get("Căn Hộ (T6-T29)") + count);
                } else if (floor == 30) {
                    sectionStats.put("Penthouse (T30)", sectionStats.get("Penthouse (T30)") + count);
                }

            } catch (NumberFormatException e) {}
        }
        
        result.put("floorStats", floorStats);
        result.put("buildingStats", sectionStats); // Giữ key là "buildingStats" để không phải sửa nhiều ở Controller
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