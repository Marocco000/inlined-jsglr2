package org.spoofax.jsglr2;

import org.metaborg.parsetable.IParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.ImploderVariant;
import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.incremental.IncrementalParser;
import org.spoofax.jsglr2.incremental.IncrementalReduceManager;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForestManager;
import org.spoofax.jsglr2.inputstack.IInputStack;
import org.spoofax.jsglr2.inputstack.InputStack;
import org.spoofax.jsglr2.inputstack.InputStackFactory;
import org.spoofax.jsglr2.inputstack.LayoutSensitiveInputStack;
import org.spoofax.jsglr2.inputstack.incremental.EagerIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.inputstack.incremental.IncrementalInputStackFactory;
import org.spoofax.jsglr2.inputstack.incremental.LinkedIncrementalInputStack;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parser.EmptyParseReporter;
import org.spoofax.jsglr2.parser.Parser;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.parser.failure.DefaultParseFailureHandler;
import org.spoofax.jsglr2.reducing.ReduceActionFilter;
import org.spoofax.jsglr2.reducing.ReducerOptimized;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.*;
import org.spoofax.jsglr2.stack.hybrid.HybridStackManager;
import org.spoofax.jsglr2.tokens.TokenizerVariant;


public class InlinedIncrementalJSGLR2Variant extends JSGLR2Variant {
    public InlinedIncrementalJSGLR2Variant(){
        //For the recoveryIncremental variant change parameters with comments
        super (new ParserVariant(
                            ActiveStacksRepresentation.standard(),
                            ForActorStacksRepresentation.standard(),
                            ParseForestRepresentation.Incremental,
                            ParseForestConstruction.standard(),
                            StackRepresentation.Hybrid,
                            Reducing.Incremental,
                            false),//true
                ImploderVariant.RecursiveIncremental,
                TokenizerVariant.IncrementalTreeShaped);//.Recursive
    }

    @Override
    public JSGLR2<IStrategoTerm> getJSGLR2(IParseTable parseTable) {

//        IActiveStacksFactory activeStacksFactory = new ActiveStacksFactory(ActiveStacksRepresentation.ArrayList);
//        IForActorStacksFactory forActorStacksFactory =  new ForActorStacksFactory(ForActorStacksRepresentation.ArrayDeque);
//
//        IncrementalInputStackFactory<IIncrementalInputStack> incrementalInputStackFactory =
//                EagerIncrementalInputStack::new; // TODO switch between Eager, Lazy, and Linked?
//
//        Parser parser = (Parser)
//
//                new IncrementalParser<>(
//                        incrementalInputStackFactory,
//                        IncrementalParseState.factory(activeStacksFactory, forActorStacksFactory),
//                        parseTable,
//                        HybridStackManager.factory(),
//                        IncrementalParseForestManager::new,
//                        null,
//                        IncrementalReduceManager.factoryIncremental(ReducerOptimized::new),
////                       (parseTable1, stackManager, parseForestManager) -> new IncrementalReduceManager<>(parseTable1,
////                                stackManager, parseForestManager, ReducerOptimized::new),
//
//                        DefaultParseFailureHandler::new,
//                        EmptyParseReporter.factory());
//
//        parser.reduceManager.addFilter(ReduceActionFilter.ignoreRecoveryAndCompletion());
//        return new InlinedIncrementalJSGLR2(parser);
        return new InlinedIncrementalJSGLR2(parseTable);

    }

}
