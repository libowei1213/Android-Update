# Android-Update

## 简介 ##
本项目是为了方便安卓程序的迭代更新，而开发的检测更新并下载更新apk的库，及其测试Demo

## 如何使用 ##
### 服务端配置 ###
服务端需要返回一个json格式的数据
```javacript
{
  "ver": "3.333",
  "verCode":1,
  "url": "http://od4xu3l0l.bkt.clouddn.com/libowei.apk",
  "msg": "测试需要"
}
```
该json中需包含新版本的版本号、版本名、下载地址及更新说明

### 检测规则 ###
**json中的版本号大于本地版本号**且**json中版本名与本地版本名不同**，则进行更新。

### 添加到程序中 ###

在AndroidManifest.xml中添加以下权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

在Activity中添加以下代码
```java
UpdateManager updateManager = new UpdateManager(this);
updateManager.check("http://od4xu3l0l.bkt.clouddn.com/b.json");
```


## 效果展示 ##
![demo](http://odszv0fof.bkt.clouddn.com/android-update-1.png)


![demo](http://odszv0fof.bkt.clouddn.com/android-update-2.png)