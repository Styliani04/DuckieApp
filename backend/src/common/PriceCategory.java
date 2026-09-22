package common;

public enum PriceCategory {
    $(1),
    $$(2),
    $$$(3);

    private final int level;

    PriceCategory(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isCheaperThan(PriceCategory other) {
        return this.level <= other.level;
    }

    public boolean isMoreExpensiveThan(PriceCategory other) {
        return this.level > other.level;
    }

    public boolean isSameAs(PriceCategory other) {
        return this.level == other.level;
    }
}
