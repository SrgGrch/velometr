FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY frontend ./frontend
RUN ./gradlew :frontend:wasmJsBrowserDistribution --no-daemon

FROM nginx:1.27-alpine
COPY --from=build /workspace/frontend/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
