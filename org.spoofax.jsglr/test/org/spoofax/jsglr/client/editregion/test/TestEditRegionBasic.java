package org.spoofax.jsglr.client.editregion.test;

import junit.framework.Assert;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.ParseException;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;

public class TestEditRegionBasic extends AbstractTestRegionDetection {
	public TestEditRegionBasic() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException{
		pathToJavaTestInputs = "tests-editregions/java/basic"; 
		setJavaParser();
	} 

	@Test
	public void testInsertedRegion() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/insertion.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		Assert.assertEquals("if(v > 5){\n\t\t", concatenated);
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

	@Test
	public void testDeletedRegion() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/deletion.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		Assert.assertEquals("v+=\n\t\t", concatenated);
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

	@Test
	public void testMultipleDeletedRegions() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/multiple-deletions.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		//System.out.println(lastErr0AST);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		//Assert.assertEquals("package \n\t\t= 10;\n\t\tSystem..println(v);", concatenated);
		Assert.assertEquals("package = .", concatenated);
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

	@Test
	public void testNestedDeletedRegions() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/nested-deletions.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		Assert.assertEquals("private void m(int x, int y, int z{\n		int v = 10;\n		v+= \n		System.out.println(v);\n	}", concatenated);
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

	@Test
	public void testDeletionAndInsertion() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/deletion-and-insertion.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		Assert.assertEquals(" + 5 +\n\t\tSystem.out.println(v", concatenated);
		//System.out.println(editRegionRecovery.getRecoveredInput());
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

	@Test
	public void testReplacement() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/replacement.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		Assert.assertEquals("v = v +;\n\t\t", concatenated);
		Assert.assertEquals(127, editRegionRecovery.getCorrectInput().length());
		Assert.assertEquals(128, editRegionRecovery.getErroneousInput().length());
		Assert.assertEquals(128, editRegionRecovery.getRecoveredInput().length());
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

	@Test
	public void testDelInsNesting() throws IOException, InvalidParseTableException, TokenExpectedException, BadTokenException, ParseException, SGLRException {
		String pathToErroneousFile = pathToJavaTestInputs + "/del-ins-nesting.java";
		String erroneousInput = loadAsString(pathToErroneousFile);
		ArrayList<Integer> discardOffsets = super.getDiscardOffsets(lastErr0AST, erroneousInput);
		String concatenated = concatenatedDiscardChars(erroneousInput, discardOffsets);
		Assert.assertEquals("private void m(int x, int y, int z{\n		int v = 10;\n		v+= 10;\n		print(\n		System.out.println(v);\n	}", concatenated);
		super.parseString(editRegionRecovery.getRecoveredInput());
	}

}