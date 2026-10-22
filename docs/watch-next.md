# 读取「继续观看」

桌面可以显示别的应用发布到系统「继续观看」（Watch Next）和预览频道里的内容。这些数据在 TV provider 里，通过 `TvContract.WatchNextPrograms` 和 `TvContract.PreviewPrograms` 读取。

- **权限**：`android.permission.READ_TV_LISTINGS`，运行时权限。用正常弹窗申请，或者在 shell 里直接授予：`pm grant <包名> android.permission.READ_TV_LISTINGS`。
- **不需要的权限**：`com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA` 是 `signature|privileged`，侧载应用拿不到。只有 `READ_TV_LISTINGS` 时，查询返回调用方自己的行，加上所有标了 `searchable` 的行。
- **不要带 selection 查询**：没有 `ACCESS_ALL_EPG_DATA` 的调用方传 selection 会收到 `SecurityException`。整表读出来，在代码里过滤。
- **有些应用什么都不发布**：只有应用自己写入并标成 searchable 才会有行，不少应用两样都不做，所以依赖某个应用之前先在目标电视上查一下。

源码依据：AOSP 的 `packages/providers/TvProvider/src/com/android/providers/tv/TvProvider.java` 里的 `createSqlParams()` 实现了 `READ_TV_LISTINGS` 和 `searchable` 的逻辑，两个权限都定义在那个模块的 `AndroidManifest.xml` 里。
