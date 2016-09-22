package com.sxu.networkimage;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

/*******************************************************************************
 * FileName: NetworkImageApplication
 * <p>
 * Description:
 * <p>
 * Author: juhg
 * <p>
 * Version: v1.0
 * <p>
 * Date: 16/9/22
 * <p>
 * Copyright: all rights reserved by zhinanmao.
 *******************************************************************************/
public class NetworkImageApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}

