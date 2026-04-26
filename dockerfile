FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

RUN mvn clean install -DskipTests

CMD ["mvn", "spring-boot:run"]