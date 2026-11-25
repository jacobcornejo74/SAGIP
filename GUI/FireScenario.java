package GUI;

import java.util.*;
import java.util.List;

/**
 * INHERITANCE & POLYMORPHISM: Fire Scenario
 */
class FireScenario extends DisasterScenario {
    public FireScenario() {
        super("Fire", "Thermal conflagration, lithium runaway, and smoke toxicity.", "Fuel load & blocked egress.");
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
        if (hazards.isEmpty()) return "Thermal baseline clear. Verify fire extinguisher access and unblocked exit doors.";
        for (HazardItem h : hazards) {
            String n = h.getObjectName().toUpperCase();
            if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE") || n.contains("HAIR DRIER")) {
                return "High-wattage thermal appliance (" + h.getObjectName() + ") detected! Enforce 36-inch combustible clearance.";
            }
            if (n.contains("LAPTOP") || n.contains("CELL PHONE")) {
                return "Lithium-ion battery device (" + h.getObjectName() + ") detected. Monitor for swelling or thermal runaway.";
            }
            if (n.contains("BOTTLE")) {
                return "Pressurized or liquid vessel (" + h.getObjectName() + ") detected. Isolate solvents/aerosols in fire cabinet.";
            }
        }
        return "Combustible fuel load present in frame. Keep evacuation corridor clear of smoke-producing synthetics.";
    }

    @Override
    public String getHazardExplanation(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("PERSON")) return "Occupant in thermal monitoring zone; ensure immediate smoke egress visibility.";
        if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE")) return "Primary thermal ignition source capable of igniting adjacent combustibles.";
        if (n.contains("BOTTLE")) return "Potential solvent, alcohol, or pressurized vessel rupture risk under radiant heat.";
        if (n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("TV")) return "Lithium cell / capacitor bank vulnerable to self-sustaining thermal runaway.";
        if (n.contains("COUCH") || n.contains("CHAIR") || n.contains("BED") || n.contains("BACKPACK")) return "Synthetic polyurethane foam rapidly accelerates flame spread and dense toxic smoke.";
        if (n.contains("BOOK")) return "Dry cellulose stack acts as sustained solid fuel load.";
        return "Combustible surface contributing to room fire fuel density.";
    }

    @Override
    public RiskLevel getHazardRisk(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("OVEN") || n.contains("TOASTER") || n.contains("MICROWAVE") || n.contains("BOTTLE")) return RiskLevel.CRITICAL;
        if (n.contains("LAPTOP") || n.contains("CELL PHONE") || n.contains("COUCH") || n.contains("BED") || n.contains("BOOK")) return RiskLevel.HIGH;
        if (n.contains("CHAIR") || n.contains("TV") || n.contains("BACKPACK")) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }
}
