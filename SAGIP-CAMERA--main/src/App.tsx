/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from 'react';
import * as cocoSsd from '@tensorflow-models/coco-ssd';
import * as tf from '@tensorflow/tfjs';
import { VideoOff, RotateCcw, Upload, Camera } from 'lucide-react';

interface HazardRiskProfile {
  title: string;
  risks: string[];
}

/**
 * Maps detected objects / sites to specific hazard risks:
 * - Earthquake vulnerability (toppling, projectile, egress blockage)
 * - Flood vulnerability (conductive water hazard, short circuit, uneven/buoyant obstacle)
 * - Fire vulnerability (combustible fuel load, ignition source, thermal runaway)
 */
function getHazardRiskProfile(rawClass: string): HazardRiskProfile {
  const c = rawClass.toLowerCase();

  if (c.includes('cabinet') || c.includes('refrigerator') || c.includes('bookcase') || c.includes('wardrobe') || c.includes('shelf')) {
    return {
      title: 'Cabinet / Storage Unit',
      risks: [
        'may fall during an earthquake',
        'Uneven surface that may cause floods/drainage block',
        'combustible objects & interior fuel load'
      ]
    };
  }

  if (c.includes('chair') || c.includes('couch') || c.includes('sofa') || c.includes('bed') || c.includes('table') || c.includes('dining table')) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      risks: [
        'may slide and block evacuation egress during tremor',
        'Uneven surface that may caused floods / water trap',
        'combustible materials accelerate smoke toxicity'
      ]
    };
  }

  if (c.includes('laptop') || c.includes('tv') || c.includes('monitor') || c.includes('cell phone') || c.includes('microwave') || c.includes('toaster') || c.includes('oven')) {
    return {
      title: rawClass.toUpperCase(),
      risks: [
        'unanchored unit prone to projectile fall in tremor',
        'severe electrocution risk if submerged during flood',
        'lithium runaway / thermal combustion ignition source'
      ]
    };
  }

  if (c.includes('bottle') || c.includes('cup') || c.includes('wine glass') || c.includes('glass') || c.includes('vase') || c.includes('bowl')) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      risks: [
        'shatter risk producing glass debris during earthquake',
        'liquid vessel spill risk contaminating floodwater',
        'flammable solvent or aerosol rupture under radiant heat'
      ]
    };
  }

  if (c.includes('knife') || c.includes('scissors') || c.includes('fork')) {
    return {
      title: 'Sharp Object / Hazard',
      risks: [
        'loose implement capable of sliding off edges in shaking',
        'submerged puncture hazard in murky floodwater',
        'corrosive or thermal conduction risk near fire'
      ]
    };
  }

  if (c.includes('book') || c.includes('backpack') || c.includes('suitcase') || c.includes('handbag')) {
    return {
      title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
      risks: [
        'loose object that may tumble and obstruct exit path',
        'absorbent mass waterlogged in flood conditions',
        'dense combustible paper/fabric fuel load'
      ]
    };
  }

  if (c.includes('person')) {
    return {
      title: 'Occupant / Person',
      risks: [
        'vulnerable to overhead falling debris in tremor',
        'slip and conductive electrical current risk in flood',
        'smoke inhalation and thermal radiant burn hazard'
      ]
    };
  }

  // Fallback profile for other physical objects
  return {
    title: rawClass.charAt(0).toUpperCase() + rawClass.slice(1),
    risks: [
      'may shift or fall during an earthquake',
      'Uneven surface that may caused floods',
      'combustible objects in fire zone'
    ]
  };
}

function formatCameraErrorMessage(err: unknown): string {
  const error = err as { name?: string; message?: string };
  const name = error?.name || '';
  const msg = error?.message || '';

  if (name === 'NotAllowedError' || name === 'PermissionDeniedError' || msg.toLowerCase().includes('permission')) {
    return 'Camera permission was blocked. Allow camera access in your browser, then click Retry Camera.';
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError' || msg.toLowerCase().includes('not found')) {
    return 'No physical webcam detected on this machine. Connect a camera or load a video/photo file.';
  }
  if (name === 'NotReadableError' || name === 'TrackStartError' || msg.toLowerCase().includes('in use')) {
    return 'Camera is in use by another app (Zoom, Teams, or desktop app). Close it and click Retry Camera.';
  }
  return msg || 'Unable to open camera stream. Check browser camera permissions.';
}

export default function App() {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const activeStreamRef = useRef<MediaStream | null>(null);
  const objectUrlRef = useRef<string | null>(null);
  const modelRef = useRef<cocoSsd.ObjectDetection | null>(null);
  const animationFrameId = useRef<number | null>(null);

  const [isCameraActive, setIsCameraActive] = useState<boolean>(true);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const [cameraError, setCameraError] = useState<string | null>(null);

  const stopStream = () => {
    if (activeStreamRef.current) {
      activeStreamRef.current.getTracks().forEach(t => t.stop());
      activeStreamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.pause();
      videoRef.current.srcObject = null;
    }
    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }
  };

  const startCamera = async () => {
    setIsConnecting(true);
    setCameraError(null);
    setIsCameraActive(true);
    stopStream();

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setIsConnecting(false);
      setCameraError('Camera API is not supported in this browser environment.');
      return;
    }

    try {
      let stream: MediaStream;
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 1920, min: 1280 }, height: { ideal: 1080, min: 720 }, facingMode: 'user' },
          audio: false
        });
      } catch {
        // Fallback to unconstrained video
        stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: false
        });
      }

      activeStreamRef.current = stream;
      setIsConnecting(false);
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
      setCameraError(formatCameraErrorMessage(err));
    }
  };

  // Load High-Accuracy YOLO / COCO-SSD Model (Full mobilenet_v2)
  useEffect(() => {
    let isMounted = true;
    async function loadModel() {
      try {
        await tf.ready();
        // Load high-accuracy full mobilenet_v2 model for superior precision
        const loaded = await cocoSsd.load({ base: 'mobilenet_v2' });
        if (isMounted) {
          modelRef.current = loaded;
        }
      } catch {
        // Fallback to lite model if high-accuracy load fails
        try {
          const fallback = await cocoSsd.load({ base: 'lite_mobilenet_v2' });
          if (isMounted) {
            modelRef.current = fallback;
          }
        } catch {
          // Model load error
        }
      }
    }
    loadModel();
    return () => {
      isMounted = false;
    };
  }, []);

  // Initialize camera immediately
  useEffect(() => {
    startCamera();
    return () => {
      stopStream();
    };
  }, []);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    stopStream();
    setCameraError(null);
    setIsConnecting(false);
    setIsCameraActive(true);

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

  // Live High-Accuracy YOLO detection loop
  useEffect(() => {
    let isSubscribed = true;

    async function detect() {
      const video = videoRef.current;
      const canvas = canvasRef.current;

      if (video && canvas && video.readyState >= 2 && video.videoWidth > 0) {
        // Match canvas dimensions to the exact display client width & height
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
              // Higher accuracy threshold (0.50 score filter, up to 10 distinct objects)
              const predictions = await modelRef.current.detect(video, 10, 0.50);
              if (isSubscribed) {
                // Compute scale factors between native video resolution and displayed element size
                const scaleX = rect.width / video.videoWidth;
                const scaleY = rect.height / video.videoHeight;

                predictions.forEach(p => {
                  const x = p.bbox[0] * scaleX;
                  const y = p.bbox[1] * scaleY;
                  const w = p.bbox[2] * scaleX;
                  const h = p.bbox[3] * scaleY;

                  // High-contrast crisp border
                  ctx.strokeStyle = '#FFFFFF';
                  ctx.lineWidth = 2;
                  ctx.strokeRect(x, y, w, h);

                  // 3. Precision corner reticle accents
                  const corner = Math.min(22, Math.min(w / 3, h / 3));
                  ctx.lineWidth = 3.5;
                  ctx.beginPath();
                  // Top-left
                  ctx.moveTo(x, y + corner); ctx.lineTo(x, y); ctx.lineTo(x + corner, y);
                  // Top-right
                  ctx.moveTo(x + w - corner, y); ctx.lineTo(x + w, y); ctx.lineTo(x + w, y + corner);
                  // Bottom-left
                  ctx.moveTo(x, y + h - corner); ctx.lineTo(x, y + h); ctx.lineTo(x + corner, y + h);
                  // Bottom-right
                  ctx.moveTo(x + w - corner, y + h); ctx.lineTo(x + w, y + h); ctx.lineTo(x + w, y + h - corner);
                  ctx.stroke();

                  // 4. Retrieve Hazard Risk Profile for this detected object/site
                  const profile = getHazardRiskProfile(p.class);
                  const confidence = Math.round(p.score * 100);

                  // 5. Header: Object Name
                  const titleText = `${profile.title} (${confidence}%)`;
                  ctx.font = 'bold 13px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
                  const titleMetrics = ctx.measureText(titleText);

                  // 6. Risks list lines
                  ctx.font = '11px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
                  const riskLines = [
                    'risks:',
                    ...profile.risks.map(r => `• ${r}`)
                  ];

                  let maxLineWidth = titleMetrics.width;
                  riskLines.forEach(line => {
                    const lineMetrics = ctx.measureText(line);
                    if (lineMetrics.width > maxLineWidth) {
                      maxLineWidth = lineMetrics.width;
                    }
                  });

                  // Panel sizing & positioning below the detected box
                  const paddingX = 10;
                  const paddingY = 8;
                  const lineHeight = 16;
                  const cardWidth = maxLineWidth + paddingX * 2 + 10;
                  const cardHeight = paddingY * 2 + 20 + riskLines.length * lineHeight;

                  // Place below box if space permits; else place above box
                  let cardX = x;
                  if (cardX + cardWidth > canvas.width) {
                    cardX = Math.max(4, canvas.width - cardWidth - 8);
                  }
                  let cardY = y + h + 6;
                  if (cardY + cardHeight > canvas.height) {
                    cardY = Math.max(4, y - cardHeight - 6);
                  }

                  // 7. Draw Semi-Opaque Monochromatic Risk Card
                  ctx.save();
                  ctx.fillStyle = 'rgba(9, 9, 11, 0.94)';
                  ctx.fillRect(cardX, cardY, cardWidth, cardHeight);

                  ctx.strokeStyle = '#FFFFFF';
                  ctx.lineWidth = 1.4;
                  ctx.strokeRect(cardX, cardY, cardWidth, cardHeight);

                  // Top Header accent bar
                  ctx.fillStyle = '#FFFFFF';
                  ctx.fillRect(cardX, cardY, cardWidth, 24);

                  // Title text (inverted black on white header)
                  ctx.fillStyle = '#09090B';
                  ctx.font = 'bold 12px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
                  ctx.fillText(titleText, cardX + paddingX, cardY + 16);

                  // Risks label and list (white on dark background)
                  let currentTextY = cardY + 24 + 16;
                  riskLines.forEach((line, idx) => {
                    if (idx === 0) {
                      ctx.fillStyle = '#E4E4E7';
                      ctx.font = 'bold 11px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
                    } else {
                      ctx.fillStyle = '#D4D4D8';
                      ctx.font = '11px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
                    }
                    ctx.fillText(line, cardX + paddingX, currentTextY);
                    currentTextY += lineHeight;
                  });

                  ctx.restore();
                });
              }
            } catch {
              // Skip frame if busy
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
      if (animationFrameId.current) {
        cancelAnimationFrame(animationFrameId.current);
      }
    };
  }, []);

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-black flex items-center justify-center select-none">
      <input
        ref={fileInputRef}
        type="file"
        accept="video/*,image/*"
        onChange={handleFileUpload}
        className="hidden"
      />

      {/* Pure Fullscreen Camera Output */}
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        className={`w-full h-full object-cover transition-opacity duration-200 ${
          isCameraActive && !cameraError && !isConnecting ? 'opacity-100' : 'opacity-0'
        }`}
      />

      {/* Detection Boxes and Risk Lists Overlay Canvas */}
      <canvas
        ref={canvasRef}
        className="absolute inset-0 w-full h-full pointer-events-none z-10"
      />

      {/* Minimal Diagnostic Display Only If Camera Fails or Is Blocked */}
      {cameraError && !isConnecting && (
        <div className="absolute inset-0 z-20 flex flex-col items-center justify-center p-6 text-center bg-black/90 text-white">
          <div className="w-14 h-14 rounded-full border border-zinc-700 bg-zinc-900 flex items-center justify-center mb-4">
            <VideoOff className="w-6 h-6 text-zinc-300" />
          </div>
          <h2 className="text-lg font-bold mb-2">Camera Unavailable</h2>
          <p className="text-xs text-zinc-400 max-w-md mb-6 leading-relaxed">
            {cameraError}
          </p>
          <div className="flex gap-3">
            <button
              onClick={startCamera}
              className="px-5 py-2.5 bg-white text-black text-xs font-bold rounded flex items-center gap-2 hover:bg-zinc-200 transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Retry Camera</span>
            </button>
            <button
              onClick={() => fileInputRef.current?.click()}
              className="px-5 py-2.5 bg-zinc-900 border border-zinc-700 text-white text-xs font-semibold rounded flex items-center gap-2 hover:bg-zinc-800 transition-colors"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>Load Video / Image</span>
            </button>
          </div>
        </div>
      )}

      {/* Subtle Bottom Right Minimal Control (Allows re-triggering camera or loading a file without cluttering UI) */}
      <div className="absolute bottom-4 right-4 z-20 opacity-30 hover:opacity-100 transition-opacity flex items-center gap-2">
        <button
          onClick={startCamera}
          title="Restart Camera"
          className="p-2.5 rounded-full bg-black/60 border border-white/20 text-white hover:bg-white hover:text-black transition-colors"
        >
          <Camera className="w-4 h-4" />
        </button>
        <button
          onClick={() => fileInputRef.current?.click()}
          title="Load Video/Photo"
          className="p-2.5 rounded-full bg-black/60 border border-white/20 text-white hover:bg-white hover:text-black transition-colors"
        >
          <Upload className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
