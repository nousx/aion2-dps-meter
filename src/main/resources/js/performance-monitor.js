// Performance monitoring for debugging
class PerformanceMonitor {
  constructor() {
    this.enabled = false;
    this.stats = {
      fps: 0,
      renderTime: 0,
      pollRate: 0,
      lastPollTime: 0,
      updateCount: 0,
      interpolatorActive: false
    };

    this.frameCount = 0;
    this.lastFpsUpdate = performance.now();
    this.el = null;
  }

  enable() {
    if (this.enabled) return;
    this.enabled = true;
    this.createUI();
    this.startMonitoring();
  }

  disable() {
    this.enabled = false;
    if (this.el) {
      this.el.remove();
      this.el = null;
    }
  }

  toggle() {
    if (this.enabled) {
      this.disable();
    } else {
      this.enable();
    }
  }

  createUI() {
    this.el = document.createElement('div');
    this.el.style.cssText = `
      position: fixed;
      top: 10px;
      right: 10px;
      background: rgba(0, 0, 0, 0.85);
      color: #0f0;
      font-family: 'Courier New', monospace;
      font-size: 11px;
      padding: 8px 12px;
      border-radius: 4px;
      border: 1px solid #0f0;
      z-index: 9999;
      min-width: 200px;
      pointer-events: none;
      line-height: 1.4;
    `;

    document.body.appendChild(this.el);
  }

  startMonitoring() {
    // FPS counter
    const updateFps = () => {
      if (!this.enabled) return;

      this.frameCount++;
      const now = performance.now();
      const elapsed = now - this.lastFpsUpdate;

      if (elapsed >= 1000) {
        this.stats.fps = Math.round((this.frameCount * 1000) / elapsed);
        this.frameCount = 0;
        this.lastFpsUpdate = now;
        this.render();
      }

      requestAnimationFrame(updateFps);
    };

    requestAnimationFrame(updateFps);
  }

  recordPoll(duration) {
    const now = performance.now();
    if (this.stats.lastPollTime > 0) {
      const pollInterval = now - this.stats.lastPollTime;
      this.stats.pollRate = Math.round(1000 / pollInterval);
    }
    this.stats.lastPollTime = now;
    this.stats.renderTime = duration;
    this.stats.updateCount++;
  }

  recordInterpolation(active) {
    this.stats.interpolatorActive = active;
  }

  render() {
    if (!this.el) return;

    const memory = performance.memory ? (performance.memory.usedJSHeapSize / 1048576).toFixed(1) : 'N/A';

    // Use safe DOM methods instead of innerHTML
    this.el.textContent = '';

    const title = document.createElement('div');
    title.style.cssText = 'color: #0ff; font-weight: bold; margin-bottom: 4px;';
    title.textContent = '⚡ Performance Monitor';
    this.el.appendChild(title);

    const lines = [
      `FPS: ${this.stats.fps}`,
      `Poll Rate: ${this.stats.pollRate} Hz`,
      `Render: ${this.stats.renderTime.toFixed(1)}ms`,
      `Updates: ${this.stats.updateCount}`,
      `Memory: ${memory} MB`,
      `Interpolator: ${this.stats.interpolatorActive ? '✓' : '○'}`
    ];

    lines.forEach(text => {
      const line = document.createElement('div');
      line.textContent = text;
      this.el.appendChild(line);
    });

    const hint = document.createElement('div');
    hint.style.cssText = 'margin-top: 4px; color: #888; font-size: 9px;';
    hint.textContent = 'Press Ctrl+Shift+P to toggle';
    this.el.appendChild(hint);
  }
}

// Global instance
window.performanceMonitor = new PerformanceMonitor();

// Hotkey: Ctrl+Shift+P to toggle
document.addEventListener('keydown', (e) => {
  if (e.ctrlKey && e.shiftKey && e.key === 'P') {
    e.preventDefault();
    window.performanceMonitor.toggle();
  }
});
