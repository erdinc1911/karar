#!/bin/bash
# Emülatörde: kur, aç, ekran görüntüsü al, kısa bir parçayı sıraya koy, ekranı kapat, ayrıştırmanın sürüp sürmediğini ölç.
set -x
P=com.erdinc1911.karar
O=test-sonuc; mkdir -p $O
adb install -r KararEslestirici-test.apk
adb shell pm grant $P android.permission.POST_NOTIFICATIONS || true
adb shell pm grant $P android.permission.RECORD_AUDIO || true
adb logcat -c
adb shell am start -n $P/.MainActivity
sleep 25
adb exec-out screencap -p > $O/1-acilis.png
SOCK=$(adb shell cat /proc/net/unix | grep -o 'webview_devtools_remote_[0-9]*' | head -1); echo "soket: $SOCK"
adb forward tcp:9222 localabstract:$SOCK
curl -s http://127.0.0.1:9222/json | head -c 600; echo
E(){ python3 android/test/cdp.py "$1"; }
E "JSON.stringify({baslik:document.title,surum:(document.body.innerText.match(/sürüm [0-9.]+ [0-9:]+/)||[''])[0],native:!!window.KararNative,ort:typeof ort})" | tee $O/2-sayfa.txt
B64=$(base64 -w0 deneme.wav)
E "(async()=>{const b=Uint8Array.from(atob('$B64'),c=>c.charCodeAt(0));const f=new File([b],'Deneme - Test.wav',{type:'audio/wav',lastModified:1});await openLocal(f);for(let i=0;i<100&&!(media.duration>0&&libCur);i++)await sleep(200);return JSON.stringify({sure:media.duration,kayit:!!libCur})})()" | tee -a $O/2-sayfa.txt
E "(async()=>{await libDraw();const q=qGet();const x=q.find(z=>z.key===libCur.key);if(x){x.kind='mdx';x.ch=2;store.set('sepQueue',q)}qStart({now:true});await sleep(3000);return JSON.stringify(qGet().map(z=>[z.name,z.st,z.msg||'']))})()" | tee -a $O/3-sira.txt
sleep 20
adb exec-out screencap -p > $O/4-calisirken.png
E "JSON.stringify(qGet().map(z=>[z.st,z.msg||'']))" | tee -a $O/3-sira.txt
echo "--- ekran kapatılıyor ---" | tee -a $O/3-sira.txt
adb shell input keyevent KEYCODE_SLEEP
for i in $(seq 1 16); do sleep 15; echo "$(date +%T) ekran: $(adb shell dumpsys power | grep -o 'mWakefulness=[A-Za-z]*') | $(E 'JSON.stringify(qGet().map(z=>[z.st,z.msg||""]))')" | tee -a $O/3-sira.txt; done
adb shell input keyevent KEYCODE_WAKEUP; sleep 3; adb shell wm dismiss-keyguard; sleep 3
adb exec-out screencap -p > $O/5-sonra.png
E "JSON.stringify({sira:qGet().map(z=>[z.st,z.msg||'']),kanallar:store.get('scIdx',[]).length})" | tee -a $O/3-sira.txt
adb shell dumpsys activity services $P | grep -i "isForeground\|AyristirmaHizmeti" | head -5 | tee -a $O/3-sira.txt
adb logcat -d | grep -i -E "chromium|karar|console|FATAL" | tail -150 > $O/6-logcat.txt
exit 0
