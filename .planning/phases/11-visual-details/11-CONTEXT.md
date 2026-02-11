# Phase 11: Visual Details - Context

**Gathered:** 2026-02-11
**Status:** Ready for planning

<domain>
## Phase Boundary

Close specific rendering gaps between Swing L&F components and their DWC web counterparts. Four components need visual fixes: JProgressBar, JComboBox, JTextField, and JTree. No new components or capabilities — this is polish work to achieve closer visual parity with DWC web.

</domain>

<decisions>
## Implementation Decisions

### ProgressBar fill radius
- Fill (colored portion) must have fully rounded ends matching the track — pill-shaped fill inside pill-shaped track
- Match DWC rendering exactly — reference DWC CSS source for exact radius values
- Current issue: fill clips with square ends despite track having rounded corners

### ComboBox full rework
- Match DWC fully: chevron shape/size, separator line, border framing, overall proportions
- DWC has a thin chevron (small, subtle) on the right side with a subtle separator line
- Current Swing version has a wider arrow area with different coloring — too prominent
- Reference both DWC SCSS/CSS source files AND web screenshots for exact match

### TextField flat border
- Replace current 3D/inset border look with flat, single-pixel border
- Rounded corners matching DWC input styling
- No visible bevel, shadow, or 3D effect on the border

### Tree node icons
- Tree nodes must show BOTH the expand/collapse chevron AND a programmable icon
- Default icon must be visible (not hidden) — BBj renders a default icon when none is explicitly set
- Current issue: when DWC L&F is applied, icons disappear entirely
- Fix must preserve the ability for users to set custom icons per node

### Claude's Discretion
- Exact implementation approach for each fix (which classes to modify, painting strategy)
- Order of fixes within the phase
- Whether to update gallery demo to better showcase fixed rendering

</decisions>

<specifics>
## Specific Ideas

- User provided side-by-side screenshots of DWC web gallery vs Swing gallery for direct visual comparison
- DWC source files are available in this project for reference
- Goal is "recognizably identical" — close enough that the differences don't stand out in normal use

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 11-visual-details*
*Context gathered: 2026-02-11*
