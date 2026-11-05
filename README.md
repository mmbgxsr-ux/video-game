# جدار الفيديو + مشغّل التطبيقات

- `index.html`: صفحة جدار الفيديو (تُنشر عبر GitHub Pages).
- `android/`: تطبيق أندرويد يفتح عدة تطبيقات معًا.
- `.github/workflows/android.yml`: يبني ملف APK تلقائيًا.

## نشر الصفحة
Settings ← Pages ← Deploy from a branch ← `main` ← `/ (root)`.

## الحصول على التطبيق
تبويب **Actions** ← **Build Android APK** ← افتح آخر تشغيل ناجح ← نزّل **MultiLauncher-apk** ← فك الضغط وثبّت الملف على هاتفك
(يلزم السماح بالتثبيت من مصادر غير معروفة).
شرح الاستخدام في `android/README.md`.
