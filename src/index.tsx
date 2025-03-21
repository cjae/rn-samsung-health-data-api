import { NativeModules, Platform } from 'react-native';

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

// Define health data types
export type HealthDataType = 'STEPS' | 'SLEEP' | 'HEART_RATE';

// Define permission result interface
export interface PermissionResult {
  allGranted: boolean;
  message: string;
  grantedPermissions?: string[];
  deniedPermissions?: string[];
}

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
