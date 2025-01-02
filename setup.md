# Setup instructions

## Virtual machine setup

All components are designed to work with any network topology and IP addresses, as long as the app can connect to the car manufacturer and the car's manufacturer can connect to the daemon manufacturer. This includes running all programs in the same machine. It is also possible to have more than one user machine and more than one car manufacturer machine.

In a real-world scenario, it is recommended to use a topology with a DMZ, which can be emulated with the following virtual machines:

- The **user's machine**, connected to subnet 1.
- The **car manufacturer machine**, connected to subnet 1.
- The **daemon manufacturer machine**, connected to subnets 1 and 2.
- The **daemon database machine**, connected to subnet 2.

In this configuration, subnet 1 represents the Internet and subnet 2 represents the daemon's internal network.


## Registering users in the car manufacturer

Each user is authenticated with a password and a secret key. Both in the car manufacturer and in the app, the user's secret key is stored in a key file encrypted with a key derived from the user's password.

To create a key file, run the app (`./run app`) and input the username and password. A new key file `./app-key.p12` will be created using the given password. This file will be (by default) used by the app to authenticate to the manufacturer.

To register the new user in the manufacturer, copy the generated key file to `./manufacturer-db/users/<username>/key.p12` in the car manufacturer:

```sh
mkdir -p ./manufacturer-db/users/alice
cp /path/to/app-key.p12 ./manufacturer-db/users/alice/key.p12
```


## Setting up TLS

<!-- TODO: Which files need to be in which machines? -->

```sh
openssl genrsa -out manufacturer.key
openssl genrsa -out daemon.key
openssl req -new -key manufacturer.key -out manufacturer.csr
openssl req -new -key daemon.key -out daemon.csr
openssl x509 -req -days 365 -in manufacturer.csr -signkey manufacturer.key -out manufacturer.crt
echo 01 > manufacturer.srl 
openssl x509 -req -days 365 -in daemon.csr -CA manufacturer.crt -CAkey manufacturer.key -out daemon.crt
openssl x509 -in manufacturer.crt -out manufacturer.pem
openssl x509 -in daemon.crt -out daemon.pem
openssl pkcs12 -export -in manufacturer.crt -inkey manufacturer.key -out manufacturer.p12
openssl pkcs12 -export -in daemon.crt -inkey daemon.key -out daemon.p12
keytool -import -trustcacerts -file daemon.pem -keypass changeme -storepass changeme -keystore manufacturertruststore.jks
keytool -import -trustcacerts -file manufacturer.pem -keypass changeme -storepass changeme -keystore daemontruststore.jks
```


## Creating the daemon's key pair

This key pair is used to guarantee the integrity of the updates.

To generate the key pair, run the following commands in the project directory in the daemon manufacturer machine:

```sh
openssl genpkey -algorithm RSA -out daemon_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in daemon_private.pem -out daemon_public.pem
```

Afterwards, copy `./daemon_public.pem` to the car manufacturer machine.


## Database setup

1. Install MariaDB.
2. Run `./setup_tables_db.sh`.
<!-- TODO: mariadb-secure-installation -->
