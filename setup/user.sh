#!/bin/bash

# Remove useless files
rm -f {manufacturer,daemon}{.key,.pem,.crt,.p12,truststore.jks} manufacturer.srl
rm -f manufacturer_{private,public}.pem

# Setup the network connection
sudo tee -a /etc/network/interfaces > /dev/null <<EOF

auto eth0
iface eth0 inet static          
	address 192.168.1.100
	netmask 255.255.255.0
	gateway 192.168.1.1
EOF

sudo ip link set eth0 down
sudo ip link set eth0 up
sudo systemctl restart NetworkManager
sudo systemctl enable NetworkManager

echo
echo "Ready! Run \`./run app 192.168.1.1\` to connect to the car."
