import React, { useEffect, useRef } from 'react';

interface MenuBackgroundProps {
  darkMode: boolean;
}

export const MenuBackground: React.FC<MenuBackgroundProps> = ({ darkMode }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let animationFrame = 0;
    let rafId = 0;
    let lastTime = 0;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const rect = canvas.getBoundingClientRect();
      canvas.width = Math.max(1, Math.floor(rect.width * dpr));
      canvas.height = Math.max(1, Math.floor(rect.height * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    resize();
    window.addEventListener('resize', resize);

    const drawHouse = (
      x: number,
      base: number,
      tileWidth: number,
      height: number,
      variation: number
    ) => {
      const houseWidth = (tileWidth * (variation === 2 ? 72 : 80)) / 100;
      const houseHeight = height * (variation === 5 ? 0.15 : 0.18);
      const left = x + (tileWidth - houseWidth) / 2;
      const top = base - houseHeight;

      const wall =
        variation % 2 === 0
          ? darkMode
            ? 'rgb(130, 110, 94)'
            : 'rgb(225, 194, 151)'
          : darkMode
            ? 'rgb(121, 119, 104)'
            : 'rgb(205, 204, 174)';

      ctx.fillStyle = wall;
      ctx.fillRect(left, top + houseHeight / 4, houseWidth, (houseHeight * 3) / 4);

      ctx.beginPath();
      ctx.moveTo(left - 7, top + houseHeight / 3);
      ctx.lineTo(left + houseWidth / 2, top - houseHeight / 5);
      ctx.lineTo(left + houseWidth + 7, top + houseHeight / 3);
      ctx.closePath();
      ctx.fillStyle = darkMode ? 'rgb(73, 55, 51)' : 'rgb(130, 72, 55)';
      ctx.fill();

      const windowSize = Math.max(8, tileWidth / 16);
      ctx.fillStyle = darkMode ? 'rgb(255, 204, 130)' : 'rgb(248, 224, 172)';
      ctx.fillRect(left + houseWidth / 6, top + houseHeight / 2, windowSize, windowSize);
      ctx.fillRect(left + (houseWidth * 4) / 6, top + houseHeight / 2, windowSize, windowSize);

      ctx.fillStyle = darkMode ? 'rgb(72, 52, 43)' : 'rgb(113, 76, 54)';
      ctx.fillRect(
        left + houseWidth / 2 - windowSize / 2,
        base - houseHeight / 2,
        windowSize,
        houseHeight / 2
      );
    };

    const drawOffice = (
      x: number,
      base: number,
      tileWidth: number,
      height: number,
      annex: boolean
    ) => {
      const officeWidth = (tileWidth * (annex ? 82 : 88)) / 100;
      const officeHeight = height * (annex ? 0.22 : 0.25);
      const left = x + (tileWidth - officeWidth) / 2;
      const top = base - officeHeight;

      ctx.fillStyle = darkMode ? 'rgb(64, 82, 91)' : 'rgb(135, 158, 158)';
      ctx.fillRect(left, top, officeWidth, officeHeight);
      ctx.fillStyle = darkMode ? 'rgb(45, 59, 68)' : 'rgb(98, 125, 132)';
      ctx.fillRect(left, top, officeWidth, Math.max(6, height / 60));

      const rows = annex ? 3 : 4;
      const columns = 3;
      const windowWidth = Math.max(8, officeWidth / 9);
      const windowHeight = Math.max(7, officeHeight / 9);
      ctx.fillStyle = darkMode ? 'rgb(255, 202, 132)' : 'rgb(255, 231, 183)';
      for (let row = 0; row < rows; row++) {
        for (let column = 0; column < columns; column++) {
          const windowX = left + officeWidth / 8 + column * (officeWidth / 3);
          const windowY = top + officeHeight / 6 + row * (officeHeight / (rows + 1));
          ctx.fillRect(windowX, windowY, windowWidth, windowHeight);
        }
      }

      ctx.fillStyle = darkMode ? 'rgb(43, 51, 56)' : 'rgb(81, 103, 105)';
      ctx.fillRect(
        left + officeWidth / 2 - windowWidth / 2,
        base - officeHeight / 4,
        windowWidth,
        officeHeight / 4
      );
    };

    const drawSchool = (x: number, base: number, tileWidth: number, height: number) => {
      const schoolWidth = (tileWidth * 94) / 100;
      const schoolHeight = height * 0.21;
      const left = x + (tileWidth - schoolWidth) / 2;
      const top = base - schoolHeight;

      ctx.fillStyle = darkMode ? 'rgb(177, 139, 94)' : 'rgb(232, 198, 139)';
      ctx.fillRect(left, top + schoolHeight / 5, schoolWidth, (schoolHeight * 4) / 5);
      ctx.fillStyle = darkMode ? 'rgb(91, 67, 55)' : 'rgb(154, 87, 60)';
      ctx.fillRect(left - 4, top + schoolHeight / 5 - 5, schoolWidth + 8, 8);

      ctx.fillStyle = darkMode ? 'rgb(239, 226, 198)' : 'rgb(76, 64, 52)';
      const fontSize = Math.max(9, Math.round(height / 85));
      ctx.font = `bold ${fontSize}px 'Plus Jakarta Sans', sans-serif`;
      const sign = 'SCHOOL';
      const signWidth = ctx.measureText(sign).width;
      ctx.fillText(sign, left + (schoolWidth - signWidth) / 2, top + schoolHeight / 2);

      ctx.fillStyle = darkMode ? 'rgb(255, 204, 128)' : 'rgb(255, 232, 180)';
      const windowWidth = Math.max(7, tileWidth / 18);
      const windowY = top + (schoolHeight * 3) / 5;
      ctx.fillRect(left + schoolWidth / 8, windowY, windowWidth, windowWidth);
      ctx.fillRect(left + (schoolWidth * 7) / 8 - windowWidth, windowY, windowWidth, windowWidth);

      ctx.fillStyle = darkMode ? 'rgb(87, 63, 48)' : 'rgb(118, 78, 48)';
      ctx.fillRect(
        left + schoolWidth / 2 - windowWidth,
        base - schoolHeight / 3,
        windowWidth * 2,
        schoolHeight / 3
      );
    };

    const paintCloud = (
      width: number,
      height: number,
      yFraction: number,
      cloudWidth: number,
      cloudIndex: number
    ) => {
      const travel =
        (animationFrame * (0.035 + cloudIndex * 0.008) + cloudIndex * width * 0.39) %
        (width + cloudWidth * 2.0);
      const x = travel - cloudWidth;
      const y = height * yFraction;
      const cloudHeight = Math.max(24, cloudWidth / 5);
      ctx.fillStyle = darkMode ? 'rgba(228, 218, 204, 0.15)' : 'rgba(255, 255, 255, 0.36)';

      const drawEllipse = (ex: number, ey: number, ew: number, eh: number) => {
        ctx.beginPath();
        ctx.ellipse(ex + ew / 2, ey + eh / 2, ew / 2, eh / 2, 0, 0, Math.PI * 2);
        ctx.fill();
      };

      drawEllipse(x, y + cloudHeight * 0.18, cloudWidth * 0.58, cloudHeight * 0.72);
      drawEllipse(x + cloudWidth * 0.2, y, cloudWidth * 0.48, cloudHeight);
      drawEllipse(x + cloudWidth * 0.45, y + cloudHeight * 0.18, cloudWidth * 0.55, cloudHeight * 0.72);
    };

    const renderFrame = (timestamp: number) => {
      if (!prefersReducedMotion) {
        if (!lastTime || timestamp - lastTime >= 42) {
          animationFrame++;
          lastTime = timestamp;
        }
      }

      const rect = canvas.getBoundingClientRect();
      const width = rect.width;
      const height = rect.height;
      const skylineBase = Math.floor(height * 0.77);

      const skyGrad = ctx.createLinearGradient(0, 0, 0, skylineBase);
      skyGrad.addColorStop(0, darkMode ? 'rgb(18, 29, 48)' : 'rgb(112, 166, 194)');
      skyGrad.addColorStop(1, darkMode ? 'rgb(112, 82, 79)' : 'rgb(246, 190, 143)');
      ctx.fillStyle = skyGrad;
      ctx.fillRect(0, 0, width, skylineBase);

      // Sun
      const sunSize = Math.max(48, height / 11);
      const sunX = width * 0.79;
      const sunY = height * 0.19;
      ctx.fillStyle = darkMode ? 'rgba(255, 195, 133, 0.10)' : 'rgba(255, 195, 133, 0.18)';
      ctx.beginPath();
      ctx.ellipse(
        sunX - sunSize * 0.7 + sunSize * 1.2,
        sunY - sunSize * 0.7 + sunSize * 1.2,
        sunSize * 1.2,
        sunSize * 1.2,
        0,
        0,
        Math.PI * 2
      );
      ctx.fill();

      ctx.fillStyle = darkMode ? 'rgb(255, 194, 139)' : 'rgb(255, 231, 183)';
      ctx.beginPath();
      ctx.ellipse(
        sunX + sunSize / 2,
        sunY + sunSize / 2,
        sunSize / 2,
        sunSize / 2,
        0,
        0,
        Math.PI * 2
      );
      ctx.fill();

      // Clouds
      paintCloud(width, height, 0.19, 210, 0);
      paintCloud(width, height, 0.32, 155, 1);
      paintCloud(width, height, 0.13, 180, 2);

      // Scrolling Neighborhood
      const tileWidth = Math.max(145, Math.floor(width / 8));
      const sceneWidth = tileWidth * 6;
      const offset = Math.floor(animationFrame * 0.32) % sceneWidth;
      const firstX = -sceneWidth + offset;
      let firstIndex = -6;
      for (let x = firstX; x < width + sceneWidth; x += tileWidth) {
        const type = ((firstIndex++ % 6) + 6) % 6;
        if (type === 0 || type === 2 || type === 5) {
          drawHouse(x, skylineBase, tileWidth, height, type);
        } else if (type === 1 || type === 4) {
          drawOffice(x, skylineBase, tileWidth, height, type === 4);
        } else {
          drawSchool(x, skylineBase, tileWidth, height);
        }
      }

      // Ground / Road
      const roadGrad = ctx.createLinearGradient(0, skylineBase, 0, height);
      roadGrad.addColorStop(0, darkMode ? 'rgb(43, 48, 57)' : 'rgb(110, 108, 104)');
      roadGrad.addColorStop(1, darkMode ? 'rgb(18, 23, 31)' : 'rgb(65, 68, 70)');
      ctx.fillStyle = roadGrad;
      ctx.fillRect(0, skylineBase, width, height - skylineBase);

      ctx.fillStyle = darkMode ? 'rgba(255, 183, 111, 0.39)' : 'rgba(255, 218, 164, 0.47)';
      ctx.fillRect(0, skylineBase, width, 3);

      // Lane markings
      ctx.fillStyle = darkMode ? 'rgba(187, 198, 207, 0.39)' : 'rgba(231, 224, 210, 0.59)';
      const drawDash = (dx: number, dy: number, dw: number, dh: number) => {
        ctx.beginPath();
        ctx.ellipse(dx + dw / 2, dy + dh / 2, dw / 2, dh / 2, 0, 0, Math.PI * 2);
        ctx.fill();
      };
      drawDash(width * 0.48, skylineBase + height * 0.08, width * 0.04, height * 0.018);
      drawDash(width * 0.48, skylineBase + height * 0.15, width * 0.04, height * 0.018);

      // Moving headlights
      ctx.fillStyle = darkMode ? 'rgba(255, 191, 119, 0.45)' : 'rgba(255, 237, 197, 0.59)';
      const lightTravel = Math.floor(animationFrame * 0.42) % Math.max(1, width + 100);
      drawDash(lightTravel - 50, skylineBase + height * 0.22, 32, 5);
      drawDash(width - lightTravel + 50, skylineBase + height * 0.27, 28, 4);

      if (!prefersReducedMotion) {
        rafId = requestAnimationFrame(renderFrame);
      }
    };

    rafId = requestAnimationFrame(renderFrame);

    return () => {
      window.removeEventListener('resize', resize);
      if (rafId) cancelAnimationFrame(rafId);
    };
  }, [darkMode]);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      className="absolute inset-0 w-full h-full pointer-events-none"
    />
  );
};
