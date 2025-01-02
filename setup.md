# Setup instructions

## Virtual machine setup

All components are designed to work with any network topology and IP addresses, as long as the app can connect to the car server and the car's server can connect to the manufacturer server. This includes running all programs in the same machine. It is also possible to have more than one user machine and more than one car's machine.

In a real-world scenario, it is recommended to use a topology with a DMZ, which can be emulated with the following virtual machines:

- The **user's machine**, connected to subnet 1 (192.168.1.100).
- The **car's machine**, connected to subnet 1 (192.168.1.1).
- The **manufacturer server machine**, connected to subnets 1 (192.168.1.2) and 2 (192.168.2.1).
- The **manufacturer database machine**, connected to subnet 2 (192.168.2.2).

In this configuration, subnet 1 represents the Internet and subnet 2 represents the manufacturer's internal network.


## Quick setup for testing in VMs

These instructions are based on the [virtual networking lab](https://github.com/tecnico-sec/Virtual-Networking).

1. Create a Kali Linux 2024.3 VM in VirtualBox.
1. Make sure it is connected to a NAT in the network settings.
1. Boot up the VM.
1. Copy the project folder to it.
1. Run `bash setup/init.sh` and shutdown the VM.
1. In the VM's network settings, attach the **first** the network adapter to the `sw-1` internal network with promiscuous mode set to "Allow VMs".
1. Clone the VM 3 times for a total of 4 VMs (see above) and name them accordingly, with the clone type set to "Linked clone" and the MAC address policy set to "Generate new MAC addresses for all network adapters".
1. In the **manufacturer server machine**'s network settings, attach the **second** network adapter to the `sw-2` internal network with promiscuous mode set to "Allow VMs".
1. In the **manufacturer database machine**'s network settings, change the **first** network adapted, attaching it to the `sw-2` internal network instead of `sw-1`.
<!-- TODO: Unfinished -->

## Manual setup

### Registering users in the car server

Each user is authenticated with a password and a secret key. Both in the car server and in the app, the user's secret key is stored in a key file encrypted with a key derived from the user's password.

To create a key file, run the app (`./run app`) and input the username and password. A new key file `./app-key.p12` will be created using the given password. This file will be (by default) used by the app to authenticate to the server.

To register the new user in the server, copy the generated key file to `./server-db/users/<username>/key.p12` in the car server:

```sh
mkdir -p ./server-db/users/alice
cp /path/to/app-key.p12 ./server-db/users/alice/key.p12
```


### Setting up TLS

<!-- TODO: How do we handle key/certs distribution ? -->
Manufacturer server should have:
- manufacturer.p12
- manufacturertruststore.jks

UpdateDaemon (car server) should have:
- daemon.p12
- daemontruststore.jks

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


### Creating the manufacturer's key pair

This key pair is used to guarantee the integrity of the updates.

To generate the key pair, run the following commands in the project directory in the manufacturer server machine:

```sh
openssl genpkey -algorithm RSA -out manufacturer_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in manufacturer_private.pem -out manufacturer_public.pem
```

Afterwards, copy `./manufacturer_public.pem` to the car's machine.


### Database setup

1. Install MariaDB.
2. Run `./setup_tables_db.sh`.
<!-- TODO: mariadb-secure-installation -->
