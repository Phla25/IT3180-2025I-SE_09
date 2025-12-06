package BlueMoon.bluemoon.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "thong_bao_da_doc", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ma_thong_bao", "cccd_nguoi_doc"})
})
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
    private LocalDateTime thoiGianDoc;

    public ThongBaoDaDoc() {}

    public ThongBaoDaDoc(ThongBao thongBao, DoiTuong nguoiDoc) {
        this.thongBao = thongBao;
        this.nguoiDoc = nguoiDoc;
    }

    @PrePersist
    protected void onCreate() {
        if (this.thoiGianDoc == null) {
            this.thoiGianDoc = LocalDateTime.now();
        }
    }

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ThongBao getThongBao() { return thongBao; }
    public void setThongBao(ThongBao thongBao) { this.thongBao = thongBao; }
    public DoiTuong getNguoiDoc() { return nguoiDoc; }
    public void setNguoiDoc(DoiTuong nguoiDoc) { this.nguoiDoc = nguoiDoc; }
    public LocalDateTime getThoiGianDoc() { return thoiGianDoc; }
    public void setThoiGianDoc(LocalDateTime thoiGianDoc) { this.thoiGianDoc = thoiGianDoc; }
}