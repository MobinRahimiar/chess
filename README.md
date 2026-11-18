# ChessMaster AI — Stage 10 (Final build automation)

پروژهٔ اندرویدی شطرنج با رابط فارسی، تحلیل‌گر UCI/Stockfish، مربی هوشمند و بخش آکادمی.

## محتوای این مرحله

- GitHub Actions برای اجرای تست‌های واحد و ساخت خودکار APK
- ذخیرهٔ APK در بخش Artifacts هر اجرای موفق
- امکان ساخت اختیاری موتور Stockfish برای ARM64 با Android NDK
- ایجاد GitHub Release و پیوست APK هنگام Push کردن Tag نسخه‌ای مثل `v0.3.0`
- راهنمای قدم‌به‌قدم GitHub در فایل `CI_BUILD.md`

## شروع سریع

1. این پوشه را در یک GitHub Repository قرار دهید؛ پوشهٔ مخفی `.github/workflows` را حذف نکنید.
2. به زبانهٔ **Actions** مخزن بروید.
3. بعد از اجرای موفق **Android CI and APK**، فایل APK را از بخش **Artifacts** دانلود کنید.
4. برای ساخت APK همراه Stockfish، اجرای دستی Workflow را انتخاب و گزینهٔ `build_stockfish` را فعال کنید.

راهنمای کامل فارسی: [`CI_BUILD.md`](CI_BUILD.md)

## وضعیت تحویل

این بسته سورس و تنظیمات CI است، نه APK از پیش ساخته‌شده. در این محیط Android SDK/Gradle نصب نیست؛ بنابراین APK در اینجا کامپایل و روی گوشی آزمایش نشده است. GitHub Actions پس از قرار گرفتن سورس در مخزن، ساخت را در runner خودش انجام می‌دهد.

## پیش‌نیاز ساخت

- JDK 17
- Gradle 8.11.1
- Android SDK Platform 36 / Build Tools 36.0.0
- برای موتور Stockfish: Android NDK r27c یا جدیدتر

## مجوز Stockfish

در صورت بسته‌بندی موتور Stockfish، شرایط GPL-3.0-or-later باید رعایت شود. جزئیات در `STAGE7_NOTES.md` و `CI_BUILD.md` آمده است.
