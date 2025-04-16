# rn-samsung-health-data-api

Integration with Samsung health data api

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
