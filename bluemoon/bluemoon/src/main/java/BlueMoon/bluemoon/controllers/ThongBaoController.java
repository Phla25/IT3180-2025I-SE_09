package BlueMoon.bluemoon.controllers;

import BlueMoon.bluemoon.services.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho hệ thống thông báo cư dân
 * Trạng thái đọc/chưa đọc được quản lý ở frontend (localStorage)
 */
@RestController
@RequestMapping("/api/resident/notifications")
public class ThongBaoController {

    @Autowired
    private ThongBaoService thongBaoService;

    /**
     * Lấy danh sách thông báo cho cư dân
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getResidentNotifications(Authentication authentication) {
        String cccd = authentication.getName();
        List<Map<String, Object>> notifications = thongBaoService.layThongBaoChoCuDan(cccd);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lấy chi tiết một thông báo
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> getNotificationDetail(
            @PathVariable Integer notificationId, 
            Authentication authentication) {
        String cccd = authentication.getName();
        return thongBaoService.layChiTietThongBao(notificationId, cccd)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gửi phản hồi cho thông báo
     */
    @PostMapping("/{notificationId}/reply")
    public ResponseEntity<String> replyToNotification(
            @PathVariable Integer notificationId,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        String cccd = authentication.getName();
        String noiDung = payload.get("noiDung");
        
        if (noiDung == null || noiDung.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung phản hồi không được để trống");
        }
        
        try {
            thongBaoService.guiPhanHoi(notificationId, cccd, noiDung);
            return ResponseEntity.ok("Gửi phản hồi thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
