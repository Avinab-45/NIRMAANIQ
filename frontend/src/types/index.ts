export interface ConstructionStage {
  percentage: string;
  milestone: string;
  elevation: string;
  badge: string;
  completion: string;
  title: string;
  desc: string;
  variance: string;
  density: string;
  curing: string;
  schedule: string;
  crew: string;
  droneY: number;
}

export interface MetricItem {
  id: string;
  category: string;
  icon: string;
  iconColor: string;
  value: string;
  valueColor: string;
  title: string;
  description: string;
}

export interface ComparisonPoint {
  title: string;
  description: string;
}

export interface PillarItem {
  number: string;
  category: string;
  title: string;
  description: string;
  icon: string;
  iconBgClass: string;
  iconTextClass: string;
  badgeColorClass: string;
  features: string[];
  statLabel: string;
  statBadge: string;
  statBadgeColorClass: string;
  statValue: string;
  statValueColorClass?: string;
  statSubtext: string;
}

export interface InspectionPhase {
  phase: string;
  title: string;
  description: string;
  icon: string;
  tag: string;
}

export interface TestimonialItem {
  quote: string;
  authorInitials: string;
  initialsBgClass: string;
  initialsTextClass: string;
  authorName: string;
  authorRole: string;
}
