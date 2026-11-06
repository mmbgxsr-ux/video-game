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
بعد كل بناء ناجح على `main` يُنشر الملف في **Releases** برابط ثابت لا يتغيّر ولا يتطلب تسجيل دخول:

- جدار الفيديو: <https://github.com/mmbgxsr-ux/video-game/releases/download/video-wall-latest/video-wall.apk>
- مشغّل التطبيقات: <https://github.com/mmbgxsr-ux/video-game/releases/download/launcher-latest/launcher.apk>

ثبّته على هاتفك (يلزم السماح بالتثبيت من مصادر غير معروفة). بديل: تبويب **Actions** ← آخر تشغيل ناجح ← **Artifacts**.

| الـ workflow | اسم الـ artifact | يُشغَّل تلقائيًا عند تعديل |
|---|---|---|
| Build Video Wall APK | `video-wall-apk` | إعدادات Capacitor أو `offline.html` أو `scripts/` |
| Build Launcher APK | `MultiLauncher-apk` | `launcher-app/` |

يمكن تشغيل أيٍّ منهما يدويًا من زر **Run workflow**.

### التوقيع الثابت (مهم للتحديث فوق النسخة المثبّتة)
أندرويد يرفض تثبيت نسخة جديدة فوق نسخة قديمة إذا اختلف توقيعهما. بدون مفتاح ثابت يُنشئ GitHub مفتاحًا عشوائيًا في كل بناء، فتضطر لحذف التطبيق قبل كل تحديث. لتفعيل المفتاح الثابت أضف سرًّا واحدًا:

1. Settings ← Secrets and variables ← Actions ← **New repository secret**.
2. الاسم: `DEBUG_KEYSTORE_BASE64`، والقيمة: محتوى ملف `DEBUG_KEYSTORE_BASE64.txt` كاملًا (سطر واحد).
3. شغّل الـ workflow مرة أخرى، ثم ثبّت النسخة الناتجة (يلزم حذف النسخة القديمة هذه المرة فقط). بعدها تُثبَّت كل النسخ التالية فوقها مباشرة.

احتفظ بنسخة من المفتاح في مكان آمن: إن ضاع فلن تُثبَّت النسخ الجديدة فوق القديمة. ولا ترفعه إلى المستودع.

## حفظ الجدار
- الجلسة الأخيرة (الروابط ووضع العرض والسرعة) تُستعاد تلقائيًا عند فتح الصفحة.
- زر **الجدارات المحفوظة**: احفظ الجدار الحالي باسم، ثم حمّله أو احذفه لاحقًا.
- فيديوهات جهازك لا تُحفظ لأن روابطها مؤقتة، أما روابط يوتيوب والروابط المباشرة فتُحفظ.
- يُخزَّن كل شيء على الجهاز نفسه (`localStorage`) ولا يُرفع لأي مكان.

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
