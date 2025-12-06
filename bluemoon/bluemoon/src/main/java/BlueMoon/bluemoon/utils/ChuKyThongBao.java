package BlueMoon.bluemoon.utils;

public enum ChuKyThongBao {
    HANG_TUAN("hang_tuan"),
    HANG_THANG("hang_thang"),
    HANG_NAM("hang_nam");
    
    private final String dbValue;
    
    ChuKyThongBao(String dbValue) {
        this.dbValue = dbValue;
    }
    
    public String getDbValue() {
        return dbValue;
    }
}

