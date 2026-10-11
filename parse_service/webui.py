"""解析模块本地演示服务（零依赖，纯标准库）。

启动：python -m parse_service.webui
然后浏览器打开终端打印的地址（默认 http://127.0.0.1:8765）。

说明：服务商未接入，页面用 MockProvider 演示「链接 → js格式JSON → 解码上货」闭环。
"""
from __future__ import annotations

import json
import os
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from .models import Platform
from .providers import MockProvider
from .service import ParseService, ForbiddenWordChecker

# 当前用 Mock 服务商（万象未配置）。接入真实服务商后替换即可。
svc = ParseService(MockProvider(), ForbiddenWordChecker(["仿品", "假货", "高仿"]))


class Handler(BaseHTTPRequestHandler):
    def _send(self, code: int, body: bytes, ctype: str) -> None:
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _json(self, obj, code: int = 200) -> None:
        self._send(code, json.dumps(obj, ensure_ascii=False).encode("utf-8"),
                   "application/json; charset=utf-8")

    def _body(self) -> dict:
        n = int(self.headers.get("Content-Length") or 0)
        if n <= 0:
            return {}
        return json.loads(self.rfile.read(n).decode("utf-8"))

    def do_GET(self):
        if self.path in ("/", "/index.html"):
            self._send(200, PAGE.encode("utf-8"), "text/html; charset=utf-8")
        elif self.path == "/api/status":
            self._json({
                "provider": {"name": svc.provider.name, "configured": svc.provider.configured},
                "forbiddenWords": svc.forbidden.words,
                "storedItems": len(svc.store),
            })
        else:
            self._json({"ok": False, "error": "not found"}, 404)

    def do_POST(self):
        try:
            data = self._body()
            if self.path == "/api/parse":
                item = svc.parse_single(data["url"].strip(), Platform(data.get("platform", "TAOBAO")))
                self._json({"ok": item.is_ok(), "item": item.to_json()})
            elif self.path == "/api/batch":
                items = [svc.parse_single(u.strip(), Platform(data.get("platform", "TAOBAO")))
                         for u in data.get("urls", []) if u.strip()]
                self._json({"ok": True, "items": [i.to_json() for i in items]})
            elif self.path == "/api/publish":
                url = data["url"].strip()
                res = svc.store.get(url) or svc.parse_single(url, Platform(data.get("platform", "TAOBAO")))
                payload = svc.decode_for_publish(res, data.get("shopId", "SHOP_DEMO_001"))
                self._json({"ok": res.is_ok(), "payload": payload, "itemError": res.error})
            elif self.path == "/api/forbidden":
                words = [w.strip() for w in data.get("words", []) if w.strip()]
                svc.forbidden.words = words
                self._json({"ok": True, "words": words})
            else:
                self._json({"ok": False, "error": "not found"}, 404)
        except Exception as e:
            self._json({"ok": False, "error": str(e)}, 400)

    def log_message(self, fmt, *args):  # 精简日志
        print("[webui]", self.address_string(), fmt % args)


PAGE = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>解析模块 · 本地演示</title>
<style>
  :root{--bg:#f6f7f9;--card:#fff;--ink:#1f2329;--sub:#646a73;--line:#e5e7eb;
        --acc:#2f66ff;--ok:#0a8f52;--bad:#c9372c;--warn:#b26b00;--chip:#eef2ff}
  *{box-sizing:border-box} body{margin:0;font:14px/1.6 system-ui,"Segoe UI","Microsoft YaHei",sans-serif;
  background:var(--bg);color:var(--ink)}
  .wrap{max-width:980px;margin:0 auto;padding:24px 16px 60px}
  h1{font-size:20px;margin:0 0 4px} .sub{color:var(--sub);font-size:12.5px;margin-bottom:18px}
  .badge{display:inline-block;padding:2px 10px;border-radius:999px;font-size:12px;background:var(--chip);color:var(--acc);margin-left:8px;vertical-align:2px}
  .card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:18px;margin-bottom:16px}
  .card h2{font-size:15px;margin:0 0 12px}
  .row{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
  input[type=text],select,textarea{border:1px solid var(--line);border-radius:8px;padding:8px 10px;font:inherit;background:#fff;color:var(--ink)}
  input[type=text]{flex:1;min-width:260px} textarea{width:100%;min-height:84px;resize:vertical}
  button{border:0;border-radius:8px;padding:9px 18px;background:var(--acc);color:#fff;font:inherit;cursor:pointer}
  button.ghost{background:#fff;color:var(--acc);border:1px solid var(--acc)}
  button:disabled{opacity:.5;cursor:not-allowed}
  pre{background:#0d1117;color:#d4e2ff;border-radius:10px;padding:14px;overflow:auto;max-height:340px;font-size:12px;line-height:1.5}
  .grid2{display:grid;grid-template-columns:1fr 1fr;gap:12px} @media(max-width:760px){.grid2{grid-template-columns:1fr}}
  .pane h3{font-size:13px;margin:0 0 8px;color:var(--sub)}
  table{width:100%;border-collapse:collapse;font-size:13px}
  th,td{text-align:left;padding:7px 8px;border-bottom:1px solid var(--line)}
  th{color:var(--sub);font-weight:500}
  .tag{padding:1px 8px;border-radius:6px;font-size:12px}
  .t-ok{background:#e6f6ee;color:var(--ok)} .t-bad{background:#fdecea;color:var(--bad)}
  .t-warn{background:#fff4e0;color:var(--warn)}
  .hint{color:var(--sub);font-size:12.5px;margin-top:10px}
  .err{color:var(--bad);font-size:12.5px;margin-top:8px}
</style>
</head>
<body>
<div class="wrap">
  <h1>解析模块 · 本地演示<span class="badge" id="prov">服务商加载中…</span></h1>
  <div class="sub">链接 → 标准化商品 JSON（js 格式）→ 解码上货 · 对应原系统 /goods/parse-item、/goods/parse-task、autoPublish</div>

  <div class="card">
    <h2>① 单链接解析</h2>
    <div class="row">
      <select id="plat">
        <option value="TAOBAO">淘宝</option>
        <option value="ALIBABA1688">1688</option>
        <option value="PINDUODUO">拼多多</option>
        <option value="JD">京东</option>
      </select>
      <input type="text" id="url" placeholder="粘贴商品链接，如 https://item.taobao.com/item.htm?id=661234567890">
      <button id="go">解析</button>
    </div>
    <div id="perr" class="err"></div>
    <div class="grid2" style="margin-top:12px">
      <div class="pane"><h3>标准化 JSON（js 格式 / 商品数据包）</h3><pre id="jout">— 尚未解析 —</pre></div>
      <div class="pane"><h3>解码上货 payload（发给上新执行器）</h3>
        <div class="row" style="margin-bottom:8px">
          <input type="text" id="shop" placeholder="目标店铺ID，默认 SHOP_DEMO_001" style="flex:1">
          <button class="ghost" id="pub">解码上货</button>
        </div>
        <pre id="pout">— 点击「解码上货」生成 —</pre>
      </div>
    </div>
  </div>

  <div class="card">
    <h2>② 批量解析（对应 parse-task /upload）</h2>
    <textarea id="urls" placeholder="每行一个链接，# 开头为注释"></textarea>
    <div class="row" style="margin-top:8px"><button id="bgo">批量解析</button><span id="bstat" class="hint"></span></div>
    <table id="btab" style="margin-top:10px;display:none">
      <thead><tr><th>平台</th><th>状态</th><th>标题</th><th>SKU</th><th>链接</th></tr></thead>
      <tbody></tbody>
    </table>
  </div>

  <div class="card">
    <h2>③ 违禁词校验</h2>
    <div class="row">
      <input type="text" id="words" placeholder="违禁词，逗号分隔（对应 checkTitle/checkBrand）" style="flex:2">
      <input type="text" id="wtext" placeholder="测试文本" style="flex:1">
      <button class="ghost" id="wgo">校验</button>
    </div>
    <div id="wout" class="hint"></div>
  </div>

  <div class="hint">服务商未配置（万象 base_url / access_token 留空），当前使用 MockProvider 联调；
  接入真实服务商后替换 providers.WanxiangProvider 配置即可，页面与流程无需改动。</div>
</div>

<script>
const $ = id => document.getElementById(id);
async function api(path, body){
  const r = await fetch(path, {method:'POST', headers:{'Content-Type':'application/json'},
                               body: JSON.stringify(body||{})});
  return r.json();
}
// 状态徽标
fetch('/api/status').then(r=>r.json()).then(s=>{
  const p = s.provider;
  $('prov').textContent = '服务商：' + p.name + (p.configured ? '（已配置）' : '（未配置 · 走 Mock）');
});
// 单链接解析
$('go').onclick = async () => {
  $('perr').textContent = '';
  const r = await api('/api/parse', {url: $('url').value, platform: $('plat').value});
  if(!r.ok && r.item && r.item.error){ $('perr').textContent = r.item.error; }
  $('jout').textContent = JSON.stringify(r.item, null, 2);
};
// 解码上货
$('pub').onclick = async () => {
  const r = await api('/api/publish', {url: $('url').value, platform: $('plat').value, shopId: $('shop').value || 'SHOP_DEMO_001'});
  $('pout').textContent = JSON.stringify(r.payload, null, 2);
};
// 批量解析
$('bgo').onclick = async () => {
  const urls = $('urls').value.split('\\n').map(s=>s.trim()).filter(s=>s && !s.startsWith('#'));
  if(!urls.length){ $('bstat').textContent = '请先粘贴链接'; return; }
  const r = await api('/api/batch', {urls, platform: $('plat').value});
  const tb = $('btab'); tb.style.display='table'; tb.querySelector('tbody').innerHTML='';
  let ok=0;
  r.items.forEach(it=>{
    if(it.status==='SUCCESS') ok++;
    const tr = document.createElement('tr');
    tr.innerHTML = '<td>'+it.platform+'</td>'+
      '<td><span class="tag '+(it.status==='SUCCESS'?'t-ok':'t-bad')+'">'+it.status+'</span></td>'+
      '<td>'+(it.title||'—')+'</td><td>'+it.skus.length+'</td>'+
      '<td style="max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">'+it.source_url+'</td>';
    tb.querySelector('tbody').appendChild(tr);
  });
  $('bstat').textContent = '共 '+r.items.length+' 条，成功 '+ok+' 条';
};
// 违禁词
$('wgo').onclick = async () => {
  const words = $('words').value.split(/[,，]/).map(s=>s.trim()).filter(Boolean);
  if(words.length) await api('/api/forbidden', {words});
  const t = $('wtext').value;
  const hits = words.filter(w=>t.includes(w));
  $('wout').innerHTML = hits.length
    ? '<span class="tag t-bad">命中：'+hits.join('、')+'</span>'
    : '<span class="tag t-ok">未命中违禁词</span>';
};
</script>
</body>
</html>"""


def main() -> None:
    port = 8765
    server = None
    for p in range(port, port + 20):
        try:
            server = ThreadingHTTPServer(("127.0.0.1", p), Handler)
            port = p
            break
        except OSError:
            continue
    if server is None:
        raise SystemExit("8765~8784 端口均被占用，无法启动本地演示服务")
    with open(os.path.join(os.path.dirname(__file__), "webui.port"), "w") as f:
        f.write(str(port))
    print(f"[webui] 解析模块本地演示已启动: http://127.0.0.1:{port}")
    print("[webui] 服务商: MockProvider（万象未配置）· 按 Ctrl+C 停止")
    server.serve_forever()


if __name__ == "__main__":
    main()
