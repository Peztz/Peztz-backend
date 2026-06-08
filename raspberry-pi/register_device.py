import socket
import subprocess
import uuid
import requests

# 서버 주소
SERVER_URL = "http://34.50.7.78:8080/api/raspberrypis/register"
#http://192.168.150.113:8080/api/raspberrypis/register"

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
    data = {
        "macAddress": get_mac().upper(),
        "lastIp": get_register_ip()
    }
    print(f"전송 데이터: {data}")
    
    try:
        res = requests.post(SERVER_URL, json=data)
        print(f"서버 응답: {res.status_code} / {res.text}")
    except Exception as e:
        print(f"전송 실패: {e}")

if __name__ == "__main__":
    send_info()

    
