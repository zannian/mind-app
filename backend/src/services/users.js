import { getDb } from '../db/index.js';
import { nowIso } from '../lib/time.js';

const DEFAULT_MODULES = [
  { module_type: 'drink', title: '喝水', daily_target: 2000, sort_order: 0 },
  { module_type: 'stand', title: '起身站立', daily_target: 6, sort_order: 1 },
  { module_type: 'kegel', title: '提肛', daily_target: 3, sort_order: 2 },
];

export function findUserById(id) {
  return getDb().prepare('SELECT * FROM users WHERE id = ?').get(id);
}

export function findUserByToken(token) {
  if (!token) return null;
  return getDb().prepare('SELECT * FROM users WHERE device_token = ?').get(token);
}

export function seedHabitModules(userId) {
  const db = getDb();
  const insertMod = db.prepare(
    `INSERT INTO habit_modules (user_id, module_type, title, daily_target, sort_order, enabled)
     VALUES (?, ?, ?, ?, ?, 1)`,
  );
  for (const m of DEFAULT_MODULES) {
    insertMod.run(userId, m.module_type, m.title, m.daily_target, m.sort_order);
  }
}

export function createDeviceUser({ deviceToken, side, partnerPronoun = 'ta' }) {
  const db = getDb();
  const info = db
    .prepare(
      `INSERT INTO users (device_token, side, partner_pronoun, created_at)
       VALUES (?, ?, ?, ?)`,
    )
    .run(deviceToken, side, partnerPronoun, nowIso());
  const user = findUserById(info.lastInsertRowid);
  seedHabitModules(user.id);
  return user;
}

export function rotateDeviceToken(userId, newToken) {
  getDb()
    .prepare(`UPDATE users SET device_token = ? WHERE id = ?`)
    .run(newToken, userId);
  return findUserById(userId);
}

export function publicUser(user) {
  if (!user) return null;
  return {
    id: user.id,
    side: user.side,
    partnerPronoun: user.partner_pronoun,
    createdAt: user.created_at,
  };
}

/** 更新本机称呼等设置（无账号密码） */
export function updateUserSettings(userId, { partnerPronoun } = {}) {
  const allowed = new Set(['she', 'he', 'ta']);
  if (partnerPronoun != null) {
    const code = String(partnerPronoun).trim().toLowerCase();
    if (!allowed.has(code)) {
      const err = new Error('称呼只能是 她 / 他 / TA');
      err.statusCode = 400;
      throw err;
    }
    getDb()
      .prepare(`UPDATE users SET partner_pronoun = ? WHERE id = ?`)
      .run(code, userId);
  }
  return findUserById(userId);
}


export function getRelationshipForUser(userId) {
  return getDb()
    .prepare(
      `SELECT * FROM relationships
       WHERE user_a_id = ? OR user_b_id = ?`,
    )
    .get(userId, userId);
}

export function getPartnerId(relationship, userId) {
  if (!relationship) return null;
  if (relationship.user_a_id === userId) return relationship.user_b_id ?? null;
  if (relationship.user_b_id === userId) return relationship.user_a_id;
  return null;
}

export function isPaired(relationship) {
  return !!(relationship && relationship.user_b_id);
}
