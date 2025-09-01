# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy sources
COPY pom.xml .
COPY src ./src

# Build a production jar (skip tests; don't start Vaadin dev server)
RUN mvn -DskipTests \
       -Dvaadin.productionMode=true \
       -Dvaadin.skip.devserver=true \
       clean package

# ---------- Run stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/bjj-tech-map-*.jar app.jar

# Render provides $PORT at runtime (e.g. 10000)
# Use shell form so $PORT gets expanded
CMD ["sh","-c","java -Dserver.port=$PORT -Dvaadin.productionMode=true -Dvaadin.skip.devserver=true -jar app.jar"]
