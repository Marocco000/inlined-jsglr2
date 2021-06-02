package org.spoofax.jsglr2.stack.collections;

import java.util.*;

import com.google.common.collect.Iterables;
import org.spoofax.jsglr2.incremental.IncrementalParseState2;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public class ForActorStacksArrayDeque2 implements IForActorStacks<HybridStackNode2> {

    protected final Queue<HybridStackNode2> forActor;

    private final ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing;
    protected final Queue<HybridStackNode2> forActorDelayed;

    public ForActorStacksArrayDeque2(
            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing) {
        this.observing = observing;

        // TODO: implement priority (see P9707 Section 8.4)
        Comparator<HybridStackNode2> stackNodePriorityComparator = (HybridStackNode2 stackNode1, HybridStackNode2 stackNode2) -> 0;

        this.forActorDelayed = new PriorityQueue<>(stackNodePriorityComparator);

        this.forActor = new ArrayDeque<>();
    }

    // from ForActorStacks

    @Override
    public void add(HybridStackNode2 stack) {
        observing.notify(observer -> observer.addForActorStack(stack));

        if (stack.state().isRejectable())
            forActorDelayed.add(stack);
        else
            forActor.add(stack);
    }

    @Override
    public boolean contains(HybridStackNode2 stack) {
        return forActor.contains(stack) || forActorDelayed.contains(stack);
    }

    @Override
    public boolean nonEmpty() {
        return !forActor.isEmpty() || !forActorDelayed.isEmpty();
    }

    @Override
    public HybridStackNode2 remove() {
        // First return all actors in forActor
        if (!forActor.isEmpty())
            return forActor.remove();

        // Then return actors from forActorDelayed
        return forActorDelayed.remove();
    }

    @Override
    public Iterator<HybridStackNode2> iterator() {
        return Iterables.concat(forActor, forActorDelayed).iterator();
    }
}
