FROM maven:3-sapmachine-25

WORKDIR /app

COPY . .

RUN mvn package -DskipTests

ENV SPRING_PROFILES_ACTIVE=cloud
ENV PORT=10000
EXPOSE 10000

CMD ["java", "-jar", "target/backend-1.0.0-SNAPSHOT.jar"]