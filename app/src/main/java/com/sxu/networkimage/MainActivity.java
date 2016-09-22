package com.sxu.networkimage;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.facebook.drawee.backends.pipeline.Fresco;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NetworkImageView imageView = (NetworkImageView) findViewById(R.id.image);
        imageView.displayImage("http://od186sz8s.bkt.clouddn.com/git_icon.png");
    }
}
