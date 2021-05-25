package org.spoofax.jsglr2.tokens;

public enum TokenizerVariant {
    Null, Recursive, Iterative, TreeShaped, IncrementalTreeShaped, InlinedIncremental;

    public static TokenizerVariant standard() {
        return Null;
    }
}
