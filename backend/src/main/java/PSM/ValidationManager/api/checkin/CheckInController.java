package PSM.ValidationManager.api.checkin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/checkin")
public class CheckInController {
    private final CheckInService service;

    public CheckInController(CheckInService service) {
        this.service = service;
    }

    @PostMapping
    public CheckInResponseDTO checkIn(@RequestBody CheckInRequestDTO request) {
        return service.checkIn(request);
    }
}

