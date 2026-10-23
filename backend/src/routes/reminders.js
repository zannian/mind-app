import {
  ackReminder,
  createReminder,
  getReminder,
  listReminders,
  listToday,
  markRead,
  respondReminder,
} from '../services/reminders.js';
import { getPartnerId, getRelationshipForUser } from '../services/users.js';
import { notifyReminderChange } from '../realtime/hub.js';

function handle(reply, fn) {
  try {
    return fn();
  } catch (e) {
    return reply.code(e.statusCode || 500).send({ error: e.message });
  }
}

function pairUserIds(userId) {
  const rel = getRelationshipForUser(userId);
  if (!rel) return [userId];
  const partner = getPartnerId(rel, userId);
  return partner ? [userId, partner] : [userId];
}

export async function reminderRoutes(fastify) {
  const auth = { preHandler: [fastify.authenticate] };

  fastify.get('/reminders', auth, async (request, reply) =>
    handle(reply, () =>
      listReminders(request.currentUser.id, {
        cursor: request.query.cursor,
        limit: request.query.limit,
      }),
    ),
  );

  fastify.get('/reminders/today', auth, async (request, reply) =>
    handle(reply, () => listToday(request.currentUser.id)),
  );

  fastify.get('/reminders/:id', auth, async (request, reply) =>
    handle(reply, () => getReminder(request.currentUser.id, request.params.id)),
  );

  fastify.post('/reminders', auth, async (request, reply) =>
    handle(reply, () => {
      const item = createReminder(request.currentUser.id, request.body || {});
      notifyReminderChange(pairUserIds(request.currentUser.id), 'created', item.id);
      return item;
    }),
  );

  fastify.post('/reminders/:id/read', auth, async (request, reply) =>
    handle(reply, () => {
      const item = markRead(request.currentUser.id, request.params.id);
      // 已读主要影响发送方「未读/仅读」
      notifyReminderChange(pairUserIds(request.currentUser.id), 'read', item.id);
      return item;
    }),
  );

  fastify.post('/reminders/:id/ack', auth, async (request, reply) =>
    handle(reply, () => {
      const item = ackReminder(request.currentUser.id, request.params.id);
      notifyReminderChange(pairUserIds(request.currentUser.id), 'acked', item.id);
      return item;
    }),
  );

  fastify.post('/reminders/:id/respond', auth, async (request, reply) =>
    handle(reply, () => {
      const item = respondReminder(
        request.currentUser.id,
        request.params.id,
        request.body || {},
      );
      notifyReminderChange(pairUserIds(request.currentUser.id), 'responded', item.id);
      return item;
    }),
  );
}
