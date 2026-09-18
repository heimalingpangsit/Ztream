RINGKASAN PERUBAHAN — SESI 2
============================================================

A. TAMPILAN LOGIN SESUAI login.html
------------------------------------------------------------
ui/auth/LoginScreen.kt (ditulis ulang), ui/auth/LoginViewModel.kt,
data/local/SessionManager.kt

- Palet dipindah ke nuansa login.html: latar #0A0000, kartu #110303,
  border merah transparan, aksen #CC1111 / #EE2222, teks #FFDDDD.
- Banner 16:9 dengan garis merah tipis di atas, gradasi gelap ke bawah,
  tulisan "WELCOME" + "BACK" (merah) dan tag "CREATED BY JEPIN666X".
- Kartu login nyambung ke banner: sudut atas rounded di banner, sudut
  bawah rounded di kartu, persis kayak versi web.
- Header kartu: judul italic "Masuk ke akun kamu", subjudul
  "SECURE · ENCRYPTED · PRIVATE", garis pembatas merah.
- Field username/password bernuansa merah gelap + ikon, tombol mata
  buat lihat password, checkbox "INGAT SAYA" custom (bukan Checkbox
  default Material).
- Tombol "MASUK SEKARANG" pakai gradient merah + border, spinner saat
  loading.
- Tombol BELI ACCOUNT & PULIHKAN ACCOUNT full-width bertumpuk (urutan
  sama kayak login.html), plus WHATSAPP & INSTAGRAM biar fitur kontak
  yang sudah ada gak hilang.
- Footer "ZARSTREAM · 2026 · JEPIN666X" dengan titik pemisah.
- TOMBOL SIMPAN (kanan atas kartu) SEKARANG BERFUNGSI: kredensial
  disimpan di SharedPreferences (Base64) lewat SessionManager
  saveCredentials/getSavedCredentials/clearSavedCredentials, dan
  otomatis terisi pas app dibuka. Label berubah SIMPAN <-> TERSIMPAN.


B. GANTI IDENTITAS JADI JEPIN666X
------------------------------------------------------------
app/src/main/assets/order.html, helper.html

- "Vloriyus Store" / "VLORIYUS STORE" -> "JEPIN666X STORE"
- "Vloriyus Helper" / "VLORIYUS HELPER" -> "JEPIN666X HELPER"
- "Official Vloriyus" -> "Official JEPIN666X"
- "Vloriyus Helper Account" -> "JEPIN666X Helper Account"
- title halaman + log konsol ikut diganti.
- semua varian nuxD3T / nuxDT juga diganti JEPIN666X.
- Nol sisa kata "vloriyus"/"nux" di seluruh assets.


C. ABOUT US DI TAB ACCOUNT
------------------------------------------------------------
app/src/main/assets/about.html (baru), ui/navigation/Screen.kt,
ui/navigation/AppNavigation.kt, ui/profile/ProfileScreen.kt,
ui/webpage/AssetWebPageScreen.kt

- Menu "About Us" (ikon Info) ditambah di kartu "Pengaturan Akun",
  posisinya di atas Owner Panel. Route baru: about_us -> WebView
  in-app yang buka about.html dari assets.
- about.html: desain asli dipertahankan (blood theme, blood drip,
  ikon DNA, animasi orbit), yang diubah cuma isinya:
    * brand hero: JEPIN666X + badge "ZARSTREAM · VERSION 3.0.0"
    * DUA developer:
        - JEPIN666X — "Owner & Developer", badge OWNER, kartunya
          nge-link ke Telegram
        - LehzzXD — "Developer", kartu statis tanpa link sosial
    * tombol sosial HANYA akun JEPIN666X:
        Telegram -> https://t.me/jepin666x
        TikTok   -> https://tiktok.com/@jepin420_
    * avatar pakai inisial (JP / LX), bukan gambar dari host luar,
      biar gak pernah muncul gambar rusak
    * kartu singkat "Tentang Aplikasi"
- Tombol back DI DALAM halaman HTML sekarang jalan. Sebelumnya about/
  order/helper nunjuk ke dashboard.html / login.html yang gak ada di
  app, jadi tombolnya mati. Sekarang halaman manggil zarBack() ->
  ZarApp.close() (JavascriptInterface baru di AssetWebPageScreen) ->
  navigasi native yang nutup layar. Ada rule ProGuard baru biar bridge
  ini gak dibuang R8.


D. BUG FIX 1 — UNDANGAN NOBAR GAK KELIHATAN DI SISI YANG DIUNDANG
------------------------------------------------------------
PENYEBAB:
  NobarHubViewModel lama nyari undangan dengan cara nyisir
  /api/notifications lalu mewajibkan teks notifikasi memuat pola
  "ZarStream-xxxxxx" PERSIS. Kode room aslinya digenerate backend,
  dan teks notifikasinya gak selalu memuat kode itu — jadi hasil
  filternya kosong. Si B gak pernah lihat undangan dari si A walaupun
  POST /api/nobar/{room}/invite sendirinya sukses.

PERBAIKAN (ui/nobar/NobarInvite.kt baru, NobarInviteViewModel,
NobarHubViewModel, NobarHubScreen, NobarRealtimeApi, NobarModels):

  Pengiriman undangan sekarang lewat TIGA jalur sekaligus, dan
  dianggap berhasil kalau minimal satu jalur nyampe:
    1. endpoint undangan backend (notifikasi bawaan, seperti sebelumnya)
    2. service Nobar realtime — undangan dicatat terstruktur
       (roomId, dari siapa, judul konten)
    3. chat pribadi berisi penanda "[NOBAR] <kode-room> :: <judul>"
       — jalur paling pasti, karena fitur chat memang sudah jalan

  Pembacaan undangan juga dari tiga sumber itu, digabung & dedup per
  kode room:
    1. service realtime
    2. chat pribadi (pesan terakhir tiap percakapan + isi 8 percakapan
       teratas, cuma pesan dari orang lain yang dibaca)
    3. notifikasi backend, dengan deteksi yang jauh lebih longgar:
       cukup topiknya nobar (kata kunci nobar / nonton bareng /
       watch party / undang / invite), kode room diambil dari pola
       ZarStream-xxx, penanda [NOBAR], atau pola id ber-tanda-hubung
    Undangan yang kodenya beneran gak kebaca tetap ditampilkan, tapi
    tombol Gabung-nya mati + ada keterangan "minta kodenya ke host".

  UI: tombol muat ulang di header "Undangan Masuk", auto-refresh tiap
  15 detik selama layar kebuka, nama pengundang + kode room ditampilkan,
  status tombol "Mengirim..." saat undangan lagi dikirim.


E. BUG FIX 2 — CHAT NOBAR GAK NYAMPE KE PESERTA LAIN
------------------------------------------------------------
PENYEBAB:
  Chat dikirim ke wss://zarstream-nobar.<akun>.workers.dev/websocket,
  worker realtime yang BELUM ADA di project ini (belum pernah
  di-deploy). Client lama cuma sekali coba connect, tanpa reconnect
  dan tanpa jalur cadangan, TAPI pesan pengirim langsung ditambahkan
  ke daftar lokal — makanya A ngerasa chatnya terkirim padahal pesannya
  gak pernah keluar dari HP-nya, dan B gak nerima apa pun.

PERBAIKAN CLIENT (data/remote/NobarSocketClient.kt ditulis ulang,
PlayerViewModel, PlayerScreen, AppModule):
  - reconnect otomatis dengan backoff (maks 15 detik) selama layar
    player kebuka
  - ping tiap 20 detik (OkHttp pingInterval) biar koneksi gak diputus
    perantara
  - FALLBACK POLLING HTTP: selama WebSocket belum nyambung, chat tetap
    jalan lewat GET/POST /api/nobar/room/{roomId}/messages tiap 2 detik
  - tiap pesan punya id unik; server echo balik pesan yang sama dan
    app dedup pakai id itu, jadi chat gak dobel
  - event Connected baru: panel chat nampilin status "Menyambungkan...",
    "Tersambung", atau "Tersambung (mode hemat)" (mode hemat = polling)
  - kirim chat otomatis pindah ke HTTP kalau socket belum siap

PERBAIKAN SERVER (worker-addon/nobar-realtime-worker.js +
nobar-wrangler.toml — BARU, SIAP DEPLOY):
  Worker Cloudflare dengan 2 Durable Object:
    - NobarRoom     : satu instance per kode room. WebSocket chat +
                      sinkronisasi PLAY/PAUSE/SEEK (aksi pemutaran gak
                      dibalikin ke pengirimnya), simpan 200 pesan
                      terakhir buat dibaca jalur polling, kirim 40 pesan
                      terakhir ke yang baru masuk, hitung peserta
                      (WebSocket + yang lewat polling), pesan sistem
                      masuk/keluar room.
    - NobarInvites  : daftar undangan semua user, auto-hangus 6 jam,
                      anti-duplikat per (roomId, penerima), maks 400
                      entri.
  Endpoint: /websocket, /api/nobar/room/:id/messages (GET & POST),
  /api/nobar/invites (GET & POST), /api/nobar/invites/:id (DELETE),
  /health. Gak butuh KV, gak butuh D1, gak butuh API eksternal.

  >>> WAJIB DI-DEPLOY, kalau enggak chat nobar gak akan pernah jalan:
        wrangler login
        wrangler deploy -c nobar-wrangler.toml
      Nama worker harus "zarstream-nobar". Kalau subdomain akunmu bukan
      "pokaycore", ubah SATU baris di app:
        data/remote/NobarSocketClient.kt -> NobarRealtimeConfig.BASE_HOST
      Cek worker hidup: buka https://zarstream-nobar.<subdomain>.workers.dev/health


F. YANG TIDAK DIUBAH
------------------------------------------------------------
- Nama app, package, tema, ikon, navigasi bawah, katalog, player,
  subtitle/CC, owner panel, sistem HWID/login device, badge role,
  chat 1-1, follow/discover, order & helper flow: semua tetap.
- Perubahan sesi sebelumnya (CHANGES-SESI-INI.md) tetap berlaku.
