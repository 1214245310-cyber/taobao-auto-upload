"""上货执行器子包。

用户决策（2026-10-12）：上货走淘宝开放平台官方 API（taobao.item.add 等），
个人开发者 + 自己店铺 OAuth 授权(session) 即可发布到自己店，无需企业认证。
"""
from .top_client import TopClient, TopApiError, TopClientConfig

__all__ = ["TopClient", "TopApiError", "TopClientConfig"]
