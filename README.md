# florescence

Is a framework to unify the data sharing process in a data ecosystem by dynamically managing the permissions and data flow of a kafka cluster. The name originates from the idea of the symbiosis of insects visiting a blossom.

## setup
To setup the ecosystem you have to

1. edit /etc/hosts to forward `keycloak` to `localhost` 
2. run the script ./broker/cert/create_ca.sh, use password for keystore: `password`
3. start the stack with `docker-compose up`
4. go to `keycloak:8080`, login with the credentials admin/admin and import the realm under `keycloak-config.json`
5. update the secret of the users `alice` to `alice-secret` and `bob` to `bob-secret`
6. import the project under ./test to IntelliJ and run the tests to see how the framework works
