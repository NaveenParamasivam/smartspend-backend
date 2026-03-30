package com.smartspend.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.smartspend.entity.Expense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generatePdfReport(List<Expense> expenses, LocalDate from, LocalDate to, String userName) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(26, 26, 46));
            Font subFont  = FontFactory.getFont(FontFactory.HELVETICA, 11, new BaseColor(100, 116, 139));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(51, 65, 85));
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(26, 26, 46));

            // Title
            Paragraph title = new Paragraph("SmartSpend Expense Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph sub = new Paragraph(
                    userName + " | " + DATE_FMT.format(from) + " – " + DATE_FMT.format(to), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            // Summary line
            BigDecimal totalIncome  = expenses.stream()
                    .filter(e -> e.getType() == Expense.TransactionType.INCOME)
                    .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = expenses.stream()
                    .filter(e -> e.getType() == Expense.TransactionType.EXPENSE)
                    .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal net = totalIncome.subtract(totalExpense);

            PdfPTable summary = new PdfPTable(3);
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(20);
            addSummaryCell(summary, "Total Income", "₹ " + totalIncome.toPlainString(),
                    new BaseColor(16, 185, 129), totalFont);
            addSummaryCell(summary, "Total Expense", "₹ " + totalExpense.toPlainString(),
                    new BaseColor(239, 68, 68), totalFont);
            addSummaryCell(summary, "Net Balance", "₹ " + net.toPlainString(),
                    net.signum() >= 0 ? new BaseColor(59, 130, 246) : new BaseColor(239, 68, 68), totalFont);
            doc.add(summary);

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 2f, 2f, 3f});

            String[] headers = {"Title", "Amount", "Category", "Type", "Date", "Description"};
            BaseColor headerBg = new BaseColor(26, 26, 46);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(8);
                cell.setBorderColor(headerBg);
                table.addCell(cell);
            }

            boolean alt = false;
            for (Expense e : expenses) {
                BaseColor rowColor = alt ? new BaseColor(248, 250, 252) : BaseColor.WHITE;
                addTableCell(table, e.getTitle(), cellFont, rowColor);
                addTableCell(table, "₹ " + e.getAmount().toPlainString(), cellFont, rowColor);
                addTableCell(table, e.getCategory().name(), cellFont, rowColor);
                BaseColor typeColor = e.getType() == Expense.TransactionType.INCOME
                        ? new BaseColor(16, 185, 129) : new BaseColor(239, 68, 68);
                Font typFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, typeColor);
                PdfPCell typeCell = new PdfPCell(new Phrase(e.getType().name(), typFont));
                typeCell.setBackgroundColor(rowColor); typeCell.setPadding(6);
                table.addCell(typeCell);
                addTableCell(table, DATE_FMT.format(e.getDate()), cellFont, rowColor);
                addTableCell(table, e.getDescription() != null ? e.getDescription() : "-", cellFont, rowColor);
                alt = !alt;
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("PDF generation error: {}", ex.getMessage());
            throw new RuntimeException("Failed to generate PDF report");
        }
    }

    private void addSummaryCell(PdfPTable t, String label, String value, BaseColor color, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorderColor(color);
        cell.setBorderWidth(2);
        Paragraph p = new Paragraph();
        Font lFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(100, 116, 139));
        p.add(new Chunk(label + "\n", lFont));
        p.add(new Chunk(value, font));
        cell.addElement(p);
        t.addCell(cell);
    }

    private void addTableCell(PdfPTable t, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        t.addCell(cell);
    }

    public byte[] generateExcelReport(List<Expense> expenses, LocalDate from, LocalDate to, String userName)
            throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("Expense Report");
            sheet.setColumnWidth(0, 7000); sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 4000); sheet.setColumnWidth(3, 3500);
            sheet.setColumnWidth(4, 4000); sheet.setColumnWidth(5, 8000);

            // Title row
            XSSFCellStyle titleStyle = wb.createCellStyle();
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(new XSSFColor(new byte[]{(byte)26, (byte)26, (byte)46}, null));
            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SmartSpend Expense Report — " + userName);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue(
                    DATE_FMT.format(from) + " to " + DATE_FMT.format(to));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            // Header row
            XSSFCellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26, (byte)26, (byte)46}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(3);
            String[] cols = {"Title", "Amount (₹)", "Category", "Type", "Date", "Description"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            XSSFCellStyle evenStyle = wb.createCellStyle();
            evenStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)248, (byte)250, (byte)252}, null));
            evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle incomeStyle = wb.createCellStyle();
            XSSFFont incFont = wb.createFont();
            incFont.setColor(new XSSFColor(new byte[]{(byte)16, (byte)185, (byte)129}, null));
            incFont.setBold(true); incomeStyle.setFont(incFont);

            XSSFCellStyle expStyle = wb.createCellStyle();
            XSSFFont expFont = wb.createFont();
            expFont.setColor(new XSSFColor(new byte[]{(byte)239, (byte)68, (byte)68}, null));
            expFont.setBold(true); expStyle.setFont(expFont);

            int rowNum = 4;
            for (Expense e : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getTitle());
                row.createCell(1).setCellValue(e.getAmount().doubleValue());
                row.createCell(2).setCellValue(e.getCategory().name());
                Cell typeCell = row.createCell(3);
                typeCell.setCellValue(e.getType().name());
                typeCell.setCellStyle(e.getType() == Expense.TransactionType.INCOME ? incomeStyle : expStyle);
                row.createCell(4).setCellValue(DATE_FMT.format(e.getDate()));
                row.createCell(5).setCellValue(e.getDescription() != null ? e.getDescription() : "");
            }

            // Summary
            int sumRow = rowNum + 1;
            BigDecimal inc = expenses.stream().filter(e -> e.getType() == Expense.TransactionType.INCOME)
                    .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal exp = expenses.stream().filter(e -> e.getType() == Expense.TransactionType.EXPENSE)
                    .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            sheet.createRow(sumRow).createCell(0).setCellValue("Total Income: ₹" + inc.toPlainString());
            sheet.createRow(sumRow + 1).createCell(0).setCellValue("Total Expense: ₹" + exp.toPlainString());
            sheet.createRow(sumRow + 2).createCell(0)
                    .setCellValue("Net Balance: ₹" + inc.subtract(exp).toPlainString());

            wb.write(out);
            return out.toByteArray();
        }
    }
}