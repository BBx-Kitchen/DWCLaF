package com.dwc.laf.defaults;

import java.util.Collections;
import java.util.List;

/**
 * A mapping entry: one CSS token name mapped to one or more UIDefaults targets.
 *
 * <p>For example, {@code --dwc-border-radius} might map to
 * {@code [Button.arc:INT, Component.arc:INT, CheckBox.arc:INT]}.</p>
 *
 * @param cssTokenName the CSS custom property name (e.g., "--dwc-color-primary")
 * @param targets      one or more UIDefaults targets (unmodifiable)
 */
public record MappingEntry(String cssTokenName, List<MappingTarget> targets) {

    /**
     * Creates a MappingEntry with an unmodifiable copy of the targets list.
     */
    public MappingEntry {
        if (cssTokenName == null || cssTokenName.isBlank()) {
            throw new IllegalArgumentException("cssTokenName must not be null or blank");
        }
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("targets must not be null or empty");
        }
        targets = Collections.unmodifiableList(List.copyOf(targets));
    }
}
