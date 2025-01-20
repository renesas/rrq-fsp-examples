package com.renesas.wifi.firmware;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.util.MyLog;
import java.io.File;

public class SupportActivity extends AppCompatActivity {

   private WebView mWebView;
   private WebSettings mWebSettings;
   private Context mContext;

   /**
    * {@inheritDoc}
    * <p>
    * Perform initialization of all fragments.
    *
    * @param savedInstanceState
    */
   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
              WindowManager.LayoutParams.FLAG_FULLSCREEN);

      setContentView(R.layout.activity_support);

      mContext = this;

      mWebView = (WebView) findViewById(R.id.webView);

      mWebView.setWebViewClient(new WebViewClient());
      mWebSettings = mWebView.getSettings();
      mWebSettings.setJavaScriptEnabled(true);
      mWebSettings.setSupportMultipleWindows(false);
      mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false);
      mWebSettings.setLoadWithOverviewMode(true);
      mWebSettings.setUseWideViewPort(true);
      mWebSettings.setSupportZoom(false);
      mWebSettings.setBuiltInZoomControls(false);
      mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
      mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
      mWebSettings.setDomStorageEnabled(true);

      mWebView.loadUrl("https://www.dialog-semiconductor.com/products/wi-fi/da16200#tab-field_tab_content_resources");

   }

   private void createFolder() {
      File path = Environment.getExternalStoragePublicDirectory("/(Enter path)");

      if (!path.mkdirs()) {
         MyLog.e("Directory not created");
      }
      else {
         Toast.makeText(this, "Save folder", Toast.LENGTH_SHORT).show();
      }
   }
}
