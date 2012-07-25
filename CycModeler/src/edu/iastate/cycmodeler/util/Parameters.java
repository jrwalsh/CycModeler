package edu.iastate.cycmodeler.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import edu.iastate.cycmodeler.logic.CycModeler.Setting;

public class Parameters {
	// Global static variables
	static public String connectionStringLocal =  "jrwalsh.student.iastate.edu";
	static public String connectionStringEcoServer =  "ecoserver.vrac.iastate.edu";
	static public String connectionStringTHTServer =  "tht.vrac.iastate.edu";
	static public String organismStringK12 =  "ECOLI"; //Built-in K12 model
	static public String organismStringCBIRC =  "CBIRC"; //CBiRC E. coli model
	static public int defaultPort =  4444;
	
	
	// Parameters
	protected String OutputDirectory;
	public String DefaultCompartment;
	protected int DefaultSBMLLevel;
	protected int DefaultSBMLVersion;
	public HashMap<String, String> CompartmentAbrevs;
	public String SpeciesPrefix;
	public String ReactionPrefix;
	public String BoundaryCompartmentName;
	public String ExchangeReactionSuffix;
	protected String ModelName;
	protected String ExternalCompartmentName;
	
	public Parameters() {
		initDefault();
	}
	
	public void initDefault() {
		OutputDirectory = "/home/jesse/Desktop/output/";
		DefaultCompartment = "CCO-CYTOSOL";
		DefaultSBMLLevel = 2;
		DefaultSBMLVersion = 1;
		ModelName = "DefaultName";
		BoundaryCompartmentName = "Boundary";
		ExchangeReactionSuffix = "Exchange";
		SpeciesPrefix = "M";
		ReactionPrefix = "R";
		CompartmentAbrevs = new HashMap<String, String>();
		ExternalCompartmentName = "CCO-EXTRACELLULAR";
	}
	
	public void initializeFromConfigFile(String fileName) {
		String outputDirectory = null;
		String defaultCompartment = null;
		int defaultSBMLLevel = 0;
		int defaultSBMLVersion = 0;
		String modelName = null;
		String boundaryCompartmentName = null;
		String exchangeReactionSuffix = null;
		String speciesPrefix = null;
		String reactionPrefix = null;
		HashMap<String, String> compartmentAbrevs = new HashMap<String, String>();
		
		
		File configFile = new File(fileName);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(configFile));
			String text = null;
			
			// Parse settings from file
			while ((text = reader.readLine()) != null) {
				String command = text.substring(0, text.indexOf(" "));
				String value = text.substring(text.indexOf(" ")+1);
				
				switch (Setting.value(command)) {
					case OUTPUTDIRECTORY: outputDirectory = value; break;
					case DEFAULTCOMPARTMENT: defaultCompartment = value; break;
					case DEFAULTSBMLLEVEL: defaultSBMLLevel = Integer.parseInt(value); break;
					case DEFAULTSBMLVERSION: defaultSBMLVersion = Integer.parseInt(value); break;
					case MODELNAME: modelName = value; break;
					case BOUNDARYCOMPARTMENTNAME: boundaryCompartmentName = value; break;
					case EXCHANGEREACTIONSUFFIX: exchangeReactionSuffix = value; break;
					case SPECIESPREFIX: speciesPrefix = value; break;
					case REACTIONPREFIX: reactionPrefix = value; break;
					case COMPARTMENTABREVS: {
						String[] values = value.split(";");
						for (String compartmentAbrevPair : values) {
							String[] pair = compartmentAbrevPair.split(",");
							compartmentAbrevs.put(pair[0], pair[1]);
						}
					} break;
					default: {
						System.err.println("Unknown config command : " + command);
					} break;
				}
			}
			
			// Verify settings
			assert outputDirectory != null;
			assert defaultCompartment != null;
			assert defaultSBMLLevel != 0;
			assert defaultSBMLVersion != 0;
			assert modelName != null;
			assert boundaryCompartmentName != null;
			assert exchangeReactionSuffix != null;
			assert speciesPrefix != null;
			assert reactionPrefix != null;
			assert compartmentAbrevs.size() != 0;
			
			// Set variables
			OutputDirectory = outputDirectory;
			DefaultCompartment = defaultCompartment;
			DefaultSBMLLevel = defaultSBMLLevel;
			DefaultSBMLVersion = defaultSBMLVersion;
			ModelName = modelName;
			BoundaryCompartmentName = boundaryCompartmentName;
			ExchangeReactionSuffix = exchangeReactionSuffix;
			SpeciesPrefix = speciesPrefix;
			ReactionPrefix = reactionPrefix;
			CompartmentAbrevs = compartmentAbrevs;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
