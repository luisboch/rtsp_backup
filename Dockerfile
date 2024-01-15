FROM alpine
RUN mkdir /app
RUN apk add ffmpeg bash jq --no-cache
COPY daemon /app/daemon
COPY cron /app/cron
COPY entrypoint /app

VOLUME /conf

ENV CONFIGURATION /conf/config.json
ENV AUTO_CLEANUP_ENABLED true
ENV DATA_DIR /data

ENTRYPOINT ["/bin/bash"]
CMD ["/app/entrypoint"]


