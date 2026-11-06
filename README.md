# جدار الفيديو + مشغّل التطبيقات

- `index.html`: صفحة جدار الفيديو (يوتيوب وروابط مباشرة وفيديوهات من جهازك وعارض PDF)، وتُنشر عبر GitHub Pages.
- `launcher-app/`: تطبيق أندرويد يفتح عدة تطبيقات معًا.
- `.github/workflows/launcher-app.yml`: يبني ملف APK تلقائيًا.

## نشر الصفحة
Settings ← Pages ← Deploy from a branch ← `main` ← `/ (root)`.

## الحصول على التطبيق
تبويب **Actions** ← **Build Launcher APK** ← افتح آخر تشغيل ناجح ← نزّل **MultiLauncher-apk** ← فك الضغط وثبّت الملف على هاتفك
(يلزم السماح بالتثبيت من مصادر غير معروفة).
شرح الاستخدام في `launcher-app/README.md`.
