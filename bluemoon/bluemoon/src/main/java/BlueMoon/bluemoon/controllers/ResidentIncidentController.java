package BlueMoon.bluemoon.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import BlueMoon.bluemoon.entities.BaoCaoSuCo;
import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.entities.PhanHoi;
import BlueMoon.bluemoon.entities.TaiSanChungCu;
import BlueMoon.bluemoon.services.BaoCaoSuCoService;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.PhanHoiService;
import BlueMoon.bluemoon.services.TaiSanChungCuService;
import BlueMoon.bluemoon.utils.PriorityLevel;

@Controller
@RequestMapping("/resident/incidents")
public class ResidentIncidentController {

    @Autowired private BaoCaoSuCoService baoCaoSuCoService;
    @Autowired private NguoiDungService nguoiDungService;
    @Autowired private TaiSanChungCuService taiSanChungCuService;
    @Autowired private PhanHoiService phanHoiService; // Service lấy phản hồi

    /**
     * Hiển thị trang danh sách & Modal tạo mới
     */
    @GetMapping
    public String showIncidentPage(Model model, Principal principal) {
        // Lấy user hiện tại
        DoiTuong currentUser = nguoiDungService.timNguoiDungThuongTheoCCCD(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        // Lấy danh sách sự cố của user
        List<BaoCaoSuCo> myIncidents = baoCaoSuCoService.getIncidentsByNguoiBaoCao(currentUser);
        
        // Lấy tất cả tài sản (để hiển thị dropdown chọn)
        List<TaiSanChungCu> assetList = taiSanChungCuService.getAllAssets(null);

        model.addAttribute("user", currentUser);
        model.addAttribute("danhSachSuCo", myIncidents);
        model.addAttribute("danhSachTaiSan", assetList);

        return "incident-resident"; 
    }

    /**
     * [MỚI] API xem chi tiết sự cố + Lịch sử xử lý
     */
    @GetMapping("/detail/{id}")
    public String getIncidentDetail(@PathVariable("id") Integer id, Model model) {
        // 1. Lấy thông tin sự cố
        BaoCaoSuCo suCo = baoCaoSuCoService.getIncidentById(id);
        
        // 2. Lấy danh sách phản hồi/tiến độ từ Admin
        List<PhanHoi> history = phanHoiService.getFeedbackByIncident(suCo);
        
        model.addAttribute("suCo", suCo);
        model.addAttribute("lichSuXuLy", history); // Truyền list này ra view

        // Trả về Fragment "detailContent" để nhúng vào Modal
        return "incident-resident :: detailContent"; 
    }

    /**
     * Xử lý tạo mới
     */
    @PostMapping("/create")
    public String createIncident(Principal principal,
                                 @RequestParam("tieuDe") String tieuDe,
                                 @RequestParam("maTaiSan") Integer maTaiSan,
                                 @RequestParam("mucDo") String mucDoStr,
                                 @RequestParam("noiDung") String noiDung) {
        try {
            DoiTuong reporter = nguoiDungService.timNguoiDungThuongTheoCCCD(principal.getName()).orElseThrow();
            TaiSanChungCu taiSan = taiSanChungCuService.getAssetById(maTaiSan).orElseThrow();
            PriorityLevel priority = PriorityLevel.valueOf(mucDoStr);

            baoCaoSuCoService.createIncident(reporter, taiSan, tieuDe, noiDung, priority);
            return "redirect:/resident/incidents?success";
        } catch (Exception e) {
            return "redirect:/resident/incidents?error";
        }
    }
}