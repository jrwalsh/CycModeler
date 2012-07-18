package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashMap;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.Pathway;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/**
 * Represents a reaction in the reaction network of a model.
 * 
 * @author Jesse Walsh
 */
public class ReactionInstance {
	public Reaction parentReaction_;
	public Reaction thisReactionFrame_;
	public String name_;
	public boolean reversible_;
	public ArrayList<MetaboliteInstance> reactants_;
	public ArrayList<MetaboliteInstance> products_;
	public String reactantSlot_;
	public String productSlot_;
	public String specificLocation_;
	
	// Used when converting javacyco reactions to cycmodeler reaction instances
	public ReactionInstance(Reaction thisReactionFrame) throws PtoolsErrorException {
		this(null, thisReactionFrame, thisReactionFrame.getCommonName(), thisReactionFrame.isReversible(), null, null, null, true);
	}
	
//	public ReactionInstance(Reaction parentReactionFrame, Reaction thisReactionFrame, String name, boolean reversible) {
//		this(parentReactionFrame, thisReactionFrame, name, reversible, null, null, null, true);
//	}
	
	// Also used when converting javacyco reactions to cycmodeler reaction instances
	public ReactionInstance(Reaction parentReactionFrame, Reaction thisReactionFrame, String name, boolean reversible, String specificLocation) {
		this(parentReactionFrame, thisReactionFrame, name, reversible, specificLocation, null, null, true);
	}
	
	// The "all included" constructor
	public ReactionInstance(Reaction parentReactionFrame, Reaction thisReactionFrame, String name, boolean reversible, String specificLocation, ArrayList<MetaboliteInstance> reactants, ArrayList<MetaboliteInstance> products, boolean initializeReactantsProducts) {
		parentReaction_ = parentReactionFrame;
		thisReactionFrame_ = thisReactionFrame;
		name_ = name;
		reversible_ = reversible;
		specificLocation_ = specificLocation;
		reactants_ = reactants;
		products_ = products;
		initializeReactantProductSlotVariables();
		
		if (initializeReactantsProducts) initializeReactantProductMetaboliteInstances();
	}

	
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
				// Special Case
				int specialCases = 0;
				if (reactant.metabolite_.getLocalID().equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (reactant.metabolite_.getLocalID().equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
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
				for (Object o : reactant.metabolite_.getSlotValues("CHEMICAL-FORMULA")) {
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
				// Special Case
				int specialCases = 0;
				if (product.metabolite_.getLocalID().equalsIgnoreCase("|Acceptor|")) specialCases = 1;
				else if (product.metabolite_.getLocalID().equalsIgnoreCase("|Donor-H2|")) specialCases = 2;
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
				for (Object o : product.metabolite_.getSlotValues("CHEMICAL-FORMULA")) {
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
	
	/**
	 * Generates new instantiated reactions for a generic reaction by finding all instances of the metabolite classes in the reaction and
	 * generating every combination of these instances possible. Only metabolite combinations that result in reactions that are
	 * elementally balanced are included in the results.
	 * 
	 * Note: Generation of instances of metabolite classes depends on the existence of this information in EcoCyc. It is known that
	 * this information is currently incomplete, resulting in reactions that should exist but aren't included here. Also, reactions
	 * have been found that should occur but do not pass the elemental balancing step due to missing proton/water molecules. This 
	 * issue is currently left up to manual review.
	 * 
	 * @return List of newly created, elementally balanced reaction instances
	 */
	protected ArrayList<ReactionInstance> generateInstantiatedReactions() {
		JavacycConnection conn;
		ArrayList<ReactionInstance> newReactions = new ArrayList<ReactionInstance>();
		
		ArrayList<MetaboliteInstance> genericReactants = new ArrayList<MetaboliteInstance>();
		ArrayList<MetaboliteInstance> genericProducts = new ArrayList<MetaboliteInstance>();
		ArrayList<MetaboliteInstance> nonGenericReactantMetabolites = new ArrayList<MetaboliteInstance>();
		ArrayList<MetaboliteInstance> nonGenericProductMetabolites = new ArrayList<MetaboliteInstance>();
		
		try {
			if (thisReactionFrame_ == null) return null;
			
			conn = thisReactionFrame_.getConnection();
			
			// If reaction has specific forms, then assume those forms are already in the model
			if (conn.specificFormsOfReaction(thisReactionFrame_.getLocalID()).size() > 0) return null;//TODO
			
			// If reaction cannot be balanced then it cannot be instantiated
			if (thisReactionFrame_.hasSlot("CANNOT-BALANCE?") && thisReactionFrame_.getSlotValue("CANNOT-BALANCE?") != null) return null;
			
			// Sort generic from non-generic reactants and products.
			for (MetaboliteInstance reactant : reactants_) {
				if (conn.getFrameType(reactant.metabolite_.getLocalID()).toUpperCase().equals(":CLASS")) genericReactants.add(reactant);
				else nonGenericReactantMetabolites.add(reactant);
			}
			for (MetaboliteInstance product : products_) {
				if (conn.getFrameType(product.metabolite_.getLocalID()).toUpperCase().equals(":CLASS")) genericProducts.add(product);
				else nonGenericProductMetabolites.add(product);
			}
			
			// Make sure this reaction is a generic reaction
			if (genericReactants.size() == 0 && genericProducts.size() == 0) return null;
			
			//Generate instantiated reactions
			try {
				// Generate all possible combinations of instances for the generic terms
				ArrayList<NamedList> listSet = new ArrayList<NamedList>();
				for (MetaboliteInstance genericTerm : genericReactants) {
					ArrayList<String> instancesOfGenericTerm = new ArrayList<String>();
					for (Object instance : conn.getClassAllInstances(genericTerm.metabolite_.getLocalID())) instancesOfGenericTerm.add(instance.toString());
					if (instancesOfGenericTerm.size() == 0) instancesOfGenericTerm.add(genericTerm.metabolite_.getLocalID());
					NamedList namedList = new NamedList(genericTerm.metabolite_.getLocalID(), instancesOfGenericTerm);
					if (!listSet.contains(namedList)) listSet.add(namedList);
				}
				
				for (MetaboliteInstance genericTerm : genericProducts) {
					ArrayList<String> instancesOfGenericTerm = new ArrayList<String>();
					for (Object instance : conn.getClassAllInstances(genericTerm.metabolite_.getLocalID())) instancesOfGenericTerm.add(instance.toString());
					if (instancesOfGenericTerm.size() == 0) instancesOfGenericTerm.add(genericTerm.metabolite_.getLocalID());
					NamedList namedList = new NamedList(genericTerm.metabolite_.getLocalID(), instancesOfGenericTerm);
					if (!listSet.contains(namedList)) listSet.add(namedList);
				}
				
				ListCombinationResults termCombinations = listCombinations(listSet);
				
				// For each combination, create a new reaction for it if the reaction is elementally balanced
				for (ArrayList<String> combinationSet : termCombinations.listOfTuples) {
					ReactionInstance newReaction = new InstantiatedReactionInstance(thisReactionFrame_, "", reversible_, specificLocation_, new ArrayList<MetaboliteInstance>(), new ArrayList<MetaboliteInstance>());
					
					// Non-generic metabolites
					for (MetaboliteInstance reactant : nonGenericReactantMetabolites) {
						newReaction.reactants_.add(new MetaboliteInstance(reactant.metabolite_, reactant.compartment_, reactant.coefficient_));
					}
					for (MetaboliteInstance product : nonGenericProductMetabolites) {
						newReaction.products_.add(new MetaboliteInstance(product.metabolite_, product.compartment_, product.coefficient_));
					}

					// Generic metabolites -- Create a new MetaboliteInstance by replacing the old generic metabolite frame object with the new metabolite frame while keeping the compartment and stoichiometry the same 
					for (MetaboliteInstance genericReactant : genericReactants) {
						Frame newMetaboliteFrame = Frame.load(conn, combinationSet.get(termCombinations.nameList.indexOf(genericReactant.metabolite_.getLocalID())));
						MetaboliteInstance newMetabolite = new MetaboliteInstance(newMetaboliteFrame, genericReactant.compartment_, genericReactant.coefficient_);
						newReaction.reactants_.add(newMetabolite);
					}
					for (MetaboliteInstance genericProduct : genericProducts) {
						Frame newMetaboliteFrame = Frame.load(conn, combinationSet.get(termCombinations.nameList.indexOf(genericProduct.metabolite_.getLocalID())));
						MetaboliteInstance newMetabolite = new MetaboliteInstance(newMetaboliteFrame, genericProduct.compartment_, genericProduct.coefficient_);
						newReaction.products_.add(newMetabolite);
					}
					
					// If the chosen metabolite instances result in this new reaction having a balanced elemental equation, include it in the new
					// reactionInstances to be returned.
					if (newReaction.isReactionBalanced()) {
						// We need unique names for the newly instantiated reactions. Append the names of the instantiated metabolites to the reaction name.
						String nameModifier = "";
						for (String term : combinationSet) nameModifier += term + "_";
						if (nameModifier.endsWith("_")) nameModifier = nameModifier.substring(0, nameModifier.length()-1);
								
						newReaction.name_ = newReaction.parentReaction_.getCommonName() + nameModifier;
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
	 * This method was written as a way to instantiate generic terms in a reaction. Each generic term in a reaction has 
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
	 * Get slot name of Reaction reactants and products depending on reaction direction. Reversible reactions report reactant slot as "LEFT".
	 * Only in the case of reaction-direction given as "RIGHT-TO-LEFT" do we switch the reactant/product slots.
	 */
	protected void initializeReactantProductSlotVariables() {
		Reaction reaction = null;
		if (thisReactionFrame_ != null) reaction = thisReactionFrame_;
		else if (parentReaction_ != null) reaction = parentReaction_;
		
		try {
			assert reaction != null;
			
			if (reaction.getSlotValue("REACTION-DIRECTION") == null || !reaction.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
				reactantSlot_ = "LEFT";
				productSlot_ = "RIGHT";
			} else {
				reactantSlot_ = "RIGHT";
				productSlot_ = "LEFT";
			}
		} catch (Exception e) {
			reactantSlot_ = "LEFT";
			productSlot_ = "RIGHT";
		}
	}
	
	/**
	 * A way to generate SBML reaction IDs for instantiated reactions.
	 */
	public String generateReactionID() {
		String baseID = "";
		if (thisReactionFrame_ != null) baseID = thisReactionFrame_.getLocalID();
		else if (parentReaction_ != null) baseID = parentReaction_.getLocalID();
		else baseID = name_;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "" + baseID);
		else return CycModeler.convertToSBMLSafe(CycModeler.ReactionPrefix + "_" + baseID);
	}
	
	/**
	 * Test if a reaction is a generic reaction (i.e., it must contain at least one class frame in its reactions or products).
	 * 
	 * @return True if reaction is generic.
	 */
	@SuppressWarnings("unchecked")
	protected boolean isGenericReaction(JavacycConnection conn) {
		try {
			ArrayList<String> leftMetabolites = thisReactionFrame_.getSlotValues("LEFT");
			ArrayList<String> rightMetabolites = thisReactionFrame_.getSlotValues("RIGHT");
			
			for (String left : leftMetabolites) {
				if (conn.getFrameType(left).toUpperCase().equals(":CLASS")) return true;
			}
			
			for (String right : rightMetabolites) {
				if (conn.getFrameType(right).toUpperCase().equals(":CLASS")) return true;
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return false;
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
	public String reactionGeneRule(boolean asBNumber) throws PtoolsErrorException {
		Reaction reaction = null;
		if (thisReactionFrame_ != null) reaction = thisReactionFrame_;
		else reaction = parentReaction_;
		
		String reactionID = reaction.getLocalID();
		JavacycConnection conn = reaction.getConnection();
		
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
	
	/**
	 * Overwrite existing reactants_ and products_ by looking up thisReactionFrame_ in the database and creating MetaboliteInstances for all
	 * reactants and products found there for this reaction.
	 */
	@SuppressWarnings("unchecked")
	private void initializeReactantProductMetaboliteInstances() {
		reactants_ = new ArrayList<MetaboliteInstance>();
		products_ = new ArrayList<MetaboliteInstance>();
		try {
			JavacycConnection conn = thisReactionFrame_.getConnection();
			
			ArrayList<String> reactantIDs = thisReactionFrame_.getSlotValues(reactantSlot_);
			for (String reactantID : reactantIDs) {
				Frame metabolite = Frame.load(conn, reactantID);
				String compartment = getCompartmentOfMetabolite(reactantID, reactantSlot_);
				int coeficient = 1;
				try {
					coeficient = Integer.parseInt(conn.getValueAnnot(thisReactionFrame_.getLocalID(), reactantSlot_, reactantID, "COEFFICIENT"));
				} catch (Exception e) {
					coeficient = 1;
				}
				reactants_.add(new MetaboliteInstance(metabolite, compartment, coeficient));
			}
			
			ArrayList<String> productIDs = thisReactionFrame_.getSlotValues(productSlot_);
			for (String productID : productIDs) {
				Frame metabolite = Frame.load(conn, productID);
				String compartment = getCompartmentOfMetabolite(productID, productSlot_);
				int coeficient = 1;
				try {
					coeficient = Integer.parseInt(conn.getValueAnnot(thisReactionFrame_.getLocalID(), productSlot_, productID, "COEFFICIENT"));
				} catch (Exception e) {
					coeficient = 1;
				}
				products_.add(new MetaboliteInstance(metabolite, compartment, coeficient));
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Warning: reactions that occur in multiple compartments should be separate reactions in the model. Otherwise, reactions with multiple locations will default
	 * to the first location in the list. If no location is given for this reaction, then it defaults to the DefaultCompartment.  SpecificLocation setting
	 * is only used to disambiguate the "duplicate" reactions that occur when a reaction has multiple locations.
	 * 
	 * @param metaboliteID
	 * @param slot
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected String getCompartmentOfMetabolite(String metaboliteID, String slot) {
		Reaction reaction = thisReactionFrame_;
		String compartment = "";
		try {
			JavacycConnection conn = thisReactionFrame_.getConnection();
			ArrayList<String> locations = reaction.getSlotValues("RXN-LOCATIONS");

			if (locations.isEmpty()) {
				compartment = CycModeler.DefaultCompartment;
			} else if (locations.size() == 1) {
				boolean isSpace;
				try { 
					isSpace = conn.instanceAllInstanceOfP("CCO-SPACE", locations.get(0));
				} catch (PtoolsErrorException e) {
					isSpace = false; // Throws an exception when the location is a "special symbol unique to this slot", in which case we treat it as a type T reaction
				}
				
				if (isSpace) compartment = locations.get(0); // Since label is a child of CCO-SPACE, we treat this as a type S reaction according to EcoCyc
				else {
					// Label is a child of CCO-MEMBRANE, or a special symbol unique to this slot.  We treat these reactions as type T reactions according to EcoCyc
					HashMap<String, String> labelMap = new HashMap<String, String>();
					for (String label : conn.getAllAnnotLabels(reaction.getLocalID(), "RXN-LOCATIONS", locations.get(0))) {
						labelMap.put(label, conn.getValueAnnot(reaction.getLocalID(), "RXN-LOCATIONS", locations.get(0), label));
					}
					
					compartment = labelMap.get(conn.getValueAnnot(reaction.getLocalID(), slot, metaboliteID, "COMPARTMENT"));
				}
			} else {
				// If there are multiple possible locations for this reactionInstance, only one can be processed.  We process the location that matches the
				// specificLocation variable.  If specificLocation does not match, then we only process the first reaction location in the list.
				// Reactions that occur in multiple compartments should be separate reactions.
				int locationIndex;
				if (specificLocation_ == null || specificLocation_.isEmpty() || locations.indexOf(specificLocation_) == -1) {
					locationIndex = 0;
					System.err.println("Location information expected but not provided.");
				}
				else locationIndex = locations.indexOf(specificLocation_);

				boolean isSpace;
				try { 
					isSpace = conn.instanceAllInstanceOfP("CCO-SPACE", locations.get(locationIndex));
				} catch (PtoolsErrorException e) {
					isSpace = false; // Throws an exception when the location is a "special symbol unique to this slot", in which case we treat it as a type T reaction
				}
				
				if (isSpace) compartment = locations.get(locationIndex); // Since label is a child of CCO-SPACE, we treat this as a type S reaction according to EcoCyc
				else {
					// Label is a child of CCO-MEMBRANE, or a special symbol unique to this slot.  We treat these reactions as type T reactions according to EcoCyc
					HashMap<String, String> labelMap = new HashMap<String, String>();
					for (String label : conn.getAllAnnotLabels(reaction.getLocalID(), "RXN-LOCATIONS", locations.get(locationIndex))) {
						labelMap.put(label, conn.getValueAnnot(reaction.getLocalID(), "RXN-LOCATIONS", locations.get(locationIndex), label));
					}
					
					compartment = labelMap.get(conn.getValueAnnot(reaction.getLocalID(), slot, metaboliteID, "COMPARTMENT"));
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		// For the sake of viability of the model, any metabolite that does not have compartment information is assumed to be in the cytoplasm.  While
		// this may not always be correct, it helps more problems than it causes.  In particular, with electron transfer reactions, the actual location
		// of the metabolite may be in the membrane, but we assume the cytoplasm for network connectivity reasons.
		if (compartment == null || compartment.equalsIgnoreCase("")) {
			compartment = CycModeler.DefaultCompartment;
//			System.err.println("Null compartment here, assuming default compartment: " + reaction.getLocalID());
		}
		return compartment;
	}
	
	
	// Internal Classes
	/**
 	 * Internal class to facilitate generic reaction instantiation by holding a metabolite class as "name" and all
 	 * metabolite instances of the class in "list".
 	 * 
 	 * @author Jesse Walsh
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
 	 * @author Jesse Walsh
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
