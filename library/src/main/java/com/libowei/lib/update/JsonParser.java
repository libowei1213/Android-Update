package com.libowei.lib.update;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by libowei on 2016-09-13.
 */
public class JsonParser {

    /**
     * 解析从服务器返回的json，得到更新信息对象
     *
     * @param json json String
     * @return updateInfo
     */
    public static UpdateInfo parse(String json) {

        UpdateInfo updateInfo = null;

        try {
            JSONObject object = new JSONObject(json);

            String versionName = object.getString(Constants.STR_VERSION_NAME);
            String downloadUrl = object.getString(Constants.STR_DOWNLOAD_URL);
            String updateMsg = object.getString(Constants.STR_UPDATE_MSG);
            updateInfo = new UpdateInfo();
            updateInfo.setDownloadUrl(downloadUrl);
            updateInfo.setVersionName(versionName);

        } catch (JSONException e) {
            //e.printStackTrace();
        }

        return updateInfo;

    }

}
