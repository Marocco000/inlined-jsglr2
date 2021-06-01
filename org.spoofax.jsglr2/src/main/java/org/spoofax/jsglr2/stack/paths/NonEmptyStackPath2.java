package org.spoofax.jsglr2.stack.paths;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public class NonEmptyStackPath2
//        <ParseForest, StackNode extends IStackNode> extends StackPath2<ParseForest, StackNode> {
                extends StackPath2{
    public final StackPath2 tail;
    public final StackLink<IncrementalParseForest, HybridStackNode2> link;

    public NonEmptyStackPath2(StackLink<IncrementalParseForest, HybridStackNode2> stackLink, StackPath2 tail) {
        super(tail.length + 1);
        this.tail = tail;
        this.link = stackLink;
    }

    @Override public boolean isEmpty() {
        return false;
    }

    @Override public HybridStackNode2 head() {
        return this.link.to;
    }

    @Override public boolean contains(StackLink<IncrementalParseForest, HybridStackNode2> link) {
        return this.link == link || (this.tail != null && this.tail.contains(link));
    }

}
