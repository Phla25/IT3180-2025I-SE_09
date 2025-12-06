package BlueMoon.bluemoon.controllers;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import BlueMoon.bluemoon.entities.DoiTuong;
import BlueMoon.bluemoon.services.NguoiDungService;
import BlueMoon.bluemoon.services.ThongBaoService;

@Controller
@RequestMapping("/resident/notifications")
public class ResidentNotificationController {

    @Autowired private ThongBaoService thongBaoService;
    @Autowired private NguoiDungService nguoiDungService;

    // ... Các method hiển thị danh sách (giữ nguyên) ...

    // [MỚI] API để JS gọi khi cư dân click xem thông báo
    @PostMapping("/mark-read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@RequestBody Map<String, Integer> payload, Principal principal) {
        try {
            Integer maThongBao = payload.get("maThongBao");
            DoiTuong currentUser = nguoiDungService.timNguoiDungThuongTheoCCCD(principal.getName()).orElse(null);
            
            if (currentUser != null && maThongBao != null) {
                thongBaoService.danhDauDaDoc(maThongBao, currentUser);
                return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}