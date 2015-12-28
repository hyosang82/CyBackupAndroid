package kr.hyosang.cybackup;

        import android.app.Activity;
        import android.app.AlertDialog;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.os.Handler;
        import android.os.Message;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.webkit.JsResult;
        import android.webkit.WebChromeClient;
        import android.webkit.WebView;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.Toast;

        import java.io.IOException;
        import java.io.OutputStream;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.net.URLEncoder;
        import java.util.List;
        import java.util.Map;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

        import javax.net.ssl.HttpsURLConnection;

        import kr.hyosang.common.HttpUtil;

public class LoginActivity extends Activity {
    private static final String TAG = "LoginActivity";

    private static final int MSG_SHOW_TOAST = 0x01;
    private static final int MSG_LOGIN_SUCCESS = 0x10;

    private WebView mWeb;
    private String email;
    private String mCookie;
    private Button mBtnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mBtnLogin = (Button) findViewById(R.id.btn_login);

        mWeb = (WebView) findViewById(R.id.webview);
        mWeb.getSettings().setJavaScriptEnabled(true);

        mWeb.setWebChromeClient(new MyWebChromeClient());

        mWeb.loadUrl("https://cyxso.cyworld.com/mnate/Login.sk?loginstr=redirect&redirection=");

        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnLogin.setEnabled(false);

                EditText et_e = (EditText) findViewById(R.id.et_email);
                EditText et_p = (EditText) findViewById(R.id.et_pass);

                email = et_e.getText().toString();
                String pass = et_p.getText().toString();

                String js = "javascript:";

                js += "document.login.email.value='" + email + "';";
                js += "document.login.passwd.value='" + pass + "';";
                js += "checkInput();";
                js += "alert('CKIE'+document.cookie);";
                js += "alert('RSA|^|'+document.login.passwd_rsa.value);";

                mWeb.loadUrl(js);
            }
        });

        (new AlertDialog.Builder(this))
                .setMessage("이 어플을 사용함에 따른 모든 책임은 사용자 본인에게 있음을 알고 있으며, 이에 동의합니다.")
                .setPositiveButton("동의", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNegativeButton("동의하지 않음", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .create().show();
    }

    private void requestLogin(String rsaPasswd) {
        HttpUtil.HttpData login = HttpUtil.HttpData.createPostRequest("https://cyxso.cyworld.com/LoginAuth.sk");

        login.requestHeaders.put("Referer", "http://cyxso.cyworld.com/Login.sk");
        login.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        login.postData.put("email", email);
        login.postData.put("passwd", "");
        login.postData.put("loginstr", "direct");
        login.postData.put("redirection", "http://www.cyworld.com/cymain");
        login.postData.put("pop", "");
        login.postData.put("passwd_rsa", rsaPasswd);
        login.postData.put("mode", "");
        login.postData.put("cpurl", "");

        login.mListener = new HttpUtil.HttpListener() {
            @Override
            public void onCompleted(HttpUtil.HttpData data) {
                requestMain();
            }
        };

        HttpUtil.getInstance().add(login);
    }

    private void requestMain() {
        HttpUtil.HttpData home = HttpUtil.HttpData.createGetRequest("http://www.cyworld.com/cymain");
        home.mListener = new HttpUtil.HttpListener() {
            @Override
            public void onCompleted(HttpUtil.HttpData data) {
                //get tid
                Pattern p = Pattern.compile("tid=\\\"([0-9]+)\\\"");
                Matcher m = p.matcher(data.responseBody);

                if(m.find()) {
                    Log.d(TAG, "TID = " + m.group(1));
                    CyData.getInstance().mTid = m.group(1);

                    moveToDownloader(true);
                }else {
                    Toast.makeText(LoginActivity.this, "Cannot find TID", Toast.LENGTH_LONG).show();
                }
            }
        };

        HttpUtil.getInstance().add(home);
    }

    private void moveToDownloader(boolean bFinish) {
        Intent i = new Intent(LoginActivity.this, DownloadActivity.class);
        startActivity(i);

        if(bFinish) {
            finish();
        }

    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if(message.startsWith("CKIE")) {
                mCookie = message.substring(4);
                Log.d("TEST", "CyData = " + mCookie);
            }else if(message.startsWith("RSA|^|")) {
                //RSA
                String rsa = message.substring(6);
                Log.d("TEST", "RSA = " + rsa);
                requestLogin(rsa);
            }else {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }

            result.confirm();

            return true;
        }
    };
}
