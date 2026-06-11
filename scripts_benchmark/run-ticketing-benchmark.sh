#!/bin/bash
# run-ticketing-benchmark.sh

echo "═══════════════════════════════════════════════════════════════"
echo "         BENCHMARK TICKETING - Transacional"
echo "═══════════════════════════════════════════════════════════════"

cd ~/mei/pea/projeto-EA

mvn spring-boot:run -pl backend \
    -Dspring-boot.run.profiles=db-only \
    "-Dspring-boot.run.arguments=--runTicketingBenchmark"