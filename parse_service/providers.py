"""解析服务商适配器。

用户决策（2026-10-12）：解析走第三方服务（万邦/onebound/万象 同类），绕开企业认证门槛。
原系统把「真正的抓取/解码」外包给第三方（实测万象 WANXIANG，
baseUrl=http://121.5.176.133:8097/kunpeng-interface/...）。

本文件提供通用 ThirdPartyParseProvider（任意第三方聚合服务适配器），WanxiangProvider
作为其预设子类。服务商地址/令牌**先空着**，填入即可上线。
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Optional

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


class ThirdPartyParseProvider(ParseProvider):
    """通用第三方商品解析聚合服务适配器（万邦 / onebound / 万象 同类）。

    用户决策（2026-10-12）：解析走第三方服务，绕开企业认证门槛。
    不同服务商只是 base_url / access_token / 各平台 endpoint 不同，统一归一到本类。
    base_url / access_token 留空时调用即报错，提示「未配置」。
    """

    def __init__(self, name: str, base_url: str = "", access_token: str = "",
                 platform_endpoints: Optional[dict] = None):
        self.name = name
        self.base_url = base_url
        self.access_token = access_token
        self.platform_endpoints = dict(platform_endpoints or {})
        self.configured = bool(base_url and access_token)

    def _ensure_configured(self) -> None:
        if not self.base_url or not self.access_token:
            raise NotImplementedError(
                f"解析服务商[{self.name}]未配置：ThirdPartyParseProvider("
                f"name='{self.name}', base_url=..., access_token=...) 填入后调用。"
            )

    def parse_single(self, url: str, platform: Platform) -> ProductParseResult:
        self._ensure_configured()
        # TODO(接入服务商): POST {base_url}/{platform_endpoints.get(platform)}  Bearer {access_token}
        #   把上游返回归一化成 ProductParseResult（标题/主图/详情图/价格/SKU/详情…）
        #   结构参考 demo.py 的 MockProvider。
        raise NotImplementedError(f"{self.name}.parse_single 待接入服务商实现")


class WanxiangProvider(ThirdPartyParseProvider):
    """万象(WANXIANG)解析服务 —— 原系统实测使用的第三方，作为 ThirdPartyParseProvider 的预设。

    默认 base_url 指向原系统实测地址；access_token 仍留空（需向服务商索取）。
    """

    def __init__(self, base_url: str = "http://121.5.176.133:8097/kunpeng-interface/", access_token: str = ""):
        super().__init__(name="WANXIANG", base_url=base_url, access_token=access_token)


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


def get_provider(name: str = "wanxiang", **kw) -> ParseProvider:
    """解析服务商工厂：按名字返回对应适配器实例。

    已注册：wanxiang/万象 -> WanxiangProvider。
    其它服务商（万邦/onebound…）可直接传 ThirdPartyParseProvider(name=..., base_url=..., access_token=...)。
    """
    n = (name or "").lower()
    if n in ("wanxiang", "万象"):
        return WanxiangProvider(**kw)
    return ThirdPartyParseProvider(name=name.upper(), **kw)
