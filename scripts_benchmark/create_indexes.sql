-- ============================================
-- CRIAR ÍNDICES PARA TICKETING
-- ============================================

-- Índices para tabela title
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_title_user_id ON catchit.title(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_title_state_name ON catchit.title(state_name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_title_valid_until ON catchit.title(valid_until);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_title_created_at ON catchit.title(created_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_title_user_state_date ON catchit.title(user_id, state_name, created_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_title_type ON catchit.title(title_type);

-- Índices para tickets (origem/destino)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_from_stop ON catchit.title(from_id) WHERE title_type = 'ticket';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_to_stop ON catchit.title(to_id) WHERE title_type = 'ticket';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_route ON catchit.title(from_id, to_id) WHERE title_type = 'ticket';

-- Índices para cards (zona)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_card_zone ON catchit.title(zone_id) WHERE title_type = 'card';

-- Índices auxiliares
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticketpack_discount ON catchit.ticketpack(discount);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stop_name ON catchit.stop(name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_zone_name ON catchit.zone(name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_email ON catchit.users(email);

-- Mensagem de conclusão
\echo '=========================================='
\echo '✅ TODOS OS ÍNDICES FORAM CRIADOS!'
\echo '=========================================='

-- Listar índices criados
SELECT indexname 
FROM pg_indexes 
WHERE schemaname = 'catchit' 
  AND indexname NOT LIKE '%pkey'
  AND indexname NOT LIKE 'uk%'
ORDER BY indexname;