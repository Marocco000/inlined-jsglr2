package org.spoofax.jsglr2.imploder;

import static org.spoofax.interpreter.terms.IStrategoTerm.MUTABLE;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.terms.TermFactory;

public abstract class StrategoTermImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends ParseForest,
    Derivation  extends IDerivation<ParseForest>>
//@formatter:on
    extends TokenizedTreeImploder<ParseForest, ParseNode, Derivation, IStrategoTerm> {

    public StrategoTermImploder() {
        super(new TermTreeFactory(new TermFactory().getFactoryWithStorageType(MUTABLE)));
    }

    @Override protected void tokenTreeBinding(IToken token, IStrategoTerm term) {
        token.setAstNode(term);
    }

}
