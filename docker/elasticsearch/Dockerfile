# Elasticsearch 8.12.2 + IK 中文分词（infinilabs 官方分发）
# 构建阶段会下载 IK 插件；若网络受限，可按 docs/ES-NOTE.md 改为离线安装。
# 国内拉取：docker.m.daocloud.io 为 DaoCloud 公共镜像代理（等同 Docker Hub library/elasticsearch）
FROM docker.m.daocloud.io/library/elasticsearch:8.12.2

RUN bin/elasticsearch-plugin install --batch \
    "https://get.infini.cloud/elasticsearch/analysis-ik/8.12.2"
