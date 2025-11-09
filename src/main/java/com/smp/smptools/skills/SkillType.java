package com.smp.smptools.skills;

public enum SkillType {
    MINING("Mining"),
    WOODCUTTING("Woodcutting"),
    EXCAVATION("Excavation");

    private final String displayName;

    SkillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
