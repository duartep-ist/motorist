# Setup instructions

## Virtual machine setup

All components are designed to work with any network topology and IP addresses, as long as the app can connect to the car server and the car's server can connect to the manufacturer server. This includes running all programs in the same machine. It is also possible to have more than one user machine and more than one car server machine.

In a real-world scenario, it is recommended to use a topology with a DMZ, which can be emulated with the following virtual machines:

- The **user's machine**, connected to subnet 1.
- The **car server machine**, connected to subnet 1.
- The **manufacturer server machine**, connected to subnets 1 and 2.
- The **manufacturer database machine**, connected to subnet 2.

In this configuration, subnet 1 represents the Internet and subnet 2 represents the manufacturer's internal network.


## Registering users in the car server

Each user is authenticated with a password and a secret key. Both in the car server and in the app, the user's secret key is stored in a key file encrypted with a key derived from the user's password.

To create a key file, run the app (`./run app`) and input the username and password. A new key file `./app-key.p12` will be created using the given password. This file will be (by default) used by the app to authenticate to the server.

To register the new user in the server, copy the generated key file to `./server-db/users/<username>/key.p12` in the car server:

```sh
mkdir -p ./server-db/users/alice
cp /path/to/app-key.p12 ./server-db/users/alice/key.p12
```


## Setting up TLS

<!-- TODO: Which files need to be in which machines? -->

```sh
openssl genrsa -out server.key
openssl genrsa -out manufacturer.key
openssl req -new -key server.key -out server.csr
openssl req -new -key manufacturer.key -out manufacturer.csr
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
echo 01 > server.srl 
openssl x509 -req -days 365 -in manufacturer.csr -CA server.crt -CAkey server.key -out manufacturer.crt
openssl x509 -in server.crt -out server.pem
openssl x509 -in manufacturer.crt -out manufacturer.pem
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12
openssl pkcs12 -export -in manufacturer.crt -inkey manufacturer.key -out manufacturer.p12
keytool -import -trustcacerts -file manufacturer.pem -keypass changeme -storepass changeme -keystore servertruststore.jks
keytool -import -trustcacerts -file server.pem -keypass changeme -storepass changeme -keystore manufacturertruststore.jks
```


## Creating the manufacturer's key pair

This key pair is used to guarantee the integrity of the updates.

To generate the key pair, run the following commands in the project directory in the manufacturer server machine:

```sh
openssl genpkey -algorithm RSA -out manufacturer_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in manufacturer_private.pem -out manufacturer_public.pem
```

Afterwards, copy `./manufacturer_public.pem` to the car server machine.


## Database setup

1. Install MariaDB.
2. Run `./setup_tables_db.sh`.
<!-- TODO: mariadb-secure-installation -->
