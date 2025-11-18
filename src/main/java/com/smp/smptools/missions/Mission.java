package com.smp.smptools.missions;

import java.util.List;

public class Mission {
    private final String id;
    private final String name;
    private final String description;
    private final MissionType type;
    private final String objective;
    private final int amount;
    private final List<String> rewards;
    private final List<String> prerequisites;

    public Mission(String id, String name, String description, MissionType type, String objective, int amount, List<String> rewards, List<String> prerequisites) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.objective = objective;
        this.amount = amount;
        this.rewards = rewards;
        this.prerequisites = prerequisites;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public MissionType getType() { return type; }
    public String getObjective() { return objective; }
    public int getAmount() { return amount; }
    public List<String> getRewards() { return rewards; }
    public List<String> getPrerequisites() { return prerequisites; }
}
