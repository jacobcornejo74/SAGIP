package GUI;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.imageio.ImageIO;
import java.util.List;

/**
 * Saves and loads camera scan records (sagip-storage/scans.json) and PNG captures with boxes.
 */
class ScanStorageManager {
    private final Path storageDir = Paths.get("sagip-storage");
    private final Path capturesDir = storageDir.resolve("captures");
    private final Path jsonFile = storageDir.resolve("scans.json");

    public List<SavedScanRecord> loadAll() {
        List<SavedScanRecord> result = new ArrayList<>();
        if (!Files.exists(jsonFile)) {
            return result;
        }
        try {
            String raw = Files.readString(jsonFile, StandardCharsets.UTF_8);
            Object parsed = new SimpleJsonParser(raw).parse();
            if (!(parsed instanceof List<?> list)) {
                return result;
            }
            for (Object itemObj : list) {
                if (!(itemObj instanceof Map<?, ?> map)) continue;
                String id = asStr(map.get("id"), String.valueOf(System.currentTimeMillis()));
                String timestamp = asStr(map.get("timestamp"), "");
                String severity = asStr(map.get("overallSeverity"), "LOW");
                String label = asStr(map.get("overallLabel"), "LOW RISK");
                int count = asInt(map.get("detectionCount"), 0);
                String imageFile = asStr(map.get("imageFile"), "");

                BufferedImage snapshot = null;
                if (!imageFile.isEmpty()) {
                    Path imgPath = Paths.get(imageFile);
                    if (Files.exists(imgPath)) {
                        try {
                            snapshot = ImageIO.read(imgPath.toFile());
                        } catch (Exception ignored) {}
                    }
                }

                List<SavedDetectionEntry> detections = new ArrayList<>();
                Object itemsObj = map.get("items");
                if (itemsObj instanceof List<?> detList) {
                    for (Object detRaw : detList) {
                        if (!(detRaw instanceof Map<?, ?> det)) continue;
                        String dLabel = asStr(det.get("label"), "Object");
                        String dTag = asStr(det.get("tag"), "E");
                        int dConf = asInt(det.get("confidence"), 0);
                        String dSev = asStr(det.get("severity"), "LOW");
                        double bx = 0, by = 0, bw = 0, bh = 0;
                        if (det.get("bbox") instanceof Map<?, ?> boxMap) {
                            bx = asDouble(boxMap.get("x"), 0);
                            by = asDouble(boxMap.get("y"), 0);
                            bw = asDouble(boxMap.get("width"), 0);
                            bh = asDouble(boxMap.get("height"), 0);
                        }
                        List<String> risks = new ArrayList<>();
                        if (det.get("risks") instanceof List<?> riskList) {
                            for (Object r : riskList) {
                                if (r != null) risks.add(String.valueOf(r));
                            }
                        }
                        detections.add(new SavedDetectionEntry(dLabel, dTag, dConf, dSev, bx, by, bw, bh, risks));
                    }
                }
                result.add(new SavedScanRecord(id, timestamp, severity, label, count, imageFile, snapshot, detections));
            }
        } catch (Exception e) {
            System.err.println("[SAGIP] Failed to read scans.json: " + e.getMessage());
        }
        return result;
    }

    public SavedScanRecord saveNewScan(String id, String timestamp, String overallSeverity, String overallLabel,
                                       int detectionCount, BufferedImage snapshot, List<SavedDetectionEntry> detections) {
        String relImagePath = "";
        try {
            Files.createDirectories(capturesDir);
            if (snapshot != null) {
                Path imgPath = capturesDir.resolve("scan_" + id + ".png");
                ImageIO.write(snapshot, "png", imgPath.toFile());
                relImagePath = "sagip-storage/captures/scan_" + id + ".png";
            }
        } catch (Exception e) {
            System.err.println("[SAGIP] Failed to save capture image: " + e.getMessage());
        }

        SavedScanRecord record = new SavedScanRecord(
                id, timestamp, overallSeverity, overallLabel, detectionCount, relImagePath, snapshot, detections);

        List<SavedScanRecord> current = loadAll();
        current.add(0, record);
        writeAll(current);
        return record;
    }

    public void deleteScan(SavedScanRecord target, List<SavedScanRecord> remaining) {
        if (target.imageFile != null && !target.imageFile.isEmpty()) {
            try {
                Files.deleteIfExists(Paths.get(target.imageFile));
            } catch (Exception ignored) {}
        }
        writeAll(remaining);
    }

    public void clearAll(List<SavedScanRecord> records) {
        for (SavedScanRecord rec : records) {
            if (rec.imageFile != null && !rec.imageFile.isEmpty()) {
                try {
                    Files.deleteIfExists(Paths.get(rec.imageFile));
                } catch (Exception ignored) {}
            }
        }
        writeAll(Collections.emptyList());
    }

    private void writeAll(List<SavedScanRecord> records) {
        try {
            Files.createDirectories(storageDir);
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < records.size(); i++) {
                SavedScanRecord r = records.get(i);
                sb.append("  {\n");
                sb.append("    \"id\": ").append(jsonStr(r.id)).append(",\n");
                sb.append("    \"timestamp\": ").append(jsonStr(r.timestamp)).append(",\n");
                sb.append("    \"overallSeverity\": ").append(jsonStr(r.overallSeverity)).append(",\n");
                sb.append("    \"overallLabel\": ").append(jsonStr(r.overallLabel)).append(",\n");
                sb.append("    \"detectionCount\": ").append(r.detectionCount).append(",\n");
                sb.append("    \"imageFile\": ").append(jsonStr(r.imageFile)).append(",\n");
                sb.append("    \"items\": [\n");
                for (int j = 0; j < r.detections.size(); j++) {
                    SavedDetectionEntry d = r.detections.get(j);
                    sb.append("      {\n");
                    sb.append("        \"label\": ").append(jsonStr(d.label)).append(",\n");
                    sb.append("        \"tag\": ").append(jsonStr(d.tag)).append(",\n");
                    sb.append("        \"confidence\": ").append(d.confidence).append(",\n");
                    sb.append("        \"severity\": ").append(jsonStr(d.severity)).append(",\n");
                    sb.append(String.format(Locale.US,
                            "        \"bbox\": { \"x\": %.4f, \"y\": %.4f, \"width\": %.4f, \"height\": %.4f },\n",
                            d.x, d.y, d.width, d.height));
                    sb.append("        \"risks\": [");
                    for (int k = 0; k < d.risks.size(); k++) {
                        if (k > 0) sb.append(", ");
                        sb.append(jsonStr(d.risks.get(k)));
                    }
                    sb.append("]\n");
                    sb.append("      }").append(j < r.detections.size() - 1 ? "," : "").append("\n");
                }
                sb.append("    ]\n");
                sb.append("  }").append(i < records.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]\n");
            Files.writeString(jsonFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[SAGIP] Failed to write scans.json: " + e.getMessage());
        }
    }

    private static String jsonStr(String value) {
        if (value == null) return "\"\"";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }

    private static String asStr(Object obj, String fallback) {
        return obj == null ? fallback : String.valueOf(obj);
    }

    private static int asInt(Object obj, int fallback) {
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception ignored) { return fallback; }
    }

    private static double asDouble(Object obj, double fallback) {
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception ignored) { return fallback; }
    }

    private static class SimpleJsonParser {
        private final String src;
        private int pos = 0;

        SimpleJsonParser(String src) {
            this.src = src != null ? src : "";
        }

        Object parse() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            return parseValue();
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') { pos += 4; return null; }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else {
                    if (pos < src.length() && src.charAt(pos) == '}') pos++;
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                list.add(parseValue());
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else {
                    if (pos < src.length() && src.charAt(pos) == ']') pos++;
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();
            if (pos < src.length() && src.charAt(pos) == '"') pos++;
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) { pos += 4; return true; }
            if (src.startsWith("false", pos)) { pos += 5; return false; }
            pos++;
            return false;
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            String num = src.substring(start, pos);
            try {
                if (num.contains(".") || num.contains("e") || num.contains("E")) {
                    return Double.parseDouble(num);
                }
                return Long.parseLong(num);
            } catch (Exception e) {
                return 0;
            }
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }
}
