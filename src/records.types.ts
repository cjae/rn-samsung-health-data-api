export type HealthDataType = 'STEPS' | 'SLEEP' | 'HEART_RATE';

export type HealthDataGap = 'hourly' | 'daily' | 'monthly';

export type TimeRangeFilter =
  | {
      operator: 'between';
      startTime: string;
      endTime: string;
      gap?: number;
      groupBy?: HealthDataGap;
    }
  | {
      operator: 'after';
      startTime: string;
      gap?: number;
      groupBy?: HealthDataGap;
    }
  | {
      operator: 'before';
      endTime: string;
      gap?: number;
      groupBy?: HealthDataGap;
    };

export interface ReadRecordsOptions {
  timeRangeFilter: TimeRangeFilter;
  ascendingOrder?: boolean;
}
