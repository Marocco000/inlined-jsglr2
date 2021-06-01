package org.spoofax.jsglr2.stack.paths;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public abstract class StackPath2 {
//        <ParseForest, StackNode extends IStackNode> {

    public final int length;

    protected StackPath2(int length) {
        this.length = length;
    }

    public abstract boolean isEmpty();

    public abstract HybridStackNode2 head();

    public abstract boolean contains(StackLink<IncrementalParseForest, HybridStackNode2> link);

}
