#!/usr/bin/env python3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json

class Handler(BaseHTTPRequestHandler):
    def _send(self, payload, status=200):
        body=json.dumps(payload).encode()
        self.send_response(status)
        self.send_header('Content-Type','application/json')
        self.send_header('Content-Length',str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def do_GET(self):
        if self.path == '/v1/models':
            self._send({'object':'list','data':[{'id':'hermes-agent','object':'model'}]})
        elif self.path == '/api/jobs':
            self._send({'jobs':[{'job_id':'run-live-1','name':'Live Hermes run','status':'running'},{'job_id':'cron-brief','name':'Daily gateway brief','status':'idle'}]})
        else:
            self._send({'error':'not found','path':self.path},404)
    def do_POST(self):
        length=int(self.headers.get('Content-Length','0') or 0)
        raw=self.rfile.read(length).decode() if length else '{}'
        try:
            data=json.loads(raw)
        except Exception:
            data={}
        if self.path == '/v1/chat/completions':
            messages=data.get('messages') or []
            text=''
            if messages:
                text=messages[-1].get('content','')
            self._send({'id':'chatcmpl-mock','object':'chat.completion','choices':[{'index':0,'message':{'role':'assistant','content':f'Mock Hermes acknowledged: {text}'},'finish_reason':'stop'}]})
        else:
            self._send({'error':'not found','path':self.path},404)
    def log_message(self, format, *args):
        print('%s - %s' % (self.address_string(), format % args), flush=True)

if __name__ == '__main__':
    ThreadingHTTPServer(('0.0.0.0', 8642), Handler).serve_forever()
