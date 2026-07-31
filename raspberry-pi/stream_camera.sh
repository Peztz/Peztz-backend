#!/usr/bin/env bash
set -Eeuo pipefail

: "${CAMERA_RTSP_URL:?CAMERA_RTSP_URL is required}"
: "${MEDIAMTX_PUBLISH_URL:?MEDIAMTX_PUBLISH_URL is required}"

exec ffmpeg \
  -nostdin \
  -hide_banner \
  -loglevel info \
  -rtsp_transport tcp \
  -rw_timeout 15000000 \
  -fflags +genpts \
  -use_wallclock_as_timestamps 1 \
  -i "${CAMERA_RTSP_URL}" \
  -map 0:v:0 \
  -an \
  -c:v copy \
  -f rtsp \
  -rtsp_transport tcp \
  "${MEDIAMTX_PUBLISH_URL}"
