package org.spoofax.jsglr2.recoveryincremental;

import org.spoofax.jsglr2.incremental.IIncrementalParseState;
import org.spoofax.jsglr2.incremental.lookaheadstack.ILookaheadStack;
import org.spoofax.jsglr2.incremental.lookaheadstack.LinkedLookaheadStack;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseStateFactory;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.recovery.AbstractRecoveryParseState;
import org.spoofax.jsglr2.recovery.IRecoveryParseState;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.ActiveStacksFactory;
import org.spoofax.jsglr2.stack.collections.ForActorStacksFactory;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

public class RecoveryIncrementalParseState
//@formatter:off
   <ParseForest extends IParseForest,
    StackNode   extends IStackNode>
//@formatter:on
    extends AbstractRecoveryParseState<ParseForest, StackNode, IncrementalBacktrackChoicePoint<StackNode>>
    implements IIncrementalParseState {

    private boolean multipleStates = false;
    ILookaheadStack lookahead;

    RecoveryIncrementalParseState(String inputString, String filename, IActiveStacks<StackNode> activeStacks,
        IForActorStacks<StackNode> forActorStacks) {
        super(inputString, filename, activeStacks, forActorStacks);
    }

    public static
//@formatter:off
   <ParseForest_ extends IParseForest,
    Derivation_  extends IDerivation<ParseForest_>,
    ParseNode_   extends IParseNode<ParseForest_, Derivation_>,
    StackNode_   extends IStackNode,
    ParseState_  extends AbstractParseState<ParseForest_, StackNode_> & IRecoveryParseState<StackNode_, IncrementalBacktrackChoicePoint<StackNode_>> & IIncrementalParseState>
//@formatter:on
    ParseStateFactory<ParseForest_, Derivation_, ParseNode_, StackNode_, ParseState_> factory(ParserVariant variant) {
        return (inputString, filename, observing) -> {
            IActiveStacks<StackNode_> activeStacks =
                new ActiveStacksFactory(variant.activeStacksRepresentation).get(observing);
            IForActorStacks<StackNode_> forActorStacks =
                new ForActorStacksFactory(variant.forActorStacksRepresentation).get(observing);

            return (ParseState_) new RecoveryIncrementalParseState<>(inputString, filename, activeStacks,
                forActorStacks);
        };
    }

    @Override public void initializeBacktrackChoicePoints(String input) {
        backtrackChoicePoints = new IncrementalBacktrackChoicePoint[inputLineCount(input)];
    }

    protected IncrementalBacktrackChoicePoint<StackNode> createBacktrackChoicePoint(int offset,
        Iterable<StackNode> activeStacks) {
        return new IncrementalBacktrackChoicePoint<>(++lastBacktrackChoicePointIndex, offset, activeStacks);
    }

    @Override public void initParse(IncrementalParseForest updatedTree, String inputString) {
        this.lookahead = new LinkedLookaheadStack(updatedTree, inputString);
        this.currentChar = lookahead.actionQueryCharacter();
    }

    @Override public String actionQueryLookahead(int length) {
        return lookahead.actionQueryLookahead(length);
    }

    @Override public boolean hasNext() {
        return lookahead.get() != null; // null is the lookahead of the EOF node
    }

    @Override public void next() {
        currentOffset += lookahead.get().width();
        lookahead.popLookahead();
        currentChar = lookahead.actionQueryCharacter();
    }

    @Override public ILookaheadStack lookahead() {
        return lookahead;
    }

    @Override public boolean isMultipleStates() {
        return multipleStates;
    }

    @Override public void setMultipleStates(boolean multipleStates) {
        this.multipleStates = multipleStates;
    }

}
