import socket
import uuid
import requests

# 서버 주소
SERVER_URL = "http://34.50.7.78:8000/register"

def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ip = s.getsockname()[0]
    s.close()
    return ip

def get_mac():
    mac = ':'.join(['{:02x}'.format((uuid.getnode() >> i) & 0xff)
                   for i in range(0, 48, 8)][::-1])
    return mac

def send_info():
    data = {
        "ip": get_ip(),
        "mac": get_mac()
    }
    print(f"전송 데이터: {data}")
    
    try:
        res = requests.post(SERVER_URL, json=data)
        print(f"서버 응답: {res.status_code} / {res.text}")
    except Exception as e:
        print(f"전송 실패: {e}")

if __name__ == "__main__":
    send_info()