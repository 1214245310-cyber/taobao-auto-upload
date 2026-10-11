# 解析模块（parse_service）

把淘宝 / 1688 等链接解析成标准化商品 JSON（原系统称「商品数据包 / js 格式」），
并提供「解码上货」桥接，对接「上新」执行器。

> 当前状态：**解析服务商未接入**（万象适配器 `base_url`/`access_token` 留空），
> 用 `MockProvider` 已可跑通整条闭环。拿到服务商地址后填入即可上线。

## 目录

```
parse_service/
  models.py      # 标准化商品模型（js 格式 schema）
  providers.py   # 解析服务商适配器（WanxiangProvider 留空 / MockProvider 联调）
  service.py     # 编排：单链接/批量解析、存储、违禁词校验、解码上货
  demo.py        # 联调演示
```

## 跑起来

```bash
cd <workspace>
python -m parse_service.demo
```

## 对应原系统 API

| 原系统接口 | 本模块 |
|---|---|
| `GET /goods/parse-channel/list` | `providers.WanxiangProvider`（渠道=平台+服务商+令牌） |
| `GET/POST /goods/parse-item/*` | `service.ParseService.parse_single` |
| `POST /goods/parse-task/upload` | `service.ParseService.parse_batch_file` |
| `parse-item?withRaw=true` | `ProductParseResult.raw` 字段 |
| `parse-task` 的 `autoPublish` | `service.ParseService.decode_for_publish` |
| `checkTitle/checkBrand` | `service.ForbiddenWordChecker` |

## 接入真实服务商（步骤）

1. 拿万象（或同类）服务的 `base_url` 与 `access_token`。
2. 改 `providers.WanxiangProvider.__init__` 默认值，或调用时传入：
   ```python
   wx = WanxiangProvider(base_url="http://<host>:<port>/kunpeng-interface/taobao",
                         access_token="<your-token>")
   ```
3. 在 `WanxiangProvider.parse_single` 的 `TODO` 处实现：
   - 按 `platform` 选择端点（淘宝 / 1688 …）；
   - 带 `access_token` 调用上游，拿到商品原始数据；
   - 归一成 `ProductParseResult`（标题 / 主图 / 详情图 / 价格 / SKU / 详情 …），
     原始数据放入 `raw` 字段。
4. 用 `ParseService(wx)` 替换 `MockProvider` 即可，其余代码无需改动。

## 解码上货桥接

`decode_for_publish(res, target_shop_id)` 把标准化 JSON 映射成目标店铺上架报文。
字段映射随目标平台不同而异，接入「上新」执行器时按目标店铺 API 调整即可。
这是「解析」与「上新」两模块的衔接点（原系统 `autoPublish`）。
