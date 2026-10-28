package vn.iotstar.config;

import java.util.List;

import vn.iotstar.enums.AdvisorType;

/** Rotates presentation titles for generated local consultant accounts. */
final class DemoAdvisorTitles {
    private static final List<AdvisorType> TITLES = List.of(
            AdvisorType.STUDENT, AdvisorType.LECTURER, AdvisorType.STAFF);

    private DemoAdvisorTitles() {}

    static AdvisorType forIndex(int index) {
        return TITLES.get(Math.floorMod(index - 1, TITLES.size()));
    }
}
