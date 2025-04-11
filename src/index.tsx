import { NativeModules, Platform } from 'react-native';
import type {
  HeartRateData,
  PermissionResult,
  SleepData,
  StepsData,
} from './results.types';
import type {
  HealthDataType,
  ReadRecordsOptions,
  TimeRangeFilter,
} from './records.types';

const LINKING_ERROR =
  `The package 'rn-samsung-health-data-api' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RnSamsungHealthDataApi = NativeModules.RnSamsungHealthDataApi
  ? NativeModules.RnSamsungHealthDataApi
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

// Define health data functions
export function initializeHealthStore(): Promise<boolean> {
  return RnSamsungHealthDataApi.initializeHealthStore();
}

export function checkHealthPermissionsGranted(
  permissions: HealthDataType[]
): Promise<PermissionResult> {
  return RnSamsungHealthDataApi.checkHealthPermissionsGranted(permissions);
}

export function requestHealthPermissions(
  permissions: HealthDataType[]
): Promise<PermissionResult> {
  return RnSamsungHealthDataApi.requestHealthPermissions(permissions);
}

export function readStepData(option: ReadRecordsOptions): Promise<StepsData> {
  const { timeRangeFilter, ascendingOrder } = option;
  const { operator, groupBy = 'hourly', gap = 1 } = timeRangeFilter;

  // Handle the properties based on operator type
  let startDate: string | undefined;
  let endDate: string | undefined;

  if (operator === 'between') {
    startDate = (timeRangeFilter as { startTime: string }).startTime;
    endDate = (timeRangeFilter as { endTime: string }).endTime;
  } else if (operator === 'after') {
    startDate = (timeRangeFilter as { startTime: string }).startTime;
    endDate = undefined;
  } else if (operator === 'before') {
    startDate = undefined;
    endDate = (timeRangeFilter as { endTime: string }).endTime;
  }

  // Convert your operator names to the ones used in Kotlin
  return RnSamsungHealthDataApi.readStepData(
    gap,
    operator,
    groupBy,
    startDate,
    endDate,
    ascendingOrder
  );
}

export function readSleepData(filter: TimeRangeFilter): Promise<SleepData> {
  const { operator } = filter;

  // Handle the properties based on operator type
  let startDate: string | undefined;
  let endDate: string | undefined;

  if (operator === 'between') {
    startDate = (filter as { startTime: string }).startTime;
    endDate = (filter as { endTime: string }).endTime;
  } else if (operator === 'after') {
    startDate = (filter as { startTime: string }).startTime;
    endDate = undefined;
  } else if (operator === 'before') {
    startDate = undefined;
    endDate = (filter as { endTime: string }).endTime;
  }

  // Convert your operator names to the ones used in Kotlin
  return RnSamsungHealthDataApi.readSleepData(operator, startDate, endDate);
}

export function readHeartRateData(
  option: ReadRecordsOptions
): Promise<HeartRateData> {
  const { timeRangeFilter, ascendingOrder } = option;
  const { operator } = timeRangeFilter;

  // Handle the properties based on operator type
  let startDate: string | undefined;
  let endDate: string | undefined;

  if (operator === 'between') {
    startDate = (timeRangeFilter as { startTime: string }).startTime;
    endDate = (timeRangeFilter as { endTime: string }).endTime;
  } else if (operator === 'after') {
    startDate = (timeRangeFilter as { startTime: string }).startTime;
    endDate = undefined;
  } else if (operator === 'before') {
    startDate = undefined;
    endDate = (timeRangeFilter as { endTime: string }).endTime;
  }

  // Convert your operator names to the ones used in Kotlin
  return RnSamsungHealthDataApi.readHeartRateData(
    operator,
    startDate,
    endDate,
    ascendingOrder
  );
}
