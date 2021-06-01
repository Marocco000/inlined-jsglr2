package org.spoofax.jsglr2.stack.hybrid;

import java.util.ArrayList;
import java.util.Collections;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.util.iterators.SingleElementWithListIterable;

public class HybridStackNode2
//        extends AbstractStackNode<IncrementalParseForest, HybridStackNode2> {
        implements IStackNode {

    private StackLink<IncrementalParseForest, HybridStackNode2> firstLink;
    private ArrayList<StackLink<IncrementalParseForest, HybridStackNode2>> otherLinks;

    public final IState state;

    public HybridStackNode2(IState state) {
        this.state = state;
    }

    public Iterable<StackLink<IncrementalParseForest, HybridStackNode2>> getLinks() {
        if(otherLinks == null) {
            return Collections.singleton(firstLink);
        } else {
            return SingleElementWithListIterable.of(firstLink, otherLinks);
        }
    }

    public StackLink<IncrementalParseForest, HybridStackNode2>
    addLink(StackLink<IncrementalParseForest, HybridStackNode2> link) {
        if(firstLink == null)
            firstLink = link;
        else {
            if(otherLinks == null)
                otherLinks = new ArrayList<>();

            otherLinks.add(link);
        }

        return link;
    }

    @Override public boolean allLinksRejected() {
        if(firstLink == null || !firstLink.isRejected())
            return false;

        if(otherLinks == null)
            return true;

        for(StackLink<IncrementalParseForest, HybridStackNode2> link : otherLinks) {
            if(!link.isRejected())
                return false;
        }

        return true;
    }

    // from abstract stack node

    public IState state() {
        return state;
    }

//    public StackLink<IncrementalParseForest, HybridStackNode2> addLink(HybridStackNode2 parent, IncrementalParseForest parseNode) {
//        StackLink<IncrementalParseForest, HybridStackNode2> link = new StackLink<>( this, parent, parseNode);
//
//        return addLink(link);
//    }

}
