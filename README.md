# Catetan

Aplikasi catatan Android yang simpel dan lokal. Tanpa login, tanpa akun, tanpa cloud. Semua data tersimpan langsung di HP.

## Fitur

- **Catatan:** buat, lihat, edit, dan hapus catatan
- **Kertas bergaris:** double-tap untuk masuk ke mode edit
- **Warna catatan:** label warna dapat diatur per catatan
- **Tema:** terang / gelap / ikuti sistem
- **Undo / redo:** kembali ke isi catatan sebelumnya
- **Ekspor:** semua catatan ke `.ctn` (JSON Catetan), atau satu catatan ke TXT / JSON / Markdown
- **Impor:** catatan dari file `.ctn` / `.txt` / `.json` / `.md`

## Teknologi

- Kotlin
- Jetpack Compose
- Room (database lokal)
- Material 3

## Build

```bash
cd /path/to/Catetan
./gradlew assembleDebug
```

APK akan ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Lisensi

MIT License.
