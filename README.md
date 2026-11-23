# Catetan

Aplikasi catatan Android yang simpel dan lokal. Tanpa login, tanpa akun, tanpa cloud. Semua data tersimpan langsung di HP.

<p align="center">
  <img src="https://raw.githubusercontent.com/Nirune/Catetan/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Catetan icon" width="128" height="128">
</p>

## Fitur

- **Catatan:** buat, lihat, edit, dan hapus catatan
- **Kertas bergaris:** double-tap untuk masuk ke mode edit
- **Warna catatan:** label warna dapat diatur per catatan
- **Tema:** terang / gelap / ikuti sistem, plus 8 tema warna (Catetan, Catppuccin, Tokyo Night, Rosé Pine, Gruvbox, Kanagawa, One Dark, Ayu) di layar Tampilan
- **Mode tampilan:** Sistem / Terang / Gelap, berlaku ke seluruh layar
- **Undo / redo:** kembali ke isi catatan sebelumnya
- **Ekspor:** semua catatan ke `.ctn` (JSON Catetan), atau satu catatan ke TXT / JSON / Markdown
- **Impor:** catatan dari file `.ctn` / `.txt` / `.json` / `.md`
- **Todo:** daftar tugas dengan centang, pengingat, dan hitung mundur live
- **Tanggal Dibuat & Diubah** di kartu catatan (list, grid, search): baris "Dibuat <tgl>" dan, bila selisih > 60 detik, baris "Diubah <tgl>"
- **Tanggal Dibuat** di kartu Todo: baris "Dibuat <tgl>" di bawah teks todo
- **Dialog Ekspor** diperbaiki: satu kolom penuh, judul + deskripsi lengkap per format, clickable seluruh baris, scrollable, dibaca di semua tema
- **Penulisan file export** via SAF: validasi izin permanen, nama file unik, folder target bisa dipilih ulang
- **Urutan DB-lalu-alarm** pada toggle/hapus Todo: perubahan database dulu, baru cancel alarm; error alarm tidak crash
- **ReminderScheduler.cancel()** aman untuk todo tanpa pengingat (no-op, tanpa NPE)

## Teknologi

- Kotlin 1.9.23
- Jetpack Compose (Compose BOM 2023.10.00, compiler 1.5.11)
- Room 2.6.1 (database lokal)
- Material 3
- Android Gradle Plugin 8.4.0, Gradle 8.6

## Persyaratan & Kompatibilitas

- **Android minimal:** 7.0 Nougat (API 24, `minSdk = 24`)
- **Android target:** 14 (API 34, `targetSdk = 34`; `compileSdk = 34`)
- **Arsitektur:** APK universal, tanpa `abiFilters` atau `splits`. Tidak ada native library (`.so`), jadi bisa jalan di perangkat ARM64, ARMv7, x86, dan x86_64 yang memenuhi API 24 ke atas.
- **Izin:** tidak ada `<uses-permission>` di manifest, jadi tidak butuh izin runtime untuk pemakaian normal.
- **Build:** JDK 17 (`jvmTarget = 17`) dan Android SDK platform 34.
- **Bahasa antarmuka:** Indonesia.

## Unduh

Rilis terbaru: [Catetan v1.0.9](https://github.com/Nirune/Catetan/releases/latest). Versi sebelumnya ada di [halaman rilis](https://github.com/Nirune/Catetan/releases).

## Build

```bash
cd /path/to/Catetan
./gradlew assembleDebug
```

APK akan ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Lisensi

MIT License.
