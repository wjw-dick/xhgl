package com.xinhuo.boiler.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {
    private static final BaseFont FONT = loadFont();

    private static BaseFont loadFont() {
        try (InputStream in = PdfService.class.getResourceAsStream("/fonts/simhei.ttf")) {
            if (in == null) {
                return null;
            }
            Path temp = Files.createTempFile("xh-pdf-font-", ".ttf");
            Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return BaseFont.createFont(temp.toAbsolutePath().toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            return null;
        }
    }

    private Font font(float size, boolean bold, Color color) {
        if (FONT == null) {
            return FontFactory.getFont(FontFactory.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL, color);
        }
        return new Font(FONT, size, bold ? Font.BOLD : Font.NORMAL, color);
    }

    public byte[] build(String title, String subtitle, Map<String, String> fields,
                        List<String[]> table, List<String> notes, String signLine) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 48, 42);
            PdfWriter.getInstance(document, out);
            document.open();
            Paragraph brand = new Paragraph("鑫火锅炉数字化平台", font(9, false, Color.GRAY));
            brand.setAlignment(Element.ALIGN_RIGHT);
            document.add(brand);
            Paragraph h = new Paragraph(title, font(20, true, new Color(31, 111, 235)));
            h.setAlignment(Element.ALIGN_CENTER);
            h.setSpacingAfter(6);
            document.add(h);
            if (subtitle != null && !subtitle.isBlank()) {
                Paragraph sub = new Paragraph(subtitle, font(10, false, Color.DARK_GRAY));
                sub.setAlignment(Element.ALIGN_CENTER);
                sub.setSpacingAfter(16);
                document.add(sub);
            }
            if (fields != null && !fields.isEmpty()) {
                PdfPTable info = new PdfPTable(4);
                info.setWidthPercentage(100);
                info.setWidths(new float[]{1.1f, 2.2f, 1.1f, 2.2f});
                for (Map.Entry<String, String> e : fields.entrySet()) {
                    info.addCell(labelCell(e.getKey()));
                    info.addCell(valueCell(e.getValue()));
                }
                info.setSpacingAfter(16);
                document.add(info);
            }
            if (table != null && !table.isEmpty()) {
                int cols = table.get(0).length;
                PdfPTable t = new PdfPTable(cols);
                t.setWidthPercentage(100);
                for (int i = 0; i < table.size(); i++) {
                    String[] row = table.get(i);
                    for (String cell : row) {
                        PdfPCell pc = new PdfPCell(new Phrase(cell == null ? "" : cell,
                                font(9, i == 0, i == 0 ? Color.WHITE : Color.BLACK)));
                        pc.setPadding(6);
                        if (i == 0) {
                            pc.setBackgroundColor(new Color(31, 111, 235));
                        } else if (i % 2 == 0) {
                            pc.setBackgroundColor(new Color(245, 248, 252));
                        }
                        t.addCell(pc);
                    }
                }
                t.setSpacingAfter(16);
                document.add(t);
            }
            if (notes != null) {
                for (String note : notes) {
                    Paragraph p = new Paragraph(note, font(9, false, new Color(80, 80, 80)));
                    p.setLeading(15);
                    document.add(p);
                }
            }
            Paragraph sign = new Paragraph(signLine == null ? "" : signLine, font(10, true, Color.BLACK));
            sign.setSpacingBefore(28);
            document.add(sign);
            Paragraph footer = new Paragraph("本文件由鑫火锅炉平台生成，仅用于业务流转；锅炉特种设备相关资质以原件及监管平台核验为准。",
                    font(8, false, Color.GRAY));
            footer.setSpacingBefore(20);
            document.add(footer);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败：" + e.getMessage(), e);
        }
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, true, Color.DARK_GRAY)));
        cell.setPadding(5);
        cell.setBackgroundColor(new Color(240, 244, 250));
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font(9, false, Color.BLACK)));
        cell.setPadding(5);
        return cell;
    }
}
