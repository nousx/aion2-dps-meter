// Smooth number interpolation for buttery animations
class NumberInterpolator {
  constructor() {
    this.values = new Map(); // id -> { current, target, velocity }
    this.lastUpdate = performance.now();
    this.isRunning = false;
  }

  setValue(id, targetValue) {
    const current = this.values.get(id);

    if (!current) {
      // First time seeing this value - start at target (no animation)
      this.values.set(id, {
        current: targetValue,
        target: targetValue,
        velocity: 0
      });
      return targetValue;
    }

    // Update target
    current.target = targetValue;

    // Start animation loop if not running
    if (!this.isRunning) {
      this.startLoop();
    }

    return current.current;
  }

  getValue(id) {
    const val = this.values.get(id);
    return val ? val.current : 0;
  }

  startLoop() {
    if (this.isRunning) return;
    this.isRunning = true;
    this.lastUpdate = performance.now();
    this.animate();
  }

  animate() {
    if (!this.isRunning) return;

    const now = performance.now();
    const deltaTime = Math.min((now - this.lastUpdate) / 1000, 0.1); // Cap at 100ms
    this.lastUpdate = now;

    let hasActiveAnimations = false;

    // Spring physics for smooth interpolation
    const SPRING_STIFFNESS = 8.0;  // Higher = snappier
    const SPRING_DAMPING = 0.7;     // Higher = less bouncy
    const EPSILON = 0.1;            // Stop threshold

    for (const [id, val] of this.values) {
      const diff = val.target - val.current;

      // Skip if already at target
      if (Math.abs(diff) < EPSILON && Math.abs(val.velocity) < EPSILON) {
        val.current = val.target;
        val.velocity = 0;
        continue;
      }

      hasActiveAnimations = true;

      // Spring physics
      const springForce = diff * SPRING_STIFFNESS;
      const dampingForce = val.velocity * SPRING_DAMPING;
      const acceleration = springForce - dampingForce;

      // Update velocity and position
      val.velocity += acceleration * deltaTime;
      val.current += val.velocity * deltaTime;

      // Clamp to prevent overshooting on large jumps
      if (val.velocity > 0 && val.current > val.target) {
        val.current = val.target;
        val.velocity = 0;
      } else if (val.velocity < 0 && val.current < val.target) {
        val.current = val.target;
        val.velocity = 0;
      }
    }

    if (hasActiveAnimations) {
      requestAnimationFrame(() => this.animate());
    } else {
      this.isRunning = false;
    }
  }

  clear() {
    this.values.clear();
    this.isRunning = false;
  }
}

// Global interpolator instance
window.numberInterpolator = new NumberInterpolator();
