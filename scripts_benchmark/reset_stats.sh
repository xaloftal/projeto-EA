#!/bin/bash

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║              RESETAR ESTATÍSTICAS DO POSTGRESQL               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Verificar se o container está a correr
if ! docker ps | grep -q catchit-db; then
    echo -e "${RED}❌ Container não está a correr!${NC}"
    exit 1
fi

echo -e "\n${YELLOW}📊 A resetar estatísticas...${NC}"

# Resetar todas as estatísticas
docker exec catchit-db psql -U postgres -d catchitdb << 'EOF'
SELECT pg_stat_reset();
SELECT pg_stat_reset_shared('bgwriter');
SELECT pg_stat_reset_slru();
\echo '✅ Estatísticas resetadas com sucesso!'
EOF

echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         ESTATÍSTICAS RESETADAS!                                 ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}💡 Agora podes executar o benchmark:${NC}"
echo -e "   ./run-ticketing-benchmark.sh"
echo ""
echo -e "${YELLOW}💡 Depois do benchmark, executa:${NC}"
echo -e "   ./export_index_stats.sh"