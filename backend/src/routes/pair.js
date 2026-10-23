import {
  createPair,
  getMe,
  joinPair,
  recoverPair,
  unbindPair,
} from '../services/pair.js';
import { updateUserSettings } from '../services/users.js';

function handle(reply, fn) {
  try {
    return fn();
  } catch (e) {
    return reply.code(e.statusCode || 500).send({ error: e.message });
  }
}

export async function pairRoutes(fastify) {
  /** 创建一对：返回设备令牌 + 一次性邀请码 + 恢复码（只此一次展示） */
  fastify.post('/pair/create', async (request, reply) =>
    handle(reply, () => createPair(request.body || {})),
  );

  /** 用邀请码加入 */
  fastify.post('/pair/join', async (request, reply) =>
    handle(reply, () => joinPair(request.body || {})),
  );

  /** 换机：恢复码 + 身份 a/b */
  fastify.post('/pair/recover', async (request, reply) =>
    handle(reply, () => recoverPair(request.body || {})),
  );

  fastify.get(
    '/me',
    { preHandler: [fastify.authenticate] },
    async (request, reply) => handle(reply, () => getMe(request.currentUser)),
  );

  fastify.patch(
    '/me/settings',
    { preHandler: [fastify.authenticate] },
    async (request, reply) =>
      handle(reply, () => {
        const user = updateUserSettings(request.currentUser.id, request.body || {});
        return getMe(user);
      }),
  );

  fastify.post(
    '/pair/unbind',
    { preHandler: [fastify.authenticate] },
    async (request, reply) =>
      handle(reply, () => unbindPair(request.currentUser.id)),
  );
}
