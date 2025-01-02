#!/bin/bash

# Remove useless files
rm -f app-key.p12 manufacturer{.p12,truststore.jks} manufacturer.srl manufacturer_private.pem

# Setup the network connection
sudo tee -a /etc/network/interfaces > /dev/null <<EOF

auto eth0
iface eth0 inet static
	address 192.168.1.1
	netmask 255.255.255.0
EOF
sudo systemctl enable NetworkManager

# Setup the firewall
echo "Setting up the firewall rules..."
sudo iptables -P INPUT DROP
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT # SSH
sudo iptables -A INPUT -p tcp --dport 5000 -j ACCEPT # Car server
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
sudo netfilter-persistent save

echo
echo "Rebooting..."
sudo systemctl reboot
