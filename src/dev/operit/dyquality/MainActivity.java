package dev.operit.dyquality;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
import android.graphics.Color;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);ScrollView s=new ScrollView(this);TextView t=new TextView(this);t.setTextSize(17);t.setTextColor(Color.WHITE);t.setPadding(40,60,40,60);s.setBackgroundColor(0xff17191c);t.setText("抖音画质选择 · 试验版 0.1\n\n这是独立模块，不是 Dyyds 的修改版。\n\n安装要求：Android 10+；支持 Modern API 102 的 LSPosed / Vector。\n\n1. 在框架中启用本模块，仅勾选普通抖音。\n2. 测试前关闭 Dyyds 的强制原画/播放画质修改，避免两个模块争抢。\n3. 完全退出并重新打开抖音，寻找左侧‘画质’小按钮（可拖动）。\n4. 默认自动；先打开‘诊断’查看读取次数是否增长，再试 1080P 或 2K 上限。\n\n作用：尝试筛选平台返回的码率候选，在同分辨率下优先低码率，减少不必要流量。不是倍增画质，不是缓存加速器。\n\n限制：\n• 作者上传原文件尚未证实可获取。平台最高画质不是上传原文件。\n• 目前只完成静态分析及策略测试，未完成设备播放验证。\n• 切换在下一次播放器读取列表时生效，不保证当前视频即时切换。\n• 1080P/1440P 是上限，没有目标档则使用上限内最高档；维度未知则保留抖音默认。\n• 诊断显示最近一次候选列表，可能是预加载视频，不是当前解码画质。\n• DASH、缓存、直播、直连路径可能不生效。\n• 没有版本号白名单，但仍依赖模型字段和方法，不能保证全版本兼容。\n\n出问题：在框架中禁用本模块，重新启动抖音即可。\n\n模块不申请网络或悬浮窗权限，不记录视频URL，不修改系统，不覆盖 Dyyds。\n\n使用 API：io.github.libxposed:api:102.0.0（Apache-2.0），仅编译依赖，不打包进 APK。\n");s.addView(t);setContentView(s);}
}