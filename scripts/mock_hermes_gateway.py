#!/usr/bin/env python3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import time

RUNS = {}

class Handler(BaseHTTPRequestHandler):
    def _json(self, payload, status=200):
        body=json.dumps(payload).encode()
        self.send_response(status)
        self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _sse(self, events):
        body=''.join(f"event: {kind}\ndata: {json.dumps(data)}\n\n" for kind, data in events).encode()
        self.send_response(200)
        self.send_header('Content-Type','text/event-stream')
        self.send_header('Cache-Control','no-cache')
        self.send_header('Content-Length',str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        length=int(self.headers.get('Content-Length','0') or 0)
        raw=self.rfile.read(length).decode() if length else '{}'
        try:
            return json.loads(raw)
        except Exception:
            return {}

    def do_GET(self):
        if self.path == '/v1/models':
            self._json({'object':'list','data':[{'id':'hermes-agent','object':'model'},{'id':'gpt-5.5','object':'model'}]})
        elif self.path == '/api/jobs':
            self._json({'jobs':[{'job_id':'run-live-1','name':'Live Hermes run','status':'running'},{'job_id':'cron-brief','name':'Daily gateway brief','status':'idle'}]})
        elif self.path.startswith('/v1/runs/') and self.path.endswith('/events'):
            run_id=self.path.split('/')[3]
            self._sse([
                ('run.status', {'run_id': run_id, 'status': 'running'}),
                ('run.reasoning', {'summary': 'Mock Hermes selected mobile overlay context and phone tool affordances.'}),
                ('tool.call', {'name': 'phone.snapshot', 'status': 'ready'}),
                ('message.delta', {'text': 'Mock run event stream reached the Android client.'}),
                ('run.status', {'run_id': run_id, 'status': 'completed'}),
            ])
        elif self.path.startswith('/v1/runs/'):
            run_id=self.path.split('/')[3]
            self._json(RUNS.get(run_id, {'id':run_id,'status':'completed'}))
        else:
            self._json({'error':'not found','path':self.path},404)

    def do_POST(self):
        data=self._body()
        if self.path == '/v1/chat/completions':
            messages=data.get('messages') or []
            text=''
            system=''
            for msg in messages:
                if msg.get('role') == 'system': system=msg.get('content','')
            if messages:
                text=messages[-1].get('content','')
            phone=' phone tools armed' if 'phone.snapshot' in system or 'Current phone context' in text else ''
            self._json({'id':'chatcmpl-mock','object':'chat.completion','choices':[{'index':0,'message':{'role':'assistant','content':f'Mock Hermes acknowledged: {text}{phone}'},'finish_reason':'stop'}]})
        elif self.path == '/v1/runs':
            run_id='run-mock-%d' % int(time.time()*1000)
            RUNS[run_id]={'id':run_id,'status':'running','input':data}
            self._json(RUNS[run_id], 201)
        elif self.path.startswith('/v1/runs/') and self.path.endswith('/stop'):
            run_id=self.path.split('/')[3]
            RUNS[run_id]={'id':run_id,'status':'stopped'}
            self._json(RUNS[run_id])
        else:
            self._json({'error':'not found','path':self.path},404)

    def log_message(self, format, *args):
        print('%s - %s' % (self.address_string(), format % args), flush=True)

if __name__ == '__main__':
    ThreadingHTTPServer(('0.0.0.0', 8642), Handler).serve_forever()
