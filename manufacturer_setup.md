SSL setup:

openssl genrsa -out server.key
openssl genrsa -out manufacturer.key
openssl req -new -key server.key -out server.csr
openssl req -new -key manufacturer.key -out manufacturer.csr
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
echo 01 > server.srl 
openssl x509 -req -days 365 -in manufacturer.csr -CA server.crt -CAkey server.key -out manufacturer.crt
openssl x509 -in server.crt -out server.pem
openssl x509 -in manufacturer.crt -out manufacturer.pem
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12
openssl pkcs12 -export -in manufacturer.crt -inkey manufacturer.key -out manufacturer.p12
keytool -import -trustcacerts -file manufacturer.pem -keypass changeme -storepass changeme -keystore servertruststore.jks
keytool -import -trustcacerts -file server.pem -keypass changeme -storepass changeme -keystore usertruststore.jks

Asym keys:
openssl genpkey -algorithm RSA -out manufacturer_private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in manufacturer_private.pem -out manufacturer_public.pem
