package org.spoofax.jsglr2.stack.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.incremental.IncrementalParseState2;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public class ActiveStacksArrayList2
////@formatter:off
//        <ParseForest extends IParseForest,
//                Derivation  extends IDerivation<ParseForest>,
//                ParseNode   extends IParseNode<ParseForest, Derivation>,
//                StackNode   extends IStackNode,
//                ParseState  extends AbstractParseState<?, StackNode>>
////@formatter:on
    implements IActiveStacks<HybridStackNode2>
{

    protected ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode,
            HybridStackNode2, IncrementalParseState2> observing;
    protected List<HybridStackNode2> activeStacks;

    public ActiveStacksArrayList2(ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing) {
        this.observing = observing;
        this.activeStacks = new ArrayList<>();
    }

    @Override public void add(HybridStackNode2 stack) {
        observing.notify(observer -> observer.addActiveStack(stack));

        activeStacks.add(stack);
    }

    @Override public boolean isSingle() {
        return activeStacks.size() == 1;
    }

    @Override public HybridStackNode2 getSingle() {
        return activeStacks.get(0);
    }

    @Override public boolean isEmpty() {
        return activeStacks.isEmpty();
    }

    @Override public boolean isMultiple() {
        return activeStacks.size() > 1;
    }

    @Override public HybridStackNode2 findWithState(IState state) {
        observing.notify(observer -> observer.findActiveStackWithState(state));

        for(HybridStackNode2 stack : activeStacks)
            if(stack.state().id() == state.id())
                return stack;

        return null;
    }

    @Override public Iterable<HybridStackNode2> forLimitedReductions(IForActorStacks<HybridStackNode2> forActorStacks) {
        return () -> new Iterator<HybridStackNode2>() {

            int index = 0;

            // Save the number of active stacks to prevent the for loop from processing active stacks that are added
            // by doLimitedReductions. We can safely limit the loop by the current number of stacks since new stack are
            // added at the end.
            final int currentSize = activeStacks.size();

            @Override public boolean hasNext() {
                // skip non-applicable actions
                while(index < currentSize && !(!activeStacks.get(index).allLinksRejected()
                        && !forActorStacks.contains(activeStacks.get(index)))) {
                    index++;
                }
                return index < currentSize;
            }

            @Override public HybridStackNode2 next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                return activeStacks.get(index++);
            }

        };
    }

    @Override public void addAllTo(IForActorStacks<HybridStackNode2> other) {
        for(HybridStackNode2 stack : activeStacks)
            other.add(stack);
    }

    @Override public void clear() {
        activeStacks.clear();
    }

    @Override public Iterator<HybridStackNode2> iterator() {
        return activeStacks.iterator();
    }

}
