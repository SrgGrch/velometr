// CI/Docker builds run the Gradle test task as root, where Chrome refuses to launch its
// sandbox (https://crbug.com/638180). This registers a headless launcher with --no-sandbox
// so `./gradlew :frontend:wasmJsTest` works in those environments; harmless locally otherwise.
config.set({
    customLaunchers: {
        ChromeHeadlessNoSandbox: {
            base: 'ChromeHeadless',
            flags: ['--no-sandbox', '--disable-gpu'],
        },
    },
    browsers: ['ChromeHeadlessNoSandbox'],
});
