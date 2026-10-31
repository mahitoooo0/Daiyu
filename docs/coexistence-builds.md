# 与原版代鱼共存安装（同一台设备，两个包）

默认构建的包名是 `app.daiyu.android`，和上游原版完全一致，会覆盖安装。要让“原版”和“我们的构建”同时存在，用两个可选 Gradle 属性给本次构建换一个包名后缀和启动器名称，代码与资源不需要分叉。

```sh
./gradlew :app:assembleDebug \
  -PdaiyuApplicationIdSuffix=.mine \
  -PdaiyuApplicationLabel=代鱼·开发版
```

- `daiyuApplicationIdSuffix`：必须以 `.` 开头，只含小写字母、数字、下划线，最长 33 字符。生效后包名变为 `app.daiyu.android<后缀>`。若同时开启既有的 `-PdaiyuSyntheticApplication=true`，两段后缀会拼成 `app.daiyu.android.synthetic<后缀>`。
- `daiyuApplicationLabel`：启动器/应用详情里的名称，纯文字，不超过 40 字符，不得含引号、尖括号、`&`、`$`。不设置时仍是 `@string/app_name`（保留 `values-zh` 等多语言）。单独设置标签而不改包名会触发构建检查失败，避免两个同名入口。
- 两个属性都不传时，`applicationId`、`android:label`、provider `authorities`、`taskAffinity` 与改动前逐字节一致，既有 CI 构建行为不变。

## 隔离边界

包名不同即意味着两份完全独立的安装：应用私有目录（含 `filesDir/linux-installations` 的 Linux 环境）、SharedPreferences、provider `authorities`（`${applicationId}.shizuku`、`${applicationId}.opened.attachments`、`${applicationId}.accessibility.health`）、`taskAffinity`（`${applicationId}.vd.anchor`）都随包名分开。

需要重新授予、不会自动继承的东西：无障碍服务、悬浮窗、通知读取、使用情况访问、Shizuku 授权、Linux 环境需要在新包里重新安装一次（两套 rootfs 各占约 100MB 解压空间）、Root 增强授权。

## 代码侧约定

`activity-alias`、`Service`、`Activity` 这些**清单类名**由 `namespace`（`app.daiyu.android`）决定，和包名后缀无关；代码里凡是需要点名清单组件的地方，都必须用类引用或 `BuildConfig.MANIFEST_PACKAGE`，不能用 `context.packageName` 拼字符串：

- `appearance/PackageManagerIconAdapter.kt`：alias 名称根用 `BuildConfig.MANIFEST_PACKAGE`；`ComponentName` 的包名仍用 `context.packageName`。
- `appearance/IconAppearanceCoordinator.kt`：把 alias 名称根传给 `LauncherIconSwitcher`。
- `execution/UserExecutionService.kt`：通知点击用 `Intent(this, MainActivity::class.java)`。
- `vd/VirtualDisplayAnchorActivity.kt`：`TASK_AFFINITY` 由 `BuildConfig.APPLICATION_ID` 派生，与清单 `${applicationId}.vd.anchor` 对齐。

新增组件或新的显式 Intent 时沿用同一规则，否则只会在带后缀的构建里失败，普通构建看不出来。

## 验证

1. GitHub 构建（或本机）分别产出原版与共存版 APK；
2. `adb shell pm list packages | grep app.daiyu.android` 应同时列出 `app.daiyu.android` 与 `app.daiyu.android.mine`；
3. 两个入口互相独立启动，桌面图标名称不同、最近任务不互相吞并；
4. 在共存版里切换一次启动器图标，确认 `validate` 通过（这是最容易漏的一条，alias 名称根不是包名）；
5. 原版的设置、Linux 环境、授权在共存版中都不存在（反之亦然）。

## 限制

- 同一包名换签名不能覆盖安装，两个包名则各自独立，互不影响。
- 共存版与 Xposed/Shizuku 等按包名识别的外部组件需要重新配置。
- 本仓库的 CI 只构建默认包名；共存版需要额外传参，未在 CI 中固定。
