#!/bin/bash
# chmod +x setup_tables_db.sh
# ./setup_tables_db.sh

DB_NAME="firmware_db"
DB_USER="manufacturer_user"
DB_PASSWORD="password"
IP="192.168.2.1"
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
sudo mariadb -e "USE $DB_NAME; 
DROP TABLE IF EXISTS $TABLE_NAME; 
CREATE TABLE IF NOT EXISTS $TABLE_NAME (
    id INT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL UNIQUE,
    release_date DATE NOT NULL,
    description TEXT,
    firmware_data LONGBLOB NOT NULL  -- Store the binary firmware file here
);"

#FIRMWARE_FILE_PATH="/home/kali/project/motorist/firmware/update_v1.bin"
# LOAD_FILE('$FIRMWARE_FILE_PATH') 
echo "Inserting sample data into the table..."

sudo mariadb -e "USE $DB_NAME; 
INSERT INTO $TABLE_NAME (version, release_date, description, firmware_data) VALUES
('1.0.0', '2024-01-01', 'Initial firmware release', 'aaaaaaa'),
('1.1.0', '2024-02-15', 'Bug fix and performance improvements', 'bbbbbbbbb'),
('2.0.0', '2025-06-01', 'Major update with new features', 'cccccccc');" 

# restrict database access only to host
echo "Securing MariaDB configurations..."
CONFIG_FILE="/etc/mysql/mariadb.conf.d/50-server.cnf"
sudo sed -i "s/^bind-address.*/bind-address = 0.0.0.0/" "$CONFIG_FILE"



# Restart MariaDB to apply changes
echo "Restarting MariaDB service..."
sudo systemctl restart mariadb

echo "MariaDB setup complete."
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo "Password: $DB_PASSWORD"
