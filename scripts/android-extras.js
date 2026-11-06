// يُشغَّل بعد "npx cap add android": تعديلات الأندرويد
// (ذاكرة أكبر، السماح بـ http، تشغيل الوسائط تلقائيًا، إبقاء الشاشة مضاءة)
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
  fs.writeFileSync(manifest, patched);
}

const dir = path.join(main, 'java', ...appId.split('.'));
fs.mkdirSync(dir, { recursive: true });
fs.writeFileSync(path.join(dir, 'MainActivity.java'), `package ${appId};

import android.os.Bundle;
import android.view.WindowManager;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getBridge().getWebView().getSettings().setMediaPlaybackRequiresUserGesture(false);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }
}
`);
console.log('android extras applied');
