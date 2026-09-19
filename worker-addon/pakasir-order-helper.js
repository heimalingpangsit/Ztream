/**
 * ============================================================================
 * PAKASIR + ORDER/HELPER WORKER ADDON — buat jepverse.pokaycore.workers.dev
 * ============================================================================
 *
 * PENTING DIBACA DULU:
 * Aku (Claude) nulis file ini TANPA pernah liat source code worker asli
 * kamu — karena itu nggak ada di zip yang di-upload. Jadi ini BUKAN patch
 * siap-tempel, tapi modul referensi yang perlu kamu (atau aku, kalau nanti
 * kirim source worker aslinya) GABUNGIN manual ke worker yang udah jalan,
 * terutama bagian:
 *   - Cara nyimpen/baca data user (aku asumsikan KV, ganti sesuai punyamu:
 *     bisa D1, Durable Object, dll)
 *   - Cara bikin/reset password akun
 *   - Middleware/router yang udah ada (aku tulis sebagai routing polos)
 *
 * ENDPOINT DI SINI MATCH PERSIS SAMA YANG DIPANGGIL order.html & helper.html
 * yang udah aku update (lihat app/src/main/assets/):
 *   POST /api/order/create        { role, duration, maxDevices, username, password }
 *   GET  /api/order/status/:id
 *   POST /api/helper/create       { username, amount }
 *   GET  /api/helper/status/:id
 *   POST /api/helper/complete     { invoice_id, username }
 *   POST /api/pakasir/webhook     <- dipanggil PAKASIR, bukan app
 *
 * ENV VARS yang dibutuhin (Worker → Settings → Variables):
 *   PAKASIR_SLUG     = slug project Pakasir kamu
 *   PAKASIR_API_KEY  = API key project Pakasir kamu
 *   HELPER_PRICE     = harga jasa pulihkan akun, mis. "5000"
 *
 * KV NAMESPACES (buat di Worker → Settings → Bindings, sesuaikan nama):
 *   ORDERS  -> nyimpen status tiap invoice (paid/pending)
 *   USERS   -> tempat akun (kalau kamu udah punya cara nyimpen user sendiri,
 *              GANTI semua `env.USERS.*` di bawah ke pemanggilan fungsi kamu)
 *
 * SOAL API PAKASIR:
 * Aku udah riset API Pakasir (endpoint create-transaksi, format webhook,
 * dll) tapi aku TIDAK bisa buka dokumentasi resminya langsung (butuh login
 * akun Pakasir kamu). Yang aku YAKIN dari riset:
 *   - Webhook Pakasir POST ke URL yang kamu set di dashboard project, body:
 *       { amount, order_id, project, status, payment_method, completed_at }
 *     status "completed" = lunas. TIDAK ADA signature/HMAC di payload ini,
 *     jadi verifikasi keasliannya penting (lihat catatan di webhook handler).
 *   - Endpoint hosted-checkout (redirect user ke halaman bayar Pakasir):
 *       https://app.pakasir.com/pay/{slug}/{amount}?order_id=...&qris_only=1
 *     Ini yang aku pakai di bawah karena paling minim risiko salah tebak
 *     endpoint (nggak perlu API call buat generate-nya).
 *   - ⚠️ CEK ULANG di https://app.pakasir.com/docs (login akun Pakasir kamu)
 *     sebelum deploy — terutama kalau kamu maunya generate QR string LANGSUNG
 *     (bukan redirect ke halaman Pakasir), itu butuh endpoint create-transaksi
 *     resmi yang aku kurang yakin 100% path-nya.
 * ============================================================================
 */

const ROLE_PRICES = {
  member:      { 7: 10000, 14: 15000, 30: 25000 },
  member_plus: { 7: 17000, 14: 27000, 30: 45000 },
};
const ROLE_MAX_DEVICES = { member: 1, member_plus: 5 };

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function newInvoiceId(prefix) {
  const rand = Math.random().toString(36).slice(2, 8).toUpperCase();
  const ts = Date.now().toString(36).toUpperCase();
  return `${prefix}${ts}${rand}`;
}

/** Bangun link checkout QRIS Pakasir (hosted page, bukan raw QR string). */
function buildPakasirPayUrl(env, orderId, amount) {
  const slug = env.PAKASIR_SLUG;
  return `https://app.pakasir.com/pay/${slug}/${amount}?order_id=${encodeURIComponent(orderId)}&qris_only=1`;
}

export async function handlePakasirRoutes(request, env, url) {
  const { pathname } = url;
  const method = request.method;

  // ---------------------------------------------------------------
  // POST /api/order/create — beli akun member/member+ (baru)
  // ---------------------------------------------------------------
  if (pathname === "/api/order/create" && method === "POST") {
    const body = await request.json();
    const { role, duration, username, password } = body;

    if (!ROLE_PRICES[role] || !ROLE_PRICES[role][duration]) {
      return json({ success: false, error: "Role/durasi tidak valid" }, 400);
    }
    if (!username || username.length < 3) {
      return json({ success: false, error: "Username minimal 3 karakter" }, 400);
    }
    if (!password || password.length < 6) {
      return json({ success: false, error: "Password minimal 6 karakter" }, 400);
    }

    // TODO: cek username belum dipakai (sesuai penyimpanan user kamu)
    // const exists = await env.USERS.get(`user:${username}`);
    // if (exists) return json({ success: false, error: "Username sudah dipakai" }, 400);

    const amount = ROLE_PRICES[role][duration];
    const invoiceId = newInvoiceId("ORD");

    await env.ORDERS.put(
      `order:${invoiceId}`,
      JSON.stringify({
        type: "order",
        invoiceId,
        role,
        duration,
        maxDevices: ROLE_MAX_DEVICES[role],
        username,
        password, // TODO: idealnya di-hash duluan sebelum disimpan, JANGAN plaintext lama-lama
        amount,
        status: "pending",
        createdAt: Date.now(),
      }),
      { expirationTtl: 60 * 60 } // invoice basi otomatis abis 1 jam kalau gak dibayar
    );

    return json({
      success: true,
      invoice_id: invoiceId,
      amount,
      qr_string: buildPakasirPayUrl(env, invoiceId, amount),
    });
  }

  // ---------------------------------------------------------------
  // GET /api/order/status/:id — dipolling order.html tiap beberapa detik
  // ---------------------------------------------------------------
  if (pathname.startsWith("/api/order/status/") && method === "GET") {
    const invoiceId = pathname.split("/").pop();
    const raw = await env.ORDERS.get(`order:${invoiceId}`);
    if (!raw) return json({ success: false, error: "Invoice tidak ditemukan" }, 404);

    const order = JSON.parse(raw);
    return json({
      success: true,
      status: order.status, // "pending" | "completed"
      username: order.status === "completed" ? order.username : undefined,
      password: order.status === "completed" ? order.password : undefined,
      role: order.role,
      uid: order.uid,
    });
  }

  // ---------------------------------------------------------------
  // POST /api/helper/create — jasa pulihkan akun (reset HWID)
  // ---------------------------------------------------------------
  if (pathname === "/api/helper/create" && method === "POST") {
    const { username } = await request.json();
    if (!username) return json({ success: false, error: "Username wajib diisi" }, 400);

    // TODO: pastikan username ini beneran ada (sesuai penyimpanan user kamu)

    const amount = Number(env.HELPER_PRICE || 5000);
    const invoiceId = newInvoiceId("HLP");

    await env.ORDERS.put(
      `order:${invoiceId}`,
      JSON.stringify({
        type: "helper",
        invoiceId,
        username,
        amount,
        status: "pending",
        createdAt: Date.now(),
      }),
      { expirationTtl: 60 * 60 }
    );

    return json({
      success: true,
      invoice_id: invoiceId,
      amount,
      qr_string: buildPakasirPayUrl(env, invoiceId, amount),
    });
  }

  // ---------------------------------------------------------------
  // GET /api/helper/status/:id
  // ---------------------------------------------------------------
  if (pathname.startsWith("/api/helper/status/") && method === "GET") {
    const invoiceId = pathname.split("/").pop();
    const raw = await env.ORDERS.get(`order:${invoiceId}`);
    if (!raw) return json({ success: false, error: "Invoice tidak ditemukan" }, 404);
    const order = JSON.parse(raw);
    return json({ success: true, status: order.status });
  }

  // ---------------------------------------------------------------
  // POST /api/helper/complete — dipanggil setelah status "completed",
  // beneran nge-reset device count user itu ke 0
  // ---------------------------------------------------------------
  if (pathname === "/api/helper/complete" && method === "POST") {
    const { invoice_id, username } = await request.json();
    const raw = await env.ORDERS.get(`order:${invoice_id}`);
    if (!raw) return json({ success: false, error: "Invoice tidak ditemukan" }, 404);

    const order = JSON.parse(raw);
    if (order.status !== "completed") {
      return json({ success: false, error: "Pembayaran belum lunas" }, 400);
    }
    if (order.username !== username) {
      return json({ success: false, error: "Username tidak cocok dengan invoice" }, 400);
    }

    // TODO: ganti ini sesuai cara kamu nyimpen device list per user.
    // Intinya: kosongin daftar HWID yang terdaftar buat `username`, biar dia
    // bisa login dari HP baru lagi.
    // const clearedCount = await resetUserDevices(env, username);
    const clearedCount = 0; // <- ganti sesuai implementasi asli kamu

    return json({ success: true, username, cleared_count: clearedCount });
  }

  // ---------------------------------------------------------------
  // POST /api/pakasir/webhook — dipanggil PAKASIR pas pembayaran lunas.
  // Body (sesuai riset, TIDAK ada signature): 
  //   { amount, order_id, project, status, payment_method, completed_at }
  // ---------------------------------------------------------------
  if (pathname === "/api/pakasir/webhook" && method === "POST") {
    const payload = await request.json();
    const { order_id, amount, status, project } = payload;

    if (project !== env.PAKASIR_SLUG) {
      return json({ success: false, error: "Project tidak cocok" }, 400);
    }
    if (status !== "completed") {
      return json({ success: true }); // abaikan status selain completed
    }

    const raw = await env.ORDERS.get(`order:${order_id}`);
    if (!raw) return json({ success: false, error: "Order tidak ditemukan" }, 404);
    const order = JSON.parse(raw);

    // Karena webhook Pakasir gak ada signature, kita verifikasi silang:
    // amount di webhook HARUS sama persis dengan amount pas invoice dibuat.
    if (Number(amount) !== Number(order.amount)) {
      return json({ success: false, error: "Amount tidak cocok — dicurigai" }, 400);
    }
    if (order.status === "completed") {
      return json({ success: true }); // udah diproses sebelumnya, jangan dobel
    }

    if (order.type === "order") {
      // TODO: bikin akun beneran di sini sesuai sistem user kamu, mis:
      // const uid = await createUser(env, {
      //   username: order.username,
      //   password: order.password,
      //   role: "USER",
      //   vip: true,
      //   maxDevices: order.maxDevices,
      //   expiresAt: new Date(Date.now() + order.duration * 86400000).toISOString(),
      // });
      // order.uid = uid;
    } else if (order.type === "helper") {
      // Untuk helper, complete beneran dilakuin di /api/helper/complete
      // (dipanggil dari helper.html setelah polling status jadi "completed").
      // Di sini cukup tandai lunas.
    }

    order.status = "completed";
    await env.ORDERS.put(`order:${order_id}`, JSON.stringify(order), { expirationTtl: 60 * 60 * 24 });

    return json({ success: true });
  }

  return null; // bukan salah satu route di atas, lempar ke router utama
}
