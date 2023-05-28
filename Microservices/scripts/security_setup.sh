#!/bin/bash

# Set the output directory
output_dir="Credentials"

# Generate the key pair:
openssl genrsa -out "$output_dir/server.key"

# Create a self-signed server certificate
openssl req -new -key "$output_dir/server.key" -out "$output_dir/server.csr" -subj "/C=PT/ST=Lisbon/L=Alameda/O=Quarkus/OU=Quarkus/CN=undefined/emailAddress=undefined"
openssl x509 -req -days 365 -in "$output_dir/server.csr" -signkey "$output_dir/server.key" -out "$output_dir/server.crt"

echo "Server's self-signed certificate:"
openssl x509 -in "$output_dir/server.crt" -noout -text
