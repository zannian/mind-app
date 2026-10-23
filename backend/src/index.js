import Fastify from 'fastify';
import cors from '@fastify/cors';
import websocket from '@fastify/websocket';
import { config } from './config.js';
import { migrate } from './db/index.js';
import { registerAuth } from './plugins/auth.js';
import { habitRoutes } from './routes/habits.js';
import { pairRoutes } from './routes/pair.js';
import { reminderRoutes } from './routes/reminders.js';
import { uploadRoutes } from './routes/uploads.js';
import { wsRoutes } from './routes/ws.js';

migrate();

const app = Fastify({ logger: true });

await app.register(cors, { origin: true });
await app.register(websocket);
await registerAuth(app);

app.get('/health', async () => ({ ok: true, timezone: config.timezone }));

await app.register(pairRoutes);
await app.register(reminderRoutes);
await app.register(habitRoutes);
await app.register(uploadRoutes);
await app.register(wsRoutes);

app.setErrorHandler((err, request, reply) => {
  request.log.error(err);
  reply.code(err.statusCode || 500).send({ error: err.message || '服务器错误' });
});

await app.listen({ port: config.port, host: config.host });
console.log(`惦 Mind API http://${config.host}:${config.port}`);
