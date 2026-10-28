package dev.operit.dyquality;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.*;
import java.util.*;
import java.lang.ref.WeakReference;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedInterface;

/** Experimental: restricts renditions only when the getter matches its original list. */
public final class QualityModule extends XposedModule {
    private static final String TAG="DyQuality";
    private static final String TARGET="com.ss.android.ugc.aweme";
    private volatile Context context;
    private volatile int mode;
    private volatile String last="尚未观测到码率列表。默认不修改播放。";
    private volatile long lastAt;
    private volatile long calls, filtered;
    private volatile int hooks;
    private boolean ready;
    private final Map<Class<?>,Method[]> accessors=Collections.synchronizedMap(new WeakHashMap<Class<?>,Method[]>());
    private final Set<Application> registered=Collections.newSetFromMap(new WeakHashMap<Application,Boolean>());
    private final Map<Activity,WeakReference<View>> buttons=new WeakHashMap<Activity,WeakReference<View>>();

    @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam p) {
        Log.i(TAG,"module loaded process="+p.getProcessName());
    }
    @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam p) {
        if(!TARGET.equals(p.getPackageName()) || !p.isFirstPackage() || ready) return;
        ready=true;
        try {
            hook(Application.class.getDeclaredMethod("onCreate")).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(new XposedInterface.Hooker(){
                @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result=chain.proceed();
                    attach((Application)chain.getThisObject());
                    return result;
                }
            });
            // Lifecycle safety net for Application subclasses not calling super.onCreate().
            hook(Activity.class.getDeclaredMethod("onResume")).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(new XposedInterface.Hooker(){
                @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result=chain.proceed(); Activity a=(Activity)chain.getThisObject();
                    attach(a.getApplication()); addButton(a);return result;
                }
            });
            installModel(p.getClassLoader(),"com.ss.android.ugc.playerkit.simapicommon.model.SimVideoUrlModel");
            installModel(p.getClassLoader(),"com.ss.android.ugc.aweme.feed.model.VideoUrlModel");
            Log.i(TAG,"installed list hooks="+hooks+"; fail-open on missing signatures");
        } catch(Throwable e){Log.e(TAG,"initialization failed, no forced playback",e);}
    }
    private synchronized void attach(Application app) {
        if(app==null || !TARGET.equals(app.getPackageName()))return;
        context=app;mode=Settings.mode(app);
        if(!registered.add(app))return;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle b){}
            public void onActivityStarted(Activity a){}
            public void onActivityResumed(Activity a){addButton(a);}
            public void onActivityPaused(Activity a){}
            public void onActivityStopped(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}
            public void onActivityDestroyed(Activity a){buttons.remove(a);}
        });
    }
    private void installModel(ClassLoader loader,String name) {
        try {
            Class<?> cls=Class.forName(name,false,loader);
            final Field bitrates=findField(cls,"bitRate");
            if(!List.class.isAssignableFrom(bitrates.getType()))return;
            int count=0;
            for(Method m:cls.getDeclaredMethods()) {
                if(Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length!=0 || !List.class.isAssignableFrom(m.getReturnType()))continue;
                m.setAccessible(true);
                hook(m).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(new XposedInterface.Hooker(){
                    @Override public Object intercept(XposedInterface.Chain chain) throws Throwable {
                        Object result=chain.proceed();
                        if(context==null || !(result instanceof List))return result;
                        Object raw=bitrates.get(chain.getThisObject());
                        if(!(raw instanceof List) || !sameItems((List<?>)raw,(List<?>)result))return result;
                        List<?> list=(List<?>)result;
                        if(list.isEmpty() || list.size()>64)return result;
                        try{return filter(list);}catch(Throwable e){Log.w(TAG,"unknown model; original list retained",e);return result;}
                    }
                });
                count++;
            }
            hooks+=count;
        } catch(Throwable e){Log.w(TAG,"model not supported: "+name,e);}
    }
    private static Field findField(Class<?> cls,String name)throws NoSuchFieldException {
        for(Class<?> c=cls;c!=null;c=c.getSuperclass()) {
            try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException e){}
        }
        throw new NoSuchFieldException(name);
    }
    private static boolean sameItems(List<?> a,List<?> b){
        if(a.size()!=b.size() || a.isEmpty())return false;
        for(int i=0;i<a.size();i++)if(a.get(i)!=b.get(i))return false;
        return true;
    }
    private Method[] methods(Object obj)throws Exception {
        Class<?> c=obj.getClass();Method[] cached=accessors.get(c);if(cached!=null)return cached;
        Method[] ms={c.getMethod("getVideoWidth"),c.getMethod("getVideoHeight"),c.getMethod("getBitRate")};
        for(Method m:ms)m.setAccessible(true);accessors.put(c,ms);return ms;
    }
    private Object filter(List<?> list)throws Exception {
        List<QualityPolicy.Spec> specs=new ArrayList<QualityPolicy.Spec>();
        StringBuilder info=new StringBuilder();
        for(Object item:list) {
            Method[] ms=methods(item);
            int w=((Number)ms[0].invoke(item)).intValue(),h=((Number)ms[1].invoke(item)).intValue(),b=((Number)ms[2].invoke(item)).intValue();
            specs.add(new QualityPolicy.Spec(w,h,b));
            info.append(w).append('×').append(h).append(" · ").append(String.format(Locale.US,"%.2f",b/1000000.0)).append(" Mbps\n");
        }
        int selected=QualityPolicy.choose(specs,mode); calls++;
        String status=selected<0?"保留抖音自动选择":("提供给播放器的候选："+specs.get(selected).width+"×"+specs.get(selected).height+" / "+String.format(Locale.US,"%.2f",specs.get(selected).bitrate/1000000.0)+" Mbps");
        last=status+"\n\n最近一次候选列表（可能属于预加载视频，不代表当前解码规格）：\n"+info;
        long now=SystemClock.elapsedRealtime();boolean log=now-lastAt>10000;lastAt=now;
        if(selected<0){if(log)Log.i(TAG,status+" candidates="+list.size());return list;}
        filtered++;if(log)Log.i(TAG,status+" candidates="+list.size());
        ArrayList<Object> chosen=new ArrayList<Object>();chosen.add(list.get(selected));return chosen;
    }
    private static int dp(Context c,float x){return (int)(c.getResources().getDisplayMetrics().density*x+0.5f);}
    private boolean videoActivity(Activity a){
        String n=a.getClass().getName();
        return n.equals("com.ss.android.ugc.aweme.main.MainActivity") || n.startsWith("com.ss.android.ugc.aweme.detail.") || n.equals("com.ss.android.ugc.aweme.feed.ui.VideoPlayActivity");
    }
    private void addButton(final Activity a) {
        if(!videoActivity(a) || a.isFinishing())return;
        if(Looper.myLooper()!=Looper.getMainLooper()){a.runOnUiThread(new Runnable(){public void run(){addButton(a);}});return;}
        WeakReference<View> old=buttons.get(a);if(old!=null && old.get()!=null){old.get().bringToFront();return;}
        View content=a.findViewById(android.R.id.content);if(!(content instanceof FrameLayout))return;
        final FrameLayout parent=(FrameLayout)content;
        final TextView b=new TextView(a);b.setText("画质");b.setTextSize(12);b.setTextColor(Color.WHITE);b.setGravity(Gravity.CENTER);
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xb5222222);bg.setCornerRadius(dp(a,18));bg.setStroke(dp(a,1),0x70ffffff);b.setBackground(bg);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(a,48),dp(a,34),Gravity.TOP|Gravity.LEFT);lp.leftMargin=dp(a,12);lp.topMargin=dp(a,170);parent.addView(b,lp);buttons.put(a,new WeakReference<View>(b));
        b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){menu(a);}});
        b.setOnTouchListener(new View.OnTouchListener(){float x,y,ox,oy;boolean moved;
            public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();ox=v.getTranslationX();oy=v.getTranslationY();moved=false;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-x,dy=e.getRawY()-y;if(Math.abs(dx)+Math.abs(dy)>dp(a,8))moved=true;if(moved){v.setTranslationX(ox+dx);v.setTranslationY(oy+dy);}return true;}
                if(e.getAction()==MotionEvent.ACTION_UP){if(!moved)v.performClick();return true;}return e.getAction()==MotionEvent.ACTION_CANCEL;
            }
        });
    }
    private void menu(final Activity a) {
        final String[] rows={QualityPolicy.LABELS[0],QualityPolicy.LABELS[1],QualityPolicy.LABELS[2],QualityPolicy.LABELS[3],QualityPolicy.LABELS[4],"诊断 / 实际候选规格","作者上传原文件：暂未验证可获取"};
        new AlertDialog.Builder(a).setTitle("画质策略 · 试验版 0.1").setSingleChoiceItems(rows,mode,new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog,int which){
                if(which>=5){dialog.dismiss();diagnostics(a);return;}
                mode=which;Settings.mode(a,which);dialog.dismiss();
                Toast.makeText(a,"已设置："+QualityPolicy.LABELS[which]+"\n在下次播放器读取候选列表时生效；不保证当前视频即时切换。",Toast.LENGTH_LONG).show();
            }
        }).setNegativeButton("关闭",null).show();
    }
    private void diagnostics(Activity a) {
        String msg="钩子数："+hooks+"；读取次数："+calls+"；筛选次数："+filtered+"\n策略："+QualityPolicy.LABELS[Math.max(0,Math.min(mode,4))]+"\n\n"+last+"\n\n重要：候选规格不是已解码画质。缓存/DASH/直连播放可能绕过本钩子。默认自动；不修改原始视频对象，不拦截网络，不采集播放地址。\n\n测试前请关闭 Dyyds 的强制原画/播放画质修改，避免冲突。平台最高档不等于作者上传原文件。";
        new AlertDialog.Builder(a).setTitle("播放诊断（未完成实机验证）").setMessage(msg).setPositiveButton("确定",null).show();
    }
}