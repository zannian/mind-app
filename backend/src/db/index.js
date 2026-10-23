import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import Database from 'better-sqlite3';
import { config } from '../config.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let db;

export function getDb() {
  if (!db) {
    fs.mkdirSync(path.dirname(config.dbPath), { recursive: true });
    db = new Database(config.dbPath);
    db.pragma('journal_mode = WAL');
    db.pragma('foreign_keys = ON');
  }
  return db;
}

export function migrate() {
  const database = getDb();
  const schema = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
  database.exec(schema);
  // 已有库补列
  const userCols = database.prepare(`PRAGMA table_info(users)`).all().map((c) => c.name);
  if (!userCols.includes('mipush_reg_id')) {
    database.exec(`ALTER TABLE users ADD COLUMN mipush_reg_id TEXT`);
  }
  const remCols = database.prepare(`PRAGMA table_info(reminders)`).all().map((c) => c.name);
  if (!remCols.includes('image_url')) {
    database.exec(`ALTER TABLE reminders ADD COLUMN image_url TEXT`);
  }
  return database;
}
