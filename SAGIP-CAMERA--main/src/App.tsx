import React, { useEffect, useRef, useState } from 'react';
import * as cocoSsd from '@tensorflow-models/coco-ssd';
import * as tf from '@tensorflow/tfjs';
import { Upload, Camera, Trash2, BookmarkCheck } from 'lucide-react';
import { MenuBackground } from './components/MenuBackground';
import {
  DetectedObjectSummary,
  RiskSeverity,
  SavedAssessment,
  computeOverallSeverity,
  getHazardProfile,
  getRiskColor,
} from './hazards';

type Screen = 'home' | 'camera' | 'storage' | 'manual';

const STORAGE_KEY = 'sagip_saved_assessments_v1';

function formatCameraErrorMessage(err: unknown): string {
  const error = err as { name?: string; message?: string };
  const name = error?.name || '';
  const msg = error?.message || '';

  if (
    name === 'NotAllowedError' ||
    name === 'PermissionDeniedError' ||
    msg.toLowerCase().includes('permission')
  ) {
    return 'Camera permission was blocked. Allow camera access in your browser, then click Start Camera.';
  }
  if (
    name === 'NotFoundError' ||
    name === 'DevicesNotFoundError' ||
    msg.toLowerCase().includes('not found')
  ) {
    return 'No physical webcam detected on this device. Connect a camera or load a video/photo file.';
  }
  if (
    name === 'NotReadableError' ||
    name === 'TrackStartError' ||
    msg.toLowerCase().includes('in use')
  ) {
    return 'Camera is in use by another application. Close it and click Start Camera.';
  }
  return msg || 'Unable to open camera stream. Check browser camera permissions.';
}

export default function App() {
  const [screen, setScreen] = useState<Screen>('home');
  const [darkMode, setDarkMode] = useState<boolean>(true);
  const [savedAssessments, setSavedAssessments] = useState<SavedAssessment[]>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as SavedAssessment[]) : [];
    } catch {
      return [];
    }
  });

  // Camera & Detection state
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const activeStreamRef = useRef<MediaStream | null>(null);
  const objectUrlRef = useRef<string | null>(null);
  const modelRef = useRef<cocoSsd.ObjectDetection | null>(null);
  const animationFrameId = useRef<number | null>(null);
  const latestDetectionsRef = useRef<DetectedObjectSummary[]>([]);

  const [isCameraRunning, setIsCameraRunning] = useState<boolean>(false);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [detectionCount, setDetectionCount] = useState<number>(0);
  const [isMediaUploaded, setIsMediaUploaded] = useState<boolean>(false);
  const [scanSavedFeedback, setScanSavedFeedback] = useState<boolean>(false);

  // Sync `.dark` class on root <html>
  useEffect(() => {
    document.documentElement.classList.toggle('dark', darkMode);
  }, [darkMode]);

  // Persist assessments in localStorage
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(savedAssessments));
    } catch {
      // Ignore storage quota errors
    }
  }, [savedAssessments]);

  // Preload object detection model
  useEffect(() => {
    let isMounted = true;
    async function loadModel() {
      try {
        await tf.ready();
        const loaded = await cocoSsd.load({ base: 'mobilenet_v2' });
        if (isMounted) modelRef.current = loaded;
      } catch {
        try {
          const fallback = await cocoSsd.load({ base: 'lite_mobilenet_v2' });
          if (isMounted) modelRef.current = fallback;
        } catch {
          // Model unavailable offline
        }
      }
    }
    loadModel();
    return () => {
      isMounted = false;
    };
  }, []);

  const stopStream = () => {
    if (activeStreamRef.current) {
      activeStreamRef.current.getTracks().forEach((t) => t.stop());
      activeStreamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.pause();
      videoRef.current.srcObject = null;
      videoRef.current.removeAttribute('src');
      videoRef.current.load();
    }
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }
    latestDetectionsRef.current = [];
    setDetectionCount(0);
    setIsCameraRunning(false);
    setIsMediaUploaded(false);
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      ctx?.clearRect(0, 0, canvas.width, canvas.height);
    }
  };

  const startCamera = async () => {
    stopStream();
    setIsConnecting(true);
    setCameraError(null);

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setIsConnecting(false);
      setCameraError('Camera API is not supported in this browser environment.');
      return;
    }

    try {
      let stream: MediaStream;
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 1920, min: 1280 }, height: { ideal: 1080, min: 720 }, facingMode: 'environment' },
          audio: false,
        });
      } catch {
        stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: false,
        });
      }

      activeStreamRef.current = stream;
      setIsConnecting(false);
      setIsCameraRunning(true);
      setCameraError(null);

      const video = videoRef.current;
      if (video) {
        video.srcObject = stream;
        try {
          await video.play();
        } catch {
          video.onloadedmetadata = () => {
            video.play().catch(() => {});
          };
        }
      }
    } catch (err) {
      setIsConnecting(false);
      setIsCameraRunning(false);
      setCameraError(formatCameraErrorMessage(err));
    }
  };

  const handleToggleCamera = () => {
    if (isCameraRunning) {
      stopStream();
    } else {
      startCamera();
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    stopStream();
    setCameraError(null);
    setIsConnecting(false);
    setIsMediaUploaded(true);
    setIsCameraRunning(true);

    const url = URL.createObjectURL(file);
    objectUrlRef.current = url;

    if (videoRef.current) {
      videoRef.current.srcObject = null;
      videoRef.current.src = url;
      videoRef.current.loop = true;
      videoRef.current.play().catch(() => {});
    }
    e.target.value = '';
  };

  const handleSaveAssessment = () => {
    const video = videoRef.current;
    const overlayCanvas = canvasRef.current;
    if (!video || !isCameraRunning || video.videoWidth === 0) return;

    const snap = document.createElement('canvas');
    const targetW = 640;
    const targetH = Math.round((video.videoHeight / video.videoWidth) * targetW) || 360;
    snap.width = targetW;
    snap.height = targetH;
    const sctx = snap.getContext('2d');
    if (sctx) {
      sctx.drawImage(video, 0, 0, targetW, targetH);
      if (overlayCanvas && overlayCanvas.width > 0 && overlayCanvas.height > 0) {
        sctx.drawImage(overlayCanvas, 0, 0, targetW, targetH);
      }
    }

    const items = [...latestDetectionsRef.current];
    const record: SavedAssessment = {
      id: `${Date.now()}`,
      timestamp: new Date().toLocaleString(),
      overallSeverity: computeOverallSeverity(items),
      detectionCount: items.length,
      snapshotDataUrl: snap.toDataURL('image/jpeg', 0.82),
      items,
    };

    setSavedAssessments((prev) => [record, ...prev]);
    setScanSavedFeedback(true);
    window.setTimeout(() => setScanSavedFeedback(false), 1800);
  };

  // Stop webcam when navigating away from camera page
  useEffect(() => {
    if (screen !== 'camera') {
      stopStream();
    }
  }, [screen]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopStream();
    };
  }, []);

  // Live Object Detection Loop when on Camera screen
  useEffect(() => {
    if (screen !== 'camera' || !isCameraRunning) {
      return;
    }

    let isSubscribed = true;

    async function detect() {
      const video = videoRef.current;
      const canvas = canvasRef.current;

      if (video && canvas && video.readyState >= 2 && video.videoWidth > 0) {
        const rect = video.getBoundingClientRect();
        if (canvas.width !== rect.width || canvas.height !== rect.height) {
          canvas.width = rect.width;
          canvas.height = rect.height;
        }

        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, canvas.width, canvas.height);

          if (modelRef.current) {
            try {
              const predictions = await modelRef.current.detect(video, 10, 0.5);
              if (isSubscribed) {
                const scaleX = rect.width / video.videoWidth;
                const scaleY = rect.height / video.videoHeight;

                const summaries: DetectedObjectSummary[] = [];

                predictions.forEach((p) => {
                  const x = p.bbox[0] * scaleX;
                  const y = p.bbox[1] * scaleY;
                  const w = p.bbox[2] * scaleX;
                  const h = p.bbox[3] * scaleY;

                  const profile = getHazardProfile(p.class);
                  const confidence = Math.round(p.score * 100);
                  const boxColor = getRiskColor(profile.severity, darkMode);

                  summaries.push({
                    label: profile.title,
                    confidence,
                    severity: profile.severity,
                    tag: profile.tag,
                    risks: profile.risks,
                  });

                  // Crisp Risk-Colored Bounding Box (No opaque tint over natural camera video)
                  ctx.strokeStyle = boxColor;
                  ctx.lineWidth = 2.5;
                  ctx.strokeRect(x, y, w, h);

                  // Corner accent brackets
                  const corner = Math.min(20, Math.min(w / 3, h / 3));
                  ctx.lineWidth = 4;
                  ctx.beginPath();
                  ctx.moveTo(x, y + corner); ctx.lineTo(x, y); ctx.lineTo(x + corner, y);
                  ctx.moveTo(x + w - corner, y); ctx.lineTo(x + w, y); ctx.lineTo(x + w, y + corner);
                  ctx.moveTo(x, y + h - corner); ctx.lineTo(x, y + h); ctx.lineTo(x + corner, y + h);
                  ctx.moveTo(x + w - corner, y + h); ctx.lineTo(x + w, y + h); ctx.lineTo(x + w, y + h - corner);
                  ctx.stroke();

                  // Label + [F]/[E] Hazard Tag + Risk list directly below the box
                  const titleText = `[${profile.tag}] ${profile.title} (${confidence}%)`;
                  ctx.font = 'bold 12px "JetBrains Mono", monospace';
                  const titleMetrics = ctx.measureText(titleText);

                  ctx.font = '11px "JetBrains Mono", monospace';
                  const riskLines = ['risks:', ...profile.risks.map((r) => `• ${r}`)];

                  let maxLineWidth = titleMetrics.width;
                  riskLines.forEach((line) => {
                    const m = ctx.measureText(line);
                    if (m.width > maxLineWidth) maxLineWidth = m.width;
                  });

                  const paddingX = 10;
                  const paddingY = 8;
                  const lineHeight = 16;
                  const cardWidth = maxLineWidth + paddingX * 2 + 12;
                  const cardHeight = paddingY * 2 + 24 + riskLines.length * lineHeight;

                  let cardX = Math.max(4, x);
                  if (cardX + cardWidth > canvas.width) {
                    cardX = Math.max(4, canvas.width - cardWidth - 8);
                  }
                  let cardY = y + h + 6;
                  if (cardY + cardHeight > canvas.height) {
                    cardY = Math.max(4, y - cardHeight - 6);
                  }

                  ctx.save();
                  ctx.fillStyle = 'rgba(29, 34, 36, 0.92)';
                  ctx.fillRect(cardX, cardY, cardWidth, cardHeight);

                  ctx.strokeStyle = boxColor;
                  ctx.lineWidth = 1.6;
                  ctx.strokeRect(cardX, cardY, cardWidth, cardHeight);

                  // Colored header band matching Legend risk level
                  ctx.fillStyle = boxColor;
                  ctx.fillRect(cardX, cardY, cardWidth, 24);

                  ctx.fillStyle = '#1D2224';
                  ctx.font = 'bold 12px "JetBrains Mono", monospace';
                  ctx.fillText(titleText, cardX + paddingX, cardY + 16);

                  let textY = cardY + 24 + 16;
                  riskLines.forEach((line, idx) => {
                    if (idx === 0) {
                      ctx.fillStyle = '#CDC6B4';
                      ctx.font = 'bold 11px "JetBrains Mono", monospace';
                    } else {
                      ctx.fillStyle = '#F7F1E2';
                      ctx.font = '11px "JetBrains Mono", monospace';
                    }
                    ctx.fillText(line, cardX + paddingX, textY);
                    textY += lineHeight;
                  });
                  ctx.restore();
                });

                latestDetectionsRef.current = summaries;
                setDetectionCount(summaries.length);
              }
            } catch {
              // Skip busy frame
            }
          }
        }
      }

      if (isSubscribed) {
        animationFrameId.current = requestAnimationFrame(detect);
      }
    }

    animationFrameId.current = requestAnimationFrame(detect);
    return () => {
      isSubscribed = false;
      if (animationFrameId.current) cancelAnimationFrame(animationFrameId.current);
    };
  }, [screen, isCameraRunning, darkMode]);

  // =========================================================================
  // HOME SCREEN (MainPage.createHome)
  // =========================================================================
  if (screen === 'home') {
    return (
      <div className="relative min-h-screen w-full overflow-hidden flex flex-col justify-between px-6 py-5 md:px-8 md:py-6 select-none">
        <MenuBackground darkMode={darkMode} />

        {/* Top-Right Eyebrow */}
        <div className="relative z-10 flex justify-end">
          <span className="text-[11px] font-bold tracking-[0.14em] uppercase text-[var(--sagip-muted)]">
            SMART INCIDENT PREVENTION
          </span>
        </div>

        {/* Centered ContrastPanel */}
        <main className="relative z-10 flex-1 flex items-center justify-center py-6">
          <div
            className="w-full max-w-[440px] rounded-[18px] px-8 py-7 border shadow-2xl backdrop-blur-sm transition-colors duration-200"
            style={{
              backgroundColor: 'var(--sagip-contrast-card)',
              borderColor: 'var(--sagip-border)',
            }}
          >
            <div className="text-center mb-8">
              <h1 className="text-[54px] leading-none font-extrabold tracking-tight text-[var(--sagip-text)]">
                SAGIP
              </h1>
              <p className="mt-2 text-[12px] tracking-[0.12em] uppercase text-[var(--sagip-muted)]">
                SMART GUARD FOR INCIDENT PREVENTION
              </p>
            </div>

            <div className="flex flex-col gap-2.5">
              <HomeMenuButton
                title="START"
                detail="Scan a space for hazards"
                onClick={() => setScreen('camera')}
              />
              <HomeMenuButton
                title="STORAGE"
                detail={`View saved assessments (${savedAssessments.length})`}
                onClick={() => setScreen('storage')}
              />
              <HomeMenuButton
                title={`THEME  ·  ${darkMode ? 'DARK' : 'LIGHT'}`}
                detail="Switch the appearance"
                onClick={() => setDarkMode((prev) => !prev)}
              />
              <HomeMenuButton
                title="MANUAL"
                detail="Learn how to use SAGIP"
                onClick={() => setScreen('manual')}
              />
            </div>
          </div>
        </main>

        {/* Bottom Footer */}
        <footer className="relative z-10 text-center">
          <span className="text-[10px] font-bold tracking-[0.16em] uppercase text-[var(--sagip-text)]">
            SAGIP  •  FIRE &amp; EARTHQUAKE PREPAREDNESS
          </span>
        </footer>
      </div>
    );
  }

  // =========================================================================
  // CAMERA SCREEN (CameraPage.java + Live Color Camera + YOLO Boxes)
  // =========================================================================
  if (screen === 'camera') {
    const statusColor = isCameraRunning
      ? 'var(--sagip-risk-low)'
      : 'var(--sagip-muted)';

    return (
      <div className="min-h-screen w-full flex flex-col bg-[var(--sagip-bg)] text-[var(--sagip-text)] transition-colors duration-200">
        <input
          ref={fileInputRef}
          type="file"
          accept="video/*,image/*"
          onChange={handleFileUpload}
          className="hidden"
        />

        {/* Top Header */}
        <PageHeader
          title="SAGIP"
          subtitle="Camera assessment"
          onBack={() => setScreen('home')}
          extraAction={
            <button
              onClick={() => setDarkMode((prev) => !prev)}
              className="px-4 py-2.5 text-xs font-bold tracking-wide uppercase border border-[var(--sagip-border)] bg-[var(--sagip-surface)] text-[var(--sagip-text)] hover:bg-[var(--sagip-hover)] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              {darkMode ? 'LIGHT MODE' : 'DARK MODE'}
            </button>
          }
        />

        {/* Two-Column Content: Camera Card (4fr) + Legend Card (360-380px) */}
        <main className="flex-1 px-5 py-5 md:px-7 md:py-6 grid grid-cols-1 lg:grid-cols-[1fr_360px] xl:grid-cols-[1fr_380px] gap-5 items-stretch">
          {/* Left Column: Camera Card */}
          <section className="flex flex-col justify-between gap-3.5 p-5 bg-[var(--sagip-surface)] border border-[var(--sagip-border)]">
            {/* Card Header */}
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-[12px] font-bold tracking-[0.08em] uppercase text-[var(--sagip-accent)]">
                  LIVE CAMERA FEED
                </p>
                <h2 className="text-[22px] font-bold leading-tight text-[var(--sagip-text)] mt-0.5">
                  Live hazard detection
                </h2>
                <p className="text-[13px] text-[var(--sagip-muted)] mt-0.5">
                  Environmental risk assessment
                </p>
              </div>
              <div
                className="text-[13px] font-bold tracking-wider whitespace-nowrap shrink-0 pt-1"
                style={{ color: statusColor }}
              >
                {isCameraRunning ? '● LIVE' : '● STANDBY'}
              </div>
            </div>

            {/* Camera Viewport (Natural Full Color Video + Risk Boxes) */}
            <div
              className="relative w-full min-h-[340px] flex-1 overflow-hidden border-2 flex items-center justify-center"
              style={{
                backgroundColor: 'var(--sagip-preview)',
                borderColor: 'var(--sagip-accent)',
              }}
            >
              {/* Live Full-Color Video */}
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className={`w-full h-full object-cover transition-opacity duration-200 ${
                  isCameraRunning && !cameraError && !isConnecting
                    ? 'opacity-100'
                    : 'opacity-0 pointer-events-none absolute inset-0'
                }`}
              />

              {/* Bounding Box & Risk List Overlay Canvas */}
              <canvas
                ref={canvasRef}
                className="absolute inset-0 w-full h-full pointer-events-none z-10"
              />

              {/* CameraPlaceholder Pattern When Standby or Error */}
              {(!isCameraRunning || cameraError || isConnecting) && (
                <div
                  className="relative z-0 w-full h-full flex flex-col items-center justify-center p-6 text-center"
                  style={{
                    backgroundImage: `repeating-linear-gradient(-45deg, transparent, transparent 26px, ${
                      darkMode ? 'rgba(205, 198, 180, 0.08)' : 'rgba(104, 91, 73, 0.09)'
                    } 26px, ${
                      darkMode ? 'rgba(205, 198, 180, 0.08)' : 'rgba(104, 91, 73, 0.09)'
                    } 27.5px)`,
                  }}
                >
                  <div className="mb-4 text-[var(--sagip-muted)]">
                    <Camera className="w-14 h-14 stroke-[1.75]" />
                  </div>
                  <h3 className="text-[20px] font-bold tracking-wide text-[var(--sagip-muted)]">
                    {isConnecting ? 'CONNECTING CAMERA...' : 'CAMERA PREVIEW'}
                  </h3>
                  <p className="text-[14px] text-[var(--sagip-text)] max-w-md mt-2">
                    {cameraError
                      ? cameraError
                      : 'Live camera view and hazard boxes will appear here.'}
                  </p>
                </div>
              )}
            </div>

            {/* Camera Card Footer */}
            <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
              <div className="font-mono tabular-nums text-[14px] font-bold tracking-wide text-[var(--sagip-text)]">
                DETECTIONS {String(detectionCount).padStart(2, '0')}
                {isMediaUploaded ? ' · MEDIA' : ''}
              </div>

              <div className="flex flex-wrap items-center gap-2.5">
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="px-4 py-2.5 text-[12px] font-bold tracking-wide uppercase border border-[var(--sagip-border)] bg-[var(--sagip-bg)] text-[var(--sagip-text)] hover:bg-[var(--sagip-hover)] transition-colors flex items-center gap-2 whitespace-nowrap shrink-0 cursor-pointer"
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>LOAD VIDEO / PHOTO</span>
                </button>

                {isCameraRunning && (
                  <button
                    onClick={handleSaveAssessment}
                    className="px-4 py-2.5 text-[12px] font-bold tracking-wide uppercase border border-[var(--sagip-accent)] bg-[var(--sagip-bg)] text-[var(--sagip-text)] hover:bg-[var(--sagip-hover)] transition-colors flex items-center gap-2 whitespace-nowrap shrink-0 cursor-pointer"
                  >
                    <BookmarkCheck className="w-3.5 h-3.5" />
                    <span>{scanSavedFeedback ? 'SAVED TO STORAGE' : 'SAVE SCAN'}</span>
                  </button>
                )}

                <button
                  onClick={handleToggleCamera}
                  className="px-4 py-2.5 text-[13px] font-bold tracking-wide uppercase bg-[var(--sagip-surface)] text-[var(--sagip-text)] hover:bg-[var(--sagip-hover)] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                  style={{
                    border: `1px solid ${
                      isCameraRunning
                        ? 'var(--sagip-risk-critical)'
                        : 'var(--sagip-accent)'
                    }`,
                  }}
                >
                  {isCameraRunning ? 'STOP CAMERA' : 'START CAMERA'}
                </button>
              </div>
            </div>
          </section>

          {/* Right Column: Legend Card */}
          <aside className="flex flex-col p-5 bg-[var(--sagip-surface)] border border-[var(--sagip-border)]">
            <div className="mb-3">
              <p className="text-[12px] font-bold tracking-[0.08em] uppercase text-[var(--sagip-accent)]">
                LEGEND
              </p>
              <p className="text-[13px] text-[var(--sagip-muted)] mt-1">
                How to read the boxes on the camera feed
              </p>
            </div>

            <div className="flex-1 overflow-y-auto pr-1 space-y-5">
              <div>
                <h3 className="text-[13px] font-bold tracking-wide uppercase text-[var(--sagip-muted)] pt-2 pb-1">
                  RISK LEVEL (BOX COLOR)
                </h3>
                <div className="space-y-3 mt-1">
                  <LegendRiskItem
                    color={getRiskColor('CRITICAL', darkMode)}
                    name="Critical"
                    description="Immediate danger. Fix or remove this hazard first."
                  />
                  <LegendRiskItem
                    color={getRiskColor('HIGH', darkMode)}
                    name="High"
                    description="Serious hazard likely to cause injury or damage. Address as soon as possible."
                  />
                  <LegendRiskItem
                    color={getRiskColor('MEDIUM', darkMode)}
                    name="Medium"
                    description="Moderate risk. Schedule a fix and keep monitoring."
                  />
                  <LegendRiskItem
                    color={getRiskColor('LOW', darkMode)}
                    name="Low"
                    description="Minor risk. Keep it in mind during routine safety checks."
                  />
                </div>
              </div>

              <div>
                <h3 className="text-[13px] font-bold tracking-wide uppercase text-[var(--sagip-muted)] pt-1 pb-1">
                  HAZARD TYPE (LABEL TAG)
                </h3>
                <div className="space-y-3 mt-1">
                  <LegendTagItem
                    glyph="F"
                    name="Fire hazard"
                    description="Objects that can start or spread a fire, such as an unattended iron, oven, or battery device."
                  />
                  <LegendTagItem
                    glyph="E"
                    name="Earthquake hazard"
                    description="Objects that can fall or topple during shaking, such as an unsecured shelf or heavy items on high shelves."
                  />
                </div>
              </div>

              <p className="text-[12px] leading-relaxed text-[var(--sagip-muted)] pt-2 border-t border-[var(--sagip-border)]">
                Each bounding box displays the hazard name, tag, and detection confidence, with specific earthquake, flood, and fire risks listed directly below the box.
              </p>
            </div>
          </aside>
        </main>

        <PageFooter />
      </div>
    );
  }

  // =========================================================================
  // STORAGE SCREEN (MainPage.createStoragePage)
  // =========================================================================
  if (screen === 'storage') {
    return (
      <div className="min-h-screen w-full flex flex-col bg-[var(--sagip-bg)] text-[var(--sagip-text)] transition-colors duration-200">
        <PageHeader
          title="STORAGE"
          subtitle="Saved assessments"
          onBack={() => setScreen('home')}
          extraAction={
            savedAssessments.length > 0 ? (
              <button
                onClick={() => setSavedAssessments([])}
                className="px-4 py-2.5 text-xs font-bold tracking-wide uppercase border border-[var(--sagip-risk-critical)] bg-[var(--sagip-surface)] text-[var(--sagip-text)] hover:bg-[var(--sagip-hover)] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                CLEAR ALL
              </button>
            ) : undefined
          }
        />

        <main className="flex-1 p-6 md:p-8">
          {savedAssessments.length === 0 ? (
            <div className="h-full min-h-[420px] flex flex-col items-center justify-center text-center">
              <div
                className="w-12 h-12 mb-3 flex items-center justify-center border-2 text-2xl font-bold"
                style={{ color: 'var(--sagip-accent)', borderColor: 'var(--sagip-accent)' }}
              >
                ▣
              </div>
              <h2 className="text-[17px] font-bold text-[var(--sagip-text)]">
                No saved assessments yet.
              </h2>
              <p className="text-[12px] text-[var(--sagip-muted)] mt-1.5 mb-6">
                Completed scans will appear here.
              </p>
              <button
                onClick={() => setScreen('camera')}
                className="px-5 py-2.5 text-[12px] font-bold tracking-wide uppercase border border-[var(--sagip-accent)] bg-[var(--sagip-surface)] text-[var(--sagip-text)] hover:bg-[var(--sagip-hover)] transition-colors cursor-pointer"
              >
                START NEW SCAN
              </button>
            </div>
          ) : (
            <div className="max-w-5xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-5">
              {savedAssessments.map((item) => {
                const riskColor = getRiskColor(item.overallSeverity, darkMode);
                return (
                  <article
                    key={item.id}
                    className="flex flex-col bg-[var(--sagip-surface)] border border-[var(--sagip-border)] overflow-hidden"
                  >
                    <div className="relative aspect-video bg-[var(--sagip-preview)] border-b border-[var(--sagip-border)]">
                      <img
                        src={item.snapshotDataUrl}
                        alt={`Scan saved on ${item.timestamp}`}
                        className="w-full h-full object-cover"
                      />
                    </div>
                    <div className="p-4 flex-1 flex flex-col justify-between gap-3">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div
                            className="text-xs font-bold tracking-wider uppercase"
                            style={{ color: riskColor }}
                          >
                            {item.overallSeverity} RISK · {item.detectionCount} DETECTIONS
                          </div>
                          <div className="text-xs font-mono tabular-nums text-[var(--sagip-muted)] mt-0.5">
                            {item.timestamp}
                          </div>
                        </div>
                        <button
                          onClick={() =>
                            setSavedAssessments((prev) => prev.filter((x) => x.id !== item.id))
                          }
                          title="Delete assessment"
                          className="p-2 text-[var(--sagip-muted)] hover:text-[var(--sagip-risk-critical)] transition-colors cursor-pointer"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>

                      {item.items.length > 0 && (
                        <ul className="space-y-1 text-xs text-[var(--sagip-text)] border-t border-[var(--sagip-border)] pt-2.5">
                          {item.items.slice(0, 4).map((obj, idx) => (
                            <li key={idx} className="flex items-center justify-between gap-2">
                              <span className="font-medium">
                                [{obj.tag}] {obj.label}
                              </span>
                              <span className="font-mono tabular-nums text-[var(--sagip-muted)]">
                                {obj.confidence}%
                              </span>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </main>

        <PageFooter />
      </div>
    );
  }

  // =========================================================================
  // MANUAL SCREEN (MainPage.createManualPage)
  // =========================================================================
  return (
    <div className="min-h-screen w-full flex flex-col bg-[var(--sagip-bg)] text-[var(--sagip-text)] transition-colors duration-200">
      <PageHeader title="MANUAL" subtitle="Quick guide" onBack={() => setScreen('home')} />

      <main className="flex-1 px-6 py-9 md:px-14 flex flex-col items-center">
        <div className="w-full max-w-[760px] space-y-4">
          <GuideItem
            number="1"
            title="Start"
            detail="Open a new assessment and point the camera at the space."
          />
          <GuideItem
            number="2"
            title="Review"
            detail="Check detected fire, flood, or earthquake hazards, box colors, and risks listed below each object."
          />
          <GuideItem
            number="3"
            title="Respond"
            detail="Follow the suggested corrective actions and click Save Scan to store the assessment."
          />
        </div>
      </main>

      <PageFooter />
    </div>
  );
}

// ===========================================================================
// Reusable UI Components Matching MainPage & CameraPage Layout Contracts
// ===========================================================================

interface HomeMenuButtonProps {
  title: string;
  detail: string;
  onClick: () => void;
}

const HomeMenuButton: React.FC<HomeMenuButtonProps> = ({ title, detail, onClick }) => (
  <button
    onClick={onClick}
    className="w-full py-2.5 px-5 text-center border transition-colors duration-150 cursor-pointer bg-[var(--sagip-btn-bg)] border-[var(--sagip-btn-border)] hover:bg-[var(--sagip-hover)]"
  >
    <div className="text-[16px] font-bold tracking-wide text-[var(--sagip-text)] leading-snug">
      {title}
    </div>
    <div className="text-[11px] text-[var(--sagip-muted)] mt-0.5">{detail}</div>
  </button>
);

interface PageHeaderProps {
  title: string;
  subtitle: string;
  onBack: () => void;
  extraAction?: React.ReactNode;
}

const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, onBack, extraAction }) => (
  <header className="px-6 py-4 bg-[var(--sagip-surface)] border-b border-[var(--sagip-border)] flex items-center justify-between gap-6">
    <div>
      <h1 className="text-[20px] font-bold leading-tight text-[var(--sagip-text)] whitespace-nowrap">
        {title}
      </h1>
      <p className="text-[12px] text-[var(--sagip-muted)] mt-0.5 whitespace-nowrap">
        {subtitle}
      </p>
    </div>

    <div className="flex items-center gap-2.5 shrink-0">
      {extraAction}
      <button
        onClick={onBack}
        className="px-4 py-2.5 text-[12px] font-bold tracking-wide uppercase bg-[var(--sagip-surface)] text-[var(--sagip-text)] border border-[var(--sagip-accent)] hover:bg-[var(--sagip-hover)] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
      >
        BACK TO MENU
      </button>
    </div>
  </header>
);

const PageFooter: React.FC = () => (
  <footer className="px-6 py-2.5 bg-[var(--sagip-surface)] border-t border-[var(--sagip-border)]">
    <span className="text-[11px] font-bold tracking-[0.12em] uppercase text-[var(--sagip-muted)]">
      SAGIP  •  FIRE &amp; EARTHQUAKE PREPAREDNESS
    </span>
  </footer>
);

const GuideItem: React.FC<{ number: string; title: string; detail: string }> = ({
  number,
  title,
  detail,
}) => (
  <div className="flex items-center gap-4 px-5 py-4 bg-[var(--sagip-surface)] border border-[var(--sagip-border)]">
    <div
      className="w-11 h-11 shrink-0 flex items-center justify-center text-[24px] font-bold"
      style={{ color: 'var(--sagip-accent)' }}
    >
      {number}
    </div>
    <div>
      <h2 className="text-[15px] font-bold text-[var(--sagip-text)]">{title}</h2>
      <p className="text-[13px] text-[var(--sagip-muted)] mt-1 leading-relaxed">{detail}</p>
    </div>
  </div>
);

const LegendRiskItem: React.FC<{ color: string; name: string; description: string }> = ({
  color,
  name,
  description,
}) => (
  <div className="flex items-start gap-3">
    <div
      className="w-[30px] h-[30px] shrink-0 rounded-[4px] mt-0.5"
      style={{
        border: `2.5px solid ${color}`,
        backgroundColor: `${color}26`,
      }}
    />
    <div>
      <div className="text-[15px] font-bold text-[var(--sagip-text)] leading-tight">{name}</div>
      <p className="text-[13px] text-[var(--sagip-muted)] mt-0.5 leading-snug">{description}</p>
    </div>
  </div>
);

const LegendTagItem: React.FC<{ glyph: string; name: string; description: string }> = ({
  glyph,
  name,
  description,
}) => (
  <div className="flex items-start gap-3">
    <div className="w-[30px] h-[30px] shrink-0 rounded-full mt-0.5 flex items-center justify-center border-2 border-[var(--sagip-muted)] text-[14px] font-bold text-[var(--sagip-text)]">
      {glyph}
    </div>
    <div>
      <div className="text-[15px] font-bold text-[var(--sagip-text)] leading-tight">{name}</div>
      <p className="text-[13px] text-[var(--sagip-muted)] mt-0.5 leading-snug">{description}</p>
    </div>
  </div>
);
