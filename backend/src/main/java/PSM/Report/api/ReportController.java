package PSM.Report.api;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import PSM.UserManagement.User;
import PSM.UserManagement.api.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    private void checkAdminAccess(HttpServletRequest request) {
        User user = authService.currentUser(request);
        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Apenas administradores podem aceder aos relatorios.");
        }
    }

    @GetMapping("/stats")
    public TransportReportStatsDTO getStats(
            @RequestParam UUID vehicleId,
            @RequestParam String month,
            HttpServletRequest request) {
        checkAdminAccess(request);
        return reportService.getStats(vehicleId, month);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReport(
            @RequestParam UUID vehicleId,
            @RequestParam String month,
            @RequestParam String format,
            HttpServletRequest request) {
        checkAdminAccess(request);

        String vehicleInfo = reportService.getVehicleInfo(vehicleId);
        byte[] fileContent;
        String fileName;
        MediaType mediaType;

        if ("pdf".equalsIgnoreCase(format)) {
            TransportReportStatsDTO stats = reportService.getStats(vehicleId, month);
            fileContent = reportService.generatePdf(vehicleId, month, stats, vehicleInfo);
            fileName = "relatorio_" + month + "_" + vehicleId.toString().substring(0, 8) + ".pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else if ("csv".equalsIgnoreCase(format)) {
            fileContent = reportService.generateCsv(vehicleId, month);
            fileName = "relatorio_" + month + "_" + vehicleId.toString().substring(0, 8) + ".csv";
            mediaType = MediaType.parseMediaType("text/csv; charset=utf-8");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato nao suportado. Utilize 'pdf' ou 'csv'.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
}