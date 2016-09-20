package com.libowei.lib.update;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by libowei on 2016-09-13.
 * 单线程下载类
 */
public class DownUtil {

    private static final String TAG = "DownUtil";

    private String path;
    private String targetFile;
    private int fileSize = 0;
    private int downSize = 0;
    private boolean terminate = false;

    public DownUtil(String path, String targetFile) {
        this.path = path;
        this.targetFile = targetFile;
    }

    public void download() throws Exception {

        new Thread() {

            URLConnection conn;
            InputStream is;
            FileOutputStream fos;

            @Override
            public void run() {

                try {
                    URL url = new URL(path);
                    conn = url.openConnection();
                    if (conn.getReadTimeout() == 5) {
                        Log.i(TAG, "当前网络有问题");
                        return;
                    }
                    is = conn.getInputStream();


                    fos = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[1024];
                    fileSize = conn.getContentLength();
                    int len;
                    while (!terminate && downSize < fileSize) {
                        if ((len = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                            downSize += len;
                            //Log.i(TAG, downSize + "");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    /**
     * 下载进度
     *
     * @return
     */
    public int getCompleteRate() {
        if (fileSize == 0) return 0;
        return downSize * 100 / fileSize;
    }

}
