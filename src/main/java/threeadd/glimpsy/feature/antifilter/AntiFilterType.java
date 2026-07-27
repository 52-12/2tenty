package threeadd.glimpsy.feature.antifilter;

public enum AntiFilterType {

    DROP("antifilter.drop-title", "Anti-Drop"),
    PICKUP("antifilter.pickup-title", "Anti-Pickup");

    private final String titleKey;
    private final String titleDefault;

    AntiFilterType(String titleKey, String titleDefault) {
        this.titleKey = titleKey;
        this.titleDefault = titleDefault;
    }

    public String titleKey() {
        return titleKey;
    }

    public String titleDefault() {
        return titleDefault;
    }
}
