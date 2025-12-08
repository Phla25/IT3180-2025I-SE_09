package BlueMoon.bluemoon.utils;

public enum RecipientType {
    tat_ca("tat_ca"),
    chu_ho("chu_ho"),
    ca_nhan("ca_nhan"),
    theo_ho("theo_ho");
    
    private final String dbValue;
    RecipientType(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }
}