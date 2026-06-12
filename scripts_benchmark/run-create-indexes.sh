#!/bin/bash

echo "=========================================="
echo "CRIAR ÍNDICES"
echo "=========================================="

docker exec -i catchit-db psql -U postgres -d catchitdb < create_indexes.sql

echo ""
echo "✅ Índices criados com sucesso!"
