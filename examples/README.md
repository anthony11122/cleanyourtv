# 机型清单

一个目录一台机器，里面是 `debloat.txt`（可安全禁用的包，已分组加注释）。这些都是起点，不是标准答案：你用得到的应用，把那行注释掉。

清单只对**同固件**负责。同一个型号换批次、换固件版本，包名和包数量都可能变，先备份再动手。

| 目录 | 机型 | 系统 | 说明 |
|---|---|---|---|
| `hisense-vidaa-mt9653-cn/` | VIDAA_TV（本次实测机，国内 JuUI） | Android 11 / SDK 30，`RP1A.200720.011 / 02.02.70.01` | 本仓库的主力清单，29 个包：广告 SDK、遥测、推送、零售/酒店/工厂、预装内容应用、广告屏保、第二个「桌面」、原桌面、OTA 三件套。`com.hisense.tv.tiletransition` 不在里面（禁了输入源切不过去）；原桌面只在新桌面成为 HOME 后才能禁。详见 [实测记录](../docs/hisense/vidaa-mt9653.md) |
| `hisense-e5q-pro/` | 海信 E5Q Pro（内部型号同为 VIDAA_TV，MT9653） | Android TV 11，`02.02.70.01` | 和上面同 build 的保守版：只清广告、遥测、推送、预装内容，**保留原桌面和 OTA**。不想换桌面、只想少点广告就用这份，18 个包 |
| `vidda-mt9653/` | Vidda（海信子品牌，MT9653 / mt5879） | Android TV 11，`RP1A.200720.011 / 02.02.70.01` | 同固件的激进版：连原桌面一起禁，另含第二个桌面 `com.keylab.speech.view.vidaa`（开机按组件名抢前台，只能整包禁，副作用是语音 UI 和麦克风键一起没了）。换桌面时用这份 |
| `hisense-hismarttv-a4/` | 海信 HiSmartTV A4（xiaoyushan，MStar m7322），印度版 | Android TV 9，`PTMR.190127.037` | 日志上报、赞助首页行、门店演示、预装自动安装；OTA 保留。USB 调试直接开 5555，没有无线调试开关 |
| `hisense-hismarttv-a4-android11/` | 海信 HiSmartTV A4（xiaoyushan，m7322） | Android TV 11，`RTT2.221108.001` | 上面那台的 Android 11 版，包名换成 FFM 命名空间：ACR（evservice）、推送、VIDAA Channels、HiAlexa、酒店模式、Google 合作方追踪；OTA / 投屏 / 助手保留 |
| `hisense-hg9/` | 海信 HG9（HiSmart 4K A4FFM，laohushan） | Android TV 9，`PTMR.190127.037` | ACR 采集、Smartsense、日志上报、门店模式、B2B；原桌面用 FLauncher 顶替（不是本仓库的桌面） |
| `hisense-smarttv-4k-ffm/` | 海信 SmartTV 4K FFM（huangshan），拉美；美国同款 U9DG | Android TV 10，`QTG3.200902.001` | ACR（evservice）、VIDAA 预装、门店/酒店模式、Google 合作方追踪；`smartsense` 必须留着。输入源键是 `KEYCODE_TV_INPUT`，只有原桌面应答，所以配置里要开 `handle_view_inputs` |
| `hisense-smarttv-4k-ffm-android11/` | 海信 SmartTV 4K FFM（songshan，m7632） | Android 11 / SDK 30，`RTT2.220118.001` | 同名不同板：门店演示、首次体验、Google 合作方自动安装；换桌面要先处理 setupwraith（它占着一个没用上的已保存网络，先查 `dumpsys wifi`）。遥控器应用键是自定义键码（YouTube 293、Prime 295） |
| `hisense-smarttv-4k-ffm-lushan/` | 海信 SmartTV 4K FFM（lushan，MT9676 / mt5896），蒙古 | Google TV 12，`STT2.230929.001.092` | 第三块 FFM 板：ACR、Google 合作方追踪、海信浏览器、家庭时光、门店模式；`smartsense` / AIoT 一律不碰。setupwraith 必须禁（RecoveryActivity 抢 HOME），遥控器按键是 scancode（YouTube 469、HDMI 362、应用 580、字幕 370） |
| `hisense-u75-hengshan/` | 海信 U75（hengshan），拉美 / 巴西 | Google TV 12，`STT2.230310.002` | ACR、VIDAA Channels Plus、家庭时光、门店/酒店/工厂、Ginga 中间件、Google 自动安装；OTA、投屏、流媒体全保留。`smartsense` / AIoT 必须留着 |
| `hisense-fire-tv-aftcibfbd1/` | 海信 Fire TV（AFTCIBFBD1，coyote），美国 | Fire OS 8.0 / Android 11 | Fire OS 只能清不能换桌面（`com.amazon.tv.launcher` 受保护）。Amazon ACR、广告推送、购物、演示、遥测；Alexa 语音每次开机都会被固件重新启用，禁不掉；HDMI 切换是 Amazon 私有接口 |
| `coocaa-k3-pro-90p3d-max/` | 酷开 K3 Pro 90P3D Max（8R145_P3DM，Realtek RTD2885N），国内 | Android 11 / SDK 30，Coocaa OS | 海信系同平台：酷开广告 SDK、ACR、遥测、推送、静默安装、内容应用；`com.tianci.movieplatform` **绝对不能禁**（禁了卡 LOGO，只能恢复出厂）。Home 键硬编码到酷开桌面，无开机广播 |
| `haier-android-tv-dvb2k/` | 海尔 Android TV DVB2K（broadway，HaierATVT56DVB） | Android TV 11，`RTMA.250416.179` | 同平台参考清单：Anoki ACR、门店演示、海尔内容应用和自动安装；1 GB 内存、720p。setupwraith 保留 |
| `haier-h32s80efx/` | 海尔 H32S80EFX（HRT56EU，deto，MediaTek），泰国 | Google TV 14，`UKNV.260302.001` | 同平台参考清单：海尔 hOn 物联网服务、Haier 页签、门店演示、清理器；setupwraith 必须禁（组件级禁用被拒）。Live TV 要指向数字调谐器 |

## 用法

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50

tools/disable.sh examples/hisense-vidaa-mt9653-cn/debloat.txt     # 应用
tools/restore.sh examples/hisense-vidaa-mt9653-cn/debloat.txt     # 撤销
tools/venv/bin/python -I tools/check.py examples/hisense-vidaa-mt9653-cn/debloat.txt   # 固件更新后看漂移
```

清单是普通文本，一行一个包名，`#` 开头是注释，行尾也可以写注释。

## 自己加一台

1. 备份：`pm list packages -f`、`pm list packages -d`、`getprop`、`settings list global`；
2. 按分组加包，每加一批重启一次，确认开机、网络、遥控器、输入源都正常；
3. 头部写清机型、芯片、固件版本、哪些包不能禁、哪些是自愿保留；
4. 用 `tools/check.py` 确认重启后没有包被固件拉回来。
