package be.vib.imagej.registration;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public class WizardPageRegistration extends WizardPage
{		
	private MaxShiftPanel maxShiftPanel;
	private SliceThicknessCorrectionPanel sliceThicknessCorrectionPanel;
	private JButton startButton;
	private JButton cancelButton;
	private JLabel statusLabel;
	private JProgressBar progressBar;
	private boolean busyRegistering = false;
	private RegistrationSwingWorker worker;
	
	public WizardPageRegistration(Wizard wizard, String name)
	{
		super(wizard, name);
		buildUI();
	}

	// TODO: after registration, report on max shift in X and Y;
	//       this may help the user gain some intuition on how large these shifts typically are.
	// TODO? while registering, make a plot of the X and Y shifts?
	
	// TODO: log/print the exact registration parameters: max shift x & y, reference patch size, and reference patch location;
	
	// TODO: add summary section again with indication of input and output folders - we don't want to accidentally dump a large amount of files in the wrong place...
	
	private void buildUI()
	{		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		startButton = new JButton("Start Registration");
		cancelButton = new JButton("Cancel");
		
		statusLabel = new JLabel();
		
		progressBar = new JProgressBar();
		
		startButton.addActionListener(e -> {
			register();
		});
		
		cancelButton.addActionListener(e -> {
		    worker.cancel(false);
		});
		
		maxShiftPanel = new MaxShiftPanel();
	
		sliceThicknessCorrectionPanel = new SliceThicknessCorrectionPanel();
	
		startButton.setAlignmentX(CENTER_ALIGNMENT);
		cancelButton.setAlignmentX(CENTER_ALIGNMENT);
		statusLabel.setAlignmentX(CENTER_ALIGNMENT);
				
		JPanel registrationPanel = new JPanel();
		registrationPanel.setLayout(new BoxLayout(registrationPanel, BoxLayout.Y_AXIS));
		registrationPanel.setBorder(BorderFactory.createTitledBorder("Registration"));
		registrationPanel.add(startButton);
		registrationPanel.add(progressBar);
		registrationPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		registrationPanel.add(statusLabel);
		registrationPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		registrationPanel.add(cancelButton);
				
		// Make sure all sub-panels fill the window horizontally.
		// (It there no cleaner way to accomplish this?)
		maxShiftPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxShiftPanel.getMaximumSize().height));
		sliceThicknessCorrectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, sliceThicknessCorrectionPanel.getMaximumSize().height));
		registrationPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, registrationPanel.getMaximumSize().height));

		add(maxShiftPanel);
		add(sliceThicknessCorrectionPanel);
//		add(Box.createRigidArea(new Dimension(0, 10)));
		add(registrationPanel);
		add(Box.createVerticalGlue());

		setReadyToRegister();
	}

    // register() is executed on the Java EDT, so it needs to complete ASAP.
	// Off-load calculations to a separate thread and return immediately.
	private void register() 
	{		
		busyRegistering = true;
		wizard.updateButtons();  // disable the Back button while we're busy registering
		
		startButton.setVisible(false);
		cancelButton.setVisible(true);
				
		maxShiftPanel.setEditable(false);
		sliceThicknessCorrectionPanel.setEditable(false);

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
			statusLabel.setText(worker.isCancelled() ? "Registration cancelled": "Registration done");
			progressBar.setVisible(false);
			wizard.updateButtons();
		};
			
		WizardModel model = wizard.getModel();
		Rectangle rect = model.getReferenceImage().getRoi().getBounds();
		
		RegistrationParameters parameters = new RegistrationParameters(model.getInputFiles(), model.getOutputFolder(), rect, maxShiftPanel.getMaxShiftX(), maxShiftPanel.getMaxShiftY(), sliceThicknessCorrectionPanel.thicknessCorrection(), sliceThicknessCorrectionPanel.thicknessNM());

		worker = new RegistrationSwingWorker(parameters, progressBar, whenDone);
		
		// Run the registration on a separate worker thread and return here immediately.
		// Once registration has completed, the worker will automatically update the user interface to indicate this.
		worker.execute();
	}

	@Override
	public void arriveFromPreviousPage()
	{
		// After registration was complete, we may have gone back, chosen another set of images
		// and returned to the registration panel. So we may need to update the state of the user interface.
		
		busyRegistering = false;
		setReadyToRegister();		
	}	
	
	@Override
	public boolean canGoToPreviousPage()
	{
		return !busyRegistering;
	}

	private void setReadyToRegister()
	{
		maxShiftPanel.setEditable(true);
		sliceThicknessCorrectionPanel.setEditable(true);
		startButton.setVisible(true);
		cancelButton.setVisible(false);
		progressBar.setVisible(false);
		statusLabel.setVisible(false);
	}
}
