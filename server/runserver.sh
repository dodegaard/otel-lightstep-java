export LS_ACCESS_TOKEN=NNClrk9E/HMFB1TZ8ouaFL93ibW+3aMuTRU2J9sEpV1J6d2Eb/NscB7VTDMvJOIBUkZA8wSRDniyU2ut93VezzXPnCFkl9KEFsjzeMsw
export OTEL_SERVICE_NAME=test-httpserver
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=https://ingest.lightstep.com:443

java -javaagent:lightstep-opentelemetry-javaagent.jar \
     -jar 