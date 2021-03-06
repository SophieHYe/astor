package fr.inria.astor.approaches.tos.ingredients.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

import fr.inria.astor.approaches.tos.entity.TOSEntity;
import fr.inria.astor.approaches.tos.entity.TOSVariablePlaceholder;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.sourcecode.VariableResolver;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.util.MapList;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;

/**
 * 
 * @author Matias Martinez
 *
 */
public class TOSPlaceholderGenerator implements TOSGenerator {

	public static String PLACEHOLDER_VAR = "_%s_%d";

	protected Logger log = Logger.getLogger(this.getClass().getName());

	@Override
	public List<? extends TOSEntity> createTOS(CtStatement ingredientStatement) {

		int nrPlaceholders = ConfigurationProperties.getPropertyInt("nrPlaceholders");

		List<TOSVariablePlaceholder> createdTemplates = new ArrayList<>();

		List<CtVariableAccess> varAccessCollected = VariableResolver.collectVariableAccess(ingredientStatement, true);
		List<String> varsNames = varAccessCollected.stream().map(e -> e.getVariable().getSimpleName()).distinct()
				.collect(Collectors.toList());
		List<Set<String>> variableNamesCombinations = Sets.powerSet(new HashSet<>(varsNames)).stream()
				.filter(e -> e.size() == nrPlaceholders && !e.isEmpty()).collect(Collectors.toList());
		log.debug("Names " + varsNames);
		log.debug("combinations " + variableNamesCombinations);

		for (Set<String> targetPlaceholders : variableNamesCombinations) {

			log.debug("analyzing target Placeholders: " + targetPlaceholders);

			TOSVariablePlaceholder tosCreated = createParticularTOS(ingredientStatement, targetPlaceholders);

			if (tosCreated != null) {
				// log.debug("Adding generated TOS: " + tosCreated);
				createdTemplates.add(tosCreated);
			}

		}
		return createdTemplates;
	}

	@SuppressWarnings("unchecked")
	private TOSVariablePlaceholder createParticularTOS(CtStatement ingredientStatement,
			Set<String> targetPlaceholders) {

		// Vars name mapped to placeholders
		Map<String, String> placeholderVarNamesMappings = new HashMap<>();
		MapList<String, CtVariableAccess> placeholdersToVariables = new MapList<>();
		List<CtVariableAccess> variablesNotModified = new ArrayList<>();
		//

		CtElement original = ingredientStatement;
		CtElement ingredientElement = MutationSupporter.clone(ingredientStatement);// ingredientStatement.clone();

		// We collect all variables
		List<CtVariableAccess> varAccessCollected = VariableResolver.collectVariableAccess(ingredientElement, true);

		int nrvar = 0;
		for (int i = 0; i < varAccessCollected.size(); i++) {

			CtVariableAccess<?> variableUnderAnalysis = varAccessCollected.get(i);

			if (!targetPlaceholders.contains(variableUnderAnalysis.getVariable().getSimpleName())) {
				// The variable name is not in the list of placeholders
				variablesNotModified.add(variableUnderAnalysis);
				continue;
			}

			if (VariableResolver.isStatic(variableUnderAnalysis.getVariable())) {
				variablesNotModified.add(variableUnderAnalysis);
				continue;
			}

			String abstractName = "";
			// We have not transform another variable with the same name
			if (!placeholderVarNamesMappings.containsKey(variableUnderAnalysis.getVariable().getSimpleName())) {
				String currentTypeName = variableUnderAnalysis.getVariable().getType().getSimpleName();
				if (currentTypeName.contains("?")) {
					// Any change in case of ?
					abstractName = variableUnderAnalysis.getVariable().getSimpleName();
				} else {
					abstractName = String.format(PLACEHOLDER_VAR, currentTypeName, nrvar);
				}
				placeholderVarNamesMappings.put(variableUnderAnalysis.getVariable().getSimpleName(), abstractName);
				nrvar++;
			} else {
				// We use the placeholder name previously defined for a variable
				// with similar name
				abstractName = placeholderVarNamesMappings.get(variableUnderAnalysis.getVariable().getSimpleName());
			}
			placeholdersToVariables.add(abstractName, variableUnderAnalysis);

			variableUnderAnalysis.getVariable().setSimpleName(abstractName);
			// workaround: Problems with var Shadowing
			variableUnderAnalysis.getFactory().getEnvironment().setNoClasspath(true);
			if (variableUnderAnalysis instanceof CtFieldAccess) {
				CtFieldAccess fieldAccess = (CtFieldAccess) variableUnderAnalysis;
				fieldAccess.getVariable().setDeclaringType(null);
			}

		}

		TOSVariablePlaceholder ingredient = new TOSVariablePlaceholder(ingredientElement, null, original,
				placeholdersToVariables, placeholderVarNamesMappings, variablesNotModified);

		return ingredient;
	}

}
