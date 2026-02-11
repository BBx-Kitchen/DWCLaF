# Phase 12: More Rendering Detail to Match DWC - Context

**Gathered:** 2026-02-11
**Status:** Ready for planning

<domain>
## Phase Boundary

Close remaining visual rendering gaps between the 13 existing Swing component delegates and their DWC web counterparts. No new components — this phase refines rendering fidelity on what exists. Focus areas: font matching, button text colors, ComboBox full rework, TextField flat border, ProgressBar track rounding, border crispness, and typography weight/size accuracy.

</domain>

<decisions>
## Implementation Decisions

### Font family
- Use closest available system font to match DWC's web font stack
- Claude derives the correct system font mapping from DWC's CSS font-family declarations
- Do NOT bundle a .ttf file — use platform-native fonts

### Font size and weight
- Both size and weight are off compared to DWC — need correction
- Claude should audit all font token mappings against DWC CSS and fix mismatches
- Section headers, button text, label text, and body text all need review

### Button text color
- Default variant button has black text where DWC renders white text on blue background
- All colored variant buttons should have white text matching DWC
- Claude to audit foreground color tokens for every button variant and fix

### Button borders
- Colored variant buttons should have barely-visible or same-color borders matching DWC
- Current Swing borders are too prominent on colored variants
- Match DWC exactly — derive border colors from DWC tokens

### ComboBox rendering (full rework)
- Everything needs rework: border, background, arrow area, separator, overall crispness
- Current rendering looks blurry and substantially different from DWC
- Target: thin crisp border, flat background, small chevron, subtle separator line
- Claude has discretion on implementation approach but must match DWC visual closely

### TextField appearance
- Must be fully flat — zero inset/shadow/3D effect
- Thin border only, no sunken look whatsoever
- Current Swing rendering has a visible sunken/inset appearance that DWC does not have

### ProgressBar track rounding
- DWC rounds both the colored fill AND the gray track (the 100% background container)
- Current Swing only rounds the fill bar
- Track (background container) must also have rounded ends matching DWC

### Border thickness and crispness
- High priority — DWC borders are very thin and crisp (1px)
- Swing TextField and ComboBox borders appear thicker or less crisp
- Claude to audit border rendering for 1px precision at all scale factors

### ProgressBar text contrast
- DWC dynamically switches percentage text color (dark/white) based on fill color contrast
- Swing should do the same — use white text on dark fills, dark text on light fills
- Claude to implement contrast-aware text color selection

### Claude's Discretion
- Exact system font selection per platform (Mac/Windows/Linux)
- ComboBox implementation approach (within constraint of matching DWC visuals)
- Border rendering technique for 1px crispness (stroke alignment, subpixel handling)
- ProgressBar contrast threshold for text color switching
- Any shadow parameter tuning (panels were explicitly skipped in discussion)

</decisions>

<specifics>
## Specific Ideas

- User provided side-by-side screenshots of DWC web (browser) and Swing (Webswing) for direct comparison
- The Default button specifically has black text where DWC shows white — this is the most visible text color mismatch
- ComboBox is described as "blurry and way different" — needs comprehensive attention, not incremental fixes
- TextField is described as "still sunken" — previous attempts haven't fully flattened it
- ProgressBar track radius is described as "only round for the colored bar" — the gray container also needs rounding in DWC

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 12-more-rendering-detail-to-match-dwc*
*Context gathered: 2026-02-11*
