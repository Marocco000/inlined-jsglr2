package org.spoofax.jsglr2.inlinedIncremental;

public abstract class StackPath2 {

    public final int length;

    protected StackPath2(int length) {
        this.length = length;
    }

    public abstract boolean isEmpty();

    public abstract HybridStackNode2 head();

    public abstract boolean contains(StackLink2 link);

}
