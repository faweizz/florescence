keytool -keystore kafka.server.keystore.jks -alias localhost -keyalg RSA -validity 365 -genkey -ext SAN=IP:127.0.0.1 #generate broker keystore
openssl req -new -x509 -keyout ca-key -out ca-cert -days 365 #generate ca cert and key
keytool -keystore kafka.client.truststore.jks -alias CARoot -importcert -file ca-cert #import ca cert to client truststore
keytool -keystore kafka.server.truststore.jks -alias CARoot -importcert -file ca-cert #import ca cert to broker truststore
keytool -keystore kafka.server.keystore.jks -alias localhost -certreq -file cert-file #export broker cert
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:password #sign broker cert
keytool -keystore kafka.server.keystore.jks -alias CARoot -importcert -file ca-cert #import ca cert to broker keystore
keytool -keystore kafka.server.keystore.jks -alias localhost -importcert -file cert-signed #import signed broker cert to broker keystore