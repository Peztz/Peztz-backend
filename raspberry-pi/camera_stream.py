from flask import Flask, Response
from picamera2 import Picamera2
import cv2

app = Flask(__name__)

picam2 = Picamera2()

config = picam2.create_video_configuration(
    main={"size": (640, 480)}
)

picam2.configure(config)
picam2.start()


def generate_frames():
    while True:
        frame = picam2.capture_array()

        success, buffer = cv2.imencode(".jpg", frame)

        if not success:
            continue

        frame_bytes = buffer.tobytes()

        yield (
            b"--frame\r\n"
            b"Content-Type: image/jpeg\r\n\r\n"
            + frame_bytes +
            b"\r\n"
        )


@app.route("/")
def index():
    return """
    <html>
        <head>
            <title>Peztz Camera Stream</title>
        </head>
        <body>
            <h2>Raspberry Pi Camera Stream</h2>
            <img src="/video_feed">
        </body>
    </html>
    """


@app.route("/video_feed")
def video_feed():
    return Response(
        generate_frames(),
        mimetype="multipart/x-mixed-replace; boundary=frame"
    )


if __name__ == "__main__":
    print("===================================")
    print("Camera Stream Server Started")
    print("Open:")
    print("http://라즈베리파이IP:8001")
    print("http://라즈베리파이IP:8001/video_feed")
    print("===================================")

    app.run(
        host="0.0.0.0",
        port=8001,
        threaded=True
    )