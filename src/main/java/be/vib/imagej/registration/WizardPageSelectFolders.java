package be.vib.imagej.registration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Colors;

@SuppressWarnings("serial")
public class WizardPageSelectFolders extends WizardPage
{		
	private JLabel inputFolderLabel;
	private JLabel inputFilePatternLabel;
	private JLabel outputFolderLabel;
	private JTextField inputFolderField;
	private JTextField inputFilePatternField;
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
	
	private void handleInputFolderChange(Path folder)
	{
		System.out.println("handleInputFolderChange: " + folder);
		wizard.getModel().setInputFolder(folder);
		inputFilePatternChangeImpl(inputFilePatternField.getText());
	}
	
	private void handleOutputFolderChange(Path folder)
	{
		System.out.println("handleOutputFolderChanges: " + folder);
		wizard.getModel().setOutputFolder(folder);
		updateStatusIndicators();
		wizard.updateButtons();		
	}
	
	private void handleInputFilePatternChange(String pattern)
	{
		System.out.println("handleInputFilePatternChange: " + pattern);
		inputFilePatternChangeImpl(pattern);
	}
	
	private void inputFilePatternChangeImpl(String pattern)
	{
		wizard.getModel().scanInputFolder(inputFilePatternField.getText());
		tryToLoadReferenceImage();
		updateStatusIndicators();
		wizard.updateButtons();			
	}

	private void buildUI()
	{		
		// IMPROVEME: place a green check mark or a red cross next to the folder to indicate if the folder exists or not
		
		inputFolderLabel = new JLabel("Input folder:");
		inputFilePatternLabel = new JLabel("Input file pattern:");
		outputFolderLabel = new JLabel("Output folder:");
		
		infoLabel = new JLabel("");
		
		inputFolderField = new JTextField();
		inputFolderField.getDocument().addDocumentListener(new DocumentListenerAdapter() {    // FIXME: when input folder is edited to some invalid folder (e.g. ending in a space) an exception is thrown :(
			@Override
			public void handleChange(DocumentEvent e)
			{
				System.out.println("handleChange: " + inputFilePatternField.getText());	
				Path folder = Paths.get(inputFolderField.getText());
				handleInputFolderChange(folder);
			}
		});
		
		inputFilePatternField = new JTextField("*.tif");
		inputFilePatternField.getDocument().addDocumentListener(new DocumentListenerAdapter() {
			@Override
			public void handleChange(DocumentEvent e)
			{
				System.out.println("handleChange: " + inputFilePatternField.getText());	
				handleInputFilePatternChange(inputFilePatternField.getText());
			}
		});
		
		outputFolderField = new JTextField();	
		outputFolderField.getDocument().addDocumentListener(new DocumentListenerAdapter() {
			@Override
			public void handleChange(DocumentEvent e)
			{
				System.out.println("handleChange: " + inputFilePatternField.getText());	
				Path folder = Paths.get(outputFolderField.getText());
				handleOutputFolderChange(folder);
			}
		});
		
		inputFolderButton = new JButton("Select...");
		inputFolderButton.addActionListener(e -> {
			Path folder = showFolderChooser(wizard.getModel().getInputFolder());
			if (folder == null) return;
			inputFolderField.setText(folder.toString());  // triggers a handleInputFolderChange
		});
		
		outputFolderButton = new JButton("Select...");
		outputFolderButton.addActionListener(e -> {
			Path folder = showFolderChooser(wizard.getModel().getOutputFolder());	
			if (folder == null) return;
			outputFolderField.setText(folder.toString());  // triggers a handleOutputFolderChange
		});
		
		badInputFolderLabel = new JLabel();
		badOutputFolderLabel = new JLabel();
		
		// Avoid that input field becomes bigger if the status toggles between a cross and a checkmark
		Dimension crossDim = new Dimension(20, 20);
		badInputFolderLabel.setPreferredSize(crossDim);
		badInputFolderLabel.setMinimumSize(crossDim);
		badOutputFolderLabel.setPreferredSize(crossDim);
		badOutputFolderLabel.setMinimumSize(crossDim);
		
		JPanel foldersPanel = new JPanel();
		
		GroupLayout layout = new GroupLayout(foldersPanel);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(
		   layout.createSequentialGroup()
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
			           .addComponent(inputFolderLabel)
			           .addComponent(inputFilePatternLabel)
			           .addComponent(outputFolderLabel))
		      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, true)
			           .addComponent(inputFolderField)
			           .addComponent(inputFilePatternField)
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
				    		   .addComponent(inputFilePatternLabel)
				    		   .addComponent(inputFilePatternField))
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		   .addComponent(outputFolderLabel)
				    		   .addComponent(outputFolderField)
					           .addComponent(outputFolderButton)
					           .addComponent(badOutputFolderLabel))
				      );  	
		
		foldersPanel.setLayout(layout);
		
		foldersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(foldersPanel);
		add(Box.createRigidArea(new Dimension(0, 20)));
		add(infoLabel);
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
		final boolean inputFolderGood = inputFolderGood();
		final boolean outputFolderGood = outputFolderGood();
		final boolean haveReferenceImage = (wizard.getModel().getReferenceImage() != null);
		
		setStatus(badInputFolderLabel, inputFolderGood);
		setStatus(badOutputFolderLabel, outputFolderGood);
		
		if (!inputFolderGood)
		{
			showInfoMessage("The input folder does not exist or cannot be read.");
		}
		else if (!haveReferenceImage)
		{
			showInfoMessage("Could not read a reference image in the input folder. Maybe the input folder is empty or none of the files match the input file pattern?");			
		}
		else if (!outputFolderGood)
		{
			showInfoMessage("The output folder does not exist.");			
		}
		else
		{
			showInfoMessage(null);			
		}
	}
	
	private void setStatus(JLabel label, boolean good)
	{
		final String heavyCheckMark = "\u2714";
		final String heavyBallotX = "\u2718";
		
		final Color green = Color.decode("#1e9906");
		final Color red = Color.RED;
		
		label.setText(good ? heavyCheckMark : heavyBallotX);
		label.setForeground(good ? green : red);	
		label.setFont(new Font("Serif", Font.PLAIN, 16));
	}
	
	private void showInfoMessage(String message)
	{
		if (message == null)
		{
			infoLabel.setVisible(false);
		}
		else
		{
			infoLabel.setVisible(true);
			infoLabel.setText(htmlAttention(message));
		}
	}
	
	private void setupPage()
	{
		WizardModel model = wizard.getModel();
		inputFolderField.setText(getPathString(model.getInputFolder()));
		outputFolderField.setText(getPathString(model.getOutputFolder()));
		model.scanInputFolder(inputFilePatternField.getText());
		tryToLoadReferenceImage();
		updateStatusIndicators();
		wizard.updateButtons();
	}

	// Wraps an ordinary text string in HTML for rendering it as red text. 
	private static String htmlAttention(String s)
	{
		return "<html><font color=red>" + s + "</font></html>";
	}
}
