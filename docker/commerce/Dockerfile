# 在仓库根执行: mvn -q -DskipTests package
# docker build -f docker/commerce/Dockerfile -t bcommerce/commerce:local .
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY bcommerce-commerce/target/bcommerce-commerce-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=k8s
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
