// يُشغَّل بعد "npx cap add android": تعديلات الأندرويد
// (ذاكرة أكبر، السماح بـ http، تشغيل الوسائط تلقائيًا، إبقاء الشاشة مضاءة،
//  واستقبال المشاركة من تطبيق يوتيوب وغيره)
const fs = require('fs');
const path = require('path');

const fail = msg => { console.error('android-extras: ' + msg); process.exit(1); };

const { appId } = JSON.parse(fs.readFileSync('capacitor.config.json', 'utf8'));
if (!appId) fail('appId غير موجود في capacitor.config.json');

const main = 'android/app/src/main';
const manifest = path.join(main, 'AndroidManifest.xml');
if (!fs.existsSync(manifest)) fail('لم يُعثر على ' + manifest + ' — شغّل "npm run android:add" أولًا');

let m = fs.readFileSync(manifest, 'utf8');

if (!m.includes('largeHeap')) {
  const patched = m.replace('<application', '<application android:largeHeap="true" android:usesCleartextTraffic="true"');
  if (patched === m) fail('لم يُعثر على وسم <application> في AndroidManifest.xml');
  m = patched;
}

// يظهر التطبيق في قائمة "مشاركة" لأي نص (مثل رابط فيديو من تطبيق يوتيوب)
if (!m.includes('android.intent.action.SEND')) {
  const share = `
        <intent-filter>
            <action android:name="android.intent.action.SEND" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="text/plain" />
        </intent-filter>`;
  const patched = m.replace('</intent-filter>', '</intent-filter>' + share);   // أول فلتر = فلتر النشاط الرئيسي
  if (patched === m) fail('لم يُعثر على intent-filter في AndroidManifest.xml');
  m = patched;
}
fs.writeFileSync(manifest, m);

const dir = path.join(main, 'java', ...appId.split('.'));
fs.mkdirSync(dir, { recursive: true });
fs.writeFileSync(path.join(dir, 'MainActivity.java'), `package ${appId};

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
  // النص المشارك ينتظر حتى تصبح الصفحة جاهزة (الصفحة تُحمَّل من الإنترنت)
  private String pendingShare = null;
  private int shareAttempts = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getBridge().getWebView().getSettings().setMediaPlaybackRequiresUserGesture(false);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    // لا نعيد معالجة المشاركة عند إعادة إنشاء النشاط (مثل تدوير الشاشة)
    if (savedInstanceState == null && captureShare(getIntent())) {
      deliverShare();
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    if (captureShare(intent)) {
      deliverShare();
    }
  }

  private boolean captureShare(Intent intent) {
    if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return false;
    String text = intent.getStringExtra(Intent.EXTRA_TEXT);
    if (text == null || text.isEmpty()) return false;
    pendingShare = text;
    shareAttempts = 0;
    return true;
  }

  // يستدعي addShared في الصفحة، ويعيد المحاولة كل نصف ثانية حتى تُحمَّل الصفحة (حتى 30 ثانية)
  private void deliverShare() {
    final String text = pendingShare;
    if (text == null) return;
    final WebView web = getBridge().getWebView();
    final String js = "(function(t){if(typeof window.addShared==='function'){window.addShared(t);return 1}return 0})("
        + JSONObject.quote(text) + ")";
    web.evaluateJavascript(js, result -> {
      if ("1".equals(result)) {
        pendingShare = null;
      } else if (++shareAttempts < 60) {
        web.postDelayed(this::deliverShare, 500);
      }
    });
  }
}
`);
console.log('android extras applied');
