package update;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.libowei.lib.update.Constants;
import com.libowei.lib.update.DownUtil;
import com.libowei.lib.update.UpdateInfo;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by libowei on 2016-09-13.
 */
public class UpdateManager {

    private Context context;
    private String curVersion = null;
    private Integer curVersionCode = -1;
    private UpdateHandler handler = null;

    UpdateInfo updateInfo;
    DownUtil downUtil;
    ProgressDialog progressDialog;

    /**
     * 构造函数
     *
     * @param context
     */
    public UpdateManager(Context context) {
        this.context = context;
        this.handler = new UpdateHandler();
    }

    /**
     * 检查更新
     *
     * @param url
     */
    public void check(final String url) {

        new Thread() {
            @Override
            public void run() {

                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(5 * 1000);
                    conn.connect();
                    // 连接成功
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                        String response = IOUtils.toString(conn.getInputStream());

                        Log.i("updateManager", response);

                        updateInfo = parse(response);
                        if (updateInfo == null) {
                            handler.sendEmptyMessage(Constants.MSG_PARSE_ERROR);
                            return;
                        }
                        // 判断版本号
                        String currentVersionName = getVersionName();
                        int currentVersionCode = getVersionCode();

                        if (currentVersionName != null && currentVersionCode != -1) {
                            if (!currentVersionName.equals(updateInfo.getVersionName())
                                    && currentVersionCode < updateInfo.getVersionCode()) {

                                // 版本号不一致->有更新
                                if (updateInfo.getDownloadUrl() == null || "".equals(updateInfo.getDownloadUrl())) {
                                    // 没有下载url
                                    handler.sendEmptyMessage(Constants.MSG_PARSE_ERROR);
                                    return;
                                }
                                // 有更新, 发送Message
                                handler.sendEmptyMessage(Constants.MSG_NEW_UPDATE);

                            } else {
                                handler.sendEmptyMessage(Constants.MSG_NO_UPDATE);

                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    /**
     * 当前版本名称
     */
    private String getVersionName() {

        if (this.curVersion != null) {
            return this.curVersion;
        }
        try {
            this.curVersion = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
            return this.curVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getVersionCode() {

        if (this.curVersionCode != -1) {
            return this.curVersionCode;
        }
        try {
            this.curVersionCode = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionCode;
            return this.curVersionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 显示更新提示框
     */
    private void showUpdateNotice() {

        String title = "发现新版本：" + updateInfo.getVersionName();

        StringBuilder dialogText = new StringBuilder();
        String updateMsg = updateInfo.getUpdateMsg();
        if (updateMsg != null && !"".equals(updateMsg)) {
            dialogText.append("更新说明：");
            dialogText.append(updateMsg);
        }

        new AlertDialog.Builder(context).setTitle(title).setMessage(dialogText)
                .setPositiveButton("开始更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // 开始下载
                        downloadApk();

                    }
                }).setNegativeButton("下次再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();

    }

    /**
     * 下载apk文件
     */
    private void downloadApk() {

        String path = this.context.getExternalCacheDir().getPath() + "/" + updateInfo.getVersionName() + ".apk";
        Log.i("UPDATE", path);

        downUtil = new DownUtil(updateInfo.getDownloadUrl(), path);

        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("更新软件");
        progressDialog.show();

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
                                handler.sendEmptyMessage(Constants.MSG_PROGRESS_CANCEL);
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
     * 内部类 UpdateHandler 用于处理更新中的各种msg
     */
    class UpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MSG_NETWORK_ERROR: {
                    // 网络问题
                    Log.e("UPDATE", "网络问题");
                    break;
                }
                case Constants.MSG_NEW_UPDATE: {
                    // 有更新
                    showUpdateNotice();
                    break;
                }
                case Constants.MSG_NO_UPDATE: {
                    // 没有更新
                    Log.e("UPDATE", "没有更新啊");
                    break;
                }
                case Constants.MSG_DOWNLOADING: {
                    progressDialog.setProgress(msg.arg1);
                    break;
                }
                case Constants.MSG_OPEN_APK: {
                    String path = context.getExternalCacheDir().getPath() + "/" + updateInfo.getVersionName() + ".apk";
                    File file = new File(path);
                    openFile(file);
                    break;
                }
                case Constants.MSG_PROGRESS_CANCEL: {
                    progressDialog.cancel();
                    break;
                }

                default: {
                    break;
                }
            }

        }
    }

    /**
     * 解析升级的json数据
     *
     * @param json
     * @return
     */
    private UpdateInfo parse(String json) {

        UpdateInfo updateInfo = null;

        try {
            JSONObject object = new JSONObject(json);

            String versionName = object.getString(Constants.STR_VERSION_NAME);
            String downloadUrl = object.getString(Constants.STR_DOWNLOAD_URL);
            String updateMsg = object.getString(Constants.STR_UPDATE_MSG);
            Integer versionCode = object.getInt(Constants.STR_VERSION_CODE);
            updateInfo = new UpdateInfo();
            updateInfo.setDownloadUrl(downloadUrl);
            updateInfo.setVersionName(versionName);
            updateInfo.setUpdateMsg(updateMsg);
            updateInfo.setVersionCode(versionCode);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return updateInfo;
    }

    private void openFile(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        this.context.startActivity(intent);
    }

}
