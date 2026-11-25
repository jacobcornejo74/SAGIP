package GUI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanStorageManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndReloadsScanAndSnapshot() throws IOException {
        ScanStorageManager storage = new ScanStorageManager(tempDir);
        BufferedImage snapshot = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);

        SavedScanRecord saved = storage.saveNewScan(
                "test-scan", "2026-11-25 12:00:00", "LOW", "LOW RISK",
                0, snapshot, Collections.emptyList());

        assertTrue(Files.exists(tempDir.resolve("scans.json")));
        assertTrue(Files.exists(Path.of(saved.imageFile)));
        assertEquals(1, storage.loadAll().size());
        assertNotNull(storage.loadAll().get(0).snapshot);

        storage.deleteScan(saved, Collections.emptyList());

        assertTrue(storage.loadAll().isEmpty());
        assertFalse(Files.exists(Path.of(saved.imageFile)));
    }

    @Test
    void reportsStorageReadFailureAndRemovesUncommittedSnapshot() throws IOException {
        Path scansPath = tempDir.resolve("scans.json");
        Files.createDirectory(scansPath);
        ScanStorageManager storage = new ScanStorageManager(tempDir);
        BufferedImage snapshot = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);

        assertThrows(IOException.class, storage::loadAll);
        assertThrows(IOException.class, () -> storage.saveNewScan(
                "failed-scan", "2026-11-25 12:00:00", "LOW", "LOW RISK",
                0, snapshot, Collections.emptyList()));

        Path capturesDir = tempDir.resolve("captures");
        assertTrue(Files.exists(capturesDir));
        try (var files = Files.list(capturesDir)) {
            assertEquals(0, files.count());
        }
    }
}
