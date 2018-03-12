package be.vib.imagej.registration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import ij.ImagePlus;
import ij.io.Opener;

@SuppressWarnings("serial")
public class WizardPageSelectFolders extends WizardPage
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
		// TODO? add some kind of file filter on the input folder; it often contains a couple of non-tiff files that must be ignored
		// TODO? add info/error label to inform the user that e.g. the input directory exists but is empty
		// TODO: make directory fields editable
// TODO: remember last input and output folders - use ImageJ prefs
// TODO: make sure that file selector opens in the directory that was specified previously (when opening closing it multiple times)
		
		inputFolderLabel = new JLabel("Input folder:");
		outputFolderLabel = new JLabel("Output folder:");
		
		inputFolderField = new JTextField();
		inputFolderField.setEditable(false);
		
		outputFolderField = new JTextField();		
		outputFolderField.setEditable(false);
		
		inputFolderButton = new JButton("Select...");
		inputFolderButton.addActionListener(e -> {
			Path folder = showFolderChooser(wizard.getModel().getInputFolder());
			if (folder == null) return;
			inputFolderField.setText(folder.toString());			
			wizard.getModel().setInputFolder(folder);
			wizard.updateButtons();			
		});
		
		outputFolderButton = new JButton("Select...");
		outputFolderButton.addActionListener(e -> {
			Path folder = showFolderChooser(wizard.getModel().getOutputFolder());	
			if (folder == null) return;
			outputFolderField.setText(folder.toString());			
			wizard.getModel().setOutputFolder(folder);
			wizard.updateButtons();			
		});
						
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

	private Path showFolderChooser(Path defaultFolder)
	{
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setCurrentDirectory(defaultFolder.toFile());

		int result = fileChooser.showOpenDialog(this);
		if (result != JFileChooser.APPROVE_OPTION)
			return null;
		
		File file = fileChooser.getSelectedFile();
		return file.toPath();
	}

	@Override
	public void arriveFromPreviousPage()
	{
		setupPage();
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

		// Open that slice as an image so the user can then select the patch for registration
		// in the next wizard page.
		//image = (imageTitle != null) ? ij.WindowManager.getImage(imageTitle) : null;
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(firstSliceName.toString());
		
		wizard.getModel().setReferenceImage(imp);
		// FIXME: Lock the reference image so the user cannot close it while we are busy registering.
				
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
	
	private void setupPage()
	{
		inputFolderField.setText(getPathString(wizard.getModel().getInputFolder()));
		outputFolderField.setText(getPathString(wizard.getModel().getOutputFolder()));		
	}
}
