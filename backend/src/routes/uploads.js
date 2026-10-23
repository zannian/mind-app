import { createReadStream, createWriteStream, existsSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { pipeline } from 'node:stream/promises';
import multipart from '@fastify/multipart';
import { nanoid } from 'nanoid';
import { config } from '../config.js';

const ALLOWED = new Map([
  ['image/jpeg', '.jpg'],
  ['image/jpg', '.jpg'],
  ['image/png', '.png'],
  ['image/webp', '.webp'],
  ['image/heic', '.heic'],
  ['image/heif', '.heif'],
]);

function ensureDir() {
  mkdirSync(config.uploadsDir, { recursive: true });
}

export async function uploadRoutes(app) {
  ensureDir();
  await app.register(multipart, {
    limits: { fileSize: 8 * 1024 * 1024, files: 1 },
  });

  app.post('/uploads', { preHandler: [app.authenticate] }, async (request, reply) => {
    const data = await request.file();
    if (!data) {
      return reply.code(400).send({ error: '请选择一张图片' });
    }
    const mime = (data.mimetype || '').toLowerCase();
    const ext = ALLOWED.get(mime);
    if (!ext) {
      return reply.code(400).send({ error: '只支持 jpg / png / webp' });
    }
    const name = `${nanoid(16)}${ext}`;
    const dest = path.join(config.uploadsDir, name);
    try {
      await pipeline(data.file, createWriteStream(dest));
    } catch (err) {
      request.log.error(err);
      return reply.code(500).send({ error: '上传失败' });
    }
    if (data.file.truncated) {
      return reply.code(413).send({ error: '图片太大（最多 8MB）' });
    }
    return { url: `/uploads/${name}` };
  });

  app.get('/uploads/:name', async (request, reply) => {
    const name = path.basename(request.params.name || '');
    if (!name || name !== request.params.name) {
      return reply.code(400).send({ error: '无效路径' });
    }
    const file = path.join(config.uploadsDir, name);
    if (!existsSync(file)) {
      return reply.code(404).send({ error: '图片不存在' });
    }
    const ext = path.extname(name).toLowerCase();
    const type =
      ext === '.png' ? 'image/png'
        : ext === '.webp' ? 'image/webp'
          : ext === '.heic' || ext === '.heif' ? 'image/heic'
            : 'image/jpeg';
    return reply.type(type).send(createReadStream(file));
  });
}
