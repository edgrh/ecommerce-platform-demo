# 在仓库根执行: mvn -q -DskipTests package
# docker build -f docker/gateway/Dockerfile -t bcommerce/gateway:local .
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY bcommerce-gateway/target/bcommerce-gateway-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=k8s
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
