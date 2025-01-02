
systemctl status mysql

## Start MariaDB service
    sudo systemctl start mariadb

## Check MariaDB status
    sudo systemctl status mariadb
- Currently running on port: 3306 (default)

## Configure security settings (disallow remote access to root)
    sudo mariadb-secure-installation

## Log into MariaDB:
    sudo mariadb -u root -p

## MariaDB Database setup
- /setup_tables_db.sh


