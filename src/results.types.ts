export interface PermissionResult {
  allGranted: boolean;
  message: string;
  grantedPermissions?: string[];
  deniedPermissions?: string[];
}

export interface StepsData {
  count: number;
  data: StepRecord[];
}

export interface StepRecord {
  value: number;
  startTime: string;
  endTime: string;
}

export interface SleepData {
  totalDurationInHours: number;
  totalDurationInMinutes: number;
  data: SleepRecord[];
}

export interface SleepRecord {
  score: number;
  sessionCount: number;
  durationHours: number;
  durationMinutes: number;
  startTime: string;
  endTime: string;
}

export interface HeartRateData {
  unit: string;
  data: HeartRateRecord[];
}

export interface HeartRateRecord {
  min: number | null;
  max: number | null;
  heartRate: number | null;
  startTime: string;
  endTime: string;
}
