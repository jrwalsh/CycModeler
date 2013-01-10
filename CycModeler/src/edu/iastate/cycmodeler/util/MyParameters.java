package edu.iastate.cycmodeler.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class MyParameters {
	// Connection Info
	public String Host;
	public int Port;
	public String Organism;
	public String User;
	public String Password;
	
	// Parameters
	public String OutputDirectory;
	public String OutputFileName;
	public String DefaultCompartment;
	public int DefaultSBMLLevel;
	public int DefaultSBMLVersion;
	public HashMap<String, String> CompartmentAbrevs;
	public String SpeciesPrefix;
	public String ReactionPrefix;
	public String BoundaryCompartmentName;
	public String ExchangeReactionSuffix;
	public String ModelName;
	public String ExternalCompartmentName;
	public float DiffusionSize;
	public int DefaultUpperBound;
	public int DefaultLowerBound;
	
	
	public MyParameters() {
		initDefault();
	}
	
	public void initDefault() {
		Host = "localhost";
		Port = 4444;
		Organism = "ECOLI";
		User = "";
		Password = "";
		
		OutputDirectory = "/home/jesse/Desktop/output/";
		OutputFileName = Organism + "_Model";
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
		DiffusionSize = (float) 0;
		DefaultUpperBound = 9999;
		DefaultLowerBound = -9999;
	}
	
	public void initializeFromConfigFile(String fileName) {
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
					case HOST: Host = value; break;
					case PORT: Port = Integer.parseInt(value); break;
					case ORGANISM: Organism = value; break;
					case USER: User = value; break;
					case PASSWORD: Password = value; break;
					case OUTPUTDIRECTORY: OutputDirectory = value; break;
					case OUTPUTFILENAME: OutputFileName = value; break;
					case DEFAULTCOMPARTMENT: DefaultCompartment = value; break;
					case DEFAULTSBMLLEVEL: DefaultSBMLLevel = Integer.parseInt(value); break;
					case DEFAULTSBMLVERSION: DefaultSBMLVersion = Integer.parseInt(value); break;
					case MODELNAME: ModelName = value; break;
					case BOUNDARYCOMPARTMENTNAME: BoundaryCompartmentName = value; break;
					case EXCHANGEREACTIONSUFFIX: ExchangeReactionSuffix = value; break;
					case SPECIESPREFIX: SpeciesPrefix = value; break;
					case REACTIONPREFIX: ReactionPrefix = value; break;
					case COMPARTMENTABREVS: {
						String[] values = value.split(";");
						for (String compartmentAbrevPair : values) {
							String[] pair = compartmentAbrevPair.split(",");
							CompartmentAbrevs.put(pair[0], pair[1]);
						}
					} break;
					case DIFFUSIONSIZE: DiffusionSize = Float.parseFloat(value); break;
					case DEFAULTUPPERBOUND: DefaultUpperBound = Integer.parseInt(value); break;
					case DEFAULTLOWERBOUND: DefaultLowerBound = Integer.parseInt(value); break;
					default: {
						System.err.println("Unknown config command : " + command);
					} break;
				}
			}
			
			// Verify settings
			assert Host != null;
			assert Port > 0;
			assert Organism != null;
			assert User != null;
			assert Password != null;
			
			assert OutputDirectory != null;
			assert OutputFileName != null;
			assert DefaultCompartment != null;
			assert DefaultSBMLLevel != 0;
			assert DefaultSBMLVersion != 0;
			assert ModelName != null;
			assert BoundaryCompartmentName != null;
			assert ExchangeReactionSuffix != null;
			assert SpeciesPrefix != null;
			assert ReactionPrefix != null;
			assert CompartmentAbrevs.size() != 0;
			assert DiffusionSize >= (float) 0;
			assert DefaultUpperBound >= DefaultLowerBound;
			assert DefaultLowerBound <= DefaultUpperBound;
			
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
	
	// Internal Classes
	public enum Setting	{
		HOST,
		PORT,
		ORGANISM,
		USER,
		PASSWORD,
		OUTPUTDIRECTORY,
		OUTPUTFILENAME,
		DEFAULTCOMPARTMENT,
		DEFAULTSBMLLEVEL,
		DEFAULTSBMLVERSION,
		MODELNAME,
		BOUNDARYCOMPARTMENTNAME,
		EXCHANGEREACTIONSUFFIX,
		SPECIESPREFIX,
		REACTIONPREFIX,
		COMPARTMENTABREVS,
		DIFFUSIONSIZE,
		DEFAULTUPPERBOUND,
		DEFAULTLOWERBOUND,
		NOVALUE;

	    public static Setting value(String setting) {
	        try {
	            return valueOf(setting.toUpperCase());
	        } catch (Exception e) {
	            return NOVALUE;
	        }
	    }  
	}
}
