#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         BENCHMARK COM ANÁLISE DE ÍNDICES                     ║"
echo "║         Reset → Benchmark → Export                           ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 1. Resetar estatísticas
echo -e "\n${YELLOW}[1/4] A resetar estatísticas...${NC}"
./reset_stats.sh

# 2. Perguntar nível de carga
echo -e "\n${YELLOW}[2/4] Configurar benchmark...${NC}"
echo "Escolhe o nível de carga:"
echo "1) Normal (200 ops)"
echo "2) Médio (500 ops)"
echo "3) Alto (1000 ops)"
echo "4) Muito Alto (2000 ops)"
echo "5) Extreme (5000 ops)"
echo "6) Stress (10000 ops)"
read -p "Opção: " opt

case $opt in
    1) ITER=200; BATCH=100; THREADS=10; CONCURRENT=1000; NAME="NORMAL";;
    2) ITER=500; BATCH=200; THREADS=20; CONCURRENT=2000; NAME="MEDIO";;
    3) ITER=1000; BATCH=300; THREADS=30; CONCURRENT=3000; NAME="ALTO";;
    4) ITER=2000; BATCH=500; THREADS=50; CONCURRENT=5000; NAME="MUITO_ALTO";;
    5) ITER=5000; BATCH=1000; THREADS=80; CONCURRENT=8000; NAME="EXTREME";;
    6) ITER=10000; BATCH=2000; THREADS=100; CONCURRENT=10000; NAME="STRESS";;
    *) ITER=200; BATCH=100; THREADS=10; CONCURRENT=1000; NAME="NORMAL";;
esac

echo -e "${GREEN}   Carga: $NAME (${ITER} iterações)${NC}"

# 3. Executar benchmark
echo -e "\n${YELLOW}[3/4] A executar benchmark...${NC}"
cd ~/mei/pea/projeto-EA
mvn spring-boot:run -pl backend \
    -Dspring-boot.run.profiles=db-only \
    "-Dspring-boot.run.arguments=--runTicketingBenchmark" \
    -Dbenchmark.iterations=$ITER \
    -Dbenchmark.batch.size=$BATCH \
    -Dbenchmark.threads=$THREADS \
    -Dbenchmark.concurrent.ops=$CONCURRENT

# 4. Exportar estatísticas
echo -e "\n${YELLOW}[4/4] A exportar estatísticas...${NC}"
cd ~/mei/pea/projeto-EA/scripts_benchmark
./export_index_stats.sh

echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         PROCESSO CONCLUÍDO!                                    ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"