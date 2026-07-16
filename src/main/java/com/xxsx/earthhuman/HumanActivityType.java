package com.xxsx.earthhuman;

public enum HumanActivityType {
    IDLE(0, "idle"),
    RESTING(1, "resting"),
    SLEEPING(2, "sleeping"),
    WALKING(3, "walking"),
    SPRINTING(4, "sprinting"),
    SWIMMING(5, "swimming"),
    CLIMBING(6, "climbing"),
    RIDING(7, "riding"),
    ELYTRA(8, "elytra"),
    FLYING(9, "flying");

    public final int networkId;
    public final String id;

    HumanActivityType(int networkId, String id) {
        this.networkId = networkId;
        this.id = id;
    }

    public String translationKey() {
        return "activity.earth_human." + id;
    }

    public static HumanActivityType byNetworkId(int networkId) {
        for (HumanActivityType type : values()) {
            if (type.networkId == networkId) {
                return type;
            }
        }
        return IDLE;
    }

    public static HumanActivityType byId(String id) {
        if (id != null) {
            for (HumanActivityType type : values()) {
                if (type.id.equalsIgnoreCase(id.trim())) {
                    return type;
                }
            }
        }
        return IDLE;
    }
}
