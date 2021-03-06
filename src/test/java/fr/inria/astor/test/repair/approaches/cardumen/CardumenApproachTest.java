package fr.inria.astor.test.repair.approaches.cardumen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import fr.inria.astor.approaches.cardumen.CardumenApproach;
import fr.inria.astor.approaches.cardumen.CardumenExhaustiveEngine4Stats;
import fr.inria.astor.approaches.ingredientbased.IngredientBasedApproach;
import fr.inria.astor.approaches.ingredientbased.IngredientBasedRepairApproachImpl;
import fr.inria.astor.approaches.jgenprog.operators.ExpressionReplaceOperator;
import fr.inria.astor.core.entities.Ingredient;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.loop.AstorCoreEngine;
import fr.inria.astor.core.loop.spaces.ingredients.ingredientSearch.EfficientIngredientStrategy;
import fr.inria.astor.core.loop.spaces.ingredients.ingredientSearch.ProbabilisticIngredientStrategy;
import fr.inria.astor.core.loop.spaces.ingredients.scopes.ExpressionTypeIngredientSpace;
import fr.inria.astor.core.loop.spaces.ingredients.scopes.IngredientSpaceScope;
import fr.inria.astor.core.loop.spaces.ingredients.transformations.DynamicIngredient;
import fr.inria.astor.core.loop.spaces.ingredients.transformations.ProbabilisticTransformationStrategy;
import fr.inria.astor.core.loop.spaces.ingredients.transformations.RandomTransformationStrategy;
import fr.inria.astor.core.loop.spaces.operators.AstorOperator;
import fr.inria.astor.core.manipulation.sourcecode.VarCombinationForIngredient;
import fr.inria.astor.core.manipulation.sourcecode.VarMapping;
import fr.inria.astor.core.manipulation.sourcecode.VariableResolver;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.stats.Stats;
import fr.inria.astor.test.repair.evaluation.regression.ClosureTest;
import fr.inria.astor.test.repair.evaluation.regression.MathCommandsTests;
import fr.inria.astor.util.CommandSummary;
import fr.inria.main.AstorOutputStatus;
import fr.inria.main.ExecutionMode;
import fr.inria.main.evolution.AstorMain;
import spoon.reflect.code.CtCodeElement;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtVariable;

/**
 * 
 * @author Matias Martinez
 *
 */
public class CardumenApproachTest {

	protected Logger log = Logger.getLogger(this.getClass().getName());

	@Test
	public void testCardumentM70() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();
		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		ProbabilisticIngredientStrategy ingredientStrategy = (ProbabilisticIngredientStrategy) cardumen
				.getIngredientSearchStrategy();

		assertNotNull(ingredientStrategy);

		ProbabilisticTransformationStrategy probTransformation = (ProbabilisticTransformationStrategy) cardumen
				.getIngredientTransformationStrategy();
		assertNotNull(probTransformation);

		assertTrue(Boolean.valueOf(ConfigurationProperties.getProperty("cleantemplates")));

		assertEquals(1, cardumen.getOperatorSpace().size());

		assertTrue(ExpressionReplaceOperator.class.isInstance(cardumen.getOperatorSpace().getOperators().get(0)));

	}

	@Test
	public void testCardumentM70ScopeLocal() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);

		CtElement codeElement0 = pvar.getModificationPoints().stream()
				.filter(e -> e.getCodeElement().toString().equals("solve(min, max)")).findFirst().get()
				.getCodeElement();
		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(codeElement0);

		List<String> locations = ingredientSpace.getLocations();

		log.info("Locations " + locations);

		for (CtCodeElement ingredientElement : ingredients) {
			assertEquals(codeElement0.getParent(CtType.class).getQualifiedName(),
					ingredientElement.getParent(CtType.class).getQualifiedName());
			assertEquals(((CtExpression) codeElement0).getType().getQualifiedName(),
					((CtExpression) ingredientElement).getType().getQualifiedName());
			assertEquals("org.apache.commons.math.analysis.solvers.BisectionSolver",
					ingredientElement.getParent(CtType.class).getQualifiedName());
		}

		///
		String p1 = "org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl";
		ModificationPoint mp40 = pvar.getModificationPoints().stream()
				.filter(e -> e.getCtClass().getQualifiedName().equals(p1)).findFirst().get();
		CtElement codeElement40 = mp40.getCodeElement();
		assertEquals(p1, mp40.getCtClass().getQualifiedName());
		List<CtCodeElement> ingredients40 = ingredientSpace.getIngredients(codeElement40);

		List<?> locations40 = ingredientSpace.getLocations();

		boolean withName = false;
		for (Object location : locations40) {
			if (((MultiKey) location).getKey(0).toString()
					.contains("org.apache.commons.math.analysis.solvers.UnivariateRealSolverImp"))
				withName = true;
		}
		assertTrue(withName);

		log.info("Locations " + locations40);

		for (CtCodeElement ingredientElement : ingredients40) {
			assertEquals(codeElement40.getParent(CtType.class).getQualifiedName(),
					ingredientElement.getParent(CtType.class).getQualifiedName());
			assertEquals(((CtExpression) codeElement40).getType().getQualifiedName(),
					((CtExpression) ingredientElement).getType().getQualifiedName());
			assertEquals("org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl",
					ingredientElement.getParent(CtType.class).getQualifiedName());
		}

	}

	@Test
	public void testCardumentM70ScopePackage() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);

		CtElement codeElement0 = pvar.getModificationPoints().stream()
				.filter(e -> e.getCodeElement().toString().equals("solve(min, max)")).findFirst().get()
				.getCodeElement();

		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(codeElement0);

		List<String> locations = ingredientSpace.getLocations();

		log.info("Locations " + locations);

		for (CtCodeElement ingredientElement : ingredients) {
			assertEquals(codeElement0.getParent(CtPackage.class).getQualifiedName(),
					ingredientElement.getParent(CtPackage.class).getQualifiedName());
			assertEquals(((CtExpression) codeElement0).getType().getQualifiedName(),
					((CtExpression) ingredientElement).getType().getQualifiedName());
			assertEquals("org.apache.commons.math.analysis.solvers",
					ingredientElement.getParent(CtPackage.class).getQualifiedName());
		}

		///

		String p1 = "org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl";
		ModificationPoint mp40 = pvar.getModificationPoints().stream()
				.filter(e -> e.getCtClass().getQualifiedName().equals(p1)).findFirst().get();
		assertEquals("org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl",
				mp40.getCtClass().getQualifiedName());
		CtElement codeElement40 = mp40.getCodeElement();

		List<CtCodeElement> ingredients40 = ingredientSpace.getIngredients(codeElement40);

		List<?> locations40 = ingredientSpace.getLocations();

		boolean withName = false;
		for (Object location : locations40) {
			if (((MultiKey) location).getKey(0).toString().equals("org.apache.commons.math.analysis.solvers"))
				withName = true;
		}
		assertTrue(withName);

		log.info("Locations " + locations40);

		for (CtCodeElement ingredientElement : ingredients40) {
			assertEquals(codeElement40.getParent(CtPackage.class).getQualifiedName(),
					ingredientElement.getParent(CtPackage.class).getQualifiedName());
			assertEquals(((CtExpression) codeElement40).getType().getQualifiedName(),
					((CtExpression) ingredientElement).getType().getQualifiedName());
			assertEquals("org.apache.commons.math.analysis.solvers",
					ingredientElement.getParent(CtPackage.class).getQualifiedName());
		}

	}

	@Test
	public void testCardumentM70TransformationBugSameCombination() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "disablelog:true");
		int maxCombinations = 30000;
		command.command.put("-parameters", "maxVarCombination:" + maxCombinations + ":disablelog:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);
		int mpi = 0;
		for (ModificationPoint mp : pvar.getModificationPoints()) {

			log.info("--mp " + (mpi) + " [" + mp.getCodeElement().getClass().getCanonicalName() + "] " + mp);
			CtElement codeElement0 = mp.getCodeElement();
			List<CtCodeElement> ingredients = ingredientSpace.getIngredients(codeElement0);
			int t = 0;
			for (CtCodeElement ctCodeElement : ingredients) {

				log.info("--Template-->" + ctCodeElement);

				Ingredient ingredientToModify = new Ingredient(ctCodeElement);

				List<Ingredient> ingredientsTransformed = cardumen.getIngredientTransformationStrategy().transform(mp,
						ingredientToModify);
				log.info("--mp " + (mpi) + " [" + mp.getCodeElement().getClass().getCanonicalName() + "] " + mp);

				log.info("--Template-" + (t++) + " ->" + ctCodeElement);

				Set<String> ing = new HashSet<>();
				int c = 0;
				for (Ingredient ingredientInstantiated : ingredientsTransformed) {
					c++;
					if (c < 100)
						log.info("------Instance-->" + ingredientInstantiated.getCode());

					// TODO: Should not be duplicates
					// assertFalse(ing.contains(ingredientInstantiated.getCode().toString()));
					ing.add(ingredientInstantiated.getCode().toString());

				}
				mpi++;
			}

		}

	}

	@Test
	public void testCardumentM70TransformationBugSameCombination2() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "disablelog:true");
		int maxCombinations = 30000;
		command.command.put("-parameters", "maxVarCombination:" + maxCombinations + ":disablelog:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);
		int mpi = 0;
		ModificationPoint mp = pvar.getModificationPoints().get(0);

		log.info("--mp " + (mpi) + " [" + mp.getCodeElement().getClass().getCanonicalName() + "] " + mp);
		CtElement codeElement0 = mp.getCodeElement();
		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(codeElement0);
		int t = 0;
		CtCodeElement ctCodeElement = ingredients.stream()
				.filter(e -> e.toString().equals("solve(_UnivariateRealFunction_0, _double_1, _double_2)")).findFirst()
				.get();

		log.info("--Template-->" + ctCodeElement);

		Ingredient ingredientToModify = new Ingredient(ctCodeElement);

		List<Ingredient> ingredientsTransformed = cardumen.getIngredientTransformationStrategy().transform(mp,
				ingredientToModify);
		log.info("--mp " + (mpi) + " [" + mp.getCodeElement().getClass().getCanonicalName() + "] " + mp);

		log.info("--Template-" + (t++) + " ->" + ctCodeElement);

		Set<String> ing = new HashSet<>();

		for (Ingredient ingredientInstantiated : ingredientsTransformed) {

			log.info("------Instance-->" + ingredientInstantiated.getCode());

			// TODO: Should not be duplicates
			// assertFalse(ing.contains(ingredientInstantiated.getCode().toString()));
			ing.add(ingredientInstantiated.getCode().toString());

		}

	}

	@Test
	public void testCardumentM70bugHEAP() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		int maxCombinations = 3000;
		command.command.put("-parameters", "maxVarCombination:" + maxCombinations + ":disablelog:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		assertEquals((Integer) maxCombinations, (Integer) ConfigurationProperties.getPropertyInt("maxVarCombination"));

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);

		ModificationPoint mp = pvar.getModificationPoints().stream()
				.filter(e -> e.getCodeElement().toString().equals("i < (maximalIterationCount)")).findFirst().get();
		CtElement codeElement0 = mp.getCodeElement();
		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(codeElement0);

		String template = "((_double_0 < _double_1) || (_double_0 > _double_2))";

		CtCodeElement ctCodeElement = ingredients.stream().filter(e -> e.toString().equals(template)).findFirst().get();

		log.info("MP code: " + codeElement0);

		log.info("--Template-->" + ctCodeElement);

		Ingredient ingredientToModify = new Ingredient(ctCodeElement);

		List<Ingredient> ingredientsTransformed = cardumen.getIngredientTransformationStrategy().transform(mp,
				ingredientToModify);

		// 3_13 = 2197
		assertEquals(2197, ingredientsTransformed.size());
		assertTrue(ingredientsTransformed.size() + "vs " + maxCombinations,
				ingredientsTransformed.size() <= maxCombinations);

		Set<String> ing = new HashSet<>();

		int i = 0;
		for (Ingredient ingredient : ingredientsTransformed) {
			log.info("--Ing-->" + (i++) + " " + ingredient);
			String transformation = ingredient.getCode().toString();
			assertFalse(ing.contains(transformation));
			ing.add(transformation);

			assertFalse("((max < min) || (min > max))".equals(transformation));
		}

	}

	@Test
	@Ignore
	public void testCardumentM70InspectIngredients() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);
		int mpi = 0;
		ModificationPoint mp = pvar.getModificationPoints().get(245);

		log.info("--mp " + (mpi) + " [" + mp.getCodeElement().getClass().getCanonicalName() + "] " + mp);
		CtElement codeElement0 = mp.getCodeElement();
		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(codeElement0);

		CtCodeElement ctCodeElement = ingredients.get(23);

		log.info("--Template-->" + ctCodeElement);

		Ingredient ingredientToModify = new Ingredient(ctCodeElement);

		List<Ingredient> ingredientsTransformed = cardumen.getIngredientTransformationStrategy().transform(mp,
				ingredientToModify);
		log.info("--mp " + (mpi) + " [" + mp.getCodeElement().getClass().getCanonicalName() + "] " + mp);

		int c = 0;
		for (Ingredient ingredientInstantiated : ingredientsTransformed) {
			c++;
			if (c < 100)
				log.info("------Instance-->" + ingredientInstantiated.getCode());

		}

	}

	@Test
	public void testCardumentM70IngredientPartialMapped() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);

		ModificationPoint mp15 = pvar.getModificationPoints().stream()
				.filter(e -> e.getCodeElement().toString().equals("(a + b) * 0.5")).findFirst().get();

		assertNotNull(mp15);
		log.info("-->" + mp15.getCodeElement());
		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(mp15.getCodeElement());

		String code = "solve(_UnivariateRealFunction_0, _double_1, _double_2)";

		CtCodeElement template = ingredients.stream().filter(e -> e.toString().equals(code)).findFirst().get();
		Ingredient ingredientToModify = new Ingredient(template);
		assertNotNull(ingredientToModify);
		assertEquals(code, template.toString());

		List<Ingredient> ingredientsTransformed = cardumen.getIngredientTransformationStrategy().transform(mp15,
				ingredientToModify);
		log.info("MPoint: " + mp15);

		assertTrue(ingredientsTransformed.isEmpty());

		///
		log.info("Checking ");
		ModificationPoint mp0 = pvar.getModificationPoints().stream()
				.filter(e -> e.getCodeElement().toString().equals("solve(min, max)")).findFirst().get();
		assertEquals("solve(min, max)", mp0.getCodeElement().toString());

		List<CtCodeElement> ingredients0 = ingredientSpace.getIngredients(mp0.getCodeElement());

		CtCodeElement element = ingredients0.stream().filter(e -> code.equals(e.toString())).findFirst().get();
		Ingredient ingredientToModify0 = new Ingredient(element);
		assertEquals(code, element.toString());

		List<Ingredient> ingredientsTransformed0 = cardumen.getIngredientTransformationStrategy().transform(mp0,
				ingredientToModify0);
		log.info("MPoint: " + mp15);

		boolean found = false;
		assertTrue(!ingredientsTransformed0.isEmpty());
		for (Ingredient ingredient : ingredientsTransformed0) {
			log.info(ingredient.getCode());
			if (ingredient.getCode().toString().equals("solve(f, min, max)")) {
				found = true;
			}
		}
		assertTrue(found);
	}

	@Test
	@Ignore
	@Deprecated // since we dont manage the VarAccess
	public void testCardumentM70TransformationReference() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(scope, ingredientSpace.scope);

		ProgramVariant pvar = cardumen.getVariants().get(0);

		log.info("Checking ");
		ModificationPoint mp15 = pvar.getModificationPoints().get(15);

		assertEquals("lower >= upper", mp15.getCodeElement().toString());

		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(mp15.getCodeElement());
		log.info(ingredients);
		assertEquals("_double_0", ingredients.get(1).toString());

		Ingredient ingredientToModify = new Ingredient(ingredients.get(1));

		List<Ingredient> ingredientsTransformed = cardumen.getIngredientTransformationStrategy().transform(mp15,
				ingredientToModify);

		for (Ingredient ingInstance : ingredientsTransformed) {
			assertTrue(DynamicIngredient.class.isInstance(ingInstance));
			log.info(ingInstance.getCode());
			String c = ingInstance.getCode().toString();
			assertFalse("_double_0".equals(c));
		}

	}

	@Test
	public void testCardumentM70Evolve() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "400");
		command.command.put("-maxgen", "200");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "disablelog:false");
		command.command.put("-maxVarCombination", "100");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertTrue(cardumen.getSolutions().size() > 0);
	}

	@Test
	public void testCardumentM70EvolveNotUniformreplacement() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;
		// Configuration for paper experiment
		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-maxgen", "100");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters",
				"limitbysuspicious:false:" + "disablelog:true:uniformreplacement:false:frequenttemplate:true");
		command.command.put("-loglevel", Level.INFO.toString());
		command.command.put("-maxVarCombination", "1000");
		command.command.put("-stopfirst", "false");
		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

	}

	@Test
	public void testCardumentM70EvolveUniformreplacement() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "400");
		command.command.put("-maxgen", "200");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "disablelog:true:uniformreplacement:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		for (Object keys : ingredientSpace.mkp.keySet()) {
			log.info(keys + ": ");
			log.info(ingredientSpace.mkp.get(keys));
		}

		assertTrue(cardumen.getSolutions().size() > 0);

	}

	@Test
	public void testCardumentM70EvolveUniformreplacementMostUsedTemplate() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.01");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "400");
		command.command.put("-maxgen", "200");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-stopfirst", "false");
		command.command.put("-parameters", "disablelog:true:uniformreplacement:true:frequenttemplate:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertTrue(cardumen.getSolutions().size() > 0);

	}

	@Test
	public void testCardumentM70Suspicious() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		Integer maxsusp = 2;
		command.command.put("-maxsuspcandidates", maxsusp.toString());

		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "400");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-stopfirst", "false");
		command.command.put("-parameters", "disablelog:true:uniformreplacement:true:frequenttemplate:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		assertEquals((int) maxsusp, cardumen.getVariants().get(0).getModificationPoints().size());

		main1 = new AstorMain();
		maxsusp = 100;
		command.command.put("-maxsuspcandidates", maxsusp.toString());
		command.command.put("-flthreshold", "0.9");

		main1.execute(command.flat());

		cardumen = (CardumenApproach) main1.getEngine();

		assertEquals(1, cardumen.getVariants().get(0).getModificationPoints().size());

		main1 = new AstorMain();
		maxsusp = 100;
		command.command.put("-maxsuspcandidates", maxsusp.toString());
		command.command.put("-flthreshold", "0.000000001");
		command.command.put("-parameters", "limitbysuspicious:false");

		main1.execute(command.flat());

		cardumen = (CardumenApproach) main1.getEngine();

		assertEquals(12, cardumen.getVariants().get(0).getModificationPoints().size());

	}

	@Test
	public void testCardumentM70AssertStats() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "400");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-stopfirst", "false");
		command.command.put("-loglevel", "INFO");
		command.command.put("-parameters", "disablelog:true:uniformreplacement:true:frequenttemplate:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.currentStat = null;
		Stats.createStat();
		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		EfficientIngredientStrategy estrategy = (EfficientIngredientStrategy) cardumen.getIngredientSearchStrategy();
		ExpressionTypeIngredientSpace ingredientSpace = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertEquals(0, Stats.createStat().getIngredientsStats().combinationByIngredientSize.size());
		assertEquals(0, Stats.createStat().getIngredientsStats().ingredientSpaceSize.size());

		ModificationPoint mp1 = cardumen.getVariants().get(0).getModificationPoints().get(0);

		AstorOperator op1 = cardumen.getOperatorSpace().getOperators().get(0);
		List elements = estrategy.getNotExhaustedBaseElements(mp1, op1);
		int initialIngredients = elements.size();
		assertEquals(elements.size(), ingredientSpace.getIngredients(mp1.getCodeElement()).size());

		assertTrue(elements.size() > 0);

		// assertEquals(0, Stats.createStat().ingredientSpaceSize.get());

		List<CtCodeElement> bases1 = estrategy.getNotExhaustedBaseElements(mp1, op1);

		assertNotNull(bases1);

		CtCodeElement base1 = bases1.get(0);
		assertNotNull(base1);
		log.info("base 1:" + base1);

		// assertEquals(1, (int)
		// Stats.getCurrentStats().ingredientSpaceSize.get(bases.size()));

		List<Ingredient> ingredientsAfterTransformation = estrategy.getInstancesFromBase(mp1, op1,
				new Ingredient(base1));
		int conmbination1Size = ingredientsAfterTransformation.size();
		assertTrue(conmbination1Size > 0);

		long nrcomb = ingredientsAfterTransformation.size();
		Ingredient ins1 = estrategy.getNotUsedTransformedElement(mp1, op1, new Ingredient(base1));

		assertNotNull(ins1);
		log.info(Stats.currentStat.getIngredientsStats().combinationByIngredientSize);

		assertTrue(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey(nrcomb));
		// Two attempts done before
		assertEquals(2, (long) Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get(nrcomb));

		for (int i = 1; i < nrcomb; i++) {
			log.info("-->" + i);
			Ingredient ins1i = estrategy.getNotUsedTransformedElement(mp1, op1, new Ingredient(base1));
			assertNotNull(ins1i);

			assertEquals(i + 1, Stats.currentStat.getIngredientsStats().combinationByIngredientSize.keySet().size());
			log.info(Stats.currentStat.getIngredientsStats().combinationByIngredientSize);
			assertTrue(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey(nrcomb - i));
			assertEquals(1, (long) Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get(nrcomb - i));
			if (Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 0)) {
				log.info("Putting zero for " + i);
			}
			if (i != 120)
				assertNull(estrategy.exhaustTemplates.get(estrategy.getKey(mp1, op1)));
		}
		for (Long i : Stats.currentStat.getIngredientsStats().combinationByIngredientSize.keySet()) {
			assertTrue("--" + i, i <= 121 && i > 0);
		}
		assertEquals(nrcomb, Stats.currentStat.getIngredientsStats().combinationByIngredientSize.keySet().size());
		assertFalse(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 0));

		// Cloning stats to be sure that is not modified when there is no more
		// ingredients to select
		Map clonedCombStats = new HashMap<>(Stats.currentStat.getIngredientsStats().combinationByIngredientSize);
		// Any ingredients to add....
		Ingredient ins1i = estrategy.getNotUsedTransformedElement(mp1, op1, new Ingredient(base1));
		assertNull(ins1i);
		// Now, after the space is completely navigated, a zero must Not be
		// written
		assertFalse(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 0));

		// The same that before, the cache of ingredients would return zero
		// ingredients
		Ingredient ins1i2 = estrategy.getNotUsedTransformedElement(mp1, op1, new Ingredient(base1));
		assertNull(ins1i2);

		// No change in the stats
		assertEquals(clonedCombStats.keySet().size(),
				Stats.currentStat.getIngredientsStats().combinationByIngredientSize.keySet().size());

		for (Long i : Stats.currentStat.getIngredientsStats().combinationByIngredientSize.keySet()) {
			assertTrue("--" + i, i <= 121 && i > 0);
			assertEquals(clonedCombStats.get(i),
					Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get(i));
		}

		for (int i = 1; i < conmbination1Size; i++) {
			assertEquals("-->" + i, 1,
					Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get((long) i), 0);

		}

		assertEquals(nrcomb, Stats.currentStat.getIngredientsStats().combinationByIngredientSize.keySet().size());

		assertEquals(1, estrategy.exhaustTemplates.keySet().size());
		assertTrue(!estrategy.exhaustTemplates.get(estrategy.getKey(mp1, op1)).isEmpty());
		assertTrue(estrategy.exhaustTemplates.get(estrategy.getKey(mp1, op1)).contains(base1));

		log.info("-------Base 2-------------");

		CtCodeElement base2 = bases1.get(1);
		assertNotNull(base2);
		log.info("base 2" + base2);
		assertEquals(1, estrategy.exhaustTemplates.keySet().size());

		List<CtCodeElement> bases2 = estrategy.getNotExhaustedBaseElements(mp1, op1);

		assertNotNull(bases2);

		List<Ingredient> ingredientsAfterTransformationNull = estrategy.getInstancesFromBase(mp1, op1,
				new Ingredient(base1));
		assertNull(ingredientsAfterTransformationNull);

		List<Ingredient> ingredientsAfterTransformation2 = estrategy.getInstancesFromBase(mp1, op1,
				new Ingredient(base2));

		int conmbination2 = ingredientsAfterTransformation2.size();
		assertTrue(conmbination2 > 0);
		int nrcomb2 = ingredientsAfterTransformation2.size();
		assertEquals(bases1.size(), bases2.size() + 1);

		assertFalse(bases2.contains(base1));

		log.info("-------Base 3-----------");
		List<CtCodeElement> bases3 = estrategy.getNotExhaustedBaseElements(mp1, op1);

		log.info("\nbase 3 " + bases3);
		CtCodeElement base3 = bases3.get(2);
		log.info("before 3: " + Stats.currentStat.getIngredientsStats().combinationByIngredientSize);

		List<Ingredient> ingredientsAfterTransformation3 = estrategy.getInstancesFromBase(mp1, op1,
				new Ingredient(base3));
		log.info("Ingredients for base 3\n" + ingredientsAfterTransformation3.size());
		assertTrue(ingredientsAfterTransformation3.size() > 0);

		int ingredientsAfterTransformation3Size = ingredientsAfterTransformation3.size();
		for (int i = 1; i <= ingredientsAfterTransformation3Size; i++) {
			log.info("-->" + i);
			Ingredient ins3i = estrategy.getNotUsedTransformedElement(mp1, op1, new Ingredient(base3));
			assertNotNull(ins3i);
			// assertEquals(i + 1,
			// Stats.currentStat.combinationByIngredientSize.keySet().size());
			log.info("After: " + Stats.currentStat.getIngredientsStats().combinationByIngredientSize);
			int remainingSize = ingredientsAfterTransformation3Size - i + 1;// +1
																			// due
																			// i
																			// starts
																			// in
																			// 1
			assertTrue(" " + i, (long) Stats.currentStat.getIngredientsStats().combinationByIngredientSize
					.get((long) (remainingSize)) >= 2);
			if (Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 0)) {
				log.info("Putting zero for " + i + ", ing " + ins3i + ", total attepts with zero "
						+ Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get((long) 0));
			}
		}

		Ingredient ins3n = estrategy.getNotUsedTransformedElement(mp1, op1, new Ingredient(base3));
		assertNull(ins3n);

		log.info("Putting zero for " + base3 + " total attepts with zero "
				+ Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get((long) 0));

		assertTrue(estrategy.exhaustTemplates.get(estrategy.getKey(mp1, op1)).contains(base3));

		log.info("--------BASE 4--no trasformation--------");

		List<CtCodeElement> bases4 = estrategy.getNotExhaustedBaseElements(mp1, op1);
		assertFalse(bases4.contains(base3));

		///

		assertTrue(!estrategy.exhaustTemplates.get(estrategy.getKey(mp1, op1)).isEmpty());

		List l = (List) ingredientSpace.mkp.values().toArray()[0];
		CtCodeElement base4 = (CtCodeElement) l.get(0);
		assertEquals("clearResult()", base4.toString());

		long before = Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get((long) 1);
		ModificationPoint mp5 = cardumen.getVariants().get(0).getModificationPoints().get(5);
		List<Ingredient> ingredientsAfterTransformation4 = estrategy.getInstancesFromBase(mp5, op1,
				new Ingredient(base4));
		log.info("mp5 " + Stats.currentStat.getIngredientsStats().combinationByIngredientSize);
		assertTrue(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 1));

		assertEquals(before + 1, Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get((long) 1), 0);

		assertTrue(ingredientsAfterTransformation4.size() > 0);

		log.info("--------BASE 5----------");

		SuspiciousModificationPoint mp7 = (SuspiciousModificationPoint) cardumen.getVariants().get(0)
				.getModificationPoints().get(7);
		assertTrue(mp7.getSuspicious().getClassName().contains("UnivariateRealSolverUtils"));
		assertEquals("solve(_UnivariateRealFunction_0, _double_1, _double_2)", base1.toString());

		assertFalse(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 0));

		List<Ingredient> ingredientsAfterTransformation5 = estrategy.getInstancesFromBase(mp7, op1,
				new Ingredient(base1));

		assertTrue(ingredientsAfterTransformation5.isEmpty());

		assertTrue(Stats.currentStat.getIngredientsStats().combinationByIngredientSize.containsKey((long) 0));

		assertEquals(1, Stats.currentStat.getIngredientsStats().combinationByIngredientSize.get((long) 0), 0);

	}

	@Test
	public void testCardumentM70Exhausitve() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		// command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "considerzerovaluesusp:true:scope:" + scope.toString().toLowerCase()
				+ "scope:limitbysuspicious:false:" + "disablelog:true:uniformreplacement:false:frequenttemplate:true");
		command.command.put("-loglevel", Level.INFO.toString());
		command.command.put("-maxVarCombination", "100000000");

		AstorMain main1 = new AstorMain();
		log.info("LOCAL SCOPE");
		// Local
		command.command.put("-parameters", "considerzerovaluesusp:false:scope:local" + ":limitbysuspicious:false:"
				+ "disablelog:true:uniformreplacement:false:frequenttemplate:true");

		main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();
		CardumenExhaustiveEngine4Stats cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();

		assertEquals(73, cardumen.totalBases);

		log.info("PACKAGE SCOPE");
		// PACKAGE
		command.command.put("-parameters", "considerzerovaluesusp:false:scope:package" + ":limitbysuspicious:false:"
				+ "disablelog:true:uniformreplacement:false:frequenttemplate:true");

		main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();
		cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();

		assertTrue(cardumen.totalBases >= 160);
	}

	@Test
	public void testCardumentM70ExhausitveMaxLimited() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;
		// Configuration for paper experiment
		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters",
				"limitbysuspicious:false:" + "disablelog:true:uniformreplacement:false:frequenttemplate:true");
		command.command.put("-loglevel", Level.INFO.toString());
		command.command.put("-maxVarCombination", "1000");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();
		CardumenExhaustiveEngine4Stats cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();

		assertTrue(cardumen.totalIngredients + " > 86299730", cardumen.totalIngredients >= 86299730);
		assertTrue(86299730 >= cardumen.totalIngredientsCutted);
		assertTrue(cardumen.totalIngredientsCutted >= 26915);
		assertTrue(cardumen.totalBases >= 160);

	}

	@Test
	public void testCardumentM70ExhausitveMaxSuspiciousLimited() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "skipfitnessinitialpopulation:true:limitbysuspicious:false:"
				+ "disablelog:false:uniformreplacement:false:frequenttemplate:false");
		command.command.put("-loglevel", Level.INFO.toString());
		command.command.put("-maxVarCombination", "100");
		command.command.put("-maxsuspcandidates", "1000");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();
		CardumenExhaustiveEngine4Stats cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();

		assertEquals(12, cardumen.totalmp);

		command.command.put("-maxsuspcandidates", "3");
		main1.execute(command.flat());
		Stats.createStat();
		cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();
		assertEquals(3, cardumen.totalmp);

		// assertEquals(100605077,cardumen.totalIngredients);
		// assertTrue(100605077 > cardumen.totalIngredientsCutted);
		// assertEquals(38222,cardumen.totalBases);

	}

	@Test
	public void testCardumentM70MaxModPoints() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;

		int maxModPoints = 7;// Let's say 7, the number of MP over this
								// configuration is 12.

		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters",
				"maxmodificationpoints:" + maxModPoints + ":skipfitnessinitialpopulation:true:limitbysuspicious:false");
		command.command.put("-loglevel", Level.INFO.toString());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();

		assertEquals(maxModPoints, main1.getEngine().getVariants().get(0).getModificationPoints().size());
	}

	@Test
	public void testCardumentM70ExhausitveReplacement() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;
		boolean uniformreplacement = false;
		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "skipfitnessinitialpopulation:true:limitbysuspicious:false:"
				+ "disablelog:false:uniformreplacement:" + Boolean.toString(uniformreplacement));
		command.command.put("-loglevel", Level.INFO.toString());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();
		assertFalse(ConfigurationProperties.getPropertyBool("uniformreplacement"));

		CardumenExhaustiveEngine4Stats cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();

		assertTrue(cardumen.totalmp + " >= 12", cardumen.totalmp >= 12);
		long tingNotUnif = cardumen.totalIngredientsCutted;
		long tingNotUnifall = cardumen.totalIngredients;

		// changing property

		uniformreplacement = true;

		command.command.put("-parameters", "skipfitnessinitialpopulation:true:limitbysuspicious:false:"
				+ "disablelog:false:uniformreplacement:" + Boolean.toString(uniformreplacement));

		cardumen = null;
		main1.execute(command.flat());
		Stats.createStat();
		cardumen = (CardumenExhaustiveEngine4Stats) main1.getEngine();
		assertTrue(ConfigurationProperties.getPropertyBool("uniformreplacement"));
		long tingUnif = cardumen.totalIngredientsCutted;
		long tingUnifall = cardumen.totalIngredients;
		log.info(tingNotUnif + " > " + tingUnif);
		assertTrue(tingNotUnif + " > " + tingUnif, tingNotUnif > tingUnif);
		log.info(tingNotUnifall + " > " + tingUnifall);
		assertTrue(tingNotUnifall + " > " + tingUnifall, tingNotUnifall > tingUnifall);
	}

	@Test
	@Ignore
	public void testCardumentM42() throws Exception {
		CommandSummary command = MathCommandsTests.getMath42Command();

		IngredientSpaceScope scope = IngredientSpaceScope.PACKAGE;
		boolean uniformreplacement = false;
		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "skipfitnessinitialpopulation:true:limitbysuspicious:false:"
				+ "disablelog:false:uniformreplacement:" + Boolean.toString(uniformreplacement));
		command.command.put("-loglevel", Level.INFO.toString());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		Stats.createStat();

	}

	@Test
	public void testCardumentM70ScopeLocalProbTransformation() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.00");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-parameters", "probabilistictransformation:true");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		ProbabilisticTransformationStrategy probTransf = (ProbabilisticTransformationStrategy) cardumen
				.getIngredientTransformationStrategy();

		probTransf.calculateGramsProbs();

		ProgramVariant pv = cardumen.getVariants().get(0);
		// C1:.BisectionSolver l: 66,
		CtElement clearResult = (CtCodeElement) pv.getModificationPoints().get(2).getCodeElement();

		List<CtVariable> varContextClearResult = VariableResolver.searchVariablesInScope(clearResult);//
		CtElement returnExpression = pv.getModificationPoints().get(8).getCodeElement();
		VarMapping vmapping = VariableResolver.mapVariablesFromContext(varContextClearResult, returnExpression);

		ModificationPoint mpointCleanResult = pv.getModificationPoints().get(8);

		assertEquals("clearResult()", clearResult.toString());

		assertEquals("(a + b) * 0.5", returnExpression.toString());

		List<VarCombinationForIngredient> varsComb4Ingredients = probTransf.findAllVarMappingCombinationUsingProbab(
				vmapping.getMappedVariables(), mpointCleanResult, new Ingredient(returnExpression));
		assertTrue(varsComb4Ingredients.size() > 0);

		Double probability = varsComb4Ingredients.get(0).getProbality();
		assertTrue(probability > 0);

		for (VarCombinationForIngredient varCombinationForIngredient : varsComb4Ingredients) {
			log.info(varCombinationForIngredient);
			assertTrue(probability >= varCombinationForIngredient.getProbality());
			probability = varCombinationForIngredient.getProbality();
		}

	}

	@Test
	public void testCardumentM70ScopeLocalRandomTransformation() throws Exception {
		CommandSummary command = MathCommandsTests.getMath70Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.00");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "1");
		command.command.put("-maxgen", "0");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		// RANDOM
		command.command.put("-parameters", "probabilistictransformation:false");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();

		RandomTransformationStrategy probTransf = (RandomTransformationStrategy) cardumen
				.getIngredientTransformationStrategy();

		ProgramVariant pv = cardumen.getVariants().get(0);
		// C1:.BisectionSolver l: 66,
		CtElement clearResult = (CtCodeElement) pv.getModificationPoints().get(2).getCodeElement();

		List<CtVariable> varContextClearResult = VariableResolver.searchVariablesInScope(clearResult);//
		CtElement returnExpression = pv.getModificationPoints().get(8).getCodeElement();
		VarMapping vmapping = VariableResolver.mapVariablesFromContext(varContextClearResult, returnExpression);

		ModificationPoint mpointCleanResult = pv.getModificationPoints().get(8);

		assertEquals("clearResult()", clearResult.toString());

		assertEquals("(a + b) * 0.5", returnExpression.toString());

		List<VarCombinationForIngredient> varsComb4Ingredients = probTransf
				.findAllVarMappingCombinationUsingRandom(vmapping.getMappedVariables(), mpointCleanResult);
		assertTrue(varsComb4Ingredients.size() > 0);

		Double probability = varsComb4Ingredients.get(0).getProbality();
		assertTrue(probability > 0);

		for (VarCombinationForIngredient varCombinationForIngredient : varsComb4Ingredients) {
			log.info(varCombinationForIngredient);
			assertEquals(probability, varCombinationForIngredient.getProbality());
			probability = varCombinationForIngredient.getProbality();
		}

	}

	@Test
	public void testCardumentM57() throws Exception {
		CommandSummary command = MathCommandsTests.getMath57Command();

		IngredientSpaceScope scope = IngredientSpaceScope.LOCAL;

		command.command.put("-mode", ExecutionMode.CARDUMEN.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-seed", "20007");
		command.command.put("-maxgen", "10");
		command.command.put("-population", "1");
		command.command.put("-scope", scope.toString().toLowerCase());
		command.command.put("-loglevel", "INFO");
		// RANDOM
		// command.command.put("-parameters",
		// "probabilistictransformation:false");

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		CardumenApproach cardumen = (CardumenApproach) main1.getEngine();
		assertEquals(AstorOutputStatus.MAX_GENERATION, cardumen.getOutputStatus());

		ExpressionTypeIngredientSpace space = (ExpressionTypeIngredientSpace) cardumen.getIngredientSearchStrategy()
				.getIngredientSpace();
		for (CtElement c : space.allElementsFromSpace) {
			log.info("-> " + c);
		}
	}

	@Test
	@Ignore
	public void testCardumenClosure1() throws Exception {
		File projectLocation = new File("./examples/closure_1");
		AstorMain main1 = new AstorMain();
		File dirLibs = new File(projectLocation.getAbsolutePath() + File.separator + "/lib/");
		String dep = ClosureTest.getDependencies(projectLocation, dirLibs);
		File out = new File(ConfigurationProperties.getProperty("workingDirectory"));
		String[] args = new String[] { //
				"-dependencies", dep, //
				"-mode", "statement", //
				"-location", projectLocation.getAbsolutePath(), //
				"-srcjavafolder", "/src/:/test/", //
				"-srctestfolder", "/test/", //
				"-binjavafolder", "/build/classes/", //
				"-bintestfolder", "/build/test/", //
				"-javacompliancelevel", "6", //
				"-flthreshold", "0.5", //
				"-out", out.getAbsolutePath(), //
				"-scope", "local", //
				"-seed", "10", //
				"-maxgen", "0", // No run
				"-stopfirst", "true", //
				"-maxtime", "100"//

		};

		File fileLog = File.createTempFile("logclos1", "txt");

		CommandSummary command = new CommandSummary(args);
		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", "package");
		command.command.put("-parameters",
				"skipfitnessinitialpopulation:true:limitbysuspicious:false:"
						+ "disablelog:false:uniformreplacement:false:skipfitnessinitialpopulation:true:logfilepath:"
						+ fileLog.getAbsolutePath());
		command.command.put("-loglevel", Level.INFO.toString());

		main1.execute(command.flat());

	}

	@Test
	@Ignore
	public void testTime11() throws Exception {
		// MP: (156/834) MP=org.joda.time.DateTimeZone line: 263, pointed
		// element: CtBinaryOperatorImpl|| code: hoursInMinutes < 0||
		// spoon.support.reflect.code.CtBinaryOperatorImpl (boolean)
		// Base: (44/161) spoon.support.reflect.code.CtBinaryOperatorImpl
		// (boolean) ((_long_0 ^ _int_1) >= 0)
		// [DEBUG]
		// fr.inria.astor.core.manipulation.sourcecode.VariableResolver.mapVariablesFromContext(VariableResolver.java:343)
		// - #vars out of context: 2
		// [DEBUG]
		// fr.inria.astor.approaches.cardumen.CardumenExhaustiveEngine4Stats.startEvolution(CardumenExhaustiveEngine4Stats.java:123)
		// - -nrIng-[0, 0]

		// command line arguments: [-mode statement -location . -id Time
		// -dependencies lib/ -failing org.joda.time.tz.TestCompiler: -package
		// org.joda -jvm4testexecution
		// /usr/lib/jvm/java-1.7.0-openjdk-amd64/bin/ -jvm4evosuitetestexecution
		// /home/mmartinez/jdk1.8.0_45/bin/ -javacompliancelevel 5 -maxgen
		// 1000000 -seed 10 -stopfirst false -scope local -maxtime 120
		// -population 1 -srcjavafolder src/main/java/ -srctestfolder
		// src/test/java/ -binjavafolder target/classes/ -bintestfolder
		// target/test-classes/ -flthreshold 0.1 -validation
		// fr.inria.astor.core.validation.validators.RegressionValidation
		// -evosuitetimeout 300 -ignoredtestcases
		// org.apache.commons.lang.LocaleUtilsTest]
		File fileLog = File.createTempFile("logtime11", "txt");
		fileLog.createNewFile();

		String m2path = System.getenv("HOME") + "/.m2/repository/";
		File fm2 = new File(m2path);
		if (!fm2.exists()) {
			throw new Exception(m2path + "does not exit");
		}

		String[] cms = new String[] { "-mode", "statement"

				, "-location", (new File("./examples/time_11")).getAbsolutePath(), "-dependencies",
				new File("./examples/libs/junit-3.8.2.jar").getAbsolutePath() + File.pathSeparator
						+ new File(m2path + "/org/joda/joda-convert/1.2/joda-convert-1.2.jar").getAbsolutePath()

				, "out" + new File(ConfigurationProperties.getProperty("workingDirectory")), "-failing",
				"org.joda.time.tz.TestCompiler", "-package", "org.joda", "-javacompliancelevel", "7", "-maxgen",
				"1000000", "-seed", "6001", "-stopfirst", "true", "-scope", "package", "-maxtime", "10", "-population",
				"1", "-srcjavafolder", "src/main/java/", "-srctestfolder", "src/test/java/", "-binjavafolder",
				"target/classes/", "-bintestfolder", "target/test-classes/", "-flthreshold", "0.1" }
		// + "
		// -validation,fr.inria.astor.core.validation.validators.RegressionValidation,"
		;

		CommandSummary command = new CommandSummary(cms);
		command.command.put("-mode", ExecutionMode.custom.name());
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "60");
		command.command.put("-population", "1");
		command.command.put("-customengine", CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", "package");
		command.command.put("-parameters",
				"skipfitnessinitialpopulation:true:limitbysuspicious:false:"
						+ "disablelog:false:uniformreplacement:false:skipfitnessinitialpopulation:true:logfilepath:"
						+ fileLog.getAbsolutePath());
		command.command.put("-loglevel", Level.INFO.toString());
		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

	}

	@Test
	@Ignore
	public void testTime11Step() throws Exception {

		File fileLog = File.createTempFile("logtime11", "txt");
		fileLog.createNewFile();

		String m2path = System.getenv("HOME") + "/.m2/repository/";
		File fm2 = new File(m2path);
		if (!fm2.exists()) {
			throw new Exception(m2path + "does not exit");
		}
		String[] cms = new String[] { "-mode", "statement"

				, "-location", (new File("./examples/time_11")).getAbsolutePath(), "-dependencies",
				new File("./examples/libs/junit-3.8.2.jar").getAbsolutePath() + File.pathSeparator
						+ new File(fm2 + "/org/joda/joda-convert/1.2/joda-convert-1.2.jar").getAbsolutePath()

				, "out" + new File(ConfigurationProperties.getProperty("workingDirectory")), "-failing",
				"org.joda.time.tz.TestCompiler", "-package", "org.joda", "-javacompliancelevel", "7", "-maxgen",
				"1000000", "-seed", "6001", "-stopfirst", "true", "-scope", "package", "-maxtime", "10", "-population",
				"1", "-srcjavafolder", "src/main/java/", "-srctestfolder", "src/test/java/", "-binjavafolder",
				"target/classes/", "-bintestfolder", "target/test-classes/", "-flthreshold", "0.1" }
		// + "
		// -validation,fr.inria.astor.core.validation.validators.RegressionValidation,"
		;

		CommandSummary command = new CommandSummary(cms);
		command.command.put("-mode", "Cardumen");
		command.command.put("-flthreshold", "0.1");
		command.command.put("-maxtime", "0");
		command.command.put("-population", "1");
		// command.command.put("-customengine",
		// CardumenExhaustiveEngine4Stats.class.getCanonicalName());
		command.command.put("-scope", "package");
		command.command.put("-maxgen", "0");
		command.command.put("-parameters",
				"skipfitnessinitialpopulation:true:limitbysuspicious:false:"
						+ "disablelog:false:uniformreplacement:false:skipfitnessinitialpopulation:true:logfilepath:"
						+ fileLog.getAbsolutePath());
		command.command.put("-loglevel", Level.INFO.toString());
		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());
		
		AstorCoreEngine  approach = main1.getEngine();
		IngredientBasedApproach ingapproach = (IngredientBasedApproach) main1.getEngine();

		// MP: (156/834) MP=org.joda.time.DateTimeZone line: 263, pointed
		// element: CtBinaryOperatorImpl|| code: hoursInMinutes < 0||
		// spoon.support.reflect.code.CtBinaryOperatorImpl (boolean)
		// Base: (44/161) spoon.support.reflect.code.CtBinaryOperatorImpl
		// (boolean) ((_long_0 ^ _int_1) >= 0)

		ProgramVariant pv = main1.getEngine().getVariants().get(0);

		// ingapproach.getIngredientStrategy().getIngredientSpace().defineSpace(pv);
		ModificationPoint mp156 = pv.getModificationPoints().get(155);

		assertEquals("hoursInMinutes < 0", mp156.getCodeElement().toString());
		List<CtVariable> varContextClearResult = VariableResolver.searchVariablesInScope(mp156.getCodeElement());//
		CtElement returnExpression = pv.getModificationPoints().get(8).getCodeElement();

		EfficientIngredientStrategy estrategy = (EfficientIngredientStrategy) ingapproach.getIngredientSearchStrategy();

		List<CtCodeElement> baseElements = estrategy.getNotExhaustedBaseElements(mp156,
				approach.getOperatorSpace().getOperators().get(0));

		CtCodeElement ingredient = baseElements.get(43);
		assertEquals("((_long_0 ^ _int_1) >= 0)", ingredient.toString());

		VarMapping vmapping = VariableResolver.mapVariablesFromContext(varContextClearResult, ingredient);
		log.info(vmapping);
		assertEquals(1, vmapping.getMappedVariables().size());
		assertEquals(1, vmapping.getNotMappedVariables().size());

	}

}
