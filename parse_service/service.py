"""解析编排服务。

负责：单链接解析、批量解析（文件上传）、结果存储、违禁词校验（占位）、
把标准化 JSON 解码成目标店铺上货 payload，以及通过官方 API 上货到目标店铺。
"""
from __future__ import annotations

import os
from typing import Optional

from .models import Platform, ProductParseResult, ParseStatus, Sku
from .providers import ParseProvider
from .publish.top_client import TopClient, TopApiError


class ForbiddenWordChecker:
    """违禁词校验（对应原系统 checkTitle/checkBrand/checkTitleShort 开关）。

    ⚠️ 词库先空着，后续填入即生效。返回命中列表。
    """

    def __init__(self, words: Optional[list[str]] = None):
        self.words = list(words or [])

    def check(self, text: str) -> list[str]:
        if not text:
            return []
        hits = [w for w in self.words if w and w in text]
        return hits


class ParseService:
    def __init__(self, provider: ParseProvider, forbidden: Optional[ForbiddenWordChecker] = None):
        self.provider = provider
        self.forbidden = forbidden or ForbiddenWordChecker()
        self.store: dict[str, ProductParseResult] = {}  # source_url -> 结果

    # ---- 单链接解析（对应 /goods/parse-item） ----
    def parse_single(self, url: str, platform: Platform = Platform.TAOBAO) -> ProductParseResult:
        try:
            res = self.provider.parse_single(url, platform)
            # 违禁词校验（标题/品牌）
            hits = self.forbidden.check(res.title) + self.forbidden.check(res.brand or "")
            if hits:
                res.error = f"命中违禁词: {hits}"
                # 保留结果但标记状态，交由上层决定是否阻断
            if res.status == ParseStatus.PENDING and not res.error:
                res.status = ParseStatus.SUCCESS
        except NotImplementedError as e:
            res = ProductParseResult(
                source_url=url, platform=platform,
                status=ParseStatus.FAILED, error=str(e),
            )
        except Exception as e:  # 真实服务商接入后的网络/解析异常
            res = ProductParseResult(
                source_url=url, platform=platform,
                status=ParseStatus.FAILED, error=f"解析异常: {e}",
            )
        self.store[url] = res
        return res

    # ---- 批量解析（对应 /goods/parse-task/upload） ----
    def parse_batch_file(self, file_path: str, platform: Platform = Platform.TAOBAO) -> list[ProductParseResult]:
        urls = self._read_urls(file_path)
        results = [self.parse_single(u, platform) for u in urls]
        return results

    @staticmethod
    def _read_urls(file_path: str) -> list[str]:
        ext = os.path.splitext(file_path)[1].lower()
        urls: list[str] = []
        if ext == ".csv":
            import csv
            with open(file_path, "r", encoding="utf-8") as f:
                for row in csv.reader(f):
                    if row and row[0].strip():
                        urls.append(row[0].strip())
        else:  # .txt 默认每行一个链接
            with open(file_path, "r", encoding="utf-8") as f:
                for line in f:
                    u = line.strip()
                    if u and not u.startswith("#"):
                        urls.append(u)
        return urls

    # ---- 解码上货（桥接「上新」执行器：对应 autoPublish / 对 js 解码上货） ----
    def decode_for_publish(self, res: ProductParseResult, target_shop_id: str) -> dict:
        """把标准化商品 JSON 解码成目标店铺的上货 payload。

        这是「解析」与「上新」的衔接点：上游解析出的 JSON 在这里映射成
        目标店铺能消费的上架报文。字段映射规则随目标平台不同而不同，
        此处给出通用映射，实际接入时按目标店铺 API 调整。
        """
        return {
            "shopId": target_shop_id,
            "title": res.title,
            "subtitle": res.subtitle,
            "images": res.main_images,
            "detailImages": res.detail_images,
            "price": res.price,
            "originalPrice": res.original_price,
            "currency": res.currency,
            "category": res.category,
            "brand": res.brand,
            "description": res.description,
            "skus": [
                {
                    "props": s.props,
                    "price": s.price,
                    "originalPrice": s.original_price,
                    "stock": s.stock,
                    "image": s.image,
                }
                for s in res.skus
            ],
            "sourceUrl": res.source_url,
            "sourcePlatform": res.platform.value,
        }

    # ---- 通过官方 API 上货到目标淘宝店铺（对应 taobao.item.add） ----
    def publish_to_shop(self, res: ProductParseResult, target_shop_id: str,
                        top_client: Optional[TopClient] = None,
                        cid: Optional[int] = None, **kwargs) -> dict:
        """通过淘宝开放平台官方 API 上货到目标店铺。

        用户决策（2026-10-12）：上货走官方 API（个人店铺 + 店铺授权 session 即可）。
        解析出的标准化商品在此直接调 TopClient.add_item 发布。
        top_client 需先配置好 app_key / app_secret / session。
        target_shop_id 仅用于记录/日志；真实发布由 top_client.session 绑定的店铺授权决定。
        """
        if top_client is None:
            raise NotImplementedError(
                "未配置淘宝开放平台客户端：传入 TopClient(app_key=..., app_secret=..., session=...) "
                "再调用 publish_to_shop。"
            )
        kwargs.setdefault("approve_status", "onsale")
        return top_client.add_item(res, cid=cid, **kwargs)
