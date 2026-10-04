export const OFF_ROAD_REASONS = ['Workshop', 'No driver', 'Accident', 'Other'] as const;
export type OffRoadReason = (typeof OFF_ROAD_REASONS)[number];
