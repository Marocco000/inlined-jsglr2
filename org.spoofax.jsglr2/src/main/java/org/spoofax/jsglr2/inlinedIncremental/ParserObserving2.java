package org.spoofax.jsglr2.inlinedIncremental;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parser.observing.IParserNotification;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode;

public class ParserObserving2 {

    private final List<IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>>> observers;

    public ParserObserving2() {
        this.observers = new ArrayList<>();
    }

    public void notify(IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> notification) {
        if(observers.isEmpty())
            return;

        for(IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observer : observers)
            notification.notify(observer);
    }

    public void attachObserver(IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observer) {
        observers.add(observer);
    }

}
