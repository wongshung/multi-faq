# -*- coding: utf-8 -*-

from http.server import HTTPServer, BaseHTTPRequestHandler
import synonyms
import json
import os
import time
import urllib

data = {'status': 0, 'word': '', 'score': ''}
host = ('', 12345)


class Resquest(BaseHTTPRequestHandler):
    def do_GET(self):
        self.queryString = urllib.parse.unquote(self.path.split('?', 1)[1])
        # name=str(bytes(params['name'][0],'GBK'),'utf-8')
        params = urllib.parse.parse_qs(self.queryString)
        query = str(params['query'][0])
        print(query)
        #     name = params["name"][0] if "name" in params else None
        r = synonyms.nearby(query)
        data['word'] = r[0]
        # data['score'] = r[1]

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode())


if __name__ == '__main__':
    pid = os.fork()
    if pid != 0:
        os._exit(0)
    else:
        server = HTTPServer(host, Resquest)
        print("Server is started! Listening at http://%s:%s" % host)
        server.serve_forever()
        time.sleep(10)
        server.shutdown()
