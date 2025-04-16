# rn-samsung-health-data-api

A React Native library for accessing Samsung Health data on **Android**. Supports step count, sleep, and heart rate data retrieval using Samsung Health SDK.

> ⚠️ **Android Only**: This package only supports Android. All methods are no-ops on iOS and will not crash your app, but will not return any data either.

---

## Installation

```sh
npm install rn-samsung-health-data-api
```

## Usage

```js
import {
  initializeHealthStore,
  checkHealthPermissionsGranted,
  requestHealthPermissions,
  readStepData,
} from 'rn-samsung-health-data-api';

// ...

const initData = await initializeHealthStore();
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
