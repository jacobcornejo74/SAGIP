package GUI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes real YOLOv8n ONNX neural object detection (80 COCO classes) via
 * Microsoft ONNX Runtime (`ai.onnxruntime`). Uses reflection to keep ONNX
 * Runtime an optional compile-time dependency.
 */
class YoloOnnxDetector {
    private static final String MODEL_FILE = "yolov8n.onnx";
    private static final String[] MODEL_URLS = {
        "https://huggingface.co/cabelo/yolov8/resolve/main/yolov8n.onnx",
        "https://huggingface.co/Kalray/yolov8/resolve/main/yolov8n.onnx"
    };

    private static final String[] COCO_CLASSES = {
        "PERSON", "BICYCLE", "CAR", "MOTORCYCLE", "AIRPLANE", "BUS", "TRAIN", "TRUCK", "BOAT",
        "TRAFFIC LIGHT", "FIRE HYDRANT", "STOP SIGN", "PARKING METER", "BENCH", "BIRD", "CAT",
        "DOG", "HORSE", "SHEEP", "COW", "ELEPHANT", "BEAR", "ZEBRA", "GIRAFFE", "BACKPACK",
        "UMBRELLA", "HANDBAG", "TIE", "SUITCASE", "FRISBEE", "SKIS", "SNOWBOARD", "SPORTS BALL",
        "KITE", "BASEBALL BAT", "BASEBALL GLOVE", "SKATEBOARD", "SURFBOARD", "TENNIS RACKET",
        "BOTTLE", "WINE GLASS", "CUP", "FORK", "KNIFE", "SPOON", "BOWL", "BANANA", "APPLE",
        "SANDWICH", "ORANGE", "BROCCOLI", "CARROT", "HOT DOG", "PIZZA", "DONUT", "CAKE",
        "CHAIR", "COUCH", "POTTED PLANT", "BED", "DINING TABLE", "TOILET", "TV", "LAPTOP",
        "MOUSE", "REMOTE", "KEYBOARD", "CELL PHONE", "MICROWAVE", "OVEN", "TOASTER", "SINK",
        "REFRIGERATOR", "BOOK", "CLOCK", "VASE", "SCISSORS", "TEDDY BEAR", "HAIR DRIER", "TOOTHBRUSH"
    };

    private Object ortEnv = null;
    private Object ortSession = null;
    private String inputName = "images";
    private volatile boolean ready = false;
    private volatile String status = "YOLO INIT";
    private final AtomicBoolean initStarted = new AtomicBoolean(false);

    public String getStatus() { return status; }
    public boolean isReady() { return ready; }

    public void initializeAsync() {
        if (!initStarted.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> {
            try {
                Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
                File modelFile = new File(MODEL_FILE);
                if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
                    downloadModel(modelFile.toPath());
                }
                if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
                    status = "YOLOv8 WEIGHTS OFFLINE";
                    return;
                }

                status = "LOADING YOLOv8n ONNX...";
                ortEnv = envClass.getMethod("getEnvironment").invoke(null);
                Class<?> optsClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
                Object opts = optsClass.getConstructor().newInstance();
                ortSession = envClass.getMethod("createSession", String.class, optsClass)
                        .invoke(ortEnv, modelFile.getAbsolutePath(), opts);

                Set<?> names = (Set<?>) ortSession.getClass().getMethod("getInputNames").invoke(ortSession);
                if (names != null && !names.isEmpty()) {
                    inputName = String.valueOf(names.iterator().next());
                }
                ready = true;
                status = "YOLOv8n-ONNX READY";
                System.out.println(">> [YOLO ENGINE] YOLOv8n ONNX Neural Network loaded and active.");
            } catch (ClassNotFoundException e) {
                status = "ONNX JAR PENDING (RUN VIA MAVEN)";
                System.err.println(">> [YOLO NOTICE] ONNX Runtime not on classpath. Run via: mvn clean compile exec:java");
            } catch (Throwable ex) {
                status = "YOLO LOAD ERR";
                System.err.println(">> [YOLO ERROR] " + ex.getMessage());
            }
        }, "YOLOv8-Loader");
        t.setDaemon(true);
        t.start();
    }

    private void downloadModel(Path target) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        for (String url : MODEL_URLS) {
            try {
                status = "DOWNLOADING YOLOv8n.ONNX...";
                System.out.println(">> [YOLO ENGINE] Downloading YOLOv8n ONNX weights from " + url);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Java-Swing-YOLO-Hazard-Detector/3.0")
                        .GET()
                        .build();
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) {
                    Path tmp = Path.of(target.toString() + ".part");
                    try (InputStream in = resp.body()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (Files.size(tmp) > 1_000_000L) {
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println(">> [YOLO ENGINE] Saved yolov8n.onnx (" + (Files.size(target) / 1024 / 1024) + " MB)");
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println(">> [YOLO DOWNLOAD RETRY] " + e.getMessage());
            }
        }
    }

    public List<HazardItem> detect(BufferedImage frame, float confThreshold) {
        if (!ready || ortSession == null || frame == null) return Collections.emptyList();

        Object tensor = null;
        Object result = null;
        try {
            final int SIZE = 640;
            BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(frame, 0, 0, SIZE, SIZE, null);
            g2.dispose();

            int[] rgb = scaled.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
            int area = SIZE * SIZE;
            float[] chw = new float[3 * area];
            for (int i = 0; i < area; i++) {
                int p = rgb[i];
                chw[i] = ((p >> 16) & 0xFF) / 255.0f;
                chw[area + i] = ((p >> 8) & 0xFF) / 255.0f;
                chw[2 * area + i] = (p & 0xFF) / 255.0f;
            }

            Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
            Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
            tensor = tensorClass.getMethod("createTensor", envClass, FloatBuffer.class, long[].class)
                    .invoke(null, ortEnv, FloatBuffer.wrap(chw), new long[]{1, 3, SIZE, SIZE});

            result = ortSession.getClass().getMethod("run", Map.class)
                    .invoke(ortSession, Collections.singletonMap(inputName, tensor));

            Object onnxVal = result.getClass().getMethod("get", int.class).invoke(result, 0);
            float[][][] out = (float[][][]) onnxVal.getClass().getMethod("getValue").invoke(onnxVal);
            float[][] preds = out[0]; // [84][8400]
            int numAnchors = preds[0].length;
            int numClasses = Math.min(COCO_CLASSES.length, preds.length - 4);

            List<RawBox> candidates = new ArrayList<>();
            for (int a = 0; a < numAnchors; a++) {
                float maxScore = 0f;
                int bestClass = -1;
                for (int c = 0; c < numClasses; c++) {
                    float s = preds[4 + c][a];
                    if (s > maxScore) {
                        maxScore = s;
                        bestClass = c;
                    }
                }
                if (maxScore >= confThreshold && bestClass >= 0) {
                    float cx = preds[0][a];
                    float cy = preds[1][a];
                    float bw = preds[2][a];
                    float bh = preds[3][a];

                    double nx = Math.max(0.0, Math.min(1.0, (cx - bw / 2.0) / SIZE));
                    double ny = Math.max(0.0, Math.min(1.0, (cy - bh / 2.0) / SIZE));
                    double nw = Math.max(0.02, Math.min(1.0 - nx, bw / SIZE));
                    double nh = Math.max(0.02, Math.min(1.0 - ny, bh / SIZE));
                    candidates.add(new RawBox(bestClass, maxScore, nx, ny, nw, nh));
                }
            }

            // Non-Maximum Suppression (NMS)
            candidates.sort((a, b) -> Float.compare(b.score, a.score));
            List<RawBox> kept = new ArrayList<>();
            for (RawBox box : candidates) {
                boolean overlaps = false;
                for (RawBox k : kept) {
                    if (k.classId == box.classId && iou(box, k) > 0.45) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    kept.add(box);
                    if (kept.size() >= 10) break;
                }
            }

            List<HazardItem> items = new ArrayList<>();
            int idx = 1;
            for (RawBox k : kept) {
                String label = COCO_CLASSES[k.classId];
                items.add(new HazardItem(
                        String.format("YOLO-%02d", idx++),
                        label,
                        k.score,
                        k.x, k.y, k.w, k.h
                ));
            }
            return items;
        } catch (Throwable t) {
            return Collections.emptyList();
        } finally {
            try { if (result instanceof AutoCloseable) ((AutoCloseable) result).close(); } catch (Exception ignored) {}
            try { if (tensor instanceof AutoCloseable) ((AutoCloseable) tensor).close(); } catch (Exception ignored) {}
        }
    }

    private static double iou(RawBox a, RawBox b) {
        double x1 = Math.max(a.x, b.x);
        double y1 = Math.max(a.y, b.y);
        double x2 = Math.min(a.x + a.w, b.x + b.w);
        double y2 = Math.min(a.y + a.h, b.y + b.h);
        double inter = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        double union = a.w * a.h + b.w * b.h - inter;
        return union <= 0 ? 0 : inter / union;
    }

    private static final class RawBox {
        final int classId;
        final float score;
        final double x, y, w, h;
        RawBox(int classId, float score, double x, double y, double w, double h) {
            this.classId = classId;
            this.score = score;
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }
}
