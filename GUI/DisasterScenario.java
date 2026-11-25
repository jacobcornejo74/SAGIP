package GUI;

import java.util.*;
import java.util.List;

/**
 * ABSTRACTION: Base contract for disaster scenarios.
 */
abstract class DisasterScenario {
    private final String name;
    private final String description;
    private final String primaryConcern;

    protected DisasterScenario(String name, String description, String primaryConcern) {
        this.name = name;
        this.description = description;
        this.primaryConcern = primaryConcern;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrimaryConcern() { return primaryConcern; }

    public abstract RiskLevel evaluateOverallRisk(List<HazardItem> hazards);
    public abstract String getAdvisoryWarning(List<HazardItem> hazards);
    public abstract String getHazardExplanation(HazardItem hazard);
    public abstract RiskLevel getHazardRisk(HazardItem hazard);

    @Override
    public String toString() { return name.toUpperCase() + " PROTOCOL"; }
}
