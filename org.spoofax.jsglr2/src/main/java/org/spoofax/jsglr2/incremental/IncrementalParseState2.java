package org.spoofax.jsglr2.incremental;

import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseException;

import org.spoofax.jsglr2.parser.observing.IParserNotification;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

import java.util.Collection;
import java.util.List;

public class IncrementalParseState2
        extends AbstractParseState<IIncrementalInputStack, HybridStackNode2> {//implements IIncrementalParseState {
//        {
    private boolean multipleStates = false;

    public IncrementalParseState2(JSGLR2Request request, IIncrementalInputStack inputStack,
                                  IActiveStacks<HybridStackNode2> activeStacks,
                                  IForActorStacks<HybridStackNode2> forActorStacks) {
        super(request, inputStack, activeStacks, forActorStacks);
    }

//    public static
//    ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack,
//            HybridStackNode2, IncrementalParseState2>
//    factory(ParserVariant variant) {
//        //@formatter:off
//        return factory(
//                new ActiveStacksFactory(variant.activeStacksRepresentation),
//                new ForActorStacksFactory(variant.forActorStacksRepresentation)
//        );
//        //@formatter:on
//    }

//    public static
//    ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack,
//            HybridStackNode2, IncrementalParseState2>
//    factory(IActiveStacksFactory activeStacksFactory, IForActorStacksFactory forActorStacksFactory) {
//        return (request, inputStack, observing) -> {
//            IActiveStacks<HybridStackNode2> activeStacks = activeStacksFactory.get(observing);
//            IForActorStacks<HybridStackNode2> forActorStacks = forActorStacksFactory.get(observing);
//
//            return new IncrementalParseState2(request, inputStack, activeStacks, forActorStacks);
//        };
//    }

    @Override
    public void nextParseRound(ParserObserving observing) throws ParseException {
//        super.nextParseRound(observing);
        observing.notify(observer -> observer.parseRound(this, activeStacks));
        setMultipleStates(activeStacks.isMultiple());
    }


    public void nextParseRound(
                List<IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>> observers
    ) throws ParseException {
        if(!observers.isEmpty()) {
            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.parseRound(this, activeStacks)).notify(observer1);
        }

        setMultipleStates(activeStacks.isMultiple());
    }

    public boolean newParseNodesAreReusable() {
        return !multipleStates;
    }

    public void setMultipleStates(boolean multipleStates) {
        this.multipleStates = multipleStates;
    }

    // from AbstractParseState
    public Collection<Message> postProcessMessages(Collection<Message> messages, ITokens tokens) {
        return messages;
    }

}
