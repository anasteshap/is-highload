FROM maven:3.8.4-openjdk-17 as build

WORKDIR /

COPY storage-component /storage-component
COPY server-component /server-component
#COPY client-component /client-component

RUN cd storage-component
RUN cd server-component
RUN #cd client-component

#ADD target/storage-service-1.0-SNAPSHOT.jar storage-service.jar
#CMD ["java", "-jar", "storage-service.jar"]
FROM openjdk:17.0.1-jdk-slim

#COPY --from=builder /app/target/*.jar /app/*.jar
COPY --from=build /storage-component/build/libs/storage-component-1.0-SNAPSHOT.jar /storage-component.jar
COPY --from=build /server-component/build/libs/server-component-1.0-SNAPSHOT.jar /server-component.jar
#COPY --from=build /client-component/build/libs/client-component-1.0-SNAPSHOT.jar /client-component.jar

EXPOSE 8181

ENTRYPOINT ["java","-jar","server-component.jar"]
#ENTRYPOINT ["java","-jar","storage-service.jar"]
#ENTRYPOINT ["java", "-jar", "/app/*.jar"]

#COPY . /app/.
