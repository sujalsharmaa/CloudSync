#!/bin/bash

PUBLIC_IP=$(curl -s https://icanhazip.com)
END=$((SECONDS+60))

while [ $SECONDS -lt $END ]; do
    echo "${PUBLIC_IP}"
    sleep 1
done