import { findUserByToken } from '../services/users.js';
import { addSocket, removeSocket } from '../realtime/hub.js';

/**
 * GET /ws?token=<device_token>
 * 鉴权后双向保活；业务变更由 hub 推 JSON。
 */
export async function wsRoutes(fastify) {
  fastify.get('/ws', { websocket: true }, (socket, request) => {
    const raw =
      request.query?.token ||
      (request.headers.authorization || '').replace(/^Bearer\s+/i, '').trim();
    const user = findUserByToken(String(raw || '').trim());
    if (!user) {
      socket.close(4401, 'unauthorized');
      return;
    }

    addSocket(user.id, socket);
    socket.send(
      JSON.stringify({ type: 'hello', userId: user.id, at: new Date().toISOString() }),
    );

    const ping = setInterval(() => {
      try {
        if (socket.readyState === 1) socket.ping();
      } catch (_) {
        clearInterval(ping);
      }
    }, 25000);

    socket.on('message', (buf) => {
      const text = String(buf || '');
      if (text === 'ping') {
        try {
          socket.send(JSON.stringify({ type: 'pong' }));
        } catch (_) {}
      }
    });

    const cleanup = () => {
      clearInterval(ping);
      removeSocket(user.id, socket);
    };
    socket.on('close', cleanup);
    socket.on('error', cleanup);
  });
}
