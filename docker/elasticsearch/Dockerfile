# Elasticsearch 8.12.2 + IK 中文分词（infinilabs 官方分发）
# 构建需能访问 https://get.infini.cloud （国内一般可用；若失败可开代理或改用 release.infinilabs.com 手动下载后 COPY 安装）
FROM docker.m.daocloud.io/library/elasticsearch:8.12.2

RUN bin/elasticsearch-plugin install --batch \
    "https://get.infini.cloud/elasticsearch/analysis-ik/8.12.2"
