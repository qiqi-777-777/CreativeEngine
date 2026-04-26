package com.mingde.creativeengine.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bp")
public class BpExportController {

    @PostMapping("/export/{format}")
    public ResponseEntity<byte[]> export(@PathVariable String format, @RequestBody Map<String, String> params) {
        String projectName = valueOrDefault(params.get("projectName"), "项目计划书");
        String content = cleanContent(valueOrDefault(params.get("content"), ""));

        if ("pdf".equalsIgnoreCase(format)) {
            byte[] bytes = buildPdf(projectName, content);
            return fileResponse(bytes, projectName + ".pdf", "application/pdf");
        }

        byte[] bytes = buildWord(projectName, content);
        return fileResponse(bytes, projectName + ".doc", "application/msword; charset=UTF-8");
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private byte[] buildWord(String projectName, String content) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"UTF-8\">");
        html.append("<style>");
        html.append("body{font-family:'Microsoft YaHei',SimSun,sans-serif;line-height:1.8;color:#111;padding:32px;}");
        html.append("h1{text-align:center;font-size:26px;margin-bottom:28px;}");
        html.append("h2{font-size:18px;margin:22px 0 8px;border-bottom:1px solid #ddd;padding-bottom:6px;}");
        html.append("p{font-size:14px;margin:8px 0;text-indent:2em;}");
        html.append("</style></head><body>");
        html.append("<h1>").append(escapeHtml(projectName)).append(" 项目计划书</h1>");

        for (String paragraph : splitParagraphs(content)) {
            if (isHeading(paragraph)) {
                html.append("<h2>").append(escapeHtml(paragraph)).append("</h2>");
            } else {
                html.append("<p>").append(escapeHtml(paragraph)).append("</p>");
            }
        }

        html.append("</body></html>");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildPdf(String projectName, String content) {
        try {
            List<byte[]> pages = renderPdfPages(projectName, content);
            return assemblePdf(pages, 1190, 1684);
        } catch (Exception e) {
            throw new IllegalStateException("PDF导出失败", e);
        }
    }

    private List<byte[]> renderPdfPages(String projectName, String content) throws Exception {
        int width = 1190;
        int height = 1684;
        int margin = 92;
        int bottom = height - 96;
        Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 44);
        Font headingFont = new Font("Microsoft YaHei", Font.BOLD, 30);
        Font bodyFont = new Font("Microsoft YaHei", Font.PLAIN, 26);
        List<byte[]> pages = new ArrayList<>();

        BufferedImage image = newPage(width, height);
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        int y = drawTitle(g, projectName + " 项目计划书", titleFont, width, margin);

        for (String paragraph : splitParagraphs(content)) {
            boolean heading = isHeading(paragraph);
            Font font = heading ? headingFont : bodyFont;
            int lineHeight = heading ? 46 : 42;
            int extraTop = heading ? 24 : 8;
            int extraBottom = heading ? 10 : 14;

            g.setFont(font);
            List<String> lines = wrapText(paragraph, g.getFontMetrics(), width - margin * 2);
            int needed = extraTop + lines.size() * lineHeight + extraBottom;

            if (y + needed > bottom) {
                g.dispose();
                pages.add(toJpeg(image));
                image = newPage(width, height);
                g = image.createGraphics();
                setupGraphics(g);
                y = margin;
            }

            y += extraTop;
            g.setFont(font);
            g.setColor(Color.BLACK);
            for (String line : lines) {
                g.drawString(line, margin, y);
                y += lineHeight;
            }
            y += extraBottom;
        }

        g.dispose();
        pages.add(toJpeg(image));
        return pages;
    }

    private BufferedImage newPage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private void setupGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private int drawTitle(Graphics2D g, String title, Font font, int width, int margin) {
        g.setFont(font);
        g.setColor(Color.BLACK);
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = wrapText(title, metrics, width - margin * 2);
        int y = margin;
        for (String line : lines) {
            int x = (width - metrics.stringWidth(line)) / 2;
            g.drawString(line, x, y);
            y += 58;
        }
        g.setColor(new Color(220, 220, 220));
        g.drawLine(margin, y, width - margin, y);
        return y + 34;
    }

    private byte[] toJpeg(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private byte[] assemblePdf(List<byte[]> imagePages, int imageWidth, int imageHeight) throws Exception {
        List<byte[]> objects = new ArrayList<>();
        StringBuilder kids = new StringBuilder();
        int pageCount = imagePages.size();

        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(new byte[0]);

        for (int i = 0; i < pageCount; i++) {
            int pageObj = 3 + i * 3;
            int contentObj = pageObj + 1;
            int imageObj = pageObj + 2;
            kids.append(pageObj).append(" 0 R ");

            String page = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                "/Resources << /XObject << /Im" + i + " " + imageObj + " 0 R >> >> " +
                "/Contents " + contentObj + " 0 R >>";
            objects.add(page.getBytes(StandardCharsets.ISO_8859_1));

            String content = "q\n595 0 0 842 0 0 cm\n/Im" + i + " Do\nQ\n";
            objects.add(stream(content.getBytes(StandardCharsets.ISO_8859_1), null));

            String imageHeader = "<< /Type /XObject /Subtype /Image /Width " + imageWidth +
                " /Height " + imageHeight + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode";
            objects.add(stream(imagePages.get(i), imageHeader));
        }

        objects.set(1, ("<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>").getBytes(StandardCharsets.ISO_8859_1));

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.size());
            pdf.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.write(objects.get(i));
            pdf.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }

        int xref = pdf.size();
        pdf.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i < offsets.size(); i++) {
            pdf.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
        }
        pdf.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return pdf.toByteArray();
    }

    private byte[] stream(byte[] data, String header) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            String prefix = (header == null ? "<<" : header) + " /Length " + data.length + " >>\nstream\n";
            out.write(prefix.getBytes(StandardCharsets.ISO_8859_1));
            out.write(data);
            out.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String value = text == null ? "" : text.trim();
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            String next = line + String.valueOf(ch);
            if (metrics.stringWidth(next) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(ch);
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private boolean isHeading(String paragraph) {
        String text = paragraph == null ? "" : paragraph.trim();
        return text.matches("^[一二三四五六七八九十]+[、.．].*") || text.matches("^\\d+[、.．].*");
    }

    private List<String> splitParagraphs(String content) {
        String[] parts = cleanContent(content).split("\\n+");
        List<String> paragraphs = new ArrayList<>();
        for (String part : parts) {
            String text = part.trim();
            if (!text.isEmpty()) {
                paragraphs.add(text);
            }
        }
        return paragraphs;
    }

    private String cleanContent(String content) {
        return valueOrDefault(content, "")
            .replace("\r", "\n")
            .replaceAll("(?m)^#{1,6}\\s*", "")
            .replaceAll("\\*\\*([^*]+?)\\*\\*", "$1")
            .replaceAll("\\*([^*\\n]+?)\\*", "$1")
            .replaceAll("`([^`]+?)`", "$1")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    private String escapeHtml(String value) {
        return valueOrDefault(value, "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
