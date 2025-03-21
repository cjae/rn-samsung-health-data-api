import {
  initializeHealthStore,
  checkHealthPermissionsGranted,
} from 'rn-samsung-health-data-api';
import { Text, View, StyleSheet } from 'react-native';
import { useState, useEffect } from 'react';

export default function App() {
  const [result, setResult] = useState<string | undefined>();

  useEffect(() => {
    const handleInitialize = async () => {
      const initData = await initializeHealthStore();
      if (!initData) {
        setResult('Error');
      }

      setResult('Init');
      console.log('==== HERE');

      try {
        const granted = await checkHealthPermissionsGranted(['STEPS']);
        console.log('Permissions Result:', granted);
      } catch (error) {
        console.error('Error requesting Samsung Health permissions:', error);
      }
    };

    handleInitialize();
  }, []);

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
