package BlueMoon.bluemoon.utils;

public enum NotificationType {
    quan_trong("quan_trong"),
    binh_thuong("binh_thuong"),
    khan_cap("khan_cap"),
    bao_tri_tai_san("bao_tri_tai_san"),
    bao_tri_dich_vu("bao_tri_dich_vu"),
    bao_tri_chung_cu("bao_tri_chung_cu"),
    thong_bao_chung("thong_bao_chung");
    
    private final String dbValue;
    NotificationType(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }
}