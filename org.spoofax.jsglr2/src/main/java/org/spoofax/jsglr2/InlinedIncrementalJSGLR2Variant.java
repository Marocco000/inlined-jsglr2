package org.spoofax.jsglr2;

import org.metaborg.parsetable.IParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.ImploderVariant;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.*;
import org.spoofax.jsglr2.tokens.TokenizerVariant;


public class InlinedIncrementalJSGLR2Variant extends JSGLR2Variant {
    public InlinedIncrementalJSGLR2Variant() {
        //For the recoveryIncremental variant change parameters with comments
        super(new ParserVariant(
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
        return new InlinedIncrementalJSGLR2(parseTable);
    }
}
