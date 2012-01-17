package cycModeler;

import java.util.ArrayList;

import edu.iastate.javacyco.Frame;
import edu.iastate.javacyco.JavacycConnection;
import edu.iastate.javacyco.PtoolsErrorException;

public class ReactionNetwork {
	private JavacycConnection conn = null;
	ArrayList<ReactionInstance> reactions;

	// Network modification statistics
	private boolean debug = true;
	private int totalStartingReactions = 0;
	private int filteredReactions = 0;
	private int genericReactionsFound = 0;
	private int genericReactionsInstantiated = 0;
	private int instantiatedReactions = 0;
	private int boundaryMetabolitesFound = 0;
	private int boundaryReactionsAdded = 0;
	
	public ReactionNetwork (JavacycConnection connection, ArrayList<ReactionInstance> reactions) {
		conn = connection;
		this.reactions = reactions;
		
		if (debug) totalStartingReactions += reactions.size();
	}
	
	/**
	 * Creates exchange reactions for each metabolite that, for any reaction in reactions list, is also in compartment at least once.
	 * 
	 * @param compartment Compartment in which exchange reactions to metabolites will be created
	 * @param reactions List of all reaction in model, which provides metabolite and compartment information
	 * @return Exchange reactions
	 */
	public ArrayList<ReactionInstance> addBoundaryReactionsByCompartment(String compartment) {
		ArrayList<Frame> exchangeMetabolites = new ArrayList<Frame>();
		ArrayList<String> exchangeMetaboliteIDs = new ArrayList<String>();
		if (reactions == null) {
			//TODO
		}
		
		// For each reaction, check for reactants or products which are consumed or produced in compartment
		for (ReactionInstance reaction : reactions) {
			ArrayList<MetaboliteInstance> allReactants = reaction.reactants;
			ArrayList<MetaboliteInstance> allProducts = reaction.products;
			
			for (MetaboliteInstance reactant : allReactants) {
				if (reactant.compartment.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(reactant.metabolite.getLocalID())) {
					exchangeMetabolites.add(reactant.metabolite);
					exchangeMetaboliteIDs.add(reactant.metabolite.getLocalID());
				}
			}
			for (MetaboliteInstance product : allProducts) {
				if (product.compartment.equalsIgnoreCase(compartment) && !exchangeMetaboliteIDs.contains(product.metabolite.getLocalID())) {
					exchangeMetabolites.add(product.metabolite);
					exchangeMetaboliteIDs.add(product.metabolite.getLocalID());
				}
			}
		}
		
		// Generate exchange reactions
		ArrayList<ReactionInstance> exchangeReactions = new ArrayList<ReactionInstance>();
		for (Frame metabolite : exchangeMetabolites) {
			ArrayList<MetaboliteInstance> reactants = new ArrayList<MetaboliteInstance>();
			reactants.add(new MetaboliteInstance(metabolite, compartment, 1));
			ArrayList<MetaboliteInstance> products = new ArrayList<MetaboliteInstance>();
			products.add(new MetaboliteInstance(metabolite, CycModeler.BoundaryCompartmentName, 1));
			exchangeReactions.add(new ReactionInstance(null, null, metabolite.getLocalID() + "_" + CycModeler.ExchangeReactionSuffix, true, reactants, products));
		}
		
		reactions.addAll(exchangeReactions);
		if (debug) {
			boundaryMetabolitesFound += exchangeMetabolites.size();
			boundaryReactionsAdded += exchangeReactions.size();
		}
		
		return exchangeReactions;
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
	 * TODO
	 */
	public void getSmallMetabolites() {
		ArrayList<Frame> compounds;
		try {
			int count = 0;
			compounds = conn.getAllGFPInstances("|Compounds|");
			for (Frame compound : compounds) {
				try {
					String weight = compound.getSlotValue("MOLECULAR-WEIGHT");
					if (Float.parseFloat(weight) <= 610.00) {
						System.out.println(compound.getCommonName() + " : " + compound.getSlotValue("MOLECULAR-WEIGHT"));
						count++;
					}
				} catch (Exception e) {
					System.err.println(compound.getCommonName() + " : " + compound.getSlotValue("MOLECULAR-WEIGHT"));
				}
			}
			System.out.println(count + "/" + compounds.size());
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove all Reactions from the ArrayList reactions which are either an instance of any of the EcoCyc classes in classToFilter, or
	 * are explicitly named with their EcoCyc Frame ID in the reactionsToFilter list. 
	 * @param reactions List of Reactions to which the filter will be applied
	 * @param classToFilter EcoCyc Frame ID of a class frame, instances of which should be removed from reactions
	 * @param reactionsToFilter EcoCyc Frame ID of a reaction frame which should be removed from reactions
	 * @return FilterResults containing the filtered reaction list and a list of reactions actually removed
	 */
	public FilterResults filterReactions(ArrayList<String> classToFilter, ArrayList<String> reactionsToFilter) {
		ArrayList<String> filter = new ArrayList<String>();
		ArrayList<ReactionInstance> removedList = new ArrayList<ReactionInstance>();
		ArrayList<ReactionInstance> keepList = new ArrayList<ReactionInstance>();
		
		try {
			if (classToFilter != null) {
				for (String reactionClass : classToFilter) {
					for (Object reaction : conn.getClassAllInstances(reactionClass)) filter.add(reaction.toString());
				}
			}
			
			if (reactionsToFilter != null) {
				for (String reaction : reactionsToFilter) filter.add(reaction);
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		
		for (ReactionInstance reaction : reactions) {
			if (filter.contains(reaction.thisReactionFrame.getLocalID())) removedList.add(reaction);
			else keepList.add(reaction);
		}
		
		reactions = keepList;
		if (debug) filteredReactions += keepList.size();
		
		return new FilterResults(keepList, removedList);
	}
	
	/**
	 * Will take in a list of reactions, find any generic reactions (according to EcoCyc), and will attempt to return instances of the
	 * generic reactions from data in EcoCyc.
	 * 
	 * Note: Metabolite class instance information is not complete, resulting in reactions that should exist but aren't included here.
	 * Also, reactions have been found that should occur but do not pass the elemental balancing step due to missing proton/water
	 * molecules.
	 * 
	 * @param reactions List of reactions to instantiate
	 * @return Results of the attempt to instantiate generic reactions
	 */
	public InstantiationResults generateSpecificReactionsFromGenericReactions() {
		InstantiationResults instantiationResults = new InstantiationResults(new ArrayList<ReactionInstance>(), new ArrayList<ReactionInstance>(), new ArrayList<ReactionInstance>(), new ArrayList<ReactionInstance>());
		
		for (ReactionInstance reaction : reactions) {
			if (reaction.isGeneralizedReaction(conn)) {
				instantiationResults.genericReactionsFound.add(reaction);
				ArrayList<ReactionInstance> instantiatedReactions = reaction.generateInstantiatedReactions();
				if (instantiatedReactions != null && instantiatedReactions.size() > 0) {
					instantiationResults.instantiatedReactions.addAll(instantiatedReactions);
				} else {
					instantiationResults.genericReactionsFailedToInstantiate.add(reaction);
				}
			} else {
				instantiationResults.nonGenericReaction.add(reaction);
			}
		}
		
		reactions = instantiationResults.nonGenericReaction;
		reactions.addAll(instantiationResults.instantiatedReactions);
		if (debug) {
			genericReactionsFound += instantiationResults.genericReactionsFound.size();
			genericReactionsInstantiated += instantiationResults.genericReactionsFound.size() - instantiationResults.genericReactionsFailedToInstantiate.size();
			instantiatedReactions += instantiationResults.instantiatedReactions.size();
		}
		
		return instantiationResults;
	}
	
	public void printNetworkStatistics() {
		System.out.println("Writing statistics ...");
		System.out.println("All reactions : " + totalStartingReactions);
		System.out.println("Removed reactions due to filtering : " + filteredReactions);
		System.out.println("Generic reactions found : " + genericReactionsFound);
		System.out.println("Generic reactions instantiated : " + genericReactionsInstantiated);
		System.out.println("New reactions from generic reaction instantiations : " + instantiatedReactions);
		System.out.println("Boundary metabolites found : " + boundaryMetabolitesFound);
		System.out.println("Exchange reactions added : " + boundaryReactionsAdded);
		System.out.println("Total transport reactions in network (excluding exchange and diffusion): " + countTransportReactions());
		System.out.println("Total reactions in network: " + reactions.size());
	}
	
	@SuppressWarnings("unchecked")
	public int countTransportReactions() {
		ArrayList<String> list;
		int transportReactionCount = 0;
		try {
			list = (ArrayList<String>)conn.getClassAllInstances("|Transport-Reactions|");
			for (ReactionInstance reaction : reactions) {
				if (reaction.thisReactionFrame != null) {
					if (list.contains(reaction.thisReactionFrame.getLocalID())) transportReactionCount++;
				}
				else if (reaction.parentReaction != null) {
					if (list.contains(reaction.parentReaction.getLocalID())) transportReactionCount++;
				}
			}
		} catch (PtoolsErrorException e) {
			e.printStackTrace();
		}
		return transportReactionCount;
	}
	
	
	// Internal Classes
	/**
 	 * Internal class which holds results of an attempt to instantiate generic reactions in a list of reactions.
 	 * 
 	 * @author Jesse
 	 */
 	private class InstantiationResults {
 		public ArrayList<ReactionInstance> instantiatedReactions;
 		public ArrayList<ReactionInstance> genericReactionsFound;
 		public ArrayList<ReactionInstance> genericReactionsFailedToInstantiate;
 		public ArrayList<ReactionInstance> nonGenericReaction;
		
		public InstantiationResults(ArrayList<ReactionInstance> instantiatedReactions, ArrayList<ReactionInstance> genericReactionsFound, ArrayList<ReactionInstance> genericReactionsFailedToInstantiate, ArrayList<ReactionInstance> nonGenericReaction) {
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
		public ArrayList<ReactionInstance> keepList;
		public ArrayList<ReactionInstance> removedList;
		
		public FilterResults(ArrayList<ReactionInstance> keepList, ArrayList<ReactionInstance> removedList) {
			this.keepList = keepList;
			this.removedList = removedList;
		}
	}
}
