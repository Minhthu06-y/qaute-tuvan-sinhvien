package vn.iotstar.enums;

public enum AdvisorType {
    STUDENT("Sinh viên"),
    ADVISOR("Tư vấn viên"),
    LECTURER("Giảng viên"),
    STAFF("Cán bộ"),
    TEAM_LEADER("Trưởng ban cố vấn");

    private final String label;

    AdvisorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
