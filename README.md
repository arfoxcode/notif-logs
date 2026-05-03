# Notif Logs v2.4

Notif Logs adalah aplikasi Android lokal untuk mencatat dan mengelola notifikasi dalam bentuk thread/chat.

## Fokus update v2.4

- Thread/catatan notifikasi sekarang memakai kombinasi **judul + nama aplikasi**.
- Nama package tetap disimpan untuk rules/debug internal, tetapi tidak lagi menjadi identitas utama catatan UI.
- Filter group summary untuk mengurangi notifikasi dobel dari aplikasi chat.
- Parser MessagingStyle untuk mengambil isi pesan dari notifikasi chat modern.
- Dedup mode: SAFE, NORMAL, AGGRESSIVE.
- Dedup window bisa diatur: 5s, 30s, 60s, 120s.
- Settings baru untuk capture ongoing, retention, hide preview, debug logging.
- Diagnostics baru untuk melihat listener, battery, last capture, last saved, last skip, dan alasan skip terakhir.
- Runtime logging lebih hemat, INFO detail hanya disimpan saat debug logging aktif.
- gradle.properties diringankan untuk build di HP RAM kecil/JStudio.

## Package

`com.navre.notiflogs`

## Build

Project ini memakai Kotlin + Android Gradle Plugin dan ditargetkan untuk JStudio/Android Studio.

Rekomendasi `gradle.properties` sudah disetel untuk RAM kecil:

```properties
org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
kotlin.jvm.target.validation.mode=warning
org.gradle.daemon=true
org.gradle.parallel=false
org.gradle.workers.max=1
kotlin.incremental=true
android.nonTransitiveRClass=true
```

## Catatan

Aplikasi tidak memakai permission INTERNET. Semua data notifikasi disimpan lokal di SQLite.
