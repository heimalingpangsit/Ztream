/**
 * ZarStream — Nobar Realtime Worker
 * =================================================================
 * Worker ini yang bikin dua hal jalan:
 *
 *   1. CHAT waktu nobar  -> WebSocket (Durable Object) + fallback polling HTTP
 *   2. UNDANGAN nobar    -> daftar undangan per user, dibaca tab "Join Nobar"
 *
 * Kenapa perlu worker terpisah? App Android nyoba nyambung ke
 * wss://zarstream-nobar.<akunmu>.workers.dev/websocket, dan sebelum ini
 * worker itu belum ada — jadi chat cuma nongol di HP pengirim, sementara
 * peserta lain gak pernah nerima apa-apa.
 *
 * -----------------------------------------------------------------
 * CARA DEPLOY (sekali saja, ~3 menit)
 * -----------------------------------------------------------------
 *   1) npm install -g wrangler        (kalau belum ada)
 *   2) wrangler login
 *   3) taruh file ini + nobar-wrangler.toml di satu folder, lalu:
 *        wrangler deploy -c nobar-wrangler.toml
 *   4) pastikan nama worker-nya "zarstream-nobar" supaya domainnya jadi
 *      https://zarstream-nobar.<subdomain-akunmu>.workers.dev
 *
 *      Kalau subdomain akunmu BUKAN "pokaycore", ubah satu baris di app:
 *        data/remote/NobarSocketClient.kt -> NobarRealtimeConfig.BASE_HOST
 *
 * Gak butuh KV, gak butuh D1. Undangan disimpan di Durable Object dan
 * otomatis kadaluarsa sendiri setelah 6 jam.
 * -----------------------------------------------------------------
 * ENDPOINT
 *   GET  /websocket?roomId=&username=            -> realtime chat & sync
 *   GET  /api/nobar/room/:roomId/messages?since= -> polling chat (fallback)
 *   POST /api/nobar/room/:roomId/messages        -> kirim chat via HTTP
 *   GET  /api/nobar/invites?username=            -> undangan masuk
 *   POST /api/nobar/invites                      -> catat undangan
 *   DELETE /api/nobar/invites/:id                -> hapus undangan
 *   GET  /health                                 -> cek worker hidup
 */

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

const INVITE_TTL_MS = 6 * 60 * 60 * 1000; // undangan hangus setelah 6 jam
const MESSAGE_KEEP = 200;                  // riwayat chat per room

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json', ...CORS },
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS });
    }

    if (path === '/health') {
      return json({ ok: true, service: 'zarstream-nobar', now: Date.now() });
    }

    // ---------- WebSocket chat & sync ----------
    if (path === '/websocket') {
      const roomId = url.searchParams.get('roomId');
      if (!roomId) return json({ error: 'roomId wajib' }, 400);
      return roomStub(env, roomId).fetch(request);
    }

    // ---------- Chat lewat HTTP (fallback kalau WebSocket gagal) ----------
    const roomMatch = path.match(/^\/api\/nobar\/room\/([^/]+)\/messages$/);
    if (roomMatch) {
      const roomId = decodeURIComponent(roomMatch[1]);
      return roomStub(env, roomId).fetch(
        new Request(`https://room/messages${url.search}`, request),
      );
    }

    // ---------- Undangan ----------
    if (path === '/api/nobar/invites') {
      const stub = inviteStub(env);
      if (request.method === 'GET') {
        const username = (url.searchParams.get('username') || '').trim();
        if (!username) return json({ invites: [] });
        return stub.fetch(
          new Request(`https://invites/list?username=${encodeURIComponent(username)}`),
        );
      }
      if (request.method === 'POST') {
        return stub.fetch(new Request('https://invites/add', request));
      }
    }

    const dismissMatch = path.match(/^\/api\/nobar\/invites\/([^/]+)$/);
    if (dismissMatch && request.method === 'DELETE') {
      return inviteStub(env).fetch(
        new Request(`https://invites/remove?id=${encodeURIComponent(dismissMatch[1])}`, {
          method: 'DELETE',
        }),
      );
    }

    return json({ error: 'not found' }, 404);
  },
};

function roomStub(env, roomId) {
  return env.NOBAR_ROOM.get(env.NOBAR_ROOM.idFromName('room:' + roomId));
}

function inviteStub(env) {
  return env.NOBAR_INVITES.get(env.NOBAR_INVITES.idFromName('invites:v1'));
}

/* ==================================================================
 * NobarRoom — satu instance per kode room
 * ================================================================== */
export class NobarRoom {
  constructor(state) {
    this.state = state;
    this.sessions = new Map(); // WebSocket -> username
    this.messages = [];        // riwayat chat + system, dipakai polling
  }

  async fetch(request) {
    const url = new URL(request.url);

    if (url.pathname === '/websocket' || request.headers.get('Upgrade') === 'websocket') {
      const username = (url.searchParams.get('username') || 'Tamu').slice(0, 40);
      const pair = new WebSocketPair();
      this.accept(pair[1], username);
      return new Response(null, { status: 101, webSocket: pair[0] });
    }

    if (url.pathname === '/messages') {
      if (request.method === 'GET') {
        const since = Number(url.searchParams.get('since') || 0);
        const username = (url.searchParams.get('username') || '').slice(0, 40);
        if (username) this.touchPoller(username);
        return json({
          messages: this.messages.filter((m) => m.at > since),
          participants: this.participantCount(),
          now: Date.now(),
        });
      }

      if (request.method === 'POST') {
        let body = {};
        try {
          body = await request.json();
        } catch (_) {
          return json({ error: 'body bukan JSON' }, 400);
        }
        const text = String(body.text || '').slice(0, 1000);
        if (!text.trim()) return json({ error: 'text kosong' }, 400);
        const message = {
          id: String(body.id || crypto.randomUUID()),
          type: 'CHAT',
          username: String(body.username || 'Tamu').slice(0, 40),
          text,
          at: Date.now(),
          system: false,
        };
        this.record(message);
        this.broadcast(message, null);
        return json({ ok: true });
      }
    }

    return json({ error: 'not found' }, 404);
  }

  accept(ws, username) {
    ws.accept();
    this.sessions.set(ws, username);

    // kirim riwayat singkat biar yang baru masuk gak lihat chat kosong
    for (const message of this.messages.slice(-40)) {
      try {
        ws.send(JSON.stringify(message));
      } catch (_) {}
    }

    this.system(`${username} masuk room`);

    ws.addEventListener('message', (event) => {
      let data = {};
      try {
        data = JSON.parse(event.data);
      } catch (_) {
        return;
      }
      const type = String(data.type || '').toUpperCase();

      if (type === 'PING') {
        try {
          ws.send(JSON.stringify({ type: 'PONG', at: Date.now() }));
        } catch (_) {}
        return;
      }

      if (type === 'CHAT') {
        const text = String(data.text || '').slice(0, 1000);
        if (!text.trim()) return;
        const message = {
          id: String(data.id || crypto.randomUUID()),
          type: 'CHAT',
          username: String(data.username || username).slice(0, 40),
          text,
          at: Date.now(),
          system: false,
        };
        this.record(message);
        // di-echo ke SEMUA termasuk pengirim; app dedup pakai id pesan
        this.broadcast(message, null);
        return;
      }

      if (type === 'PLAY' || type === 'PAUSE' || type === 'SEEK') {
        this.broadcast(
          {
            type,
            positionMs: Number(data.positionMs || 0),
            by: String(data.by || username).slice(0, 40),
            at: Date.now(),
          },
          ws, // aksi pemutaran gak perlu dibalikin ke pengirimnya
        );
      }
    });

    const bye = () => {
      if (!this.sessions.has(ws)) return;
      const name = this.sessions.get(ws);
      this.sessions.delete(ws);
      this.system(`${name} keluar room`);
    };
    ws.addEventListener('close', bye);
    ws.addEventListener('error', bye);
  }

  system(text) {
    const message = {
      id: crypto.randomUUID(),
      type: 'SYSTEM',
      username: '',
      text,
      at: Date.now(),
      system: true,
      participants: this.participantCount(),
    };
    this.record(message);
    this.broadcast(message, null);
  }

  record(message) {
    this.messages.push(message);
    if (this.messages.length > MESSAGE_KEEP) {
      this.messages = this.messages.slice(-MESSAGE_KEEP);
    }
  }

  broadcast(payload, exclude) {
    const body = JSON.stringify({ ...payload, participants: this.participantCount() });
    for (const ws of [...this.sessions.keys()]) {
      if (ws === exclude) continue;
      try {
        ws.send(body);
      } catch (_) {
        this.sessions.delete(ws);
      }
    }
  }

  touchPoller(username) {
    this.pollers = this.pollers || new Map();
    this.pollers.set(username, Date.now());
  }

  participantCount() {
    const names = new Set(this.sessions.values());
    if (this.pollers) {
      const cutoff = Date.now() - 12_000;
      for (const [name, at] of this.pollers) {
        if (at >= cutoff) names.add(name);
        else this.pollers.delete(name);
      }
    }
    return Math.max(names.size, this.sessions.size);
  }
}

/* ==================================================================
 * NobarInvites — daftar undangan semua user
 * ================================================================== */
export class NobarInvites {
  constructor(state) {
    this.state = state;
  }

  async fetch(request) {
    const url = new URL(request.url);

    if (url.pathname === '/list') {
      const username = (url.searchParams.get('username') || '').toLowerCase();
      const all = (await this.state.storage.get('invites')) || [];
      const alive = all.filter((i) => Date.now() - i.createdAt < INVITE_TTL_MS);
      if (alive.length !== all.length) await this.state.storage.put('invites', alive);
      return json({
        invites: alive
          .filter((i) => (i.toUsername || '').toLowerCase() === username)
          .sort((a, b) => b.createdAt - a.createdAt)
          .slice(0, 30),
      });
    }

    if (url.pathname === '/add') {
      let body = {};
      try {
        body = await request.json();
      } catch (_) {
        return json({ error: 'body bukan JSON' }, 400);
      }
      const roomId = String(body.roomId || '').trim();
      const toUsername = String(body.toUsername || '').trim();
      if (!roomId || !toUsername) return json({ error: 'roomId & toUsername wajib' }, 400);

      const all = (await this.state.storage.get('invites')) || [];
      const fresh = all.filter((i) => Date.now() - i.createdAt < INVITE_TTL_MS);
      const duplicate = fresh.find(
        (i) =>
          i.roomId === roomId &&
          (i.toUsername || '').toLowerCase() === toUsername.toLowerCase(),
      );

      if (!duplicate) {
        fresh.push({
          id: crypto.randomUUID(),
          roomId,
          fromUsername: String(body.fromUsername || '').slice(0, 40),
          toUsername: toUsername.slice(0, 40),
          contentTitle: String(body.contentTitle || '').slice(0, 160),
          createdAt: Date.now(),
        });
        await this.state.storage.put('invites', fresh.slice(-400));
      }
      return json({ ok: true });
    }

    if (url.pathname === '/remove') {
      const id = url.searchParams.get('id');
      const all = (await this.state.storage.get('invites')) || [];
      await this.state.storage.put('invites', all.filter((i) => i.id !== id));
      return json({ ok: true });
    }

    return json({ error: 'not found' }, 404);
  }
}
