# ---- Stage 1: build the app ----
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom first to leverage Docker layer cache
COPY pom.xml .
# If you use a /src/main/frontend or Vaadin resources, we still need the whole src later.

# Download deps (speeds up subsequent builds)
RUN mvn -q -e -DskipTests dependency:go-offline

# Now copy sources and build
COPY src ./src
# If you have a package.json / frontend (Vaadin), copy those too:
# COPY package*.json ./
# COPY vite*.ts tsconfig.json types.d.ts ./
# COPY node_modules ./node_modules  # (only if checked in — usually not)

# Build in production (no tests)
RUN mvn -Pproduction -DskipTests clean package

# ---- Stage 2: run the app ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the fat jar built above (matches 1.0.0 or any future version)
COPY --from=build /app/target/bjj-tech-map-*.jar app.jar

# Render provides $PORT – forward it to Spring Boot
ENV JAVA_OPTS=""
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]

