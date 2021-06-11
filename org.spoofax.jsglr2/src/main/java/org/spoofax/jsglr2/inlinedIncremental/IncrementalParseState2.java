package org.spoofax.jsglr2.inlinedIncremental;

import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseException;

import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

import java.util.Collection;

public class IncrementalParseState2
        extends AbstractParseState<IIncrementalInputStack, HybridStackNode2> {//implements IIncrementalParseState {
//        {
    private boolean multipleStates = false;

    public IncrementalParseState2(JSGLR2Request request, IIncrementalInputStack inputStack,
                                  IActiveStacks<HybridStackNode2> activeStacks,
                                  IForActorStacks<HybridStackNode2> forActorStacks) {
        super(request, inputStack, activeStacks, forActorStacks);
    }

    public void nextParseRound() throws ParseException {
        this.multipleStates = activeStacks.isMultiple();
    }

    public boolean newParseNodesAreReusable() {
        return !multipleStates;
    }

//    public void setMultipleStates(boolean multipleStates) {
//        this.multipleStates = multipleStates;
//    }
    public void setMultipleStatesTrue() {
        this.multipleStates = true;
    }

    // from AbstractParseState
    public Collection<Message> postProcessMessages(Collection<Message> messages, ITokens tokens) {
        return messages;
    }

}
