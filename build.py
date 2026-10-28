#!/usr/bin/env python3
import pathlib,subprocess,zipfile,urllib.request,hashlib,shutil
root=pathlib.Path(__file__).resolve().parent
b=root/'build';b.mkdir(exist_ok=True)
classes=b/'classes';classes.mkdir(exist_ok=True)
android=pathlib.Path('/usr/lib/android-sdk/platforms/android-23/android.jar')
api=root/'deps/xposed-api.jar';api.parent.mkdir(exist_ok=True)
if not api.exists():
 u='https://repo.maven.apache.org/maven2/io/github/libxposed/api/102.0.0/api-102.0.0.aar'
 data=urllib.request.urlopen(u,timeout=30).read();(b/'api.aar').write_bytes(data)
 with zipfile.ZipFile(b/'api.aar') as z:api.write_bytes(z.read('classes.jar'))
def run(args):
 print('RUN',' '.join(map(str,args)),flush=True);subprocess.run(list(map(str,args)),check=True)
t=b/'tests';t.mkdir(exist_ok=True)
run(['javac','--release','8','-encoding','UTF-8','-d',t,root/'src/dev/operit/dyquality/QualityPolicy.java',root/'test/QualityPolicyTest.java'])
run(['java','-cp',t,'dev.operit.dyquality.QualityPolicyTest'])
run(['javac','--release','8','-encoding','UTF-8','-cp',str(android)+':'+str(api),'-d',classes]+sorted((root/'src').rglob('*.java')))
# Debian dx accepts Java 8 class files, no lambdas/default implementation are bundled.
run(['/usr/lib/android-sdk/build-tools/debian/dx','--dex','--output='+str(b/'classes.dex'),classes])
run(['aapt','package','-f','-M',root/'AndroidManifest.xml','-I',android,'-S',root/'res','-F',b/'unsigned.apk'])
with zipfile.ZipFile(b/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 z.write(b/'classes.dex','classes.dex')
 z.writestr('META-INF/xposed/java_init.list','dev.operit.dyquality.QualityModule\n')
 z.writestr('META-INF/xposed/module.prop','minApiVersion=102\ntargetApiVersion=102\nstaticScope=true\nautoHotReload=false\n')
 z.writestr('META-INF/xposed/scope.list','com.ss.android.ugc.aweme\n')
key=b/'debug.keystore'
if not key.exists():run(['keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=DyQuality experimental','-keyalg','RSA','-keysize','2048','-validity','3650'])
run(['/usr/lib/android-sdk/build-tools/debian/zipalign','-f','4',b/'unsigned.apk',b/'aligned.apk'])
out=b/'DyQuality-0.1-experimental.apk'
run(['apksigner','sign','--ks',key,'--ks-pass','pass:android','--key-pass','pass:android','--out',out,b/'aligned.apk'])
run(['apksigner','verify','--verbose',out])
run(['aapt','dump','badging',out])
print('APK',out,'SHA256',hashlib.sha256(out.read_bytes()).hexdigest())