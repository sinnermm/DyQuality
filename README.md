# 抖音画质选择 · 0.1 experimental

独立模块，包名 dev.operit.dyquality；不是 Dyyds 修改版。默认自动，无网络权限。

## 验证状态
- 目标 APK：普通抖音 com.ss.android.ugc.aweme，40.7.0 / 400701，55 个 DEX。
- 已静态确认 SimVideoUrlModel.bitRate 及其列表 getter LJJJJLI；getter 复制列表，元素身份保持。
- Bitrate 实现 getVideoWidth / getVideoHeight / getBitRate 已静态确认。
- 12 项纯 Java 策略测试通过，编译、DEX 生成、APK v3 签名校验通过。
- 尚未安装启用本模块、验证小按钮和播放器实际选择，不能宣称已修复卡顿。
- 作者上传原文件未证实能够获取，本版没有此功能。
- Dyyds 的 Java 字符串已还原，可见 videoPlaybackQualityMode、videoOriginalWifiOnly、enableLongPressResolution 等字段；核心逻辑 Native 化，尚不能确认其“原画”具体取流方式。
- UI只读检查未成功：虚拟屏全黑，主屏设置浮窗黑色，未读取到选项。未修改设置。

## 安装和测试
需要 Android 10+ 与支持 libxposed Modern API 102 的 LSPosed/Vector。仅作用于普通抖音。
1. 安装 APK，在框架启用模块，作用域选普通抖音。
2. 关闭 Dyyds 的强制原画/播放画质修改，以免冲突。未获授权时不代用户修改。
3. 完全退出并重启抖音，寻找左侧“画质”按钮。默认自动，不筛选。
4. 打开诊断，播放/滑动几条视频查看读取次数是否增长。
5. 测试1080P上限或1440P上限；不存在目标分辨率时只选上限内实际最高档，全部超上限或维度不明则保留自动。
6. 单独比较同一视频下的缓冲和解码规格；候选列表不能充当已播放规格。
7. 有问题在框架禁用模块，重新启动抖音。

## 原理与局限
通过类型和对象身份检查筛选码率getter的返回列表，不修改原视频对象。不硬编码版本号或混淆方法名；仍依赖模型类名、bitRate字段和稳定访问方法，不能保证全版本兼容。
同分辨率低码率模式只减少传输量，不保证感知画质相同，也不保证流畅。最高码率仍可能卡顿。没有网络测速、动态自动降档、作者原文件、CDN测速或当前视频即时重载功能。
只有列表getter路径生效，DASH/直连/缓存可能绕过。最近一次候选可能来自预加载，不代表当前视频；须进行后续实机验证。按钮目前按Activity注入，主页非视频Tab也可能显示。

## 构建
Ubuntu（这里为 Android proot Ubuntu 24）。Java 21，Debian Android build tools（aapt/dx/zipalign/apksigner），android-23.jar，仅用于平台基础类编译。
执行 `python3 build.py`，产物 `build/DyQuality-0.1-experimental.apk`。
编译依赖 `io.github.libxposed:api:102.0.0`（Apache-2.0），来自 Maven Central： https://repo.maven.apache.org/maven2/io/github/libxposed/api/102.0.0/ ，不打包进模块。
构建自动使用本地测试签名，非发布签名。
先搜索了 https://github.com/twyora/DouyinEnhancer ，其 FeedVideoHooker 是下载地址替换逻辑，并不是清晰度选择实现。未复制它的源码。本项目实现为独立试验代码。

## 后续验收标准
确认框架加载日志、小按钮和菜单可用；确认码率getter读取/筛选计数；确认实际解码分辨率与所选策略一致；同视频自动/1080P/1440P/最高档对比。只有这些完成才可以声称播放功能有效。卡顿根因尚待同视频网络与解码指标验证。
