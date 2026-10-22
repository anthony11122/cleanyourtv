# 模块卡片，spec 1

已安装的 Android TV 应用可以向桌面声明一张卡片。卡片就是这个应用的图标，但名字由它自己定，可以带副标题和光晕颜色，排在字母网格之前。桌面只在电视主人回答「显示」之后才展示卡片，并且把这个回答钉在应用的签名密钥上。

这份文档是模块（应用）和桌面之间的契约。写给模块作者的教程在 [`docs/modules.md`](../modules.md)。

## 声明

模块的 `AndroidManifest.xml` 里，在已经有 `LEANBACK_LAUNCHER` 过滤器的那个 Activity 上（也就是桌面显示成图标的那个）：

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

`res/xml/module_card.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<card spec="1" label="Retro" caption="NES games" accent="#E8A33D" />
```

声明规则：

- 卡片过滤器和 `meta-data` 必须挂在同时带 `MAIN` + `LEANBACK_LAUNCHER`、`exported` 且 enabled 的 Activity 上。挂在别处一律丢弃。
- `meta-data` 必须用 `android:resource` 指向 XML 资源。`android:value` 不读。
- 一个包只认一张卡片。声明多张时只读系统列出的第一张。
- 根元素是 `<card>`，只有属性，没有 XML 命名空间。子元素忽略，未知属性忽略，属性总数上限 16。

## 字段

值是写在 XML 里的字面文本。spec 1 不解析引用（`@string/name`、`?attr/name`），写了卡片就无效，所以 spec 1 的卡片不能做多语言。

| 属性 | 必填 | 规则 |
| :- | :- | :- |
| `spec` | 是 | 十进制整数。本桌面读 `1`。其他值：忽略这张卡片，打一行日志。 |
| `label` | 是 | 1 到 24 个字符，取自 `A-Z a-z 0-9`、空格、`. & + ' -`，首尾不能有空格，至少含一个字母或数字。见下面的保留名。 |
| `caption` | 否 | 最多 32 个字符，字符集同上。卡片在网格里聚焦时显示在 label 后面。图标下面只有一行，4 列布局时 label + caption 大约只能显示 26 个字符，所以副标题写几个词就好。 |
| `accent` | 否 | `#RRGGBB`。卡片在停靠栏聚焦时背后的环境光晕颜色。 |

资源编译器会把某些属性值存成带类型的格式；桌面对上面这几个值接受它拿回来的形式（`spec="1"` 是整数，`accent="#E8A33D"` 是颜色，读回来是 `#ffe8a33d`）。label 和 caption 请写纯单词：读起来像带单位的数字或者像颜色的值会被存成对应类型，跟你写的对不上。

### 保留名

比较用的是 label 的折叠形式：转小写，去掉除 ASCII 字母和数字之外的所有字符。「Live TV」「live-tv」「LIVETV」是同一个名字。

以下 label 会被拒：

- 任何位置包含 `verified` 或 `official`：暗示官方背书的词保留，模块声明的东西永远不会被呈现成「官方」（caption 里同样拒这两个词）；
- 是桌面自己 UI 用到的名字，英文或电视当前语言：`Home`、`Settings`、`Inputs`、`Wi-Fi`、`Live TV`、`Default home`、`Clock`、`cleanyourtv`、`tv-vibehack`；
- 是输入源菜单里显示的输入源名：`HDMI` 以及以它开头的任何写法、`AV`；
- 在等待回答期间（新卡片，或者签名密钥、label 变了的卡片）：等于这台电视上另一个已装应用的 label（主屏上显示的名字），或者应用自己的名字等于另一个已装应用的名字。模块自己的应用名没问题：应用叫 Retro、卡片也叫 Retro 是正常情况。主人回答过之后名字就归这张卡片了：之后装的同名应用顶不掉它。回答是针对这个名字的，卡片在更新里改 label 会重新进入待定状态，再走一遍这些检查；
- 两张卡片 label 相同：主人已经决定过的那张保留名字（最早的决定的优先），另一张丢弃；两张都没被决定过就都丢弃，也不去问主人一个有争议的名字。

## 限制

桌面从不相信模块的输入是小的、合法的。所有东西都有上限，越界只丢那一张卡片，打一行日志，其他没有影响。

| 项 | 上限 |
| :- | :- |
| 每次刷新包列表读取的卡片数 | 32，多出的忽略 |
| 显示成图标的卡片数 | 8，按 label 排序；其余在模块卡片列表里标记为超限，保留普通图标 |
| `cards.json` 里记录的决定数 | 128，超出丢最旧的 |
| 每张卡片遍历的 XML 事件数 | 64 |
| XML 嵌套深度 | 2（`<card>` 和它的直接子元素） |
| `<card>` 上的属性数 | 16 |
| label / caption 长度 | 24 / 32 个字符，在做其他检查之前先卡 |
| 签名者 | 只能有一个签名证书，多个的直接拒绝 |

## 桌面行为

1. **发现**。每次回到主屏，桌面向包管理器查询带 `MAIN` + `com.gathin.tvhome.category.DOCK_CARD` 的 Activity（一次包查询，不轮询、不起服务、不联网）。逐个检查 Activity、通过包管理器读 XML、套用上面的规则、算签名摘要。每张卡片在自己的错误边界里读取。
2. **签名摘要**。包唯一签名者的 DER 证书的 SHA-256，和 `apksigner verify --print-certs` 打印的「Signer #1 certificate SHA-256 digest」是同一个值。Android 9 以上读当前签名者，Android 8 读包的签名。多证书签名的包直接拒。
3. **决定记录**。主人的决定按包存在桌面私有存储里（`files/cards.json`，格式版本 2）：包名、签名摘要、卡片折叠后的 label、决定（`show` / `hide`）、首次看到时间、决定时间。文件原子写入，读取时严格校验；校验不过就改名成 `cards.json.bad`，桌面当没有决定继续跑。记录永远不离开电视（桌面的备份是关的）。
4. **询问**。没有记录的卡片，或者签名摘要、折叠 label 跟记录不一致的卡片不显示。等网格布局完成、主屏拿到焦点、没有菜单打开时，桌面弹一次询问。它用应用自己的名字（去掉控制字符和格式字符，最多 40 个字符）和包名、密钥摘要前 12 个十六进制字符来标识；卡片 label 和应用名不同时两个名字都写。按钮是「保持隐藏」（默认焦点）和「显示卡片」；返回键等于「保持隐藏」。询问里也写明之后怎么改。左右在按钮间移动，上下不做事。回答完立刻轮到下一张待定的卡片。
5. **换签名**。签名摘要跟记录不一致的包当作新包：卡片不显示，询问里写明「应用用了不同的签名密钥更新」。记录只由主人的回答替换，检测本身不会改记录。**改 label** 同理：折叠 label 跟记录不一致的卡片不显示，询问里写明卡片改了名字。一个决定只对一个密钥下的一个名字有效。
6. **位置**。已显示的卡片顶替它所在包的所有图标：普通图标不再显示在旁边。`hidden_apps`（`config.xml`）里的包既不显示图标也不显示卡片。`dock_apps` 里的包，卡片就放在那个停靠栏槽位。其他卡片按 label 排在配置的停靠栏应用之后、字母网格之前。默认配置（4 个停靠栏应用、4 列）下，那就是网格的第一个位置。`dock_apps` 少于列数时，空槽位用网格前几个应用补上（`fill_dock`，默认开），已显示的卡片会占住空槽位，因为这时停靠栏就是第一行；`fill_dock` 为 false 时空槽位留空，已显示的卡片是网格第一个。在停靠栏里，展台显示 label，光晕用 accent；在网格里，聚焦图标下面的名字行显示 label 和 caption，能显示多少显示多少。卡片的图用应用自己的 banner（或者图标）。
7. **隐藏**。隐藏的卡片让应用的普通图标保持原样：隐藏卡片从不移除应用，显示卡片也从不安装任何东西。要改就在模块卡片列表里改：长按设置按钮。列表里列出每张发现的卡片，带应用名、包名、完整签名摘要和状态；按 OK 在「显示」和「隐藏」之间切换。
8. **卸载**。卸载模块，卡片就没了。记录留着，用同一把密钥重装不需要重新询问。
9. **启动**。卡片按显式组件启动声明它的那个 Activity，不带 extras、不带 data，跟所有图标一样。卡片的任何字段都不能指定 intent、URI、类名或包名。长按 OK 打开应用信息页，跟普通图标一样。
10. **开关**。`config.xml` 或主题 `config.json` 里的 `module_cards`（默认 `true`）。为 false 时桌面完全不找卡片、不询问，模块卡片列表只说没装。

## 安全

- 桌面从不安装、下载、启用、禁用任何东西，不绑服务、不加载代码，除了那张 XML 和每个图标本来就会用的名字、图片之外，不读模块的任何视图和资源，也没有网络权限。
- 主人不说显示就不显示，回答钉在签名密钥上。主人判断包名和应用名，桌面判断摘要。
- 卡片只能让自己那个 Activity 长得不一样，启动不了别的。启动它就等于启动应用的普通图标。
- label 规则只允许 ASCII，避免形近字母冒充别的名字；跟电视上其他应用或桌面自己 UI 同名的 label 一律拒。
- 卡片代码永远拖不垮主屏：任何一部分在这台电视上出错，本次会话就关掉卡片，打一行日志，桌面表现得跟 `module_cards` 为 false 一样。
- 残余风险：持有「在其他应用上层绘制」权限的应用可以在询问文字上作画。它没法用遥控器按到询问的按钮。询问只在主屏拥有窗口焦点时显示，主屏离开前台就关掉。

## 拒绝

每张被拒的卡片在 logcat 的 `TvHomeCards` 标签下打一行，带包名和原因，例如：

```
W TvHomeCards: io.example.spoof: label 'Retro' is the name of another installed app
W TvHomeCards: io.example.deep: XML nested deeper than 2
W TvHomeCards: io.example.big: label is longer than 24 characters
```

用 `adb logcat -s TvHomeCards` 或 `tools/tv.py 'logcat -d -s TvHomeCards'` 看。被拒的卡片没有其他影响：应用保留普通图标。
