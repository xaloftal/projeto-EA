#!/bin/bash

echo "=========================================="
echo "REMOVER ÍNDICES"
echo "=========================================="

docker exec -i catchit-db psql -U postgres -d catchitdb < drop_indexes.sql

echo ""
echo "✅ Índices removidos com sucesso!"
