import { getDb } from '../db/index.js';
import { calendarDate, nowIso } from '../lib/time.js';

export function listModules(userId) {
  return getDb()
    .prepare(
      `SELECT * FROM habit_modules
       WHERE user_id = ? AND enabled = 1
       ORDER BY sort_order ASC, id ASC`,
    )
    .all(userId)
    .map(mapModule);
}

function mapModule(row) {
  return {
    id: row.id,
    moduleType: row.module_type,
    title: row.title,
    dailyTarget: row.daily_target,
    sortOrder: row.sort_order,
    enabled: !!row.enabled,
  };
}

export function addModule(userId, { title, dailyTarget = 3 }) {
  const name = String(title || '').trim();
  if (!name) {
    const err = new Error('模块名不能为空');
    err.statusCode = 400;
    throw err;
  }
  if ([...name].length > 8) {
    const err = new Error('模块名最多 8 字');
    err.statusCode = 400;
    throw err;
  }
  const target = Number(dailyTarget) || 3;
  const db = getDb();
  const maxSort =
    db
      .prepare(
        `SELECT COALESCE(MAX(sort_order), -1) AS m FROM habit_modules WHERE user_id = ?`,
      )
      .get(userId).m + 1;
  const info = db
    .prepare(
      `INSERT INTO habit_modules (user_id, module_type, title, daily_target, sort_order, enabled)
       VALUES (?, 'custom', ?, ?, ?, 1)`,
    )
    .run(userId, name, target, maxSort);
  return mapModule(
    db.prepare(`SELECT * FROM habit_modules WHERE id = ?`).get(info.lastInsertRowid),
  );
}

export function deleteModule(userId, moduleId) {
  const db = getDb();
  const row = db
    .prepare(`SELECT * FROM habit_modules WHERE id = ? AND user_id = ?`)
    .get(moduleId, userId);
  if (!row) {
    const err = new Error('模块不存在');
    err.statusCode = 404;
    throw err;
  }
  if (row.module_type !== 'custom') {
    const err = new Error('默认模块不能删');
    err.statusCode = 400;
    throw err;
  }
  db.prepare(`DELETE FROM habit_modules WHERE id = ?`).run(moduleId);
  return { ok: true };
}

export function updateModule(userId, moduleId, { dailyTarget } = {}) {
  const db = getDb();
  const row = db
    .prepare(`SELECT * FROM habit_modules WHERE id = ? AND user_id = ?`)
    .get(moduleId, userId);
  if (!row) {
    const err = new Error('模块不存在');
    err.statusCode = 404;
    throw err;
  }
  const target = Number(dailyTarget);
  if (!Number.isFinite(target) || target <= 0) {
    const err = new Error('目标无效');
    err.statusCode = 400;
    throw err;
  }
  db.prepare(`UPDATE habit_modules SET daily_target = ? WHERE id = ?`).run(
    Math.round(target),
    moduleId,
  );
  return mapModule(
    db.prepare(`SELECT * FROM habit_modules WHERE id = ?`).get(moduleId),
  );
}

export function logDrink(userId, { amountMl, clientNonce } = {}) {
  const ml = Number(amountMl);
  if (!Number.isFinite(ml) || ml <= 0) {
    const err = new Error('水量无效');
    err.statusCode = 400;
    throw err;
  }
  const db = getDb();
  const loggedAt = nowIso();
  const info = db
    .prepare(
      `INSERT INTO drink_logs (user_id, amount_ml, source, reminder_id, logged_at)
       VALUES (?, ?, 'manual', NULL, ?)`,
    )
    .run(userId, ml, loggedAt);
  return {
    id: info.lastInsertRowid,
    amountMl: ml,
    source: 'manual',
    loggedAt,
  };
}

export function logActivity(userId, { activityType, count = 1, moduleId } = {}) {
  let type = activityType;
  if (moduleId) {
    const mod = getDb()
      .prepare(`SELECT * FROM habit_modules WHERE id = ? AND user_id = ?`)
      .get(moduleId, userId);
    if (!mod) {
      const err = new Error('模块不存在');
      err.statusCode = 404;
      throw err;
    }
    type = mod.module_type === 'custom' ? 'custom' : mod.module_type;
  }
  if (!['stand', 'kegel', 'custom'].includes(type)) {
    const err = new Error('活动类型无效');
    err.statusCode = 400;
    throw err;
  }
  const n = Math.max(1, Number(count) || 1);
  const loggedAt = nowIso();
  const info = getDb()
    .prepare(
      `INSERT INTO activity_logs
        (user_id, activity_type, count, source, reminder_id, logged_at)
       VALUES (?, ?, ?, 'manual', NULL, ?)`,
    )
    .run(userId, type, n, loggedAt);
  return {
    id: info.lastInsertRowid,
    activityType: type,
    count: n,
    source: 'manual',
    loggedAt,
    moduleId: moduleId ?? null,
  };
}

function timeLabelOf(iso) {
  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(new Date(iso));
  const hh = parts.find((p) => p.type === 'hour')?.value ?? '00';
  const mm = parts.find((p) => p.type === 'minute')?.value ?? '00';
  return `${hh}:${mm}`;
}

/** 日常页汇总：每个模块今天的值 + 最近几次时间 */
export function dailyOverview(userId) {
  const modules = listModules(userId);
  const today = calendarDate();
  const db = getDb();

  const drinks = db
    .prepare(`SELECT * FROM drink_logs WHERE user_id = ? ORDER BY logged_at DESC`)
    .all(userId)
    .filter((r) => calendarDate(r.logged_at) === today);

  const acts = db
    .prepare(`SELECT * FROM activity_logs WHERE user_id = ? ORDER BY logged_at DESC`)
    .all(userId)
    .filter((r) => calendarDate(r.logged_at) === today);

  return {
    date: today,
    modules: modules.map((m) => {
      if (m.moduleType === 'drink') {
        const total = drinks.reduce((s, r) => s + r.amount_ml, 0);
        return {
          ...m,
          todayValue: total,
          todayTimes: drinks.map((r) => ({
            at: timeLabelOf(r.logged_at),
            value: r.amount_ml,
            unit: 'ml',
          })),
        };
      }
      const type = m.moduleType === 'custom' ? 'custom' : m.moduleType;
      // custom modules：按 activity_type=custom 汇总（MVP 不区分哪个自定义模块）
      const matched = acts.filter((a) => a.activity_type === type);
      const total = matched.reduce((s, r) => s + r.count, 0);
      return {
        ...m,
        todayValue: total,
        todayTimes: matched.map((r) => ({
          at: timeLabelOf(r.logged_at),
          value: r.count,
          unit: '次',
        })),
      };
    }),
  };
}

export function getTrend(userId, moduleId, days = 7) {
  const db = getDb();
  const mod = db
    .prepare(`SELECT * FROM habit_modules WHERE id = ? AND user_id = ?`)
    .get(moduleId, userId);
  if (!mod) {
    const err = new Error('模块不存在');
    err.statusCode = 404;
    throw err;
  }

  const n = Math.min(Math.max(Number(days) || 7, 1), 30);
  const today = calendarDate();
  const daysList = [];
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(`${today}T12:00:00+08:00`);
    d.setDate(d.getDate() - i);
    daysList.push(calendarDate(d));
  }

  let series;
  let todayEntries;

  if (mod.module_type === 'drink') {
    const rows = db
      .prepare(`SELECT * FROM drink_logs WHERE user_id = ? ORDER BY logged_at ASC`)
      .all(userId);
    series = daysList.map((date) => {
      const value = rows
        .filter((r) => calendarDate(r.logged_at) === date)
        .reduce((s, r) => s + r.amount_ml, 0);
      return { date, value };
    });
    todayEntries = rows
      .filter((r) => calendarDate(r.logged_at) === today)
      .map((r) => ({
        at: timeLabelOf(r.logged_at),
        value: r.amount_ml,
        unit: 'ml',
      }));
  } else {
    const type = mod.module_type === 'custom' ? 'custom' : mod.module_type;
    const rows = db
      .prepare(
        `SELECT * FROM activity_logs WHERE user_id = ? AND activity_type = ? ORDER BY logged_at ASC`,
      )
      .all(userId, type);
    series = daysList.map((date) => {
      const value = rows
        .filter((r) => calendarDate(r.logged_at) === date)
        .reduce((s, r) => s + r.count, 0);
      return { date, value };
    });
    todayEntries = rows
      .filter((r) => calendarDate(r.logged_at) === today)
      .map((r, idx) => ({
        at: timeLabelOf(r.logged_at),
        value: r.count,
        unit: '次',
        index: idx + 1,
      }));
  }

  const values = series.map((s) => s.value);
  const avg = values.length
    ? Math.round((values.reduce((a, b) => a + b, 0) / values.length) * 10) / 10
    : 0;
  const hitDays = values.filter((v) => v >= mod.daily_target).length;
  const maxV = Math.max(...values, 0);
  const minV = Math.min(...values);
  const maxDay = series.find((s) => s.value === maxV)?.date;
  const minDay = series.find((s) => s.value === minV)?.date;
  const todayTotal = series[series.length - 1]?.value ?? 0;

  return {
    module: mapModule(mod),
    days: n,
    series,
    average: avg,
    hitDays,
    max: { date: maxDay, value: maxV },
    min: { date: minDay, value: minV },
    todayEntries,
    todayTotal,
    unit: mod.module_type === 'drink' ? 'ml' : '次',
  };
}
