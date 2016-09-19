package com.libowei.lib.update;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.loopj.android.http.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;
import me.drakeet.materialdialog.MaterialDialog;

/**
 * Created by libowei on 2016-09-13.
 */
public class UpdateManager {


    private Context context;
    private String curVersion = null;
    private UpdateHandler handler = null;


    UpdateInfo updateInfo;
    MaterialDialog downloadDialog;
    DownUtil downUtil;

    /**
     * 构造函数
     *
     * @param context
     */
    public UpdateManager(Context context) {
        this.context = context;
        this.handler = new UpdateHandler();
    }


    public void check(String url) {

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                String json = new String(response);

                Log.i("UPDATE", json);

                updateInfo = JsonParser.parse(json);
                if (updateInfo == null) {
                    handler.sendEmptyMessage(Constants.MSG_PARSE_ERROR);
                    return;
                }
                //判断版本号
                if (getVersionName().equals(updateInfo.getVersionName())) {
                    //版本号一致->没有更新
                    handler.sendEmptyMessage(Constants.MSG_NO_UPDATE);
                } else {
                    //版本号不一致->有更新
                    if (updateInfo.getDownloadUrl() == null || "".equals(updateInfo.getDownloadUrl())) {
                        //没有下载url
                        handler.sendEmptyMessage(Constants.MSG_PARSE_ERROR);
                        return;
                    }

                    //有更新, 发送Message
                    handler.sendEmptyMessage(Constants.MSG_NEW_UPDATE);

                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                //传递网络错误Message
                handler.sendEmptyMessage(Constants.MSG_NETWORK_ERROR);
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }


    /**
     * 获取当前版本名称
     *
     * @return
     */
    private String getVersionName() {

        if (this.curVersion != null) {
            return this.curVersion;
        }

        String versionName = null;
        try {
            this.curVersion = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
            return this.curVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void showDialog() {

        StringBuilder dialogText = new StringBuilder();
        dialogText.append("最新版本：" + updateInfo.getVersionName());
        dialogText.append("\n");
        dialogText.append("当前版本：" + updateInfo.getVersionName());
        dialogText.append("\n");
        String updateMsg = updateInfo.getUpdateMsg();
        if (updateMsg != null && !"".equals(updateMsg)) {
            dialogText.append("更新说明：\n");
            dialogText.append(updateMsg);
        }

        MaterialDialog dialog = new MaterialDialog(this.context);

        dialog.setTitle("版本更新").setMessage(dialogText).setPositiveButton("开始更新", new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        }).setNegativeButton("下次再说", new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        }).show();
    }

    /**
     * 下载apk文件
     */
    private void downloadApk() {

        String path = this.context.getExternalCacheDir().getPath() + "/" + updateInfo.getVersionName() + ".apk";

        //String path = Environment.getExternalStorageDirectory() + "/a.apk";

        Log.i("UPDATE", path);

        downUtil = new DownUtil(updateInfo.getDownloadUrl(), path);


        boolean showMinMax = true;
        downloadDialog = new MaterialDialog(this.context)
                .setTitle("更新软件")
                .setMessage("下载中");
        downloadDialog.show();

//        while (dialog.getCurrentProgress() != dialog.getMaxProgress()) {
//            // If the progress dialog is cancelled (the user closes it before it's done), break the loop
//            if (dialog.isCancelled()) break;
//            // Wait 50 milliseconds to simulate doing work that requires progress
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                break;
//            }
//            // Increment the dialog's progress by 1 after sleeping for 50ms
//            dialog.incrementProgress(1);
//        }


        new Thread() {
            @Override
            public void run() {
                try {
                    downUtil.download();


                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            int completeRate = downUtil.getCompleteRate();

                            Message msg = handler.obtainMessage();
                            msg.arg1 = completeRate;
                            msg.what = Constants.MSG_DOWNLOADING;
                            handler.sendMessage(msg);

                            if (completeRate >= 100) {
                                timer.cancel();
                                handler.sendEmptyMessage(Constants.MSG_OPEN_APK);
                            }

                        }
                    }, 0, 100);


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();

    }


    /**
     * 内部类 UpdateHandler
     * 用于处理更新中的各种msg
     */
    class UpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MSG_NETWORK_ERROR: {
                    //网络问题
                    Log.e("UPDATE", "网络问题");
                    break;
                }
                case Constants.MSG_NEW_UPDATE: {
                    //有更新
                    downloadApk();
                    break;
                }
                case Constants.MSG_NO_UPDATE: {
                    //没有更新
                    Log.e("UPDATE", "没有更新啊");
                    break;
                }
                case Constants.MSG_DOWNLOADING: {
                    downloadDialog.setMessage("进度：" + msg.arg1 + "%");
                    break;
                }
                case Constants.MSG_OPEN_APK: {
                    String path = context.getExternalCacheDir().getPath() + "/" + updateInfo.getVersionName() + ".apk";
                    File file = new File(path);
                    openFile(file);
                    break;
                }
                default: {
                    break;
                }
            }

        }
    }


    private void openFile(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        this.context.startActivity(intent);
    }

}
