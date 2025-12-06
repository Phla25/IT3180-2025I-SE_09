package BlueMoon.bluemoon.controllers;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.ThongBaoService;

@ControllerAdvice(basePackages = "BlueMoon.bluemoon.controllers")
public class GlobalControllerAdvice {

    @Autowired private NguoiDungService nguoiDungService;
    @Autowired private ThongBaoService thongBaoService;

    @ModelAttribute("unreadCount")
    public Long globalUnreadCount(Principal principal) {
        if (principal == null) {
            return 0L; // Chưa đăng nhập
        }

        try {
            // Cố gắng tìm người dùng thường (Cư dân)
            // Sử dụng Optional để tránh NullPointerException nếu service trả về Optional
            Optional<DoiTuong> userOpt = nguoiDungService.timNguoiDungThuongTheoCCCD(principal.getName());
            
            if (userOpt.isPresent()) {
                return thongBaoService.demSoThongBaoChuaDoc(userOpt.get());
            }
        } catch (Exception e) {
            // Nếu xảy ra lỗi "Người dùng không tồn tại" (do đang login bằng Admin)
            // Thì bỏ qua và trả về 0
            return 0L;
        }
        
        return 0L;
    }
}