RINGKASAN PERUBAHAN — sesi ini
============================================================

✅ SELESAI & SIAP PAKAI (client Android, gak butuh apa-apa lagi):
------------------------------------------------------------
1. HWID beneran ngirim device ID sekarang.
   - util/DeviceIdProvider.kt (baru)
   - data/model/AuthModels.kt -> LoginRequest tambah deviceId, deviceName
   - domain/repository/AuthRepository.kt + data/repository/AuthRepositoryImpl.kt
     -> login() minta deviceId/deviceName
   - ui/auth/LoginViewModel.kt -> generate & kirim device ID beneran

2. Kode room Nobar sekarang "ZarStream-xxxxxx" (client-side).
   - ui/nobar/NobarRoomViewModel.kt

3. Tab baru "Join Nobar" di bottom nav, sebelah Beranda.
   - ui/nobar/NobarHubScreen.kt (baru)
   - ui/nobar/NobarHubViewModel.kt (baru) — input kode + daftar undangan
     temen (di-parse dari /api/notifications, cari pola ZarStream-xxxxxx)
   - ui/navigation/Screen.kt + AppNavigation.kt -> route & item bottom nav

4. Nobar sekarang cuma HOST yang bisa play/pause/seek/skip.
   - ui/player/PlayerViewModel.kt -> begitu roomId ada, ambil data room ASLI
     dari backend (getNobarRoom), tentuin isHost dari perbandingan username,
     override contentId/title/season/episode biar yang GABUNG lewat kode
     nonton konten yang sama persis kayak host (sebelumnya ini kosong/salah)
   - ui/player/PlayerScreen.kt -> ExoPlayer controller disembunyikan total
     buat non-host + badge "🔒 Cuma host yang bisa kontrol pemutaran"
     (untuk sumber WebView/VidSrc, sentuhan non-host ditelan — TAPI sync
     beneran tetap TIDAK berlaku buat sumber WebView, lihat catatan di bawah)

5. Badge role: MEMBER / MEMBER+ (bukan VIP lagi), dibedain dari field
   `maxDevices` yang UDAH ADA di UserDto (backend gak perlu ubah apa-apa
   asal owner set maxDevices=1 buat member, 5 buat member+ pas jual akun).
   - ui/common/RoleBadge.kt
   - ui/profile/ProfileScreen.kt, ui/owner/OwnerPanelScreen.kt (pass maxDevices)

6. Login screen di-reskin lebih gelap/merah (senada login.html), + tombol
   BELI ACCOUNT & PULIHKAN ACCOUNT.
   - ui/auth/LoginScreen.kt

7. order.html & helper.html dibundel ke app/src/main/assets/, dibuka lewat
   WebView in-app (bukan browser luar), base URL di-set ke domain worker
   asli (jepverse.pokaycore.workers.dev) supaya semua fetch("/api/...")
   di dalam halaman itu tetap jalan normal.
   - ui/webpage/AssetWebPageScreen.kt (baru)
   - Screen.kt + AppNavigation.kt -> route BeliAccount / PulihkanAccount

8. order.html DIROMBAK: dari jual 3 role flat (member/reseller/owner tanpa
   durasi) jadi 2 role x 3 durasi sesuai permintaan:
     Member   (1 HP): 7 hari Rp10.000 · 14 hari Rp15.000 · 30 hari Rp25.000
     Member+  (5 HP): 7 hari Rp17.000 · 14 hari Rp27.000 · 30 hari Rp45.000
   Semuanya JAUH di bawah Netflix (Rp54.000/bln 1 HP, Rp184.000/bln 4 HP).
   Body yang dikirim ke /api/order/create sekarang: { role, duration,
   maxDevices, username, password }.


⚠️ BELUM SELESAI — butuh source code worker asli kamu:
------------------------------------------------------------
worker-addon/pakasir-order-helper.js adalah DRAFT referensi (BUKAN siap
tempel) buat endpoint:
  POST /api/order/create, GET /api/order/status/:id,
  POST /api/helper/create, GET /api/helper/status/:id,
  POST /api/helper/complete, POST /api/pakasir/webhook

Kenapa belum bisa "selesai beneran":
- Aku gak tau cara worker kamu nyimpen data user (KV? D1? sesuatu yang lain?)
  jadi draft ini pakai KV generik dengan banyak TODO yang HARUS diisi sesuai
  sistem penyimpanan asli kamu (bikin akun, reset device, dll).
- Endpoint resmi Pakasir buat generate transaksi/QR aku belum 100% yakin
  persis (dokumentasinya butuh login akun Pakasir kamu, aku gak bisa buka).
  Yang aku pakai (redirect ke https://app.pakasir.com/pay/{slug}/{amount})
  itu paling aman secara riset, tapi CEK ULANG di app.pakasir.com/docs
  sebelum deploy — terutama kalau maunya QR ditampilin langsung di app
  (bukan buka halaman Pakasir).
- Payload webhook Pakasir (yang aku pakai buat nandain lunas) aku cukup
  yakin formatnya: {amount, order_id, project, status, payment_method,
  completed_at} — tapi TIDAK ADA signature, jadi verifikasi keasliannya
  cuma dari cross-check amount+order_id+project yang cocok (sudah aku
  terapin di draft-nya).

KIRIM SOURCE WORKER ASLI KAMU (file .js/.ts yang di-deploy ke Cloudflare)
biar aku integrasiin PRESISI: role/durasi/harga, HWID enforcement di
server (cek jumlah device vs maxDevices pas login), dan Pakasir-nya
sekaligus — bukan tebak-tebakan lagi.


⚠️ CATATAN JUJUR — VidSrc / WebView:
------------------------------------------------------------
Kalau sumber video hasil resolve-nya WebView embed (bukan .m3u8 langsung),
sinkron play/pause/seek antara host & partisipan TETAP TIDAK BERFUNGSI,
karena itu player pihak ketiga tanpa hook. Yang jalan cuma "kunci sentuhan"
partisipan (mereka gak bisa apa-apain videonya), tapi videonya sendiri
gak dipaksa senada sama host. Sinkron penuh cuma jalan kalau resolver
berhasil dapet link .m3u8 langsung (ExoPlayer).
