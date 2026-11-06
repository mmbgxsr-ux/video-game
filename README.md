# جدار الفيديو + مشغّل التطبيقات

مشروعان في مستودع واحد:

1. **جدار الفيديو**: صفحة ويب تشغّل عدة فيديوهات معًا (يوتيوب، روابط مباشرة، فيديوهات من جهازك، وعارض PDF). تُنشر على GitHub Pages ويمكن تغليفها كتطبيق أندرويد عبر Capacitor.
2. **مشغّل التطبيقات** (`launcher-app/`): تطبيق أندرويد أصلي (Kotlin) يفتح عدة تطبيقات معًا.

## هيكل المستودع

```
index.html                       صفحة جدار الفيديو (يجب أن تبقى في الجذر لـ GitHub Pages)
offline.html                     صفحة تظهر في التطبيق عند انقطاع الإنترنت
capacitor.config.json            إعدادات تغليف الصفحة كتطبيق أندرويد
package.json / package-lock.json اعتماديات Capacitor وسكربتات البناء
scripts/android-extras.js        تعديلات أندرويد تُطبَّق بعد إنشاء مشروع Capacitor
launcher-app/                    مشروع مشغّل التطبيقات (انظر launcher-app/README.md)
.github/workflows/
  video-wall-apk.yml             يبني APK جدار الفيديو  (Build Video Wall APK)
  launcher-app.yml               يبني APK المشغّل        (Build Launcher APK)
```

## نشر الصفحة
Settings ← Pages ← Deploy from a branch ← `main` ← `/ (root)`.

## الحصول على ملفات APK
تبويب **Actions** ← اختر الـ workflow ← افتح آخر تشغيل ناجح ← نزّل الملف من **Artifacts** ← فك الضغط وثبّته على هاتفك (يلزم السماح بالتثبيت من مصادر غير معروفة).

| الـ workflow | اسم الـ artifact | يُشغَّل تلقائيًا عند تعديل |
|---|---|---|
| Build Video Wall APK | `video-wall-apk` | إعدادات Capacitor أو `offline.html` أو `scripts/` |
| Build Launcher APK | `MultiLauncher-apk` | `launcher-app/` |

يمكن تشغيل أيٍّ منهما يدويًا من زر **Run workflow**.

## التحديث التلقائي لتطبيق جدار الفيديو
التطبيق يحمّل الصفحة من GitHub Pages (`server.url` في `capacitor.config.json`)، لذلك:

- أي تعديل على `index.html` يُدفع إلى `main` يصل إلى التطبيق المثبّت عند فتحه، دون APK جديد. قد يتأخر دقائق بسبب نشر Pages والتخزين المؤقت.
- يلزم الإنترنت لتشغيل التطبيق، وعند انقطاعه تظهر `offline.html`.
- تغييرات الجزء الأصلي (`scripts/android-extras.js` أو `capacitor.config.json` أو إصدار Capacitor) تحتاج APK جديدًا يُثبَّت يدويًا، لأن أندرويد لا يسمح بتحديث صامت خارج Google Play.
- إذا غيّرتَ اسم المستودع أو حسابك، حدّث `server.url` في `capacitor.config.json`.

## البناء المحلي لتطبيق جدار الفيديو
يلزم Node 20+ و JDK 21 و Android SDK:

```bash
npm ci
npm run build:web      # ينسخ index.html إلى www/
npm run android:add    # ينشئ مجلد android/
npm run android:patch  # يطبّق scripts/android-extras.js
npm run android:sync
cd android && ./gradlew assembleDebug
```

المجلدان `www/` و `android/` يُولَّدان أثناء البناء ولا يُرفعان إلى Git.

## مشغّل التطبيقات
شرح الاستخدام والحدود في `launcher-app/README.md`.
