# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy everything (Vaadin needs package.json/vite/TS files too)
COPY . .

# Package for production; Vaadin plugin will run prepare-frontend + build-frontend
RUN mvn -DskipTests -Pproduction clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Render provides $PORT. Default to 10000 for local runs.
ENV PORT=10000

# Copy the fat jar from build stage
COPY --from=build /app/target/*.jar /app/app.jar

# (Optional) expose for local docker run
EXPOSE 10000

# Run Spring Boot on Render's PORT and tell Vaadin to use production mode
CMD ["sh","-c","java -Dserver.port=$PORT -Dvaadin.productionMode=true -Dvaadin.skip.devserver=true -jar /app/app.jar"]
