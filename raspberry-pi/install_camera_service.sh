#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

sudo install -d -m 0755 /opt/peztz-camera
sudo install -m 0755 "${script_dir}/stream_camera.sh" /opt/peztz-camera/stream_camera.sh
sudo install -m 0644 "${script_dir}/peztz-camera.service" /etc/systemd/system/peztz-camera.service
sudo install -d -m 0750 /etc/peztz

if [[ ! -f /etc/peztz/camera-stream.env ]]; then
  sudo install -m 0600 "${script_dir}/camera-stream.env.example" /etc/peztz/camera-stream.env
  echo "Created /etc/peztz/camera-stream.env. Replace the placeholder values before starting the service."
fi

sudo systemctl daemon-reload

echo "Installation complete. Configure /etc/peztz/camera-stream.env, then run:"
echo "sudo systemctl enable --now peztz-camera"
