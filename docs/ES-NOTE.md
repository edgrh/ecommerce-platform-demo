# Elasticsearch 商品检索

已实现：**关键词搜索**走 Elasticsearch；**货架列表**仍为 MySQL + Redis 缓存。

## IK 中文分词

- 使用 **自定义镜像** `docker/elasticsearch/Dockerfile`：基于 ES 8.12.2，通过官方渠道安装 **IK**（`https://get.infini.cloud/elasticsearch/analysis-ik/8.12.2`）。
- 索引字段 `title` / `subtitle` / `detail` 使用 **ik_max_word** 建索引、**ik_smart** 做查询分词（见 `ProductSpuDocument`）。
- 首次或更新 mapping 后请 **重新构建并启动 ES**，再启动业务服务以全量重建索引：

在仓库根目录执行：

```bash
docker compose build elasticsearch
docker compose up -d elasticsearch
```

若插件下载失败（网络限制），可开代理后重试构建，或到 [release.infinilabs.com](https://release.infinilabs.com/analysis-ik/stable/) 下载对应 zip，改为 Dockerfile 里 `COPY` + `elasticsearch-plugin install file:///...`。

## 本地运行

1. `docker compose up -d`（含 ES，端口 **9200**）。
2. `spring.elasticsearch.uris: http://localhost:9200`。
3. `ProductEsIndexRunner` 启动时 **删索引并重建 mapping** 后从 MySQL 全量导入；商家新建 SPU 会增量写入 ES。

## 未启动 ES 时

- 启动日志会出现 reindex 失败告警，应用仍可启动。
- 关键词搜索会 **回退** 到 MySQL `LIKE`。

## 生产扩展

- 大规模同步可用 **Canal / Debezium** 替代每次启动全量 reindex（当前为演示简化）。
