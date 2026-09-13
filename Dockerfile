FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY StudentScoreWeb.java ./
COPY public ./public
RUN mkdir -p build && javac -encoding UTF-8 --release 17 -d build StudentScoreWeb.java \
    && jar --create --file app.jar --main-class StudentScoreWeb -C build . -C . public

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/app.jar ./app.jar
ENV PORT=10000
EXPOSE 10000
USER 10001
CMD ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
