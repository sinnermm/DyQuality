package dev.operit.dyquality;
import android.content.Context;
import android.content.SharedPreferences;
public final class Settings {
    private static final String FILE="dev.operit.dyquality.private";
    public static int mode(Context c){int v=c.getSharedPreferences(FILE,0).getInt("mode",0);return v>=0 && v<QualityPolicy.LABELS.length?v:0;}
    public static void mode(Context c,int v){c.getSharedPreferences(FILE,0).edit().putInt("mode",v).apply();}
}