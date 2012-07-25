package edu.iastate.cycmodeler.model;

import java.util.HashMap;
import java.util.HashSet;

import edu.iastate.javacyco.PtoolsErrorException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class AbstractReactionInstance {
	public String Name;
	public boolean Reversible;
	public HashSet<MetaboliteInstance> Reactants;
	public HashSet<MetaboliteInstance> Products;
	public String ReactionLocation;
	
	/**
	 * Test if a reaction is balanced by adding up each element on the reactant side and each element on the product side and
	 * comparing the quantity of each.
	 * 
	 * Note: This method does not interpret chemical shorthand (eg R-groups, etc), but does make a special case for electron acceptors and donors.
	 * An acceptor is treated as one "A" (for acceptor), while a donor is treated as 1 "A" and 2 "H" (hydrogen). This method also
	 * assumes strict matching only, so a missing proton or water will result in a failed test for balance, even though these compounds can
	 * sometimes assumed to be present. (Reactions missing a water or proton have been found in EcoCyc on occasion). Also note that non-standard codes
	 * used to represent elements (such as using "COBALT" instead of "Co") do not matter, since they are consistent across both sides of the comparison. 
	 * 
	 * @return Returns true if reactants and products are elementally balanced, false if not.
	 * Any errors or unreadable/missing formulas return false.
	 */
	protected boolean isReactionBalanced() {
		HashMap<String, Integer> reactantElements = new HashMap<String, Integer>();
		HashMap<String, Integer> productElements = new HashMap<String, Integer>();
		try {
			for (MetaboliteInstance reactant : Reactants) {
				// Special Case
				int specialCases = 0;
				if (reactant.MetaboliteFrame.getLocalID().equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (reactant.MetaboliteFrame.getLocalID().equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
				switch (specialCases) {
					case 1: {
						if (reactantElements.containsKey("A")) {
							reactantElements.put("A", reactantElements.get("A") + (1*reactant.coefficient_));
						} else {
							reactantElements.put("A", (1*reactant.coefficient_));
						}
					} break;
					case 2: {
						if (reactantElements.containsKey("A")) {
							reactantElements.put("A", reactantElements.get("A") + (1*reactant.coefficient_));
						} else {
							reactantElements.put("A", (1*reactant.coefficient_));
						}
						if (reactantElements.containsKey("H")) {
							reactantElements.put("H", reactantElements.get("H") + (2*reactant.coefficient_));
						} else {
							reactantElements.put("H", (2*reactant.coefficient_));
						}
					} break;
				}
				if (specialCases != 0) {
					continue;
				}
				
				// Regular Case
				for (Object o : reactant.MetaboliteFrame.getSlotValues("CHEMICAL-FORMULA")) {
					String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
					String element = chemicalFormulaElement.split(",")[0];
					Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
					
					//Add to map
					if (reactantElements.containsKey(element)) {
						reactantElements.put(element, reactantElements.get(element) + (quantity*reactant.coefficient_));
					} else {
						reactantElements.put(element, (quantity*reactant.coefficient_));
					}
				}
			}
			
			for (MetaboliteInstance product : Products) {
				// Special Case
				int specialCases = 0;
				if (product.MetaboliteFrame.getLocalID().equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (product.MetaboliteFrame.getLocalID().equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
				switch (specialCases) {
					case 1: {
						if (productElements.containsKey("A")) {
							productElements.put("A", productElements.get("A") + (1*product.coefficient_));
						} else {
							productElements.put("A", (1*product.coefficient_));
						}
					} break;
					case 2: {
						if (productElements.containsKey("A")) {
							productElements.put("A", productElements.get("A") + (1*product.coefficient_));
						} else {
							productElements.put("A", (1*product.coefficient_));
						}
						if (productElements.containsKey("H")) {
							productElements.put("H", productElements.get("H") + (2*product.coefficient_));
						} else {
							productElements.put("H", (1*product.coefficient_));
						}
					} break;
				}
				if (specialCases != 0) {
					continue;
				}
				
				// Regular Case
				for (Object o : product.MetaboliteFrame.getSlotValues("CHEMICAL-FORMULA")) {
					String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
					String element = chemicalFormulaElement.split(",")[0];
					Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
					
					//Add to map
					if (productElements.containsKey(element)) {
						productElements.put(element, productElements.get(element) + (quantity*product.coefficient_));
					} else {
						productElements.put(element, (quantity*product.coefficient_));
					}
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			return false;
		}
		
		if (!reactantElements.keySet().containsAll(productElements.keySet()) || !productElements.keySet().containsAll(reactantElements.keySet())) return false;
		for (String key : reactantElements.keySet()) {
//			if (key.equalsIgnoreCase("H")) { //TODO account for reactions which fail to match by commonly omitted elements
//				if (reactantElements.get(key) - productElements.get(key) == 1 || reactantElements.get(key) - productElements.get(key) == -1) {
//					System.out.println("Save reaction with a proton.");
//				}
//			}
			if (reactantElements.get(key) != productElements.get(key)) return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).
	        append(Reactants).
	        append(Products).
	        append(ReactionLocation).
	        toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;

		AbstractReactionInstance other = (AbstractReactionInstance) obj;
		return new EqualsBuilder().
				append(this.Reactants, other.Reactants).
				append(this.Products, other.Products).
				append(this.ReactionLocation, other.ReactionLocation).
				isEquals();
	}
	
	public abstract String generateReactionID();
	protected abstract void addReactant(MetaboliteInstance reactant);
	protected abstract void addProduct(MetaboliteInstance product);
}
