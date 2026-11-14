#!/bin/bash
# Emülatörde: kur, aç, ekran görüntüsü al, kısa bir parçayı sıraya koy, ekranı kapat, ayrıştırmanın sürüp sürmediğini ölç.
set -x
P=com.erdinc1911.karar
O=test-sonuc; mkdir -p $O
adb install -r KararEslestirici-test.apk
adb shell wm size 1920x3000; adb shell wm density 400
adb shell pm grant $P android.permission.POST_NOTIFICATIONS || true
adb shell pm grant $P android.permission.RECORD_AUDIO || true
adb logcat -c
adb shell am start -n $P/.MainActivity
sleep 25
adb exec-out screencap -p > $O/1-acilis.png
SOCK=$(adb shell cat /proc/net/unix | grep -o 'webview_devtools_remote_[0-9]*' | head -1); echo "soket: $SOCK"
adb forward tcp:9222 localabstract:$SOCK
curl -s http://127.0.0.1:9222/json | tee $O/0-hedefler.txt | head -c 900; echo
E(){ python3 android/test/cdp.py "$1"; }
E "JSON.stringify({gen:innerWidth,yuk:innerHeight,tasma:document.documentElement.scrollWidth-innerWidth,ust:document.querySelector('header')?document.querySelector('header').getBoundingClientRect().top:null,dpr:devicePixelRatio,apk:document.documentElement.classList.contains('apk')})" | tee $O/2-olcu.txt
adb shell settings put system user_rotation 1; adb shell settings put system accelerometer_rotation 0; sleep 6
adb exec-out screencap -p > $O/1b-yatay.png
E "JSON.stringify({gen:innerWidth,yuk:innerHeight,tasma:document.documentElement.scrollWidth-innerWidth})" | tee -a $O/2-olcu.txt
adb shell settings put system user_rotation 0; sleep 6
E "JSON.stringify({baslik:document.title,surum:(document.body.innerText.match(/sürüm [0-9.]+ [0-9:]+/)||[''])[0],native:!!window.KararNative,ort:typeof ort})" | tee $O/2-sayfa.txt
python3 android/test/cdp.py yukle deneme.wav | tee -a $O/2-sayfa.txt
E "(async()=>{await libDraw();const q=qGet();const x=q.find(z=>z.key===libCur.key);if(x){x.kind='mdx';x.ch=2;store.set('sepQueue',q)}qStart({now:true});await sleep(3000);return JSON.stringify(qGet().map(z=>[z.name,z.st,z.msg||'']))})()" | tee -a $O/3-sira.txt
sleep 20
adb exec-out screencap -p > $O/4-calisirken.png
E "JSON.stringify({st:qGet().map(z=>z.st),bitti:sepJob?sepJob.done:null,toplam:sepJob?sepJob.nCh:null,yazi:(document.getElementById('qBarT')||{}).innerText||'',coi:self.crossOriginIsolated,sab:typeof SharedArrayBuffer,cekirdek:navigator.hardwareConcurrency})" | tee -a $O/3-sira.txt
echo "--- ekran kapatılıyor ---" | tee -a $O/3-sira.txt
adb shell input keyevent KEYCODE_SLEEP
for i in $(seq 1 20); do sleep 30; echo "$(date +%T) ekran: $(adb shell dumpsys power | grep -o 'mWakefulness=[A-Za-z]*') | $(E "JSON.stringify({st:qGet().map(z=>z.st),bitti:sepJob?sepJob.done:null,toplam:sepJob?sepJob.nCh:null,yazi:(document.getElementById('qBarT')||{}).innerText||'',coi:self.crossOriginIsolated,sab:typeof SharedArrayBuffer,cekirdek:navigator.hardwareConcurrency})")" | tee -a $O/3-sira.txt; done
adb shell input keyevent KEYCODE_WAKEUP; sleep 3; adb shell wm dismiss-keyguard; sleep 3
adb exec-out screencap -p > $O/5-sonra.png
E "JSON.stringify({sira:qGet().map(z=>[z.st,z.msg||'']),kanallar:store.get('scIdx',[]).length})" | tee -a $O/3-sira.txt
adb shell dumpsys activity services $P | grep -i "isForeground\|AyristirmaHizmeti" | head -5 | tee -a $O/3-sira.txt
adb logcat -d | grep -i -E "KararWeb|chromium|AndroidRuntime|FATAL" | tail -200 > $O/6-logcat.txt
exit 0
