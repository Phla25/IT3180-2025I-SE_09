    package BlueMoon.bluemoon.utils;

    public enum ServiceType {
        dinh_ky("dinh_ky"),
        theo_yeu_cau("theo_yeu_cau");
        
        private final String dbValue;
        ServiceType(String dbValue) { this.dbValue = dbValue; }
        public String getDbValue() { return dbValue; }
    }