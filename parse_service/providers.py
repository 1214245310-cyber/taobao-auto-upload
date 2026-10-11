"""解析服务商适配器。

原系统把「真正的抓取/解码」外包给第三方服务（实测为「万象 WANXIANG」，
baseUrl=http://121.5.176.133:8097/kunpeng-interface/...）。
本文件预留 WanxiangProvider 适配器，服务商地址/令牌**先空着**，后续填入即可。
"""
from __future__ import annotations

from abc import ABC, abstractmethod

from .models import Platform, ParseStatus, Sku, ProductParseResult


class ParseProvider(ABC):
    """解析服务商统一接口。新增任意上游只需实现这两个方法。"""

    name: str = "abstract"
    configured: bool = True  # 服务商是否已配置（前端展示用）

    @abstractmethod
    def parse_single(self, url: str, platform: Platform) -> ProductParseResult:
        """解析单个链接 -> 标准化商品 JSON。"""
        raise NotImplementedError

    def parse_batch(self, urls: list[str], platform: Platform) -> list[ProductParseResult]:
        """批量解析（默认串行；服务商支持批量接口时可覆盖）。"""
        return [self.parse_single(u, platform) for u in urls]


class WanxiangProvider(ParseProvider):
    """万象解析服务适配器。

    ⚠️ 服务商配置先空着：base_url / access_token 留空时调用会明确报错，
       提示「未配置」。后续拿到服务商地址与令牌后填入即可上线。
    """

    name = "WANXIANG"

    def __init__(self, base_url: str = "", access_token: str = ""):
        self.base_url = base_url
        self.access_token = access_token
        self.configured = bool(base_url and access_token)

    def _ensure_configured(self) -> None:
        if not self.base_url or not self.access_token:
            raise NotImplementedError(
                "解析服务商未配置：请在 WanxiangProvider(base_url=..., access_token=...) "
                "中填入万象服务的 base_url 与 access_token 后再调用。"
            )

    def parse_single(self, url: str, platform: Platform) -> ProductParseResult:
        self._ensure_configured()
        # TODO(接入服务商): 实际调用
        #   POST {base_url}/{platform_endpoint}  Headers: Authorization: Bearer {access_token}
        #   把上游返回归一化成 ProductParseResult（标题/主图/详情图/价格/SKU/详情…）
        # 示意见 demo.py 的 MockProvider，结构一致。
        raise NotImplementedError("WanxiangProvider.parse_single 待接入服务商实现")


class MockProvider(ParseProvider):
    """本地联调用的假服务商：不联网，构造结构合法的解析结果。

    用于在没有真实服务商时跑通「链接 -> JSON -> 解码上货」整条闭环。
    """

    name = "MOCK"

    def parse_single(self, url: str, platform: Platform) -> ProductParseResult:
        suffix = url[-12:] if len(url) >= 12 else url
        return ProductParseResult(
            source_url=url,
            platform=platform,
            title=f"[MOCK] 示例商品 {suffix}",
            subtitle="Mock 解析结果，用于联调",
            main_images=[
                "https://example.com/mock/main_1.jpg",
                "https://example.com/mock/main_2.jpg",
            ],
            detail_images=["https://example.com/mock/detail_1.jpg"],
            price=99.0,
            original_price=129.0,
            skus=[
                Sku(
                    props=[{"name": "颜色", "value": "红"}, {"name": "尺码", "value": "L"}],
                    price=99.0,
                    original_price=129.0,
                    stock=10,
                    image="https://example.com/mock/main_1.jpg",
                ),
                Sku(
                    props=[{"name": "颜色", "value": "蓝"}, {"name": "尺码", "value": "L"}],
                    price=99.0,
                    original_price=129.0,
                    stock=5,
                    image="https://example.com/mock/main_2.jpg",
                ),
            ],
            description="<p>这是 Mock 解析结果，验证 解析→JSON→解码上货 闭环。</p>",
            category="示例/服饰",
            brand="MockBrand",
        )
