package be.vib.imagej.registration;

import java.awt.Dimension;
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
//	private JLabel fileFilterLabel;
	private JTextField inputFolderField;
	private JTextField outputFolderField;
	private JButton inputFolderButton;
	private JButton outputFolderButton;
	private JLabel badInputFolderLabel;
	private JLabel badOutputFolderLabel;
	private JLabel infoLabel;   // used for displaying warning or error messages concerning the input/output folders
	
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
		// TODO: make the directory fields editable
		
		inputFolderLabel = new JLabel("Input folder:");
		//fileFilterLabel = new JLabel("File filter:"); // TODO
		outputFolderLabel = new JLabel("Output folder:");
		
		infoLabel = new JLabel("");
		
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
			wizard.getModel().scanInputFolder();
			tryToLoadReferenceImage();
			updateStatusIndicators();
			wizard.updateButtons();			
		});
		
		outputFolderButton = new JButton("Select...");
		outputFolderButton.addActionListener(e -> {
			Path folder = showFolderChooser(wizard.getModel().getOutputFolder());	
			if (folder == null) return;
			outputFolderField.setText(folder.toString());	
			wizard.getModel().setOutputFolder(folder);
			updateStatusIndicators();
			wizard.updateButtons();			
		});
		
		// TODO: red cross icon instead of "X"
		badInputFolderLabel = new JLabel("X");
		badOutputFolderLabel = new JLabel("X");
		
		// TODO: avoid that input field becomes bigger if cross is hidden
		Dimension crossDim = new Dimension(20, 20);
		badInputFolderLabel.setPreferredSize(crossDim);
		badInputFolderLabel.setMinimumSize(crossDim);
		badOutputFolderLabel.setPreferredSize(crossDim);
		badOutputFolderLabel.setMinimumSize(crossDim);
		
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
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
			           .addComponent(badInputFolderLabel)
			           .addComponent(badOutputFolderLabel))
		      );
		
		layout.setVerticalGroup(
				   layout.createSequentialGroup()
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		   .addComponent(inputFolderLabel)
				    		   .addComponent(inputFolderField)
					           .addComponent(inputFolderButton)
					           .addComponent(badInputFolderLabel))
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		   .addComponent(outputFolderLabel)
				    		   .addComponent(outputFolderField)
					           .addComponent(outputFolderButton)
					           .addComponent(badOutputFolderLabel))
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
		ImagePlus imp = wizard.getModel().getReferenceImage();
		assert(imp != null);  // we check for this in canGoToNextPage()
		
		imp.show(); // FIXME: can we automatically close this window if the users changes the input folder and we re-read a new reference image?
		            // FIXME: the user can still close the window, and return to the WizardPageSpecifyPatch. The page then still has a reference to the image, but the window is not shown anymore, so the ROI cannot be modified anymore by the user.
	}
	
	private void tryToLoadReferenceImage()
	{
		WizardModel model = wizard.getModel();
		model.setReferenceImage(null);

		List<Path> files = model.getInputFiles();
		if (files == null)
			return;
		
		if (files.isEmpty())
			return;
		
		Collections.sort(files);
		Path firstSliceName = files.get(0);
		System.out.println("First slice assumed to be " + firstSliceName);

		// Open that slice as an image so the user can then select the patch for registration
		// in the next wizard page.
		//image = (imageTitle != null) ? ij.WindowManager.getImage(imageTitle) : null;
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(firstSliceName.toString());  // imp will be null if this fails; fine we'll remember it in our model
		model.setReferenceImage(imp);
		
		// FIXME: we need locking/unlock of the reference image probably
	}
	
	@Override
	public boolean canGoToNextPage()
	{		
		boolean haveReferenceImage = (wizard.getModel().getReferenceImage() != null);  // this implies that the folder with input files exists, and contains at least one image that we could open
    	return outputFolderGood() && haveReferenceImage;
	}
	
	private boolean inputFolderGood()
	{
    	WizardModel model = wizard.getModel(); 	  
    	Path input = model.getInputFolder();
    	return (input != null) && Files.exists(input);
	}
	
	private boolean outputFolderGood()
	{
    	WizardModel model = wizard.getModel(); 	  
		Path output = model.getOutputFolder();
		assert(output != null);
		return (output != null) && Files.exists(output);
	}
	
	private void updateStatusIndicators()
	{
		badInputFolderLabel.setVisible(!inputFolderGood());
		badOutputFolderLabel.setVisible(!outputFolderGood());
		System.out.println("Have reference image? " + (wizard.getModel().getReferenceImage() != null)); // TODO: update error/info label too: input folder does not exist, or it exists but has no input files.
	}
	
	private void setupPage()
	{
		WizardModel model = wizard.getModel();
		inputFolderField.setText(getPathString(model.getInputFolder()));
		outputFolderField.setText(getPathString(model.getOutputFolder()));
		model.scanInputFolder();
		tryToLoadReferenceImage();
		updateStatusIndicators();
		wizard.updateButtons();
	}
}
