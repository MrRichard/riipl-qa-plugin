package edu.wfubmc.riiplqa;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.File;

import ij.ImagePlus;
import ij.io.FileSaver;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

//import ij.measure.Calibration;

//Slice numbering
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.Graphics;
import java.awt.Color;

//Image Resizing
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

//Alternate saving
//import javax.imageio.ImageIO;

//Nifti reading class
import net.sourceforge.niftilib.Nifti1Dataset;

//Correcting for sagital views and skew
//Math should be in the standard java lang includes
//import java.lang.Math;

/** A class/executable for creating a mosaic of slices from nifti images
* @author Ben Wagner
* @author RIIPL Lab
* @version 0.1 August 2011
*/
public class NiftiSnapshot
{
	/** Executable is "java -jar NiftiSnapshot outfile.jpg nifti1.nii nifti2.nii ..."
	*/
	public static void main (String[] args) 
	{
		String outFile=null;
		ArrayList<File> files=new ArrayList<File>();
		for (String s: args) 
		{
				if (outFile==null)
				{
					outFile=s;
				}
				else
				{
					files.add(new File(s));
				}
		}
		
		try
		{
			ImagePlus outImg = createMontage(outFile, files, true, true);
		}
		catch (Exception e)
		{
			System.out.println();
			e.printStackTrace();
			System.exit(1);
		}
//		outImg.show();
	}

	
	/** Creates a jpg of slices from multiple nifti images
	* @param		outFile	Name of the jpg output file
	* @param		files		An ArrayList of nifti filenames.  May be nii, nii.gz, or img/hdr
	* @param		flipLR	Flip the voxel information Left to Right
	* @param		flipUD	Flip the voxel information Up to Down
	* @return						An ij.ImagePlus (ImageJ) that may be shown or otherwise manipulated
	*/
	public static ImagePlus createMontage(String outFile, ArrayList<File> files, boolean flipLR, boolean flipUD) throws Exception
	{
		int maxX=0, maxY=0, maxZ=0;
		ArrayList<Nifti1Dataset> niftiSets = new ArrayList<Nifti1Dataset>();
		int orient=-1;

		String[] orientNames = new String[] {"Axial","Sagittal","Coronal"};

		Iterator it=files.iterator();
		while(it.hasNext())
		{
			int thisOrient=-1;
			File f=(File)it.next();
			System.out.println("Reading: " + f);
			Nifti1Dataset nifti1 = new Nifti1Dataset(f.getAbsolutePath());
			try
			{
				nifti1.readHeader();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new Exception("Error reading file: " + f.getAbsolutePath());
			}
			niftiSets.add(nifti1);
//				nifti1.printHeader();

			short nX = nifti1.XDIM;
			short nY = nifti1.YDIM;
			short nZ = nifti1.ZDIM;
			short imageTypeTmp = nifti1.getDatatype();

			System.out.println("nX: " + nX);
			System.out.println("nY: " + nY);
			System.out.println("nZ: " + nZ);
			System.out.println("BitDepth: " + nifti1.getBitpix());
			System.out.println("ImageType: " + imageTypeTmp);
			System.out.println("DataType: " + nifti1.decodeDatatype(imageTypeTmp));

			if (nX > maxX)
				maxX=nX;
			if (nY > maxY)
				maxY=nY;
			if (nZ > maxZ)
				maxZ=nZ;


/*
			System.out.println("Q factor:\t\t\t\t\t"+nifti1.qfac);
			System.out.println("Qform transform code:\t\t\t\t"+nifti1.qform_code+" ("+nifti1.decodeXform(nifti1.qform_code)+")");
			System.out.println("Quaternion b,c,d params:\t\t\t"+nifti1.quatern[0]+" "+nifti1.quatern[1]+" "+nifti1.quatern[2]);
			System.out.println("Quaternion x,y,z shifts:\t\t\t"+nifti1.qoffset[0]+" "+nifti1.qoffset[1]+" "+nifti1.qoffset[2]);
*/

			System.out.println("Affine transform code:\t\t\t\t"+nifti1.sform_code+" ("+nifti1.decodeXform(nifti1.sform_code)+")");
			System.out.print("1st row affine transform:\t\t\t");
			for (int i=0; i<4; i++)
				System.out.printf("% 02.4f ",nifti1.srow_x[i]);
			System.out.println("");
			System.out.print("2nd row affine transform:\t\t\t");
			for (int i=0; i<4; i++)
				System.out.printf("% 02.4f ",nifti1.srow_y[i]);
			System.out.println("");
			System.out.print("3rd row affine transform:\t\t\t");
			for (int i=0; i<4; i++)
				System.out.printf("% 02.4f ",nifti1.srow_z[i]);

			/* //This method doesn't work when skews are involved
			if (nifti1.srow_x[0] != 0)
				thisOrient=0; //axial
			else if(nifti1.srow_y[0] != 0)
				thisOrient=1; //sag
			else if(nifti1.srow_z[0] != 0)
				thisOrient=2; //coronal
			else
				throw new Exception("Unknown orientation for file: " + f.getAbsolutePath());
			*/
			
			double affx, affy, affz, maxAff;
			affx=Math.abs(nifti1.srow_x[0]);
			affy=Math.abs(nifti1.srow_y[0]);
			affz=Math.abs(nifti1.srow_z[0]);
			maxAff=Math.max(affx,Math.max(affy,affz));
			if (affx == maxAff)
				thisOrient=0; //axial
			else if(affy == maxAff)
				thisOrient=1; //sag
			else if(affz == maxAff)
				thisOrient=2; //coronal
			else
				throw new Exception("Unknown orientation for file: " + f.getAbsolutePath());
			
			
			if (orient==2)
				throw new Exception("Coronal views not yet programed: " + f.getAbsolutePath());
			
			if (orient < 0)
				orient=thisOrient;
				
			if (orient != thisOrient)
				throw new Exception("Image orientation mismatch!!\n  Template: "+orientNames[orient]+" - "+files.get(0).getAbsolutePath()+ "\n  Current File: "+orientNames[thisOrient]+" - "+f.getAbsolutePath());

			System.out.println();
		}

		System.out.println("max X: " + maxX);
		System.out.println("max Y: " + maxY);
		System.out.println("max Z: " + maxZ);
		System.out.println("Orient (0=Axial,1=Sag,2=Coronal): " + orient);
		
		double fontSize=10;
		int montX=0;
		int montY=0;
		if (orient==0) //axial
		{
			montX=maxX * maxZ;
			montY=maxY * files.size();
			fontSize=maxY/10;
		}
		else if (orient==1) //sag
		{
//			montX=maxZ * maxX;
//			montY=maxY * files.size();
			montX=maxZ * maxY;
			montY=maxX * files.size();
			fontSize=maxZ/10;
			flipUD=!flipUD;
			flipLR=!flipLR;
		}
		else //coronal
		{
			montX=maxX * maxZ;
			montY=maxY * files.size();
			fontSize=maxY/10;
		}

		
		System.out.println("montage X: " + montX);
		System.out.println("montage Y: " + montY);
		
		BufferedImage bufImage = new BufferedImage(montX,montY,BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster= bufImage.getRaster();
		Font f = new Font(Font.MONOSPACED, Font.PLAIN, (int)Math.round(fontSize)); 
		Graphics g = bufImage.getGraphics();
		g.setFont(f);	
		g.setColor(Color.WHITE);
		

		for (int i=0; i < files.size(); i++)
		{
			System.out.println("Working Image Index: " + i);
			Nifti1Dataset nifti1=niftiSets.get(i);
			double[][][] niftiData=null;
			try
			{
				short volNum=0;
				niftiData=nifti1.readDoubleVol(volNum);
				//!!!!  DATA IN niftiData IS IN ZYX ORDER !!!!
			}
			catch (Exception e)
			{
				throw new Exception("Reading volume information");
			}
			
			short nX = nifti1.XDIM;
			short nY = nifti1.YDIM;
			short nZ = nifti1.ZDIM;
			
			double minV=Double.MAX_VALUE;
			double maxV=Double.MIN_VALUE;
			for (int x=0; x < nX; x++)
			{
				for (int y=0; y < nY; y++)
				{
					for (int z=0; z < nZ; z++)
					{
						try
						{
							if (niftiData[z][y][x] > maxV)
								maxV=niftiData[z][y][x];
							if (niftiData[z][y][x] < minV)
								minV=niftiData[z][y][x];
						}
						catch (Exception e)
						{
							System.out.println("Error reading point in image index "+i+" at ("+x+","+y+","+z+")");
							System.out.println("Max size ("+nX+","+nY+","+nZ+")");
							throw new Exception("Reading image data at ("+x+","+y+","+z+")");
						}
					}
				}
			}
			
			System.out.println("minV: " + minV);
			System.out.println("maxV: " + maxV);
			
			int locA=0, locB=0, locC=0;
			int maxA=0, maxB=0, maxC=0;
			double v=0;

			if (orient==0) //axial
			{
				maxA=maxX;
				maxB=maxY;
				maxC=maxZ;
			}
			else if (orient==1) //sag
			{
				maxA=maxZ;
				maxB=maxX;
				maxC=maxY;
			}
			else //coronal
			{
				maxA=maxX;
				maxB=maxY;
				maxC=maxZ;
			}


			for (int z=0; z < nZ; z++)
			{
				for (int x=0; x < nX; x++)
				{
					for (int y=0; y < nY; y++)
					{
						try
						{
							v=niftiData[z][y][x];
							v=((v-minV)/(maxV-minV))*255;
							v=Math.round(v);
						}
						catch (Exception e)
						{
							System.out.println("Error reading point in image index "+i+" at ("+x+","+y+","+z+")");
							System.out.println("Max size ("+nX+","+nY+","+nZ+")");
							throw new Exception("Reading/Setting image data at ("+x+","+y+","+z+")");
						}
						if (orient==0) //axial
						{
							
							locA=x;
							locB=y;
							locC=z;
						}
						else if (orient==1) //sag
						{
							locA=z;
							locB=x;
							locC=y;
						}
						else //coronal
						{
							locA=x;
							locB=y;
							locC=z;
						}
/*
						try
						{
*/
							if (flipLR && flipUD)
							{
								raster.setSample((locC+1)*maxA-(locA+1),(i+1)*maxB-(locB+1),0,v);
							}
							else if (flipLR)
							{
								raster.setSample((locC+1)*maxA-(locA+1),locB+(i*maxB),0,v);
							}
							else if (flipUD)
							{
								raster.setSample(locA+(locC*maxA),(i+1)*maxB-(locB+1),0,v);
							}
							else
							{
								raster.setSample(locA+(locC*maxA),locB+(i*maxB),0,v);
							}
/*
						}
						catch (Exception e)
						{
							//do nothing, image will show it
							System.out.println("Error at ("+locA+","+locB+","+locC+")");
							System.out.println("Error at ("+locA+","+locB+","+locC+") is (" +locA+(locC*maxA) +"," +locB+(i*maxB)+")");
						}
*/
					}
				}
			}
			//Slice numbers
			for (locC=0; locC< maxC; locC++)
			{
				g.drawString(Integer.toString(locC+1),locC*maxA,i*maxB+maxB);
			}
		}


		g.dispose();
		
		//Check height/width restrictions (not greater than Short.MAX_VALUE
		int resizedMontX=montX;
		int resizedMontY=montY;
		if (resizedMontX > Short.MAX_VALUE)
		{
			double factor=(double)Short.MAX_VALUE/montX;
			resizedMontX=(int)((double)resizedMontX*factor);
			resizedMontY=(int)((double)resizedMontY*factor);
		}
		if (resizedMontY > Short.MAX_VALUE)
		{
			double factor=(double)Short.MAX_VALUE/montY;
			resizedMontX=(int)((double)resizedMontX*factor);
			resizedMontY=(int)((double)resizedMontY*factor);
		}

		System.out.println("resized montage X: " + resizedMontX);
		System.out.println("resized montage Y: " + resizedMontY);
		
		
		if (montX != resizedMontX || montY != resizedMontY)
		{
			double scale=(double)resizedMontX/(double)montX;
			System.out.println("Total image scaled to "+scale+"%");
			
			//First method for resizing (paint)	from: http://helpdesk.objects.com.au/java/how-do-i-scale-a-bufferedimage
//				BufferedImage scaledImage = new BufferedImage(resizedMontX,resizedMontY,bufImage.getType());
//				Graphics2D graphics2D = scaledImage.createGraphics();
//				graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//				graphics2D.drawImage(bufImage, 0, 0, resizedMontX, resizedMontY, null);
//				graphics2D.dispose();
		
			//Second method for resizing (AffineTransform)	from: http://helpdesk.objects.com.au/java/how-do-i-scale-a-bufferedimage
		
			BufferedImage scaledImage = new BufferedImage(resizedMontX,resizedMontY,bufImage.getType());
			Graphics2D graphics2D = scaledImage.createGraphics();
			AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics2D.drawImage(bufImage, xform, null);
			graphics2D.dispose();
			
			bufImage=scaledImage;
		}

		ImagePlus imp=new ImagePlus("Montage Image",bufImage);

		System.out.println();
		System.out.println("Final BitDepth: " + imp.getBitDepth());
		System.out.println("Final MinV: " + imp.getDisplayRangeMin());
		System.out.println("Final MaxV: " + imp.getDisplayRangeMax());
		System.out.println("Final ImageType: " + imp.getType());
		FileSaver fs=new FileSaver(imp);
		fs.setJpegQuality(100);
		System.out.println("Saving file: "+outFile);
		fs.saveAsJpeg(outFile);
		System.out.println("Done!");
		return imp;
	}

}


