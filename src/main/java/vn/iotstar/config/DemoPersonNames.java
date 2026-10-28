package vn.iotstar.config;

import vn.iotstar.enums.Role;

/** Stable Vietnamese names for automatically generated local test accounts. */
final class DemoPersonNames {
    private static final String[] FAMILY_NAMES = {
            "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ"
    };
    private static final String[] MIDDLE_NAMES = {
            "Minh", "Gia", "Quốc", "Thanh", "Ngọc", "Thảo", "Hoàng", "Khánh"
    };
    private static final String[] GIVEN_NAMES = {
            "Anh", "An", "Bảo", "Chi", "Duy", "Hà", "Hân", "Khang", "Linh", "Mai", "Nam", "Phúc"
    };

    private DemoPersonNames() {}

    static String forRole(Role role, int index) {
        int nameIndex = Math.max(0, index - 1) + role.ordinal() * 128;
        String family = FAMILY_NAMES[nameIndex % FAMILY_NAMES.length];
        String middle = MIDDLE_NAMES[(nameIndex / FAMILY_NAMES.length) % MIDDLE_NAMES.length];
        String given = GIVEN_NAMES[(nameIndex / (FAMILY_NAMES.length * MIDDLE_NAMES.length)) % GIVEN_NAMES.length];
        return family + " " + middle + " " + given;
    }
}
