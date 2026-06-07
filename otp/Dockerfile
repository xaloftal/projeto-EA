FROM opentripplanner/opentripplanner:2.10.0_2026-06-05T13-52

COPY data/gtfs-stcp.zip            /var/opentripplanner/gtfs-stcp.zip
COPY data/gtfs-metro.zip           /var/opentripplanner/gtfs-metro.zip
COPY data/porto_portugal.osm.pbf  /var/opentripplanner/porto_portugal.osm.pbf
COPY build-config.json             /var/opentripplanner/build-config.json

# Give the graph build enough heap (Portugal-wide needs a lot)
ENV JAVA_OPTS="-Xmx20g"

# Build the graph; the entrypoint already injects /var/opentripplanner/
RUN /docker-entrypoint.sh --build --save \
    && ls -la /var/opentripplanner

# Runtime: same trick — only the flags
CMD ["--load", "--serve"]