package BlueMoon.bluemoon.services;

import BlueMoon.bluemoon.daos.*;
import BlueMoon.bluemoon.entities.*;
import BlueMoon.bluemoon.utils.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    @Autowired private DoiTuongDAO doiTuongDAO;
    @Autowired private TaiSanChungCuDAO taiSanDAO;
    @Autowired private HoGiaDinhDAO hoGiaDinhDAO;
    @Autowired private ThanhVienHoDAO thanhVienHoDAO;
    @Autowired private DichVuDAO dichVuDAO;

    @PersistenceContext
    private EntityManager entityManager;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (doiTuongDAO.countResidents() > 0) {
            System.out.println(">>> Dữ liệu đã tồn tại. Bỏ qua bước seeding.");
            return;
        }

        System.out.println(">>> BẮT ĐẦU SEEDING DỮ LIỆU MỚI...");

        // Tối ưu: Mã hóa mật khẩu 1 lần dùng cho tất cả 1800 người
        String encodedPassword = encoder.encode("123");

        // 1. Tạo Nhân sự hệ thống
        DoiTuong admin = createStaff("000000000001", "Nguyễn Quản Trị", UserRole.ban_quan_tri, encodedPassword);
        createStaff("000000000002", "Lê Thị Kế Toán", UserRole.ke_toan, encodedPassword);
        createStaff("000000000003", "Trần Minh Công An", UserRole.co_quan_chuc_nang, encodedPassword);
        createStaff("000000000000", "Nguyễn Văn Cư Dân", UserRole.nguoi_dung_thuong, encodedPassword);

        // 2. Tạo danh mục dịch vụ
        seedDichVu(admin);

        // 3. Vòng lặp sinh 30 Tầng x 15 Căn hộ
        String[] hoArr = {"Nguyễn", "Trần", "Lê", "Phạm", "Vũ", "Hoàng", "Phan"};
        String[] demNam = {"Văn", "Minh", "Quốc", "Anh", "Đức", "Lâm"};
        String[] tenNam = {"Hải", "Tùng", "Sơn", "Thắng", "Bách", "Long", "Lâm", "Khánh"};
        String[] demNu = {"Thị", "Ngọc", "Thanh", "Mai", "Quỳnh"};
        String[] tenNu = {"Lan", "Hoa", "Trang", "Linh", "Hà", "An"};

        int residentSerial = 100000;

        for (int floor = 1; floor <= 30; floor++) {
            for (int room = 1; room <= 15; room++) {
                
                String prefix = (floor == 1) ? "K" : (floor == 30) ? "P" : "A";
                String roomName = String.format("%s-%d%02d", prefix, floor, room);
                String maHo = String.format("%09d", floor * 100 + room);

                // A. Khởi tạo Hộ gia đình trước
                HoGiaDinh hoGD = new HoGiaDinh();
                hoGD.setMaHo(maHo);
                hoGD.setTenHo("Hộ gia đình " + roomName);
                hoGD.setTrangThai(HouseholdStatus.hoat_dong);
                hoGD = hoGiaDinhDAO.save(hoGD); // Capture saved instance

                // B. Tạo 4 Cư dân và gán vào Hộ
                for (int i = 1; i <= 4; i++) {
                    String cccd = String.format("038%09d", residentSerial++);
                    boolean isNam = random.nextBoolean();
                    String hoVaTen = hoArr[random.nextInt(hoArr.length)] + " " + 
                                     (isNam ? demNam[random.nextInt(demNam.length)] + " " + tenNam[random.nextInt(tenNam.length)] 
                                            : demNu[random.nextInt(demNu.length)] + " " + tenNu[random.nextInt(tenNu.length)]);

                    DoiTuong res = new DoiTuong();
                    res.setCccd(cccd);
                    res.setHoVaTen(hoVaTen);
                    res.setMatKhau(encodedPassword);
                    res.setVaiTro(i == 1 ? UserRole.nguoi_dung_thuong : UserRole.khong_dung_he_thong);
                    res.setLaCuDan(true);
                    res.setGioiTinh(isNam ? Gender.nam : Gender.nu);
                    res.setNgaySinh(LocalDate.of(1970 + random.nextInt(45), random.nextInt(12) + 1, random.nextInt(28) + 1));
                    res.setTrangThaiTaiKhoan(i == 1 ? AccountStatus.hoat_dong : AccountStatus.chua_kich_hoat);
                    res.setTrangThaiDanCu(ResidentStatus.o_chung_cu);
                    res = doiTuongDAO.save(res); // Capture saved instance

                    ThanhVienHoID id = new ThanhVienHoID(cccd, LocalDate.now());
                    String quanHe = (i == 1) ? "Chủ hộ" : (i == 2 ? "Vợ/Chồng" : "Con");
                    ThanhVienHo tvh = new ThanhVienHo(id, res, hoGD, (i == 1), quanHe);
                    thanhVienHoDAO.save(tvh);
                }

                // C. Tạo Căn hộ và liên kết
                TaiSanChungCu apt = new TaiSanChungCu(roomName, AssetType.can_ho, 
                        new BigDecimal(60 + random.nextInt(90)), 
                        new BigDecimal(1500000000L + random.nextInt(2000000000)), 
                        "Tầng " + floor, null);
                apt.setHoGiaDinh(hoGD);
                apt.setTrangThai(AssetStatus.da_duoc_thue);
                taiSanDAO.save(apt);
            }
        }
        seedCommunalAssets();
        System.out.println(">>> SEEDING HOÀN TẤT THÀNH CÔNG: 450 HỘ, 1800 CƯ DÂN.");
    }

    private void resetDatabase() {
        // Lệnh SQL Native dành cho PostgreSQL để xóa sạch và reset ID
        String query = "TRUNCATE TABLE thanh_vien_ho, tai_san_chung_cu, thong_bao, " +
                       "dich_vu, ho_gia_dinh, doi_tuong RESTART IDENTITY CASCADE";
        entityManager.createNativeQuery(query).executeUpdate();
    }

    private DoiTuong createStaff(String cccd, String name, UserRole role, String pass) {
        DoiTuong staff = new DoiTuong();
        staff.setCccd(cccd);
        staff.setHoVaTen(name);
        staff.setMatKhau(pass);
        staff.setVaiTro(role);
        staff.setLaCuDan(true);
        staff.setTrangThaiTaiKhoan(AccountStatus.hoat_dong);
        staff.setTrangThaiDanCu(ResidentStatus.o_chung_cu);
        staff.setNgaySinh(LocalDate.of(1985, 1, 1));
        return doiTuongDAO.save(staff);
    }

    private void seedDichVu(DoiTuong admin) {
        List<DichVu> ds = List.of(
            new DichVu("Phí Quản lý", admin, "Vận hành chung", new BigDecimal("12000"), "m2", ServiceType.dinh_ky),
            new DichVu("Tiền Điện", admin, "Sinh hoạt", new BigDecimal("3500"), "kWh", ServiceType.dinh_ky),
            new DichVu("Tiền Nước", admin, "Nước sạch", new BigDecimal("15000"), "m3", ServiceType.dinh_ky),
            new DichVu("Gửi xe máy", admin, "Hầm B1", new BigDecimal("120000"), "Chiếc", ServiceType.dinh_ky),
            new DichVu("Gửi ô tô", admin, "Hầm B2", new BigDecimal("1200000"), "Chiếc", ServiceType.dinh_ky),
            new DichVu("Vệ sinh", admin, "Phí rác thải", new BigDecimal("50000"), "Hộ", ServiceType.dinh_ky),
            new DichVu("Internet", admin, "Gói 100Mbps", new BigDecimal("250000"), "Tháng", ServiceType.dinh_ky),
            new DichVu("Gym & Pool", admin, "Tiện ích tầng thượng", new BigDecimal("300000"), "Người", ServiceType.dinh_ky),
            new DichVu("Dọn hộ theo giờ", admin, "Dọn dẹp nội thất", new BigDecimal("200000"), "Lần", ServiceType.theo_yeu_cau),
            new DichVu("Sửa điện nước", admin, "Kỹ thuật hỗ trợ", new BigDecimal("150000"), "Lần", ServiceType.theo_yeu_cau)
        );
        ds.forEach(dv -> {
            dv.setTrangThai(AssetStatus.hoat_dong);
            dichVuDAO.save(dv);
        });
    }
    private void seedCommunalAssets() {
    System.out.println(">>> Đang seeding tài sản khu vực công cộng...");
    // 2. Tài sản trọng yếu của tòa nhà (Đặt tại các vị trí đặc thù)
    List<TaiSanChungCu> specialAssets = List.of(
        new TaiSanChungCu("Hệ thống PCCC Tổng", AssetType.thiet_bi, new BigDecimal("200"), new BigDecimal("2000000000"), "Phòng kỹ thuật Tầng 1", null),
        new TaiSanChungCu("Máy phát điện dự phòng", AssetType.thiet_bi, new BigDecimal("50"), new BigDecimal("1500000000"), "Khu vực kỹ thuật Hầm B1", null),
        new TaiSanChungCu("Trạm biến áp nội bộ", AssetType.thiet_bi, new BigDecimal("40"), new BigDecimal("1200000000"), "Sân sau tòa nhà", null),
        new TaiSanChungCu("Hệ thống Camera an ninh", AssetType.thiet_bi, new BigDecimal("100"), new BigDecimal("800000000"), "Toàn bộ tòa nhà", null),
        new TaiSanChungCu("Bể bơi vô cực", AssetType.tien_ich, new BigDecimal("500"), new BigDecimal("5000000000"), "Tầng thượng (31)", null),
        new TaiSanChungCu("Vườn cảnh quan sảnh chính", AssetType.tien_ich, new BigDecimal("300"), new BigDecimal("300000000"), "Khu vực sảnh A", null)
    );

    specialAssets.forEach(ts -> {
        ts.setTrangThai(AssetStatus.hoat_dong);
        taiSanDAO.save(ts);
    });
}
}