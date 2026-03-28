package com.dae.creep;

public enum CreepSkinMode {
    STATIC("static"),
    FADE("fade");

    private final String id;

    CreepSkinMode(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static CreepSkinMode fromId(String id) {
        for (CreepSkinMode value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return STATIC;
    }
}
