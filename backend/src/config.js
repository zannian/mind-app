import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..');

export const config = {
  port: Number(process.env.PORT || 8787),
  host: process.env.HOST || '0.0.0.0',
  jwtSecret: process.env.JWT_SECRET || 'mind-dev-secret-change-me',
  timezone: 'Asia/Shanghai',
  dbPath: process.env.DB_PATH || path.join(root, 'data', 'mind.db'),
  uploadsDir: process.env.UPLOADS_DIR || path.join(root, 'uploads'),
};
