import os
import socket
import subprocess
import uuid
import requests

SERVER_URL = os.getenv("PEZTZ_REGISTER_URL")
DEVICE_API_KEY = os.getenv("PEZTZ_DEVICE_API_KEY")

def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ip = s.getsockname()[0]
    s.close()
    return ip

def get_tailscale_ip():
    try:
        result = subprocess.run(
            ["tailscale", "ip", "-4"],
            check=False,
            capture_output=True,
            text=True,
            timeout=5
        )
    except (FileNotFoundError, subprocess.SubprocessError):
        return None

    if result.returncode != 0:
        return None

    for line in result.stdout.splitlines():
        ip = line.strip()
        if ip.startswith("100."):
            return ip
    return None

def get_register_ip():
    return get_tailscale_ip() or get_ip()

def get_mac():
    mac = ':'.join(['{:02x}'.format((uuid.getnode() >> i) & 0xff)
                   for i in range(0, 48, 8)][::-1])
    return mac

def send_info():
    if not SERVER_URL:
        raise RuntimeError("PEZTZ_REGISTER_URL is not configured")
    if not DEVICE_API_KEY:
        raise RuntimeError("PEZTZ_DEVICE_API_KEY is not configured")

    data = {
        "macAddress": get_mac().upper(),
        "lastIp": get_register_ip()
    }

    try:
        response = requests.post(
            SERVER_URL,
            json=data,
            headers={"X-Device-Api-Key": DEVICE_API_KEY},
            timeout=10,
        )
        response.raise_for_status()
        print(f"Device registration completed: HTTP {response.status_code}")
    except requests.RequestException:
        print("Device registration failed")
        raise

if __name__ == "__main__":
    send_info()

    
