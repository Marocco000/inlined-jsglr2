package org.spoofax.jsglr2.recovery;

import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.failure.IParseFailureHandler;
import org.spoofax.jsglr2.parser.failure.ParseFailureHandlerFactory;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.parser.result.ParseFailureType;
import org.spoofax.jsglr2.stack.IStackNode;

public class RecoveryParseFailureHandler
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<ParseForest, StackNode> & IRecoveryParseState<ParseForest, StackNode>>
//@formatter:on
    implements IParseFailureHandler<ParseForest, StackNode, ParseState> {

    public static
//@formatter:off
   <ParseForest_ extends IParseForest,
    Derivation_  extends IDerivation<ParseForest_>,
    ParseNode_   extends IParseNode<ParseForest_, Derivation_>,
    StackNode_   extends IStackNode,
    ParseState_  extends AbstractParseState<ParseForest_, StackNode_> & IRecoveryParseState<ParseForest_, StackNode_>>
//@formatter:on
    ParseFailureHandlerFactory<ParseForest_, Derivation_, ParseNode_, StackNode_, ParseState_>
    factory() {
        return RecoveryParseFailureHandler::new;
    }

    ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing;

    RecoveryParseFailureHandler(ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing) {
        this.observing = observing;
    }

    @Override public boolean onFailure(ParseState parseState) {
        if(!parseState.isRecovering())
            parseState.startRecovery(parseState.currentOffset);

        return parseState.nextRecoveryIteration();
    }

    @Override public ParseFailureType failureType(ParseState parseState) {
        return ParseFailureType.Unknown;
    }

}
