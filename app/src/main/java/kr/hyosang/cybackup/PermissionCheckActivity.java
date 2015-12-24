package kr.hyosang.cybackup;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hyosang on 2015-12-24.
 */
public class PermissionCheckActivity extends Activity {
    private static int PERMISSION_CHECK_ID = 0x01;

    private static String [] CHECK_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android 6.0부터 dangerous permission 별도 체크 필요
            checkPermissions();
        }else {
            //그 이하는 그대로 진행
            startApp();
        }
    }

    private void checkPermissions() {
        boolean needCheck = false;

        for (String perm : CHECK_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                    //[거부]를 선택한 경우 재실행시 : 메세지 표시 후 재요청
                    /*
                    */
                    return;
                }else {
                    //미승인 권한
                    needCheck = true;
                }
            }else {
                //승인된 권한
            }
        }

        if(needCheck) {
            //request permission
            ActivityCompat.requestPermissions(this, CHECK_PERMISSIONS, PERMISSION_CHECK_ID);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == PERMISSION_CHECK_ID) {
            boolean allGranted = true;

            if(grantResults.length > 0) {
                for(int i=0;i<grantResults.length;i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }

                if(allGranted) {
                    //퍼미션 획득 완료
                    startApp();
                    return;
                }else {
                    //퍼미션 획득 실패
                    finish();
                }
            }
        }
    }

    private void startApp() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);

        finish();
    }
}
