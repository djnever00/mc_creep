package com.dae.creep;

public enum CreepSkinType {
    BASE("creep"),
    TINT("tint"),
    DIM("dim"),
    DARK("dark"),
    SPOT("spot"),
    BLU("blu"),
    CAMO("camo"),
    ORA("ora"),
    RED("red"),
    SNOW("snow"),
    YEL("yel");

    private final String id;

    CreepSkinType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static CreepSkinType fromId(String id) {
        for (CreepSkinType value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return BASE;
    }
}
