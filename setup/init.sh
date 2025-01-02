#!/bin/bash

echo "This script is part of the \"quick setup\" described in setup.md."

echo "Installing packages..."
sudo apt-get -y install iptables iptables-persistent
sudo systemctl enable netfilter-persistent

echo "Downloading dependencies and building the project... This might take a while."
mvn -q verify

echo
echo "First, let's create a user named \"alice\" for the car. Please choose a password for this user."
rm -f ./app-key.p12
sh ./run app - 0 ./app-key.p12 alice
rm -rf ./server-db
mkdir -p ./server-db/users/alice
cp ./app-key.p12 ./server-db/users/alice/key.p12

echo
echo "Generating TLS key pairs and certificates..."
rm -f {manufacturer,daemon}{.key,.pem,.crt,.p12,truststore.jks} manufacturer.srl
openssl genrsa -out manufacturer.key
openssl genrsa -out daemon.key
openssl req -batch -new -key manufacturer.key -out manufacturer.csr -passout pass:changeme
openssl req -batch -new -key daemon.key -out daemon.csr -passout pass:changeme
openssl x509 -req -days 365 -in manufacturer.csr -signkey manufacturer.key -out manufacturer.crt
echo 01 > manufacturer.srl
openssl x509 -req -days 365 -in daemon.csr -CA manufacturer.crt -CAkey manufacturer.key -out daemon.crt
openssl x509 -in manufacturer.crt -out manufacturer.pem
openssl x509 -in daemon.crt -out daemon.pem
openssl pkcs12 -export -in manufacturer.crt -inkey manufacturer.key -out manufacturer.p12 -passout pass:changeme
openssl pkcs12 -export -in daemon.crt -inkey daemon.key -out daemon.p12 -passout pass:changeme
keytool -importcert -noprompt -trustcacerts -file daemon.pem -keypass changeme -storepass changeme -keystore manufacturertruststore.jks
keytool -importcert -noprompt -trustcacerts -file manufacturer.pem -keypass changeme -storepass changeme -keystore daemontruststore.jks
rm -f "{manufacturer,daemon}{.key,.pem,.crt}"


echo
echo "Generating the manufacturer's key pair..."
rm -f manufacturer_{private,public}.pem
openssl genpkey -algorithm RSA -out manufacturer_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in manufacturer_private.pem -out manufacturer_public.pem


echo "Copying the host SSH public key to the known_hosts file..."
mkdir -p "$HOME/.ssh"
printf "\n192.168.1.1 " >> "$HOME/.ssh/known_hosts"
cat /etc/ssh/ssh_host_ed25519_key.pub >> "$HOME/.ssh/known_hosts"

echo
echo "Done! Now, shutdown and clone this VM as described in the setup instructions."
