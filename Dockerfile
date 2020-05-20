FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1 AS builder
WORKDIR /build
COPY . .
RUN sbt assembly

FROM openjdk:11
WORKDIR /app
COPY --from=builder /build/target/scala-2.13/user-service.jar .
CMD ["java", "-jar", "user-service.jar"]