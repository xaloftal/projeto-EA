package PSM.Ticketing.benchmark;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class TicketingTransactionalBenchmarkRunner implements ApplicationRunner {

    private final DatabaseTicketingTransactionalBenchmark benchmark;

    public TicketingTransactionalBenchmarkRunner(DatabaseTicketingTransactionalBenchmark benchmark) {
        this.benchmark = benchmark;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.containsOption("runTicketingBenchmark")) {
            System.out.println("🚀 A iniciar Benchmark Transacional de Ticketing...\n");
            benchmark.runFullBenchmark();
            System.out.println("\n✅ Benchmark Ticketing concluído!");
            System.exit(0);
        }
    }
}