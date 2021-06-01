package org.spoofax.jsglr2.stack.paths;

import org.spoofax.jsglr2.stack.StackLink2;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public abstract class StackPath2 {

    public final int length;

    protected StackPath2(int length) {
        this.length = length;
    }

    public abstract boolean isEmpty();

    public abstract HybridStackNode2 head();

    public abstract boolean contains(StackLink2 link);

}
