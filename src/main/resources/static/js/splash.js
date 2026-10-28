/**
 * InChef Animated Intro (Splash) Screen & Smooth Home Transition Controller
 * Coordinates seamless intro animation, exit transition, and reliable unmounting.
 */
(function () {
  'use strict';

  const SESSION_KEY = 'inchef_splash_shown';

  function initSplash() {
    const splashEl = document.getElementById('inchef-splash-screen');
    if (!splashEl) return;

    // Detect browser reload or forced play via URL query param (?splash=true / ?splash=play)
    try {
      const navEntries = performance.getEntriesByType('navigation');
      const isReload = navEntries.length > 0 && navEntries[0].type === 'reload';
      const urlParams = new URLSearchParams(window.location.search);
      if (isReload || urlParams.get('splash') === 'true' || urlParams.get('splash') === 'play') {
        sessionStorage.removeItem(SESSION_KEY);
      }
    } catch (e) {}

    // Check if splash has already been viewed in this browser session
    try {
      if (sessionStorage.getItem(SESSION_KEY) === 'true') {
        document.body.classList.remove('inchef-splash-active', 'home-prepare-reveal', 'home-skip-revealing');
        if (splashEl.parentNode) {
          splashEl.parentNode.removeChild(splashEl);
        }
        return;
      }
    } catch (e) {
      // In case sessionStorage is restricted
    }

    // Lock page scroll and prepare Home page elements for smooth staggered entrance
    document.body.classList.add('inchef-splash-active', 'home-prepare-reveal');

    let dismissed = false;
    let unmounted = false;

    // Unmount splash cleanly once exit animation has completed
    function unmountSplash() {
      if (unmounted) return;
      unmounted = true;

      try {
        sessionStorage.setItem(SESSION_KEY, 'true');
      } catch (e) {}

      // Release page scroll without any layout jump
      document.body.classList.remove('inchef-splash-active');

      // Schedule removal of preparation reveal classes once Home entrance settles
      setTimeout(function () {
        document.body.classList.remove('home-prepare-reveal', 'home-skip-revealing');
      }, 500);

      // Remove splash element from DOM cleanly
      if (splashEl && splashEl.parentNode) {
        splashEl.parentNode.removeChild(splashEl);
      }

      // Clean up window event listeners
      window.removeEventListener('keydown', handleKeydown);
    }

    // Deterministic auto-finish timer at 4.0s (3.2s reading hold + 0.7s exit animation + 0.1s buffer)
    // Guarantees the splash NEVER freezes or blocks scrolling under any circumstances
    const autoUnmountTimer = setTimeout(unmountSplash, 4000);

    // Listen for animationend on the splash overlay
    splashEl.addEventListener('animationend', function (e) {
      if (e.target !== splashEl) return;
      if (
        e.animationName === 'splashFadeOut' ||
        e.animationName === 'splashQuickFade' ||
        e.animationName === 'splashReducedFadeOut'
      ) {
        clearTimeout(autoUnmountTimer);
        unmountSplash();
      }
    });

    // Also listen on the content element in case overlay animation was composited
    const splashContent = splashEl.querySelector('.inchef-splash-content');
    if (splashContent) {
      splashContent.addEventListener('animationend', function (e) {
        if (
          e.animationName === 'splashContentExit' ||
          e.animationName === 'splashContentQuickExit'
        ) {
          clearTimeout(autoUnmountTimer);
          unmountSplash();
        }
      });
    }

    // Skip handler: triggers quick 0.3s fade-out and accelerated reveal
    function handleSkip() {
      if (dismissed || unmounted) return;
      dismissed = true;
      clearTimeout(autoUnmountTimer);

      // Switch to accelerated 0.3s exit on splash and shortened reveal on Home page
      splashEl.classList.add('splash-skipping');
      document.body.classList.remove('home-prepare-reveal');
      document.body.classList.add('home-skip-revealing');

      // Unmount after the accelerated 0.3s fade completes
      setTimeout(unmountSplash, 320);
    }

    // Dedicated Skip button click listener
    const skipBtn = document.getElementById('inchef-splash-skip');
    if (skipBtn) {
      skipBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        handleSkip();
      });
    }

    // Prevent accidental skip on initial load: only allow overlay click to skip after 800ms
    let allowOverlaySkip = false;
    setTimeout(function () {
      allowOverlaySkip = true;
    }, 800);

    splashEl.addEventListener('click', function (e) {
      if (e.target === skipBtn || (skipBtn && skipBtn.contains(e.target))) {
        return;
      }
      if (allowOverlaySkip) {
        handleSkip();
      }
    });

    // Keyboard listener: only Space or Escape triggers skip
    function handleKeydown(e) {
      if (!e) return;
      if (e.key === 'Escape' || e.key === ' ' || e.code === 'Space') {
        handleSkip();
      }
    }
    window.addEventListener('keydown', handleKeydown);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSplash);
  } else {
    initSplash();
  }
})();
