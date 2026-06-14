package PSM.ExitManager.api.checkout;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout-transport")
public class CheckOutController {
    private final CheckOutTransportService service;

    public CheckOutController(CheckOutTransportService service) {
        this.service = service;
    }

    @PostMapping
    public CheckOutResponseDTO checkOut(@RequestBody CheckOutRequestDTO request) {
        return service.checkOut(request);
    }
    
    @GetMapping("/situation/{titleId}/{tripId}")
    public CheckoutSituationResponseDTO getCheckoutSituation(
            @PathVariable UUID titleId, 
            @PathVariable UUID tripId) {
        return service.getCheckoutSituation(titleId, tripId);
    }
}