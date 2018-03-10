package be.vib.imagej.registration;

import java.util.concurrent.Callable;

import be.vib.bits.QFunction;   // IMPROVEME: move the Quasar code to be.vib.bits.quasar (or be.vib.quasar).
import be.vib.bits.QValue;
import ij.process.ImageProcessor;

public class Registerer implements Callable<RegistrationResult>
{
	private ImageProcessor referencePatch;
	private ImageProcessor image;
	private int xmin;
	private int ymin;
	private int xmax;
	private int ymax;
	
	public Registerer()
	{
		this.xmin = this.ymin = this.xmax = this.ymax = 0;
	}
	
	public void setParameters(ImageProcessor image, ImageProcessor referencePatch, int xmin, int xmax, int ymin, int ymax)
	{
		this.image = image;
		this.referencePatch = referencePatch;
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	}
	
	// Important: call() *must* be run on the Quasar thread!
	@Override
	public RegistrationResult call()
	{		
		QValue quasarImage = ImageUtils.newCubeFromImage(image);
		QValue quasarReferencePatch = ImageUtils.newCubeFromImage(referencePatch);   // TODO: avoid repeatedly converting the (currently fixed) reference patch to a Quasar cube
		
		// Quasar function registration(img, ref_patch, xmin, xmax, ymin, ymax, best_pos)
		QFunction registration = new QFunction("registration(mat,mat,int,int,int,int,mat)"); 
		
		QFunction zeros = new QFunction("zeros(...)");
		QValue bestPos = zeros.apply(new QValue(1), new QValue(2));  // a 1x2 placeholder vector for returning the position [y, x] of the best matching patch

		registration.apply(quasarImage, quasarReferencePatch, new QValue(xmin), new QValue(xmax), new QValue(ymin), new QValue(ymax), bestPos);

		int posX = bestPos.at(1).getInt();
		int posY = bestPos.at(0).getInt();
		
		quasarImage.dispose();
		quasarReferencePatch.dispose();
		bestPos.dispose();
		
		return new RegistrationResult(posX, posY);
	}

}