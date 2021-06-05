package org.spoofax.jsglr2.inlinedIncremental;

import java.util.Collections;

import org.metaborg.parsetable.symbols.IMetaVarSymbol;
import org.metaborg.parsetable.symbols.ISymbol;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;

import com.google.common.collect.Iterables;

public class StrategoTermTreeFactoryInlined {

    private final ITermFactory termFactory;

    public StrategoTermTreeFactoryInlined() {
        this.termFactory = new TermFactory();
    }

    public IStrategoTerm createCharacterTerminal(int character) {
        return termFactory.makeInt(character);
    }

    public IStrategoTerm createStringTerminal(ISymbol symbol, String value) {
        return termFactory.makeString(value);
    }

    public IStrategoTerm createMetaVar(IMetaVarSymbol symbol, String value) {
        return termFactory.makeAppl(symbol.metaVarCardinality().constructor, termFactory.makeString(value));
    }

    public IStrategoTerm createNonTerminal(ISymbol symbol, String constructor,
                                           Iterable<IStrategoTerm> childASTs) {
        IStrategoTerm[] terms = Iterables.toArray(childASTs, IStrategoTerm.class);
        return termFactory.makeAppl(
                termFactory.makeConstructor(constructor != null ? constructor : ISymbol.getSort(symbol), terms.length),
                terms);
    }

    public IStrategoTerm createList(Iterable<IStrategoTerm> children) {
        return termFactory.makeList(Iterables.toArray(children, IStrategoTerm.class));
    }

    public IStrategoTerm createOptional(ISymbol symbol, Iterable<IStrategoTerm> children) {
        return createNonTerminal(symbol, children == null || Iterables.isEmpty(children) ? "None" : "Some", children);
    }

    public IStrategoTerm createTuple(Iterable<IStrategoTerm> children) {
        return termFactory.makeTuple(Iterables.toArray(children, IStrategoTerm.class));
    }

    public IStrategoTerm createAmb(Iterable<IStrategoTerm> alternatives) {
        return createNonTerminal(null, "amb", Collections.singletonList(createList(alternatives)));
    }

//    private static IStrategoTerm[] toArray(Iterable<IStrategoTerm> children) {
//        return Iterables.toArray(children, IStrategoTerm.class);
//    }

}
