package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashSet;


import edu.iastate.cycmodeler.logic.CycModeler;
import edu.iastate.cycmodeler.util.Report;
import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;
import edu.iastate.javacyco.Reaction;

/*
 * The reaction network class shall maintain validity of the underlying model itself.  This means that it shall not allow unbalanced reactions,
 * generic reactions, duplicate reactions, or duplicate identifiers.  Validity specific to the SBML model itself, such as invalid characters in the
 * identifiers, will be handled in a separate class.
 */
public class ReactionNetwork {
	public HashSet<AbstractReactionInstance> Reactions;

	// Network modification statistics
	private Report report;
	
	public ReactionNetwork (ArrayList<Reaction> reactions) {
		this.Reactions = new HashSet<AbstractReactionInstance>();
		importJavacycReactions(reactions);
		
		report = new Report();
		report.setTotalInitialReactionsCount(Reactions.size());
	}
	
	/**
	 * Creates exchange reactions for each metabolite that, for any reaction in reactions list, is also in compartment at least once. Adds these new reactions
	 * to the reactions_ variable.
	 * 
	 * @param compartment Compartment in which exchange reactions to metabolites will be created
	 * @return Exchange reactions created
	 */
	public ArrayList<AbstractReactionInstance> addBoundaryReactionsByCompartment(String compartment) {
		ArrayList<Frame> exchangeMetabolites = new ArrayList<Frame>();
		ArrayList<String> exchangeMetaboliteIDs = new ArrayList<String>();
		
		assert Reactions != null;
		
		// For each reaction, check for reactants or products which are consumed or produced in boundary compartment
		for (AbstractReactionInstance reaction : Reactions) {
			for (MetaboliteInstance reactant : reaction.reactants_) {
				if (reactant.compartment_.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(reactant.getMetaboliteID())) {
					exchangeMetabolites.add(reactant.getMetaboliteFrame());
					exchangeMetaboliteIDs.add(reactant.getMetaboliteID());
				}
			}
			for (MetaboliteInstance product : reaction.products_) {
				if (product.compartment_.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(product.getMetaboliteID())) {
					exchangeMetabolites.add(product.getMetaboliteFrame());
					exchangeMetaboliteIDs.add(product.getMetaboliteID());
				}
			}
		}
		
		// Generate exchange reactions
		ArrayList<AbstractReactionInstance> exchangeReactions = new ArrayList<AbstractReactionInstance>();
		for (Frame metabolite : exchangeMetabolites) {
//			ArrayList<MetaboliteInstance> reactants = new ArrayList<MetaboliteInstance>();
//			reactants.add(new MetaboliteInstance(metabolite, compartment, 1));
//			ArrayList<MetaboliteInstance> products = new ArrayList<MetaboliteInstance>();
//			products.add(new MetaboliteInstance(metabolite, CycModeler.BoundaryCompartmentName, 1));
			exchangeReactions.add(new ExchangeReactionInstance(metabolite.getLocalID() + "_" + CycModeler.parameters.ExchangeReactionSuffix, metabolite, compartment));
		}
		
		addReactionsToNetwork(exchangeReactions);
		
		report.setBoundaryMetabolitesFound(exchangeMetabolites.size());
		report.setBoundaryReactionsAdded(exchangeReactions.size());
		
		return exchangeReactions;
	}
	
	/**
	 * 
	 * @param compartment1 Compartment to look for metabolites which might diffuse across a membrane
	 * @param compartment2 Compartment into which metabolites will diffuse
	 * @param maxSize Maximum size of metabolites that can freely diffuse. Any larger than this and they will be ignored. Size in daltons
	 * @return
	 */
	public ArrayList<AbstractReactionInstance> addPassiveDiffusionReactions(String compartment1, String compartment2, float maxSize) {
		ArrayList<Frame> diffusionMetabolites = new ArrayList<Frame>();
		ArrayList<String> diffusionMetaboliteIDs = new ArrayList<String>();
		
		assert Reactions != null;
		
		// For each reaction, check for reactants or products which are consumed or produced in compartment1
		for (AbstractReactionInstance reaction : Reactions) {
			for (MetaboliteInstance reactant : reaction.reactants_) {
				if (reactant.compartment_.equalsIgnoreCase(compartment1) && !diffusionMetaboliteIDs.contains(reactant.getMetaboliteID())) {
					Float weight = (float) -1.0;
					try {
						String weightString = reactant.getMetaboliteFrame().getSlotValue("MOLECULAR-WEIGHT");
						if (weightString.contains("d")) weightString = weightString.substring(0, weightString.indexOf("d")-1);
						weight = Float.parseFloat(weightString);
					} catch (Exception e) {
						try {
							System.err.println(reactant.getMetaboliteFrame().getCommonName() + " : " + reactant.getMetaboliteFrame().getSlotValue("MOLECULAR-WEIGHT"));
						} catch (PtoolsErrorException e1) {
							e1.printStackTrace();
						}
					}
					if (weight <= maxSize) {
						diffusionMetabolites.add(reactant.getMetaboliteFrame());
						diffusionMetaboliteIDs.add(reactant.getMetaboliteID());
					}
				}
			}
			for (MetaboliteInstance product : reaction.products_) {
				if (product.compartment_.equalsIgnoreCase(compartment1) && !diffusionMetaboliteIDs.contains(product.getMetaboliteID())) {
					Float weight = (float) -1.0;
					try {
						String weightString = product.getMetaboliteFrame().getSlotValue("MOLECULAR-WEIGHT");
						if (weightString.contains("d")) weightString = weightString.substring(0, weightString.indexOf("d")-1);
						weight = Float.parseFloat(weightString);
					} catch (Exception e) {
						try {
							System.err.println(product.getMetaboliteFrame().getCommonName() + " : " + product.getMetaboliteFrame().getSlotValue("MOLECULAR-WEIGHT"));
						} catch (PtoolsErrorException e1) {
							e1.printStackTrace();
						}
					}
					if (weight <= maxSize) {
						diffusionMetabolites.add(product.getMetaboliteFrame());
						diffusionMetaboliteIDs.add(product.getMetaboliteID());
					}
				}
			}
		}
		
		// Generate diffusion reactions
		ArrayList<AbstractReactionInstance> diffusionReactions = new ArrayList<AbstractReactionInstance>();
		for (Frame metabolite : diffusionMetabolites) {
			HashSet<MetaboliteInstance> reactants = new HashSet<MetaboliteInstance>();
			reactants.add(new MetaboliteInstance(metabolite, compartment1, 1));
			HashSet<MetaboliteInstance> products = new HashSet<MetaboliteInstance>();
			products.add(new MetaboliteInstance(metabolite, compartment2, 1));
//			diffusionReactions.add(new ReactionInstance(null, null, metabolite.getLocalID() + "_" + "passiveDiffusionReaction", true, null, reactants, products));
			diffusionReactions.add(new DiffusionReactionInstance(metabolite.getLocalID() + "_" + "passiveDiffusionReaction", compartment1, compartment2, reactants, products));
		}
		
		addReactionsToNetwork(diffusionReactions);
		
		report.setDiffusionMetabolitesFound(diffusionMetabolites.size());
		report.setDiffusionReactionsAdded(diffusionReactions.size());
		
		return diffusionReactions;
	}
	
	/**
	 * TODO
	 */
	private ArrayList<ReactionInstance> addBoundaryReactionsByID(String boundaryCompartment, ArrayList<String> exchangeMetaboliteIDs) throws PtoolsErrorException {
		ArrayList<ReactionInstance> exchangeReactions = new ArrayList<ReactionInstance>();
//		for (String metaboliteID : exchangeMetaboliteIDs) {
//			Frame metabolite = Frame.load(conn, metaboliteID);
//			ArrayList<Metabolite> reactants = new ArrayList<Metabolite>();
//			reactants.add(new Metabolite(metabolite, boundaryCompartment, 1, getChemicalFormula(metabolite)));
//			ArrayList<Metabolite> products = new ArrayList<Metabolite>();
//			products.add(new Metabolite(metabolite, BoundaryCompartmentName, 1, getChemicalFormula(metabolite)));
//			exchangeReactions.add(new ReactionInstance(null, null, metabolite.getLocalID() + "_" + ExchangeReactionSuffix, true, reactants, products));
//		}
//		
		return exchangeReactions;
	}
	
//	public FilterResults initializeGenomeScaleReactionNetwork(ArrayList<String> classToFilter, ArrayList<String> reactionsToFilter) {
//		ArrayList<String> filter = new ArrayList<String>();
//		ArrayList<AbstractReactionInstance> removedList = new ArrayList<AbstractReactionInstance>();
//		ArrayList<AbstractReactionInstance> keepList = new ArrayList<AbstractReactionInstance>();
//		ArrayList<AbstractReactionInstance> allReactions = new ArrayList<AbstractReactionInstance>();
//		
//		try {
//			allReactions = reactionListToReactionInstances(Reaction.all(conn));
//
//			if (classToFilter != null) {
//				for (String reactionClass : classToFilter) {
//					for (Object reaction : conn.getClassAllInstances(reactionClass)) filter.add(reaction.toString());
//				}
//			}
//			
//			if (reactionsToFilter != null) {
//				for (String reaction : reactionsToFilter) filter.add(reaction);
//			}
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
//		
//		for (AbstractReactionInstance reaction : allReactions) {
//			if (reaction instanceof ReactionInstance) {
//				if (filter.contains(((ReactionInstance)reaction).reactionFrame_.getLocalID())) removedList.add(reaction);
//				else keepList.add(reaction);
//			}
//		}
//		
//		for (AbstractReactionInstance reaction : keepList) {
////			if any reactant or product is an instance of |Proteins|, |DNA|, |RNA|, than filter reaction out.
//			for (MetaboliteInstance reactant : reaction.reactants_) {
//				if ()
//			}
//		}
//		
//		addReactionsToNetwork(keepList);
//		report.setFilteredReactions(removedList.size());
//		
//		return new FilterResults(keepList, removedList);
//	}
	
//	//TODO rewrite with generic class/reaction/metabolite class
//	public void initializeReactionNetwork() {
//		ArrayList<AbstractReactionInstance> keepList2 = new ArrayList<AbstractReactionInstance>();
//		
//		try {
//			ArrayList<Reaction> dbReactions = Reaction.all(conn);
//			dbReactions.
//			ArrayList<ReactionInstance> allReactions = reactionListToReactionInstances(Reaction.all(conn));
//			ArrayList<ReactionInstance> keepList = new ArrayList<ReactionInstance>();
//			ArrayList<ReactionInstance> removedList = new ArrayList<ReactionInstance>();
//			
//			for (ReactionInstance reaction : allReactions) {
//				if (reaction.reactionFrame_.isGFPClass("|Polynucleotide-Reactions|")) removedList.add(reaction);
//				else if (reaction.reactionFrame_.isGFPClass("|Protein-Reactions|")) removedList.add(reaction);
//				else if (reaction.reactionFrame_.isGFPClass("|RNA-Reactions|")) removedList.add(reaction);
//				else keepList.add(reaction);
//			}
//			
//			for (ReactionInstance reaction : keepList) {
//				ArrayList<MetaboliteInstance> mets = new ArrayList<MetaboliteInstance>();
//				mets.addAll(reaction.reactants_);
//				mets.addAll(reaction.products_);
//				boolean match = false;
//				for (MetaboliteInstance met : mets) {
//					if (met.getMetaboliteFrame().isGFPClass("|Proteins|")) match = true;
////					else if (met.getMetaboliteFrame().isGFPClass("|DNA-N|"))
//					else if (met.getMetaboliteFrame().isGFPClass("|Nucleotides|")) match = true;
//				}
//				if (match) removedList.add(reaction);
//				else keepList2.add(reaction);
//			}
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
//		
//		addReactionsToNetwork(keepList2);
//	}
	
	
//	/**
//	 * Remove all Reactions from the ArrayList reactions_ which are either an instance of any of the EcoCyc classes in classToFilter, or
//	 * are explicitly named with their EcoCyc Frame ID in the reactionsToFilter list. 
//	 * 
//	 * @param Reactions List of Reactions to which the filter will be applied
//	 * @param classToFilter EcoCyc Frame ID of a class frame, instances of which should be removed from reactions
//	 * @param reactionsToFilter EcoCyc Frame ID of a reaction frame which should be removed from reactions
//	 * @return FilterResults containing the filtered reaction list and a list of reactions actually removed
//	 */
//	public FilterResults filterReactions(ArrayList<String> classToFilter, ArrayList<String> reactionsToFilter) {
//		ArrayList<String> filter = new ArrayList<String>();
//		ArrayList<AbstractReactionInstance> removedList = new ArrayList<AbstractReactionInstance>();
//		ArrayList<AbstractReactionInstance> keepList = new ArrayList<AbstractReactionInstance>();
//		
//		try {
//			if (classToFilter != null) {
//				for (String reactionClass : classToFilter) {
//					for (Object reaction : conn.getClassAllInstances(reactionClass)) filter.add(reaction.toString());
//				}
//			}
//			
//			if (reactionsToFilter != null) {
//				for (String reaction : reactionsToFilter) filter.add(reaction);
//			}
//		} catch (PtoolsErrorException e) {
//			e.printStackTrace();
//		}
//		
//		for (AbstractReactionInstance reaction : this.Reactions) {
//			if (reaction instanceof ReactionInstance) {
//				if (filter.contains(((ReactionInstance)reaction).reactionFrame_.getLocalID())) removedList.add(reaction);
//				else keepList.add(reaction);
//			}
//		}
//		
//		addReactionsToNetwork(keepList);
//		report.setFilteredReactions(removedList.size());
//		
//		return new FilterResults(keepList, removedList);
//	}
	
	/**
	 * Will take in a list of reactions, find any generic reactions (according to EcoCyc), and will attempt to return instances of the
	 * generic reactions from data in EcoCyc.
	 * 
	 * Note: Metabolite class instance information is not complete, resulting in reactions that should exist but aren't included here.
	 * Also, reactions have been found that should occur but do not pass the elemental balancing step due to missing proton/water
	 * molecules.
	 * 
	 * @return Results of the attempt to instantiate generic reactions
	 */
	public InstantiationResults generateSpecificReactionsFromGenericReactions() {
		InstantiationResults instantiationResults = new InstantiationResults(new ArrayList<AbstractReactionInstance>(), new ArrayList<AbstractReactionInstance>(), new ArrayList<AbstractReactionInstance>(), new ArrayList<AbstractReactionInstance>());
		
		for (AbstractReactionInstance reaction : Reactions) {
			if (reaction instanceof ReactionInstance) {
				if (((ReactionInstance) reaction).isGenericReaction(CycModeler.conn)) {
					instantiationResults.genericReactionsFound.add(reaction);
					ArrayList<InstantiatedReactionInstance> instantiatedReactions = ((ReactionInstance) reaction).generateInstantiatedReactions();
					if (instantiatedReactions != null && instantiatedReactions.size() > 0) {
						instantiationResults.instantiatedReactions.addAll(instantiatedReactions);
					} else {
						instantiationResults.genericReactionsFailedToInstantiate.add(reaction);
					}
				} else {
					instantiationResults.nonGenericReaction.add(reaction);
				}
			}
		}
		
//		addReactionsToNetwork(instantiationResults.nonGenericReaction);
		//Reactions = new HashSet<AbstractReactionInstance>();
		for (AbstractReactionInstance genericReaction : instantiationResults.genericReactionsFailedToInstantiate) Reactions.remove(genericReaction);
		addReactionsToNetwork(instantiationResults.instantiatedReactions);
//		Reactions = instantiationResults.nonGenericReaction;
//		Reactions.addAll(instantiationResults.instantiatedReactions);
		
		report.setGenericReactionsFound(instantiationResults.genericReactionsFound.size());
		report.setGenericReactionsInstantiated(instantiationResults.genericReactionsFound.size() - instantiationResults.genericReactionsFailedToInstantiate.size());
		report.setInstantiatedReactions(instantiationResults.instantiatedReactions.size());
		
		return instantiationResults;
	}
	
	public void printNetworkStatistics() {
		report.setTransportReactions(countTransportReactions());
		report.setTotalReactions(Reactions.size());
		System.out.println(report.report());
//		System.out.println("Writing statistics ...");
//		System.out.println("All reactions : " + totalStartingReactions_);
//		System.out.println("Removed reactions due to filtering : " + filteredReactions_);
//		System.out.println("Generic reactions found : " + genericReactionsFound_);
//		System.out.println("Generic reactions instantiated : " + genericReactionsInstantiated_);
//		System.out.println("New reactions from generic reaction instantiations : " + instantiatedReactions_);
//		System.out.println("Diffusion metabolites found : " + diffusionMetabolitesFound_);
//		System.out.println("Diffusion reactions added : " + diffusionReactionsAdded_);
//		System.out.println("Boundary metabolites found : " + boundaryMetabolitesFound_);
//		System.out.println("Exchange reactions added : " + boundaryReactionsAdded_);
//		System.out.println("Total transport reactions in network (excluding exchange and diffusion): " + countTransportReactions());
//		System.out.println("Total reactions in network: " + reactions_.size());
	}
	
	@SuppressWarnings("unchecked")
	public int countTransportReactions() {
		ArrayList<String> list;
		int transportReactionCount = 0;
		try {
			list = (ArrayList<String>)CycModeler.conn.getClassAllInstances("|Transport-Reactions|");
			for (AbstractReactionInstance reaction : Reactions) {
				if (reaction instanceof ReactionInstance) {
					if (list.contains(((ReactionInstance)reaction).reactionFrame_.getLocalID())) transportReactionCount++;
				}
				else if (reaction instanceof InstantiatedReactionInstance) {
					if (list.contains(((InstantiatedReactionInstance)reaction).parentReactionFrame_.getLocalID())) transportReactionCount++;
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return transportReactionCount;
	}
	
	/**
	 * Convert an ArrayList of JavaCycO Reaction objects into an ArrayList of ReactionInstances
	 * 
	 * @param reactions ArrayList of Reaction objects
	 * @return ArrayList of ReactionInstance objects
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<AbstractReactionInstance> reactionListToReactionInstances(ArrayList<Reaction> reactions) {
		ArrayList<AbstractReactionInstance> reactionInstances = new ArrayList<AbstractReactionInstance>();
		for (Reaction reaction : reactions) {
			try {
				ArrayList<String> locations = reaction.getSlotValues("RXN-LOCATIONS");
				if (locations.size() > 1) {
					//System.err.println("Split " + reaction.getLocalID() + " into " + (locations.size()));
					//if (debug_) newReactionsFromReactionsSplitByLocation += locations.size()-1;
					for (String location : locations) {
//						reactionInstances.add(new ReactionInstance(null, reaction, reaction.getLocalID() + "_" + location, reaction.isReversible(), location));
						reactionInstances.add(new ReactionInstance(reaction, reaction.getLocalID() + "_" + location, reaction.isReversible(), location));
					}
				} else reactionInstances.addAll(ReactionInstance.getReactionInstanceFromReactionFrames(reaction));//new ReactionInstance(reaction));
			} catch (PtoolsErrorException e) {
				e.printStackTrace();
			}
		}
		
		return reactionInstances;
	}
	
	private void addBiomassEquation() {
		//TODO
	}
	
	private void addATPMaintenanceEquation() {
		//TODO
//		<reaction id="R_ATPM" name="ATP maintenance requirement" reversible="false">
//		<notes>
//		<html:p>Abbreviation: R_ATPM</html:p>
//		<html:p>Synonyms: _0</html:p>
//		<html:p>SUBSYSTEM: Unassigned</html:p>
//		<html:p>Equation: [c] : atp + h2o --&gt; adp + h + pi</html:p>
//		<html:p>Confidence Level: 0</html:p>
//		<html:p>GENE ASSOCIATION: </html:p>
//		</notes>
//		<listOfReactants>
//		<speciesReference species="M_atp_c" stoichiometry="1"/>
//		<speciesReference species="M_h2o_c" stoichiometry="1"/>
//		</listOfReactants>
//		<listOfProducts>
//		<speciesReference species="M_adp_c" stoichiometry="1"/>
//		<speciesReference species="M_h_c" stoichiometry="1"/>
//		<speciesReference species="M_pi_c" stoichiometry="1"/>
//		</listOfProducts>
//		<kineticLaw>
//		<math xmlns="http://www.w3.org/1998/Math/MathML">
//		<ci>FLUX_VALUE</ci>
//		</math>
//		<listOfParameters>
//		<parameter id="LOWER_BOUND" value="8.39" units="mmol_per_gDW_per_hr"/>
//		<parameter id="UPPER_BOUND" value="8.39" units="mmol_per_gDW_per_hr"/>
//		<parameter id="OBJECTIVE_COEFFICIENT" value="0" />
//		<parameter id="FLUX_VALUE" value="0" units="mmol_per_gDW_per_hr"/>
//		</listOfParameters>
//		</kineticLaw>
//		</reaction>
	}
	
	// Network Verification Steps
	private void verifyNetwork() {
		//TODO
	}
	
	public void importJavacycReactions(ArrayList<Reaction> reactions) {
		addReactionsToNetwork(reactionListToReactionInstances(reactions));
	}
	
	public void addReactionsToNetwork(ArrayList<AbstractReactionInstance> reactions) {
		//TODO Detect and handle duplicates.
		for (AbstractReactionInstance reaction : reactions) {
			if (Reactions.contains(reaction)) {
				for (AbstractReactionInstance aReaction : Reactions) {
					if (aReaction.equals(reaction)) {
						System.err.println("Duplicate Reaction: " + ((ReactionInstance)aReaction).reactionFrame_.getLocalID() + " = " + ((ReactionInstance)reaction).reactionFrame_.getLocalID());
					}
				}
			}
			Reactions.add(reaction);
		}
	}
	
	// Internal Classes
	/**
 	 * Internal class which holds results of an attempt to instantiate generic reactions in a list of reactions.
 	 * 
 	 * @author Jesse
 	 */
 	private class InstantiationResults {
 		public ArrayList<AbstractReactionInstance> instantiatedReactions;
 		public ArrayList<AbstractReactionInstance> genericReactionsFound;
 		public ArrayList<AbstractReactionInstance> genericReactionsFailedToInstantiate;
 		public ArrayList<AbstractReactionInstance> nonGenericReaction;
		
		public InstantiationResults(ArrayList<AbstractReactionInstance> instantiatedReactions, ArrayList<AbstractReactionInstance> genericReactionsFound, ArrayList<AbstractReactionInstance> genericReactionsFailedToInstantiate, ArrayList<AbstractReactionInstance> nonGenericReaction) {
			this.instantiatedReactions = instantiatedReactions;
			this.genericReactionsFound = genericReactionsFound;
			this.genericReactionsFailedToInstantiate = genericReactionsFailedToInstantiate;
			this.nonGenericReaction = nonGenericReaction;
		}
 	}
 	
 	/**
	 * Internal class to hold the results of filtering reactions to be excluded from the reaction set.
	 * 
	 * @author Jesse
	 */
 	private class FilterResults {
		public ArrayList<AbstractReactionInstance> keepList;
		public ArrayList<AbstractReactionInstance> removedList;
		
		public FilterResults(ArrayList<AbstractReactionInstance> keepList, ArrayList<AbstractReactionInstance> removedList) {
			this.keepList = keepList;
			this.removedList = removedList;
		}
	}
}
