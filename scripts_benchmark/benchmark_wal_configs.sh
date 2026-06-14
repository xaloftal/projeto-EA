#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         BENCHMARK - CONFIGURAÇÕES DE WAL/CHECKPOINT          ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

BASE_DIR="/home/gojalo/mei/pea/projeto-EA"
RESULTS_DIR="$BASE_DIR/wal_benchmark_results"
mkdir -p $RESULTS_DIR

# Configurações a testar (max_wal_size, checkpoint_completion_target, min_wal_size)
declare -a CONFIGS=(
    "1GB:0.5:512MB:default"
    "2GB:0.7:1GB:moderado"
    "4GB:0.9:1GB:escritas_intensas"
    "8GB:0.95:2GB:escritas_muito_intensas"
)

# Ficheiro de resultados
RESULT_CSV="$RESULTS_DIR/wal_benchmark_$(date +%Y%m%d_%H%M%S).csv"
echo "configuracao,max_wal_size,checkpoint_target,min_wal_size,throughput_medio,latencia_media,tempo_execucao" > $RESULT_CSV

# Função para aplicar configuração
set_wal_config() {
    local max_wal=$1
    local checkpoint=$2
    local min_wal=$3
    
    echo -e "${YELLOW}🔧 A aplicar: max_wal_size=$max_wal, checkpoint_target=$checkpoint, min_wal_size=$min_wal${NC}"
    
    docker exec catchit-db psql -U postgres -d catchitdb << EOF
ALTER SYSTEM SET max_wal_size = '$max_wal';
ALTER SYSTEM SET checkpoint_completion_target = $checkpoint;
ALTER SYSTEM SET min_wal_size = '$min_wal';
SELECT pg_reload_conf();
EOF
    sleep 2
}

# Função para verificar configuração atual
get_wal_config() {
    docker exec catchit-db psql -U postgres -d catchitdb -t -c "SHOW max_wal_size;" | tr -d ' '
    docker exec catchit-db psql -U postgres -d catchitdb -t -c "SHOW checkpoint_completion_target;" | tr -d ' '
}

# Função para forçar checkpoint (opcional)
force_checkpoint() {
    docker exec catchit-db psql -U postgres -d catchitdb -c "CHECKPOINT;"
}

# Guardar configuração original
ORIGINAL_MAX_WAL=$(docker exec catchit-db psql -U postgres -d catchitdb -t -c "SHOW max_wal_size;" | tr -d ' ')
ORIGINAL_CHECKPOINT=$(docker exec catchit-db psql -U postgres -d catchitdb -t -c "SHOW checkpoint_completion_target;" | tr -d ' ')
ORIGINAL_MIN_WAL=$(docker exec catchit-db psql -U postgres -d catchitdb -t -c "SHOW min_wal_size;" | tr -d ' ')

echo -e "\n${GREEN}📌 Configuração original:${NC}"
echo "   max_wal_size: $ORIGINAL_MAX_WAL"
echo "   checkpoint_completion_target: $ORIGINAL_CHECKPOINT"
echo "   min_wal_size: $ORIGINAL_MIN_WAL"
echo ""

for config in "${CONFIGS[@]}"; do
    IFS=':' read -r max_wal checkpoint min_wal name <<< "$config"
    
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE} TESTANDO: $name${NC}"
    echo -e "${BLUE} max_wal_size=$max_wal | checkpoint_target=$checkpoint | min_wal_size=$min_wal${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    
    # Aplicar configuração
    set_wal_config "$max_wal" "$checkpoint" "$min_wal"
    
    # Forçar checkpoint para começar com estado limpo
    force_checkpoint
    
    # Executar benchmark
    LOG_FILE="$RESULTS_DIR/benchmark_${name}.log"
    echo -e "${YELLOW}🚀 A executar benchmark...${NC}"
    
    cd $BASE_DIR
    mvn spring-boot:run -pl backend \
        -Dspring-boot.run.profiles=db-only \
        "-Dspring-boot.run.arguments=--runTicketingBenchmark" \
        -Dbenchmark.iterations=200 \
        > "$LOG_FILE" 2>&1
    
    # Extrair métricas
    throughput=$(grep "Throughput médio" "$LOG_FILE" | grep -oP '[\d.]+' | head -1)
    latency=$(grep "Latência média" "$LOG_FILE" | grep -oP '[\d.]+' | head -1)
    exec_time=$(grep "Total time:" "$LOG_FILE" | tail -1 | grep -oP '[\d.]+' | head -1)
    
    echo "$name,$max_wal,$checkpoint,$min_wal,$throughput,$latency,$exec_time" >> $RESULT_CSV
    
    echo -e "${GREEN}   ✅ Throughput: ${throughput:-N/A} ops/s, Latência: ${latency:-N/A} ms${NC}"
    
    # Aguardar estabilização entre testes
    echo -e "${YELLOW}⏳ Aguardando 10 segundos para estabilizar...${NC}"
    sleep 10
done

# Restaurar configuração original
echo -e "\n${YELLOW}🔄 A restaurar configuração original...${NC}"
set_wal_config "$ORIGINAL_MAX_WAL" "$ORIGINAL_CHECKPOINT" "$ORIGINAL_MIN_WAL"

echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         TESTES CONCLUÍDOS!                                    ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}📁 Resultados: $RESULT_CSV${NC}"
echo ""
column -t -s',' $RESULT_CSV