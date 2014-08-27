package edu.iastate.cycmodeler.view;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;
import java.awt.Font;
import javax.swing.JButton;

public class OptionPanel extends JPanel {
	DefaultController controller;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JTextField textField_4;
	private JTextField textField_5;
	private JTextField textField_6;
	private JTextField textField_7;
	private JTextField textField_8;
	private JTextField textField_9;
	private JTextField textField_10;
	private JTextField textField_11;

	public OptionPanel(DefaultController controller) {
		setPreferredSize(new Dimension(800, 400));
		this.controller = controller;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{112, 0, 94, 28, 208, 191, 0};
		gridBagLayout.rowHeights = new int[]{0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel("Options Panel");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.gridwidth = 3;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 2;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		JLabel lblOrganismDatabase = new JLabel("Organism Database");
		GridBagConstraints gbc_lblOrganismDatabase = new GridBagConstraints();
		gbc_lblOrganismDatabase.anchor = GridBagConstraints.WEST;
		gbc_lblOrganismDatabase.insets = new Insets(0, 0, 5, 5);
		gbc_lblOrganismDatabase.gridx = 1;
		gbc_lblOrganismDatabase.gridy = 1;
		add(lblOrganismDatabase, gbc_lblOrganismDatabase);
		
		JComboBox comboBox = new JComboBox();
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_comboBox.insets = new Insets(0, 0, 5, 5);
		gbc_comboBox.gridx = 2;
		gbc_comboBox.gridy = 1;
		add(comboBox, gbc_comboBox);
		
		JLabel lblDefaultCompartment = new JLabel("Default Compartment");
		GridBagConstraints gbc_lblDefaultCompartment = new GridBagConstraints();
		gbc_lblDefaultCompartment.insets = new Insets(0, 0, 5, 5);
		gbc_lblDefaultCompartment.anchor = GridBagConstraints.EAST;
		gbc_lblDefaultCompartment.gridx = 1;
		gbc_lblDefaultCompartment.gridy = 2;
		add(lblDefaultCompartment, gbc_lblDefaultCompartment);
		
		textField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 2;
		gbc_textField.gridy = 2;
		add(textField, gbc_textField);
		textField.setColumns(10);
		
		JLabel lblSbmlLevel = new JLabel("SBML Level");
		GridBagConstraints gbc_lblSbmlLevel = new GridBagConstraints();
		gbc_lblSbmlLevel.anchor = GridBagConstraints.EAST;
		gbc_lblSbmlLevel.insets = new Insets(0, 0, 5, 5);
		gbc_lblSbmlLevel.gridx = 1;
		gbc_lblSbmlLevel.gridy = 3;
		add(lblSbmlLevel, gbc_lblSbmlLevel);
		
		textField_1 = new JTextField();
		textField_1.setColumns(10);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.insets = new Insets(0, 0, 5, 5);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 2;
		gbc_textField_1.gridy = 3;
		add(textField_1, gbc_textField_1);
		
		JLabel lblSbmlVersion = new JLabel("SBML Version");
		GridBagConstraints gbc_lblSbmlVersion = new GridBagConstraints();
		gbc_lblSbmlVersion.anchor = GridBagConstraints.EAST;
		gbc_lblSbmlVersion.insets = new Insets(0, 0, 5, 5);
		gbc_lblSbmlVersion.gridx = 3;
		gbc_lblSbmlVersion.gridy = 3;
		add(lblSbmlVersion, gbc_lblSbmlVersion);
		
		textField_2 = new JTextField();
		textField_2.setColumns(10);
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.insets = new Insets(0, 0, 5, 5);
		gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_2.gridx = 4;
		gbc_textField_2.gridy = 3;
		add(textField_2, gbc_textField_2);
		
		JLabel lblModelName = new JLabel("Model Name");
		GridBagConstraints gbc_lblModelName = new GridBagConstraints();
		gbc_lblModelName.anchor = GridBagConstraints.EAST;
		gbc_lblModelName.insets = new Insets(0, 0, 5, 5);
		gbc_lblModelName.gridx = 1;
		gbc_lblModelName.gridy = 4;
		add(lblModelName, gbc_lblModelName);
		
		textField_3 = new JTextField();
		GridBagConstraints gbc_textField_3 = new GridBagConstraints();
		gbc_textField_3.insets = new Insets(0, 0, 5, 5);
		gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_3.gridx = 2;
		gbc_textField_3.gridy = 4;
		add(textField_3, gbc_textField_3);
		textField_3.setColumns(10);
		
		JLabel lblBoundaryCompartment = new JLabel("Boundary Compartment");
		GridBagConstraints gbc_lblBoundaryCompartment = new GridBagConstraints();
		gbc_lblBoundaryCompartment.anchor = GridBagConstraints.EAST;
		gbc_lblBoundaryCompartment.insets = new Insets(0, 0, 5, 5);
		gbc_lblBoundaryCompartment.gridx = 1;
		gbc_lblBoundaryCompartment.gridy = 5;
		add(lblBoundaryCompartment, gbc_lblBoundaryCompartment);
		
		textField_4 = new JTextField();
		GridBagConstraints gbc_textField_4 = new GridBagConstraints();
		gbc_textField_4.insets = new Insets(0, 0, 5, 5);
		gbc_textField_4.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_4.gridx = 2;
		gbc_textField_4.gridy = 5;
		add(textField_4, gbc_textField_4);
		textField_4.setColumns(10);
		
		JLabel lblExchangeReactionSuffix = new JLabel("Exchange Reaction Suffix");
		GridBagConstraints gbc_lblExchangeReactionSuffix = new GridBagConstraints();
		gbc_lblExchangeReactionSuffix.anchor = GridBagConstraints.EAST;
		gbc_lblExchangeReactionSuffix.insets = new Insets(0, 0, 5, 5);
		gbc_lblExchangeReactionSuffix.gridx = 1;
		gbc_lblExchangeReactionSuffix.gridy = 6;
		add(lblExchangeReactionSuffix, gbc_lblExchangeReactionSuffix);
		
		textField_5 = new JTextField();
		GridBagConstraints gbc_textField_5 = new GridBagConstraints();
		gbc_textField_5.insets = new Insets(0, 0, 5, 5);
		gbc_textField_5.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_5.gridx = 2;
		gbc_textField_5.gridy = 6;
		add(textField_5, gbc_textField_5);
		textField_5.setColumns(10);
		
		JLabel lblSpeciesPrefix = new JLabel("Species Prefix");
		GridBagConstraints gbc_lblSpeciesPrefix = new GridBagConstraints();
		gbc_lblSpeciesPrefix.anchor = GridBagConstraints.EAST;
		gbc_lblSpeciesPrefix.insets = new Insets(0, 0, 5, 5);
		gbc_lblSpeciesPrefix.gridx = 1;
		gbc_lblSpeciesPrefix.gridy = 7;
		add(lblSpeciesPrefix, gbc_lblSpeciesPrefix);
		
		textField_6 = new JTextField();
		GridBagConstraints gbc_textField_6 = new GridBagConstraints();
		gbc_textField_6.insets = new Insets(0, 0, 5, 5);
		gbc_textField_6.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_6.gridx = 2;
		gbc_textField_6.gridy = 7;
		add(textField_6, gbc_textField_6);
		textField_6.setColumns(10);
		
		JLabel lblReactionPrefix = new JLabel("Reaction Prefix");
		GridBagConstraints gbc_lblReactionPrefix = new GridBagConstraints();
		gbc_lblReactionPrefix.anchor = GridBagConstraints.EAST;
		gbc_lblReactionPrefix.insets = new Insets(0, 0, 5, 5);
		gbc_lblReactionPrefix.gridx = 1;
		gbc_lblReactionPrefix.gridy = 8;
		add(lblReactionPrefix, gbc_lblReactionPrefix);
		
		textField_7 = new JTextField();
		GridBagConstraints gbc_textField_7 = new GridBagConstraints();
		gbc_textField_7.insets = new Insets(0, 0, 5, 5);
		gbc_textField_7.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_7.gridx = 2;
		gbc_textField_7.gridy = 8;
		add(textField_7, gbc_textField_7);
		textField_7.setColumns(10);
		
		JLabel lblCompartmentAbbrevs = new JLabel("Compartment Abbrevs");
		GridBagConstraints gbc_lblCompartmentAbbrevs = new GridBagConstraints();
		gbc_lblCompartmentAbbrevs.anchor = GridBagConstraints.EAST;
		gbc_lblCompartmentAbbrevs.insets = new Insets(0, 0, 5, 5);
		gbc_lblCompartmentAbbrevs.gridx = 1;
		gbc_lblCompartmentAbbrevs.gridy = 9;
		add(lblCompartmentAbbrevs, gbc_lblCompartmentAbbrevs);
		
		textField_8 = new JTextField();
		GridBagConstraints gbc_textField_8 = new GridBagConstraints();
		gbc_textField_8.insets = new Insets(0, 0, 5, 5);
		gbc_textField_8.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_8.gridx = 2;
		gbc_textField_8.gridy = 9;
		add(textField_8, gbc_textField_8);
		textField_8.setColumns(10);
		
		JLabel lblDiffusionSize = new JLabel("Diffusion Size");
		GridBagConstraints gbc_lblDiffusionSize = new GridBagConstraints();
		gbc_lblDiffusionSize.anchor = GridBagConstraints.EAST;
		gbc_lblDiffusionSize.insets = new Insets(0, 0, 5, 5);
		gbc_lblDiffusionSize.gridx = 1;
		gbc_lblDiffusionSize.gridy = 10;
		add(lblDiffusionSize, gbc_lblDiffusionSize);
		
		textField_9 = new JTextField();
		GridBagConstraints gbc_textField_9 = new GridBagConstraints();
		gbc_textField_9.insets = new Insets(0, 0, 5, 5);
		gbc_textField_9.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_9.gridx = 2;
		gbc_textField_9.gridy = 10;
		add(textField_9, gbc_textField_9);
		textField_9.setColumns(10);
		
		JLabel lblDefaultUpperBound = new JLabel("Default Upper Bound");
		GridBagConstraints gbc_lblDefaultUpperBound = new GridBagConstraints();
		gbc_lblDefaultUpperBound.anchor = GridBagConstraints.EAST;
		gbc_lblDefaultUpperBound.insets = new Insets(0, 0, 5, 5);
		gbc_lblDefaultUpperBound.gridx = 1;
		gbc_lblDefaultUpperBound.gridy = 11;
		add(lblDefaultUpperBound, gbc_lblDefaultUpperBound);
		
		textField_10 = new JTextField();
		GridBagConstraints gbc_textField_10 = new GridBagConstraints();
		gbc_textField_10.insets = new Insets(0, 0, 5, 5);
		gbc_textField_10.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_10.gridx = 2;
		gbc_textField_10.gridy = 11;
		add(textField_10, gbc_textField_10);
		textField_10.setColumns(10);
		
		JLabel lblDefaultLowerBound = new JLabel("Default Lower Bound");
		GridBagConstraints gbc_lblDefaultLowerBound = new GridBagConstraints();
		gbc_lblDefaultLowerBound.anchor = GridBagConstraints.EAST;
		gbc_lblDefaultLowerBound.insets = new Insets(0, 0, 5, 5);
		gbc_lblDefaultLowerBound.gridx = 1;
		gbc_lblDefaultLowerBound.gridy = 12;
		add(lblDefaultLowerBound, gbc_lblDefaultLowerBound);
		
		textField_11 = new JTextField();
		GridBagConstraints gbc_textField_11 = new GridBagConstraints();
		gbc_textField_11.insets = new Insets(0, 0, 5, 5);
		gbc_textField_11.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_11.gridx = 2;
		gbc_textField_11.gridy = 12;
		add(textField_11, gbc_textField_11);
		textField_11.setColumns(10);
		
		JButton btnBack = new JButton("Back");
		GridBagConstraints gbc_btnBack = new GridBagConstraints();
		gbc_btnBack.insets = new Insets(0, 0, 0, 5);
		gbc_btnBack.gridx = 0;
		gbc_btnBack.gridy = 14;
		add(btnBack, gbc_btnBack);
		
		JButton btnNext = new JButton("Next");
		GridBagConstraints gbc_btnNext = new GridBagConstraints();
		gbc_btnNext.gridx = 5;
		gbc_btnNext.gridy = 14;
		add(btnNext, gbc_btnNext);
		initComponents();
        localInitialization();
    }

    public void localInitialization() {
    }
    
    private void initComponents() {
	}
    
}
