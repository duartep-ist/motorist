# Setup instructions

## Quick setup for testing in VMs

All components are designed to work with any network topology and IP addresses, as long as the app can connect to the car server and the car's server can connect to the manufacturer server. This includes running all programs in the same machine. It is also possible to have more than one user machine and more than one car's machine.

In a real-world scenario, it is recommended to use a topology with a DMZ, which can be emulated with the following virtual machines:

- The **user's machine**, connected to subnet 1 (192.168.1.100).
- The **car's machine**, connected to subnet 1 (192.168.1.1).
- The **manufacturer's server machine**, connected to subnets 1 (192.168.1.2) and 2 (192.168.2.1).
- The **manufacturer's database machine**, connected to subnet 2 (192.168.2.2).

In this configuration, subnet 1 represents the Internet and subnet 2 represents the manufacturer's internal network.

These instructions are based on the [virtual networking lab](https://github.com/tecnico-sec/Virtual-Networking).

1. Create a Kali Linux 2024.3 VM in VirtualBox.
1. Make sure it is connected to a NAT in the network settings.
1. Boot up the VM.
1. Copy the project folder to it.
1. Run `bash setup/init.sh`. If prompted to save current iptables rules, select "No" on both prompts.
1. Shutdown the VM.
1. In the VM's network settings, attach the **first** the network adapter to the `sw-1` internal network with promiscuous mode set to "Allow VMs".
1. Clone the VM 4 times and name the new VMs according to the above list, with the clone type set to "Linked clone" and the MAC address policy set to "Generate new MAC addresses for all network adapters".
1. In the **manufacturer's server machine**'s network settings, attach the **second** network adapter to the `sw-2` internal network with promiscuous mode set to "Allow VMs".
1. In the **manufacturer's database machine**'s network settings, change the **first** network adapted, attaching it to the `sw-2` internal network instead of `sw-1`.
1. Shutdown the original VM. These instructions will no longer refer the original VM but it might be helpful in case something goes wrong with the rest of the setup.
1. Boot all of the new VMs.
1. In all of the new VMs, `cd` into the project directory.
1. Run `bash setup/user.sh` in the **user's machine**. This will reboot the VM.
1. Run `bash setup/car.sh` in the **car's machine**. This will reboot the VM.
1. Run `bash setup/man_server.sh` in the **manufacturer's server machine**. This will reboot the VM.
1. Run `bash setup/man_db.sh` in the **manufacturer's database machine**. This will reboot the VM.

To run the components:
1. `cd` into the project directory in all 4 VMs.
1. In the **manufacturer's server machine**, run `sh run manufacturer 5001 192.168.2.2`.
1. In the **car's machine**, run `sh run server`.
1. In the **user's machine**, run `ssh -N -L localhost:5000:localhost:5000 kali@192.168.1.1` in a separate terminal. Write `kali` as the password.
1. In the **user's machine**, run `sh run app` in a separate terminal.

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

To generate the key pair, run the following commands in the project directory in the manufacturer's server machine:

```sh
openssl genpkey -algorithm RSA -out manufacturer_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in manufacturer_private.pem -out manufacturer_public.pem
```

Afterwards, copy `./manufacturer_public.pem` to the car's machine.


### Database setup

1. Install MariaDB.
2. Run `./setup_tables_db.sh`.


### Firewall setup (optional)

Just run the following code on each of those machines, replacing `$PORT` by the appropriate port:


**car's machine**
**manufacturer's server machine**

- For the **car's machine**, run:
  ```sh
  sudo apt-get -y install iptables iptables-persistent
  sudo systemctl enable netfilter-persistent

  sudo iptables -P INPUT DROP
  sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT # SSH
  sudo iptables -A INPUT -i lo -p tcp --dport 5000 -j ACCEPT # Car server (only accessible via the loopback interface, for SSH tunneling)
  sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
  sudo netfilter-persistent save
  ```
- For the **manufacturer's server machine**, run:
  ```sh
  sudo apt-get -y install iptables iptables-persistent
  sudo systemctl enable netfilter-persistent

  sudo iptables -P INPUT DROP
  sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT # SSH (optional)
  sudo iptables -A INPUT -p tcp --dport 5001 -j ACCEPT # Firmware update server
  sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
  sudo netfilter-persistent save
  ```
- For the **manufacturer's database machine**, run:
  ```sh
  sudo apt-get -y install iptables iptables-persistent
  sudo systemctl enable netfilter-persistent

  sudo iptables -P INPUT DROP
  sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT # SSH (optional)
  sudo iptables -A INPUT -p tcp --dport 3306 -j ACCEPT # MariaDB
  sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
  sudo netfilter-persistent save
  ```

If prompted to save current iptables rules, select "No" on both prompts. Then reboot.
