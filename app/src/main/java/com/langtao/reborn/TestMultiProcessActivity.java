package com.langtao.reborn;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by zzr on 2018/4/12.
 */

public class TestMultiProcessActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String test = null;
                test.compareTo("asdf");
            }
        },3000);
    }



}
