FROM alpine
RUN mkdir /app
RUN apk add ffmpeg bash jq --no-cache
RUN apk add tzdata
RUN ln -s /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime

COPY daemon /app/daemon
COPY cron /app/cron
COPY entrypoint /app

VOLUME /conf

ENV CONFIGURATION '{}'
ENV AUTO_CLEANUP_ENABLED true
ENV DATA_DIR /data

ENTRYPOINT ["/bin/bash"]
CMD ["/app/entrypoint"]