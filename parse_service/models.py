"""标准化商品模型 —— 原系统的「js 格式 / 商品数据包」。

所有上游（淘宝/1688/拼多多/京东…）返回的数据，最终都归一化到这个结构。
字段命名采用通用电商语义，方便后续映射到任意目标店铺的上货接口。
"""
from __future__ import annotations

import time
import uuid
from dataclasses import dataclass, field, asdict
from enum import Enum
from typing import Optional


class Platform(str, Enum):
    """支持的货源平台（与原系统 purchase-platform 枚举一致）。"""

    TAOBAO = "TAOBAO"          # 淘宝 / 天猫
    ALIBABA1688 = "ALIBABA1688"  # 1688
    PINDUODUO = "PINDUODUO"    # 拼多多
    JD = "JD"                  # 京东
    OFFLINE = "OFFLINE"        # 线下
    SUPPLIER = "SUPPLIER"      # 供应商


class ParseStatus(str, Enum):
    PENDING = "PENDING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


@dataclass
class Sku:
    """单个 SKU。props 为规格组合，如 [{name:'颜色', value:'红'}, {name:'尺码', value:'L'}]。"""

    sku_id: Optional[str] = None
    props: list[dict] = field(default_factory=list)  # [{name, value}]
    price: float = 0.0
    original_price: float = 0.0
    stock: int = 0
    image: Optional[str] = None


@dataclass
class ProductParseResult:
    """解析结果：标准化商品 JSON。

    raw 字段保留上游原始返回（对应原系统 parse-item?withRaw=true），
    便于排查与二次加工。
    """

    source_url: str
    platform: Platform
    title: str = ""
    subtitle: str = ""
    main_images: list[str] = field(default_factory=list)   # 主图
    detail_images: list[str] = field(default_factory=list)  # 详情图
    price: float = 0.0            # 售价
    original_price: float = 0.0   # 原价
    currency: str = "CNY"
    skus: list[Sku] = field(default_factory=list)
    description: str = ""         # 详情文案（HTML / 纯文本）
    category: Optional[str] = None
    brand: Optional[str] = None
    raw: Optional[dict] = None    # 上游原始数据（可空）
    status: ParseStatus = ParseStatus.PENDING
    error: Optional[str] = None
    item_id: str = field(default_factory=lambda: "PI_" + uuid.uuid4().hex[:16])
    parsed_at: float = field(default_factory=time.time)

    def to_json(self) -> dict:
        return asdict(self)

    def is_ok(self) -> bool:
        return self.status == ParseStatus.SUCCESS and not self.error
