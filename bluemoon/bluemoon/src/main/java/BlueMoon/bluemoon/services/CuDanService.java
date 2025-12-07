package BlueMoon.bluemoon.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.utils.AccountStatus;
import BlueMoon.bluemoon.utils.ResidentStatus;
import BlueMoon.bluemoon.utils.UserRole;

@Service
public class CuDanService {

    @Autowired
    private DoiTuongDAO doiTuongDAO;

    /**
     * Thêm cư dân mới
     */
    @Transactional
    public DoiTuong themCuDan(DoiTuong cuDan) {
        try {
            // Validate required fields
            if (cuDan.getCccd() == null || cuDan.getCccd().trim().isEmpty()) {
                throw new IllegalArgumentException("CCCD không được để trống");
            }
        
            // Check if CCCD already exists
            if (doiTuongDAO.findByCccd(cuDan.getCccd()).isPresent()) {
                cuDan.setTrangThaiDanCu(ResidentStatus.o_chung_cu);
                cuDan.setLaCuDan(true);
                cuDan.setVaiTro(UserRole.khong_dung_he_thong);
                cuDan.setTrangThaiTaiKhoan(AccountStatus.chua_kich_hoat);
                cuDan.setMatKhau(BCryptPasswordEncoder("123")); // Chưa có mật khẩu
                if (doiTuongDAO.timNguoiDungThuongTheoCCCD(cuDan.getCccd()).isPresent()) {
                    DoiTuong existingUser = doiTuongDAO.timNguoiDungThuongTheoCCCD(cuDan.getCccd()).get();
                    cuDan.setVaiTro(UserRole.nguoi_dung_thuong);
                    cuDan.setTrangThaiTaiKhoan(AccountStatus.hoat_dong);
                    cuDan.setMatKhau(existingUser.getMatKhau());
                    return doiTuongDAO.save(cuDan);
                }
                return doiTuongDAO.save(cuDan);
            }

            // Set default values
            cuDan.setLaCuDan(true);
            cuDan.setVaiTro(UserRole.khong_dung_he_thong);
            cuDan.setTrangThaiDanCu(ResidentStatus.o_chung_cu);
            cuDan.setTrangThaiTaiKhoan(AccountStatus.chua_kich_hoat);
            cuDan.setMatKhau(BCryptPasswordEncoder("123")); // Chưa có mật khẩu

            return doiTuongDAO.save(cuDan);
        
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Lỗi khi lưu dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin cư dân
     */
    @Transactional
    public DoiTuong capNhatCuDan(String cccd, DoiTuong cuDanCapNhat) {
        Optional<DoiTuong> cuDanHienTai = doiTuongDAO.findByCccd(cccd);
        
        if (cuDanHienTai.isPresent()) {
            DoiTuong cuDan = cuDanHienTai.get();
            
            // Chỉ cập nhật các thông tin được phép thay đổi
            cuDan.setHoVaTen(cuDanCapNhat.getHoVaTen());
            cuDan.setNgaySinh(cuDanCapNhat.getNgaySinh());
            cuDan.setGioiTinh(cuDanCapNhat.getGioiTinh());
            cuDan.setQueQuan(cuDanCapNhat.getQueQuan());
            cuDan.setSoDienThoai(cuDanCapNhat.getSoDienThoai());
            cuDan.setEmail(cuDanCapNhat.getEmail());
            cuDan.setNgheNghiep(cuDanCapNhat.getNgheNghiep());
            
            return doiTuongDAO.save(cuDan);
        }
        
        throw new RuntimeException("Không tìm thấy cư dân với CCCD: " + cccd);
    }

    /**
     * Xóa cư dân (thực tế là đánh dấu đã chuyển đi)
     */

    /**
     * Đánh dấu cư dân đã chuyển đi/mất (Thao tác xóa mềm)
     */
@Transactional
public void xoaCuDan(String cccd, ResidentStatus lyDo) {
    System.out.println("=== BẮT ĐẦU XÓA CƯ DÂN ===");
    System.out.println("CCCD: " + cccd);
    System.out.println("Lý do: " + lyDo);
    
    Optional<DoiTuong> cuDanOptional = doiTuongDAO.findResidentByCccd(cccd);
    
    System.out.println("Tìm thấy cư dân: " + cuDanOptional.isPresent());
    
    if (cuDanOptional.isPresent()) {
        DoiTuong cuDan = cuDanOptional.get();
        
        System.out.println("Trước khi update:");
        System.out.println("- Trạng thái dân cư: " + cuDan.getTrangThaiDanCu());
        System.out.println("- Là cư dân: " + cuDan.getLaCuDan());
        System.out.println("- Trạng thái tài khoản: " + cuDan.getTrangThaiTaiKhoan());
        
        // 1. Cập nhật trạng thái dân cư theo lý do (roi_di hoặc da_chet)
        cuDan.setTrangThaiDanCu(lyDo); 

        // 2. Đánh dấu không phải cư dân hiện tại
        cuDan.setLaCuDan(false);
        
        // 3. Tùy chọn: Đánh dấu tài khoản không hoạt động (nếu có)
        cuDan.setTrangThaiTaiKhoan(AccountStatus.tam_ngung); 
        
        System.out.println("Sau khi set:");
        System.out.println("- Trạng thái dân cư: " + cuDan.getTrangThaiDanCu());
        System.out.println("- Là cư dân: " + cuDan.getLaCuDan());
        System.out.println("- Trạng thái tài khoản: " + cuDan.getTrangThaiTaiKhoan());
        
        DoiTuong saved = doiTuongDAO.save(cuDan);
        
        System.out.println("Sau khi save:");
        System.out.println("- Trạng thái dân cư: " + saved.getTrangThaiDanCu());
        System.out.println("- Là cư dân: " + saved.getLaCuDan());
        System.out.println("- Trạng thái tài khoản: " + saved.getTrangThaiTaiKhoan());
        
        System.out.println("=== HOÀN THÀNH XÓA CƯ DÂN ===");
    } else {
        System.out.println("KHÔNG TÌM THẤY CƯ DÂN!");
        throw new RuntimeException("Không tìm thấy cư dân với CCCD: " + cccd);
    }
}
    /**
     * Lấy danh sách tất cả cư dân đang cư trú
     */
    public List<DoiTuong> layDanhSachCuDan() {
        return doiTuongDAO.findResidentsInComplex(ResidentStatus.o_chung_cu);
    }

    /**
     * Tìm cư dân theo CCCD
     */
    public Optional<DoiTuong> timCuDanTheoCCCD(String cccd) {
        return doiTuongDAO.findResidentByCccd(cccd);
    }
    /**
     * Đăng ký tài khoản mới cho cư dân
     */
    public DoiTuong dangKyTaiKhoan(DoiTuong cuDan) {
        // Kiểm tra có phải cư dan trong chung cư không
        if (!doiTuongDAO.findResidentByCccd(cuDan.getCccd()).isPresent()) {
            throw new IllegalArgumentException("Người dùng không phải cư dân trong chung cư");
        }

        // Kiểm tra CCCD đã được đăng ký tài khoản chưa
        if (doiTuongDAO.timNguoiDungThuongTheoCCCD(cuDan.getCccd()).isPresent()) {
            throw new IllegalArgumentException("CCCD đã được đăng ký tài khoản");
        }

        // Mã hóa mật khẩu
        String hashedPassword = BCryptPasswordEncoder((cuDan.getMatKhau()));
        cuDan.setMatKhau(hashedPassword);

        // Set các giá trị mặc định
        cuDan.setVaiTro(UserRole.nguoi_dung_thuong);
        cuDan.setLaCuDan(true);
        cuDan.setTrangThaiTaiKhoan(AccountStatus.hoat_dong);
        cuDan.setTrangThaiDanCu(ResidentStatus.o_chung_cu);

        return doiTuongDAO.save(cuDan);
    }
    private String BCryptPasswordEncoder(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }
    public List<DoiTuong> layTatCaNguoiDung() {
        return doiTuongDAO.findAll();
    }
    public List<DoiTuong> timKiemvaLoc(String keyword, ResidentStatus trangThaiDanCu) {
        if (trangThaiDanCu != null) {
            return doiTuongDAO.findResidentsInComplex(trangThaiDanCu);
        }
        return doiTuongDAO.searchResidents(keyword);
    }
    public List<DoiTuong> timKiemvaLoc(String keyword, ResidentStatus trangThaiDanCu, AccountStatus accountStatus) {
        if ((keyword != null && !keyword.trim().isEmpty()) || trangThaiDanCu != null|| accountStatus != null) {
            return doiTuongDAO.searchResidentsAndFilter(keyword, trangThaiDanCu, accountStatus);
        }
        return doiTuongDAO.findResidentsInComplex(ResidentStatus.o_chung_cu);
    }
    /**
     * Lấy dữ liệu thống kê cư dân cho biểu đồ
     */
    /**
     * Lấy dữ liệu thống kê cư dân (Đã Việt hóa nhãn hiển thị)
     */
    public Map<String, Object> getResidentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Thống kê theo Giới tính (Việt hóa)
        List<Object[]> genderRaw = doiTuongDAO.countResidentsByGender();
        List<String> genderLabels = new ArrayList<>();
        List<Long> genderData = new ArrayList<>();
        
        for (Object[] row : genderRaw) {
            BlueMoon.bluemoon.utils.Gender g = (BlueMoon.bluemoon.utils.Gender) row[0];
            String label = "Nữ";
            
            if (g != null) {
                // Kiểm tra theo Enum Name hoặc giá trị
                if (g.name().equalsIgnoreCase("nam")) label = "Nam";
            }
            
            genderLabels.add(label);
            genderData.add((Long) row[1]);
        }
        stats.put("genderLabels", genderLabels);
        stats.put("genderData", genderData);

        // 2. Thống kê theo Trạng thái cư trú (Việt hóa)
        List<Object[]> statusRaw = doiTuongDAO.countResidentsByStatus();
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusData = new ArrayList<>();
        
        for (Object[] row : statusRaw) {
            BlueMoon.bluemoon.utils.ResidentStatus s = (BlueMoon.bluemoon.utils.ResidentStatus) row[0];
            String label = "Đã rời đi";

            if (s != null) {
                switch (s) {
                    case o_chung_cu -> label = "Đang sinh sống";
                    case roi_di -> label = "Đã rời đi";
                    case da_chet -> label = "Đã mất";
                }
            }
            
            statusLabels.add(label);
            statusData.add((Long) row[1]);
        }
        stats.put("resStatusLabels", statusLabels);
        stats.put("resStatusData", statusData);
        
        // 3. Thống kê theo Độ tuổi
        List<DoiTuong> allResidents = doiTuongDAO.findResidentsInComplex(ResidentStatus.o_chung_cu);
        long treEmMamNon = 0; // 0-5 tuổi
        long hocSinh = 0; // 6-17 tuổi
        long sinhVien = 0; // 18-22 tuổi
        long nguoiDiLam = 0; // 23-59 tuổi
        long nguoiGia = 0; // 60+ tuổi

        int currentYear = java.time.Year.now().getValue();

        for (DoiTuong resident : allResidents) {
            if (resident.getNgaySinh() != null) {
                int age = currentYear - resident.getNgaySinh().getYear();

                if (age <= 5) {
                    treEmMamNon++;
                } else if (age <= 17) {
                    hocSinh++;
                } else if (age <= 22) {
                    sinhVien++;
                } else if (age <= 59) {
                    nguoiDiLam++;
                } else {
                    nguoiGia++;
                }
            }
        }

        List<String> ageLabels = List.of("Trẻ em mầm non (0-5)", "Học sinh (6-17)",
                "Sinh viên (18-22)", "Người đi làm (23-59)",
                "Người già (60+)");
        List<Long> ageData = List.of(treEmMamNon, hocSinh, sinhVien, nguoiDiLam, nguoiGia);

        stats.put("ageLabels", ageLabels);
        stats.put("ageData", ageData);

        return stats;
    }
    
}