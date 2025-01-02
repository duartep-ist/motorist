#!/bin/bash

# Remove useless files
rm -f app-key.p12 {manufacturer,daemon}{.p12,truststore.jks} manufacturer.srl manufacturer_{private,public}.pem

# Setup the network connection
sudo tee -a /etc/network/interfaces > /dev/null <<EOF

auto eth0
iface eth0 inet static
	address 192.168.2.2
	netmask 255.255.255.0
EOF
sudo systemctl enable NetworkManager

echo "Starting the MariaDB service..."
sudo systemctl enable mariadb
sudo systemctl start mariadb

. ./setup/man_db_tables.sh
sleep 5

echo
echo "Rebooting..."
sudo systemctl reboot
