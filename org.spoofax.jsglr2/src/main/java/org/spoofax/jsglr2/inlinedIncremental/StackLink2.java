package org.spoofax.jsglr2.inlinedIncremental;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.inlinedIncremental.HybridStackNode2;

public class StackLink2 {

    public final HybridStackNode2 from; // Farthest away from initial stack node
    public final HybridStackNode2 to; // Closest to initial stack node
    public final IncrementalParseForest parseForest;
    private boolean isRejected;

    public StackLink2(HybridStackNode2 from, HybridStackNode2 to, IncrementalParseForest parseForest) {
        this.from = from;
        this.to = to;
        this.parseForest = parseForest;
        this.isRejected = false;
    }

    public void reject() {
        this.isRejected = true;
    }

    public boolean isRejected() {
        return this.isRejected;
    }

}
