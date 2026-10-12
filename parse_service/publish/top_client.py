"""淘宝开放平台（TOP）官方上货客户端骨架。

用户决策（2026-10-12）：上货走淘宝开放平台官方 API（taobao.item.add 等），
而非第三方。个人开发者 + 自己店铺 OAuth 授权(session) 即可发布到自己店，无需企业认证。

⚠️ 密钥先空着：app_key / app_secret / session 留空时调用即报错提示配置。
拿到后在实例化时填入即可，无需改其他代码。

签名算法：md5(secret + 排序拼接(kv) + secret)，结果大写 hex（UTF-8）。
"""
from __future__ import annotations

import hashlib
import json
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Optional

from ..models import ProductParseResult, Platform


class TopApiError(RuntimeError):
    """TOP 接口返回错误。携带 code/sub_msg 便于上层重试/记录失败原因。"""


@dataclass
class TopClientConfig:
    app_key: str = ""
    app_secret: str = ""
    session: str = ""          # 店铺 OAuth 授权码（个人店铺发布必需）
    gateway: str = "https://eco.taobao.com/router/rest"
    sign_method: str = "md5"   # 默认 md5；框架预留 hmac 扩展


class TopClient:
    """淘宝开放平台客户端（发布/管理自己的淘宝店铺商品）。

    最小可用：TopClient(app_key=..., app_secret=..., session=...).add_item(product, cid=类目ID)
    """

    def __init__(self, config: Optional[TopClientConfig] = None, **kwargs):
        self.cfg = config or TopClientConfig(**kwargs)

    # ---------- 签名 ----------
    def _sign(self, params: dict) -> str:
        if self.cfg.sign_method != "md5":
            raise ValueError(f"暂不支持的 sign_method: {self.cfg.sign_method}")
        plain = self.cfg.app_secret + "".join(
            f"{k}{params[k]}" for k in sorted(params)
        ) + self.cfg.app_secret
        return hashlib.md5(plain.encode("utf-8")).hexdigest().upper()

    # ---------- 通用调用 ----------
    def execute(self, method: str, **biz) -> dict:
        if not (self.cfg.app_key and self.cfg.app_secret):
            raise NotImplementedError(
                "淘宝开放平台未配置：TopClient(app_key=..., app_secret=..., session=...) "
                "填入你的应用密钥与店铺授权 session 后再调用。"
            )
        params: dict = {
            "method": method,
            "app_key": self.cfg.app_key,
            "session": self.cfg.session,
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "format": "json",
            "v": "2.0",
            "sign_method": self.cfg.sign_method,
        }
        # 过滤空值（TOP 不接受空字符串业务参数）
        for k, v in biz.items():
            if v is None:
                continue
            params[k] = v if isinstance(v, str) else str(v)
        params["sign"] = self._sign(params)

        data = urllib.parse.urlencode(params).encode("utf-8")
        req = urllib.request.Request(self.cfg.gateway, data=data, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                body = json.loads(resp.read().decode("utf-8"))
        except Exception as e:  # 网络/超时/非 JSON
            raise TopApiError(f"请求TOP失败: {e}")
        if "error_response" in body:
            err = body["error_response"]
            raise TopApiError(f"[{err.get('code')}] {err.get('sub_msg') or err.get('msg')}")
        # 返回对应方法的 response 节点
        resp_key = method.replace(".", "_") + "_response"
        return body.get(resp_key, body)

    # ---------- 图片上传（先传到图片空间再引用 pic_url） ----------
    def upload_image(self, img_url_or_path: str) -> str:
        """上传图片到淘宝图片空间，返回可引用 pic_url。

        ⚠️ 需调用 taobao.picture.upload；此处留接口，真实实现待接入。
        淘宝商品主图/详情图必须是图片空间 URL，不能直接用外部图链。
        """
        raise NotImplementedError("upload_image 待接入 taobao.picture.upload")

    # ---------- 发布商品 ----------
    def add_item(self, product: ProductParseResult, cid: Optional[int] = None,
                 location: str = "浙江 金华", approve_status: str = "onsale") -> dict:
        """把标准化商品发布到自己的淘宝店铺（taobao.item.add）。

        product: 解析出的标准化商品（ProductParseResult）
        cid: 目标类目 ID（必填，需先按店铺授权类目选择）
        location: 发货地（默认浙江金华，按实际改）
        approve_status: onsale=上架 / instock=入库
        """
        if cid is None:
            raise ValueError("发布商品必须指定目标类目 cid（先按店铺授权类目选）")
        # 主图第一张作为商品主图（实际需先 upload_image 拿到图片空间 URL）
        pic_url = product.main_images[0] if product.main_images else ""
        # SKU 组装：淘宝用 sku_properties / sku_quantities / sku_prices 多值字段
        sku_props, sku_prices, sku_quants, sku_outer = [], [], [], []
        for i, s in enumerate(product.skus):
            # props: [{name,value}] -> 真实需映射成 类目属性 pid#vid；此处占位为文本
            prop_text = ";".join(f"{p['name']}:{p['value']}" for p in s.props)
            sku_props.append(f"{i}:{prop_text}")     # 占位，接入时换算 pid#vid
            sku_prices.append(str(s.price))
            sku_quants.append(str(s.stock or 0))
            sku_outer.append(s.sku_id or f"OUT{i}")
        biz: dict = {
            "title": product.title,
            "price": product.price,
            "num": sum(s.stock or 0 for s in product.skus) or 1,
            "desc": product.description or product.subtitle,
            "cid": cid,
            "location": location,
            "approve_status": approve_status,
            "pic_url": pic_url,
        }
        if product.skus:
            biz.update({
                "sku_properties": ";".join(sku_props),
                "sku_quantities": ";".join(sku_quants),
                "sku_prices": ";".join(sku_prices),
                "sku_outer_ids": ";".join(sku_outer),
            })
        # 注：淘宝 SKU 必须用类目属性 pid#vid，上面 prop_text 仅为占位。
        # 接入时先用 alibaba.item.publish.props.get 拿类目 schema 再映射真实 pid#vid。
        return self.execute("taobao.item.add", **biz)
