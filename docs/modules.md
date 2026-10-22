# 写一个模块

模块就是一个普通 Android TV 应用，向桌面声明一张卡片。仓库、协议、签名密钥都是你自己的，APK 发在哪都行（比如 GitHub Releases，旁边附 SHA-256）。电视主人像装普通应用一样侧载，桌面发现卡片后会问一次；回答「显示」之后，你的应用就作为一张卡片出现：你的名字、可选的副标题、你的主题色光晕，排在字母网格之前。

契约在 [`docs/specs/card.md`](specs/card.md)，这篇是简版。

## 1. 声明卡片

在已经有 `LEANBACK_LAUNCHER` 过滤器的 Activity 上，再加一个过滤器和一条 `meta-data`：

```xml
<activity android:name=".LibraryActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="com.gathin.tvhome.category.DOCK_CARD" />
    </intent-filter>
    <meta-data android:name="com.gathin.tvhome.card" android:resource="@xml/module_card" />
</activity>
```

再建 `res/xml/module_card.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<card spec="1" label="Retro" caption="NES games" accent="#E8A33D" />
```

- `label`：1 到 24 个字符，只允许字母、数字、空格和 `. & + ' -`。写短点，它是图标上和展台上的名字。「verified」「official」这类词、桌面自己的 UI 名字、HDMI 这类输入源名字都会被拒；新卡片的 label（或者应用自己的名字）跟电视上另一个已装应用同名也会被拒。更新时改 label 会让主人重新确认一次，所以一次定好。
- `caption`：可选，最多 32 个字符，卡片聚焦时显示在 label 后面。图标下面只有一行，4 列布局时 label + caption 大约只能显示 26 个字符，所以副标题写几个词就好。
- `accent`：可选 `#RRGGBB`，卡片在停靠栏聚焦时背后的光晕颜色。
- 值写纯文本，spec 1 不解析 `@string` 引用。

卡片必须挂在带 `LEANBACK_LAUNCHER` 的那个 Activity 上，而且这个 Activity 要 exported 且 enabled，否则卡片会被丢掉。

## 2. 保持轻量

桌面面向 1 GB 内存的电视，而主人是要把它放在主屏幕上的。

- 不需要后台服务就别开，开了要在 README 里写明。
- 没有用户同意就不要遥测。不要放广告。
- 不要打包你没有分发权的内容。
- 用自己的图。别拿别家公司的 logo 当 banner 或图标。
- 每个申请的权限都要在 README 里解释。

## 3. 在电视上测

ADB 装上去，按 Home，弹窗就会出来：

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell input keyevent KEYCODE_HOME
```

（`adb` 二进制被拦的环境用本仓库的 `tools/tv.py`。）

几个有用的检查：

```bash
# 卡片过滤器和 meta-data 是不是落在对的 Activity 上
adb shell pm dump io.github.udhaykumarbala.retro | grep -B2 -A6 DOCK_CARD

# 卡片为什么被拒？一行一个原因
adb logcat -s TvHomeCards
```

然后依次试：回答「保持隐藏」，确认应用还是普通图标；长按设置 → 模块卡片列表 → 改成「显示」，确认卡片顶替了图标；点卡片确认能打开你的应用；卸载后确认卡片消失。给 README 截一张卡片在网格里聚焦的图。

## 4. 发布并守住密钥

主人的回答是钉在你的签名证书上的。每次发布都把证书摘要放出来，别人才能核对装的是什么：

```bash
apksigner verify -print-certs app-release.apk | grep 'SHA-256'
```

（`apksigner` 的参数写一个横线两个横线都行。）

「Signer #1 certificate SHA-256 digest」那一行就是桌面在电视上算的东西。每次更新都用同一把密钥签：换了密钥，每台电视都会重新问一次，在主人回答之前卡片一直是隐藏的。只用一个证书签名，多签名的 APK 会被拒。keystore 不要进仓库。

APK 自己的 SHA-256 也一起发出来。

## 参考模块

Retro（一个电视上的 NES 游戏库）就是按上面的方式声明卡片的：<https://github.com/udhaykumarbala/tv-vibehack-retro>。

想读别的应用的「继续观看」行的桌面，见 [`docs/watch-next.md`](watch-next.md)。
