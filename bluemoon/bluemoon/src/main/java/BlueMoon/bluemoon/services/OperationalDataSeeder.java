package BlueMoon.bluemoon.services;

import BlueMoon.bluemoon.daos.*;
import BlueMoon.bluemoon.entities.*;
import BlueMoon.bluemoon.utils.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(2)
public class OperationalDataSeeder implements CommandLineRunner {

    @Autowired private HoGiaDinhDAO hoGiaDinhDAO;
    @Autowired private DoiTuongDAO doiTuongDAO;
    @Autowired private DichVuDAO dichVuDAO;
    @Autowired private HoaDonDAO hoaDonDAO;
    @Autowired private LichSuRaVaoDAO lichSuRaVaoDAO;
    @Autowired private DangKyDichVuDAO dangKyDichVuDAO;
    @Autowired private BaoCaoSuCoDAO baoCaoSuCoDAO;
    @Autowired private ThongBaoDAO thongBaoDAO;
    @Autowired private ThongBaoDaDocDAO thongBaoDaDocDAO;
    @Autowired private PhanHoiThongBaoDAO phanHoiThongBaoDAO; // Phản hồi thông báo

    @PersistenceContext
    private EntityManager entityManager;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> [OperationalSeeder] Đang dọn dẹp và reset toàn bộ Sequence về 1...");
        resetOperationalTables();

        // Chuẩn bị dữ liệu nền tảng
        List<DoiTuong> allUsers = doiTuongDAO.findAll();
        List<DoiTuong> dsCuDan = allUsers.stream().filter(DoiTuong::getLaCuDan).collect(Collectors.toList());
        DoiTuong admin = allUsers.stream().filter(u -> u.getVaiTro() == UserRole.ban_quan_tri).findFirst().get();
        List<HoGiaDinh> dsHo = hoGiaDinhDAO.findAll();
        List<DichVu> dsDichVu = dichVuDAO.findAll();

        // 2. Thực hiện Seed đa luồng dữ liệu
        seedNotificationsReadAndFeedback(admin, dsCuDan); 
        seedInvoices(dsHo);
        seedServiceRegistrations(dsCuDan, dsDichVu, admin);
        seedEntryExitLogs(dsCuDan);
        seedIncidentReportsAndFeedback(dsCuDan); 

        System.out.println(">>> [OperationalSeeder] HOÀN TẤT SEEDING TOÀN BỘ HỆ THỐNG!");
    }

    private void resetOperationalTables() {
        String query = "TRUNCATE TABLE phan_hoi_thong_bao, phan_hoi, thong_bao_da_doc, " +
                       "anh_bao_cao, lich_su_ra_vao, hoa_don, dang_ky_dich_vu, " +
                       "bao_cao_su_co, thong_bao RESTART IDENTITY CASCADE";
        entityManager.createNativeQuery(query).executeUpdate();
    }

    private void seedIncidentReportsAndFeedback(List<DoiTuong> residents) {
        String[] titles = {"Rò nước", "Hỏng bóng đèn", "Thang máy kêu"};
        for (int i = 0; i < 20; i++) {
            DoiTuong reporter = residents.get(random.nextInt(residents.size()));
            BaoCaoSuCo sc = new BaoCaoSuCo();
            sc.setNguoiBaoCao(reporter);
            sc.setTieuDe(titles[random.nextInt(titles.length)] + " - Tầng " + (random.nextInt(30) + 1));
            
            // SỬA LỖI TẠI ĐÂY: Đảm bảo set đúng thuộc tính ánh xạ tới cột mo_ta_su_co
            // Nếu Entity của bạn là setMoTaSuCo, hãy dùng nó. Nếu là setMoTa, hãy sửa lại.
            sc.setMoTaSuCo("Cư dân báo cáo chi tiết sự cố kỹ thuật cần được xử lý."); 
            
            sc.setTrangThai(random.nextBoolean() ? IncidentStatus.da_hoan_thanh : IncidentStatus.dang_xu_ly);
            sc.setThoiGianBaoCao(LocalDateTime.now().minusDays(random.nextInt(5)));
            baoCaoSuCoDAO.save(sc);
        }
    }

    private void seedNotificationsReadAndFeedback(DoiTuong admin, List<DoiTuong> residents) {
        String[] titles = {"Lịch cắt điện", "Phun thuốc muỗi", "Họp cư dân"};
        for (int i = 0; i < titles.length; i++) {
            ThongBao tb = new ThongBao();
            tb.setNguoiGui(admin);
            tb.setTieuDe(titles[i]);
            tb.setNoiDung("Thông báo chi tiết về: " + titles[i]);
            tb.setLoaiThongBao(NotificationType.binh_thuong);
            tb.setDoiTuongNhan(RecipientType.tat_ca);
            tb.setThoiGianGui(LocalDateTime.now().minusDays(i + 1));
            tb = thongBaoDAO.save(tb);

            // A. Seed THÔNG BÁO ĐÃ ĐỌC
            Set<String> readerIds = new HashSet<>();
            int targetReaders = Math.min(50, residents.size());
            while (readerIds.size() < targetReaders) {
                DoiTuong viewer = residents.get(random.nextInt(residents.size()));
                if (readerIds.add(viewer.getCccd())) {
                    thongBaoDaDocDAO.save(new ThongBaoDaDoc(tb, viewer));
                }
            }

            // B. Seed PHẢN HỒI THÔNG BÁO (Bảng riêng biệt: phan_hoi_thong_bao)
            for (int j = 0; j < 5; j++) {
                DoiTuong responder = residents.get(random.nextInt(residents.size()));
                PhanHoiThongBao phtb = new PhanHoiThongBao();
                phtb.setThongBao(tb);
                phtb.setNguoiGui(responder);
                phtb.setNoiDung("Ý kiến cư dân về thông báo.");
                phanHoiThongBaoDAO.save(phtb);
            }
        }
    }

    private void seedInvoices(List<HoGiaDinh> households) {
        String[] months = {"11/2025", "12/2025"};
        for (HoGiaDinh ho : households) {
            for (String m : months) {
                HoaDon hd = new HoaDon();
                hd.setHoGiaDinh(ho);
                hd.setSoTien(new BigDecimal(300000 + random.nextInt(500000)));
                hd.setLoaiHoaDon(InvoiceType.dich_vu);
                hd.setGhiChu("DIEN " + m);
                InvoiceStatus status = random.nextInt(10) > 3 ? InvoiceStatus.da_thanh_toan : InvoiceStatus.chua_thanh_toan;
                hd.setTrangThai(status);
                if (status == InvoiceStatus.da_thanh_toan) {
                    hd.setNgayThanhToan(LocalDateTime.now().minusDays(random.nextInt(10)));
                }
                hd.setNguoiDangKyDichVu(ho.getChuHo());
                hd.setHanThanhToan(LocalDate.now().plusDays(10));
                hoaDonDAO.save(hd);
            }
        }
    }

    private void seedServiceRegistrations(List<DoiTuong> residents, List<DichVu> services, DoiTuong admin) {
        for (int i = 0; i < 100; i++) {
            DangKyDichVu reg = new DangKyDichVu();
            reg.setNguoiDung(residents.get(random.nextInt(residents.size())));
            reg.setDichVu(services.get(random.nextInt(services.size())));
            reg.setTrangThai(random.nextBoolean() ? RegistrationStatus.da_duyet : RegistrationStatus.cho_duyet);
            if (reg.getTrangThai() == RegistrationStatus.da_duyet) {
                reg.setNguoiDuyet(admin);
                reg.setNgayDuyet(LocalDateTime.now());
            }
            dangKyDichVuDAO.save(reg);
        }
    }

    private void seedEntryExitLogs(List<DoiTuong> residents) {
        String[] gates = {"Cổng Chính", "Hầm B1", "Hầm B2"};
        for (int i = 0; i < 1000; i++) {
            LichSuRaVao log = new LichSuRaVao();
            log.setCuDan(residents.get(random.nextInt(residents.size())));
            log.setLoaiHoatDong(random.nextBoolean() ? EntryExitType.VAO : EntryExitType.RA);
            log.setCongKiemSoat(gates[random.nextInt(gates.length)]);
            log.setThoiGian(LocalDateTime.now().minusMinutes(random.nextInt(10000)));
            lichSuRaVaoDAO.save(log);
        }
    }
}