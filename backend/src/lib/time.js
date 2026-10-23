import { config } from '../config.js';

/** Instant as ISO UTC string for storage */
export function nowIso() {
  return new Date().toISOString();
}

/** Calendar date YYYY-MM-DD in app timezone */
export function calendarDate(isoOrDate = new Date()) {
  const d = typeof isoOrDate === 'string' ? new Date(isoOrDate) : isoOrDate;
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: config.timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(d);
}

export function isSameCalendarDay(isoA, isoB = new Date()) {
  return calendarDate(isoA) === calendarDate(isoB);
}

export function relativeTimeLabel(createdAtIso) {
  const created = new Date(createdAtIso).getTime();
  const diffMin = Math.max(0, Math.round((Date.now() - created) / 60000));
  if (diffMin < 1) return '刚刚';
  if (diffMin < 60) return `${diffMin} 分钟前`;
  const hours = Math.floor(diffMin / 60);
  if (hours < 12 && isSameCalendarDay(createdAtIso)) {
    if (hours < 1) return `${diffMin} 分钟前`;
    return `${hours} 小时前`;
  }
  const parts = new Intl.DateTimeFormat('zh-CN', {
    timeZone: config.timezone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(new Date(createdAtIso));
  const hh = parts.find((p) => p.type === 'hour')?.value ?? '00';
  const mm = parts.find((p) => p.type === 'minute')?.value ?? '00';
  if (isSameCalendarDay(createdAtIso)) return `${hh}:${mm}`;
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  // Compare calendar dates in TZ via string trick
  const createdDay = calendarDate(createdAtIso);
  const today = calendarDate();
  const yDate = new Date(`${today}T12:00:00+08:00`);
  yDate.setDate(yDate.getDate() - 1);
  const yStr = calendarDate(yDate);
  if (createdDay === yStr) return `昨天 ${hh}:${mm}`;
  return `${createdDay.slice(5).replace('-', '/')} ${hh}:${mm}`;
}
