#!/bin/bash

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}         BENCHMARK TICKETING - TRANSACIONAL                    ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""

echo -e "${YELLOW}Seleciona o nível de carga:${NC}"
echo ""
echo "1) 🐢 Normal - 200 operações (~30 segundos)"
echo "2) 🐕 Médio - 500 operações (~1 minuto)"
echo "3) 🐆 Alto - 1000 operações (~2 minutos)"
echo "4) 🦁 Muito Alto - 2000 operações (~4 minutos)"
echo "5) 🐘 Extreme - 5000 operações (~10 minutos)"
echo "6) 🦖 Stress - 10000 operações (~20 minutos)"
echo ""
echo "0) Sair"
echo ""
read -p "Escolhe uma opção (0-6): " opt

case $opt in
    0)
        echo -e "${GREEN}👋 Até à próxima!${NC}"
        exit 0
        ;;
    1)
        ITER=200
        BATCH=100
        THREADS=10
        CONCURRENT=1000
        NAME="NORMAL"
        ;;
    2)
        ITER=500
        BATCH=200
        THREADS=20
        CONCURRENT=2000
        NAME="MÉDIO"
        ;;
    3)
        ITER=1000
        BATCH=300
        THREADS=30
        CONCURRENT=3000
        NAME="ALTO"
        ;;
    4)
        ITER=2000
        BATCH=500
        THREADS=50
        CONCURRENT=5000
        NAME="MUITO ALTO"
        ;;
    5)
        ITER=5000
        BATCH=1000
        THREADS=80
        CONCURRENT=8000
        NAME="EXTREME"
        ;;
    6)
        ITER=10000
        BATCH=2000
        THREADS=100
        CONCURRENT=10000
        NAME="STRESS"
        ;;
    *)
        echo -e "${RED}Opção inválida! A usar configuração NORMAL.${NC}"
        ITER=200
        BATCH=100
        THREADS=10
        CONCURRENT=1000
        NAME="NORMAL"
        ;;
esac

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         CONFIGURAÇÃO: $NAME${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "   📊 Iterations: $ITER"
echo "   📦 Batch Size: $BATCH"
echo "   👥 Threads: $THREADS"
echo "   ⚡ Concurrent Ops: $CONCURRENT"
echo ""
echo -e "${YELLOW}⏳ A executar benchmark... (pode demorar alguns minutos)${NC}"
echo ""

cd ~/mei/pea/projeto-EA

# Executar benchmark com os valores selecionados
mvn spring-boot:run -pl backend \
    -Dspring-boot.run.profiles=db-only \
    "-Dspring-boot.run.arguments=--runTicketingBenchmark" \
    -Dbenchmark.iterations=$ITER \
    -Dbenchmark.batch.size=$BATCH \
    -Dbenchmark.threads=$THREADS \
    -Dbenchmark.concurrent.ops=$CONCURRENT

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         BENCHMARK $NAME CONCLUÍDO!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}📁 Resultados guardados em: benchmark_ticketing_*.csv${NC}"