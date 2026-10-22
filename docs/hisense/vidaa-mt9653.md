# VIDAA_TV / MT9653 实测记录

本文是这次真机清理的完整记录。机型、包清单、按键映射、广告与更新链路都按这台机器写，换一台同型号但固件版本不同的电视，包名可能对不上，先备份再对照。

## 机器信息

| 项 | 值 |
|---|---|
| 型号字符串 | `VIDAA_TV`（国内 JuUI / 聚好看固件，设置里型号就叫这个） |
| 芯片 | MediaTek MT9653 |
| 板号 | `mt5879`（`ro.board.platform` 报 `mt9653`，两个都对） |
| 系统 | Android 11，SDK 30 |
| 固件 build | `RP1A.200720.011`，版本号 `02.02.70.01` |
| 安全补丁 | 2022-09-05 |
| 内存 / 存储 | 3.8 GB RAM，41 GB 可用 |
| 显示 | 1080p，320 dpi |
| ADB | `ro.adb.secure=0`，开 USB 调试即监听 5555，无确认弹窗 |

查一遍确认：

```bash
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py --probe
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'getprop ro.product.model'
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'getprop ro.board.platform'
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'getprop ro.build.display.id'
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'getprop ro.adb.secure'
```

预期：

```
VIDAA_TV
mt9653
RP1A.200720.011
0
```

## 29 个禁用包

清单文件：[`examples/hisense-vidaa-mt9653-cn/debloat.txt`](../../examples/hisense-vidaa-mt9653-cn/debloat.txt)

| 分组 | 包 |
|---|---|
| AOSP 遗留 | `com.android.printspooler` |
| 广告与运营位 | `com.hisense.business.ad`、`com.hisense.service.ad`、`com.hisense.widget.pro` |
| 遥测 / 日志 / 推送 | `com.hisense.hitv.das`、`com.hisense.platformreport`、`com.hisense.logupload`、`com.hisense.service.hproxy`、`com.hisense.service.message` |
| 零售 / 酒店 / 工厂 / 说明书 | `com.hisense.demonstrate`、`com.hisense.hotelmenu`、`com.hisense.autotest`、`com.hisense.electronictutorial`、`com.hisense.ar.baike`、`com.meetingroom` |
| 预装内容应用 | `com.hisense.tv.fitmirror`、`com.hisense.tv.foodmanage`、`com.hisense.tv.videochat.social`、`com.hisense.tv.videoconference`、`com.hisense.tv.weather`、`com.vision.aibook`、`com.jamdeo.calendar` |
| 首次开机向导 / 屏保 | `com.jamdeo.tv.fte`、`com.jamdeo.screensaver` |
| 第二个「桌面」 | `com.keylab.speech.view.vidaa` |
| 原桌面 | `com.jamdeo.tv.vod` |
| OTA 三件套 | `com.hisense.playerupdate`、`com.hisense.usbupgrade`、`com.jamdeo.updateservice` |

屏保在这版固件上就是广告位（拍信创意的图库 + 文案 + 点赞按钮），禁包之外再关掉开关：

```bash
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_enabled 0'
```

## 必须保留的包

| 包 | 作用 |
|---|---|
| `com.jamdeo.tv.livetv` | 有线电视和 HDMI/AV 播放 |
| `com.jamdeo.tv.service` | TVServices，输入切换的 TIL/JNI 一半 |
| `com.hisense.tv.inputs` | SysUIMerge，输入源面板和任务中心 |
| `com.hisense.tv.baseservice` | 电视基础服务 |
| `com.hisense.tv.tiletransition` | `TileAnimationService`，输入源面板切输入前必须绑它 |
| `com.hisense.bluetoothpair` | 遥控器配对，禁了可能没遥控器可用 |
| `com.sohu.inputmethod.sogou.tv` | 机器上唯一的输入法 |
| `com.hisense.padinputmethod` | 备用输入法 |
| `com.hisense.raycodereceiver` | 疑似遥控器通道，进程里常驻 |
| `com.hisense.uifac` | 工厂 UI 组件 |
| `com.hisense.systeminfo` | 系统信息 provider |
| `com.hisense.hiservice` | MQTT 基础服务 |
| `com.hisense.aiod` / `.aiservice` | AI 画质（AIPQ）和手势 |
| `com.keylab.speech.core.vidaa` | 语音核心，语音 UI 禁了它也留着 |
| `com.vt.mic.service` / `.point.remoteservice` | 麦克风和空鼠 |
| `com.dolby.android.audio.service` / `.audio.calibration` | 杜比音效 |

`com.jamdeo.tv.vod` 不在「必须保留」里，但**顺序不能错**：新桌面起来并成为 HOME 之后才能禁它。它是这台机器上唯一带开机广播的桌面。

## Home 键映射

| 层 | 值 |
|---|---|
| 物理按键 | Linux scancode `102` |
| 厂商 keylayout | 映射成 `KEYCODE_F12`（142），不是 `KEYCODE_HOME`（3） |
| 原接收者 | 厂商 SysUIMerge，用来切「上一个应用」；原桌面在时表现为回桌面 |

原桌面一禁，这个键就没有目标了。两个办法：

```bash
# 厂商属性：告诉 VIDAA 框架 Home 该交给谁，persist. 前缀重启不丢
tools/venv/bin/python -I tools/tv.py 'setprop persist.his.key_home_component com.gathin.tvhome/.MainActivity'
tools/venv/bin/python -I tools/tv.py 'getprop persist.his.key_home_component'

# 或者打开桌面的按键辅助服务
HOME_KEY_SERVICE=1 STOCK_LAUNCHER=com.jamdeo.tv.vod launcher/build-and-install.sh
```

`persist.his.key_home_component` 在 02.02.70.01 上有效，部分更晚的 02.02.x 固件不认这个属性，那就只能靠按键辅助服务。

## 输入源

这台机器 `dumpsys tv_input` 是空的（没有 TvInputManager 输入），桌面走厂商自己的面板：

| 项 | 值 |
|---|---|
| 服务 | `com.hisense.tv.inputs/com.hisense.tv.InputsUIService` |
| action | `com.hisense.tv.start_inputs` |
| 依赖 | `com.hisense.tv.tiletransition` 的 `TileAnimationService` |
| 遥控器按键 | `KEYCODE_TV_INPUT`（178） |

主题 `config.json` 写法：

```json
{
  "source_panel_service": "com.hisense.tv.inputs/com.hisense.tv.InputsUIService",
  "source_panel_action": "com.hisense.tv.start_inputs"
}
```

面板能开但选 HDMI 没反应，就是 tiletransition 被禁了，`pm enable` 回来立刻恢复。

## ad_block 是谁在改

```bash
tools/venv/bin/python -I tools/tv.py 'settings put global com.hisense.service.ad_block 1'
tools/venv/bin/python -I tools/tv.py 'settings get global com.hisense.service.ad_block'
```

写进去是 `1`，重启后变回 `0`，改它的是 `com.jamdeo.updateservice`——它在开机流程里重写这个开关。要长期生效只能禁 updateservice，代价是停更。

## 开机广告

开机视频和开机动画走系统的 `bootanimation.zip`，在 `/system/media/` 下，只读分区，不刷机改不了。能做的两件事：

1. **不改分区，用 `/data/local` 覆盖**：把自定义的 `bootanimation.zip` 推到 `/data/local/bootanimation.zip`，部分 MTK 固件优先读这里。这条不保证生效，也不保证安全，改之前先备份原文件，改坏了删掉就恢复。

```bash
tools/venv/bin/python -I tools/tv.py 'ls -l /system/media/bootanimation.zip'
tools/venv/bin/python -I tools/tv.py --push my-bootanimation.zip /data/local/bootanimation.zip
# 撤销
tools/venv/bin/python -I tools/tv.py 'rm /data/local/bootanimation.zip'
```

2. **接受它**：开机动画只有几秒，桌面起来之后就再也看不到广告了。真正烦人的是桌面里的运营位和贴片广告，那些换桌面 + 禁广告包就解决了。

固件里的开机视频如果放在 `/data` 或某个应用的数据目录（部分批次是 `com.hisense.demonstrate` 或 updateservice 拉下来的），禁掉对应包之后就不会再出现；位置和文件名各批次不一样，用 `find /data -iname '*boot*video*'` 自己找，别照抄路径。

## 验证

清理完、重启一次，逐条对：

```bash
# 1. 禁用列表：应该正好 29 个（不含出厂就禁用的那些）
tools/venv/bin/python -I tools/tv.py 'pm list packages -d' | grep -cE 'hisense|jamdeo|keylab|vision|meetingroom|printspooler'

# 2. 关键包还在
tools/venv/bin/python -I tools/tv.py 'pm list packages' | grep -E 'jamdeo.tv.(livetv|service)|hisense.tv.(inputs|baseservice|tiletransition)'

# 3. Home 是谁
tools/venv/bin/python -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'

# 4. 前台是谁
tools/venv/bin/python -I tools/tv.py 'dumpsys window | grep mCurrentFocus'

# 5. 网络还在（ADB 没把自己玩掉）
tools/venv/bin/python -I tools/tv.py 'dumpsys connectivity | grep -o "Transports: [A-Z]*" | head -3'
```

预期：第 1 条 ≥ 29；第 2 条五个包都在；第 3 条输出 `com.gathin.tvhome/.MainActivity`；第 4 条是 `com.gathin.tvhome/com.gathin.tvhome.MainActivity`；第 5 条含 `WIFI`。

再看一眼有没有包被固件自己拉回来：

```bash
tools/venv/bin/python -I tools/check.py examples/hisense-vidaa-mt9653-cn/debloat.txt
```

输出 `in sync` 并退出 0 就是稳了；列出漂移的包就是固件开机时重新启用了它们，加进定时任务每周跑一次。
