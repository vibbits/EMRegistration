package be.vib.imagej.registration;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import ij.ImagePlus;
import ij.io.Opener;

public class WizardPageSelectFolders extends WizardPage implements ActionListener
{		
	private JLabel inputFolderLabel;
	private JLabel outputFolderLabel;
	private JTextField inputFolderField;
	private JTextField outputFolderField;
	private JButton inputFolderButton;
	private JButton outputFolderButton;
	
	private final JFileChooser fileChooser = new JFileChooser();
			
	public WizardPageSelectFolders(Wizard wizard, String name)
	{
		super(wizard, name);
		
		// BEGIN FIXME
		wizard.getModel().setInputFolder(Paths.get("E:\\Datasets\\EM\\HPF\\2018_02_05_DEV_INWchem_cellMK\\Raw_Data_Cropped"));
		wizard.getModel().setOutputFolder(Paths.get("E:\\Datasets\\EM\\HPF\\2018_02_05_DEV_INWchem_cellMK\\Out"));
		wizard.getModel().setInputFiles(getFiles(wizard.getModel().getInputFolder()));
		// END FIXME

		buildUI();
	}
	
	private String getPathString(Path path)
	{
		if (path == null)
			return "";
		else
			return path.toString();
	}

	private void buildUI()
	{		
		// IMPROVEME: place a green check mark or a red cross next to the folder to indicate if the folder exists or not
		// TODO: add output filename prefix input field
		// TODO: add info/error label to inform the user that e.g. the input directory exists but is empty
		// TODO: make directory fields editable
		// TODO: remember last input and output folders
		// TODO: make sure that file selector opens in the directory that was specified previously (when opening closing it multiple times)
		
		inputFolderLabel = new JLabel("Input folder:");
		outputFolderLabel = new JLabel("Output folder:");
		
		inputFolderField = new JTextField(getPathString(wizard.getModel().getInputFolder()));
		outputFolderField = new JTextField(getPathString(wizard.getModel().getOutputFolder()));
		
		inputFolderField.setEditable(false);
		outputFolderField.setEditable(false);
		
		inputFolderButton = new JButton("Select...");
		inputFolderButton.addActionListener(this);
		
		outputFolderButton = new JButton("Select...");
		outputFolderButton.addActionListener(this);
						
		GroupLayout layout = new GroupLayout(this);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(
		   layout.createSequentialGroup()
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
			           .addComponent(inputFolderLabel)
			           .addComponent(outputFolderLabel))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
			           .addComponent(inputFolderField)
			           .addComponent(outputFolderField))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
			           .addComponent(inputFolderButton)
			           .addComponent(outputFolderButton))
		);
		
		layout.setVerticalGroup(
				   layout.createSequentialGroup()
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		   .addComponent(inputFolderLabel)
				    		   .addComponent(inputFolderField)
					           .addComponent(inputFolderButton))
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		   .addComponent(outputFolderLabel)
				    		   .addComponent(outputFolderField)
					           .addComponent(outputFolderButton))
				      );  	
		
		setLayout(layout);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() != inputFolderButton && e.getSource() != outputFolderButton)
			return;
		
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int result = fileChooser.showOpenDialog(this);
		if (result != JFileChooser.APPROVE_OPTION)
			return;
		
		File file = fileChooser.getSelectedFile();
		Path path = file.toPath();
		
    	WizardModel model = wizard.getModel(); 	    	
		if (e.getSource() == inputFolderButton)
		{
			inputFolderField.setText(path.toString());
			
			model.setInputFolder(path);
			model.setInputFiles(getFiles(path));			
		}
		else
		{
			outputFolderField.setText(path.toString());
			model.setOutputFolder(path);
		}
		
		// Check if we can move to the next page
		// in the wizard and enable buttons accordingly.
		wizard.updateButtons();
	}

	private List<Path> getFiles(Path folder)
	{		
		List<Path> paths = new ArrayList<Path>();
		
		try
		{
			paths = Files.walk(folder, 1)
			        .filter(Files::isRegularFile)
			        .collect(Collectors.toList());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		paths.forEach(System.out::println);

		return paths;
	}
	
	@Override
	public void goingToNextPage() 
	{
		// Figure out the filename of the assumed first slice in the stack.
		// We pick the first file in lexicographical order in the list of files in the inputFolder.
		
		// IMPROVEME: it is possible that out snapshot of files in getInputFiles() is not up to date anymore:
		//            files may have been deleted etc. Handle this more gracefully...
		
		List<Path> files = wizard.getModel().getInputFiles();
		Collections.sort(files);
		Path firstSliceName = files.get(0);
		System.out.println("First slice assumed to be " + firstSliceName);

		// Open that slice as an image so the user can then select the template for registration
		// in the next wizard page.
		//image = (imageTitle != null) ? ij.WindowManager.getImage(imageTitle) : null;
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(firstSliceName.toString());
		
		wizard.getModel().referenceImage = imp;
		// FIXME: lock/unlock image
		
		imp.show();
		
		// FIXME: Currently if we move back and forth between this page and the next, we repeatedly open the same image
		//        and display multiple windows with the same content and the same title... Avoid this.
	}
	
	@Override
	public boolean canGoToNextPage()
	{		
    	WizardModel model = wizard.getModel(); 	  
    	Path input = model.getInputFolder();
        Path output = model.getOutputFolder();
		return input != null && output != null && Files.exists(input) && Files.exists(output) && !model.getInputFiles().isEmpty();
	}
}
