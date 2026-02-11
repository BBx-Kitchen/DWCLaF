---
phase: 11-visual-details
plan: 01
subsystem: ui
tags: [swing, progressbar, textfield, tree, icons, css-tokens]

# Dependency graph
requires:
  - phase: 10-more-controls
    provides: ProgressBar, TextField, and Tree component delegates
provides:
  - ProgressBar pill-shaped fill via --dwc-border-radius-xl token mapping
  - TextField flat border verified (already correct)
  - DwcTreeNodeIcon class with FOLDER_OPEN, FOLDER_CLOSED, FILE types
  - Tree.openIcon, Tree.closedIcon, Tree.leafIcon in UIDefaults
affects: [11-visual-details, gallery]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Programmatic stroked-outline tree node icons (DwcTreeNodeIcon pattern)"
    - "Per-component token mapping for arc values (ProgressBar.arc from --dwc-border-radius-xl)"

key-files:
  created:
    - src/main/java/com/dwc/laf/ui/DwcTreeNodeIcon.java
  modified:
    - src/main/resources/com/dwc/laf/token-mapping.properties
    - src/main/java/com/dwc/laf/DwcLookAndFeel.java

key-decisions:
  - "ProgressBar.arc mapped from --dwc-border-radius-xl (12px) instead of --dwc-border-radius (4px) for pill shape"
  - "Pill-shaped fallback of 999 when token is missing (clamps to min(width,height) in createRoundedShape)"
  - "TextField border confirmed already flat via PaintUtils.paintOutline even-odd fill; no code changes needed"
  - "Tree node icons installed in UIDefaults only (not on renderer) to preserve UIResource contract"
  - "16x16px tree node icons with stroked outlines matching DwcTreeExpandIcon visual style"

patterns-established:
  - "DwcTreeNodeIcon: programmatic folder/file icons via Icon interface with enum Type"
  - "Component-specific token mapping: different CSS tokens for same UIDefaults key per component"

# Metrics
duration: 2min
completed: 2026-02-11
---

# Phase 11 Plan 01: Visual Details Summary

**ProgressBar pill-shaped fill via --dwc-border-radius-xl token, TextField flat border verification, and programmatic folder/file tree node icons**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-11T08:08:02Z
- **Completed:** 2026-02-11T08:09:58Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- ProgressBar.arc now maps from --dwc-border-radius-xl (12px) producing pill-shaped track and fill ends
- TextField border verified as mathematically flat (PaintUtils.paintOutline even-odd fill ring); the "3D" appearance is the intentional DWC input background tint (--dwc-input-background hsl(211,38%,95%) vs white panel)
- DwcTreeNodeIcon class created with three icon types: FOLDER_OPEN (open folder with angled flap), FOLDER_CLOSED (closed folder with tab), FILE (document with folded corner)
- Tree.openIcon, Tree.closedIcon, Tree.leafIcon installed in UIDefaults so DefaultTreeCellRenderer displays default icons

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix ProgressBar pill-shaped fill and TextField flat border** - `49a9f25` (feat)
2. **Task 2: Add default tree node icons (folder/file)** - `0a5db5a` (feat)

## Files Created/Modified
- `src/main/java/com/dwc/laf/ui/DwcTreeNodeIcon.java` - Programmatic folder/file icons implementing javax.swing.Icon with three types via enum
- `src/main/resources/com/dwc/laf/token-mapping.properties` - Moved ProgressBar.arc from --dwc-border-radius to --dwc-border-radius-xl line
- `src/main/java/com/dwc/laf/DwcLookAndFeel.java` - Added DwcTreeNodeIcon import, installed Tree.openIcon/closedIcon/leafIcon in UIDefaults, updated ProgressBar.arc fallback to 999

## Decisions Made
- **ProgressBar.arc from --dwc-border-radius-xl:** DWC SCSS uses border-radius-xl (0.75em = 12px) for both track and fill parts. Previous mapping used border-radius (4px) which was too small for pill shape.
- **Pill fallback of 999:** If the token is missing, 999 acts as "maximum rounding" since PaintUtils.createRoundedShape clamps arc to min(width, height), producing fully rounded ends.
- **TextField already correct:** The border uses PaintUtils.paintOutline which creates a flat ring via even-odd fill rule. The subtle "3D" appearance is the intentional DWC input-background tint vs the panel white background.
- **Icons in UIDefaults only:** Following the UIResource contract, icons are set via table.put("Tree.openIcon", ...) not on the renderer directly. This preserves custom icon overrides by application code and survives theme switching.
- **Stroked outlines for icons:** Consistent with DwcTreeExpandIcon's visual style (1.2f stroke, CAP_ROUND, JOIN_ROUND). Color from Tree.expandedIcon.color for matching.

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- ProgressBar, TextField, and Tree visual fixes are complete
- Plan 02 (ComboBox rework) can proceed independently
- All existing tests pass with no regressions

## Self-Check: PASSED

All files verified present. All commit hashes found in git log.

---
*Phase: 11-visual-details*
*Completed: 2026-02-11*
