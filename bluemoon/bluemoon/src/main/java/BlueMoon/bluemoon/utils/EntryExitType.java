package BlueMoon.bluemoon.utils;

public enum EntryExitType {
    VAO("Vào"),
    RA("Ra");

    private final String displayValue;

    EntryExitType(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}