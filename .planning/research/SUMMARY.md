# Project Research Summary

**Project:** Swing Look & Feel with CSS Token Integration
**Domain:** Java Desktop UI Framework Extension
**Researched:** 2026-02-10
**Confidence:** HIGH

## Executive Summary

This project creates a Swing Look & Feel that bridges web and desktop design systems by parsing CSS custom properties (design tokens) and mapping them to Swing's UIDefaults. The recommended approach follows FlatLaf's proven architecture: extend BasicLookAndFeel, build a lightweight custom CSS parser (500-800 lines) for `:root` token extraction with var() resolution, and implement ComponentUI delegates that read from UIDefaults. This delivers unique value—visual parity between web and Swing using the same CSS source—without the complexity of a full CSS engine.

The core innovation is CSS-driven theming in Swing, which no existing L&F provides. FlatLaf uses properties files, Darcula uses JSON, but this project uses actual CSS custom properties compiled from SCSS. The technical foundation is solid: Java 21+ with records and pattern matching, Maven for standard JAR packaging, and BasicLookAndFeel as the cleanest base class (same choice FlatLaf made). Zero external dependencies for runtime; build a custom parser rather than shipping 200KB-2MB CSS libraries that handle far more than needed.

Critical risks center on thread safety (EDT violations during CSS updates), variable resolution cycles (circular var() references causing stack overflow), color space mismatches (CSS sRGB vs AWT device RGB), and shadow rendering performance collapse (naive Gaussian blur on every paint). These are all preventable with proper architecture decisions in Phase 1 and early testing. Success depends on building the CSS→UIDefaults pipeline correctly first, then adding components incrementally rather than attempting all 63 DWC components at once.

## Key Findings

### Recommended Stack

The stack is deliberately minimal to maintain zero runtime dependencies. Java 21+ provides the language features needed (records for token values, sealed classes for type-safe parsing results). Maven handles standard library JAR packaging with the shade plugin available if a CSS library is ever needed. BasicLookAndFeel is the correct base class—FlatLaf uses the same approach, avoiding Metal's visual opinions and Synth's XML rigidity.

**Core technologies:**
- **Java 21+**: Records, sealed classes, pattern matching for cleaner token parsing and type safety
- **Maven 3.9+**: Standard Java library build with straightforward dependency management and JAR packaging
- **JUnit 5.10+**: Standard test framework with parameterized tests useful for multi-component validation
- **BasicLookAndFeel**: Cleanest foundation; FlatLaf's architecture proves this is the right base class

**CSS parsing approach:**
- **Build custom parser** (~500-800 lines) rather than using ph-css, jStyleParser, or CSSParser
- **Rationale**: Narrow scope (only `:root` custom properties), zero-dependency requirement, FlatLaf precedent (built own properties parser)
- **Complexity breakdown**: Lexer for `:root` blocks, var() resolver with cycle detection, color parser for hex/rgb/hsl (hardest part at ~200 lines)

### Expected Features

The feature landscape divides clearly into table stakes (required for any production L&F), differentiators (unique CSS-driven value), and anti-features (commonly requested but problematic). MVP targets 8 core components proving the CSS→Java pipeline rather than attempting all 63 DWC components.

**Must have (table stakes):**
- State-based rendering (hover, pressed, focus, disabled) — users expect visual feedback
- HiDPI/Retina support with scaled painting — required for modern displays
- Light and dark themes with runtime switching — every modern UI supports this
- Rounded corners (border-radius) — square corners look dated; FlatLaf uses Component.arc pattern
- Focus indication for keyboard navigation — accessibility requirement
- Proper font rendering from typography tokens — text must match expected typeface/weight

**Should have (competitive differentiators):**
- CSS-driven theming — theme defined by CSS files, same source as web (core innovation)
- Web-Swing visual parity — Swing app looks like web app using same design tokens
- Token mapping configurability — properties file maps `--dwc-*` to UIDefaults keys
- Shadow/elevation system — card-style elevation matching web box-shadow
- Semantic color system — primary/success/warning/danger mapped from tokens

**Defer (v2+):**
- Animated transitions — adds 2-3x complexity per delegate; start with instant state switches
- Full CSS selector matching — that's a browser engine problem; only parse `:root` tokens
- SVG icon rendering — requires Batik (5MB dependency); use pre-rasterized PNGs initially
- CSS calc() support — most values can be pre-computed in SCSS build step
- All 63 DWC components — start with 8, add incrementally after validation

**Feature dependencies:**
- ComponentUI delegates require UIDefaults populated (mapper must run before any paint)
- Shadow painter enhances multiple delegates (build shared utility first)
- Focus ring painter is cross-cutting (build once, use everywhere)
- Theme switching requires full pipeline (Parse → Map → UpdateUI must be fast enough)

### Architecture Approach

The architecture follows a clean three-layer design: CSS Token Engine parses and resolves variables, Token→UIDefaults Bridge maps to Swing conventions, and ComponentUI Delegates paint using UIDefaults values. This matches FlatLaf's proven pattern and maintains separation between CSS parsing (no Swing dependency) and component rendering (no CSS dependency).

**Major components:**

1. **CSS Token Engine** — CSSTokenParser extracts `--custom-property` declarations from `:root` blocks, VarResolver handles var() references with cycle detection and depth limits, ColorParser converts hex/rgb/hsl to sRGB Color objects. Output is immutable ThemeTokens container with typed values.

2. **Token→UIDefaults Bridge** — UIDefaultsMapper reads token-mapping.properties (one CSS token → multiple UIDefaults keys), converts ThemeTokens to UIDefaults entries following `Component.property` naming convention, enables atomic batch updates to avoid race conditions.

3. **ComponentUI Delegates** — One class per component (DwcButtonUI, DwcTextFieldUI, etc.), extend Basic*UI from BasicLookAndFeel, override paint methods while preserving keyboard navigation, read all styling from UIDefaults (never hardcode colors/fonts).

4. **Shared Painting Utilities** — ShadowPainter with LRU cache for rendered shadows, FocusRingPainter for focus-visible rings, BorderPainter for rounded borders with stroke, StateColorResolver for hover/pressed/focused color selection.

**Build order (dependencies flow top-down):**
- Phase 1: CSS Token Engine (no Swing dependencies, fully testable)
- Phase 2: Swing Bridge (UIDefaultsMapper, DwcLookAndFeel skeleton)
- Phase 3: Shared Painting (utilities used by all delegates)
- Phase 4: Component Delegates (can parallelize once utilities exist)
- Phase 5: Themes & Demo (bundle CSS, create demo application)

**FlatLaf patterns to follow:**
- UIDefaults keys use `Component.property` convention (`Button.arc`, `Button.focusedBackground`)
- Shared painting utilities avoid duplication across delegates
- State tracking via AbstractButton.getModel() for rollover/pressed/selected/enabled
- HiDPI-aware painting with float coordinates and Graphics2D transforms
- Properties files loaded in hierarchy order with variable resolution

### Critical Pitfalls

Research identified 8 critical pitfalls requiring architectural decisions and 10 moderate pitfalls needing careful implementation. Thread safety and parsing correctness are paramount.

1. **Non-EDT painting operations** — CSS updates triggering component repaints from background threads cause race conditions and visual corruption. Prevention: Marshal all UIDefaults updates and repaints to EDT via SwingUtilities.invokeLater(). Detection: CheckThreadViolationRepaintManager during testing. Must address in Phase 1.

2. **CSS variable resolution infinite loops** — Circular var() references or deeply nested chains cause stack overflow. Prevention: Cycle detection with visited set, max depth limit (10-20 levels per CSS spec). Detection: Unit tests with pathological CSS, timeout on parsing operations. Address in Phase 1 parser architecture.

3. **UIDefaults race conditions on multi-monitor** — Partial UIDefaults updates when components query mid-update, especially with different DPI per screen. Prevention: Atomic batch updates synchronized on UIDefaults table. Detection: Test on multi-monitor with different DPI scaling. Address in Phase 2.

4. **Color space mismatches (sRGB vs device RGB)** — CSS colors are sRGB, java.awt.Color default constructor uses device RGB, causing visible color drift. Prevention: Always use explicit sRGB ColorSpace constructor. Detection: Side-by-side visual comparison with browser. Address in Phase 1 color parser.

5. **Font metrics divergence** — CSS font-family fallback chains don't map to Java font names, text wraps differently than web. Prevention: Explicit platform-specific font mapping, TextLayout for accurate metrics, 10-15% buffer for width calculations. Detection: Unit tests comparing text width. Address in Phase 2.

6. **Shadow rendering performance collapse** — Naive Gaussian blur on every paint drops 60fps to <5fps with 100 shadowed components. Prevention: LRU cache keyed by (elevation, width, height, arc), pre-render and reuse shadow bitmaps. Detection: Profiler shows >50% time in paint, frame rate drops. Optimize by Phase 3.

7. **State management across component hierarchy** — Hover/focus/pressed states don't propagate correctly through nested components. Prevention: Global state manager tracking mouse position, AWTEventListener for global mouse events. Detection: Manual testing of nested layouts. Address in Phase 2.

8. **BasicLookAndFeel assumptions breaking subclass behavior** — Overriding only paint() isn't enough; installUI/uninstallUI have hidden dependencies. Prevention: Always call super.installUI(), copy BasicLookAndFeel's installDefaults pattern exactly. Detection: Keyboard navigation stops working. Address in Phase 2 per component.

## Implications for Roadmap

Based on research, the project naturally divides into 5 phases with clear dependencies. The CSS→UIDefaults pipeline must work before any component can paint. Shared utilities must exist before implementing multiple delegates. This ordering avoids rework and catches architectural issues early.

### Phase 1: CSS Token Engine
**Rationale:** Foundation for everything else. CSS parsing has no Swing dependencies and can be fully tested in isolation. Must get variable resolution and color parsing correct first—retrofitting parser changes after delegates exist is expensive.

**Delivers:** CSSTokenParser with lexer, VarResolver with cycle detection and depth limits, ColorParser supporting hex/rgb/hsl in sRGB color space, ValueParser for numbers/insets/fonts/shadows, ThemeTokens immutable container, comprehensive parser tests

**Addresses:** Variable resolution cycles pitfall (cycle detection), color space mismatch pitfall (explicit sRGB), foundation for CSS-driven theming differentiator

**Avoids:** Building delegates that depend on unstable parser API; retrofitting color space fixes across all delegates; discovering cycle issues in production

**Stack elements:** Java 21 records for token values, pattern matching for parse results, no external dependencies

**Research needed:** No—CSS custom property syntax is well-specified, FlatLaf provides precedent for properties parsing approach

---

### Phase 2: UIDefaults Bridge & L&F Setup
**Rationale:** Once parser produces ThemeTokens, need mechanism to map them into Swing's UIDefaults system. This is the integration layer between CSS world and Swing world. Must be atomic to avoid multi-monitor race conditions.

**Delivers:** UIDefaultsMapper reading token-mapping.properties, DwcLookAndFeel skeleton extending BasicLookAndFeel, DwcLightLaf and DwcDarkLaf theme variants, synchronized UIDefaults update mechanism, light.css and dark.css bundled as resources

**Addresses:** UIDefaults race condition pitfall (atomic updates), runtime theme switching differentiator, light/dark theme table stakes

**Avoids:** Components reading half-updated UIDefaults; theme switching breaking component state; hardcoding mappings in Java instead of config file

**Stack elements:** Maven resources for CSS files, token-mapping.properties following FlatLaf key conventions

**Research needed:** No—UIDefaults API is stable, FlatLaf's theme loading pattern is well-documented

---

### Phase 3: Shared Painting Utilities
**Rationale:** All delegates need antialiasing setup, shadow rendering, focus rings, border painting. Building these once as shared utilities before implementing delegates avoids duplication and ensures consistency. ShadowPainter cache architecture must be correct before use.

**Delivers:** DwcPaintingUtils (antialiasing, HiDPI setup), BorderPainter (rounded borders with stroke), ShadowPainter with LRU cache (pre-rendered shadows), FocusRingPainter (focus-visible ring), StateColorResolver (state-based color selection), comprehensive painting tests

**Addresses:** Shadow performance collapse pitfall (LRU cache), state management pitfall (StateColorResolver), HiDPI table stakes, rounded corners table stakes

**Avoids:** Each delegate implementing its own shadow rendering; performance issues discovered after all delegates complete; inconsistent state handling across components

**Stack elements:** Graphics2D transforms for HiDPI, UIScale pattern from FlatLaf

**Research needed:** Moderate—shadow rendering performance needs profiling to validate cache effectiveness. Quick research spike on Gaussian blur optimization recommended.

---

### Phase 4: Core Component Delegates
**Rationale:** With parser, mapper, and utilities complete, implement 8 core delegates proving the full pipeline. Start with DwcButtonUI (most complex, most visible) to validate architecture. Can parallelize simpler delegates once button works. Focus on correctness over completeness—8 components prove concept better than 63 half-working ones.

**Delivers:** DwcButtonUI (state rendering, elevation), DwcTextFieldUI (placeholder, focus), DwcCheckBoxUI (custom check mark), DwcRadioButtonUI (custom dot), DwcComboBoxUI (styled dropdown), DwcLabelUI (typography), DwcPanelUI (card elevation), DwcTabbedPaneUI (tab styling), visual tests per component

**Addresses:** Font metrics divergence pitfall (explicit font mapping per delegate), BasicLookAndFeel assumption pitfall (proper installUI pattern), state management pitfall (using StateColorResolver), consistent styling table stakes, focus indication table stakes

**Avoids:** Attempting all 63 components without validation; discovering BasicLookAndFeel quirks after many delegates complete; state management inconsistencies across delegates

**Stack elements:** BasicLookAndFeel delegate patterns, UIDefaults `Component.property` conventions

**Research needed:** Moderate—each component type may have quirks. Quick research per component recommended:
- **DwcButtonUI**: Button state tracking, rollover listeners
- **DwcComboBoxUI**: Popup styling, arrow button painting
- **DwcTabbedPaneUI**: Tab layout calculations, close button painting

---

### Phase 5: Demo Application & Validation
**Rationale:** With core components working, build comprehensive demo showing all components with theme switching. This validates the CSS→Java pipeline end-to-end and provides visual proof of concept. Demo also serves as test harness for HiDPI and multi-monitor scenarios.

**Delivers:** DwcLafDemo application (component gallery), theme switcher UI, HiDPI test cases, multi-monitor validation, visual regression baseline, README with screenshots

**Addresses:** Web-Swing visual parity differentiator (side-by-side comparison), runtime theme switching differentiator, validation of all table stakes features

**Avoids:** Discovering integration issues after claiming completion; missing HiDPI/multi-monitor bugs that appear in production

**Stack elements:** SwingUtilities.updateComponentTreeUI for theme switching

**Research needed:** No—demo application follows standard Swing patterns

---

### Phase Ordering Rationale

**Dependency-driven sequence:**
- Phase 1 (Parser) has zero dependencies, must be correct before anything else
- Phase 2 (Bridge) depends on Phase 1 producing ThemeTokens
- Phase 3 (Utilities) depends on Phase 2 populating UIDefaults
- Phase 4 (Delegates) depends on Phase 3 providing shared painting code
- Phase 5 (Demo) depends on Phase 4 having working components

**Risk mitigation:**
- Building parser first catches syntax and resolution issues before Swing complexity
- Shared utilities before delegates prevents 8x duplication of shadow/focus code
- Starting with DwcButtonUI (hardest) validates architecture before easier components
- Demo last ensures all components integrate correctly

**Pitfall avoidance:**
- EDT violations addressed in Phase 1 architecture (parser runs off-EDT, updates marshal to EDT)
- Variable cycles caught in Phase 1 tests before production CSS loaded
- UIDefaults races prevented in Phase 2 with atomic updates
- Color space issues fixed in Phase 1 parser, all delegates inherit correct behavior
- Shadow performance optimized in Phase 3 before delegates use it
- Font metrics addressed per-delegate in Phase 4 with explicit testing
- State management consistent via Phase 3 StateColorResolver
- BasicLookAndFeel quirks caught per-component in Phase 4 with thorough testing

### Research Flags

**Phases needing targeted research during planning:**

- **Phase 3 (Shared Painting)**: Shadow rendering performance optimization requires profiling actual implementation to validate cache effectiveness. Quick research spike (1-2 hours) recommended: implement naive blur, profile with 100 components, validate cache hit rate. Consider reading FlatLaf's shadow implementation.

- **Phase 4 (DwcComboBoxUI)**: JComboBox is complex (popup, arrow button, list renderer). May need research into BasicComboBoxUI internals to understand installUI requirements. Check FlatLaf's FlatComboBoxUI for reference patterns.

- **Phase 4 (DwcTabbedPaneUI)**: JTabbedPane has complex layout (tab strip, content area, close buttons). May need research into tab bounds calculation and focus painting. FlatLaf's FlatTabbedPaneUI is good reference.

**Phases with standard patterns (skip research-phase):**

- **Phase 1 (CSS Parser)**: CSS custom property syntax is well-specified, var() resolution algorithm is standard, color parsing formats are documented. FlatLaf's properties parser provides implementation reference.

- **Phase 2 (UIDefaults Bridge)**: UIDefaults API is stable since JDK 1.2, FlatLaf documents the mapping pattern thoroughly. No novel techniques required.

- **Phase 4 (DwcButtonUI, DwcTextFieldUI, DwcLabelUI, DwcPanelUI)**: These follow BasicLookAndFeel patterns directly. FlatLaf provides clear reference implementations.

- **Phase 5 (Demo)**: Standard Swing application, no novel UI patterns required.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Java 21 + Maven + BasicLookAndFeel is proven by FlatLaf. Custom parser approach validated by FlatLaf building own properties parser. |
| Features | HIGH | Table stakes list verified against FlatLaf and Darcula feature sets. Differentiators are unique and valuable. Anti-features are correctly identified. |
| Architecture | HIGH | Three-layer design matches FlatLaf proven pattern. Component responsibilities are clear. Build order follows dependency flow. |
| Pitfalls | MEDIUM | Critical pitfalls are well-documented in Swing/CSS specs, but mitigation strategies need validation with actual implementation. Shadow performance and font metrics require testing. |

**Overall confidence:** HIGH

Research is based on verified sources (FlatLaf GitHub with source code review, official Java/CSS documentation). The recommendation to build a custom parser rather than use external libraries is strongly supported by FlatLaf precedent (they built UIDefaultsLoader instead of using properties libraries). The three-layer architecture matches FlatLaf's proven design. Phase ordering is straightforward dependency flow.

### Gaps to Address

**Color space handling nuance:** While explicit sRGB ColorSpace usage is clearly required, need to validate whether Color.getColorSpace() returns correct space for UIDefaults colors. Test early with visual comparison against browser rendering. If drift appears, may need to convert all UIDefaults colors to sRGB space explicitly.

**Font metric testing approach:** The 10-15% buffer recommendation for text width is based on general Java2D knowledge, but actual divergence from web needs measurement with real DWC typography tokens. Plan for iteration in Phase 4 based on visual comparison results.

**Shadow cache sizing:** LRU cache size (recommended 100 entries) is estimate. Needs profiling with actual component count and resize patterns. Too small = cache thrash, too large = memory waste. Monitor cache hit rate and adjust in Phase 3.

**Multi-monitor testing environment:** UIDefaults race condition testing requires actual multi-monitor setup with different DPI per screen. If environment unavailable, simulate with JDK command-line scaling flags (-Dsun.java2d.uiScale=2.0) but note this may not catch all race conditions.

**CSS calc() deferral risk:** Recommendation to pre-compute calc() in SCSS assumes DWC build pipeline outputs resolved values. Verify this assumption early—if DWC CSS contains calc() at runtime, Phase 1 parser must support it (adds ~100 lines for expression evaluator).

**Component count scope creep:** MVP targets 8 components but pressure may exist to add more before validation. Resist scope creep—proving CSS→Java pipeline correctness with 8 components is more valuable than 20 partially-working components. Budget Phase 6+ for incremental component additions after Phase 5 validation.

## Sources

### Primary (HIGH confidence)
- [FlatLaf GitHub](https://github.com/JFormDesigner/FlatLaf) — Complete source code review of UIDefaultsLoader.java, FlatLaf.java, component delegates for architecture patterns and implementation reference
- [FlatLaf Official Documentation](https://www.formdev.com/flatlaf/) — How to Customize guide for UIDefaults conventions, Properties Files format guide, Themes documentation for loading patterns
- Oracle Java Documentation — Swing Look & Feel Architecture, BasicLookAndFeel API, UIDefaults thread safety, Graphics2D painting
- W3C CSS Specifications — CSS Custom Properties (Variables), CSS Color Module Level 4 for color space handling, var() resolution algorithm

### Secondary (MEDIUM confidence)
- ph-css, jStyleParser, CSSParser library evaluations — Feature comparison for why not to use external parsers (based on library documentation, not deep code review)
- Java 21 features documentation — Records, sealed classes, pattern matching for parser implementation patterns

### Tertiary (LOW confidence, needs validation)
- Shadow rendering performance characteristics — Gaussian blur complexity estimates need profiling validation with actual implementation
- Font metrics divergence magnitude — 10-15% buffer estimate needs measurement with real DWC typography tokens and platform testing

---
*Research completed: 2026-02-10*
*Ready for roadmap: yes*
