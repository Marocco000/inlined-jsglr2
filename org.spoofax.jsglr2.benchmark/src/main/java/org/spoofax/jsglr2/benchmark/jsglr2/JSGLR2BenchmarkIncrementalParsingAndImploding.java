package org.spoofax.jsglr2.benchmark.jsglr2;

import org.metaborg.parsetable.query.ActionsForCharacterRepresentation;
import org.metaborg.parsetable.query.ProductionToGotoRepresentation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;
import org.spoofax.jsglr2.JSGLR2Variants.ParserVariant;
import org.spoofax.jsglr2.benchmark.BenchmarkTestSetReader;
import org.spoofax.jsglr2.imploder.ImploderVariant;
import org.spoofax.jsglr2.incremental.IncrementalParser;
import org.spoofax.jsglr2.integration.IntegrationVariant;
import org.spoofax.jsglr2.integration.ParseTableVariant;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.ActiveStacksRepresentation;
import org.spoofax.jsglr2.stack.collections.ForActorStacksRepresentation;
import org.spoofax.jsglr2.testset.TestSet;
import org.spoofax.jsglr2.testset.testinput.IncrementalStringInput;
import org.spoofax.jsglr2.tokens.TokenizerVariant;

public abstract class JSGLR2BenchmarkIncrementalParsingAndImploding
    extends JSGLR2Benchmark<String[], IncrementalStringInput> {

    protected JSGLR2BenchmarkIncrementalParsingAndImploding(TestSet<String[], IncrementalStringInput> testSet) {
        super(new BenchmarkTestSetReader<>(testSet));
    }

    @Param({ "true" }) public boolean implode;

    @Param({ "DisjointSorted" }) ActionsForCharacterRepresentation actionsForCharacterRepresentation;

    @Param({ "JavaHashMap" }) ProductionToGotoRepresentation productionToGotoRepresentation;

    @Param({ "ArrayList" }) public ActiveStacksRepresentation activeStacksRepresentation;

    @Param({ "ArrayDeque" }) public ForActorStacksRepresentation forActorStacksRepresentation;

    @Param({ "Hybrid", "Incremental" }) public ParseForestRepresentation parseForestRepresentation;

    @Param({ "Full" }) public ParseForestConstruction parseForestConstruction;

    @Param({ "Hybrid" }) public StackRepresentation stackRepresentation;

    @Param({ "Basic" }) public Reducing reducing;

    @Param({ "TokenizedRecursive", "RecursiveIncremental" }) public ImploderVariant imploder;

    @Override protected IntegrationVariant variant() {
        if(!implode)
            throw new IllegalStateException("this variant is not used for benchmarking");

        return new IntegrationVariant(
            new ParseTableVariant(actionsForCharacterRepresentation, productionToGotoRepresentation),
            new ParserVariant(activeStacksRepresentation, forActorStacksRepresentation, parseForestRepresentation,
                parseForestConstruction, stackRepresentation, reducing),
            imploder, TokenizerVariant.Null);
    }

    @Override protected boolean implode() {
        return implode;
    }

    @Override protected Object action(Blackhole bh, IncrementalStringInput input) throws ParseException {
        if(jsglr2.parser instanceof IncrementalParser)
            ((IncrementalParser) jsglr2.parser).clearCache();

        for(String content : input.content) {
            bh.consume(jsglr2.parseUnsafe(content, input.filename, null));
        }
        return null;
    }

}
