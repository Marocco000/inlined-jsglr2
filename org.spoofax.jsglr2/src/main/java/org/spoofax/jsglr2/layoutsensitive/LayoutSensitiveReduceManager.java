package org.spoofax.jsglr2.layoutsensitive;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.sdf2table.grammar.LayoutConstraintAttribute;
import org.metaborg.sdf2table.parsetable.ParseTableProduction;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestManager;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.reducing.ReduceManager;
import org.spoofax.jsglr2.stack.AbstractStackManager;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.paths.StackPath;

public class LayoutSensitiveReduceManager
//@formatter:off
   <ParseForest extends LayoutSensitiveParseForest,
    ParseNode   extends ParseForest,
    Derivation  extends IDerivation<ParseForest>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<ParseForest, StackNode>>
//@formatter:on
    extends ReduceManager<ParseForest, ParseNode, Derivation, StackNode, ParseState> {

    public LayoutSensitiveReduceManager(IParseTable parseTable,
        AbstractStackManager<ParseForest, StackNode, ParseState> stackManager,
        ParseForestManager<ParseForest, ParseNode, Derivation, StackNode, ParseState> parseForestManager,
        ParseForestConstruction parseForestConstruction) {
        super(parseTable, stackManager, parseForestManager, parseForestConstruction);
    }

    @Override protected void doReductionsHelper(ParserObserving<ParseForest, StackNode, ParseState> observing,
        ParseState parseState, StackNode stack, IReduce reduce, StackLink<ParseForest, StackNode> throughLink) {
        pathsLoop: for(StackPath<ParseForest, StackNode> path : stackManager.findAllPathsOfLength(stack,
            reduce.arity())) {
            if(throughLink == null || path.contains(throughLink)) {
                StackNode pathBegin = path.head();
                ParseForest[] parseNodes = stackManager.getParseForests(parseForestManager, path);

                if(reduce.production() instanceof ParseTableProduction) {
                    ParseTableProduction sdf2tableProduction = (ParseTableProduction) reduce.production();

                    for(LayoutConstraintAttribute lca : sdf2tableProduction.getLayoutConstraints()) {
                        // Skip the reduction if the constraint evaluates to false
                        if(!LayoutConstraintEvaluator.evaluate(lca.getLayoutConstraint(), parseNodes).orElse(true))
                            continue pathsLoop;
                    }
                }

                reducer(observing, parseState, pathBegin, reduce, parseNodes);
            }
        }
    }

}
