"""解析模块联调演示。

演示内容：
  1. 万象服务商未配置时调用会明确报错（满足「服务商先空着」）。
  2. 用 Mock 服务商跑通：淘宝链接 -> JSON -> 解码上货 payload。
  3. 1688 链接同样流程。
  4. 批量解析（URL 文件）。
  5. 违禁词校验（词库占位）。

运行：python -m parse_service.demo
"""
from __future__ import annotations

import json
import os
import tempfile

from .models import Platform
from .providers import WanxiangProvider, MockProvider
from .service import ParseService, ForbiddenWordChecker


def _print(title: str) -> None:
    print("\n" + "=" * 60)
    print(title)
    print("=" * 60)


def main() -> None:
    # --- 1. 万象服务商未配置：调用应明确报错 ---
    _print("1) 万象服务商（未配置 base_url / access_token）")
    wx = WanxiangProvider()  # 空配置
    svc_wx = ParseService(wx)
    res = svc_wx.parse_single("https://item.taobao.com/item.htm?id=123", Platform.TAOBAO)
    print("status:", res.status.value)
    print("error :", res.error)
    assert res.status.value == "FAILED"

    # --- 2. Mock 服务商：淘宝链接 -> JSON -> 解码上货 ---
    _print("2) Mock 服务商：淘宝链接 -> 标准化 JSON -> 解码上货")
    svc = ParseService(MockProvider(), ForbiddenWordChecker(["仿品", "假货"]))
    tb_url = "https://item.taobao.com/item.htm?id=661234567890"
    r = svc.parse_single(tb_url, Platform.TAOBAO)
    print("解析成功:", r.is_ok(), "| 标题:", r.title, "| SKU数:", len(r.skus))
    payload = svc.decode_for_publish(r, target_shop_id="SHOP_DEMO_001")
    print("解码上货 payload（节选）:")
    print(json.dumps({k: payload[k] for k in ("shopId", "title", "price", "skus")}, ensure_ascii=False, indent=2))

    # --- 3. 1688 链接 ---
    _print("3) Mock 服务商：1688 链接")
    r1688 = svc.parse_single("https://detail.1688.com/offer/661234567890.html", Platform.ALIBABA1688)
    print("解析成功:", r1688.is_ok(), "| 平台:", r1688.platform.value)

    # --- 4. 批量解析（写个临时 URL 文件） ---
    _print("4) 批量解析（URL 文件）")
    tmp = os.path.join(tempfile.gettempdir(), "parse_urls.txt")
    with open(tmp, "w", encoding="utf-8") as f:
        f.write("# 每行一个链接\n")
        f.write("https://item.taobao.com/item.htm?id=111\n")
        f.write("https://detail.1688.com/offer/222.html\n")
    batch = svc.parse_batch_file(tmp, Platform.TAOBAO)
    print(f"批量解析 {len(batch)} 条，成功 {sum(1 for x in batch if x.is_ok())} 条:")
    for x in batch:
        print("  -", x.platform.value, x.status.value, x.title)

    # --- 5. 违禁词校验 ---
    _print("5) 违禁词校验（命中示例）")
    bad = svc.parse_single("https://item.taobao.com/item.htm?id=999", Platform.TAOBAO)
    bad.title = "高仿品 假货 清仓"
    hits = svc.forbidden.check(bad.title)
    print("标题命中违禁词:", hits)

    _print("演示完成 ✅")
    print("要点：解析服务尚未接入（WanxiangProvider 留空）；上货解码桥接已可用。")


if __name__ == "__main__":
    main()
