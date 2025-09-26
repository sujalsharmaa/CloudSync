#!/usr/bin/env bash
set -euo pipefail
sudo apt-get update
sudo apt-get install -y jq
# ensure docker group for ubuntu user
if ! id -nG ubuntu | grep -q docker; then
  sudo usermod -aG docker ubuntu || true
fi