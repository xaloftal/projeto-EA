package PSM.ExitManager.api.checkout;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CheckOutScheduler {
    private final CheckOutTransportService checkOutService;

    public CheckOutScheduler(CheckOutTransportService checkOutService) {
        this.checkOutService = checkOutService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void automatedCheckOut() {
        checkOutService.processAutomatedCheckOut();
    }
    
}
