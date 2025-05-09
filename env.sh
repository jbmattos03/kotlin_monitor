#!/bin/bash
IP_ADDRESS=$(ip route get 8.8.8.8 | grep -oP 'src \K[^ ]+')
HOST=$(hostnamectl | grep -oP "Static hostname\s*: \K.+")

# Check if the .env file exists; if not, create it
if [ ! -e ".env" ]; then
    touch ./.env
fi

# Check if the IP_ADDR variable exists in the .env file
# If it does, update it
# If not, add it
if grep -q "^IP_ADDR=" ./.env; then
    sed -i "s/^IP_ADDR=.*/IP_ADDR=$IP_ADDRESS/" ./.env
else
    echo "IP_ADDR=$IP_ADDRESS" >> ./.env
fi

# Check if the HOSTNAME variable exists in the .env file
# If it does, update it
# If not, add it
if grep -q "^HOST=" ./.env; then
    sed -i "s/^HOST=.*/HOST=$HOST/" ./.env
else
    echo "HOST=$HOST" >> ./.env
fi
