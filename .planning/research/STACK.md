# Stack Research

**Domain:** Swing Look & Feel with CSS Token Integration
**Researched:** 2026-02-10
**Confidence:** HIGH (verified against FlatLaf source, official docs)

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Java | 21+ | Language & runtime | Records, sealed classes, pattern matching; project minimum |
| Maven | 3.9+ | Build system | Standard for Java library JARs; straightforward dependency management, shade plugin for fat JARs |
| JUnit 5 | 5.10+ | Testing | Standard Java test framework; parameterized tests useful for multi-component testing |
| BasicLookAndFeel | JDK built-in | L&F base class | Cleanest foundation; FlatLaf uses same approach — extends BasicLookAndFeel, overrides ComponentUI delegates |

### Supporting Libraries (All Optional / Dev-Only)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| AssertJ | 3.25+ | Test assertions | Fluent assertions for color/insets/font comparisons |
| Awaitility | 4.2+ | Async test support | Testing EDT-dispatched operations |
| Maven Shade Plugin | 3.5+ | JAR packaging | Only if shading a CSS parser library; otherwise not needed |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Maven Wrapper (mvnw) | Reproducible builds | Include `mvnw` so contributors don't need Maven installed |
| JDK Flight Recorder | Performance profiling | Built into JDK 21; zero overhead profiling of paint methods |
| CheckThreadViolationRepaintManager | EDT violation detection | Include in test/dev mode to catch threading bugs early |

## CSS Parsing Approach

**Recommendation: Build a custom lightweight CSS custom property parser.**

Rationale:
1. **Zero-dependency requirement** — External CSS parsers (ph-css, jStyleParser, CSSParser) are full CSS parsers that add 200KB-2MB to the JAR and handle far more than we need
2. **Narrow scope** — We only need to parse `:root { --name: value; }` blocks and resolve `var()` references. We don't need selector specificity, cascade, or layout
3. **FlatLaf precedent** — FlatLaf built its own properties file parser (`UIDefaultsLoader.java`) rather than using an external library. It's ~1500 lines and handles all their needs
4. **CSS custom properties are simple** — The grammar for `--name: value` declarations and `var(--name, fallback)` is regular enough for a hand-written recursive descent parser
5. **Color parsing is the hardest part** — Must handle `#hex`, `rgb()`, `rgba()`, `hsl()`, `hsla()`, and modern space-separated syntax `rgb(255 0 0 / 0.5)`. This is ~200 lines of code

**Estimated effort:** 500-800 lines for the full CSS token parser including var() resolution and color parsing.

### Existing Libraries Considered

| Library | Why NOT Recommended |
|---------|---------------------|
| ph-css (github.com/phax/ph-css) | Full CSS3 parser, ~500KB, supports var() but overkill for token extraction |
| jStyleParser (github.com/radkovo/jStyleParser) | Focused on CSS-to-DOM assignment, not token extraction; heavy |
| CSSParser (cssparser.sourceforge.net) | Legacy project, CSS 2.1 focused, limited custom property support |
| JavaFX CssParser | Tied to JavaFX runtime, different property syntax, not usable in pure Swing |

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Custom CSS parser | ph-css shaded into JAR | If CSS parsing scope grows beyond custom properties (e.g., full selector matching) |
| Maven | Gradle | If project grows to multi-module with complex build logic |
| JUnit 5 | TestNG | Never — JUnit 5 is the standard and has better tooling |
| BasicLookAndFeel base | SynthLookAndFeel | If XML-based theming is preferred over code-based delegates; but Synth is less flexible for custom painting |
| Manual UIDefaults | FlatLaf as dependency | If willing to accept runtime dependency; would get HiDPI, fonts, and base theme for free |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| MetalLookAndFeel as base | Metal adds visual opinions (gradients, 3D borders) that fight custom styling | BasicLookAndFeel |
| SynthLookAndFeel | XML-based theming is rigid; can't express Java2D painting logic in XML | BasicLookAndFeel + custom delegates |
| Reflection to access private Swing APIs | Breaks across JDK versions; strong encapsulation in JDK 17+ | Public API only |
| External CSS library at runtime | Adds dependency, increases JAR size, handles more than needed | Custom parser (~500 lines) |
| java.awt.Color(int,int,int) for CSS colors | Uses device color space, not sRGB; colors won't match web | Color with explicit sRGB ColorSpace |
| Timer for animation (prototype) | Premature complexity; defer animations to post-prototype | Instant state switches |

## Version Compatibility

| Component | Compatible With | Notes |
|-----------|-----------------|-------|
| Java 21 | Maven 3.9+ | Maven needs Java 21 toolchain support |
| JUnit 5.10 | Java 21 | Full support for records in test parameters |
| BasicLookAndFeel | All JDK versions | Stable API since JDK 1.2; no breaking changes expected |

## Sources

- [FlatLaf GitHub](https://github.com/JFormDesigner/FlatLaf) — Architecture reference, UIDefaultsLoader.java pattern
- [FlatLaf How to Customize](https://www.formdev.com/flatlaf/how-to-customize/) — UIDefaults key patterns, properties file format
- [ph-css GitHub](https://github.com/phax/ph-css) — Evaluated as CSS parser option
- [jStyleParser](https://github.com/radkovo/jStyleParser) — Evaluated as CSS parser option
- [CSSParser](https://cssparser.sourceforge.net/) — Evaluated as CSS parser option

---
*Stack research for: Swing Look & Feel with CSS Token Integration*
*Researched: 2026-02-10*
