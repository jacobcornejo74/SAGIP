package GUI;

import java.util.*;
import java.util.List;

/**
 * INHERITANCE & POLYMORPHISM: Flood Scenario
 */
class FloodScenario extends DisasterScenario {
    public FloodScenario() {
        super("Flood", "Water inundation and rising conductive surface levels.", "Electrocution & chemical leaching.");
    }

    @Override
    public RiskLevel evaluateOverallRisk(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return RiskLevel.LOW;
        boolean hasCrit = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.CRITICAL);
        if (hasCrit) return RiskLevel.CRITICAL;
        boolean hasHigh = hazards.stream().anyMatch(h -> getHazardRisk(h) == RiskLevel.HIGH);
        return hasHigh ? RiskLevel.HIGH : RiskLevel.MODERATE;
    }

    @Override
    public String getAdvisoryWarning(List<HazardItem> hazards) {
        if (hazards.isEmpty()) return "No low-elevation energized electronics or open chemical vessels detected.";
        for (HazardItem h : hazards) {
            String n = h.getObjectName().toUpperCase();
            if (n.contains("TV") || n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("MICROWAVE") || n.contains("TOASTER") || n.contains("REFRIGERATOR") || n.contains("KEYBOARD")) {
                return "Energized electronics (" + h.getObjectName() + ") detected! Disconnect main breaker before water contact.";
            }
            if (n.contains("BOTTLE") || n.contains("CUP") || n.contains("SINK")) {
                return "Liquid container or plumbing fixture (" + h.getObjectName() + ") detected. Seal vessels to prevent floodwater contamination.";
            }
        }
        return "Elevate buoyant obstacles to prevent blocked drainage and submerged trip hazards.";
    }

    @Override
    public String getHazardExplanation(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("PERSON")) return "Occupant present; evacuate upward if conductive water enters room.";
        if (n.contains("LAPTOP") || n.contains("TV") || n.contains("MICROWAVE") || n.contains("TOASTER") || n.contains("OVEN") || n.contains("REFRIGERATOR")) {
            return "Energized AC/lithium circuitry creates lethal electrocution path upon inundation.";
        }
        if (n.contains("CELL PHONE") || n.contains("KEYBOARD") || n.contains("MOUSE") || n.contains("REMOTE")) {
            return "Low-voltage battery/peripheral susceptible to short-circuit and lithium outgassing in water.";
        }
        if (n.contains("BOTTLE") || n.contains("VASE") || n.contains("CUP")) {
            return "Container can float, tip, and disperse contents into standing water.";
        }
        if (n.contains("SINK")) return "Backflow vector during municipal storm surge.";
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("BACKPACK") || n.contains("BOOK")) {
            return "Porous material absorbs contaminated floodwater and increases structural floor load.";
        }
        return "Submerged obstruction hazard in rising water conditions.";
    }

    @Override
    public RiskLevel getHazardRisk(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("MICROWAVE") || n.contains("TOASTER") || n.contains("OVEN") || n.contains("REFRIGERATOR") || n.contains("TV")) return RiskLevel.CRITICAL;
        if (n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("BOTTLE") || n.contains("SINK")) return RiskLevel.HIGH;
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("BACKPACK")) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }
}
