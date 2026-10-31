package vn.bangkeo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView web;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        web.requestFocus();
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) {
            web.evaluateJavascript("window.onBack && window.onBack()", v -> {
                if (!"true".equals(v)) finish();
            });
            return true;
        }
        return super.onKeyDown(code, e);
    }

    class Bridge {
        @JavascriptInterface
        public String version() {
            try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
        }

        @JavascriptInterface
        public void get(final String url, final String cb) {
            new Thread(() -> {
                JSONObject r = new JSONObject();
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(20000);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36");
                    c.setRequestProperty("Accept", "application/json, text/plain, */*");
                    c.setRequestProperty("Referer", "https://www.sofascore.com/");
                    int code = c.getResponseCode();
                    InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
                    StringBuilder sb = new StringBuilder();
                    if (in != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                    }
                    r.put("status", code);
                    r.put("body", sb.toString());
                    String rem = c.getHeaderField("x-requests-remaining");
                    r.put("remaining", rem == null ? "" : rem);
                } catch (Exception ex) {
                    try { r.put("status", 0); r.put("body", String.valueOf(ex.getMessage())); } catch (Exception ignored) {}
                }
                final String js = "window.__cb(" + JSONObject.quote(cb) + "," + JSONObject.quote(r.toString()) + ")";
                ui.post(() -> web.evaluateJavascript(js, null));
            }).start();
        }
    }
}
