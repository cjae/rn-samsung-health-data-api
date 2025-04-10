import {
  initializeHealthStore,
  checkHealthPermissionsGranted,
  requestHealthPermissions,
  readStepData,
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
        const granted = await checkHealthPermissionsGranted(['STEPS']);
        if (granted.allGranted) {
          setIsPermissionsGrant(true);
        } else {
          const requested = await requestHealthPermissions(['STEPS']);
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

      const stepData = await readStepData({
        timeRangeFilter: {
          operator: 'between',
          startTime: startDateString,
          endTime: endDateString,
          timeGroup: 'daily',
        },
        ascendingOrder: true,
      });

      console.log({ stepData });
    };

    handleFetchSteps();
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
