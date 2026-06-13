#!/bin/bash

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         BENCHMARK VARIANDO WORK_MEM                           ║"
echo "║         Testa o impacto do work_mem na performance            ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Configurações
BASE_DIR="/home/gojalo/mei/pea/projeto-EA"
SCRIPTS_DIR="$BASE_DIR/scripts_benchmark"
RESULTS_DIR="$BASE_DIR/work_mem_results"

mkdir -p $RESULTS_DIR

# Valores de work_mem a testar (em MB)
WORK_MEM_VALUES=("4MB" "8MB" "16MB" "32MB" "64MB")

# Nível de carga do benchmark (pode ser ajustado)
ITERATIONS=200
BATCH_SIZE=100
THREADS=10
CONCURRENT_OPS=1000

echo -e "${YELLOW}📋 Configuração do Teste:${NC}"
echo "   Iterations: $ITERATIONS"
echo "   Batch Size: $BATCH_SIZE"
echo "   Threads: $THREADS"
echo "   Concurrent Ops: $CONCURRENT_OPS"
echo ""
echo -e "${YELLOW}Valores de work_mem a testar: ${WORK_MEM_VALUES[*]}${NC}"
echo ""

# Ficheiro de resultados CSV
RESULT_CSV="$RESULTS_DIR/work_mem_benchmark_$(date +%Y%m%d_%H%M%S).csv"
echo "work_mem,throughput_media,latencia_media,throughput_insert,throughput_select,execution_time" > $RESULT_CSV

# Função para extrair métricas do benchmark
extract_metrics() {
    local log_file=$1
    local work_mem=$2
    
    # Extrair throughput médio
    throughput_media=$(grep "Throughput médio" "$log_file" | grep -oP '[\d.]+' | head -1)
    
    # Extrair latência média
    latencia_media=$(grep "Latência média" "$log_file" | grep -oP '[\d.]+' | head -1)
    
    # Extrair throughput de INSERT Ticket
    throughput_insert=$(grep "INSERT Ticket" -A 10 "$log_file" | grep "Throughput:" | grep -oP '[\d.]+' | head -1)
    
    # Extrair throughput de SELECT Ticket by ID
    throughput_select=$(grep "SELECT Ticket by ID" -A 10 "$log_file" | grep "Throughput:" | grep -oP '[\d.]+' | head -1)
    
    # Extrair tempo total de execução
    execution_time=$(grep "Total time:" "$log_file" | tail -1 | grep -oP '[\d.]+' | head -1)
    
    echo "$work_mem,$throughput_media,$latencia_media,$throughput_insert,$throughput_select,$execution_time" >> $RESULT_CSV
}

# Função para aplicar work_mem no PostgreSQL
set_work_mem() {
    local value=$1
    echo -e "${YELLOW}🔧 A aplicar work_mem = $value${NC}"
    
    docker exec catchit-db psql -U postgres -d catchitdb << EOF
ALTER SYSTEM SET work_mem = '$value';
SELECT pg_reload_conf();
EOF
    
    # Aguardar estabilização
    sleep 2
}

# Função para verificar work_mem atual
get_work_mem() {
    docker exec catchit-db psql -U postgres -d catchitdb -t -c "SHOW work_mem;" | tr -d ' '
}

# Função para resetar estatísticas
reset_stats() {
    docker exec catchit-db psql -U postgres -d catchitdb -c "SELECT pg_stat_reset();" > /dev/null 2>&1
}

# Criar diretório temporário para logs
TEMP_DIR=$(mktemp -d)

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}         INICIANDO TESTES COM DIFERENTES WORK_MEM             ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

# Guardar valor original
ORIGINAL_WORK_MEM=$(get_work_mem)
echo -e "\n${GREEN}📌 Work_mem original: $ORIGINAL_WORK_MEM${NC}"

for work_mem in "${WORK_MEM_VALUES[@]}"; do
    echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN} TESTANDO: work_mem = $work_mem${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    
    # Aplicar work_mem
    set_work_mem $work_mem
    
    # Verificar se foi aplicado
    current=$(get_work_mem)
    echo -e "   work_mem atual: $current"
    
    # Resetar estatísticas
    reset_stats
    
    # Executar benchmark
    LOG_FILE="$TEMP_DIR/benchmark_${work_mem}.log"
    echo -e "   🚀 A executar benchmark..."
    
    cd $BASE_DIR
    mvn spring-boot:run -pl backend \
        -Dspring-boot.run.profiles=db-only \
        "-Dspring-boot.run.arguments=--runTicketingBenchmark" \
        -Dbenchmark.iterations=$ITERATIONS \
        -Dbenchmark.batch.size=$BATCH_SIZE \
        -Dbenchmark.threads=$THREADS \
        -Dbenchmark.concurrent.ops=$CONCURRENT_OPS \
        > "$LOG_FILE" 2>&1
    
    # Extrair métricas
    extract_metrics "$LOG_FILE" "$work_mem"
    
    # Mostrar resultado rápido
    throughput=$(grep "Throughput médio" "$LOG_FILE" | grep -oP '[\d.]+' | head -1)
    echo -e "   ✅ Throughput médio: ${throughput:-N/A} ops/s"
    
    # Aguardar entre testes para estabilizar
    echo -e "   ⏳ Aguardando 5 segundos antes do próximo teste..."
    sleep 5
done

# Restaurar valor original
echo -e "\n${YELLOW}🔄 A restaurar work_mem original: $ORIGINAL_WORK_MEM${NC}"
set_work_mem $ORIGINAL_WORK_MEM

# Gerar relatório comparativo
echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}         RESULTADOS DA COMPARAÇÃO                              ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

# Mostrar tabela de resultados
echo -e "\n${YELLOW}📊 Tabela de Resultados:${NC}"
column -t -s',' $RESULT_CSV

# Gerar gráfico com Python (se disponível)
if command -v python3 &> /dev/null; then
    echo -e "\n${YELLOW}📈 A gerar gráfico comparativo...${NC}"
    
    cat > $TEMP_DIR/plot.py << 'EOF'
import pandas as pd
import matplotlib.pyplot as plt
import sys
import os

# Ler o CSV
csv_file = sys.argv[1]
output_dir = sys.argv[2]

df = pd.read_csv(csv_file)

# Configurar gráfico
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
fig.suptitle('Impacto do work_mem na Performance', fontsize=16, fontweight='bold')

# Gráfico 1: Throughput médio
axes[0,0].plot(df['work_mem'], df['throughput_media'], 'o-', color='#2ecc71', linewidth=2, markersize=8)
axes[0,0].set_xlabel('work_mem')
axes[0,0].set_ylabel('Throughput Médio (ops/s)')
axes[0,0].set_title('Throughput vs work_mem')
axes[0,0].grid(True, alpha=0.3)

# Gráfico 2: Latência média
axes[0,1].plot(df['work_mem'], df['latencia_media'], 'o-', color='#e74c3c', linewidth=2, markersize=8)
axes[0,1].set_xlabel('work_mem')
axes[0,1].set_ylabel('Latência Média (ms)')
axes[0,1].set_title('Latência vs work_mem')
axes[0,1].grid(True, alpha=0.3)

# Gráfico 3: INSERT vs SELECT throughput
axes[1,0].plot(df['work_mem'], df['throughput_insert'], 'o-', label='INSERT Ticket', color='#3498db')
axes[1,0].plot(df['work_mem'], df['throughput_select'], 's-', label='SELECT Ticket', color='#f39c12')
axes[1,0].set_xlabel('work_mem')
axes[1,0].set_ylabel('Throughput (ops/s)')
axes[1,0].set_title('INSERT vs SELECT')
axes[1,0].legend()
axes[1,0].grid(True, alpha=0.3)

# Gráfico 4: Tempo de execução
axes[1,1].plot(df['work_mem'], df['execution_time'], 'o-', color='#9b59b6', linewidth=2, markersize=8)
axes[1,1].set_xlabel('work_mem')
axes[1,1].set_ylabel('Tempo de Execução (ms)')
axes[1,1].set_title('Tempo de Execução vs work_mem')
axes[1,1].grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig(f'{output_dir}/work_mem_comparison.png', dpi=150, bbox_inches='tight')
plt.close()

print(f"✅ Gráfico guardado em: {output_dir}/work_mem_comparison.png")
EOF

    python3 $TEMP_DIR/plot.py $RESULT_CSV $RESULTS_DIR
fi

# Limpar ficheiros temporários
rm -rf $TEMP_DIR

echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         TESTES CONCLUÍDOS!                                    ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}📁 Resultados guardados em:${NC}"
echo -e "   - CSV: $RESULT_CSV"
echo -e "   - Gráfico: $RESULTS_DIR/work_mem_comparison.png"
echo ""
echo -e "${YELLOW}💡 Para visualizar os resultados:${NC}"
echo -e "   cat $RESULT_CSV"
echo -e "   ou abrir o CSV no Excel/LibreOffice"

