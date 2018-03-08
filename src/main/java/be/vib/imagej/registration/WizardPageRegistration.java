package be.vib.imagej.registration;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class WizardPageRegistration extends WizardPage
{		
	//private DenoiseSummaryPanel denoiseSummaryPanel;
	private JButton startButton;
	private JButton cancelButton;
	private JLabel statusLabel;
	private JProgressBar progressBar;
	//private RangeSelectionPanel rangeSelectionPanel;
	private boolean busyRegistering = false;
	private RegistrationSwingWorker worker;
	
	public WizardPageRegistration(Wizard wizard, String name)
	{
		super(wizard, name);
		
		buildUI();
	}

	private void buildUI()
	{		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		startButton = new JButton("Start Registration");
		cancelButton = new JButton("Cancel Registration");
		
		statusLabel = new JLabel();
		
		progressBar = new JProgressBar();
		
		setButtonsReadyToRegister();

		startButton.addActionListener(e -> {
			register();
		});
		
		cancelButton.addActionListener(e -> {
		    worker.cancel(false);
		});
		
//		denoiseSummaryPanel = new DenoiseSummaryPanel(wizard.getModel());
//		denoiseSummaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, denoiseSummaryPanel.getMaximumSize().height));
//		
//		rangeSelectionPanel = new RangeSelectionPanel(wizard.getModel(), startButton);
//		rangeSelectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rangeSelectionPanel.getPreferredSize().height));
//		rangeSelectionPanel.addEventListener(this);
//		
//		rangeSelectionPanel.setAlignmentX(CENTER_ALIGNMENT);
		startButton.setAlignmentX(CENTER_ALIGNMENT);
		cancelButton.setAlignmentX(CENTER_ALIGNMENT);
		statusLabel.setAlignmentX(CENTER_ALIGNMENT);
		
//		add(denoiseSummaryPanel);
//		add(Box.createRigidArea(new Dimension(0, 10)));
//		add(rangeSelectionPanel);
//		add(Box.createRigidArea(new Dimension(0, 10)));
		add(startButton);
		add(progressBar);
		add(statusLabel);
		add(Box.createRigidArea(new Dimension(0, 20)));
		add(cancelButton);
		add(Box.createVerticalGlue());	}

    // register() is executed on the Java EDT, so it needs to complete ASAP.
	// Off-load calculations to a separate thread and return immediately.
	private void register() 
	{		
		busyRegistering = true;
		wizard.updateButtons();  // disable the Back button while we're busy registering

		startButton.setVisible(false);
		cancelButton.setVisible(true);
		
		//rangeSelectionPanel.setEnabled(false);
		
		statusLabel.setText("Registering...");
		statusLabel.setVisible(true);
		
		progressBar.setMinimum(0);    // progress will be mapped by DenoiseSwingWorker to a value in [0, 100]
		progressBar.setMaximum(100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true); // show percentage progress as text in the progress bar
		progressBar.setVisible(true);
		
		Runnable whenDone = () -> {
			busyRegistering = false;
			cancelButton.setVisible(false);
//			rangeSelectionPanel.setEnabled(true);
			statusLabel.setText(worker.isCancelled() ? "Registration cancelled": "Registration done");
			progressBar.setVisible(false);
			wizard.updateButtons();
		};
			
		WizardModel model = wizard.getModel();
		Rectangle rect = model.referenceImage.getRoi().getBounds();
		worker = new RegistrationSwingWorker(model.getInputFiles(), model.getOutputFolder(), rect, progressBar, whenDone);
		
		// Run the registration on a separate worker thread and return here immediately.
		// Once registration has completed, the worker will automatically update the user interface to indicate this.
		worker.execute();
	}

	@Override
	public void arriveFromPreviousPage()
	{
		// After registration was complete, we may have gone back, chosen another image or image stack, 
		// and returned to the registration panel. So some status messages or buttons may need to be updated.
		
		busyRegistering = false;
		
		//rangeSelectionPanel.aboutToShow();
		
		//denoiseSummaryPanel.updateText();

		setButtonsReadyToRegister();
		
		wizard.pack();
	}	

	@Override
	public boolean canGoToPreviousPage()
	{
		return !busyRegistering;
	}

	private void setButtonsReadyToRegister()
	{
		startButton.setVisible(true);
		cancelButton.setVisible(false);
		progressBar.setVisible(false);
		statusLabel.setVisible(false);
	}
}
