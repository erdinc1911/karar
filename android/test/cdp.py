# Emülatördeki uygulamanın sayfasında JavaScript çalıştırır (Chrome DevTools Protocol)
import json,sys,urllib.request,websocket
def ws():
    t=[x for x in json.load(urllib.request.urlopen('http://127.0.0.1:9222/json')) if x.get('type')=='page' or 'appassets' in x.get('url','')]
    return websocket.create_connection(t[0]['webSocketDebuggerUrl'],timeout=600,suppress_origin=True)
_id=[0]
def ev(c,js):
    _id[0]+=1;c.send(json.dumps({'id':_id[0],'method':'Runtime.evaluate','params':{'expression':js,'awaitPromise':True,'returnByValue':True}}))
    while True:
        r=json.loads(c.recv())
        if r.get('id')==_id[0]:
            res=r.get('result',{});
            if 'exceptionDetails' in res:return 'HATA: '+json.dumps(res['exceptionDetails'])[:400]
            return res.get('result',{}).get('value')
if __name__=='__main__':
    try:
        c=ws();print(ev(c,sys.argv[1]))
    except Exception as e:print('CDP HATA:',repr(e)[:300])
