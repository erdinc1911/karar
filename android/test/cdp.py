# Emülatördeki uygulamanın sayfasında JavaScript çalıştırır (Chrome DevTools Protocol)
import json,sys,urllib.request,websocket
def ws():
    t=[x for x in json.load(urllib.request.urlopen('http://127.0.0.1:9222/json')) if x.get('type')=='page' or 'appassets' in x.get('url','')]
    return websocket.create_connection(t[0]['webSocketDebuggerUrl'],timeout=600,suppress_origin=True)
_id=[0]
def ev(c,js):
    _id[0]+=1;c.send(json.dumps({'id':_id[0],'method':'Runtime.evaluate','params':{'expression':'window.__ev('+json.dumps(js)+')','awaitPromise':True,'returnByValue':True}}))
    while True:
        r=json.loads(c.recv())
        if r.get('id')==_id[0]:
            res=r.get('result',{});
            if 'exceptionDetails' in res:return 'HATA: '+json.dumps(res['exceptionDetails'])[:400]
            return res.get('result',{}).get('value')
if __name__=='__main__':
    try:
        c=ws()
        if sys.argv[1]=='yukle':   # büyük dosyayı parça parça sayfaya aktar, sonra uygulamada aç
            import base64;b=base64.b64encode(open(sys.argv[2],'rb').read()).decode();ev(c,"window.__p=''")
            for i in range(0,len(b),400000):ev(c,"window.__p+="+json.dumps(b[i:i+400000])+";0")
            print(ev(c,"(async()=>{const u=Uint8Array.from(atob(window.__p),x=>x.charCodeAt(0));window.__p='';const f=new File([u],'Deneme - Test.wav',{type:'audio/wav',lastModified:1});await openLocal(f);for(let i=0;i<150&&!(media.duration>0&&libCur);i++)await sleep(200);return JSON.stringify({sure:media.duration,kayit:!!libCur})})()"))
        else:print(ev(c,sys.argv[1]))
    except Exception as e:print('CDP HATA:',repr(e)[:300])
