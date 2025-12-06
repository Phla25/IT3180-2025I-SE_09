package BlueMoon.bluemoon.utils;

public enum IncidentType {
    dien("dien"),
    nuoc("nuoc"),
    ve_sinh("ve_sinh"),
    an_ninh("an_ninh"),
    mat_do("mat_do"),
    khac("khac");
    
    private final String dbValue;
    IncidentType(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }
}
