// يُشغَّل بعد "npx cap add android": تعديلات الأندرويد
const fs = require('fs');
const man = 'android/app/src/main/AndroidManifest.xml';
let m = fs.readFileSync(man, 'utf8');
if (!m.includes('largeHeap')) {
  m = m.replace('<application', '<application android:largeHeap="true" android:usesCleartextTraffic="true"');
}
fs.writeFileSync(man, m);
const dir = 'android/app/src/main/java/com/videowall/app/';
fs.writeFileSync(dir + 'MainActivity.java', `package com.videowall.app;

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
