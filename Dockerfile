FROM hseeberger/scala-sbt:11.0.7_1.3.10_2.13.2

RUN apt install postgresql -y

WORKDIR /app

COPY . .

RUN sbt assembly

RUN cp ./scripts/wait-for-postgres-and-start-app.sh .
RUN chmod +x wait-for-postgres-and-start-app.sh

RUN cp ./target/scala-2.13/user-service.jar .

CMD ["java", "-jar", "user-service.jar"]