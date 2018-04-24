package com.langtao.reborn;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.langtao.PermissionUtils;
import com.langtao.reborn.h264.RawH264Activity;
import com.langtao.reborn.pack180.LangTao180Activity;
import com.langtao.reborn.pack360.LangTao360Activity;
import com.langtao.reborn.pack720.LangTao720Activity;

/**
 * Created by zzr on 2017/3/4.
 */

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        findViewById(R.id.btn_720).setOnClickListener(this);
        findViewById(R.id.btn_360).setOnClickListener(this);
        findViewById(R.id.btn_180).setOnClickListener(this);
        findViewById(R.id.btn_h264).setOnClickListener(this);

        findViewById(R.id.btn_test_mp).setOnClickListener(this);

        PermissionUtils.requestMultiPermissions(this, mPermissionGrant);
    }

    private PermissionUtils.PermissionGrant mPermissionGrant = new PermissionUtils.PermissionGrant() {

        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case PermissionUtils.CODE_RECORD_AUDIO:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_RECORD_AUDIO", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_GET_ACCOUNTS:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_GET_ACCOUNTS", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_PHONE_STATE:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_READ_PHONE_STATE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_CALL_PHONE:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_CALL_PHONE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_CAMERA:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_CAMERA", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_ACCESS_FINE_LOCATION:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_ACCESS_FINE_LOCATION", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_ACCESS_COARSE_LOCATION:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_ACCESS_COARSE_LOCATION", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(HomeActivity.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_720:
                startActivity(new Intent(HomeActivity.this, LangTao720Activity.class));
                break;
            case R.id.btn_360:
                startActivity(new Intent(HomeActivity.this, LangTao360Activity.class));
                break;
            case R.id.btn_180:
                startActivity(new Intent(HomeActivity.this, LangTao180Activity.class));
                break;
            case R.id.btn_h264:
                startActivity(new Intent(HomeActivity.this, RawH264Activity.class));
                break;
            case R.id.btn_test_mp:
                startActivity(new Intent(HomeActivity.this, TestMultiProcessActivity.class));
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.w("HomeActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("HomeActivity", "onPause");
    }
}
