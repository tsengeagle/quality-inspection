import base64, json, sys, urllib.request, urllib.error
BASE='http://localhost:8080/api/quality-inspection'
def token(uid, role, hosp='H001'):
    p={'tz-user': json.dumps({'user':{'user_id':uid,'user_name':uid,'user_role':[{'role_name':role,'enabled':True}],'hospital_id':hosp}})}
    b=base64.urlsafe_b64encode(json.dumps(p).encode()).decode().rstrip('=')
    return 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.%s.demo'%b
def call(method, path, actor, body=None):
    req=urllib.request.Request(BASE+path, method=method, headers={'api-key':token(*actor),'Content-Type':'application/json'}, data=None if body is None else json.dumps(body).encode())
    try:
        with urllib.request.urlopen(req) as r: return r.status, json.loads(r.read() or b'null')
    except urllib.error.HTTPError as e:
        t=e.read()
        try: return e.code, json.loads(t)
        except Exception: return e.code, t.decode()[:300]
MGR=('demo-manager','hospital_manager'); ADMIN=('admin-1','system_admin'); REV=('rev-1','reviewer'); REV2=('rev-2','reviewer'); AUD=('aud-1','auditor')
