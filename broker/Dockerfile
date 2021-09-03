FROM maven:3.8-openjdk-11
RUN cd /usr/src && git clone https://github.com/faweizz/strimzi-kafka-oauth.git
WORKDIR /usr/src/strimzi-kafka-oauth
RUN mvn clean install -DskipTests

FROM wurstmeister/kafka:2.13-2.7.0

COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-common/target/kafka-oauth-common-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-server/target/kafka-oauth-server-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-server-plain/target/kafka-oauth-server-plain-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-keycloak-authorizer/target/kafka-oauth-keycloak-authorizer-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-client/target/kafka-oauth-client-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-common/target/lib/keycloak-common-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-common/target/lib/keycloak-core-*.jar $KAFKA_HOME/libs/
COPY --from=0 /usr/src/strimzi-kafka-oauth/oauth-common/target/lib/bcprov-*.jar $KAFKA_HOME/libs/

COPY cert/kafka.server.truststore.jks /var/private/ssl/kafka.server.truststore.jks
COPY cert/kafka.server.keystore.jks /var/private/ssl/kafka.server.keystore.jks

ENV KAFKA_SSL_TRUSTSTORE_LOCATION=/var/private/ssl/kafka.server.truststore.jks
ENV KAFKA_SSL_TRUSTSTORE_PASSWORD=password

ENV KAFKA_SSL_KEYSTORE_LOCATION=/var/private/ssl/kafka.server.keystore.jks
ENV KAFKA_SSL_KEYSTORE_PASSWORD=password

ENV KAFKA_SSL_KEY_PASSWORD=password

ENV KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=

COPY startup.sh /home/startup.sh
RUN chmod +x /home/startup.sh

#COPY kafka_server_jaas.conf /home/kafka_server_jaas.conf
#ENV KAFKA_OPTS -Djava.security.auth.login.config=/home/kafka_server_jaas.conf

CMD ["/home/startup.sh"]