package PSM.Services.OTP;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routing")
public class OtpRoutingController {

    private final OtpRoutingService routing;

    public OtpRoutingController(OtpRoutingService routing) {
        this.routing = routing;
    }

    @GetMapping("/plan")
    public JsonNode plan(
            @RequestParam double fromLat,
            @RequestParam double fromLon,
            @RequestParam double toLat,
            @RequestParam double toLon,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time) {
        return routing.planFewestTransfers(fromLat, fromLon, toLat, toLon, date, time);
    }
}
