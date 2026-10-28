package dev.operit.dyquality;
import java.util.List;

/** Pure selection logic. Dimensions are measured, not inferred from labels. */
public final class QualityPolicy {
    public static final String[] LABELS = {"自动（抖音默认）", "1080P 上限 · 同分辨率低码率", "2K / 1440P 上限 · 同分辨率低码率", "平台最高分辨率 · 同分辨率低码率", "平台最高分辨率 · 最高码率"};
    public static final class Spec {
        public final int width, height, bitrate;
        public Spec(int w, int h, int b) { width=w; height=h; bitrate=b; }
        public int shortSide() { return Math.min(width,height); }
        public long pixels() { return (long)width*height; }
    }
    public static int choose(List<Spec> specs, int mode) {
        if (mode<=0 || mode>=LABELS.length || specs.isEmpty()) return -1;
        // Unknown dimensions/bitrate: retain the unmodified list rather than guess.
        for (Spec s:specs) if(s.width<=0 || s.height<=0 || s.bitrate<=0) return -1;
        int cap=mode==1?1080:mode==2?1440:Integer.MAX_VALUE;
        int best=-1;
        for(int i=0;i<specs.size();i++) {
            Spec s=specs.get(i);
            if(s.shortSide()>cap) continue;
            if(best<0 || better(s,specs.get(best),mode)) best=i;
        }
        // No rendition under the cap: preserve automatic playback, do not fake support.
        return best;
    }
    private static boolean better(Spec a,Spec b,int mode) {
        if(a.shortSide()!=b.shortSide()) return a.shortSide()>b.shortSide();
        if(a.pixels()!=b.pixels()) return a.pixels()>b.pixels();
        return mode==4?a.bitrate>b.bitrate:a.bitrate<b.bitrate;
    }
}
