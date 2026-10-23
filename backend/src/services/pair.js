import bcrypt from 'bcryptjs';
import { customAlphabet, nanoid } from 'nanoid';
import { getDb } from '../db/index.js';
import { nowIso } from '../lib/time.js';
import {
  createDeviceUser,
  findUserById,
  getPartnerId,
  getRelationshipForUser,
  isPaired,
  publicUser,
  rotateDeviceToken,
} from './users.js';

const genInvite = customAlphabet('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 6);
const PRONOUNS = new Set(['she', 'he', 'ta']);

function newDeviceToken() {
  return nanoid(40);
}

function newRecoveryCode() {
  // 分段好念：XXXX-XXXX-XXXX-XXXX
  const raw = customAlphabet('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 16)();
  return raw.match(/.{1,4}/g).join('-');
}

function normalizeRecovery(code) {
  return String(code || '')
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '');
}

function sessionPayload(user, relationship, { inviteCode, recoveryCode } = {}) {
  const paired = isPaired(relationship);
  return {
    token: user.device_token,
    user: publicUser(user),
    relationshipId: relationship.id,
    paired,
    waitingForPartner: user.side === 'a' && !paired,
    inviteCode: !paired && user.side === 'a' ? relationship.invite_code : null,
    recoveryCode: recoveryCode || null,
    partner: paired ? publicUser(findUserById(getPartnerId(relationship, user.id))) : null,
  };
}

export function createPair({ partnerPronoun = 'ta' } = {}) {
  if (!PRONOUNS.has(partnerPronoun)) {
    const err = new Error('称呼只能是 she / he / ta');
    err.statusCode = 400;
    throw err;
  }

  const db = getDb();
  const deviceToken = newDeviceToken();
  const recoveryCode = newRecoveryCode();
  const recoveryHash = bcrypt.hashSync(normalizeRecovery(recoveryCode), 10);

  let inviteCode;
  for (let i = 0; i < 8; i++) {
    inviteCode = genInvite();
    try {
      const tx = db.transaction(() => {
        const user = createDeviceUser({
          deviceToken,
          side: 'a',
          partnerPronoun,
        });
        const rel = db
          .prepare(
            `INSERT INTO relationships
              (user_a_id, user_b_id, invite_code, invite_used_at, recovery_code_hash, created_at)
             VALUES (?, NULL, ?, NULL, ?, ?)`,
          )
          .run(user.id, inviteCode, recoveryHash, nowIso());
        return {
          user,
          relationship: db
            .prepare(`SELECT * FROM relationships WHERE id = ?`)
            .get(rel.lastInsertRowid),
        };
      });
      const { user, relationship } = tx();
      return sessionPayload(user, relationship, { inviteCode, recoveryCode });
    } catch (e) {
      if (String(e.message).includes('UNIQUE')) {
        inviteCode = null;
        continue;
      }
      throw e;
    }
  }
  const err = new Error('生成邀请码失败');
  err.statusCode = 500;
  throw err;
}

export function joinPair({ code, partnerPronoun = 'ta' } = {}) {
  if (!PRONOUNS.has(partnerPronoun)) {
    const err = new Error('称呼只能是 she / he / ta');
    err.statusCode = 400;
    throw err;
  }

  const db = getDb();
  const inviteCode = String(code || '')
    .trim()
    .toUpperCase();
  const rel = db
    .prepare(`SELECT * FROM relationships WHERE invite_code = ?`)
    .get(inviteCode);

  if (!rel) {
    const err = new Error('邀请码无效');
    err.statusCode = 404;
    throw err;
  }
  if (rel.user_b_id || rel.invite_used_at) {
    const err = new Error('邀请码已使用（一次有效）');
    err.statusCode = 400;
    throw err;
  }

  const deviceToken = newDeviceToken();
  const tx = db.transaction(() => {
    const user = createDeviceUser({
      deviceToken,
      side: 'b',
      partnerPronoun,
    });
    db.prepare(
      `UPDATE relationships
       SET user_b_id = ?, invite_used_at = ?, invite_code = NULL
       WHERE id = ?`,
    ).run(user.id, nowIso(), rel.id);
    return {
      user,
      relationship: db.prepare(`SELECT * FROM relationships WHERE id = ?`).get(rel.id),
    };
  });

  const { user, relationship } = tx();
  return sessionPayload(user, relationship);
}

export function recoverPair({ recoveryCode, side, partnerPronoun } = {}) {
  if (side !== 'a' && side !== 'b') {
    const err = new Error('请选择身份：创建方(a) 或 加入方(b)');
    err.statusCode = 400;
    throw err;
  }

  const normalized = normalizeRecovery(recoveryCode);
  if (normalized.length < 12) {
    const err = new Error('恢复码无效');
    err.statusCode = 400;
    throw err;
  }

  const db = getDb();
  const rows = db.prepare(`SELECT * FROM relationships`).all();
  const rel = rows.find((r) => bcrypt.compareSync(normalized, r.recovery_code_hash));
  if (!rel) {
    const err = new Error('恢复码无效');
    err.statusCode = 404;
    throw err;
  }

  const userId = side === 'a' ? rel.user_a_id : rel.user_b_id;
  if (!userId) {
    const err = new Error('这一侧还没有加入过，无法恢复');
    err.statusCode = 400;
    throw err;
  }

  const newToken = newDeviceToken();
  let user = rotateDeviceToken(userId, newToken);
  if (partnerPronoun && PRONOUNS.has(partnerPronoun)) {
    db.prepare(`UPDATE users SET partner_pronoun = ? WHERE id = ?`).run(
      partnerPronoun,
      userId,
    );
    user = findUserById(userId);
  }

  return sessionPayload(user, rel);
}

export function unbindPair(userId) {
  const rel = getRelationshipForUser(userId);
  if (!rel) {
    const err = new Error('当前没有绑定关系');
    err.statusCode = 400;
    throw err;
  }

  const db = getDb();
  const ids = [rel.user_a_id, rel.user_b_id].filter(Boolean);

  const tx = db.transaction(() => {
    db.prepare(
      `DELETE FROM drink_logs WHERE user_id IN (${ids.map(() => '?').join(',')})`,
    ).run(...ids);
    db.prepare(
      `DELETE FROM activity_logs WHERE user_id IN (${ids.map(() => '?').join(',')})`,
    ).run(...ids);
    db.prepare(
      `DELETE FROM habit_modules WHERE user_id IN (${ids.map(() => '?').join(',')})`,
    ).run(...ids);
    db.prepare(`DELETE FROM reminders WHERE relationship_id = ?`).run(rel.id);
    db.prepare(`DELETE FROM relationships WHERE id = ?`).run(rel.id);
    db.prepare(
      `DELETE FROM users WHERE id IN (${ids.map(() => '?').join(',')})`,
    ).run(...ids);
  });
  tx();
  return { ok: true };
}

export function getMe(user) {
  const relationship = getRelationshipForUser(user.id);
  if (!relationship) {
    const err = new Error('关系不存在，请重新配对');
    err.statusCode = 401;
    throw err;
  }
  // token 已在 user 行里，但响应里不必每次回传；客户端已有
  const payload = sessionPayload(user, relationship);
  delete payload.token;
  return payload;
}
