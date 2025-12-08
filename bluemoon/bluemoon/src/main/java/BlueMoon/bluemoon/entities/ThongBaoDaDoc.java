package BlueMoon.bluemoon.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "thong_bao_da_doc", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ma_thong_bao", "cccd_nguoi_doc"})
})
@DynamicUpdate
public class ThongBaoDaDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_thong_bao", nullable = false)
    private ThongBao thongBao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cccd_nguoi_doc", nullable = false)
    private DoiTuong nguoiDoc;

    @Column(name = "thoi_gian_doc")
    private LocalDateTime thoiGianDoc = LocalDateTime.now();

    // Constructors
    public ThongBaoDaDoc() {}

    public ThongBaoDaDoc(ThongBao thongBao, DoiTuong nguoiDoc) {
        this.thongBao = thongBao;
        this.nguoiDoc = nguoiDoc;
        this.thoiGianDoc = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ThongBao getThongBao() { return thongBao; }
    public void setThongBao(ThongBao thongBao) { this.thongBao = thongBao; }
    public DoiTuong getNguoiDoc() { return nguoiDoc; }
    public void setNguoiDoc(DoiTuong nguoiDoc) { this.nguoiDoc = nguoiDoc; }
    public LocalDateTime getThoiGianDoc() { return thoiGianDoc; }
    public void setThoiGianDoc(LocalDateTime thoiGianDoc) { this.thoiGianDoc = thoiGianDoc; }
}