package com.libowei.updateDemo;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

import com.libowei.lib.update.UpdateManager;

/**
 * Created by li on 2016/9/19.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //测试Library

        UpdateManager updateManager = new UpdateManager(this);
        updateManager.check("http://od4xu3l0l.bkt.clouddn.com/b.json");


    }
}
