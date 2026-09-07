# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY backend ./backend
# Keep build state between runs; export the distribution outside cache mounts
# so the runtime stage can COPY it. Each service owns its caches.
RUN --mount=type=cache,id=velometr-backend-gradle,target=/root/.gradle,sharing=locked \
    --mount=type=cache,id=velometr-backend-state,target=/workspace/.gradle,sharing=locked \
    --mount=type=cache,id=velometr-backend-kotlin,target=/workspace/.kotlin,sharing=locked \
    --mount=type=cache,id=velometr-backend-root-build,target=/workspace/build,sharing=locked \
    --mount=type=cache,id=velometr-backend-build,target=/workspace/backend/build,sharing=locked \
    ./gradlew :backend:installDist --no-daemon --build-cache \
    && mkdir -p /out \
    && cp -a backend/build/install/backend /out/backend

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /out/backend /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/backend"]
