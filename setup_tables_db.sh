#!/bin/bash
# chmod +x setup_tables_db.sh
# ./setup_tables_db.sh

DB_NAME="firmware_db"
DB_USER="manufacturer_user"
DB_PASSWORD="password"
IP="127.0.0.1"
TABLE_NAME="firmware_updates"


echo "Starting MariaDB setup..."


echo "Securing MariaDB installation..."
sudo mariadb -e "DELETE FROM mysql.user WHERE User='';"
sudo mariadb -e "DROP DATABASE IF EXISTS test;"
sudo mariadb -e "FLUSH PRIVILEGES;"

# Create the database
echo "Creating database: $DB_NAME..."
sudo mariadb -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"

# Create a user for the manufacturer server 
echo "Creating user: $DB_USER with placeholder IP: $IP..."
sudo mariadb -e "CREATE USER IF NOT EXISTS '$DB_USER'@'$IP' IDENTIFIED BY '$DB_PASSWORD';"

# Grant privileges to the user
echo "Granting privileges to user: $DB_USER on database: $DB_NAME..."
sudo mariadb -e "GRANT SELECT, INSERT, UPDATE, DELETE ON $DB_NAME.* TO '$DB_USER'@'$IP';"
sudo mariadb -e "FLUSH PRIVILEGES;"

# Create the firmware updates table
echo "Creating table: $TABLE_NAME in database: $DB_NAME..."
sudo mariadb -e "USE $DB_NAME; CREATE TABLE IF NOT EXISTS $TABLE_NAME (
    id INT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL UNIQUE,
    release_date DATE NOT NULL,
    description TEXT,
    firmware_data LONGBLOB NOT NULL  -- Store the binary firmware file here
);"

FIRMWARE_FILE_PATH="/home/kali/project/motorist/firmware/update_v1.bin"
echo "Inserting sample data into the table..."
# LOAD_FILE('$FIRMWARE_FILE_PATH') this is not working 
sudo mariadb -e "USE $DB_NAME; 
INSERT INTO $TABLE_NAME (version, release_date, description, firmware_data) VALUES
('1.0.0', '2024-01-01', 'Initial firmware release', 'aaaa'),
('1.1.0', '2024-02-15', 'Bug fix and performance improvements', 'vvvvv'),
('2.0.0', '2025-06-01', 'Major update with new features', 'bbbbb');" 

# restrict database access only to host
echo "Securing MariaDB configurations..."
CONFIG_FILE="/etc/mysql/mariadb.conf.d/50-server.cnf"
sudo sed -i "s/^bind-address.*/bind-address = 127.0.0.1/" "$CONFIG_FILE"

# If we want to use SSL for MariaDB, we need to set up certificates
# echo "Enabling TLS for MariaDB..."
# echo "
# [mysqld]
# ssl-cert=/etc/mysql/ssl/server-cert.pem
# ssl-key=/etc/mysql/ssl/server-key.pem
# ssl-ca=/etc/mysql/ssl/ca-cert.pem
# " | sudo tee -a "$CONFIG_FILE" > /dev/null

# Restart MariaDB to apply changes
echo "Restarting MariaDB service..."
sudo systemctl restart mariadb

echo "MariaDB setup complete."
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo "Password: $DB_PASSWORD"
echo "Replace the IP ($IP) with the actual manufacturer server IP when known."
