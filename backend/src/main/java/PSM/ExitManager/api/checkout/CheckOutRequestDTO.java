package PSM.ExitManager.api.checkout;

import java.util.UUID;

public record CheckOutRequestDTO(UUID titleId, UUID tripId) {}
