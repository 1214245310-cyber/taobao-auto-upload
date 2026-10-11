"""解析模块（parse_service）

把淘宝/1688 等链接解析成标准化商品 JSON（原系统称「商品数据包 / js 格式」），
并为「上货」提供解码桥接。

设计对应原系统 API：
  /goods/parse-item/*   -> ParseService.parse_single + decode_for_publish
  /goods/parse-task/*   -> ParseService.parse_batch_file
  /goods/parse-channel -> providers.WanxiangProvider（服务商配置先空着）
"""

from .models import (
    Platform,
    ParseStatus,
    Sku,
    ProductParseResult,
)
from .providers import ParseProvider, WanxiangProvider, MockProvider
from .service import ParseService

__all__ = [
    "Platform",
    "ParseStatus",
    "Sku",
    "ProductParseResult",
    "ParseProvider",
    "WanxiangProvider",
    "MockProvider",
    "ParseService",
]
