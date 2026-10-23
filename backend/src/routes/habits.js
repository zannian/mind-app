import {
  addModule,
  dailyOverview,
  deleteModule,
  getTrend,
  listModules,
  logActivity,
  logDrink,
  updateModule,
} from '../services/habits.js';

function handle(reply, fn) {
  try {
    return fn();
  } catch (e) {
    return reply.code(e.statusCode || 500).send({ error: e.message });
  }
}

export async function habitRoutes(fastify) {
  const auth = { preHandler: [fastify.authenticate] };

  fastify.get('/modules', auth, async (request, reply) =>
    handle(reply, () => ({ items: listModules(request.currentUser.id) })),
  );

  fastify.get('/daily', auth, async (request, reply) =>
    handle(reply, () => dailyOverview(request.currentUser.id)),
  );

  fastify.post('/modules', auth, async (request, reply) =>
    handle(reply, () => addModule(request.currentUser.id, request.body || {})),
  );

  fastify.delete('/modules/:id', auth, async (request, reply) =>
    handle(reply, () =>
      deleteModule(request.currentUser.id, Number(request.params.id)),
    ),
  );

  fastify.patch('/modules/:id', auth, async (request, reply) =>
    handle(reply, () =>
      updateModule(
        request.currentUser.id,
        Number(request.params.id),
        request.body || {},
      ),
    ),
  );

  fastify.post('/logs/drink', auth, async (request, reply) =>
    handle(reply, () => logDrink(request.currentUser.id, request.body || {})),
  );

  fastify.post('/logs/activity', auth, async (request, reply) =>
    handle(reply, () => logActivity(request.currentUser.id, request.body || {})),
  );

  fastify.get('/trends', auth, async (request, reply) =>
    handle(reply, () =>
      getTrend(
        request.currentUser.id,
        Number(request.query.module),
        Number(request.query.days) || 7,
      ),
    ),
  );
}
