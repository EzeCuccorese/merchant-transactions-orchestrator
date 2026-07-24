# Modernization Plan: Spring Boot 4.1.0 & Java 25 Upgrade

## Objectives
1. Update `orchestration-api/build.gradle`:
   - Upgrade `org.springframework.boot` plugin version to `4.1.0` (or latest 4.x).
   - Set Java `languageVersion` to `25`.
2. Inspect and resolve any API deprecations or Spring Boot 4.1 migration requirements in `orchestration-api`.
3. Ensure JaCoCo test coverage is >= 90% and verify build execution with `./gradlew check`.
4. Commit changes following Conventional Commits format in English (strictly avoiding any mention of AI).
5. Push committed changes to GitHub remote `origin`.

## Implementation Steps
- Update `orchestration-api/build.gradle` plugin and Java language version.
- Adjust Gradle wrapper / toolchain configuration if needed for Java 25 compatibility.
- Audit source code for any Spring Boot 4.1 deprecations or signature updates.
- Run `./gradlew check` to verify compilation, test suite execution, and JaCoCo coverage rules (>= 90%).
- Execute git commit and git push.
