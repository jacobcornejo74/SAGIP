package GUI;

import java.util.*;
import java.util.List;

/**
 * INHERITANCE & POLYMORPHISM: Earthquake Scenario
 */
class EarthquakeScenario extends DisasterScenario {
    public EarthquakeScenario() {
        super("Earthquake", "Seismic ground acceleration and structural displacement.", "Toppling mass & shattering glass.");
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
        if (hazards.isEmpty()) return "Field clear of immediate seismic fall or shatter hazards. Maintain open egress.";
        for (HazardItem h : hazards) {
            String n = h.getObjectName().toUpperCase();
            if (n.contains("TV") || n.contains("LAPTOP") || n.contains("MONITOR") || n.contains("REFRIGERATOR")) {
                return "Unanchored heavy equipment (" + h.getObjectName() + ") detected. Secure with seismic straps or lower to floor.";
            }
            if (n.contains("BOTTLE") || n.contains("GLASS") || n.contains("CUP") || n.contains("VASE")) {
                return "Fragile vessel (" + h.getObjectName() + ") detected. Relocate away from elevated edges to prevent glass shrapnel.";
            }
            if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("TABLE") || n.contains("BENCH")) {
                return "Movable furniture (" + h.getObjectName() + ") detected. Verify 36-inch egress path remains unobstructed.";
            }
        }
        return "Objects detected in seismic zone. Maintain drop, cover, and hold-on clearance.";
    }

    @Override
    public String getHazardExplanation(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("PERSON")) return "Occupant inside active monitoring zone; prioritize drop-cover-hold clearance.";
        if (n.contains("TV") || n.contains("LAPTOP") || n.contains("MONITOR")) return "High-mass display unit prone to projectile toppling under lateral tremor.";
        if (n.contains("BOTTLE") || n.contains("CUP") || n.contains("WINE GLASS") || n.contains("VASE")) return "Brittle vessel shatter risk creating laceration debris along exit route.";
        if (n.contains("KNIFE") || n.contains("SCISSORS")) return "Loose sharp implement capable of sliding off surfaces during ground motion.";
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("DINING TABLE")) return "Unanchored furniture mass; may slide and jam exit doorways.";
        if (n.contains("BOOK") || n.contains("CLOCK") || n.contains("POTTED PLANT")) return "Overhead or shelf-mounted projectile hazard during shaking.";
        if (n.contains("REFRIGERATOR") || n.contains("OVEN")) return "Heavy appliance capable of tipping or rupturing utility connections.";
        return "Unsecured physical mass subject to displacement during seismic shaking.";
    }

    @Override
    public RiskLevel getHazardRisk(HazardItem hazard) {
        String n = hazard.getObjectName().toUpperCase();
        if (n.contains("KNIFE") || n.contains("SCISSORS") || n.contains("REFRIGERATOR")) return RiskLevel.CRITICAL;
        if (n.contains("TV") || n.contains("LAPTOP") || n.contains("BOTTLE") || n.contains("WINE GLASS") || n.contains("VASE") || n.contains("CUP")) return RiskLevel.HIGH;
        if (n.contains("CHAIR") || n.contains("COUCH") || n.contains("POTTED PLANT") || n.contains("BOOK")) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }
}
