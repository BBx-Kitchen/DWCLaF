---
phase: 03-shared-painting-utilities
verified: 2026-02-10T19:30:00Z
status: passed
score: 10/10 must-haves verified
re_verification: false
---

# Phase 3: Shared Painting Utilities Verification Report

**Phase Goal:** Reusable painting utilities provide antialiasing, shadows, focus rings, and rounded borders for all delegates
**Verified:** 2026-02-10T19:30:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | All component painting uses antialiased rendering with HiDPI-aware float coordinates | ✓ VERIFIED | PaintUtils.setupPaintingHints enables KEY_ANTIALIASING=VALUE_ANTIALIAS_ON and KEY_STROKE_CONTROL=VALUE_STROKE_NORMALIZE. All shape methods use float coordinates. HiDpiUtils.getScaleFactor reads from Graphics2D.getTransform().getScaleX(). |
| 2 | Rounded corners paint correctly with configurable arc radius from CSS tokens | ✓ VERIFIED | PaintUtils.createRoundedShape accepts float arc parameter and degenerates correctly: arc<=0 → Rectangle2D, arc>=min(w,h) on square → Ellipse2D, else → RoundRectangle2D.Float with clamped arc. |
| 3 | Focus ring paints outside component bounds with semi-transparent color matching DWC focus-visible | ✓ VERIFIED | FocusRingPainter.paintFocusRing paints semi-transparent ring outside component bounds using Path2D.WIND_EVEN_ODD with outer arc expanded by ringWidth. Matches DWC `box-shadow: 0 0 0 3px hsla(H,S,45%,0.4)` pattern. |
| 4 | Box shadow renders behind components using cached blurred images for performance | ✓ VERIFIED | ShadowPainter.paintShadow uses ConcurrentHashMap<ShadowCacheKey, SoftReference<BufferedImage>> for GC-friendly caching. Two-pass separable ConvolveOp with EDGE_ZERO_FILL produces Gaussian blur. Shadow cache key includes all visual parameters. |
| 5 | State color resolver picks correct color for hover/pressed/focused/disabled states consistently | ✓ VERIFIED | StateColorResolver.resolve implements disabled>pressed>hover>focused>enabled priority chain. Pressed checks isArmed() AND isPressed() per FlatLaf pattern. All states fallback to enabled color if null. |

**Score:** 5/5 truths verified

### Required Artifacts

#### Plan 01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/dwc/laf/painting/PaintUtils.java` | Antialiasing setup/restore, rounded shape factory, outline painting, rounded background fill | ✓ VERIFIED | 135 lines. Exports setupPaintingHints, restorePaintingHints, createRoundedShape, paintOutline, paintRoundedBackground. Shape degeneration logic present. WIND_EVEN_ODD for outline. |
| `src/main/java/com/dwc/laf/painting/HiDpiUtils.java` | HiDPI scale detection, device-resolution BufferedImage creation | ✓ VERIFIED | 57 lines. Exports getScaleFactor, createHiDpiImage. Scale read from Graphics2D transform only, not system properties. |
| `src/main/java/com/dwc/laf/painting/StateColorResolver.java` | Component state to color resolution with priority chain | ✓ VERIFIED | 87 lines. Exports resolve, paintWithOpacity. Priority chain: disabled>pressed>hover>focused>enabled. Coalesce helper for null fallback. |
| `src/test/java/com/dwc/laf/painting/PaintUtilsTest.java` | Tests for shape creation, hint save/restore, outline geometry | ✓ VERIFIED | 10 tests pass. Covers zero arc, normal arc, circle degeneration, arc clamping, outline painting, background fill. |
| `src/test/java/com/dwc/laf/painting/HiDpiUtilsTest.java` | Tests for scale factor extraction and image sizing | ✓ VERIFIED | 7 tests pass. Covers 1x/2x scale, null graphics, zero dimensions, TYPE_INT_ARGB. |
| `src/test/java/com/dwc/laf/painting/StateColorResolverTest.java` | Tests for state priority chain and opacity painting | ✓ VERIFIED | 11 tests pass. Covers all priority combinations, disabled fallback, opacity clamping, composite save/restore. |

#### Plan 02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/dwc/laf/painting/FocusRingPainter.java` | Semi-transparent focus ring outside component bounds via even-odd fill | ✓ VERIFIED | 72 lines. Exports paintFocusRing. Uses Path2D.WIND_EVEN_ODD with outer arc expansion. Calls PaintUtils.setupPaintingHints and restorePaintingHints. |
| `src/main/java/com/dwc/laf/painting/ShadowPainter.java` | Box shadow rendering with Gaussian blur and SoftReference image caching | ✓ VERIFIED | 174 lines. Exports paintShadow, applyGaussianBlur (package-private), clearCache (package-private). ShadowCacheKey record for cache keys. Two-pass ConvolveOp blur. Blur radius clamped to 50px. |
| `src/test/java/com/dwc/laf/painting/FocusRingPainterTest.java` | Tests for focus ring geometry, color, painting | ✓ VERIFIED | 6 tests pass. Covers null/zero guards, pixel painting, color preservation, hint restore, arc expansion. |
| `src/test/java/com/dwc/laf/painting/ShadowPainterTest.java` | Tests for shadow rendering, blur, caching, cache invalidation | ✓ VERIFIED | 8 tests pass. Covers null/zero guards, soft pixels, caching, cache miss, blur clamping, blur spread, zero radius. |

### Key Link Verification

#### Plan 01 Key Links

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| PaintUtils.java | java.awt.geom.RoundRectangle2D.Float | createRoundedShape returns Shape | ✓ WIRED | Line 85: `return new RoundRectangle2D.Float(x, y, w, h, clampedArc, clampedArc);` |
| PaintUtils.java | java.awt.geom.Path2D | paintOutline uses WIND_EVEN_ODD | ✓ WIRED | Line 105: `Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);` |
| StateColorResolver.java | javax.swing.AbstractButton | instanceof check for ButtonModel state | ✓ WIRED | Line 49: `if (c instanceof AbstractButton ab) {` |

#### Plan 02 Key Links

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| FocusRingPainter.java | PaintUtils.java | Uses createRoundedShape and setupPaintingHints | ✓ WIRED | Lines 48, 63-64, 68: setupPaintingHints, createRoundedShape (2 calls), restorePaintingHints |
| ShadowPainter.java | PaintUtils.java | Uses createRoundedShape for shadow shape | ✓ WIRED | Line 94: `ig.fill(PaintUtils.createRoundedShape(padding, padding, width, height, arc));` |
| ShadowPainter.java | HiDpiUtils.java | Uses getScaleFactor for device-resolution images | ⚠️ DEFERRED | Plan notes "For prototype, skip device-resolution shadow images (optimize later if needed)". Shadow drawn at logical dimensions; Graphics2D transform handles HiDPI scaling. Documented decision. |
| ShadowPainter.java | java.awt.image.ConvolveOp | Two-pass separable Gaussian blur | ✓ WIRED | Lines 132, 137: `new ConvolveOp(hKernel, ConvolveOp.EDGE_ZERO_FILL, null)` for both horizontal and vertical passes |

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| PAINT-01: All painting uses antialiased rendering (RenderingHints) | ✓ SATISFIED | PaintUtils.setupPaintingHints sets KEY_ANTIALIASING=VALUE_ANTIALIAS_ON and KEY_STROKE_CONTROL=VALUE_STROKE_NORMALIZE. Used by PaintUtils.paintRoundedBackground and FocusRingPainter.paintFocusRing. Tests verify hint save/restore. |
| PAINT-02: Rounded corners painted via configurable arc radius from CSS tokens | ✓ SATISFIED | PaintUtils.createRoundedShape accepts configurable float arc parameter. Shape degeneration logic handles arc=0 (Rectangle2D), arc>=min(w,h) on square (Ellipse2D), else RoundRectangle2D with clamped arc. Tests verify all degeneration cases. |
| PAINT-03: Focus ring painted outside component bounds with semi-transparent color matching DWC focus-visible style | ✓ SATISFIED | FocusRingPainter.paintFocusRing paints semi-transparent ring outside component bounds using Path2D.WIND_EVEN_ODD. Outer arc = componentArc + ringWidth. Tests verify ring paints outside bounds with correct color. Matches DWC `box-shadow: 0 0 0 3px hsla(H,S,45%,0.4)` pattern. |
| PAINT-04: Box shadow / elevation painted behind components using cached blurred images | ✓ SATISFIED | ShadowPainter.paintShadow uses ConcurrentHashMap<ShadowCacheKey, SoftReference<BufferedImage>> for GC-friendly caching. ShadowCacheKey includes all visual parameters (width, height, arc, blurRadius, colorRgb). Two-pass separable ConvolveOp produces Gaussian blur. Tests verify caching and cache invalidation. |
| PAINT-05: HiDPI-aware painting using float coordinates and Graphics2D scaling | ✓ SATISFIED | All PaintUtils shape methods use float coordinates. HiDpiUtils.getScaleFactor reads from Graphics2D.getTransform().getScaleX(), never system properties. HiDpiUtils.createHiDpiImage creates BufferedImage at device resolution (logicalSize * scale). Tests verify 1x and 2x scale. |
| PAINT-06: State color resolver picks correct color for hover/pressed/focused/disabled states | ✓ SATISFIED | StateColorResolver.resolve implements disabled>pressed>hover>focused>enabled priority chain. Pressed checks isArmed() AND isPressed() per FlatLaf pattern. All states fallback to enabled color if null. Tests verify all priority combinations and fallback behavior. |

### Anti-Patterns Found

None. All implementation files are clean:
- No TODO/FIXME/XXX/HACK/PLACEHOLDER comments
- No `return null`, `return {}`, `return []` stub patterns
- No console.log or System.out.println debugging statements
- All methods have substantive implementations with correct logic

### Task Commits Verified

All 5 task commits exist in git history:

1. `97cb5f8` - feat(03-01): add PaintUtils with antialiasing, rounded shapes, outlines, and background fill
2. `952a96d` - feat(03-01): add HiDpiUtils with scale detection and device-resolution image creation
3. `1e3f270` - feat(03-01): add StateColorResolver with state priority chain and opacity painting
4. `95dc8ef` - feat(03-02): add FocusRingPainter for DWC-style focus ring outside component bounds
5. `8e34478` - feat(03-02): add ShadowPainter with Gaussian blur and SoftReference caching

### Test Results

All 42 painting utility tests pass:
- PaintUtilsTest: 10 tests, 0 failures
- HiDpiUtilsTest: 7 tests, 0 failures
- StateColorResolverTest: 11 tests, 0 failures
- FocusRingPainterTest: 6 tests, 0 failures
- ShadowPainterTest: 8 tests, 0 failures

Total: 42 tests, 0 failures, 0 errors, 0 skipped

### External Wiring Status

The painting utilities are not yet consumed outside the `com.dwc.laf.painting` package. This is expected because:
- Phase 03 provides foundation utilities
- Phase 04-07 (component delegates) depend on Phase 03 but haven't been implemented yet
- Internal wiring within painting package is complete:
  - FocusRingPainter → PaintUtils (setupPaintingHints, createRoundedShape, restorePaintingHints)
  - ShadowPainter → PaintUtils (createRoundedShape)
  - ShadowPainter → ConvolveOp (two-pass Gaussian blur)

Ready for Phase 04 (Button Delegates) to consume these utilities.

### Patterns Established

1. **Static utility with hint save/restore**: `setupPaintingHints` returns saved state, `restorePaintingHints` restores it
2. **Shape factory degeneration**: arc=0 → Rectangle2D, arc>=min(w,h) on square → Ellipse2D, else → RoundRectangle2D
3. **Even-odd fill outline**: Path2D.WIND_EVEN_ODD with outer+inner shapes for pixel-exact rings
4. **State color resolution**: disabled>pressed>hover>focused>enabled priority chain with null fallback to enabled
5. **Focus ring via even-odd**: outer shape (component + ringWidth) minus inner shape (component)
6. **Outer arc expansion**: outerArc = componentArc + ringWidth for smooth ring curvature
7. **Two-pass separable Gaussian blur**: horizontal ConvolveOp then vertical ConvolveOp, EDGE_ZERO_FILL
8. **SoftReference image cache**: ConcurrentHashMap<CacheKey, SoftReference<BufferedImage>> for GC-friendly caching
9. **Blur padding**: 3*blurRadius on each side to prevent edge clipping

### Key Decisions Verified

1. **Pressed state checks both isArmed() AND isPressed()** (FlatLaf pattern) - ensures mouse is still within button bounds during press (StateColorResolver line 51)
2. **hasFocus() used directly on Component** rather than isFocusOwner() for broader compatibility (StateColorResolver line 59)
3. **HiDPI scale read from Graphics2D transform only**, never system properties or Toolkit (HiDpiUtils line 32)
4. **Shadow blur radius clamped to 50px max** - DWC CSS uses up to 100px but that's excessive for Swing (ShadowPainter line 25)
5. **ShadowCacheKey as Java record** for automatic hashCode/equals covering all visual parameters (ShadowPainter line 35)
6. **applyGaussianBlur made package-private** for direct testability (ShadowPainter line 122)
7. **Shadow images drawn at logical dimensions** - Graphics2D transform handles HiDPI scaling naturally (ShadowPainter line 104)

## Summary

Phase 03 goal **achieved**. All 5 observable truths verified. All 10 required artifacts exist and are substantive with complete implementations. All key links wired except one deferred optimization (HiDpiUtils in ShadowPainter, documented decision). All 6 requirements satisfied. 42 tests pass. No anti-patterns found. Ready for Phase 04.

The painting utilities package provides a complete foundation for component delegates:
- **PaintUtils**: antialiasing, rounded shapes, outlines, backgrounds
- **HiDpiUtils**: scale detection and device-resolution images  
- **StateColorResolver**: state-to-color priority chain and opacity painting
- **FocusRingPainter**: DWC-style semi-transparent focus rings
- **ShadowPainter**: Gaussian-blurred box shadows with SoftReference caching

All utilities are stateless, thread-safe, and well-tested. The phase successfully establishes the shared painting contract that Phases 4-7 component delegates depend on.

---

_Verified: 2026-02-10T19:30:00Z_
_Verifier: Claude (gsd-verifier)_
