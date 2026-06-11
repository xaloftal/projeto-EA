-- ============================================
-- REMOVER TODOS OS ÍNDICES (exceto PRIMARY KEYS)
-- ============================================

DO $$
DECLARE
    r RECORD;
    cnt INT := 0;
BEGIN
    FOR r IN (SELECT indexname FROM pg_indexes WHERE schemaname = 'catchit' AND indexname NOT LIKE '%pkey' AND indexname NOT LIKE 'uk%') LOOP
        EXECUTE format('DROP INDEX IF EXISTS catchit.%I', r.indexname);
        cnt := cnt + 1;
        RAISE NOTICE 'Removido: %', r.indexname;
    END LOOP;
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Total removido: % índices', cnt;
    RAISE NOTICE '========================================';
END $$;

SELECT '========================================' as "";
SELECT '✅ TODOS OS ÍNDICES FORAM REMOVIDOS!' as status;
SELECT COUNT(*) as indices_restantes FROM pg_indexes 
WHERE schemaname = 'catchit' 
  AND indexname NOT LIKE '%pkey'
  AND indexname NOT LIKE 'uk%';
