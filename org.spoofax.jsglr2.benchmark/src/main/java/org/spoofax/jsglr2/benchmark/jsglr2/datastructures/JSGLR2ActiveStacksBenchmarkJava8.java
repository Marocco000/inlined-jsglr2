package org.spoofax.jsglr2.benchmark.jsglr2.datastructures;

import org.spoofax.jsglr2.benchmark.BenchmarkTestSetReader;
import org.spoofax.jsglr2.testset.TestSet;

public class JSGLR2ActiveStacksBenchmarkJava8 extends JSGLR2ActiveStacksBenchmark {

    public JSGLR2ActiveStacksBenchmarkJava8() {
        this.testSetReader = new BenchmarkTestSetReader<>(TestSet.java8);
    }

}
