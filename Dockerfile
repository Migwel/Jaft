FROM openjdk:17-alpine
COPY target/Jaft-0.0.1-SNAPSHOT.jar Jaft.jar
ENTRYPOINT ["java","-jar","/Jaft.jar"]
