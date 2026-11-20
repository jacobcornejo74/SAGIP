import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';
import { defineConfig, type Plugin } from 'vite';

interface StoredScanEntry {
  id: string;
  timestamp: string;
  overallSeverity: string;
  detectionCount: number;
  imageFile?: string;
  snapshotDataUrl?: string;
  items: unknown[];
}

function sagipStoragePlugin(): Plugin {
  const storageDir = path.resolve(__dirname, 'sagip-storage');
  const capturesDir = path.join(storageDir, 'captures');
  const jsonFile = path.join(storageDir, 'scans.json');

  function ensureDirs() {
    if (!fs.existsSync(storageDir)) fs.mkdirSync(storageDir, { recursive: true });
    if (!fs.existsSync(capturesDir)) fs.mkdirSync(capturesDir, { recursive: true });
  }

  function readScans(): StoredScanEntry[] {
    ensureDirs();
    if (!fs.existsSync(jsonFile)) return [];
    try {
      const raw = fs.readFileSync(jsonFile, 'utf-8');
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  function writeScans(scans: StoredScanEntry[]) {
    ensureDirs();
    fs.writeFileSync(jsonFile, JSON.stringify(scans, null, 2), 'utf-8');
  }

  return {
    name: 'sagip-local-storage',
    configureServer(server) {
      server.middlewares.use('/api/captures', (req, res, next) => {
        if (req.method !== 'GET' || !req.url) return next();
        const safeName = path.basename(req.url.split('?')[0]);
        const target = path.join(capturesDir, safeName);
        if (!fs.existsSync(target)) {
          res.statusCode = 404;
          res.end('Not found');
          return;
        }
        res.setHeader('Content-Type', 'image/png');
        fs.createReadStream(target).pipe(res);
      });

      server.middlewares.use('/api/scans', (req, res, next) => {
        const reqPath = (req.url || '/').split('?')[0];

        if (req.method === 'GET' && (reqPath === '/' || reqPath === '')) {
          const scans = readScans().map((entry) => {
            if (!entry.snapshotDataUrl && entry.imageFile) {
              const imgName = path.basename(entry.imageFile);
              return { ...entry, snapshotDataUrl: `/api/captures/${imgName}` };
            }
            return entry;
          });
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify(scans));
          return;
        }

        if (req.method === 'POST' && (reqPath === '/' || reqPath === '')) {
          let body = '';
          req.on('data', (chunk) => {
            body += chunk;
          });
          req.on('end', () => {
            try {
              ensureDirs();
              const payload = JSON.parse(body) as StoredScanEntry;
              const safeId = String(payload.id || Date.now()).replace(/[^a-zA-Z0-9_-]/g, '');
              const imgFileName = `scan_${safeId}.png`;
              const imgAbsPath = path.join(capturesDir, imgFileName);

              if (typeof payload.snapshotDataUrl === 'string' && payload.snapshotDataUrl.startsWith('data:image/')) {
                const base64 = payload.snapshotDataUrl.split(',')[1] || '';
                fs.writeFileSync(imgAbsPath, Buffer.from(base64, 'base64'));
              }

              const entry: StoredScanEntry = {
                id: safeId,
                timestamp: payload.timestamp,
                overallSeverity: payload.overallSeverity,
                detectionCount: payload.detectionCount,
                imageFile: `sagip-storage/captures/${imgFileName}`,
                items: Array.isArray(payload.items) ? payload.items : [],
              };

              const existing = readScans().filter((s) => s.id !== safeId);
              const updated = [entry, ...existing];
              writeScans(updated);

              res.setHeader('Content-Type', 'application/json');
              res.end(
                JSON.stringify({
                  ...entry,
                  snapshotDataUrl: `/api/captures/${imgFileName}`,
                })
              );
            } catch (err) {
              res.statusCode = 400;
              res.setHeader('Content-Type', 'application/json');
              res.end(JSON.stringify({ error: String(err) }));
            }
          });
          return;
        }

        if (req.method === 'DELETE') {
          if (reqPath === '/' || reqPath === '') {
            const existing = readScans();
            for (const item of existing) {
              if (item.imageFile) {
                const imgPath = path.join(capturesDir, path.basename(item.imageFile));
                if (fs.existsSync(imgPath)) fs.unlinkSync(imgPath);
              }
            }
            writeScans([]);
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ ok: true }));
            return;
          }

          const targetId = reqPath.replace(/^\/+/, '');
          const existing = readScans();
          const target = existing.find((s) => s.id === targetId);
          if (target?.imageFile) {
            const imgPath = path.join(capturesDir, path.basename(target.imageFile));
            if (fs.existsSync(imgPath)) fs.unlinkSync(imgPath);
          }
          writeScans(existing.filter((s) => s.id !== targetId));
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ ok: true }));
          return;
        }

        next();
      });
    },
  };
}

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss(), sagipStoragePlugin()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modify—file watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
