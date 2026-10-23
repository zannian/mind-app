import { migrate } from './index.js';
import { config } from '../config.js';

migrate();
console.log(`SQLite ready: ${config.dbPath}`);
