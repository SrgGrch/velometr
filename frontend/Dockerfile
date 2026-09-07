FROM eclipse-temurin:21-jdk-jammy AS build

# Kotlin/Wasm's bundled Node.js binary requires libatomic1, which isn't in
# the base JDK image.
RUN apt-get update && apt-get install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY frontend ./frontend

# "production" (default) runs Kotlin/Wasm's dead-code-elimination and webpack
# minification - the deploy-ready bundle, but slow. "development" skips those
# optimization passes for a much faster edit-rebuild loop; never use it for a
# real deployment (larger, unminified bundle, not meant to be served publicly).
ARG FRONTEND_BUILD_MODE=production
RUN set -eu; \
    if [ "$FRONTEND_BUILD_MODE" = "development" ]; then \
      ./gradlew :frontend:wasmJsBrowserDevelopmentExecutableDistribution --no-daemon; \
      cp -r frontend/build/dist/wasmJs/developmentExecutable /workspace/dist; \
    else \
      ./gradlew :frontend:wasmJsBrowserDistribution --no-daemon; \
      cp -r frontend/build/dist/wasmJs/productionExecutable /workspace/dist; \
    fi

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
