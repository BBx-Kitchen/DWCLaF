# Phase 1: CSS Token Engine - Context

**Gathered:** 2026-02-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Parse CSS custom properties from compiled DWC theme files and resolve variable references into typed Java values (colors, dimensions, etc.). This phase delivers the parsing and resolution engine only — mapping tokens to Swing UIDefaults is Phase 2.

</domain>

<decisions>
## Implementation Decisions

### CSS source scope
- Parse :root custom properties AND component-level selectors (e.g., .dwc-button)
- Flatten all tokens into a single map — component selectors and :root share the same namespace, component-level values override :root for same property name
- Single CSS file per theme (one file input, not directory scanning)
- May need a flattening utility for customers with custom CSS structures — note for future consideration

### Token type handling
- All CSS color formats supported: hsl/hsla, hex (#rgb, #rrggbb, #rrggbbaa), rgb/rgba, and named CSS colors — all resolve to java.awt.Color
- Eager resolution: parse once, resolve all var() references upfront, build immutable resolved map. Catches all errors at load time
- Claude's Discretion: dimension handling (px vs rem conversion), whether to evaluate calc() expressions — base on actual DWC CSS content

### Override & layering
- Merge (overlay) model: external CSS tokens override matching keys, bundled CSS provides defaults for anything not overridden
- Two layers only: bundled defaults + one external override. No multi-layer cascade
- External override path specified via system property (e.g., -Ddwc.theme=/path/to/override.css)
- Startup-only theme loading — no runtime theme switching. Theme loads once at L&F initialization

### Error & edge cases
- Skip + warn on malformed CSS values: log warning via JUL, skip bad token, continue parsing rest of file
- Circular variable references: detect, log warning, exclude those tokens, continue with everything else
- Missing token references (var(--missing) with no fallback): log warning — should be rare since bundled theme provides full coverage
- All warnings/errors logged via java.util.logging (JUL) — zero dependency, integrates with app's logging config
- Reference: DWC component defaults at https://dwc.style/docs/ — bundled theme should cover all tokens

### Claude's Discretion
- Shorthand CSS property handling (parse or ignore based on what DWC CSS actually contains)
- calc() expression evaluation (evaluate if needed by Swing-relevant tokens, raw string otherwise)
- Dimension type model (pixel ints vs typed objects — based on what Swing UIDefaults actually needs)
- Internal parser implementation approach

</decisions>

<specifics>
## Specific Ideas

- DWC component documentation at https://dwc.style/docs/#/dwc/BBjButton shows component tokens — use as reference for what tokens exist
- Bundled theme should provide defaults for everything, so missing tokens are primarily an override-authoring problem, not a runtime crash concern

</specifics>

<deferred>
## Deferred Ideas

- Multi-layer CSS cascade (base → customer → runtime) — revisit if two-layer model proves insufficient
- Flattening utility for custom CSS structures — future tooling consideration
- Runtime theme switching — Phase 8 demo may need this reconsidered

</deferred>

---

*Phase: 01-css-token-engine*
*Context gathered: 2026-02-10*
