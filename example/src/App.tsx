import {
  initializeHealthStore,
  checkHealthPermissionsGranted,
  requestHealthPermissions,
  readStepData,
  readSleepData,
} from 'rn-samsung-health-data-api';
import { Text, View, StyleSheet } from 'react-native';
import { useState, useEffect } from 'react';
import { endOfWeek, startOfWeek } from 'date-fns';

export default function App() {
  const [result, setResult] = useState<string | undefined>();
  const [isPermissionsGranted, setIsPermissionsGrant] =
    useState<boolean>(false);

  useEffect(() => {
    const handleInitialize = async () => {
      const initData = await initializeHealthStore();
      if (!initData) {
        setResult('Error');
      }

      setResult('Init');

      try {
        const granted = await checkHealthPermissionsGranted(['STEPS', 'SLEEP']);
        if (granted.allGranted) {
          setIsPermissionsGrant(true);
        } else {
          const requested = await requestHealthPermissions(['STEPS', 'SLEEP']);
          if (requested.allGranted) {
            setIsPermissionsGrant(true);
          } else {
            console.log('Permissions Requested:', requested);
          }
        }
      } catch (error) {
        console.error('Error requesting Samsung Health permissions:', error);
      }
    };

    handleInitialize();
  }, []);

  useEffect(() => {
    const handleFetchSteps = async () => {
      const todaysDate = new Date();

      const startDate = startOfWeek(todaysDate, { weekStartsOn: 1 });
      const endDate = endOfWeek(new Date(), { weekStartsOn: 1 });

      const startDateString = startDate.toISOString();
      const endDateString = endDate.toISOString();

      try {
        const stepData = await readStepData({
          timeRangeFilter: {
            operator: 'between',
            startTime: startDateString,
            endTime: endDateString,
            groupBy: 'daily',
          },
          ascendingOrder: true,
        });

        console.log({ stepData });
      } catch (error) {
        console.log('======', error);
      }
    };

    if (isPermissionsGranted) {
      handleFetchSteps();
    }
  }, [isPermissionsGranted]);

  useEffect(() => {
    const handleFetchSleep = async () => {
      const todaysDate = new Date();

      const startDate = startOfWeek(todaysDate, { weekStartsOn: 1 });
      const endDate = endOfWeek(new Date(), { weekStartsOn: 1 });

      const startDateString = startDate.toISOString();
      const endDateString = endDate.toISOString();

      try {
        const sleepData = await readSleepData({
          operator: 'between',
          startTime: startDateString,
          endTime: endDateString,
        });

        console.log({ sleepData });
      } catch (error) {
        console.log('======', error);
      }
    };

    if (isPermissionsGranted) {
      handleFetchSleep();
    }
  }, [isPermissionsGranted]);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
