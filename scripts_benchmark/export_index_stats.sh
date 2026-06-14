#!/bin/bash

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Timestamp para o ficheiro
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="index_stats"
mkdir -p $OUTPUT_DIR

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║              EXPORTAR ESTATÍSTICAS DE ÍNDICES                 ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Verificar se o container está a correr
if ! docker ps | grep -q catchit-db; then
    echo -e "${RED}❌ Container não está a correr!${NC}"
    exit 1
fi

echo -e "\n${YELLOW}📊 A exportar estatísticas...${NC}"

# 1. Exportar estatísticas de índices
CSV_FILE="${OUTPUT_DIR}/index_stats_${TIMESTAMP}.csv"
echo -e "${GREEN}📁 Ficheiro: ${CSV_FILE}${NC}"

docker exec catchit-db psql -U postgres -d catchitdb -c "
SELECT 
    schemaname,
    relname as tabela,
    indexrelname as indice,
    idx_scan as usos,
    idx_tup_read as tuplas_lidas,
    idx_tup_fetch as tuplas_buscadas,
    pg_size_pretty(pg_relation_size(indexrelid)) as tamanho
FROM pg_stat_user_indexes 
WHERE schemaname = 'catchit'
ORDER BY idx_scan DESC;
" -A -F "," -o "$CSV_FILE"

echo -e "${GREEN}   ✅ Índices exportados${NC}"

# 2. Exportar estatísticas de tabelas
TABLE_FILE="${OUTPUT_DIR}/table_stats_${TIMESTAMP}.csv"
echo -e "${GREEN}📁 Ficheiro: ${TABLE_FILE}${NC}"

docker exec catchit-db psql -U postgres -d catchitdb -c "
SELECT 
    schemaname,
    relname as tabela,
    seq_scan as varreduras_sequenciais,
    seq_tup_read as tuplas_sequenciais,
    idx_scan as varreduras_indice,
    idx_tup_fetch as tuplas_indice,
    n_live_tup as registos_vivos,
    n_dead_tup as registos_mortos
FROM pg_stat_user_tables 
WHERE schemaname = 'catchit'
ORDER BY idx_scan DESC;
" -A -F "," -o "$TABLE_FILE"

echo -e "${GREEN}   ✅ Tabelas exportadas${NC}"

# 3. Exportar resumo de uso de índices
SUMMARY_FILE="${OUTPUT_DIR}/index_summary_${TIMESTAMP}.csv"
echo -e "${GREEN}📁 Ficheiro: ${SUMMARY_FILE}${NC}"

docker exec catchit-db psql -U postgres -d catchitdb -c "
SELECT 
    CASE 
        WHEN idx_scan > 100000 THEN 'CRITICO'
        WHEN idx_scan > 10000 THEN 'ALTO'
        WHEN idx_scan > 1000 THEN 'MEDIO'
        WHEN idx_scan > 0 THEN 'BAIXO'
        ELSE 'NAO_USADO'
    END as prioridade,
    COUNT(*) as quantidade_indices,
    SUM(idx_scan) as total_usos,
    pg_size_pretty(SUM(pg_relation_size(indexrelid))) as tamanho_total
FROM pg_stat_user_indexes 
WHERE schemaname = 'catchit'
GROUP BY prioridade
ORDER BY 
    CASE prioridade
        WHEN 'CRITICO' THEN 1
        WHEN 'ALTO' THEN 2
        WHEN 'MEDIO' THEN 3
        WHEN 'BAIXO' THEN 4
        ELSE 5
    END;
" -A -F "," -o "$SUMMARY_FILE"

echo -e "${GREEN}   ✅ Resumo exportado${NC}"

# 4. Exportar índices nunca usados (candidatos a remoção)
UNUSED_FILE="${OUTPUT_DIR}/unused_indexes_${TIMESTAMP}.csv"
echo -e "${GREEN}📁 Ficheiro: ${UNUSED_FILE}${NC}"

docker exec catchit-db psql -U postgres -d catchitdb -c "
SELECT 
    schemaname,
    relname as tabela,
    indexrelname as indice,
    idx_scan as usos,
    pg_size_pretty(pg_relation_size(indexrelid)) as tamanho,
    pg_size_relation(indexrelid) as tamanho_bytes
FROM pg_stat_user_indexes 
WHERE schemaname = 'catchit'
  AND idx_scan = 0
  AND indexrelname NOT LIKE '%pkey'
ORDER BY pg_relation_size(indexrelid) DESC;
" -A -F "," -o "$UNUSED_FILE"

echo -e "${GREEN}   ✅ Índices não usados exportados${NC}"

# 5. Exportar métricas de cache
CACHE_FILE="${OUTPUT_DIR}/cache_stats_${TIMESTAMP}.csv"
echo -e "${GREEN}📁 Ficheiro: ${CACHE_FILE}${NC}"

docker exec catchit-db psql -U postgres -d catchitdb -c "
SELECT 
    relname as tabela,
    heap_blks_hit as cache_hit,
    heap_blks_read as cache_read,
    CASE 
        WHEN heap_blks_hit + heap_blks_read > 0 
        THEN ROUND(100.0 * heap_blks_hit / (heap_blks_hit + heap_blks_read), 2)
        ELSE 0 
    END as hit_ratio_percent
FROM pg_statio_user_tables 
WHERE schemaname = 'catchit'
ORDER BY hit_ratio_percent ASC;
" -A -F "," -o "$CACHE_FILE"

echo -e "${GREEN}   ✅ Cache stats exportados${NC}"

echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}         EXPORTAÇÃO CONCLUÍDA!                                 ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}📁 Ficheiros gerados em: ${OUTPUT_DIR}/${NC}"
echo -e "   - index_stats_${TIMESTAMP}.csv      (estatísticas de índices)"
echo -e "   - table_stats_${TIMESTAMP}.csv      (estatísticas de tabelas)"
echo -e "   - index_summary_${TIMESTAMP}.csv    (resumo por prioridade)"
echo -e "   - unused_indexes_${TIMESTAMP}.csv   (índices nunca usados)"
echo -e "   - cache_stats_${TIMESTAMP}.csv      (métricas de cache)"
echo ""
echo -e "${YELLOW}💡 Para visualizar os CSVs:${NC}"
echo -e "   cat ${OUTPUT_DIR}/index_stats_${TIMESTAMP}.csv"
echo -e "   ou abrir no Excel/LibreOffice"