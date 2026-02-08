FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/jobweaver-scheduler-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-Duser.timezone=Asia/Kolkata","-jar","/app/app.jar"]
