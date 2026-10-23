import { nanoid } from 'nanoid';
import { getDb } from '../db/index.js';
import { isSameCalendarDay, nowIso, relativeTimeLabel } from '../lib/time.js';
import {
  getPartnerId,
  getRelationshipForUser,
  isPaired,
} from './users.js';

const TYPES = new Set(['drink', 'stand', 'kegel', 'custom', 'poke']);

function requirePair(userId) {
  const rel = getRelationshipForUser(userId);
  if (!rel || !isPaired(rel)) {
    const err = new Error('请先完成配对');
    err.statusCode = 400;
    throw err;
  }
  return rel;
}

function mapReminder(row, viewerId) {
  const dir = row.sender_id === viewerId ? 'out' : 'in';
  const isToday = isSameCalendarDay(row.created_at);
  const read = !!row.read;
  const done = row.state === 'done';

  // List-facing status words: only for my outgoing, today, not done
  let listStatus = null;
  if (dir === 'out' && isToday && !done) {
    listStatus = read ? '仅读' : '未读';
  }

  let detailStatus;
  if (done) detailStatus = '已回应';
  else if (read) detailStatus = '仅读';
  else detailStatus = '未读';

  return {
    id: row.id,
    relationshipId: row.relationship_id,
    senderId: row.sender_id,
    receiverId: row.receiver_id,
    type: row.type,
    dir,
    title: row.title,
    content: row.content,
    state: row.state,
    read,
    silenced: !!row.silenced,
    reply: row.reply,
    hasImage: !!row.has_image || !!row.image_url,
    imageUrl: row.image_url || null,
    amountMl: row.amount_ml,
    count: row.count,
    createdAt: row.created_at,
    respondedAt: row.responded_at,
    timeLabel: relativeTimeLabel(row.created_at),
    isToday,
    listStatus,
    detailStatus,
    foldCorner: done || !!row.has_image || !!row.reply,
  };
}

export function listReminders(userId, { cursor, limit = 20 } = {}) {
  const rel = requirePair(userId);
  const db = getDb();
  const lim = Math.min(Math.max(Number(limit) || 20, 1), 50);

  let rows;
  if (cursor) {
    rows = db
      .prepare(
        `SELECT * FROM reminders
         WHERE relationship_id = ?
           AND (sender_id = ? OR receiver_id = ?)
           AND created_at < ?
         ORDER BY created_at DESC
         LIMIT ?`,
      )
      .all(rel.id, userId, userId, cursor, lim);
  } else {
    rows = db
      .prepare(
        `SELECT * FROM reminders
         WHERE relationship_id = ?
           AND (sender_id = ? OR receiver_id = ?)
         ORDER BY created_at DESC
         LIMIT ?`,
      )
      .all(rel.id, userId, userId, lim);
  }

  const items = rows.map((r) => mapReminder(r, userId));
  const nextCursor = rows.length === lim ? rows[rows.length - 1].created_at : null;
  return { items, nextCursor };
}

export function listToday(userId) {
  const rel = requirePair(userId);
  const db = getDb();
  const rows = db
    .prepare(
      `SELECT * FROM reminders
       WHERE relationship_id = ?
         AND (sender_id = ? OR receiver_id = ?)
       ORDER BY created_at DESC`,
    )
    .all(rel.id, userId, userId)
    .filter((r) => isSameCalendarDay(r.created_at));

  const fromPartner = [];
  const toPartner = [];
  let careCount = 0;
  let pokeCount = 0;

  for (const r of rows) {
    const m = mapReminder(r, userId);
    if (r.type === 'poke') pokeCount += 1;
    else careCount += 1;
    if (m.dir === 'in') fromPartner.push(m);
    else toPartner.push(m);
  }

  return { fromPartner, toPartner, careCount, pokeCount };
}

export function createReminder(userId, body) {
  const rel = requirePair(userId);
  const partnerId = getPartnerId(rel, userId);
  const type = body.type;
  if (!TYPES.has(type)) {
    const err = new Error('未知提醒类型');
    err.statusCode = 400;
    throw err;
  }

  if (type === 'custom') {
    const title = (body.title || '').trim();
    if (!title) {
      const err = new Error('惦记标题不能为空');
      err.statusCode = 400;
      throw err;
    }
    if ([...title].length > 15) {
      const err = new Error('标题最多 15 字');
      err.statusCode = 400;
      throw err;
    }
  }

  const db = getDb();
  if (body.clientNonce) {
    const existing = db
      .prepare(
        `SELECT * FROM reminders WHERE sender_id = ? AND client_nonce = ?`,
      )
      .get(userId, body.clientNonce);
    if (existing) return mapReminder(existing, userId);
  }

  const id = nanoid();
  const createdAt = nowIso();
  // poke is instantly "done" visually as a line; still stored
  const state = type === 'poke' ? 'done' : 'todo';

  db.prepare(
    `INSERT INTO reminders (
      id, relationship_id, sender_id, receiver_id, type, title, content,
      state, read, silenced, client_nonce, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?)`,
  ).run(
    id,
    rel.id,
    userId,
    partnerId,
    type,
    type === 'custom' ? String(body.title).trim() : null,
    body.content ? String(body.content).trim() : null,
    state,
    body.clientNonce || null,
    createdAt,
  );

  const row = db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(id);
  return mapReminder(row, userId);
}

export function markRead(userId, reminderId) {
  const db = getDb();
  const row = db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId);
  if (!row) {
    const err = new Error('提醒不存在');
    err.statusCode = 404;
    throw err;
  }
  if (row.receiver_id !== userId) {
    const err = new Error('只有接收方可以标记已读');
    err.statusCode = 403;
    throw err;
  }
  if (!row.read) {
    db.prepare(`UPDATE reminders SET read = 1 WHERE id = ?`).run(reminderId);
  }
  return mapReminder(
    db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId),
    userId,
  );
}

export function ackReminder(userId, reminderId) {
  const db = getDb();
  const row = db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId);
  if (!row) {
    const err = new Error('提醒不存在');
    err.statusCode = 404;
    throw err;
  }
  if (row.receiver_id !== userId) {
    const err = new Error('只有接收方可以「知道了」');
    err.statusCode = 403;
    throw err;
  }
  db.prepare(
    `UPDATE reminders SET read = 1, silenced = 1 WHERE id = ?`,
  ).run(reminderId);
  return mapReminder(
    db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId),
    userId,
  );
}

export function respondReminder(userId, reminderId, body) {
  const db = getDb();
  const row = db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId);
  if (!row) {
    const err = new Error('提醒不存在');
    err.statusCode = 404;
    throw err;
  }
  if (row.receiver_id !== userId) {
    const err = new Error('只有接收方可以回应');
    err.statusCode = 403;
    throw err;
  }
  if (row.type === 'poke') {
    const err = new Error('戳一下无需回应');
    err.statusCode = 400;
    throw err;
  }
  if (row.state === 'done') {
    return mapReminder(row, userId);
  }

  const respondedAt = nowIso();
  const reply = body.reply != null ? String(body.reply).trim() : null;
  let imageUrl = null;
  if (body.imageUrl != null && String(body.imageUrl).trim()) {
    const raw = String(body.imageUrl).trim();
    // 只接受本服务上传路径，防外链
    if (!/^\/uploads\/[A-Za-z0-9._-]+$/.test(raw)) {
      const err = new Error('图片地址无效');
      err.statusCode = 400;
      throw err;
    }
    imageUrl = raw;
  }
  const hasImage = imageUrl || body.hasImage ? 1 : 0;
  let amountMl = null;
  let count = null;

  if (row.type === 'drink') {
    amountMl = Number(body.amountMl);
    if (!Number.isFinite(amountMl) || amountMl <= 0) {
      const err = new Error('请选择喝水量');
      err.statusCode = 400;
      throw err;
    }
  } else if (row.type === 'stand' || row.type === 'kegel') {
    count = Number(body.count ?? 1);
    if (!Number.isFinite(count) || count <= 0) count = 1;
  }

  const tx = db.transaction(() => {
    db.prepare(
      `UPDATE reminders
       SET state = 'done', read = 1, reply = ?, has_image = ?, image_url = ?,
           amount_ml = ?, count = ?, responded_at = ?
       WHERE id = ?`,
    ).run(reply, hasImage, imageUrl, amountMl, count, respondedAt, reminderId);

    if (row.type === 'drink') {
      db.prepare(
        `INSERT INTO drink_logs (user_id, amount_ml, source, reminder_id, logged_at)
         VALUES (?, ?, 'reminder', ?, ?)`,
      ).run(userId, amountMl, reminderId, respondedAt);
    } else if (row.type === 'stand' || row.type === 'kegel') {
      db.prepare(
        `INSERT INTO activity_logs
          (user_id, activity_type, count, source, reminder_id, logged_at)
         VALUES (?, ?, ?, 'reminder', ?, ?)`,
      ).run(userId, row.type, count, reminderId, respondedAt);
    }
  });

  try {
    tx();
  } catch (e) {
    // unique reminder_id → already logged; treat as success
    if (!String(e.message).includes('UNIQUE')) throw e;
  }

  return mapReminder(
    db.prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId),
    userId,
  );
}

export function getReminder(userId, reminderId) {
  const row = getDb().prepare(`SELECT * FROM reminders WHERE id = ?`).get(reminderId);
  if (!row) {
    const err = new Error('提醒不存在');
    err.statusCode = 404;
    throw err;
  }
  if (row.sender_id !== userId && row.receiver_id !== userId) {
    const err = new Error('无权查看');
    err.statusCode = 403;
    throw err;
  }
  return mapReminder(row, userId);
}
