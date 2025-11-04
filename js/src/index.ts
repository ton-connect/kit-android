import { setupPolyfills } from './setupPolyfills';

setupPolyfills();

// Import new layered bridge architecture
void import('./bridge/index');
