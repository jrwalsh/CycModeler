package edu.iastate.cycmodeler.model;

import java.util.HashMap;
import java.util.HashSet;

import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.javacyco.PtoolsErrorException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class AbstractReactionInstance {
	public String name_;
	public boolean reversible_;
	public HashSet<MetaboliteInstance> reactants_;
	public HashSet<MetaboliteInstance> products_;
	public String reactionLocation_;
	
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
			for (MetaboliteInstance reactant : reactants_) {
				for (Object o : reactant.getMetaboliteFrame().getSlotValues("CHEMICAL-FORMULA")) {
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
			
			for (MetaboliteInstance product : products_) {
				for (Object o : product.getMetaboliteFrame().getSlotValues("CHEMICAL-FORMULA")) {
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
			System.err.println("Could not determing balance for reaction: " + name_);
			return false;
		}
		
		try {
			return elementalDiff(reactantElements, productElements);
		} catch(Exception e) {
			String reactants = "";
			String products = "";
			for (MetaboliteInstance reactant : this.reactants_) reactants += reactant.getMetaboliteFrame().getLocalID() + ",";
			for (MetaboliteInstance product : this.products_) products += product.getMetaboliteFrame().getLocalID() + ",";
			System.err.println("Could not do a diff on reaction " + ((InstantiatedReactionInstance)this).parentReactionFrame_.getLocalID() + " using the reactants " + reactants + " and products " + products);
		}
		
		return false;
		
//		if (!reactantElements.keySet().containsAll(productElements.keySet()) || !productElements.keySet().containsAll(reactantElements.keySet())) return false;
//		for (String key : reactantElements.keySet()) {
////			if (key.equalsIgnoreCase("H")) { //TODO account for reactions which fail to match by commonly omitted elements
////				if (reactantElements.get(key) - productElements.get(key) == 1 || reactantElements.get(key) - productElements.get(key) == -1) {
////					System.out.println("Save reaction with a proton.");
////				}
////			}
//			if (reactantElements.get(key) != productElements.get(key)) return false;
//		}
//		
//		return true;
	}
	
	private boolean elementalDiff(HashMap<String, Integer> reactantElements, HashMap<String, Integer> productElements) {
		HashMap<String, Integer> elementalDiff = new HashMap<String, Integer>();
		
		elementalDiff.putAll(reactantElements);
		for (String key : productElements.keySet()) {
			if (elementalDiff.containsKey(key)) {
				elementalDiff.put(key, elementalDiff.get(key) - productElements.get(key));
			} else elementalDiff.put(key, 0 - productElements.get(key));
		}
		for (String key : elementalDiff.keySet()) {
			if (elementalDiff.get(key) != 0) return false;
		}
		return true;
		
//		if (elementalDiff.keySet().size() == 1 && elementalDiff.keySet().contains("H")) {
//			System.err.println("Reaction " + this.name_ + " doesn't balance over " + elementalDiff.get("H") + " H atom(s).");
//		} else if (elementalDiff.keySet().size() == 2 && elementalDiff.keySet().contains("H") && elementalDiff.keySet().contains("O")) {
//			System.err.println("Reaction " + this.name_ + " doesn't balance over " + elementalDiff.get("H") + " H atom(s) and " + elementalDiff.get("O") + " O atom(s).");
//		}
	}

	protected boolean isReactionGeneric() {
		try {
			for (MetaboliteInstance reactant : reactants_) {
				if (reactant.getMetaboliteFrame().isClassFrame()) return true;
			}
			for (MetaboliteInstance product : products_) {
				if (product.getMetaboliteFrame().isClassFrame()) return true;
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).
	        append(reactants_).
	        append(products_).
	        append(reactionLocation_).
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
				append(this.reactants_, other.reactants_).
				append(this.products_, other.products_).
				append(this.reactionLocation_, other.reactionLocation_).
				isEquals();
	}
	
	public abstract String generateReactionID();
	public abstract String getGeneProteinReactionRule();
	protected abstract void addReactant(MetaboliteInstance reactant);
	protected abstract void addProduct(MetaboliteInstance product);
	public abstract String printReaction();
}
