# 海信电视清理工具

基于 [CleanYourTV](https://github.com/Gathin23/cleanyourtv) 改的海信专版：清单、文档、Release 只针对海信和 VIDAA 系机型，脚本和桌面程序沿用上游。

海信国内版（VIDAA / 聚好看 JuUI）出厂就带着开机广告、贴片广告、推送通道、遥测上报和十几个用不上的预装应用，桌面还是那套塞满运营位和 VIP 横幅的聚好看。这个工具用一根网线都不用：电视和电脑连同一个 WiFi，电脑通过 ADB 把该禁的包禁掉、把该关的开关关掉，再装一个干净的桌面顶替原桌面。全程不 root、不刷机、不卸载任何系统应用。

代价先说清楚，别装完再后悔：

- 禁掉 OTA 三件套就再也收不到固件和安全补丁，想恢复只能刷机；
- 海信这批机器 `ro.adb.secure=0`，开了 USB 调试就等于在 5555 端口上开了一个不需要密码的 shell，同一个局域网里任何设备都能连进来，用完必须关掉；
- 桌面配置写坏、无障碍服务掉了，都会表现为「遥控器没反应」，救回来要敲命令，不是点两下就好。

## 这个工具做什么

1. 用 `pm disable-user` 禁掉海信的广告 SDK、推送、遥测、运营位、酒店/演示模式和预装内容应用，包还在，只是不对当前用户生效。
2. 装一个约 470 KB 的原生桌面，接管 Home，把聚好看那套运营位和 VIP 横幅整个换掉。
3. 每一步都有对应的还原路径：`tools/restore.sh` 按改动前的记录逐个 `pm enable`，桌面也能从系统设置里直接卸掉。

## 支持机型

只保留海信、Vidda、酷开、海尔这几家共用同一批芯片方案的机型。清单都来自真机，一台一份。

| 型号 | 芯片 | 系统 | 已知问题 |
|---|---|---|---|
| VIDAA_TV（本次实测机，国内 JuUI） | MT9653 / 板号 mt5879 | Android 11（SDK 30），build `RP1A.200720.011 / 02.02.70.01` | 原桌面一禁就黑屏，靠桌面内置的开机自启兜底；Home 键是 scancode 102 → F12；输入源面板依赖 `com.hisense.tv.tiletransition` |
| Vidda（海信子品牌，同 build） | MT9653 | Android TV 11 | 与上一行同固件，清单更激进：连原桌面一起禁；`com.keylab.speech.view.vidaa` 会在开机时按组件名抢前台 |
| HiSmartTV A4（xiaoyushan） | MStar m7322 | Android TV 9 / Android 11（RTT2.221108.001） | Android 11 那版换成 FFM 命名空间；输入源键只有原桌面应答 |
| SmartTV 4K FFM（huangshan / songshan / lushan） | MediaTek m7632 / MT9676 | Android TV 10 / 11 / Google TV 12 | 三块板子包名不同，别混用清单；`smartsense` / AIoT 动了有 Recovery 风险 |
| E5Q Pro（VIDAA_TV） | MT9653 | Android TV 11 | 与实测机同 build，清单保守，保留原桌面和 OTA |
| U75（hengshan） | MediaTek | Google TV 12 | 国内无对应固件，清单按拉美版整理 |
| 酷开 K3 Pro 90P3D Max | Realtek RTD2885N | Android 11（Coocaa OS） | 海信系同平台机型；`com.tianci.movieplatform` 绝对不能禁，禁了卡 LOGO 只能恢复出厂 |
| 海尔 DVB2K / H32S80EFX | HaierATVT56DVB / HRT56EU | Android TV 11 / Google TV 14 | 同平台参考清单，没在海信机器上验证过 |

同一个芯片、不同批次的固件，包名和包数量都可能不一样。**先备份再动手**，别拿别人的清单直接全量套。

## 开始之前

需要准备的东西：

- 一台和电视连同一个 WiFi 的电脑（Windows、macOS、Linux 都行），装好 Python 3；
- 电视的 IP 地址：设置 → 网络 → 有线/无线网络，点开当前连接就能看到；
- 电视上打开 USB 调试：**设置 → 关于 → 连按「版本号」7 次 → 返回 → 开发者选项 → USB 调试**。海信国内版有的固件把「开发者选项」藏在「关于 → 本机信息」里，找不到就把关于页面翻一遍。

打开 USB 调试后，海信这批机器通常直接监听 5555，不需要无线配对，也不会弹「允许 USB 调试吗」。这是因为 `ro.adb.secure=0`：**任何能连到这台电视的设备都能直接拿到 shell，没有确认弹窗**。所以：

- 只在自家可信网络里开，别在酒店、宿舍、公司访客 WiFi 下开；
- 干完活立刻关掉（开发者选项里关 USB 调试，或直接重启电视），然后在电脑上确认端口已经关掉：

```bash
python3 tools/adb-closed.py 192.168.1.50
```

输出里出现 `is closed` 才算真的关了（`... is closed: the TV refused the connection.`）。用 `nc -vz` 的话只有「Connection refused」算关，报其他错误不算。

## 三步上手

先装依赖，后面所有命令都在仓库根目录执行：

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50        # 换成你家电视的 IP
```

| 步骤 | 干什么 | 命令 |
|---|---|---|
| **1. 备份** | 把包列表、禁用列表、`getprop`、各项 settings 全部存下来，出问题照着重放 | `tools/venv/bin/python -I tools/tv.py --probe`<br>`tools/venv/bin/python -I tools/tv.py 'pm list packages -f' > backup/packages.txt`<br>`tools/venv/bin/python -I tools/tv.py 'settings list global' > backup/settings-global.txt` |
| **2. 清理** | 按机型清单批量禁用，脚本会先写还原记录再动手 | `tools/disable.sh examples/hisense-vidaa-mt9653-cn/debloat.txt` |
| **3. 装桌面** | 编译、安装、抢 Home，失败会自动回滚 | `STOCK_LAUNCHER=com.jamdeo.tv.vod HOME_KEY_SERVICE=1 launcher/build-and-install.sh` |

三步之间建议重启一次电视，确认「还能开机、还能连网、遥控器还能用」，再往下走。清单里的包是分批验证过的，你自己加包时也分批加。

装完桌面、确认新桌面已经是 Home 之后，再把原桌面禁掉：

```bash
tools/venv/bin/python -I tools/tv.py 'pm disable-user --user 0 com.jamdeo.tv.vod'
```

顺序不能反。原桌面是这台机器上唯一带开机广播的桌面，先禁它再装新桌面，中间那次重启就是黑屏。

## 桌面功能

桌面是纯 Java 写的原生应用，没有网络权限、没有后台常驻服务、没有模糊和视频，1 GB 内存的机器也跑得动。

| 功能 | 说明 |
|---|---|
| 应用聚焦 | 焦点所在的应用图标放大、显示名字，背景浮起一层取自应用图标的柔光 |
| 应用网格 | 4 / 5 / 6 / 7 列可调，列数变了图标自动缩放，不用重编 |
| 遥控器排序 | 长按 OK 进入菜单，可以移动位置、从桌面移除、看应用信息；顺序存在电视上 |
| 隐藏应用 | 默认隐藏系统应用（处理「设置」的包和从应用库 Add to Home 过的除外）；遥控器可在桌面长按图标选「Hide app」，或在应用库里 Hide / Unhide |
| 时钟屏保 | 一块细字时钟在柔光上缓慢漂移，防烧屏；在系统屏保设置里选「时钟」即可 |
| 输入源面板 | 右上角输入源按钮，列出 HDMI 口（带是否插着设备的标记）、AV、有线电视 |
| 主题 | 推一个 `config.json` 就能换壁纸、配色、停靠栏和列数，不用重新打包 |

几张真机截图（推上 `themes/aurora` 之后截的）：

| | |
|---|---|
| ![桌面首页：停靠栏和顶部展台](docs/new-home.jpg) | ![输入源面板：HDMI 口和连接状态](docs/inputs-menu.jpg) |
| 首页：停靠栏 + 当前应用的大标题 | 输入源：HDMI 口、AV、有线电视 |
| ![时钟屏保](docs/clock-screensaver.png) | ![长按 OK 移动图标](docs/tile-move.jpg) |
| 时钟屏保，缓慢漂移防烧屏 | 长按 OK 选「移动」，方向键搬运图标 |

改列数、换停靠栏应用、隐藏某个应用，都可以在电视上操作：

- 列数、停靠栏：焦点停在「设置」上长按 OK → 桌面设置。
- 隐藏单个应用：桌面长按图标 → 菜单里选「Hide app」；应用库长按条目 → 「Hide app / Unhide」。隐藏后条目还留在应用库，标成 `Hidden`（系统应用是 `Hidden · System`）。
- 系统应用：默认自动隐藏，不用维护包名列表。桌面设置页有「Hide system apps: On/Off」；应用库顶部有「Show system apps: On/Off」，打开后隐藏的系统应用会重新列出来——这是藏过头时的逃生通道。

也可以用主题文件批量改（`hide_system_apps` 和旧的 `hidden_apps` 仍然有效，后者只是静态补充）：

```bash
tools/theme.sh themes/aurora        # 推一个主题
tools/theme.sh --reset              # 恢复内置配置
```

## 屏幕上会发生什么

开机到桌面起来，中间这一段是这套方案最容易出问题的地方，先说清楚它是怎么走的：

1. 通电，厂商 LOGO 和开机动画（这一段改不了，见「去广告与屏蔽更新」）。
2. 系统启动时绑定无障碍服务 `HomeKeyService`。这个绑定不靠开机广播，屏幕还黑着也会发生。
3. 服务连上后（`onServiceConnected`），桌面在 3 秒、10 秒、22 秒、40 秒、65 秒各检查一次（`Handler` 回调，不是 `AlarmManager`）。前台不是自己就调 `performGlobalAction(GLOBAL_ACTION_HOME)` 把桌面拉起来。
4. 用户存储还没解锁（CE 未解锁）就先等 `USER_UNLOCKED`，解锁后再读配置、再排检查；桌面已经在前台就丢掉剩下的检查，不抢焦点。
5. 65 秒那次是兜底：海信这批机器冷启动慢，前几次经常撞上厂商动画还没走完。

这条路径不需要 `SYSTEM_ALERT_WINDOW`：`GLOBAL_ACTION_HOME` 是系统级动作，不走 `startActivity`，不受后台启动限制；而且这个固件每次开机都用 `revokeRuntimePermission` 把 SAW 撤掉。实测无障碍方案 3/3 次冷启动成功，旧的 AlarmManager 方案 3 次里失败 2 次。

所以正常情况下，你会看到厂商动画 → 短暂黑屏（1～3 秒）→ 桌面直接出现，不需要按任何键。如果你看到的是长时间黑屏，先按一下遥控器 Home 键，能起来说明是自启没跑成，看 [docs/hisense/开机自启.md](docs/hisense/开机自启.md) 里的排查命令。重装过 APK 的话，先确认无障碍服务还绑着——重装会把它从 `enabled_accessibility_services` 里移除。

## 去广告与屏蔽更新

海信国内版的广告分几层，能处理的和不能处理的分开说：

| 广告/更新来源 | 谁在管 | 怎么处理 | 效果 |
|---|---|---|---|
| 开机视频、开机动画 | 系统 `bootanimation.zip` + 厂商自绘动画 | 见 [docs/hisense/vidaa-mt9653.md](docs/hisense/vidaa-mt9653.md) 的「开机广告」一节 | 不改系统分区，用 `/data/local` 覆盖，可撤销 |
| 贴片/横幅广告 SDK | `com.hisense.business.ad`、`com.hisense.service.ad` | 清单里已包含，`pm disable-user` | 应用内广告位不再加载 |
| 桌面运营位、VIP 横幅 | 原桌面 `com.jamdeo.tv.vod` 自己拉取 | 只能换桌面，禁旁路应用没用 | 换桌面后彻底消失 |
| `ad_block` 开关 | `settings put global com.hisense.service.ad_block 1` | 写一次即可，但会被 `com.jamdeo.updateservice` 在开机时改回 `0` | 禁掉 updateservice 才能长期生效 |
| 固件更新（OTA 三件套） | `com.jamdeo.updateservice`、`com.hisense.usbupgrade`、`com.hisense.playerupdate` | 三个一起禁 | 永久停更，恢复要刷机 |

`ad_block` 的写法：

```bash
tools/venv/bin/python -I tools/tv.py 'settings put global com.hisense.service.ad_block 1'
tools/venv/bin/python -I tools/tv.py 'settings get global com.hisense.service.ad_block'
```

第二天再看如果又变回 `0`，就是 updateservice 干的，不是你没写进去。

**关于 OTA 的硬话**：禁用三件套之后，这台电视不会再收到任何固件更新，也就没有安全补丁。海信的固件更新只能靠官方 OTA 或售后刷机，禁了之后自己刷不回来。要更新就先 `pm enable` 这三个包，更新完再禁。细节见 [docs/hisense/屏蔽更新.md](docs/hisense/屏蔽更新.md)。

## Home 键与输入源键

海信 VIDAA 的遥控器不按 Android 的常规套路出牌，这两个键单独说。

**Home 键**：物理按键上报的是 Linux scancode `102`，厂商的 keylayout 把它映射成 `KEYCODE_F12`（142），不是 `KEYCODE_HOME`（3）。原来由厂商的 SysUIMerge 接住这个键去切「上一个应用」——原桌面在的时候，切回去的就是桌面，所以看起来 Home 是好用的；原桌面一禁，这个键就变成了没有目标。

两个办法：

```bash
# 办法一：告诉 VIDAA 的框架 Home 键该交给谁（推荐，重启后生效）
tools/venv/bin/python -I tools/tv.py 'setprop persist.his.key_home_component com.gathin.tvhome/.MainActivity'
tools/venv/bin/python -I tools/tv.py 'getprop persist.his.key_home_component'

# 办法二：打开桌面的按键辅助服务，接管按键事件
HOME_KEY_SERVICE=1 STOCK_LAUNCHER=com.jamdeo.tv.vod launcher/build-and-install.sh
```

办法一改的是厂商属性，`persist.` 前缀会写进 `/data/property`，重启不丢；想还原就 `setprop persist.his.key_home_component com.jamdeo.tv.vod/com.jamdeo.tv.vod.MainActivity`（或者干脆清空，让固件用默认值）。部分 02.02.x 之后的固件不认这个属性，那就只能靠办法二，或者把原桌面留着当 Home 键的接收器。

**输入源键**：上报 `KEYCODE_TV_INPUT`（178）。这台机器 `dumpsys tv_input` 是空的，没有 TvInputManager 输入，所以桌面走的是厂商自己的输入源面板：

- 服务：`com.hisense.tv.inputs/com.hisense.tv.InputsUIService`
- action：`com.hisense.tv.start_inputs`
- 依赖：`com.hisense.tv.tiletransition`（面板切换输入前要绑它的 `TileAnimationService`）

配置写在主题的 `config.json` 里，或者直接改 `launcher/app/src/main/res/values/config.xml`：

```json
{
  "source_panel_service": "com.hisense.tv.inputs/com.hisense.tv.InputsUIService",
  "source_panel_action": "com.hisense.tv.start_inputs"
}
```

## 恢复

一条命令，按改动前写下的记录逐个还原（原来就禁着的保持禁用，Home 还给原来的组件）：

```bash
TV_HOST=192.168.1.50 tools/restore.sh examples/hisense-vidaa-mt9653-cn/debloat.txt
```

没有记录（比如换了台电脑、删了 `~/.cleanyourtv/`）时，脚本会提示并退化成「把清单里的包全部 enable、动画倍速复位成 1.0」。

手动救急，电视还能连上但桌面起不来：

```bash
# 把原桌面放回来，Home 交回去
tools/venv/bin/python -I tools/tv.py 'pm enable --user 0 com.jamdeo.tv.vod'
tools/venv/bin/python -I tools/tv.py 'cmd package set-home-activity --user 0 com.jamdeo.tv.vod/com.jamdeo.tv.vod.MainActivity'
```

电视完全连不上（ADB 也进不去）时，用电视自己的 **设置 → 应用 → 显示系统应用** 逐个启用；这是最后一道保险，`pm disable-user` 只是对用户禁用，包一直在系统里。

## 常见问题

**1. 开机黑屏，只有背光没有画面**

原桌面被禁之后，这台机器上没有任何应用响应开机广播，VIDAA 的启动流程找不到要拉起的东西，就一直停在黑屏。现在由无障碍服务 `HomeKeyService` 在开机时把桌面拉起来（不依赖开机广播，也不需要 `SYSTEM_ALERT_WINDOW`），正常情况下不会再出现。已经黑了就按遥控器 Home 键，或者：

```bash
tools/venv/bin/python -I tools/tv.py 'am start -n com.gathin.tvhome/.MainActivity'
tools/venv/bin/python -I tools/tv.py 'dumpsys activity activities | grep "Task{.*#1 "'
```

`Task #1` 的 `sz=0` 就是「任务栈空、没有 Activity 在前台」，这是黑屏的直接证据。详见 [docs/hisense/开机自启.md](docs/hisense/开机自启.md)。

**2. `INSTALL_FAILED_UPDATE_INCOMPATIBLE`**

新 APK 的签名和电视上已装的那份不一致。两个选择：卸了重装（`pm uninstall com.gathin.tvhome`，桌面配置会一起没），或者用固定签名重新打包——本仓库 CI 已经用仓库 secrets 里的固定 keystore 签名，同一来源的包可以直接 `adb install -r` 覆盖升级。

**3. 重装桌面后无障碍服务不工作（设置里显示 Crashed services）**

重装会把 `enabled_accessibility_services` 里的条目弄脏，系统认为服务崩了。除了遥控器按键，开机自启也靠这个服务，不重绑就会停在黑屏。清空再写回，一次搞定：

```bash
tools/venv/bin/python -I tools/tv.py 'settings get secure enabled_accessibility_services'
tools/venv/bin/python -I tools/tv.py 'settings put secure enabled_accessibility_services ""'
tools/venv/bin/python -I tools/tv.py 'settings put secure accessibility_enabled 0'
# 等 2 秒，再把上面查到的条目原样写回去
tools/venv/bin/python -I tools/tv.py 'settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService'
tools/venv/bin/python -I tools/tv.py 'settings put secure accessibility_enabled 1'
```

**4. 桌面 config 写不进去**

`content write` 只吃标准输入，没有 `--string` 参数，写成 `content write --uri ... --string '{...}'` 会静默失败：

```bash
tools/venv/bin/python -I tools/tv.py 'cat > /data/local/tmp/config.json' < themes/aurora/config.json
tools/venv/bin/python -I tools/tv.py 'content write --uri content://com.gathin.tvhome.theme/config.json < /data/local/tmp/config.json'
tools/venv/bin/python -I tools/tv.py 'content read --uri content://com.gathin.tvhome.theme/config.json'
```

**5. 遥控器 Home 键没反应**

见上面「Home 键与输入源键」。先确认属性写进去了（`getprop persist.his.key_home_component`），再确认按键辅助服务绑上了（`settings get secure enabled_accessibility_services`）。

**6. 输入源面板能打开，但选 HDMI 没反应**

`com.hisense.tv.tiletransition` 被禁了。面板本身还能开、还有动画，但切换输入要绑它的 `TileAnimationService`，绑不上就什么都不发生，logcat 里是 `Unable to start service Intent { act=com.hisense.tv.tiletransition.TileAnimationService } not found`。

```bash
tools/venv/bin/python -I tools/tv.py 'pm enable --user 0 com.hisense.tv.tiletransition'
```

**7. `ad_block` 开机被改回 0**

`com.jamdeo.updateservice` 在开机时重写这个开关。要么接受（每次开机重写一次），要么禁掉 updateservice 并接受不再更新固件。

**8. 遥控器按了没反应、应用列表是空的**

两种情况都指向同一个原因：无障碍服务没绑上。应用列表空是 Android 11+ 的包可见性限制——桌面必须声明 `<queries>` 才看得见别的应用，官方包已经声明了；如果你自己改过包名或重签过，先确认安装的是本仓库的包。按键没反应就看第 3 条，把服务清空重写。

## 免责声明

- 改的是电视的系统设置（`pm disable-user`、`settings put`、`appops`），不是刷机。不卸载系统应用、不 root、不碰分区。
- 风险在于禁错包。禁错的表现可能是：黑屏、Wi-Fi 掉线、遥控器失灵、输入源切不过去。所以先备份、分批禁、随时准备 `tools/restore.sh`。
- 禁掉 OTA 三件套等于放弃固件和安全更新，恢复只能刷机。这个决定你自己做，别怪工具。
- 由此产生的保修、售后问题与本项目无关。MIT 协议，按原样提供，没有任何担保。

## 许可

MIT，见 [LICENSE](LICENSE)。本项目是 [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的修改版，原项目版权归 Gathin 所有；桌面内嵌的 Outfit 字体使用 SIL Open Font License，见 `launcher/FONT-LICENSE-OFL.txt`。
