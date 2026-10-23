PRAGMA foreign_keys = ON;

-- 设备身份（无账号密码）；side 标记左右：a=创建方，b=加入方
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  device_token TEXT NOT NULL UNIQUE,
  side TEXT NOT NULL CHECK (side IN ('a', 'b')),
  partner_pronoun TEXT NOT NULL DEFAULT 'ta' CHECK (partner_pronoun IN ('she', 'he', 'ta')),
  mipush_reg_id TEXT,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS relationships (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_a_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
  user_b_id INTEGER UNIQUE REFERENCES users(id),
  invite_code TEXT UNIQUE,
  invite_used_at TEXT,
  recovery_code_hash TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS reminders (
  id TEXT PRIMARY KEY,
  relationship_id INTEGER NOT NULL REFERENCES relationships(id) ON DELETE CASCADE,
  sender_id INTEGER NOT NULL REFERENCES users(id),
  receiver_id INTEGER NOT NULL REFERENCES users(id),
  type TEXT NOT NULL CHECK (type IN ('drink', 'stand', 'kegel', 'custom', 'poke')),
  title TEXT,
  content TEXT,
  state TEXT NOT NULL DEFAULT 'todo' CHECK (state IN ('todo', 'done')),
  read INTEGER NOT NULL DEFAULT 0,
  silenced INTEGER NOT NULL DEFAULT 0,
  reply TEXT,
  has_image INTEGER NOT NULL DEFAULT 0,
  image_url TEXT,
  amount_ml INTEGER,
  count INTEGER,
  client_nonce TEXT,
  created_at TEXT NOT NULL,
  responded_at TEXT,
  extra_data TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_reminders_nonce
  ON reminders(sender_id, client_nonce)
  WHERE client_nonce IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_reminders_rel_created
  ON reminders(relationship_id, created_at DESC);

CREATE TABLE IF NOT EXISTS drink_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id),
  amount_ml INTEGER NOT NULL,
  source TEXT NOT NULL CHECK (source IN ('manual', 'reminder')),
  reminder_id TEXT REFERENCES reminders(id),
  logged_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_drink_logs_reminder
  ON drink_logs(reminder_id)
  WHERE reminder_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS activity_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id),
  activity_type TEXT NOT NULL CHECK (activity_type IN ('stand', 'kegel', 'custom')),
  count INTEGER NOT NULL DEFAULT 1,
  source TEXT NOT NULL CHECK (source IN ('manual', 'reminder')),
  reminder_id TEXT REFERENCES reminders(id),
  logged_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_activity_logs_reminder
  ON activity_logs(reminder_id)
  WHERE reminder_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS habit_modules (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id),
  module_type TEXT NOT NULL CHECK (module_type IN ('drink', 'stand', 'kegel', 'custom')),
  title TEXT NOT NULL,
  daily_target INTEGER NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  enabled INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_habit_modules_user
  ON habit_modules(user_id, sort_order);
