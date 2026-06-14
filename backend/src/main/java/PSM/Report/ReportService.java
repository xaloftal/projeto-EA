package PSM.Report;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import PSM.ValidationManager.ValidationRecord;
import PSM.ValidationManager.api.validationrecord.ValidationRecordRepository;
import PSM.Travel.Vehicle;
import PSM.Travel.api.vehicle.VehicleRepository;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class ReportService {

    private final ValidationRecordRepository validationRecordRepository;
    private final VehicleRepository vehicleRepository;

    public ReportService(ValidationRecordRepository validationRecordRepository, VehicleRepository vehicleRepository) {
        this.validationRecordRepository = validationRecordRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<ValidationRecord> getValidationRecords(UUID vehicleId, String yearMonth) {
        // Parse "YYYY-MM"
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        LocalDate startDay = LocalDate.of(year, month, 1);
        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end = startDay.plusMonths(1).atStartOfDay().minusNanos(1);

        return validationRecordRepository.findByVehicleAndPeriod(vehicleId, start, end);
    }

    public TransportReportStatsDTO getStats(UUID vehicleId, String yearMonth) {
        List<ValidationRecord> records = getValidationRecords(vehicleId, yearMonth);

        long total = records.size();
        long success = records.stream().filter(ValidationRecord::getResult).count();
        long failed = total - success;

        // Group by Day of Week
        Map<String, Long> byDay = new LinkedHashMap<>();
        // Initialize days in order to guarantee nice display
        String[] daysPt = {"Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"};
        for (int i = 0; i < 7; i++) {
            byDay.put(daysPt[i], 0L);
        }
        for (ValidationRecord vr : records) {
            DayOfWeek dow = vr.getTimestamp().getDayOfWeek();
            int idx = dow.getValue() - 1; // Monday is 1, Sunday is 7
            String key = daysPt[idx];
            byDay.put(key, byDay.get(key) + 1);
        }

        // Group by Hour of Day
        Map<Integer, Long> byHour = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            byHour.put(h, 0L);
        }
        for (ValidationRecord vr : records) {
            int hour = vr.getTimestamp().getHour();
            byHour.put(hour, byHour.get(hour) + 1);
        }

        // Group by Stop Name
        Map<String, Long> byStop = records.stream()
            .collect(Collectors.groupingBy(
                vr -> vr.getStop() != null ? vr.getStop().getName() : "Desconhecido",
                Collectors.counting()
            ));

        // Sort stops by validation count descending
        Map<String, Long> sortedStops = byStop.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return new TransportReportStatsDTO(total, success, failed, byDay, byHour, sortedStops);
    }

    public String getVehicleInfo(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) return "Desconhecido";
        String type = vehicle.getType() != null ? vehicle.getType() : "Transporte";
        String routeName = (vehicle.getRoute() != null) ? vehicle.getRoute().getName() : "";
        return type + " (" + (routeName.isEmpty() ? vehicleId.toString().substring(0, 8) : routeName) + ")";
    }

    public byte[] generateCsv(UUID vehicleId, String yearMonth) {
        List<ValidationRecord> records = getValidationRecords(vehicleId, yearMonth);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Timestamp,Resultado,Paragem,Trip_ID\n");
        for (ValidationRecord vr : records) {
            String stopName = vr.getStop() != null ? vr.getStop().getName() : "Desconhecido";
            sb.append(vr.getId()).append(",")
              .append(vr.getTimestamp()).append(",")
              .append(vr.getResult() ? "SUCESSO" : "FALHA").append(",")
              .append("\"").append(stopName.replace("\"", "\"\"")).append("\",")
              .append(vr.getTrip() != null ? vr.getTrip().getId() : "").append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] generatePdf(UUID vehicleId, String yearMonth, TransportReportStatsDTO stats, String vehicleInfo) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font Definitions
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD, new java.awt.Color(70, 75, 162));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, new java.awt.Color(70, 75, 162));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font bodyFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            // Title
            Paragraph title = new Paragraph("CATCHIT - RELATORIO ESTATISTICO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Subtitle
            Paragraph subtitle = new Paragraph("Transporte: " + vehicleInfo + "  |  Periodo: " + yearMonth, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(25);
            document.add(subtitle);

            // Section: Resumo das Validações
            Paragraph s1 = new Paragraph("1. Resumo Geral de Validacoes", sectionFont);
            s1.setSpacingAfter(10);
            document.add(s1);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);

            addTableCell(summaryTable, "Total de Validacoes:", bodyFontBold);
            addTableCell(summaryTable, String.valueOf(stats.getTotalValidations()), bodyFont);

            addTableCell(summaryTable, "Validacoes com Sucesso:", bodyFontBold);
            addTableCell(summaryTable, String.valueOf(stats.getSuccessfulValidations()), bodyFont);

            addTableCell(summaryTable, "Validacoes com Falha:", bodyFontBold);
            addTableCell(summaryTable, String.valueOf(stats.getFailedValidations()), bodyFont);

            double rate = stats.getTotalValidations() > 0 ? 
                (double) stats.getSuccessfulValidations() / stats.getTotalValidations() * 100 : 0.0;
            addTableCell(summaryTable, "Taxa de Sucesso:", bodyFontBold);
            addTableCell(summaryTable, String.format("%.2f%%", rate), bodyFont);

            document.add(summaryTable);

            // Section: Validações por Paragem
            Paragraph s2 = new Paragraph("2. Validacoes por Paragem (Top 10)", sectionFont);
            s2.setSpacingAfter(10);
            document.add(s2);

            PdfPTable stopTable = new PdfPTable(2);
            stopTable.setWidthPercentage(100);
            stopTable.setSpacingAfter(20);
            
            PdfPCell h1 = new PdfPCell(new Phrase("Paragem", bodyFontBold));
            h1.setBackgroundColor(new java.awt.Color(230, 230, 230));
            h1.setPadding(6);
            stopTable.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Nº Validacoes", bodyFontBold));
            h2.setBackgroundColor(new java.awt.Color(230, 230, 230));
            h2.setPadding(6);
            stopTable.addCell(h2);

            int count = 0;
            for (Map.Entry<String, Long> entry : stats.getValidationsByStop().entrySet()) {
                if (count++ >= 10) break;
                addTableCell(stopTable, entry.getKey(), bodyFont);
                addTableCell(stopTable, String.valueOf(entry.getValue()), bodyFont);
            }
            if (stats.getValidationsByStop().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Sem dados disponiveis", bodyFont));
                emptyCell.setColspan(2);
                emptyCell.setPadding(6);
                stopTable.addCell(emptyCell);
            }
            document.add(stopTable);

            // Section: Distribuição Semanal
            Paragraph s3 = new Paragraph("3. Distribuicao por Dia da Semana", sectionFont);
            s3.setSpacingAfter(10);
            document.add(s3);

            PdfPTable dayTable = new PdfPTable(2);
            dayTable.setWidthPercentage(100);
            dayTable.setSpacingAfter(20);

            PdfPCell dh1 = new PdfPCell(new Phrase("Dia da Semana", bodyFontBold));
            dh1.setBackgroundColor(new java.awt.Color(230, 230, 230));
            dh1.setPadding(6);
            dayTable.addCell(dh1);

            PdfPCell dh2 = new PdfPCell(new Phrase("Nº Validacoes", bodyFontBold));
            dh2.setBackgroundColor(new java.awt.Color(230, 230, 230));
            dh2.setPadding(6);
            dayTable.addCell(dh2);

            for (Map.Entry<String, Long> entry : stats.getValidationsByDayOfWeek().entrySet()) {
                addTableCell(dayTable, entry.getKey(), bodyFont);
                addTableCell(dayTable, String.valueOf(entry.getValue()), bodyFont);
            }
            document.add(dayTable);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        table.addCell(cell);
    }
}
