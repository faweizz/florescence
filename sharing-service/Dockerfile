FROM gradle:7-jdk8
COPY . /usr/src/sharing-service
WORKDIR /usr/src/sharing-service
RUN gradle installDist

FROM openjdk:8-jdk
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=0 /usr/src/sharing-service/build/install/sharing-service/ /app/
WORKDIR /app/bin
CMD ["./sharing-service"]