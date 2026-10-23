import { findUserByToken } from '../services/users.js';

export async function registerAuth(fastify) {
  fastify.decorate('authenticate', async (request, reply) => {
    const header = request.headers.authorization || '';
    const token = header.startsWith('Bearer ')
      ? header.slice(7).trim()
      : header.trim();
    if (!token) {
      return reply.code(401).send({ error: '未配对' });
    }
    const user = findUserByToken(token);
    if (!user) {
      return reply.code(401).send({ error: '凭证无效，请重新配对或恢复' });
    }
    request.currentUser = user;
  });
}
