# 主题

一个主题就是一个文件夹，里面放 `config.json`，可选再放 `wallpaper.jpg`。推上去之后，桌面下次启动会把 `config.json` 叠加在内置的 `config.xml` 上，换配色、壁纸、停靠栏和列数都不用重新打包。

在电视上的「桌面设置」里改过的选项优先级更高，直到你选「跟随主题」。推主题或执行 `tools/theme.sh --reset` 也会清掉电视上的这些选择。

```bash
TV_HOST=192.168.1.50 tools/theme.sh themes/aurora          # 推主题并重启桌面
TV_HOST=192.168.1.50 tools/theme.sh --reset                # 回到内置 config.xml
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.50 themes\aurora
```

| 主题 | 效果 | 截图 |
|---|---|---|
| `default/` | 内置外观写成 JSON，改自己的主题时当起点 | <img src="default/screenshot.jpg" width="320" alt="default 主题"> |
| `aurora/` | 深色极光壁纸（深蓝底上的青绿和紫色，底部更暗），配色跟壁纸一致 | <img src="aurora/screenshot.jpg" width="320" alt="aurora 主题"> |
| `glass/` | 深蓝壁纸 + 柔和青紫光，影院式顶部展台、磨砂停靠栏、纯图标按钮，焦点光晕取应用主题色 | <img src="glass/screenshot.jpg" width="320" alt="glass 主题"> |
| `scenes/` | `wallpapers/` 里 14 张深色插画风景，每小时换一张，由 `tools/scenes_wallpapers.py` 用代码画出来 | <img src="scenes/screenshot.jpg" width="320" alt="scenes 主题"> |
| `six-columns/` | 每行 6 个更小的图标，停靠栏下面再露一行 | <img src="six-columns/screenshot.jpg" width="320" alt="six-columns 主题"> |
| `dussehra/` | 排灯节皮肤（到 2026-10-21）：万寿菊花环、淡金坛城、油灯当 Wi-Fi 图标、铜铃当输入源、万寿菊当设置、开机第一次进桌面撒花瓣 | <img src="dussehra/screenshot.jpg" width="320" alt="dussehra 主题"> |

每个主题目录都有一张 `screenshot.jpg`，是真机推上主题后截的。`tools/theme.sh` 不会推它，只是给你先看看效果。

## 配置项

所有键都是可选的：缺的键沿用 `config.xml` 的值；写错类型或者不认识的键直接忽略；整个文件解析失败也忽略，所以主题永远弄不坏桌面。

| 键 | 类型 | 含义 |
|---|---|---|
| `dock_apps` | 包名列表 | 停靠栏的应用和顺序，没装的自动跳过 |
| `fill_dock` | bool | `dock_apps` 没填满的槽位用网格里前几个应用补上（默认 true）；false 就空着 |
| `hidden_apps` | 包名列表 | 永远不显示成图标 |
| `live_tv_package`、`live_tv_action` | string | 有线电视应用；如果它没有 leanback 入口，用这个 action 启动 |
| `live_tv_input_id` | string | 输入源菜单里「有线电视」那一行对应的系统调谐器 id，打开它的频道列表。留空自动选 |
| `source_panel_service`、`source_panel_action` | string | 电视自带的输入源面板（没有 TvInputManager 的机型用）。可以写服务，也可以写 Activity。没有 Live TV 应用时，输入源按钮直接开它 |
| `source_panel_extra` | `"name=value"` | 跟输入源面板一起发的一个 int extra（酷开 OS：`control_center_command=100`） |
| `coocaa_sources` | bool | 酷开 OS：输入源菜单里先列 `com.skyworth.tv` 报上来的信号源 |
| `launch_actions` | `"包名=action"` 列表 | 让某个应用图标打开它内部的某个页面而不是首页；action 失效时退回正常启动 |
| `app_labels` | `"包名=名字"` 列表 | 图标显示自定义名字，不用应用自己的名字 |
| `app_banners` | `"包名=图名"` 列表 | 图标用桌面内置的 `banner_*` 图 |
| `input_action` | string | HDMI/AV 那一行的 channel URI 没人处理时改发的 action |
| `input_uri_scheme` | string | HDMI/AV 那一行改用的 URI scheme（默认 `content://`），不带 MIME 类型 |
| `input_sources` | 列表，只认 `config.xml` | 厂商信号源编号，按输入 id 末位写 `HW<n>=<编号>`；`HW<n>=-` 表示这台机器没有这个口。主题文件里读不到 |
| `columns` | 1 到 8 | 每行几个图标，5 个以上自动缩小 |
| `rows_below_dock` | 0 到 3 | 停靠栏下面露几行网格；放不下时图标缩小 |
| `dock_title` | bool | 停靠栏上方用大字显示当前应用名（true）；false 只留光晕 |
| `dock_row` | bool | 停靠栏单独一行带底板（true）；false 就跟网格连成一片 |
| `reorder_button` | bool | 顶栏加一个排序按钮（默认 false）；排序功能一直在设置按钮的长按菜单里 |
| `show_network_pill` | bool | 顶栏的 Wi-Fi/有线按钮（默认 true） |
| `show_inputs_pill` | bool | 顶栏的输入源按钮（默认 true）；遥控器上的输入源键不受影响 |
| `icon_only_buttons` | bool | Wi-Fi 和设置变成圆形纯图标按钮，输入源保留文字 |
| `top_menu` | bool | 输入源、Wi-Fi、设置收进一个圆形齿轮，点 OK 展开菜单，长按 OK 还是桌面菜单 |
| `launcher_settings_button` | bool | 顶栏设置直接打开桌面设置；长按 OK 仍是排序和模块卡片 |
| `wifi_label_width` | 0 到 300 | 配合 `icon_only_buttons`，Wi-Fi 按钮上保留多宽的网络名（dp），超出省略；0 只留图标 |
| `library_button` | bool | 顶栏加一个应用库按钮，需要同时开 `app_library` |
| `always_show_names` | bool | 所有图标下面都显示名字，未聚焦时压暗；停靠栏底板会自动加高 |
| `wallpaper_true_color` | bool | 壁纸用 32 位色解码（1080p 约 8 MB）而不是 RGB_565（4 MB），深色渐变不起色带；2 GB 内存以上再开 |
| `app_library` | bool | 首页最后一行加一个应用库入口：列出所有已装应用，一键加回桌面或移除（默认 false） |
| `shamsi_date` | bool | 在公历日期上方再显示波斯历 |
| `greeting_name` | string | 问候语后面加名字 |
| `module_cards` | bool | 是否扫描模块卡片并询问（默认 true） |
| `reduced_motion` | bool | 弱 GPU 用的降动效模式：高亮 5 帧内停住，动画不超过 90 ms，没有淡入淡出和图标入场 |
| `boot_to_live_tv` | bool | 开机后第一次进桌面直接打开有线电视，每次开机只做一次 |
| `focus_overshoot` | bool | 高亮和图标会先冲过目标再回弹（默认 true）；false 直接滑过去 |
| `vpn_app` | string | 配了 VPN 时网络按钮打开的应用；VPN 没起来时显示红色「VPN off」角标 |
| `animations` | bool | 基础动效：焦点、玻璃高亮、标题、图标入场（默认 true） |
| `advanced_effects` | bool | 可选的图标抬起、图标放大、环境色平滑过渡和打开应用动效（默认 false） |
| `show_home_settings_shortcut` | bool | 电视有主屏幕选择器时保留「默认桌面」图标（默认 false） |
| `show_stats` | bool | 时钟下面加一行 CPU、可用内存、可用存储（默认关） |
| `handle_view_inputs` | bool | 用输入源菜单应答系统的 `VIEW_INPUTS`（遥控器输入源键变成的那个），给「只有原桌面应答它」的机型用（海信 huangshan 就是这样）；默认 false，避免本来有输入源面板的电视多出一层选择 |
| `background_color` | `#RRGGBB` | 背景色，默认黑 |
| `sky_intensity` | 0 到 100 | 跟随时段的天空渐变，0 关闭（默认） |
| `wallpaper` | bool | 是否显示主题里的 `wallpaper.jpg`（默认 true） |
| `wallpaper_rotation_minutes` | 0 到 1440 | 主题 `wallpapers/` 目录的轮换间隔，0 表示不轮换 |
| `wallpaper_order` | `shuffle` 或 `sequential` | 轮换顺序。`shuffle` 默认且尽量不重复当前这张；`sequential` 按文件名走，到头回到第一张 |
| `wallpaper_dim` | 0 到 90 | 壁纸上的黑色蒙版百分比，默认 0 |
| `dock_opacity` | 30 到 100 | 停靠栏底板不透明度，让壁纸透出来；默认 100 |
| `wallpaper_captions` | bool | 轮换时在右上角显示图片名，名字来自 `wallpapers/captions.txt`（一行一张，`001.jpg<TAB>名字`） |
| `tile_menu` | bool | 长按 OK 弹出小菜单：移动、从桌面移除（需 `app_library`）、应用信息。默认 false 时长按 OK 直接进应用信息 |
| `festival` | string | 节日皮肤：网络按钮变油灯、输入源变铜铃、设置变万寿菊，当天第一次进桌面撒花瓣 |
| `festival_until` | `YYYY-MM-DD` | 节日皮肤显示到哪天，过了自动关 |
| `greeting` | string | 节日期间替换问候语 |
| `accent_color` | `#RRGGBB` | 节日期间焦点光晕的颜色 |
| `settings_key_keycodes` | 键码列表 | 打开设置的遥控器按键（`HomeKeyService` 用） |
| `settings_key_panel` | bool | 那些键改成打开一个小的快捷设置面板（电视设置、显示、网络、应用、桌面设置） |
| `restore_key_services` | bool | 桌面被强停之后，下次进桌面把按键辅助服务重新写回启用列表；安装时要带 `RESTORE_KEY_SERVICES=1` |
| `input_key_keycodes` | 键码列表 | 更多打开输入源切换的按键（`KEYCODE_TV` 和 `KEYCODE_TV_INPUT` 本来就管用） |
| `app_keys` | 字符串列表 | 遥控器按键打开指定应用，写成 `"190:com.google.android.youtube.tv"`、`"KEYCODE_BUTTON_3:com.google.android.youtube.tv"`，或用 scancode `"scan:490:com.wbd.hbomax"`；前缀 `hold:` 表示长按 |
| `key_filter` | bool | false 让按键辅助服务保持启用但不再过滤按键，用于「任何按键过滤服务都会破坏长按连发」的固件 |
| `stock_home_redirect` | string | 原桌面包名；固件把它重新启用时（XGIMI 会），把它的首页直接送回这里 |
| `race_calendar` | 比赛列表 | F1 比赛周末模式：周末第一场前 12 小时到正赛开始后 4 小时，时钟下面显示倒计时和赛程。用 `tools/race_calendar.py <主题目录>` 写入，桌面本身不联网 |
| `home_idle_clock_minutes` | 0 到 1440 | 桌面静止多少分钟后显示时钟屏保，任意键返回；0 关闭 |
| `glow_style` | string | 焦点光晕形状：`radial`（默认）或 `horizon`（颜色从上边缘往下洗到黑） |
| `cinematic_home` | bool | 影院式顶部展台：当前停靠栏应用的海报柔化后铺满上半屏，配大号名字、「上次打开」、停靠栏位置点 |
| `dock_glass` | bool | 停靠栏底板做成跟顶栏一样的磨砂玻璃 |
| `ambient_glow` | bool | 停靠栏后面的应用主题色光晕（默认 true） |
| `app_glow` | bool | 每个图标的高亮都带应用主题色（默认 false） |

## 壁纸

`wallpaper.jpg` 全屏铺满、居中裁切。建议 1920×1080、深色、300 KB 以内：文字、大标题和停靠栏都是按黑底设计的，而且 1 GB 的电视每次进桌面都要把这张图解码进内存。比屏幕大的图会先缩到屏幕尺寸；缩完还超过 4K（约 840 万像素）的图直接不显示，退回纯色背景。

要一组壁纸，就把 JPEG、PNG 或 WebP 放进主题的 `wallpapers/` 目录，然后写：

```json
{ "wallpaper": true, "wallpaper_rotation_minutes": 30, "wallpaper_dim": 40, "wallpaper_order": "sequential" }
```

电视会记住最后显示的那张，重启从它接着来，不会跳回第一张。睡眠或断电期间错过的轮换不补播，醒来最多换一次。换一组图就从新组的第一张开始。只有一张图的目录只显示那一张，不会定时变黑。图片居中裁切填满 16:9，蒙版画在图片之上、文字之下。

推主题会用新的一组图替换电视上原有的那组。文件名只用字母、数字、下划线、点和连字符。图片版权自己确认。桌面最多认 100 张非空图，单张不超过 10 MB，读不了的文件跳过并保留上一张。目录为空就退回 `wallpaper.jpg`，再没有就用背景色。`"wallpaper": false` 同时关掉单图和轮换。

有备选时随机选且不立刻重复。当前图片和更换时间在应用重启后仍然保留。轮换只在桌面可见时进行，从别的应用回来时如果已经超时会立刻换。图片在工作线程解码并限制在屏幕尺寸。没有下载服务、没有网络权限、没有电脑端定时任务。系统自带的屏保不受影响。

电视上的「桌面设置」里有一行「壁纸」：从这组图里挑一张（或者主题的 `wallpaper.jpg`），或者选「无」。挑了之后轮换停止，直到你选「跟随主题」或重新推主题。

## 降动效

`"reduced_motion": true` 保留外观但大幅减少绘制：高亮不打弹、5 帧内停住，所有动画不超过 90 ms，名字、展台、光晕直接出现不淡入，没有图标入场。给 GPU 跟不上的机器用（已知案例：Android 10 上的 MT5895）。

```json
{ "reduced_motion": true }
```

## 时段天空

`"sky_intensity": 60` 在壁纸之上、所有内容之下画一层跟随时段的深色渐变：夜里偏蓝，白天淡蓝，傍晚在八点前泛暖，底部的光从左往右跟着太阳走。颜色都压得比较暗，白色时钟、大标题和停靠栏的对比度不受影响。整层是一张 480×270 的位图，每分钟重画一次，不做动画，1 GB 的机器也毫无压力。`0` 关闭，默认就是关的。

```json
{ "sky_intensity": 60 }
```

## 加一个主题

在这个目录下新建一个文件夹，放：

- `config.json`：主题本体；
- `screenshot.jpg`：**必须**。真机推上主题后的桌面截图，不要模拟图、不要模拟器。用 `tools/tv.py 'screencap -p /sdcard/s.png'` 截、`tools/tv.py --pull /sdcard/s.png s.png` 拉下来，存成 1280×720 的 JPEG、300 KB 以内。焦点停在停靠栏上，让光晕和大标题都露出来；
- `wallpaper.jpg` 或 `wallpapers/`：如果主题用到的话。

然后在上面那张表里加一行，格式照抄别的行。停靠栏列表保持通用（主流流媒体应用），除非这个主题是给某台特定电视做的——那样的话在说明里写清楚机型。
