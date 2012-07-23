package edu.iastate.cycmodeler.util;

public class Report {
	private int totalInitialReactionsCount;
	private int filteredReactions;
	private int genericReactionsFound;
	private int genericReactionsInstantiated;
	private int instantiatedReactions;
	private int boundaryMetabolitesFound;
	private int boundaryReactionsAdded;
	private int diffusionMetabolitesFound;
	private int diffusionReactionsAdded;
	private int newReactionsFromReactionsSplitByLocation;
	private int transportReactions;
	private int totalReactions;
	
	public Report() {
		initDefault();
	}
	
	public void initDefault() {
		totalInitialReactionsCount = 0;
		filteredReactions = 0;
		genericReactionsFound = 0;
		genericReactionsInstantiated = 0;
		instantiatedReactions = 0;
		boundaryMetabolitesFound = 0;
		boundaryReactionsAdded = 0;
		diffusionMetabolitesFound = 0;
		diffusionReactionsAdded = 0;
		newReactionsFromReactionsSplitByLocation = 0;
		transportReactions = 0;
		totalReactions = 0;
	}
	
	public void setTotalInitialReactionsCount(int totalStartingReactionsCount) {
		this.totalInitialReactionsCount = totalStartingReactionsCount;
	}
	
	public void setFilteredReactions(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setGenericReactionsFound(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setGenericReactionsInstantiated(int i) {
		// TODO Auto-generated method stub
		
	}

	public void setInstantiatedReactions(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setBoundaryMetabolitesFound(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setBoundaryReactionsAdded(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setDiffusionMetabolitesFound(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setDiffusionReactionsAdded(int size) {
		// TODO Auto-generated method stub
		
	}

	public void setTransportReactions(int countTransportReactions) {
		// TODO Auto-generated method stub
		
	}

	public void setTotalReactions(int size) {
		// TODO Auto-generated method stub
		
	}
	
	public String report() {
		String reportString = "Writing statistics ...";
		reportString += "All reactions : " + totalInitialReactionsCount + "\n";
		reportString += "Removed reactions due to filtering : " + filteredReactions + "\n";
		reportString += "Generic reactions found : " + genericReactionsFound + "\n";
		reportString += "Generic reactions instantiated : " + genericReactionsInstantiated + "\n";
		reportString += "New reactions from generic reaction instantiations : " + instantiatedReactions + "\n";
		reportString += "Diffusion metabolites found : " + diffusionMetabolitesFound + "\n";
		reportString += "Diffusion reactions added : " + diffusionReactionsAdded + "\n";
		reportString += "Boundary metabolites found : " + boundaryMetabolitesFound + "\n";
		reportString += "Exchange reactions added : " + boundaryReactionsAdded + "\n";
		reportString += "Total transport reactions in network (excluding exchange and diffusion): " + transportReactions + "\n";
		reportString += "Total reactions in network: " + totalReactions + "\n";
		return reportString;
//		System.out.println("Writing statistics ...");
//		System.out.println("All reactions : " + getTotalStartingReactions_);
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
}
