package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.util.ListCombinations;
import edu.iastate.cycmodeler.util.MyParameters;
import edu.iastate.cycmodeler.util.Report;
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
public class ReactionInstance extends AbstractReactionInstance {
	public Reaction reactionFrame_;
	public String reactantSlot_;
	public String productSlot_;
	
	// Initializes the reactants and products to the values in the biocyc database for the given reactionFrame
	public ReactionInstance(Reaction reactionFrame, String name, boolean reversible, String specificLocation) {
		this(reactionFrame, name, reversible, specificLocation, new HashSet<MetaboliteInstance>(), new HashSet<MetaboliteInstance>());
		initializeReactantProductMetaboliteInstances();
	}
	
	// Constructor
	public ReactionInstance(Reaction reactionFrame, String name, boolean reversible, String reactionLocation, HashSet<MetaboliteInstance> reactants, HashSet<MetaboliteInstance> products) {
		this.reactionFrame_ = reactionFrame;
		this.name_ = name;
		this.reversible_ = reversible;
		this.reactionLocation_ = reactionLocation;
		this.reactants_ = reactants;
		this.products_ = products;
		initializeReactantProductSlotVariables();
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
	public ArrayList<InstantiatedReactionInstance> generateInstantiatedReactions() {
		ArrayList<InstantiatedReactionInstance> newReactions = new ArrayList<InstantiatedReactionInstance>();
		
		ArrayList<MetaboliteInstance> genericReactants = new ArrayList<MetaboliteInstance>();
		ArrayList<MetaboliteInstance> genericProducts = new ArrayList<MetaboliteInstance>();
		ArrayList<MetaboliteInstance> reactants = new ArrayList<MetaboliteInstance>();
		ArrayList<MetaboliteInstance> products = new ArrayList<MetaboliteInstance>();
		
		try {
			if (reactionFrame_ == null) {
				Report.instantiation.add("Reaction without a reaction frame.");
				return null;
			}
			
			//If reaction has specific forms, then assume those forms are already in the model //TODO get these explicitly, don't assume already in
			if (CycModeler.conn.specificFormsOfReaction(reactionFrame_.getLocalID()).size() > 0) {
				Report.instantiation.add("Reaction " + reactionFrame_.getLocalID() + " reports having reaction instances, skipping.");
				return null;//TODO should not assume these reactions are already there.  try to add them, and if they are duplicates they will not be added
			}
			
			// If reaction cannot be balanced then it cannot be instantiated
			if (reactionFrame_.hasSlot("CANNOT-BALANCE?") && reactionFrame_.getSlotValue("CANNOT-BALANCE?") != null) {
				Report.instantiation.add("Reaction " + reactionFrame_.getLocalID() + " reports that it cannot be balanced, skipping.");
				return null;
			}
			
			// Sort generic from non-generic reactants and products.
			for (MetaboliteInstance reactant : reactants_) {
				if (CycModeler.conn.getFrameType(reactant.getMetaboliteID()).toUpperCase().equals(":CLASS")) genericReactants.add(reactant);
				else reactants.add(reactant);
			}
			for (MetaboliteInstance product : products_) {
				if (CycModeler.conn.getFrameType(product.getMetaboliteID()).toUpperCase().equals(":CLASS")) genericProducts.add(product);
				else products.add(product);
			}
			
			// Make sure this reaction is a generic reaction
			if (genericReactants.size() == 0 && genericProducts.size() == 0) {
				System.err.println("How did a non-generic reaction get here?");
				return null;
			}
			
			//Generate instantiated reactions
			try {
				// Generate all possible combinations of instances for the generic terms
//				ArrayList<NamedList> listSet = new ArrayList<NamedList>();
//				for (MetaboliteInstance genericTerm : genericReactants) {
//					ArrayList<String> instancesOfGenericTerm = new ArrayList<String>();
//					for (Object instance : conn.getClassAllInstances(genericTerm.getMetaboliteID())) instancesOfGenericTerm.add(instance.toString());
//					if (instancesOfGenericTerm.size() == 0) instancesOfGenericTerm.add(genericTerm.getMetaboliteID());
//					NamedList namedList = new NamedList(genericTerm.getMetaboliteID(), instancesOfGenericTerm);
//					if (!listSet.contains(namedList)) listSet.add(namedList);
//				}
//				
//				for (MetaboliteInstance genericTerm : genericProducts) {
//					ArrayList<String> instancesOfGenericTerm = new ArrayList<String>();
//					for (Object instance : conn.getClassAllInstances(genericTerm.getMetaboliteID())) instancesOfGenericTerm.add(instance.toString());
//					if (instancesOfGenericTerm.size() == 0) instancesOfGenericTerm.add(genericTerm.getMetaboliteID());
//					NamedList namedList = new NamedList(genericTerm.getMetaboliteID(), instancesOfGenericTerm);
//					if (!listSet.contains(namedList)) listSet.add(namedList);
//				}
				
				ListCombinations termCombinations = ListCombinations.listCombinations(CycModeler.conn, genericReactants, genericProducts);
				
				if (termCombinations == null) return null; // ie no instances of generic metabolite available
				
				// For each combination, create a new reaction for it if the reaction is elementally balanced
				for (ArrayList<String> combinationSet : termCombinations.listOfTuples) {
					InstantiatedReactionInstance newReaction = new InstantiatedReactionInstance(reactionFrame_, "", reversible_, reactionLocation_, new HashSet<MetaboliteInstance>(), new HashSet<MetaboliteInstance>());
					
					// Non-generic metabolites
					for (MetaboliteInstance reactant : reactants) {
						newReaction.reactants_.add(new MetaboliteInstance(reactant.getMetaboliteFrame(), reactant.compartment_, reactant.coefficient_));
					}
					for (MetaboliteInstance product : products) {
						newReaction.products_.add(new MetaboliteInstance(product.getMetaboliteFrame(), product.compartment_, product.coefficient_));
					}

					// Generic metabolites -- Create a new MetaboliteInstance by replacing the generic metabolite object with an instance metabolite object while keeping the compartment and stoichiometry the same 
					for (MetaboliteInstance genericReactant : genericReactants) {
						Frame newMetaboliteFrame = Frame.load(CycModeler.conn, combinationSet.get(termCombinations.nameList.indexOf(genericReactant.getMetaboliteID())));
						MetaboliteInstance newMetabolite = new MetaboliteInstance(newMetaboliteFrame, genericReactant.compartment_, genericReactant.coefficient_);
						newReaction.reactants_.add(newMetabolite);
					}
					for (MetaboliteInstance genericProduct : genericProducts) {
						Frame newMetaboliteFrame = Frame.load(CycModeler.conn, combinationSet.get(termCombinations.nameList.indexOf(genericProduct.getMetaboliteID())));
						MetaboliteInstance newMetabolite = new MetaboliteInstance(newMetaboliteFrame, genericProduct.compartment_, genericProduct.coefficient_);
						newReaction.products_.add(newMetabolite);
					}
					
					// If the chosen metabolite instances result in a balanced elemental equation, include it in the new reactionInstances to be returned.
//					if (newReaction.isReactionGeneric()) {  //TODO simply having it be generic does not mean it cannot be balanced.  some generics have formulae
//						Report.instantiation.add("Attempt to instantiate " + reactionFrame_.getLocalID() + " resulted in another generic reaction. This should not happen.");
//					} else 
					if (!newReaction.isReactionBalanced()) {
						Report.instantiation.add("Attempt to instantiate " + reactionFrame_.getLocalID() + " resulted in unbalanced equation.");
					} else {
						newReaction.name_ = newReaction.parentReactionFrame_.getCommonName();
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
	 * Get slot name of Reaction reactants and products depending on reaction direction. Reversible reactions report reactant slot as "LEFT".
	 * Only in the case of reaction-direction given as "RIGHT-TO-LEFT" do we switch the reactant/product slots.
	 */
	protected void initializeReactantProductSlotVariables() {
		try {
			assert reactionFrame_ != null;
			
			if (reactionFrame_.getSlotValue("REACTION-DIRECTION") == null || !reactionFrame_.getSlotValue("REACTION-DIRECTION").equalsIgnoreCase("RIGHT-TO-LEFT")) {
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
		if (reactionFrame_ != null) baseID = reactionFrame_.getLocalID();
		else baseID = name_;
		
		if (baseID.startsWith("_")) return CycModeler.convertToSBMLSafe(CycModeler.parameters.ReactionPrefix + "" + baseID);
		else return CycModeler.convertToSBMLSafe(CycModeler.parameters.ReactionPrefix + "_" + baseID);
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
		String reactionID = reactionFrame_.getLocalID();
		JavacycConnection conn = reactionFrame_.getConnection();
		
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
	 * Overwrite existing reactants_ and products_ by looking up reactionFrame_ in the biocyc database and creating MetaboliteInstances for all
	 * reactants and products found there for this reaction.
	 */
	@SuppressWarnings("unchecked")
	private void initializeReactantProductMetaboliteInstances() {
		reactants_ = new HashSet<MetaboliteInstance>();
		products_ = new HashSet<MetaboliteInstance>();
		try {
			JavacycConnection conn = reactionFrame_.getConnection();
			
			ArrayList<String> reactantIDs = reactionFrame_.getSlotValues(reactantSlot_);
			for (String reactantID : reactantIDs) {
				Frame metabolite = Frame.load(conn, reactantID);
				String compartment = getCompartmentOfMetabolite(reactantID, reactantSlot_);
				int coeficient = 1;
				try {
					coeficient = Integer.parseInt(conn.getValueAnnot(reactionFrame_.getLocalID(), reactantSlot_, reactantID, "COEFFICIENT"));
				} catch (Exception e) {
					coeficient = 1;
				}
				reactants_.add(new MetaboliteInstance(metabolite, compartment, coeficient));
			}
			
			ArrayList<String> productIDs = reactionFrame_.getSlotValues(productSlot_);
			for (String productID : productIDs) {
				Frame metabolite = Frame.load(conn, productID);
				String compartment = getCompartmentOfMetabolite(productID, productSlot_);
				int coeficient = 1;
				try {
					coeficient = Integer.parseInt(conn.getValueAnnot(reactionFrame_.getLocalID(), productSlot_, productID, "COEFFICIENT"));
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
		Reaction reaction = reactionFrame_;
		String compartment = "";
		try {
			JavacycConnection conn = reactionFrame_.getConnection();
			ArrayList<String> locations = reaction.getSlotValues("RXN-LOCATIONS");

			if (locations.isEmpty()) {
				compartment = CycModeler.parameters.DefaultCompartment;
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
				if (reactionLocation_ == null || reactionLocation_.isEmpty() || locations.indexOf(reactionLocation_) == -1) {
					locationIndex = 0;
					System.err.println("Location information expected but not provided.");
				}
				else locationIndex = locations.indexOf(reactionLocation_);

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
			compartment = CycModeler.parameters.DefaultCompartment;
//			System.err.println("Null compartment here, assuming default compartment: " + reaction.getLocalID());
		}
		return compartment;
	}
	
	
	@Override
	protected void addReactant(MetaboliteInstance reactant) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void addProduct(MetaboliteInstance product) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getGeneProteinReactionRule() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String printReaction() {
		String printString = "";
		printString += "ReactionInstance" + "\t" + this.name_ + "\t" + this.reactionFrame_ + "\t";
		for (MetaboliteInstance reactant : reactants_) {
			printString += reactant.getMetaboliteID();
		}
		for (MetaboliteInstance product : products_) {
			printString += product.getMetaboliteID();
		}
		return printString;
	}
	
	
//	// Internal Classes
//	/**
// 	 * Internal class to facilitate generic reaction instantiation by holding a metabolite class as "name" and all
// 	 * metabolite instances of the class in "list".
// 	 * 
// 	 * @author Jesse Walsh
// 	 */
// 	private class NamedList {
//		public String name;
//		public ArrayList<String> list;
//		
//		public NamedList(String name, ArrayList<String> list) {
//			this.name = name;
//			this.list = list;
//		}
//		
//		/**
//		A shallow test of equality. Test the names of two NamedLists for equality. Does not compare the list itself.
//		@return true if both NamedLists have the name. 
//		*/
//		@Override public boolean equals(Object aThat) {
//			//Based on example at http://www.javapractices.com/topic/TopicAction.do?Id=17
//			
//		    //Check for self-comparison
//		    if (this == aThat) return true;
//
//		    //Check for similar class
//		    if (!(aThat instanceof NamedList)) return false;
//		    
//		    //Cast to native type
//		    NamedList that = (NamedList)aThat;
//
//		    //Compare frame IDs
//		    return this.name.equals(that.name);
//		  }
//
//		@Override public int hashCode() {
//			return this.name.hashCode();
//		  }
//	}
 	
// 	/**
// 	 * Internal class to facilitate generic reaction instantiation by holding the results of the listCombinations method.
// 	 * 
// 	 * @author Jesse Walsh
// 	 */
// 	private class ListCombinationResults {
// 		public ArrayList<String> nameList;
//		public ArrayList<ArrayList<String>> listOfTuples;
//		
//		public ListCombinationResults(ArrayList<String> nameList, ArrayList<ArrayList<String>> listOfTuples) {
//			this.nameList = nameList;
//			this.listOfTuples = listOfTuples;
//		}
// 	}
}
