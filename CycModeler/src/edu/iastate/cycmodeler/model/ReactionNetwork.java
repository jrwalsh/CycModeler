package edu.iastate.cycmodeler.model;

import java.util.ArrayList;
import java.util.HashMap;
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
//				if (((ReactionInstance)reaction).reactionFrame_.getLocalID().equalsIgnoreCase("GLYCPDIESTER-RXN")) {
//					System.out.println("Here");
//				}
				
				if (reaction.isReactionGeneric()) {
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
		
		HashSet<AbstractReactionInstance> newReactionList = new HashSet<AbstractReactionInstance>();
		newReactionList.addAll(instantiationResults.nonGenericReaction);
		newReactionList.addAll(instantiationResults.instantiatedReactions);
		Reactions = newReactionList;
//		addReactionsToNetwork(instantiationResults.nonGenericReaction);
		//Reactions = new HashSet<AbstractReactionInstance>();
//		for (AbstractReactionInstance genericReaction : instantiationResults.genericReactionsFailedToInstantiate) Reactions.remove(genericReaction);
//		addReactionsToNetwork(instantiationResults.instantiatedReactions);
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
	
	public void removeCannotBalanceReactions() {
		HashSet<AbstractReactionInstance> newReactionList = new HashSet<AbstractReactionInstance>();
		System.out.println(Reactions.size());
		for (AbstractReactionInstance reaction : Reactions) {
			if (reaction instanceof ReactionInstance) {
				try {
					if (((ReactionInstance)reaction).reactionFrame_.getSlotValue("CANNOT-BALANCE?") != null) {
						System.out.println("Removed reaction with cannot-balance set: " + ((ReactionInstance)reaction).reactionFrame_.getLocalID());
					} else newReactionList.add(reaction);
				} catch (PtoolsErrorException e) {
					e.printStackTrace();
				}
			} else newReactionList.add(reaction);
		}
		Reactions = newReactionList;
		System.out.println(Reactions.size());
	}
	
	public void removeUnbalancedReactions() {
		/*
		 * Specifically removes non-generic unbalanced reactions.  Generic reactions are removed later, and instantiated reactions must be balanced to be kept.
		 * Specific to ReactionInstance types, as they will have a reactionFrame_ on which to report what reactions were removed. It is assumed that
		 * DiffusionReactionInstances and ExchangeReactionInstances must be balanced, as they transport a single metabolite through passive means.
		 */
		HashSet<AbstractReactionInstance> newReactionList = new HashSet<AbstractReactionInstance>();
		System.out.println(Reactions.size());
		for (AbstractReactionInstance reaction : Reactions) {
			if (reaction instanceof ReactionInstance) {
//				if (((ReactionInstance)reaction).reactionFrame_.getLocalID().equalsIgnoreCase("RXN0-5258")) {
//					System.out.println("Here");
//				}
				if (reaction.isReactionGeneric()) {
					newReactionList.add(reaction);
				} else if (reaction.isReactionBalanced()) {
					newReactionList.add(reaction);
				} else {
					System.out.println("Removed unbalanced reaction: " + ((ReactionInstance)reaction).reactionFrame_.getLocalID());
				}
			} else newReactionList.add(reaction);
		}
		Reactions = newReactionList;
		System.out.println(Reactions.size());
	}
	
	private void importJavacycReactions(ArrayList<Reaction> reactions) {
		addReactionsToNetwork(reactionListToReactionInstances(reactions));
	}
	
	public String generateHeatMap() {
		HashMap<String,Integer> heatMap = new HashMap<String,Integer>();
		for (AbstractReactionInstance reaction : Reactions) {
			if (reaction instanceof ReactionInstance) {
				String reactionID = ((ReactionInstance) reaction).reactionFrame_.getLocalID();
				if (heatMap.containsKey(reactionID)) {
					heatMap.put(reactionID, new Integer(10));
				} else {
					heatMap.put(reactionID, new Integer(10));
				}
			} else if (reaction instanceof InstantiatedReactionInstance) {
				String reactionID = ((InstantiatedReactionInstance) reaction).parentReactionFrame_.getLocalID();
				if (heatMap.containsKey(reactionID)) {
					heatMap.put(reactionID, new Integer(-10));
				} else {
					heatMap.put(reactionID, new Integer(-10));
				}
			}
		}
		
		String output = "";
		for (String key : heatMap.keySet()) {
			output += key + "\t" + heatMap.get(key) + "\n";
		}
		return output;
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
					for (String location : locations) {
						reactionInstances.add(new ReactionInstance(reaction, reaction.getLocalID() + "_" + location, reaction.isReversible(), location));
					}
				} else if (locations.size() == 1) reactionInstances.add(new ReactionInstance(reaction, reaction.getLocalID(), reaction.isReversible(), locations.get(0)));
				else reactionInstances.add(new ReactionInstance(reaction, reaction.getLocalID(), reaction.isReversible(), null)); //TODO default compartment
			} catch (PtoolsErrorException e) {
				e.printStackTrace();
			}
		}
		
		return reactionInstances;
	}
	
	private void addReactionsToNetwork(ArrayList<AbstractReactionInstance> reactions) {
		for (AbstractReactionInstance reaction : reactions) {
			if (Reactions.contains(reaction)) {
				for (AbstractReactionInstance aReaction : Reactions) {
					if (aReaction.equals(reaction) && !aReaction.name_.equalsIgnoreCase(reaction.name_)) {
						//TODO Detect and handle duplicates. In particular, do we ever want to merge duplicates? Sometimes they may have different gene associations
						System.err.println("Duplicate Reaction: " + ((ReactionInstance)aReaction).reactionFrame_.getLocalID() + " = " + ((ReactionInstance)reaction).reactionFrame_.getLocalID());
						break;
					}
				}
			} else Reactions.add(reaction);
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
