package org.spoofax.jsglr2.parseforest;

public enum ParseForestRepresentation {
    Null, Basic, Hybrid, DataDependent, LayoutSensitive, Composite, Incremental;

    public static ParseForestRepresentation standard() {
        return Hybrid;
    }
}
