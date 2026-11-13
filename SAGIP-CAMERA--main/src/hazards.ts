export type RiskSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type HazardTypeTag = 'F' | 'E';

export interface HazardProfile {
  title: string;
  severity: RiskSeverity;
  tag: HazardTypeTag;
  risks: string[];
}

export interface DetectedObjectSummary {
  label: string;
  confidence: number;
  severity: RiskSeverity;
  tag: HazardTypeTag;
  risks: string[];
}

export interface SavedAssessment {
  id: string;
  timestamp: string;
  overallSeverity: RiskSeverity;
  detectionCount: number;
  snapshotDataUrl: string;
  items: DetectedObjectSummary[];
}

export function getRiskColor(severity: RiskSeverity, darkMode: boolean): string {
  switch (severity) {
    case 'CRITICAL':
      return darkMode ? '#E2483F' : '#B8302A';
    case 'HIGH':
      return darkMode ? '#F39137' : '#CC681C';
    case 'MEDIUM':
      return darkMode ? '#F0C43C' : '#BE8E10';
    case 'LOW':
      return darkMode ? '#68BE74' : '#348444';
  }
}

export function getHazardProfile(rawClass: string): HazardProfile {
  const c = rawClass.toLowerCase();

  if (
    c.includes('oven') ||
    c.includes('toaster') ||
    c.includes('microwave') ||
    c.includes('hair drier')
  ) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      severity: 'CRITICAL',
      tag: 'F',
      risks: [
        'thermal ignition source near surrounding surfaces',
        'may shift or fall during an earthquake',
        'severe electrocution risk if submerged in floods',
      ],
    };
  }

  if (c.includes('knife') || c.includes('scissors') || c.includes('fork')) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      severity: 'CRITICAL',
      tag: 'E',
      risks: [
        'loose sharp object may fall during an earthquake',
        'submerged puncture hazard in floodwater',
        'heat conduction hazard near open flame',
      ],
    };
  }

  if (
    c.includes('cabinet') ||
    c.includes('refrigerator') ||
    c.includes('bookcase') ||
    c.includes('wardrobe') ||
    c.includes('shelf')
  ) {
    return {
      title: 'Cabinet / Storage Unit',
      severity: 'HIGH',
      tag: 'E',
      risks: [
        'may fall during an earthquake',
        'Uneven surface that may caused floods',
        'combustible objects',
      ],
    };
  }

  if (c.includes('laptop') || c.includes('tv') || c.includes('monitor') || c.includes('cell phone')) {
    return {
      title: rawClass.toUpperCase(),
      severity: 'HIGH',
      tag: 'F',
      risks: [
        'lithium battery thermal runaway fire risk',
        'unanchored unit may fall during an earthquake',
        'electrocution risk if submerged in floods',
      ],
    };
  }

  if (
    c.includes('bottle') ||
    c.includes('wine glass') ||
    c.includes('cup') ||
    c.includes('vase') ||
    c.includes('bowl')
  ) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      severity: 'HIGH',
      tag: 'E',
      risks: [
        'shatter risk producing glass debris in earthquake',
        'liquid spill or floating obstacle in floods',
        'flammable solvent or aerosol rupture near fire',
      ],
    };
  }

  if (
    c.includes('chair') ||
    c.includes('couch') ||
    c.includes('sofa') ||
    c.includes('bed') ||
    c.includes('dining table') ||
    c.includes('table')
  ) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      severity: 'MEDIUM',
      tag: 'E',
      risks: [
        'may slide and block exit path during earthquake',
        'Uneven surface that may caused floods',
        'combustible objects and upholstery fuel load',
      ],
    };
  }

  if (c.includes('book') || c.includes('backpack') || c.includes('suitcase') || c.includes('handbag')) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      severity: 'MEDIUM',
      tag: 'F',
      risks: [
        'combustible objects (paper and fabric fuel load)',
        'loose item may tumble and block egress in tremor',
        'absorbent mass waterlogged in flood conditions',
      ],
    };
  }

  if (c.includes('person')) {
    return {
      title: 'Occupant',
      severity: 'LOW',
      tag: 'E',
      risks: [
        'keep clear of overhead falling objects in earthquake',
        'avoid standing water near energized outlets in flood',
        'maintain clear smoke evacuation exit',
      ],
    };
  }

  return {
    title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
    severity: 'LOW',
    tag: 'E',
    risks: [
      'may fall during an earthquake',
      'Uneven surface that may caused floods',
      'combustible objects',
    ],
  };
}

export function computeOverallSeverity(items: DetectedObjectSummary[]): RiskSeverity {
  if (items.some((i) => i.severity === 'CRITICAL')) return 'CRITICAL';
  if (items.some((i) => i.severity === 'HIGH')) return 'HIGH';
  if (items.some((i) => i.severity === 'MEDIUM')) return 'MEDIUM';
  return 'LOW';
}
