package org.spoofax.jsglr2.stack.paths;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public class EmptyStackPath2
            extends StackPath2{

    private final HybridStackNode2 stackNode;

    public EmptyStackPath2(HybridStackNode2 stackNode) {
        super(0);
        this.stackNode = stackNode;
    }

    @Override public boolean isEmpty() {
        return true;
    }

    @Override public HybridStackNode2 head() {
        return this.stackNode;
    }

    @Override public boolean contains(StackLink<IncrementalParseForest, HybridStackNode2> link) {
        return false;
    }

}
