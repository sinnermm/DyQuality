package dev.operit.dyquality;
import java.util.*;
public class QualityPolicyTest {
 static int n;
 static void eq(int actual,int expected,String name){if(actual!=expected)throw new AssertionError(name+": "+actual+" != "+expected);n++;}
 static QualityPolicy.Spec s(int w,int h,int b){return new QualityPolicy.Spec(w,h,b);}
 public static void main(String[] args){
  List<QualityPolicy.Spec> xs=Arrays.asList(s(720,1280,1200000),s(1080,1920,7000000),s(1080,1920,2500000),s(1440,2560,9000000),s(1440,2560,4500000));
  eq(QualityPolicy.choose(xs,0),-1,"auto unchanged");eq(QualityPolicy.choose(xs,1),2,"1080 lower bitrate");eq(QualityPolicy.choose(xs,2),4,"2K lower bitrate");eq(QualityPolicy.choose(xs,3),4,"highest resolution low bitrate");eq(QualityPolicy.choose(xs,4),3,"highest resolution high bitrate");
  eq(QualityPolicy.choose(Arrays.asList(s(1920,1080,2500000),s(1280,720,1000000)),1),0,"landscape");
  eq(QualityPolicy.choose(Arrays.asList(s(720,1280,1000000)),2),0,"missing 2K stays real 720");
  eq(QualityPolicy.choose(Arrays.asList(s(2160,3840,20000000)),1),-1,"no under-cap preserve auto");
  eq(QualityPolicy.choose(Arrays.asList(s(0,0,1000000)),3),-1,"unknown dimensions");
  eq(QualityPolicy.choose(Arrays.asList(s(1080,1920,0)),3),-1,"unknown bitrate");
  eq(QualityPolicy.choose(Collections.<QualityPolicy.Spec>emptyList(),2),-1,"empty");
  eq(QualityPolicy.choose(xs,99),-1,"invalid");
  System.out.println("PASS "+n+" policy tests");
 }
}