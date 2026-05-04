package PSM.Travel.api.vehicle;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles/simulation")
public class VehicleSimulationController {
    private final VehicleSimulationService service;

    public VehicleSimulationController(VehicleSimulationService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehicleSimulationSnapshotDTO> getAll() {
        return service.getLatestSnapshots();
    }

    @GetMapping("/{vehicleId}")
    public VehicleSimulationSnapshotDTO getByVehicleId(@PathVariable UUID vehicleId) {
        return service.getSnapshot(vehicleId);
    }
}