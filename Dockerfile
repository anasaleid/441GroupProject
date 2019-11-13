FROM openjdk:latest
ADD target/scala-2.12/441project-assembly-0.1.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]