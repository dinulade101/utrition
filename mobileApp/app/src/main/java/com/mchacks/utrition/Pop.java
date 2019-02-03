package com.mchacks.utrition;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;

class Pop extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popwindow);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        getWindow().setLayout(dm.widthPixels, (int)(dm.heightPixels*0.6));

    }
}
