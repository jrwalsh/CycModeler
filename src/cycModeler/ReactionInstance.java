package cycModeler;

import java.util.ArrayList;
import java.util.HashMap;

import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Represents a reaction in the reaction network of a model.
 * 
 * @author Jesse
 */
public class ReactionInstance {
	public Reaction parentReaction;
	public Reaction thisReactionFrame;
	public String name;
	public boolean reversible;
	public ArrayList<MetaboliteInstance> reactants;
	public ArrayList<MetaboliteInstance> products;
	
	
	public ReactionInstance(Reaction thisReactionFrame) {
		this.thisReactionFrame = thisReactionFrame;
	}
	
	/**
	 * 
	 * @param parentReactionFrame
	 * @param thisReactionFrame
	 * @param name
	 * @param reversible
	 * @param reactants
	 * @param products
	 */
	public ReactionInstance(Reaction parentReactionFrame, Reaction thisReactionFrame, String name, boolean reversible, ArrayList<MetaboliteInstance> reactants, ArrayList<MetaboliteInstance> products) {
		this.parentReaction = parentReactionFrame;
		this.thisReactionFrame = thisReactionFrame;
		this.name = name;
		this.reversible = reversible;
		this.reactants = reactants;
		this.products = products;
	}
	
	/**
	 * Test if a reaction is balanced by adding up each element on the reactant side and each element on the product side and
	 * comparing the quantity of each.
	 * 
	 * Note: This method does not interpret chemical shorthand (eg R-groups, etc), but does make a special case for electron acceptors and donors.
	 * An acceptor is treated as one "A" (for acceptor), while a donor is treated as 1 "A" and 2 "H" (hydrogen). This method also
	 * assumes strict matching only, so a missing proton or water will result in a failed test for balance, even though these compounds can
	 * sometimes assumed to be present. (Reactions missing a water or proton have been found in EcoCyc on occasion).
	 * 
	 * @return Returns true if reactants and products are elementally balanced, false if not.
	 * Any errors or unreadable/missing formulas return false.
	 */
	protected boolean isReactionBalanced() {
		HashMap<String, Integer> reactantElements = new HashMap<String, Integer>();
		HashMap<String, Integer> productElements = new HashMap<String, Integer>();
		try {
			for (MetaboliteInstance reactant : reactants) {
				// Special Case
				int specialCases = 0;
				if (reactant.metabolite.getLocalID().equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (reactant.metabolite.getLocalID().equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
				switch (specialCases) {
					case 1: {
						if (reactantElements.containsKey("A")) {
							reactantElements.put("A", reactantElements.get("A") + (1*reactant.stoichiometry));
						} else {
							reactantElements.put("A", (1*reactant.stoichiometry));
						}
					} break;
					case 2: {
						if (reactantElements.containsKey("A")) {
							reactantElements.put("A", reactantElements.get("A") + (1*reactant.stoichiometry));
						} else {
							reactantElements.put("A", (1*reactant.stoichiometry));
						}
						if (reactantElements.containsKey("H")) {
							reactantElements.put("H", reactantElements.get("H") + (2*reactant.stoichiometry));
						} else {
							reactantElements.put("H", (2*reactant.stoichiometry));
						}
					} break;
				}
				if (specialCases != 0) {
					continue;
				}
				
				// Regular Case
				for (Object o : reactant.metabolite.getSlotValues("CHEMICAL-FORMULA")) {
					String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
					String element = chemicalFormulaElement.split(",")[0];
					Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
					
					//Add to map
					if (reactantElements.containsKey(element)) {
						reactantElements.put(element, reactantElements.get(element) + (quantity*reactant.stoichiometry));
					} else {
						reactantElements.put(element, (quantity*reactant.stoichiometry));
					}
				}
			}
			
			for (MetaboliteInstance product : products) {
				// Special Case
				int specialCases = 0;
				if (product.metabolite.getLocalID().equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (product.metabolite.getLocalID().equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
				switch (specialCases) {
					case 1: {
						if (productElements.containsKey("A")) {
							productElements.put("A", productElements.get("A") + (1*product.stoichiometry));
						} else {
							productElements.put("A", (1*product.stoichiometry));
						}
					} break;
					case 2: {
						if (productElements.containsKey("A")) {
							productElements.put("A", productElements.get("A") + (1*product.stoichiometry));
						} else {
							productElements.put("A", (1*product.stoichiometry));
						}
						if (productElements.containsKey("H")) {
							productElements.put("H", productElements.get("H") + (2*product.stoichiometry));
						} else {
							productElements.put("H", (1*product.stoichiometry));
						}
					} break;
				}
				if (specialCases != 0) {
					continue;
				}
				
				// Regular Case
				for (Object o : product.metabolite.getSlotValues("CHEMICAL-FORMULA")) {
					String chemicalFormulaElement = o.toString().substring(1, o.toString().length()-1).replace(" ", "");
					String element = chemicalFormulaElement.split(",")[0];
					Integer quantity = Integer.parseInt(chemicalFormulaElement.split(",")[1]);
					
					//Add to map
					if (productElements.containsKey(element)) {
						productElements.put(element, productElements.get(element) + (quantity*product.stoichiometry));
					} else {
						productElements.put(element, (quantity*product.stoichiometry));
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
//			if (key.equalsIgnoreCase("H")) {
//				if (reactantElements.get(key) - productElements.get(key) == 1 || reactantElements.get(key) - productElements.get(key) == -1) {
//					System.out.println("Save reaction with a proton.");
//				}
//			}
			if (reactantElements.get(key) != productElements.get(key)) return false;
		}
		
		return true;
	}
	
	/**
	 * Generates new instantiated reactions for a generic reaction by finding all instances of the metabolite classes in the reaction and
	 * generating every combination of these instances possible. Only metabolite combinations that result in reactions that are
	 * elementally balanced are included in the results.
	 * 
	 * Note: Generation of instances of metabolite classes depends on the existance of this information in EcoCyc. It is known that
	 * this information is currently incomplete, resulting in reactions that should exist but aren't included here. Also, reactions
	 * have been found that should occur but do not pass the elemental balancing step due to missing proton/water molecules. This 
	 * issue is currently left up to manual review.
	 * 
	 * @param origReaction Generic reaction being instantiated
	 * @param reactants Non-generic reactant metabolites
	 * @param products Non-generic product metabolites
	 * @param genericReactants Generic reactant metabolites
	 * @param genericProducts Generic product metabolites
	 * @param reactantSlot Slot name which holds reactants (Usually "RIGHT" or "LEFT" depending on reaction direction)
	 * @param productSlot Slot name which holds products (Usually "RIGHT" or "LEFT" depending on reaction direction)
	 * @return List of newly created, elementally balanced reaction instances
	 */
	protected ArrayList<ReactionInstance> generateInstantiatedReactions(JavacycConnection conn) {
		ArrayList<ReactionInstance> newReactions = new ArrayList<ReactionInstance>();
		
		ArrayList<String> allReactantIDs = new ArrayList<String>();
		ArrayList<String> allProductIDs = new ArrayList<String>();
		ArrayList<Frame> genericReactants = new ArrayList<Frame>();
		ArrayList<Frame> genericProducts = new ArrayList<Frame>();
		ArrayList<Frame> reactants = new ArrayList<Frame>();
		ArrayList<Frame> products = new ArrayList<Frame>();
		String reactantSlot = reactionReactantSlot();
		String productSlot = reactionProductSlot();
		
		try {
			if (thisReactionFrame == null) return null;
			
			// If reaction has specific forms, then assume those forms are already in the model
			if (conn.specificFormsOfReaction(thisReactionFrame.getLocalID()).size() > 0) return null;//TODO
			
			// If reaction cannot be balanced then it cannot be instantiated
			if (thisReactionFrame.hasSlot("CANNOT-BALANCE?") && thisReactionFrame.getSlotValue("CANNOT-BALANCE?") != null) return null;
			
			// Get reactants and products.  Must account for direction of reaction.
			allReactantIDs = thisReactionFrame.getSlotValues(reactantSlot);
			allProductIDs = thisReactionFrame.getSlotValues(productSlot);
			
			for (String reactantID : allReactantIDs) {
				Frame reactant = Frame.load(conn,  reactantID);//loadFrame(reactantID);
				if (conn.getFrameType(reactantID).toUpperCase().equals(":CLASS")) genericReactants.add(reactant);
				else reactants.add(reactant);
			}
			for (String productID : allProductIDs) {
				Frame product = Frame.load(conn,  productID);//loadFrame(productID);
				if (conn.getFrameType(productID).toUpperCase().equals(":CLASS")) genericProducts.add(product);
				else products.add(product);
			}
			
			// Make sure this reaction is a generalized reaction
			if (genericReactants.size() == 0 && genericProducts.size() == 0) return null;
			
			//Generate instantiated reactions
			try {
				// Generate all possible combinations of instances for the general terms
				ArrayList<NamedList> listSet = new ArrayList<NamedList>();
				for (Frame genericTerm : genericReactants) {
					ArrayList<String> instancesOfGeneralTerm = new ArrayList<String>();
					for (Object instance : conn.getClassAllInstances(genericTerm.getLocalID())) instancesOfGeneralTerm.add(instance.toString());
					if (instancesOfGeneralTerm.size() == 0) instancesOfGeneralTerm.add(genericTerm.getLocalID());
					NamedList namedList = new NamedList(genericTerm.getLocalID(), instancesOfGeneralTerm);
					if (!listSet.contains(namedList)) listSet.add(namedList);
				}
				
				for (Frame genericTerm : genericProducts) {
					ArrayList<String> instancesOfGeneralTerm = new ArrayList<String>();
					for (Object instance : conn.getClassAllInstances(genericTerm.getLocalID())) instancesOfGeneralTerm.add(instance.toString());
					if (instancesOfGeneralTerm.size() == 0) instancesOfGeneralTerm.add(genericTerm.getLocalID());
					NamedList namedList = new NamedList(genericTerm.getLocalID(), instancesOfGeneralTerm);
					if (!listSet.contains(namedList)) listSet.add(namedList);
				}
				
				ListCombinationResults termCombinations = listCombinations(listSet);
				
				// Generate the Metabolite objects for reactants and products, which will be static across all new reactions
				ArrayList<MetaboliteInstance> reactantMetabolites = new ArrayList<MetaboliteInstance>();
				ArrayList<MetaboliteInstance> productMetabolites = new ArrayList<MetaboliteInstance>();
				for (Frame reactant : reactants) reactantMetabolites.add(new MetaboliteInstance(conn, thisReactionFrame, reactantSlot, reactant, reactant));
				for (Frame product : products) productMetabolites.add(new MetaboliteInstance(conn, thisReactionFrame, productSlot, product, product));
				
				// For each combination, create a new reaction for it if the reaction is elementally balanced
				for (ArrayList<String> combinationSet : termCombinations.listOfTuples) {
					ReactionInstance newReaction = new ReactionInstance(thisReactionFrame, null, "", thisReactionFrame.isReversible(), new ArrayList<MetaboliteInstance>(), new ArrayList<MetaboliteInstance>());
					
					// Non-generic metabolites
					for (MetaboliteInstance reactant : reactantMetabolites) {
						newReaction.reactants.add(reactant);
					}
					for (MetaboliteInstance product : productMetabolites) {
						newReaction.products.add(product);
					}

					// Generic metabolites
					for (Frame term : genericReactants) {
						MetaboliteInstance newMetabolite = new MetaboliteInstance(conn, thisReactionFrame, reactantSlot, term, Frame.load(conn, combinationSet.get(termCombinations.nameList.indexOf(term.getLocalID()))));
						newReaction.reactants.add(newMetabolite);
					}
					for (Frame term : genericProducts) {
						MetaboliteInstance newMetabolite = new MetaboliteInstance(conn, thisReactionFrame, productSlot, term, Frame.load(conn, combinationSet.get(termCombinations.nameList.indexOf(term.getLocalID()))));
						newReaction.products.add(newMetabolite);
					}
					
					// We need unique names for the newly instantiated reactions. Append the names of the instantiated metabolites to the reaction name.
					String nameModifier = "";
					for (String term : combinationSet) nameModifier += term + "_";
					if (nameModifier.endsWith("_")) nameModifier = nameModifier.substring(0, nameModifier.length()-1);
					
					// If the chosen metabolite instances result in this new reaction having a balanced elemental equation, include it in the new
					// reactionInstances to be returned.
					if (newReaction.isReactionBalanced()) {
						newReaction.name = newReaction.parentReaction.getCommonName() + nameModifier;
						newReactions.add(newReaction);
					}
				}
				
			} catch (PtoolsErrorException e) {
				e.printStackTrace();
			}
			return newReactions;
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * "All possible combinations from a list of sublists problem"
	 * 
	 * This function takes in a list of lists and returns every possible combination of 1 item from each sublist.
	 * Thus, if the lists [1,2,3], [4,5,6], and [7,8,9] were input, then the output would be
	 * [1,4,7], [1,4,8], [1,4,8], [1,5,7], [1,5,8], [1,5,9] ...
	 * This method was written as a way to instantiate general terms in a reaction. Each general term in a reaction has 
	 * a list of possible values, and every possible combination of terms is needed.
	 * 
	 * @param listOfNamedLists List of NamedList objects. Name of list should be the class metabolite, while the list is
	 * each instance of the class metabolite.
	 * @return ListCombinationResults where the name list is a list of all the names of the NamedList input, and a list of tuples
	 * which represent each possible combination of the items in the named list. Order of names in the NameList matches the order
	 * of the items in the tuples.
	 */
	@SuppressWarnings("unchecked")
	private ListCombinationResults listCombinations(ArrayList<NamedList> listOfNamedLists) {
		if (listOfNamedLists == null || listOfNamedLists.size() < 1) return new ListCombinationResults(new ArrayList<String>(), new ArrayList<ArrayList<String>>());
		
		NamedList namedList = listOfNamedLists.remove(0);
		ListCombinationResults results = listCombinations(listOfNamedLists);
		results.nameList.add(namedList.name);
		ArrayList<ArrayList<String>> newListOfTuples = new ArrayList<ArrayList<String>>();
		
		if (results.listOfTuples.size() > 0) {
			for (String item : namedList.list) {
				for (ArrayList<String> tuple : results.listOfTuples) {
					ArrayList<String> newTuple = new ArrayList<String>();
					newTuple = (ArrayList<String>)tuple.clone();
					newTuple.add(item);
					newListOfTuples.add(newTuple);
				}
			}
		} else {
			for (String item : namedList.list) {
				ArrayList<String> tuple = new ArrayList<String>();
				tuple.add(item);
				newListOfTuples.add(tuple);
			}
		}
		
		results.listOfTuples = newListOfTuples;
		
		return results;
	}
	
	/**
	 * Get slot name of Reaction reactants depending on reaction direction. Reversible reactions report reactant slot as "LEFT".
	 * 
	 * @param reaction
	 * @return
	 * @throws PtoolsErrorException
	 */
	protected String reactionReactantSlot() {
		Reaction reaction = null;
		if (thisReactionFrame != null) reaction = thisReactionFrame;
		else if (parentReaction != null) reaction = parentReaction;
		else return null;
		
		try {
			if (reaction.getSlotValue("REACTION-DIRECTION") == null || !reaction.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
				return "LEFT";
			} else {
				return "RIGHT";
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get slot name of Reaction products depending on reaction direction. Reversible reactions report product slot as "RIGHT".
	 * 
	 * @param reaction
	 * @return
	 * @throws PtoolsErrorException
	 */
	protected String reactionProductSlot() {
		Reaction reaction = null;
		if (thisReactionFrame != null) reaction = thisReactionFrame;
		else if (parentReaction != null) reaction = parentReaction;
		else return null;
		
		try {
			if (reaction.getSlotValue("REACTION-DIRECTION") == null || !reaction.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
				return "RIGHT";
			} else {
				return "LEFT";
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * A way to generate SBML reaction IDs for instantiated reactions.
	 * 
	 * @param baseID
	 * @return
	 */
	protected String generateReactionID() {
		String baseID = "";
		if (thisReactionFrame != null) baseID = thisReactionFrame.getLocalID();
		else if (parentReaction != null) baseID = parentReaction.getLocalID();
		else baseID = name;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.reactionPrefix + "" + baseID);
		else return CycModeler.convertToSBMLSafe(CycModeler.reactionPrefix + "_" + baseID);
	}
	
	/**
	 * Test if a reaction is a generic reaction (i.e., it must contain at least one class frame in its reactions or products).
	 * 
	 * @param reaction
	 * @return True if reaction is generic.
	 */
	@SuppressWarnings("unchecked")
	protected boolean isGeneralizedReaction(JavacycConnection conn) {
		boolean result = false;
		try {
			ArrayList<String> leftMetabolites = thisReactionFrame.getSlotValues("LEFT");
			ArrayList<String> rightMetabolites = thisReactionFrame.getSlotValues("RIGHT");
			
			for (String left : leftMetabolites) {
				if (conn.getFrameType(left).toUpperCase().equals(":CLASS")) return true;
			}
			
			for (String right : rightMetabolites) {
				if (conn.getFrameType(right).toUpperCase().equals(":CLASS")) return true;
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Return gene-reaction associations as a string of EcoCyc gene frame IDs in a boolean logic format. This format
	 * is intended for inclusion in SBML models.
	 * 
	 * In this format, the presence of each gene could be represented by TRUE and its absence by FALSE,
	 * and if the statement resolves to TRUE using the rules of boolean logic, than the reaction can be
	 * considered to be functional.
	 * 
	 * Example: Reaction F16BDEPHOS-RXN (FRUCTOSE-1,6-BISPHOSPHATASE) is governed by 4 genes, each of which will produce
	 * an enzyme capable of catalyzing this reaction, thus any one gene is sufficient.  The rule is then something like
	 * (yggF or ybhA or glpX or fbp).
	 * 
	 * Example: Reaction SUCCCOASYN-RXN (SUCCINYL-COA SYNTHETASE) is governed by 2 genes, both of which are required
	 * to produce the enzyme capable of catalyzing this reaction, thus both are necessary.  The rule is then something like
	 * (sucC and sucD).
	 * 
	 * @param reactionID EcoCyc reaction frame ID
	 * @param asBNumber If true, return string with gene b#'s instead of gene frame IDs
	 * @return String of gene-reaction associations
	 * @throws PtoolsErrorException
	 */
	protected String reactionGeneRule(JavacycConnection conn, boolean asBNumber) throws PtoolsErrorException {
		String reactionID = "";
		if (thisReactionFrame != null) reactionID = thisReactionFrame.getLocalID();
		else reactionID = parentReaction.getLocalID();
		
		String orRule = "";
		for (Object enzyme : conn.enzymesOfReaction(reactionID)) {
			String andRule = "";
			for (Object gene : conn.genesOfProtein(enzyme.toString())) {
				String geneID = gene.toString();
				if (asBNumber) {
					try {
						geneID = Frame.load(conn, geneID).getSlotValue("ACCESSION-1").replace("\"", "");
					} catch (Exception e) {
						geneID = gene.toString();
					}
				}
				andRule += geneID + " and ";
			}
			if (andRule.length() > 0) {
				andRule = "(" + andRule.substring(0, andRule.length()-5) + ")";
				orRule += andRule + " or ";
			}
		}
		if (orRule.length() > 0) orRule = orRule.substring(0, orRule.length()-4);
		return orRule;
	}
	
	
	// Internal Classes
	/**
 	 * Internal class to facilitate generic reaction instantiation by holding a metabolite class as "name" and all
 	 * metabolite instances of the class in "list".
 	 * 
 	 * @author Jesse
 	 */
 	private class NamedList {
		public String name;
		public ArrayList<String> list;
		
		public NamedList(String name, ArrayList<String> list) {
			this.name = name;
			this.list = list;
		}
		
		/**
		A shallow test of equality. Test the names of two NamedLists for equality. Does not compare the list itself.
		@return true if both NamedLists have the name. 
		*/
		@Override public boolean equals(Object aThat) {
			//Based on example at http://www.javapractices.com/topic/TopicAction.do?Id=17
			
		    //Check for self-comparison
		    if (this == aThat) return true;

		    //Check for similar class
		    if (!(aThat instanceof NamedList)) return false;
		    
		    //Cast to native type
		    NamedList that = (NamedList)aThat;

		    //Compare frame IDs
		    return this.name.equals(that.name);
		  }

		@Override public int hashCode() {
			return this.name.hashCode();
		  }
	}
 	
 	/**
 	 * Internal class to facilitate generic reaction instantiation by holding the results of the listCombinations method.
 	 * 
 	 * @author Jesse
 	 */
 	private class ListCombinationResults {
 		public ArrayList<String> nameList;
		public ArrayList<ArrayList<String>> listOfTuples;
		
		public ListCombinationResults(ArrayList<String> nameList, ArrayList<ArrayList<String>> listOfTuples) {
			this.nameList = nameList;
			this.listOfTuples = listOfTuples;
		}
 	}
}
