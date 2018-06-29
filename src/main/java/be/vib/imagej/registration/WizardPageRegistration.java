package be.vib.imagej.registration;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class WizardPageRegistration extends WizardPage
{		
	private MaxShiftPanel maxShiftPanel;
	private SliceThicknessCorrectionPanel sliceThicknessCorrectionPanel;
	private AutoCropPanel autoCropPanel;
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
		
		autoCropPanel = new AutoCropPanel(wizard.getModel());
	
		startButton.setAlignmentX(CENTER_ALIGNMENT);
		cancelButton.setAlignmentX(CENTER_ALIGNMENT);
		statusLabel.setAlignmentX(CENTER_ALIGNMENT);
				
		JPanel registrationPanel = new JPanel();
		registrationPanel.setLayout(new BoxLayout(registrationPanel, BoxLayout.Y_AXIS));
		registrationPanel.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Registration"), new EmptyBorder(10, 10, 10, 10)));
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
		autoCropPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, registrationPanel.getMaximumSize().height));		

		add(maxShiftPanel);
		add(sliceThicknessCorrectionPanel);
		add(autoCropPanel);
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
		
		progressBar.setMinimum(0);
		progressBar.setMaximum(RegistrationSwingWorker.progressBarScaleFactor * 100);
		progressBar.setValue(0);
		progressBar.setString("0.0%");
		progressBar.setStringPainted(true); // show percentage progress as text in the progress bar
		progressBar.setVisible(true);
				
		Runnable whenDone = () -> {
			setRegistrationDone(worker.isCancelled() ? "<html><font color=red>Registration cancelled.</font></html>": "Registration done.");
		};
		
		Consumer<String> whenError = (String msg) -> {
			setRegistrationDone("<html><font color=red>Registration failed.</font><p>" + msg + "</html>");
		};
			
		WizardModel model = wizard.getModel();
		Rectangle templatePatchRect = model.getReferenceImage().getRoi().getBounds();
		
		RegistrationParameters parameters = new RegistrationParameters(model.getInputFiles(), model.getOutputFolder(),
																	   templatePatchRect,
																	   maxShiftPanel.getMaxShiftX(), maxShiftPanel.getMaxShiftY(),
																	   sliceThicknessCorrectionPanel.thicknessCorrection(), sliceThicknessCorrectionPanel.thicknessNM(),
																	   sliceThicknessCorrectionPanel.preserveSliceOrder(), 
																	   autoCropPanel.getNonblackRegion());

		worker = new RegistrationSwingWorker(parameters, progressBar, whenDone, whenError);
		
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
	
	private void setRegistrationDone(String message)  // called when the registration is done (either successful, or failed with an error, or cancelled)
	{
		busyRegistering = false;
		cancelButton.setVisible(false);
		statusLabel.setText(message);
		progressBar.setVisible(false);
		wizard.updateButtons();		
	}
}
