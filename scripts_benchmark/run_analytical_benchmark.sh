#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         ANALYTICAL BENCHMARK - PostgreSQL Query Analysis     ║"
echo "║         Com warmup e cálculo de médias                        ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Verificar container
if ! docker ps | grep -q catchit-db; then
    echo -e "${YELLOW}⚠️ Container não está a correr. A iniciar...${NC}"
    docker start catchit-db
    sleep 3
fi

# Criar diretórios se não existirem
mkdir -p analytical_queries
mkdir -p results

echo ""
echo "Escolhe a configuração:"
echo "1) Rápida (1 warmup, 3 runs)"
echo "2) Normal (1 warmup, 5 runs) - RECOMENDADO"
echo "3) Extensa (2 warmup, 10 runs)"
echo "4) Personalizado"
read -p "Opção: " opt

case $opt in
    1)
        WARMUP=1
        RUNS=3
        ;;
    2)
        WARMUP=1
        RUNS=5
        ;;
    3)
        WARMUP=2
        RUNS=10
        ;;
    4)
        read -p "Número de warmup runs: " WARMUP
        read -p "Número de runs para média: " RUNS
        ;;
    *)
        WARMUP=1
        RUNS=5
        ;;
esac

echo ""
echo -e "${GREEN}🔧 Configuração:${NC}"
echo "   Warmup runs: $WARMUP (descartadas)"
echo "   Benchmark runs: $RUNS (para média)"
echo ""

# Executar benchmark
python3 analytical_benchmark.py --warmup $WARMUP --runs $RUNS

echo ""
echo -e "${GREEN}✅ Benchmark concluído!${NC}"
echo -e "${YELLOW}📁 Resultados em: results/${NC}"