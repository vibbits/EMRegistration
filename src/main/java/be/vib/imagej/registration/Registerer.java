package be.vib.imagej.registration;

import java.util.concurrent.Callable;


public class Registerer implements Callable<Void>
{
//	public Registerer()
//	{
//	}
	

	// Important: call() *must* be run on the Quasar thread!
	@Override
	public Void call()
	{		/*
		QFunction gaussian = new QFunction("gaussian_filter(mat,scalar,int,string)");
		
		QValue noisyImageCube = ImageUtils.newCubeFromImage(image);
		
		//float r = ImageUtils.bitRange(image);
		
		//QUtils.inplaceDivide(noisyImageCube, r);  // scale pixels values from [0, 255] or [0, 65535] down to [0, 1]

		GaussianParams params = (GaussianParams)this.params;

		QValue denoisedImageCube = gaussian.apply(noisyImageCube,
							                      new QValue(params.sigma),
							                      new QValue(0),
							                      new QValue("mirror"));
		
		noisyImageCube.dispose();

		//QUtils.inplaceMultiply(denoisedImageCube, r); // scale pixels values back to [0, 255] or [0, 65535]

		ImageProcessor denoisedImage = ImageUtils.newImageFromCube(image, denoisedImageCube);

		denoisedImageCube.dispose();

		return denoisedImage;*/
		return null;
	}

}