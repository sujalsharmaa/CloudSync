#!/bin/bash

# Define the path to the docker-compose file
DOCKER_COMPOSE_KAFKA_FILE="docker-compose-kafka.yaml"

# Get the EC2 instance's public IPv4 address using instance metadata
PUBLIC_IP=$(curl -s https://icanhazip.com)

if [ -z "$PUBLIC_IP" ]; then
  echo "Error: Could not retrieve EC2 public IP address." >&2 # Send error to stderr
  exit 1
fi

echo "Detected EC2 Public IP: $PUBLIC_IP"

# Use sed to replace the placeholder with the actual IP
# No need for another 'sudo -i -u ubuntu bash -c' here, as this script is already run as ubuntu
# and will modify the file directly.
# The PUBLIC_IP variable is directly accessible here.
sed -i "s|EC2_PUBLIC_IP_PLACEHOLDER|${PUBLIC_IP}|g" "$DOCKER_COMPOSE_KAFKA_FILE"

if [ $? -eq 0 ]; then
  echo "Successfully updated $DOCKER_COMPOSE_KAFKA_FILE with public IP."
else
  echo "Error: Failed to update $DOCKER_COMPOSE_KAFKA_FILE." >&2
  exit 1
fi