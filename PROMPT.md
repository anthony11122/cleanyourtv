# 给 AI 助手的操作提示词

把下面这段整个粘进 Claude Code、Codex 或 Gemini CLI（任何能跑 shell 的助手），它会在你的电脑上一步步操作电视。

前提：电脑和电视连同一个 WiFi；电视已开 USB 调试（设置 → 关于 → 连按「版本号」7 次 → 返回 → 开发者选项 → USB 调试）。如果这个仓库已经 clone 到本地，告诉助手，它能直接复用 `tools/` 和 `launcher/`，不用从零写。

电视开着 USB 调试的这段时间，端口 5555 是**明文、无认证**的（海信这批机器 `ro.adb.secure=0`，连确认弹窗都没有），同一个局域网里任何设备都能拿到 shell。整个流程一次做完，做完立刻关掉调试。

---

我要给一台海信电视去广告、换桌面。机型是 `<在这里填型号>`。

请直接操作真机，一步一步来，每一步都用电视截图（`tools/tv.py --screenshot`）确认结果，不要凭猜。涉及我的使用习惯的选择，先问我。

## 连接

- 用 `tools/tv.py`（纯 Python 的 ADB 客户端），不要依赖系统的 `adb` 二进制：macOS 的本地网络隐私权限会把 `adb connect` 拦成「No route to host」，而 `tools/tv.py` 走的是 Python socket，不受影响。
- 先 `--probe` 拿型号、Android 版本、固件号，和 `examples/` 里的清单对上再往下走。
- 海信国内版开 USB 调试就直接监听 5555，没有配对码，也没有「允许调试吗」的弹窗。**这意味着局域网里任何设备都能连**，提醒我做完关掉，并用 `python3 tools/adb-closed.py <ip>` 验证端口已关。
- 端口关着但电视有「无线调试」开关的（部分国际版固件）：在开发者选项里配对，`adb pair` 之后再 `adb connect`。
- 助手自己的沙箱连不到局域网时，让我在终端跑 `tools/relay.py <ip>` 并保持运行，然后用 `TV_HOST=127.0.0.1 TV_PORT=15555`。
- `adb install` 在 Android 14 上会等屏幕确认，卡住就改用 `pm install -r`。

## 先备份

- 动手前把 `pm list packages -f`、`pm list packages -d`、`getprop`、`settings list global`、`settings list secure` 全部存到本地 `backup/`。
- `tools/disable.sh` 自己会写还原记录到 `~/.cleanyourtv/`，但那是「按清单」的粒度，整机备份仍然要做。
- 问我 USB 口插了什么、网络是不是走 USB 网卡——有些机器开 USB 调试会断 USB 供电，网卡一断 ADB 也跟着断。

## 清理

- 优先用 `examples/` 里对应机型的清单，不要自己现编。没有对应机型就挑同芯片的最近一份，改之前先跟我确认。
- 用 `pm disable-user --user 0`，**永远不要** `pm uninstall` 系统应用。
- 永远不要禁：`android`、`com.android.systemui`、`com.android.settings`、`com.android.tv.settings`、`com.android.shell`、`com.android.phone`、权限控制器、包安装器、`ext.services`、`ext.shared`、`modulemetadata`、`com.google.android.overlay.modules*`、`com.android.providers.*`。`tools/disable.sh` 会拒绝这类清单，别绕过它。
- 包名经常骗人：动手前用 `dumpsys package <包名>` 看它有哪些 activity / service / receiver，别只看名字。
- 分批禁，每批之间重启一次、截一张图。一批带走了承重的东西，你分不出是哪个。

### 海信 / VIDAA 特有的坑（这几条是实测踩出来的，必须遵守）

1. **`com.hisense.tv.tiletransition` 不能禁。** 名字像转场动画，实际是 `TileAnimationService` 的宿主：厂商输入源面板切换输入前必须先绑它。禁掉之后面板能开、有动画，但选 HDMI 完全没反应，日志是 `Unable to start service Intent { act=com.hisense.tv.tiletransition.TileAnimationService } not found`。
2. **`com.jamdeo.tv.vod`（原桌面）只在新桌面已经起来并成为 HOME 之后才能禁。** 它是这台机器上唯一带 `BOOT_COMPLETED` 接收器的桌面，先禁它再装新桌面，中间那次重启就是黑屏。`tools/disable.sh` 有一道 Home 守卫会跳过它，不要用 `FORCE_HOME=1` 绕过。
3. **Home 键是 scancode 102 → `KEYCODE_F12`（142），不是 `KEYCODE_HOME`（3）。** 原来由厂商 SysUIMerge 处理，原桌面一禁就没目标。用 `setprop persist.his.key_home_component com.gathin.tvhome/.MainActivity`，或者 `HOME_KEY_SERVICE=1` 开按键辅助服务。**不要用 `input keyevent 3` 验证**：厂商框架会吞掉注入的 keycode 3，必须按遥控器真键。
4. **输入源键是 `KEYCODE_TV_INPUT`（178）**，且这台机器 `dumpsys tv_input` 是空的，没有 TvInputManager 输入。走厂商面板：`com.hisense.tv.inputs/com.hisense.tv.InputsUIService`，action `com.hisense.tv.start_inputs`。
5. **`ad_block` 会被覆写。** `settings put global com.hisense.service.ad_block 1` 写进去，重启后 `com.jamdeo.updateservice` 把它改回 0。要长期生效只能禁 updateservice，代价是停更——这个决定要我先确认。
6. **OTA 三件套**：`com.jamdeo.updateservice`、`com.hisense.usbupgrade`、`com.hisense.playerupdate`。禁掉 = 永久停更、没有安全补丁、恢复要刷机。禁之前必须明确告诉我。
7. **`content write` 只吃 stdin**，没有 `--string` 参数，写成 `--string '{...}'` 会静默失败。正确写法：先把文件推到 `/data/local/tmp/`，再 `content write --uri content://com.gathin.tvhome.theme/config.json < /data/local/tmp/config.json`，最后 `content read` 读回来确认。
8. **重装桌面后无障碍服务会变成 Crashed services**，遥控器按键随之失效。修复序列：`settings put secure enabled_accessibility_services ""` → `settings put secure accessibility_enabled 0` → 等 2 秒 → 写回服务名 → `accessibility_enabled 1`。
9. **第二个「桌面」**：`com.keylab.speech.view.vidaa` 里有 `com.ju.secondscreen.HomeActivity`，开机按组件名被拉起，会盖在新桌面前面，还弹「内存提速 39%」。组件级禁用会被拒（`SecurityException`），只能整包禁，副作用是语音 UI 和遥控器麦克风键一起没了。禁之前问我。

## 装桌面

- 先确认电视能吃 APK、SDK 版本够：`getprop ro.build.version.sdk` 要 ≥ 26，否则 `INSTALL_FAILED_OLDER_SDK`，直接告诉我不行，别硬试。
- 用 `launcher/build-and-install.sh`，海信要带上原桌面包名：`STOCK_LAUNCHER=com.jamdeo.tv.vod`。脚本会自己装、抢 Home、失败回滚。
- 抢完确认真的抢到了：`dumpsys activity activities | grep mFocusedApp` 和 `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`。两条都指向 `com.gathin.tvhome` 才算成功。`set-home-activity` 打印 `Success` 但实际没变的情况是存在的。
- 签名：CI 用固定 keystore 签，同一来源的包能直接 `-r` 覆盖升级。签名不一致报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，要么卸载重装，要么统一用仓库出的 APK。
- 桌面配置（列数、停靠栏、隐藏应用）优先用主题推：`tools/theme.sh <主题目录>`，别改源码重编。

## 验证

- 每一步之后 `tools/tv.py --screenshot shots/xx.png DPAD_RIGHT`：先按键再截屏，看焦点是不是落在预期位置。
- 直播电视、HDMI、Netflix 的截图是 0 字节，这是系统保护，不是坏了。改用 `dumpsys activity activities | grep topResumedActivity`。
- 遥控器按键依赖无障碍服务时，`settings get secure enabled_accessibility_services` 确认服务在列表里，然后让我按真键——注入的键不算证据。
- 全部做完重启一次，逐条确认：能开机进桌面、Wi-Fi 还在、遥控器 Home 和输入源键可用、输入源面板能切 HDMI、`tools/check.py <清单>` 退出 0。

## 收尾

- 给我一份还原命令（`tools/restore.sh <清单>`）和手动救急命令。
- 提醒我关掉 USB 调试，并用 `tools/adb-closed.py` 验证。
- 把这次发现的、清单里没有的包告诉我，我来决定加不加。

---

整个过程我会回答三四个选择题（用哪些应用、停靠栏放几个、要不要留语音助手和投屏、要不要停更）。所有改动都可撤销，但停更那条不可逆，别替我决定。
