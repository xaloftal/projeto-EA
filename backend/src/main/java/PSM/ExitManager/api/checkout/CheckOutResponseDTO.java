package PSM.ExitManager.api.checkout;

public record CheckOutResponseDTO(
    boolean success, 
    String message,
    String situation,
    String destinationStopName,
    String currentStopName
) {
    // Construtor para compatibilidade com código existente que usa apenas 2 parâmetros
    public CheckOutResponseDTO(boolean success, String message) {
        this(success, message, null, null, null);
    }
}