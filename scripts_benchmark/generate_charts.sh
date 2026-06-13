#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         GERADOR DE GRÁFICOS - ANALYTICAL BENCHMARK           ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

cd ~/mei/pea/projeto-EA/scripts_benchmark

# Verificar Python
if ! command -v python3 &> /dev/null; then
    echo -e "${YELLOW}⚠️ Python3 não está instalado. A instalar...${NC}"
    sudo apt update && sudo apt install python3 python3-pip -y
fi

# Verificar dependências
echo -e "${YELLOW}📦 A verificar dependências...${NC}"
pip3 install pandas matplotlib numpy --user --quiet

# Executar geração de gráficos
python3 generate_analytical_charts.py

echo -e "\n${GREEN}✅ Gráficos gerados!${NC}"
echo -e "${YELLOW}📁 Resultados em: analytical_charts/analytical_report.html${NC}"