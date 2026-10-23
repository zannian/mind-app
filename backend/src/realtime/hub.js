/** 按 userId 登记前台 WebSocket，用于即时提醒 */

const socketsByUser = new Map();

export function addSocket(userId, socket) {
  let set = socketsByUser.get(userId);
  if (!set) {
    set = new Set();
    socketsByUser.set(userId, set);
  }
  set.add(socket);
}

export function removeSocket(userId, socket) {
  const set = socketsByUser.get(userId);
  if (!set) return;
  set.delete(socket);
  if (set.size === 0) socketsByUser.delete(userId);
}

export function notifyUsers(userIds, payload) {
  const body = JSON.stringify(payload);
  const ids = [...new Set((userIds || []).filter(Boolean))];
  for (const id of ids) {
    const set = socketsByUser.get(id);
    if (!set) continue;
    for (const socket of set) {
      try {
        if (socket.readyState === 1) socket.send(body);
      } catch (_) {
        /* ignore broken socket */
      }
    }
  }
}

export function notifyReminderChange(userIds, action, reminderId) {
  notifyUsers(userIds, {
    type: 'reminder',
    action,
    reminderId: reminderId || null,
    at: new Date().toISOString(),
  });
}
