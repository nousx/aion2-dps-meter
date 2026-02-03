// Global Hotkeys Manager
class HotkeyManager {
  constructor() {
    this.hotkeys = {
      toggle: { ctrl: true, shift: false, alt: false, key: 'T' },
      reset: { ctrl: true, shift: false, alt: false, key: 'R' }
    };

    this.recording = null;
    this.setupUI();
    this.loadHotkeys();
    this.registerGlobalListener();
  }

  setupUI() {
    const toggleEl = document.getElementById('hotkeyToggle');
    const resetEl = document.getElementById('hotkeyReset');

    if (toggleEl) {
      toggleEl.textContent = this.formatHotkey(this.hotkeys.toggle);
      toggleEl.addEventListener('click', () => this.startRecording('toggle', toggleEl));
    }

    if (resetEl) {
      resetEl.textContent = this.formatHotkey(this.hotkeys.reset);
      resetEl.addEventListener('click', () => this.startRecording('reset', resetEl));
    }
  }

  startRecording(action, el) {
    if (this.recording) {
      this.stopRecording();
    }

    this.recording = { action, el };
    el.classList.add('recording');
    el.textContent = 'Press keys...';

    // Tell JavaFX to intercept keys at Scene level
    window.javaBridge?.startHotkeyRecording?.();
  }

  // Called by JavaFX Scene EventFilter when recording
  onNativeKey(keyCode, ctrl, shift, alt) {
    if (!this.recording) return;

    const { action, el } = this.recording;

    // Escape → cancel
    if (keyCode === 'ESCAPE') {
      this.stopRecording();
      return;
    }

    // Modifier-only → show preview
    if (keyCode === 'CONTROL' || keyCode === 'SHIFT' || keyCode === 'ALT' || keyCode === 'META') {
      el.textContent = this.formatHotkey({ ctrl, shift, alt, key: '?' });
      return;
    }

    // Derive label from JavaFX KeyCode name
    let label;
    if (keyCode.length === 1 && keyCode >= 'A' && keyCode <= 'Z') {
      label = keyCode;                          // A–Z
    } else if (/^F\d{1,2}$/.test(keyCode)) {
      label = keyCode;                          // F1–F12
    } else if (/^DIGIT(\d)$/.test(keyCode)) {
      label = keyCode.match(/^DIGIT(\d)$/)[1];  // DIGIT0 → 0
    } else {
      el.textContent = 'Unsupported key';
      return;
    }

    // Record
    const hotkey = { ctrl, shift, alt, key: label };
    this.hotkeys[action] = hotkey;
    el.textContent = this.formatHotkey(hotkey);
    this.saveHotkeys();
    this.stopRecording();
  }

  stopRecording() {
    if (!this.recording) return;

    const { el, action } = this.recording;
    el.classList.remove('recording');
    el.textContent = this.formatHotkey(this.hotkeys[action]);

    // Tell JavaFX to stop intercepting
    window.javaBridge?.stopHotkeyRecording?.();

    this.recording = null;
  }

  formatHotkey(hotkey) {
    if (!hotkey) return '';
    const parts = [];
    if (hotkey.ctrl) parts.push('Ctrl');
    if (hotkey.shift) parts.push('Shift');
    if (hotkey.alt) parts.push('Alt');
    if (hotkey.key) parts.push(hotkey.key);
    return parts.join('+');
  }

  registerGlobalListener() {
    document.addEventListener('keydown', (e) => {
      // Skip if recording
      if (this.recording) return;

      // Skip if typing in input
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

      // Check toggle hotkey
      if (this.matchesHotkey(e, this.hotkeys.toggle)) {
        e.preventDefault();
        this.toggleMeter();
      }

      // Check reset hotkey
      if (this.matchesHotkey(e, this.hotkeys.reset)) {
        e.preventDefault();
        this.resetMeter();
      }
    });
  }

  matchesHotkey(event, hotkey) {
    if (!hotkey) return false;
    return (
      event.ctrlKey === hotkey.ctrl &&
      event.shiftKey === hotkey.shift &&
      event.altKey === hotkey.alt &&
      event.key.toUpperCase() === hotkey.key
    );
  }

  toggleMeter() {
    const meter = document.querySelector('.meter');
    if (meter) {
      meter.style.display = meter.style.display === 'none' ? '' : 'none';
    }
  }

  resetMeter() {
    if (window.dpsApp?.resetAll) {
      window.dpsApp.resetAll({ callBackend: true });
    }
  }

  saveHotkeys() {
    try {
      // Save to settings
      window.javaBridge?.setSetting('hotkeys', JSON.stringify(this.hotkeys));

      // Update native hotkeys immediately
      if (window.javaBridge?.updateHotkey) {
        const toggle = this.hotkeys.toggle;
        if (toggle) {
          window.javaBridge.updateHotkey('toggle', toggle.ctrl, toggle.shift, toggle.alt, toggle.key);
        }

        const reset = this.hotkeys.reset;
        if (reset) {
          window.javaBridge.updateHotkey('reset', reset.ctrl, reset.shift, reset.alt, reset.key);
        }
      }
    } catch (e) {
      console.error('Failed to save hotkeys:', e);
    }
  }

  loadHotkeys() {
    try {
      const saved = window.javaBridge?.getSetting('hotkeys');
      if (saved) {
        this.hotkeys = { ...this.hotkeys, ...JSON.parse(saved) };
        this.setupUI(); // Update UI with loaded values
      }
    } catch (e) {
      console.error('Failed to load hotkeys:', e);
    }
  }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    window.hotkeyManager = new HotkeyManager();
  });
} else {
  window.hotkeyManager = new HotkeyManager();
}
