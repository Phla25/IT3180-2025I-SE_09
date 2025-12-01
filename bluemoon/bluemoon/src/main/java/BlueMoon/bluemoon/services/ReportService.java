package BlueMoon.bluemoon.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import BlueMoon.bluemoon.daos.DoiTuongDAO;
import BlueMoon.bluemoon.daos.HoGiaDinhDAO;
import BlueMoon.bluemoon.daos.HoaDonDAO;
import BlueMoon.bluemoon.daos.TaiSanChungCuDAO;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.HoGiaDinh;
import BlueMoon.bluemoon.entities.HoaDon;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.entities.ThanhVienHo;
import BlueMoon.bluemoon.models.ApartmentReportDTO;
import BlueMoon.bluemoon.models.HouseholdReportDTO;
import BlueMoon.bluemoon.models.InvoiceReportDTO;
import BlueMoon.bluemoon.models.ResidentReportDTO;
import BlueMoon.bluemoon.utils.AssetType;
import BlueMoon.bluemoon.utils.UserRole;

/**
 * Service để truy xuất và tổng hợp dữ liệu báo cáo theo role
 */
@Service
public class ReportService {
    
    @Autowired
    private TaiSanChungCuDAO taiSanChungCuDAO;
    
    @Autowired
    private HoaDonDAO hoaDonDAO;
    
    @Autowired
    private HoGiaDinhDAO hoGiaDinhDAO;
    
    @Autowired
    private DoiTuongDAO doiTuongDAO;
    
    /**
     * Lấy báo cáo căn hộ cho ADMIN - Xem tất cả căn hộ
     */
    public List<ApartmentReportDTO> getApartmentReportForAdmin() {
        List<TaiSanChungCu> apartments = taiSanChungCuDAO.findAllApartments();
        return convertToApartmentReportDTO(apartments);
    }
    
    /**
     * Lấy báo cáo TẤT CẢ tài sản cho ADMIN - Bao gồm cả căn hộ và tài sản chung
     */
    public List<ApartmentReportDTO> getAllAssetsReportForAdmin() {
        List<TaiSanChungCu> allAssets = taiSanChungCuDAO.findAll();
        return convertToApartmentReportDTO(allAssets);
    }
    
    /**
     * Lấy báo cáo căn hộ cho OFFICER - Xem tất cả căn hộ
     */
    public List<ApartmentReportDTO> getApartmentReportForOfficer() {
        List<TaiSanChungCu> apartments = taiSanChungCuDAO.findAllApartments();
        return convertToApartmentReportDTO(apartments);
    }
    
    /**
     * Lấy báo cáo căn hộ cho RESIDENT - Chỉ xem căn hộ của hộ mình
     */
    public List<ApartmentReportDTO> getApartmentReportForResident(String cccd) {
        // Tìm hộ gia đình của cư dân
        List<HoGiaDinh> allHouseholds = hoGiaDinhDAO.findAll();
        List<TaiSanChungCu> residentApartments = new ArrayList<>();
        
        for (HoGiaDinh household : allHouseholds) {
            DoiTuong chuHo = household.getChuHo();
            if (chuHo != null && chuHo.getCccd().equals(cccd)) {
                // Lấy các căn hộ của hộ này
                List<TaiSanChungCu> apartments = taiSanChungCuDAO.findApartmentsByHousehold(household);
                residentApartments.addAll(apartments);
            }
        }
        
        return convertToApartmentReportDTO(residentApartments);
    }
    
    /**
     * Lấy báo cáo tài sản chung cho ADMIN - Xem tất cả tài sản
     */
    public List<ApartmentReportDTO> getAssetReportForAdmin(AssetType assetType) {
        List<TaiSanChungCu> assets = taiSanChungCuDAO.findAllAssets(assetType);
        return convertToApartmentReportDTO(assets);
    }
    
    /**
     * Lấy báo cáo tài sản chung cho OFFICER
     */
    public List<ApartmentReportDTO> getAssetReportForOfficer(AssetType assetType) {
        List<TaiSanChungCu> assets = taiSanChungCuDAO.findAllAssets(assetType);
        return convertToApartmentReportDTO(assets);
    }
    
    /**
     * Lấy báo cáo hóa đơn cho ADMIN - Xem tất cả hóa đơn
     */
    public List<InvoiceReportDTO> getInvoiceReportForAdmin() {
        List<HoaDon> invoices = hoaDonDAO.findAllWithHoGiaDinh();
        return convertToInvoiceReportDTO(invoices);
    }
    
    /**
     * Lấy báo cáo hóa đơn cho ACCOUNTANT - Xem tất cả hóa đơn
     */
    public List<InvoiceReportDTO> getInvoiceReportForAccountant() {
        List<HoaDon> invoices = hoaDonDAO.findAllWithHoGiaDinh();
        return convertToInvoiceReportDTO(invoices);
    }
    
    /**
     * Lấy báo cáo hóa đơn cho RESIDENT - Chỉ xem hóa đơn của hộ mình
     */
    public List<InvoiceReportDTO> getInvoiceReportForResident(String cccd) {
        // Tìm hộ gia đình của cư dân
        List<HoGiaDinh> allHouseholds = hoGiaDinhDAO.findAll();
        List<HoaDon> residentInvoices = new ArrayList<>();
        
        for (HoGiaDinh household : allHouseholds) {
            DoiTuong chuHo = household.getChuHo();
            if (chuHo != null && chuHo.getCccd().equals(cccd)) {
                // Lấy các hóa đơn của hộ này
                List<HoaDon> invoices = hoaDonDAO.findByHoGiaDinh(household);
                residentInvoices.addAll(invoices);
            }
        }
        
        return convertToInvoiceReportDTO(residentInvoices);
    }
    
    /**
     * Lấy báo cáo hộ gia đình cho ADMIN - Xem tất cả hộ
     */
    public List<HouseholdReportDTO> getHouseholdReportForAdmin() {
        List<HoGiaDinh> households = hoGiaDinhDAO.findAll();
        return convertToHouseholdReportDTO(households);
    }
    
    /**
     * Lấy báo cáo hộ gia đình cho OFFICER - Xem tất cả hộ
     */
    public List<HouseholdReportDTO> getHouseholdReportForOfficer() {
        List<HoGiaDinh> households = hoGiaDinhDAO.findAll();
        return convertToHouseholdReportDTO(households);
    }
    
    /**
     * Lấy báo cáo hộ gia đình cho RESIDENT - Chỉ xem hộ của mình
     */
    public List<HouseholdReportDTO> getHouseholdReportForResident(String cccd) {
        List<HoGiaDinh> allHouseholds = hoGiaDinhDAO.findAll();
        List<HoGiaDinh> residentHouseholds = new ArrayList<>();
        
        for (HoGiaDinh household : allHouseholds) {
            DoiTuong chuHo = household.getChuHo();
            if (chuHo != null && chuHo.getCccd().equals(cccd)) {
                residentHouseholds.add(household);
            }
        }
        
        return convertToHouseholdReportDTO(residentHouseholds);
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Chuyển đổi từ Entity TaiSanChungCu sang ApartmentReportDTO
     */
    private List<ApartmentReportDTO> convertToApartmentReportDTO(List<TaiSanChungCu> assets) {
        return assets.stream().map(asset -> {
            HoGiaDinh hoGiaDinh = asset.getHoGiaDinh();
            DoiTuong chuHo;
            String maHo = "";
            String tenHo = "";
            String tenChuHo = "";
            String sdt = "";
            String trangThaiHo = "";
            
            if (hoGiaDinh != null) {
                maHo = hoGiaDinh.getMaHo();
                tenHo = hoGiaDinh.getTenHo();
                trangThaiHo = hoGiaDinh.getTrangThai() != null ? hoGiaDinh.getTrangThai().toString() : "";
                chuHo = hoGiaDinh.getChuHo();
                
                if (chuHo != null) {
                    tenChuHo = chuHo.getHoVaTen();
                    sdt = chuHo.getSoDienThoai();
                }
            }
            
            return new ApartmentReportDTO(
                asset.getMaTaiSan(),
                asset.getTenTaiSan(),
                asset.getLoaiTaiSan() != null ? asset.getLoaiTaiSan().toString() : "",
                asset.getTrangThai() != null ? asset.getTrangThai().toString() : "",
                asset.getDienTich(),
                asset.getViTri(),
                asset.getGiaTri(),
                asset.getNgayThem(),
                maHo,
                tenHo,
                tenChuHo,
                sdt,
                trangThaiHo
            );
        }).collect(Collectors.toList());
    }
    
    /**
     * Chuyển đổi từ Entity HoaDon sang InvoiceReportDTO
     */
    private List<InvoiceReportDTO> convertToInvoiceReportDTO(List<HoaDon> invoices) {
        return invoices.stream().map(invoice -> {
            HoGiaDinh hoGiaDinh = invoice.getHoGiaDinh();
            DoiTuong chuHo;
            DoiTuong nguoiThanhToan = invoice.getNguoiThanhToan();
            
            String maHo = "";
            String tenHo = "";
            String tenChuHo = "";
            String tenNguoiThanhToan = "";
            
            if (hoGiaDinh != null) {
                maHo = hoGiaDinh.getMaHo();
                tenHo = hoGiaDinh.getTenHo();
                chuHo = hoGiaDinh.getChuHo();
                
                if (chuHo != null) {
                    tenChuHo = chuHo.getHoVaTen();
                }
            }
            
            if (nguoiThanhToan != null) {
                tenNguoiThanhToan = nguoiThanhToan.getHoVaTen();
            }
            
            return new InvoiceReportDTO(
                invoice.getMaHoaDon(),
                maHo,
                tenHo,
                tenChuHo,
                invoice.getLoaiHoaDon() != null ? invoice.getLoaiHoaDon().toString() : "",
                invoice.getSoTien(),
                invoice.getTrangThai() != null ? invoice.getTrangThai().toString() : "",
                invoice.getNgayTao(),
                invoice.getHanThanhToan(),
                invoice.getNgayThanhToan(),
                tenNguoiThanhToan,
                invoice.getGhiChu()
            );
        }).collect(Collectors.toList());
    }
    
    /**
     * Chuyển đổi từ Entity HoGiaDinh sang HouseholdReportDTO
     */
    private List<HouseholdReportDTO> convertToHouseholdReportDTO(List<HoGiaDinh> households) {
        return households.stream().map(household -> {
            DoiTuong chuHo = household.getChuHo();
            String tenChuHo = "";
            String cccdChuHo = "";
            String sdt = "";
            String email = "";
            
            if (chuHo != null) {
                tenChuHo = chuHo.getHoVaTen();
                cccdChuHo = chuHo.getCccd();
                sdt = chuHo.getSoDienThoai();
                email = chuHo.getEmail();
            }
            
            // Đếm số thành viên
            int soThanhVien = household.getThanhVienHoList() != null ? 
                household.getThanhVienHoList().size() : 0;
            
            // Đếm số căn hộ
            int soCanHo = (int) household.getTaiSanList().stream()
                .filter(ts -> ts.getLoaiTaiSan() == AssetType.can_ho)
                .count();
            
            return new HouseholdReportDTO(
                household.getMaHo(),
                household.getTenHo(),
                tenChuHo,
                cccdChuHo,
                sdt,
                email,
                soThanhVien,
                soCanHo,
                household.getTrangThai() != null ? household.getTrangThai().toString() : "",
                household.getNgayThanhLap(),
                household.getGhiChu()
            );
        }).collect(Collectors.toList());
    }
    
    // ========== RESIDENT REPORTS ==========
    
    /**
     * Lấy báo cáo cư dân cho ADMIN - Xem tất cả cư dân
     */
    public List<ResidentReportDTO> getResidentReportForAdmin() {
        List<DoiTuong> residents = doiTuongDAO.findByVaiTro(UserRole.nguoi_dung_thuong);
        return convertToResidentReportDTO(residents);
    }
    
    /**
     * Lấy báo cáo cư dân cho OFFICER - Xem tất cả cư dân
     */
    public List<ResidentReportDTO> getResidentReportForOfficer() {
        List<DoiTuong> residents = doiTuongDAO.findByVaiTro(UserRole.nguoi_dung_thuong);
        return convertToResidentReportDTO(residents);
    }
    
    // ========== DETAIL REPORT METHODS (Single Item) ==========
    
    /**
     * Lấy chi tiết một căn hộ cụ thể cho ADMIN/OFFICER
     */
    public List<ApartmentReportDTO> getApartmentDetailReport(Integer maTaiSan) {
        TaiSanChungCu apartment = taiSanChungCuDAO.findByID(maTaiSan).orElse(null);
        if (apartment == null) {
            return new ArrayList<>();
        }
        List<TaiSanChungCu> apartments = new ArrayList<>();
        apartments.add(apartment);
        return convertToApartmentReportDTO(apartments);
    }
    
    /**
     * Lấy chi tiết một cư dân cụ thể cho ADMIN/OFFICER
     */
    public List<ResidentReportDTO> getResidentDetailReport(String cccd) {
        DoiTuong resident = doiTuongDAO.findByCccd(cccd).orElse(null);
        if (resident == null) {
            return new ArrayList<>();
        }
        List<DoiTuong> residents = new ArrayList<>();
        residents.add(resident);
        return convertToResidentReportDTO(residents);
    }
    
    /**
     * Lấy chi tiết một hóa đơn cụ thể cho ADMIN/ACCOUNTANT/RESIDENT
     */
    public List<InvoiceReportDTO> getInvoiceDetailReport(Integer maHoaDon) {
        HoaDon invoice = hoaDonDAO.findById(maHoaDon).orElse(null);
        if (invoice == null) {
            return new ArrayList<>();
        }
        List<HoaDon> invoices = new ArrayList<>();
        invoices.add(invoice);
        return convertToInvoiceReportDTO(invoices);
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Chuyển đổi từ Entity DoiTuong sang ResidentReportDTO
     */
    private List<ResidentReportDTO> convertToResidentReportDTO(List<DoiTuong> residents) {
        return residents.stream().map(resident -> {
            String maHo = "";
            String tenHo = "";
            Boolean laChuHo = false;
            String quanHeChuHo = "";
            
            // Tìm hộ gia đình của cư dân
            List<HoGiaDinh> allHouseholds = hoGiaDinhDAO.findAll();
            for (HoGiaDinh household : allHouseholds) {
                if (household.getThanhVienHoList() != null) {
                    for (ThanhVienHo tvh : household.getThanhVienHoList()) {
                        if (tvh.getDoiTuong() != null && 
                            tvh.getDoiTuong().getCccd().equals(resident.getCccd())) {
                            maHo = household.getMaHo();
                            tenHo = household.getTenHo();
                            laChuHo = tvh.getLaChuHo();
                            quanHeChuHo = tvh.getQuanHeVoiChuHo();
                            break;
                        }
                    }
                }
                if (!maHo.isEmpty()) break;
            }
            
            return new ResidentReportDTO(
                resident.getCccd(),
                resident.getHoVaTen(),
                resident.getGioiTinh() != null ? resident.getGioiTinh().toString() : "",
                resident.getNgaySinh(),
                resident.getSoDienThoai(),
                resident.getEmail(),
                "", // Địa chỉ thường trú - không có trong entity DoiTuong
                resident.getTrangThaiTaiKhoan() != null ? resident.getTrangThaiTaiKhoan().toString() : "",
                resident.getVaiTro() != null ? resident.getVaiTro().toString() : "",
                maHo,
                tenHo,
                laChuHo,
                quanHeChuHo
            );
        }).collect(Collectors.toList());
    }
}
