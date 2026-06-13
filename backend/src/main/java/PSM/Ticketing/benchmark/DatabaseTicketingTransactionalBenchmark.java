package PSM.Ticketing.benchmark;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Stop;
import PSM.Location.Zone;
import PSM.Location.api.stop.StopRepository;
import PSM.Location.api.zone.ZoneRepository;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.TicketPack;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.card.CardRepository;
import PSM.Ticketing.api.ticket.TicketRepository;
import PSM.Ticketing.api.ticketpack.TicketPackRepository;
import PSM.Ticketing.api.title.TitleRepository;
import PSM.UserManagement.User;
import PSM.UserManagement.api.user.UserRepository;

@Component
public class DatabaseTicketingTransactionalBenchmark {

    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private CardRepository cardRepository;
    
    @Autowired
    private TitleRepository titleRepository;
    
    @Autowired
    private TicketPackRepository ticketPackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StopRepository stopRepository;
    
    @Autowired
    private ZoneRepository zoneRepository;

    // ========== CONFIGURAÇÕES DE CARGA ==========
private int iterations = 10000;      // Stress
private int batchSize = 2000;        // Stress  
private int threads = 100;           // Stress
private int concurrentOps = 10000;   // Stress

    private static class TestMetrics {
        String testName;
        String operation;
        double throughput;
        double avgLatencyMs;
        double minLatencyMs;
        double maxLatencyMs;
        double p95LatencyMs;
        double p99LatencyMs;
        int successCount;
        int failureCount;
        long totalTimeMs;
        List<Long> latencies;

        TestMetrics(String testName, String operation) {
            this.testName = testName;
            this.operation = operation;
            this.successCount = 0;
            this.failureCount = 0;
            this.latencies = new ArrayList<>();
            this.minLatencyMs = Double.MAX_VALUE;
            this.maxLatencyMs = 0;
        }

        void addLatency(long latencyMs) {
            latencies.add(latencyMs);
            minLatencyMs = Math.min(minLatencyMs, latencyMs);
            maxLatencyMs = Math.max(maxLatencyMs, latencyMs);
        }

        void calculateStats() {
            if (latencies.isEmpty()) return;
            Collections.sort(latencies);
            avgLatencyMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
            int p99Index = (int) Math.ceil(latencies.size() * 0.99) - 1;
            p95LatencyMs = p95Index >= 0 ? latencies.get(p95Index) : avgLatencyMs;
            p99LatencyMs = p99Index >= 0 ? latencies.get(p99Index) : avgLatencyMs;
        }
    }

    private List<TestMetrics> allMetrics = new ArrayList<>();
    private List<UUID> userIds = new ArrayList<>();
    private List<UUID> stopIds = new ArrayList<>();
    private List<UUID> zoneIds = new ArrayList<>();
    private String csvFileName;

    // ========== CARREGAR CONFIGURAÇÃO ==========
    private void loadBenchmarkConfig() {
        String iterationsEnv = System.getProperty("benchmark.iterations");
        String batchSizeEnv = System.getProperty("benchmark.batch.size");
        String threadsEnv = System.getProperty("benchmark.threads");
        String concurrentOpsEnv = System.getProperty("benchmark.concurrent.ops");
        
        if (iterationsEnv != null) {
            iterations = Integer.parseInt(iterationsEnv);
        }
        if (batchSizeEnv != null) {
            batchSize = Integer.parseInt(batchSizeEnv);
        }
        if (threadsEnv != null) {
            threads = Integer.parseInt(threadsEnv);
        }
        if (concurrentOpsEnv != null) {
            concurrentOps = Integer.parseInt(concurrentOpsEnv);
        }
        
        System.out.println("🔧 CONFIGURAÇÃO DO BENCHMARK:");
        System.out.println("   Iterations: " + iterations);
        System.out.println("   Batch Size: " + batchSize);
        System.out.println("   Threads: " + threads);
        System.out.println("   Concurrent Ops: " + concurrentOps);
        System.out.println();
    }

    public void runFullBenchmark() {
        loadBenchmarkConfig();
        allMetrics.clear();
        
        // Gerar nome do ficheiro CSV
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        csvFileName = "benchmark_ticketing_" + timestamp + ".csv";
        
        prepareTestData();

        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              TICKETING TRANSACTIONAL BENCHMARK                                ║");
        System.out.println("║              Testes de Tickets, Cards, Packs e Estados                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println("\n");
        System.out.printf("📁 Resultados serão salvos em: %s%n\n", csvFileName);

        long benchmarkStartTime = System.currentTimeMillis();

        try {
            // ========== TESTES DE TICKET ==========
            testInsertTicket();
            testInsertTicketWithStops();
            testBatchInsertTickets();
            testUpdateTicketState();
            testFindTicketById();
            testFindTicketsByUser();
            
            // ========== TESTES DE CARD ==========
            testInsertCard();
            testInsertCardWithZone();
            testUpdateCard();
            
            // ========== TESTES DE TICKET PACK ==========
            testInsertTicketPack();
            testCreateAndBindPack();
            testFindTicketPackById();
            
            // ========== TESTES DE QR CODE ==========
            testGenerateQRCode();
            
            // ========== TESTES DE TRANSIÇÃO DE ESTADOS ==========
            testTicketStateTransitions();
            testCardStateTransitions();
            
            // ========== TESTES DE CONCORRÊNCIA ==========
            testConcurrentTicketInserts();
            testConcurrentStateTransitions();

            long benchmarkEndTime = System.currentTimeMillis();
            printTotalStatistics(benchmarkEndTime - benchmarkStartTime);
            printRecommendations();
            
            // Exportar CSV
            exportToCSV();

        } catch (Exception e) {
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
            writer.println("Teste,Operação,Throughput(ops/s),Latência_Média(ms),Latência_Mín(ms),Latência_Máx(ms),P95(ms),P99(ms),Sucesso,Falhas,Total_Operações,Tempo_Total(ms)");
            
            for (TestMetrics m : allMetrics) {
                int totalOps = m.successCount + m.failureCount;
                writer.printf("%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d%n",
                    escapeCsv(m.testName),
                    escapeCsv(m.operation),
                    m.throughput,
                    m.avgLatencyMs,
                    m.minLatencyMs == Double.MAX_VALUE ? 0 : m.minLatencyMs,
                    m.maxLatencyMs,
                    m.p95LatencyMs,
                    m.p99LatencyMs,
                    m.successCount,
                    m.failureCount,
                    totalOps,
                    m.totalTimeMs
                );
            }
            
            System.out.printf("\n📊 CSV exportado com sucesso: %s%n", csvFileName);
            
        } catch (Exception e) {
            System.err.println("Erro ao exportar CSV: " + e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ==================== PREPARAÇÃO DOS DADOS ====================
    
    private void prepareTestData() {
        System.out.println("📊 PREPARANDO DADOS DE TESTE...\n");
        
        try {
            // Criar utilizadores de teste
            for (int i = 0; i < 50; i++) {
                User user = new User();
                user.setEmail("test" + i + "@test.com");
                user.setName("Test User " + i);
                user.setPasswordHash("password");
                user = userRepository.save(user);
                userIds.add(user.getId());
            }
            
            // Buscar stops existentes
            List<Stop> stops = stopRepository.findAll();
            for (Stop stop : stops) {
                stopIds.add(stop.getId());
            }
            
            // Buscar zonas existentes
            List<Zone> zones = zoneRepository.findAll();
            for (Zone zone : zones) {
                zoneIds.add(zone.getId());
            }
            
            System.out.printf("  ✅ %d utilizadores, %d stops, %d zonas%n", 
                userIds.size(), stopIds.size(), zoneIds.size());
            
        } catch (Exception e) {
            System.err.println("  ❌ Erro: " + e.getMessage());
        }
        System.out.println();
    }

    // ==================== TESTES DE TICKET ====================

    @Transactional
    private void testInsertTicket() {
        TestMetrics metrics = new TestMetrics("INSERT Ticket", "INSERT INTO title (title_type, created_at, valid_from, valid_until, price, state_name, user_id) VALUES ('ticket', ?, ?, ?, ?, ?, ?)");
        System.out.println("📝 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Criação de ticket básico (sem stops)\n");

        if (userIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores disponíveis\n");
            return;
        }

        int iterationsCount = this.iterations;
        List<UUID> createdIds = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID userId = userIds.get(i % userIds.size());
            long startNanos = System.nanoTime();
            try {
                Ticket ticket = new Ticket();
                ticket.setCreatedAt(LocalDateTime.now());
                ticket.setValidFrom(LocalDateTime.now());
                ticket.setValidUntil(LocalDateTime.now().plusDays(30));
                ticket.setPrice(BigDecimal.valueOf(15.99));
                ticket.setStateName("UNUSED");
                
                User user = userRepository.findById(userId).orElse(null);
                ticket.setUser(user);
                
                Ticket saved = ticketRepository.save(ticket);
                createdIds.add(saved.getId());
                
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        
        for (UUID id : createdIds) {
            ticketRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    @Transactional
    private void testInsertTicketWithStops() {
        TestMetrics metrics = new TestMetrics("INSERT Ticket with Stops", "INSERT INTO ticket (from_id, to_id, ...) VALUES (?, ?, ...)");
        System.out.println("📝 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Criação de ticket com origem e destino\n");

        if (userIds.isEmpty() || stopIds.size() < 2) {
            System.out.println("  ⚠️ Necessário pelo menos 2 stops e 1 utilizador\n");
            return;
        }

        int iterationsCount = this.iterations;
        List<UUID> createdIds = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID userId = userIds.get(i % userIds.size());
            UUID fromStopId = stopIds.get(i % stopIds.size());
            UUID toStopId = stopIds.get((i + 1) % stopIds.size());
            
            long startNanos = System.nanoTime();
            try {
                Stop fromStop = stopRepository.findById(fromStopId).orElse(null);
                Stop toStop = stopRepository.findById(toStopId).orElse(null);
                User user = userRepository.findById(userId).orElse(null);
                
                Ticket ticket = new Ticket();
                ticket.setCreatedAt(LocalDateTime.now());
                ticket.setValidFrom(LocalDateTime.now());
                ticket.setValidUntil(LocalDateTime.now().plusDays(30));
                ticket.setPrice(BigDecimal.valueOf(15.99));
                ticket.setStateName("UNUSED");
                ticket.setUser(user);
                ticket.setFrom(fromStop);
                ticket.setTo(toStop);
                
                Ticket saved = ticketRepository.save(ticket);
                createdIds.add(saved.getId());
                
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        long testEndTime = System.currentTimeMillis();
        
        metrics.calculateStats();
        metrics.totalTimeMs = testEndTime - testStartTime;
        
        if (metrics.totalTimeMs > 0) {
            metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;
        } else {
            metrics.throughput = 0;
        }

        printMetrics(metrics, iterationsCount);
        
        for (UUID id : createdIds) {
            ticketRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    @Transactional
    private void testBatchInsertTickets() {
        TestMetrics metrics = new TestMetrics("BATCH INSERT Tickets", "INSERT INTO title (...) VALUES (...), (...), ...");
        System.out.println("📦 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Inserção em lote de tickets\n");

        if (userIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores disponíveis\n");
            return;
        }

        int currentBatchSize = this.batchSize;
        int totalBatches = Math.max(1, this.iterations / currentBatchSize);
        int totalOps = currentBatchSize * totalBatches;
        
        long testStartTime = System.currentTimeMillis();

        for (int batch = 0; batch < totalBatches; batch++) {
            List<Ticket> batchList = new ArrayList<>();
            for (int i = 0; i < currentBatchSize; i++) {
                UUID userId = userIds.get((batch * currentBatchSize + i) % userIds.size());
                
                Ticket ticket = new Ticket();
                ticket.setCreatedAt(LocalDateTime.now());
                ticket.setValidFrom(LocalDateTime.now());
                ticket.setValidUntil(LocalDateTime.now().plusDays(30));
                ticket.setPrice(BigDecimal.valueOf(15.99));
                ticket.setStateName("UNUSED");
                
                User user = userRepository.findById(userId).orElse(null);
                ticket.setUser(user);
                
                batchList.add(ticket);
            }
            
            long startNanos = System.nanoTime();
            try {
                ticketRepository.saveAll(batchList);
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs / batchList.size());
                metrics.successCount += batchList.size();
            } catch (Exception e) {
                metrics.failureCount += batchList.size();
            }
        }
        
        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (totalOps * 1000.0) / metrics.totalTimeMs;
        
        System.out.printf("  ✅ Total time: %d ms%n", metrics.totalTimeMs);
        System.out.printf("  ✅ Total operations: %d%n", totalOps);
        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success rate: %.1f%%%n", (metrics.successCount * 100.0 / totalOps));
        System.out.println();
        
        allMetrics.add(metrics);
    }

    @Transactional
    private void testUpdateTicketState() {
        TestMetrics metrics = new TestMetrics("UPDATE Ticket State", "UPDATE title SET state_name = ? WHERE id = ?");
        System.out.println("✏️ " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Atualização do estado do ticket (UNUSED → ACTIVE)\n");

        // Criar tickets para atualizar
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < this.iterations; i++) {
            Ticket ticket = new Ticket();
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setValidFrom(LocalDateTime.now());
            ticket.setValidUntil(LocalDateTime.now().plusDays(30));
            ticket.setPrice(BigDecimal.valueOf(15.99));
            ticket.setStateName("UNUSED");
            tickets.add(ticketRepository.save(ticket));
        }

        int iterationsCount = this.iterations;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            Ticket ticket = tickets.get(i);
            long startNanos = System.nanoTime();
            try {
                ticket.setStateName("ACTIVE");
                ticketRepository.save(ticket);
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        
        for (Ticket t : tickets) {
            ticketRepository.delete(t);
        }
        allMetrics.add(metrics);
    }

    @Transactional(readOnly = true)
    private void testFindTicketById() {
        TestMetrics metrics = new TestMetrics("SELECT Ticket by ID", "SELECT * FROM title WHERE id = ?");
        System.out.println("🔍 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Busca de ticket por ID (com EntityGraph de stops)\n");

        // Criar tickets para buscar
        List<UUID> ticketIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Ticket ticket = new Ticket();
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setValidFrom(LocalDateTime.now());
            ticket.setValidUntil(LocalDateTime.now().plusDays(30));
            ticket.setPrice(BigDecimal.valueOf(15.99));
            ticket.setStateName("UNUSED");
            ticket = ticketRepository.save(ticket);
            ticketIds.add(ticket.getId());
        }

        int iterationsCount = this.iterations * 2;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID id = ticketIds.get(i % ticketIds.size());
            long startNanos = System.nanoTime();
            try {
                Optional<Ticket> found = ticketRepository.findById(id);
                if (found.isPresent()) {
                    metrics.successCount++;
                } else {
                    metrics.failureCount++;
                }
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        long testEndTime = System.currentTimeMillis();
        
        metrics.calculateStats();
        metrics.totalTimeMs = testEndTime - testStartTime;
        
        if (metrics.totalTimeMs > 0) {
            metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;
        } else {
            metrics.throughput = 0;
        }

        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success rate: %.1f%%%n", (metrics.successCount * 100.0 / iterationsCount));
        System.out.println();
        
        for (UUID id : ticketIds) {
            ticketRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    @Transactional(readOnly = true)
    private void testFindTicketsByUser() {
        TestMetrics metrics = new TestMetrics("SELECT Tickets by User", "Query com JOINs e ORDER BY CASE");
        System.out.println("👥 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Query complexa: tickets do utilizador com JOINs\n");

        if (userIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores disponíveis\n");
            return;
        }

        // Criar tickets para os utilizadores
        for (int i = 0; i < 50; i++) {
            UUID userId = userIds.get(i % userIds.size());
            Ticket ticket = new Ticket();
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setValidFrom(LocalDateTime.now());
            ticket.setValidUntil(LocalDateTime.now().plusDays(30));
            ticket.setPrice(BigDecimal.valueOf(15.99));
            ticket.setStateName(i % 2 == 0 ? "UNUSED" : "USED");
            
            User user = userRepository.findById(userId).orElse(null);
            ticket.setUser(user);
            ticketRepository.save(ticket);
        }

        int iterationsCount = Math.max(50, this.iterations / 2);
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID userId = userIds.get(i % userIds.size());
            long startNanos = System.nanoTime();
            try {
                var tickets = ticketRepository.findTicketsSummaryByUserId(userId);
                metrics.successCount++;
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success rate: %.1f%%%n", (metrics.successCount * 100.0 / iterationsCount));
        System.out.println();
        
        allMetrics.add(metrics);
    }

    // ==================== TESTES DE CARD ====================

    @Transactional
    private void testInsertCard() {
        TestMetrics metrics = new TestMetrics("INSERT Card", "INSERT INTO title (title_type, created_at, valid_from, valid_until, price, state_name, user_id) VALUES ('card', ?, ?, ?, ?, ?, ?)");
        System.out.println("💳 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Criação de cartão básico\n");

        if (userIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores disponíveis\n");
            return;
        }

        int iterationsCount = this.iterations;
        List<UUID> createdIds = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID userId = userIds.get(i % userIds.size());
            long startNanos = System.nanoTime();
            try {
                Card card = new Card();
                card.setCreatedAt(LocalDateTime.now());
                card.setValidFrom(LocalDateTime.now());
                card.setValidUntil(LocalDateTime.now().plusDays(90));
                card.setPrice(BigDecimal.valueOf(25.00));
                card.setStateName("UNUSED");
                
                User user = userRepository.findById(userId).orElse(null);
                card.setUser(user);
                
                Card saved = cardRepository.save(card);
                createdIds.add(saved.getId());
                
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        
        for (UUID id : createdIds) {
            cardRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    @Transactional
    private void testInsertCardWithZone() {
        TestMetrics metrics = new TestMetrics("INSERT Card with Zone", "INSERT INTO card (zone_id, ...) VALUES (?, ...)");
        System.out.println("💳 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Criação de cartão com zona associada\n");

        if (userIds.isEmpty() || zoneIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores ou zonas disponíveis\n");
            return;
        }

        int iterationsCount = this.iterations;
        List<UUID> createdIds = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID userId = userIds.get(i % userIds.size());
            UUID zoneId = zoneIds.get(i % zoneIds.size());
            
            long startNanos = System.nanoTime();
            try {
                Zone zone = zoneRepository.findById(zoneId).orElse(null);
                User user = userRepository.findById(userId).orElse(null);
                
                Card card = new Card();
                card.setCreatedAt(LocalDateTime.now());
                card.setValidFrom(LocalDateTime.now());
                card.setValidUntil(LocalDateTime.now().plusDays(90));
                card.setPrice(BigDecimal.valueOf(25.00));
                card.setStateName("UNUSED");
                card.setUser(user);
                card.setZone(zone);
                
                Card saved = cardRepository.save(card);
                createdIds.add(saved.getId());
                
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        
        for (UUID id : createdIds) {
            cardRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    @Transactional
    private void testUpdateCard() {
        TestMetrics metrics = new TestMetrics("UPDATE Card", "UPDATE title SET state_name = ? WHERE id = ?");
        System.out.println("✏️ " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Atualização do cartão (renovação)\n");

        // Criar cards para atualizar
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < this.iterations; i++) {
            Card card = new Card();
            card.setCreatedAt(LocalDateTime.now());
            card.setValidFrom(LocalDateTime.now());
            card.setValidUntil(LocalDateTime.now().plusDays(90));
            card.setPrice(BigDecimal.valueOf(25.00));
            card.setStateName("UNUSED");
            cards.add(cardRepository.save(card));
        }

        int iterationsCount = this.iterations;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            Card card = cards.get(i);
            long startNanos = System.nanoTime();
            try {
                card.setStateName("ACTIVE");
                cardRepository.save(card);
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        
        for (Card c : cards) {
            cardRepository.delete(c);
        }
        allMetrics.add(metrics);
    }

    // ==================== TESTES DE TICKET PACK ====================

    @Transactional
    private void testInsertTicketPack() {
        TestMetrics metrics = new TestMetrics("INSERT TicketPack", "INSERT INTO ticketpack (id, discount) VALUES (?, ?)");
        System.out.println("📦 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Criação de pack de tickets\n");

        int iterationsCount = this.iterations;
        List<UUID> createdIds = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            long startNanos = System.nanoTime();
            try {
                TicketPack pack = new TicketPack();
                pack.setId(UUID.randomUUID());
                pack.setDiscount(BigDecimal.valueOf(0.1 + (i % 90) / 100.0));
                TicketPack saved = ticketPackRepository.save(pack);
                createdIds.add(saved.getId());
                
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        
        for (UUID id : createdIds) {
            ticketPackRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    @Transactional
    private void testCreateAndBindPack() {
        TestMetrics metrics = new TestMetrics("CREATE AND BIND PACK", "Pack + N tickets associados");
        System.out.println("🔗 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Criação de pack com N tickets associados\n");

        if (userIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores disponíveis\n");
            return;
        }

        int iterationsCount = Math.max(20, this.iterations / 4);
        int ticketsPerPack = 10;

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            long startNanos = System.nanoTime();
            try {
                UUID userId = userIds.get(i % userIds.size());
                User user = userRepository.findById(userId).orElse(null);
                
                // Criar títulos para o pack
                List<Title> titles = new ArrayList<>();
                for (int j = 0; j < ticketsPerPack; j++) {
                    Ticket ticket = new Ticket();
                    ticket.setCreatedAt(LocalDateTime.now());
                    ticket.setValidFrom(LocalDateTime.now());
                    ticket.setValidUntil(LocalDateTime.now().plusDays(30));
                    ticket.setPrice(BigDecimal.valueOf(15.99));
                    ticket.setStateName("UNUSED");
                    ticket.setUser(user);
                    titles.add(ticket);
                }
                
                // Criar pack e associar títulos
                TicketPack pack = new TicketPack();
                pack.setId(UUID.randomUUID());
                pack.setDiscount(BigDecimal.valueOf(0.2));
                TicketPack savedPack = ticketPackRepository.save(pack);
                
                for (Title title : titles) {
                    title.setTicketPack(savedPack);
                    titleRepository.save(title);
                }
                
                metrics.successCount += titles.size() + 1;
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs / (ticketsPerPack + 1));
            } catch (Exception e) {
                metrics.failureCount += ticketsPerPack + 1;
            }
        }

        int totalOps = iterationsCount * (ticketsPerPack + 1);
        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (totalOps * 1000.0) / metrics.totalTimeMs;

        System.out.printf("  ✅ Total time: %d ms%n", metrics.totalTimeMs);
        System.out.printf("  ✅ Total operations: %d%n", totalOps);
        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success rate: %.1f%%%n", (metrics.successCount * 100.0 / totalOps));
        System.out.println();
        
        allMetrics.add(metrics);
    }

    @Transactional(readOnly = true)
    private void testFindTicketPackById() {
        TestMetrics metrics = new TestMetrics("SELECT TicketPack by ID", "SELECT * FROM ticketpack WHERE id = ?");
        System.out.println("🔍 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Busca de pack por ID\n");

        // Criar packs
        List<UUID> packIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TicketPack pack = new TicketPack();
            pack.setId(UUID.randomUUID());
            pack.setDiscount(BigDecimal.valueOf(0.15));
            pack = ticketPackRepository.save(pack);
            packIds.add(pack.getId());
        }

        int iterationsCount = this.iterations * 2;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            UUID id = packIds.get(i % packIds.size());
            long startNanos = System.nanoTime();
            try {
                Optional<TicketPack> found = ticketPackRepository.findById(id);
                if (found.isPresent()) {
                    metrics.successCount++;
                } else {
                    metrics.failureCount++;
                }
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success rate: %.1f%%%n", (metrics.successCount * 100.0 / iterationsCount));
        System.out.println();
        
        for (UUID id : packIds) {
            ticketPackRepository.deleteById(id);
        }
        allMetrics.add(metrics);
    }

    // ==================== TESTES DE QR CODE ====================

    @Transactional
    private void testGenerateQRCode() {
        TestMetrics metrics = new TestMetrics("Generate QR Code", "ZXING QR Code generation");
        System.out.println("🔲 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Geração de QR code para ticket\n");

        int iterationsCount = this.iterations;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            Ticket ticket = new Ticket();
            long startNanos = System.nanoTime();
            try {
                ticket.generateQrCode("Ticket-" + UUID.randomUUID(), 200);
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs);
                metrics.successCount++;
            } catch (Exception e) {
                metrics.failureCount++;
            }
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (iterationsCount * 1000.0) / metrics.totalTimeMs;

        printMetrics(metrics, iterationsCount);
        allMetrics.add(metrics);
    }

    // ==================== TESTES DE TRANSIÇÃO DE ESTADOS ====================

    @Transactional
    private void testTicketStateTransitions() {
        TestMetrics metrics = new TestMetrics("Ticket State Transitions", "UNUSED → ACTIVE → VALIDATED → USED → EXPIRED");
        System.out.println("🔄 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Ciclo completo de estados do ticket\n");

        // Criar ticket
        Ticket ticket = new Ticket();
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setValidFrom(LocalDateTime.now());
        ticket.setValidUntil(LocalDateTime.now().plusDays(30));
        ticket.setPrice(BigDecimal.valueOf(15.99));
        ticket.setStateName("UNUSED");
        ticket = ticketRepository.save(ticket);

        String[][] transitions = {
            {"UNUSED", "ACTIVE"},
            {"ACTIVE", "VALIDATED"},
            {"VALIDATED", "USED"},
            {"USED", "EXPIRED"}
        };

        int iterationsCount = Math.max(50, this.iterations / 2);
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            Ticket t = ticketRepository.findById(ticket.getId()).orElse(null);
            if (t == null) continue;
            
            for (String[] transition : transitions) {
                long startNanos = System.nanoTime();
                try {
                    t.setStateName(transition[1]);
                    ticketRepository.save(t);
                    long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                    metrics.addLatency(latencyMs);
                    metrics.successCount++;
                } catch (Exception e) {
                    metrics.failureCount++;
                }
            }
            // Reset para UNUSED
            t.setStateName("UNUSED");
            ticketRepository.save(t);
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (metrics.successCount * 1000.0) / metrics.totalTimeMs;

        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency per transition: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Total transitions: %d%n", metrics.successCount);
        System.out.println();
        
        ticketRepository.delete(ticket);
        allMetrics.add(metrics);
    }

    @Transactional
    private void testCardStateTransitions() {
        TestMetrics metrics = new TestMetrics("Card State Transitions", "UNUSED → ACTIVE → EXPIRED");
        System.out.println("🔄 " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   Ciclo de estados do cartão (ativação e renovação)\n");

        // Criar card
        Card card = new Card();
        card.setCreatedAt(LocalDateTime.now());
        card.setValidFrom(LocalDateTime.now());
        card.setValidUntil(LocalDateTime.now().plusDays(90));
        card.setPrice(BigDecimal.valueOf(25.00));
        card.setStateName("UNUSED");
        card = cardRepository.save(card);

        int iterationsCount = Math.max(50, this.iterations / 2);
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < iterationsCount; i++) {
            Card c = cardRepository.findById(card.getId()).orElse(null);
            if (c == null) continue;
            
            long startNanos = System.nanoTime();
            try {
                c.setStateName("ACTIVE");
                cardRepository.save(c);
                
                c.setStateName("EXPIRED");
                cardRepository.save(c);
                
                long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                metrics.addLatency(latencyMs / 2);
                metrics.successCount += 2;
            } catch (Exception e) {
                metrics.failureCount += 2;
            }
            
            // Reset
            c.setStateName("UNUSED");
            cardRepository.save(c);
        }

        metrics.calculateStats();
        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (metrics.successCount * 1000.0) / metrics.totalTimeMs;

        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency per transition: %.2f ms%n", metrics.avgLatencyMs);
        System.out.println();
        
        cardRepository.delete(card);
        allMetrics.add(metrics);
    }

    // ==================== TESTES DE CONCORRÊNCIA ====================

    private void testConcurrentTicketInserts() throws InterruptedException {
        TestMetrics metrics = new TestMetrics("Concurrent Ticket Inserts", "10 threads a inserir tickets");
        System.out.println("⚡ " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   👥 Simula " + this.threads + " utilizadores a comprar tickets ao mesmo tempo\n");

        if (userIds.isEmpty()) {
            System.out.println("  ⚠️ Sem utilizadores disponíveis\n");
            return;
        }

        int threadCount = this.threads;
        int opsPerThread = this.concurrentOps / this.threads;
        int totalOps = threadCount * opsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> allLatencies = new ConcurrentLinkedQueue<>();

        long testStartTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    UUID userId = userIds.get((threadId * opsPerThread + i) % userIds.size());
                    long startNanos = System.nanoTime();
                    try {
                        Ticket ticket = new Ticket();
                        ticket.setCreatedAt(LocalDateTime.now());
                        ticket.setValidFrom(LocalDateTime.now());
                        ticket.setValidUntil(LocalDateTime.now().plusDays(30));
                        ticket.setPrice(BigDecimal.valueOf(15.99));
                        ticket.setStateName("UNUSED");
                        
                        User user = userRepository.findById(userId).orElse(null);
                        ticket.setUser(user);
                        
                        ticketRepository.save(ticket);
                        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                        allLatencies.add(latencyMs);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (totalOps * 1000.0) / metrics.totalTimeMs;
        metrics.avgLatencyMs = allLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        metrics.successCount = successCount.get();
        metrics.failureCount = failureCount.get();

        System.out.printf("  ✅ Total time: %d ms%n", metrics.totalTimeMs);
        System.out.printf("  ✅ Total operations: %d%n", totalOps);
        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success: %d, Failures: %d (%.1f%% sucesso)%n", 
            successCount.get(), failureCount.get(), (successCount.get() * 100.0 / totalOps));
        System.out.println();
        
        allMetrics.add(metrics);
    }

    private void testConcurrentStateTransitions() throws InterruptedException {
        TestMetrics metrics = new TestMetrics("Concurrent State Transitions", "10 threads a atualizar estados");
        System.out.println("⚡ " + metrics.testName);
        System.out.println("   " + metrics.operation);
        System.out.println("   👥 Simula " + this.threads + " validações simultâneas em catracas\n");

        // Criar tickets
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            Ticket ticket = new Ticket();
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setValidFrom(LocalDateTime.now());
            ticket.setValidUntil(LocalDateTime.now().plusDays(30));
            ticket.setPrice(BigDecimal.valueOf(15.99));
            ticket.setStateName("UNUSED");
            tickets.add(ticketRepository.save(ticket));
        }

        int threadCount = this.threads;
        int opsPerThread = Math.max(10, (this.concurrentOps / 2) / this.threads);
        int totalOps = threadCount * opsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> allLatencies = new ConcurrentLinkedQueue<>();

        long testStartTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    Ticket ticket = tickets.get((i * 7) % tickets.size());
                    long startNanos = System.nanoTime();
                    try {
                        ticket.setStateName("ACTIVE");
                        ticketRepository.save(ticket);
                        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
                        allLatencies.add(latencyMs);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        metrics.totalTimeMs = System.currentTimeMillis() - testStartTime;
        metrics.throughput = (totalOps * 1000.0) / metrics.totalTimeMs;
        metrics.avgLatencyMs = allLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        metrics.successCount = successCount.get();
        metrics.failureCount = failureCount.get();

        System.out.printf("  ✅ Total time: %d ms%n", metrics.totalTimeMs);
        System.out.printf("  ✅ Total operations: %d%n", totalOps);
        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Success: %d, Failures: %d (%.1f%% sucesso)%n", 
            successCount.get(), failureCount.get(), (successCount.get() * 100.0 / totalOps));
        System.out.println();
        
        for (Ticket t : tickets) {
            ticketRepository.delete(t);
        }
        allMetrics.add(metrics);
    }

    // ==================== UTILITÁRIOS ====================

    private void printMetrics(TestMetrics metrics, int iterationsCount) {
        System.out.printf("  ✅ Throughput: %.2f ops/segundo%n", metrics.throughput);
        System.out.printf("  ✅ Avg latency: %.2f ms%n", metrics.avgLatencyMs);
        System.out.printf("  ✅ Min latency: %.2f ms%n", metrics.minLatencyMs);
        System.out.printf("  ✅ Max latency: %.2f ms%n", metrics.maxLatencyMs);
        System.out.printf("  ✅ P95 latency: %.2f ms%n", metrics.p95LatencyMs);
        System.out.printf("  ✅ P99 latency: %.2f ms%n", metrics.p99LatencyMs);
        System.out.printf("  ✅ Success rate: %.1f%% (%d/%d)%n", 
            (metrics.successCount * 100.0 / iterationsCount), metrics.successCount, iterationsCount);
        System.out.println();
    }

    private void printTotalStatistics(long totalBenchmarkTime) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RESULTADOS DO BENCHMARK TICKETING                          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ TESTE                                      │ THROUGHPUT     │ LATÊNCIA MÉDIA │ P95      │ SUCESSO    │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────────────────────────────┤");

        for (TestMetrics m : allMetrics) {
            System.out.printf("│ %-40s │ %12.2f ops/s │ %12.2f ms │ %8.2f ms │ %6.1f%% │%n", 
                truncate(m.testName, 40),
                m.throughput,
                m.avgLatencyMs,
                m.p95LatencyMs,
                (m.successCount * 100.0 / (m.successCount + m.failureCount)));
        }

        System.out.println("└─────────────────────────────────────────────────────────────────────────────────────────────────────┘");
        System.out.println();

        double avgThroughput = allMetrics.stream().mapToDouble(m -> m.throughput).average().orElse(0);
        double avgLatency = allMetrics.stream().mapToDouble(m -> m.avgLatencyMs).average().orElse(0);
        int totalOps = allMetrics.stream().mapToInt(m -> m.successCount + m.failureCount).sum();

        System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                         ESTATÍSTICAS AGREGADAS                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────────────────────────────┤");
        System.out.printf("│ %-40s │ %-93s │%n", "Tempo total do benchmark", formatTime(totalBenchmarkTime));
        System.out.printf("│ %-40s │ %-93s │%n", "Total de operações", totalOps);
        System.out.printf("│ %-40s │ %-93s │%n", "Throughput médio", String.format("%.2f ops/segundo", avgThroughput));
        System.out.printf("│ %-40s │ %-93s │%n", "Latência média", String.format("%.2f ms", avgLatency));
        System.out.println("└─────────────────────────────────────────────────────────────────────────────────────────────────────┘");
    }

    private void printRecommendations() {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      RECOMENDAÇÕES PARA O DBA                                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        TestMetrics best = allMetrics.stream().max(Comparator.comparingDouble(m -> m.throughput)).orElse(null);
        TestMetrics worst = allMetrics.stream().min(Comparator.comparingDouble(m -> m.throughput)).orElse(null);

        System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ 1. MELHOR E PIOR PERFORMANCE                                                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────────────────────────────┤");
        if (best != null) {
            System.out.printf("│    ✅ Melhor throughput: %s - %.2f ops/segundo%n", best.testName, best.throughput);
        }
        if (worst != null) {
            System.out.printf("│    ❌ Pior throughput: %s - %.2f ops/segundo%n", worst.testName, worst.throughput);
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────────────────────────────────────┘");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ 2. ÍNDICES RECOMENDADOS PARA TICKETING                                                               │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_title_user_id ON catchit.title(user_id);                             │");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_title_state_name ON catchit.title(state_name);                       │");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_title_valid_until ON catchit.title(valid_until);                     │");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_title_created_at ON catchit.title(created_at DESC);                  │");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_ticket_from_stop ON catchit.title(from_id) WHERE title_type='ticket';│");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_ticket_to_stop ON catchit.title(to_id) WHERE title_type='ticket';    │");
        System.out.println("│    CREATE INDEX CONCURRENTLY idx_card_zone ON catchit.title(zone_id) WHERE title_type='card';         │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────────────────────────────┘");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ 3. PRÓXIMOS PASSOS                                                                                  │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│    1. O ficheiro CSV foi gerado com todos os resultados                                             │");
        System.out.println("│    2. Pode abrir o CSV no Excel ou outra ferramenta para análise                                     │");
        System.out.println("│    3. Compare os resultados com benchmarks anteriores                                               │");
        System.out.println("│    4. Implemente os índices recomendados para melhorar a performance                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) return String.format("%d min %d s", minutes, seconds);
        return String.format("%d s", seconds);
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}