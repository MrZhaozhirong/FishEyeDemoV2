package com.langtao.reborn;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

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

    }

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
