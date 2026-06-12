-- ============================================
-- OTIMIZAÇÃO DE ÍNDICES PARA CATCHIT
-- ============================================

-- Ativar extensão para estatísticas (opcional)
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- ============================================
-- ÍNDICES PARA TABELA TITLE (mais crítica)
-- ============================================

-- 1. Para findByStateName (busca por estado)
DROP INDEX IF EXISTS catchit.idx_title_state_name;
CREATE INDEX idx_title_state_name ON catchit.title(state_name);

-- 2. Para findById com User (joins com user)
DROP INDEX IF EXISTS catchit.idx_title_user_id;
CREATE INDEX idx_title_user_id ON catchit.title(user_id);

-- 3. Para ordenação por data (dashboard, listagens)
DROP INDEX IF EXISTS catchit.idx_title_created_at;
CREATE INDEX idx_title_created_at ON catchit.title(created_at DESC);

-- 4. Para queries com range de datas (expiração automática)
DROP INDEX IF EXISTS catchit.idx_title_valid_until;
CREATE INDEX idx_title_valid_until ON catchit.title(valid_until);

-- 5. Índice composto para findTicketsSummaryByUserId (MAIS IMPORTANTE!)
-- Cobre: WHERE user_id = ? ORDER BY state_name priority, created_at DESC
DROP INDEX IF EXISTS catchit.idx_title_user_state_created;
CREATE INDEX idx_title_user_state_created ON catchit.title(user_id, state_name, created_at DESC);

-- 6. Índice para agregações por estado
DROP INDEX IF EXISTS catchit.idx_title_state_user;
CREATE INDEX idx_title_state_user ON catchit.title(state_name, user_id);

-- 7. Índice para filtrar por tipo (Ticket vs Card)
DROP INDEX IF EXISTS catchit.idx_title_type_state;
CREATE INDEX idx_title_type_state ON catchit.title(title_type, state_name);

-- 8. Índice parcial para tickets ativos (poupa espaço)
DROP INDEX IF EXISTS catchit.idx_title_active;
CREATE INDEX idx_title_active ON catchit.title(valid_until) 
WHERE state_name IN ('UNUSED', 'ACTIVE', 'VALIDATED');

-- ============================================
-- ÍNDICES PARA COLUNAS DE RELACIONAMENTO
-- ============================================

-- 9. Índices para stop (joins com Ticket)
DROP INDEX IF EXISTS catchit.idx_ticket_from_stop;
CREATE INDEX idx_ticket_from_stop ON catchit.title(from_id) WHERE title_type = 'ticket';

DROP INDEX IF EXISTS catchit.idx_ticket_to_stop;
CREATE INDEX idx_ticket_to_stop ON catchit.title(to_id) WHERE title_type = 'ticket';

-- 10. Índice para zona (Card)
DROP INDEX IF EXISTS catchit.idx_card_zone;
CREATE INDEX idx_card_zone ON catchit.title(zone_id) WHERE title_type = 'card';

-- ============================================
-- ÍNDICES PARA TABELAS AUXILIARES
-- ============================================

-- 11. TicketPack
DROP INDEX IF EXISTS catchit.idx_ticketpack_discount;
CREATE INDEX idx_ticketpack_discount ON catchit.ticketpack(discount);

-- 12. Stop (para joins e buscas por nome)
DROP INDEX IF EXISTS catchit.idx_stop_name;
CREATE INDEX idx_stop_name ON catchit.stop(name);

-- 13. Zone
DROP INDEX IF EXISTS catchit.idx_zone_name;
CREATE INDEX idx_zone_name ON catchit.zone(name);

-- 14. User
DROP INDEX IF EXISTS catchit.idx_user_email;
CREATE INDEX idx_user_email ON catchit.users(email);

-- ============================================
-- VERIFICAR ÍNDICES CRIADOS
-- ============================================

SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE schemaname = 'catchit' 
ORDER BY tablename, indexname;