/*******************************************************************************
 * Copyright 2012 Philippe PRADOS 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fr.prados.contacts.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import fr.prados.contacts.Application;
import fr.prados.contacts.lib.R;
import static fr.prados.contacts.Constants.*;

public final class Photos
{
	public static int _photoSize;
	static
	{
		startup(Application.context);
	}
	private static void startup(Context context)
	{
		_photoSize=context.getResources().getDimensionPixelSize(R.dimen.photo_resize);
	}

	public static final Bitmap extractFace(byte[] jpeg)
	{
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		final Matrix matrix = new Matrix();
		Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length,options);
		int width=bitmap.getWidth();
		int height=bitmap.getHeight();
		int max=width>height ? height : width;
	
		PointF p=new PointF((float)width/2.0f,(float)height/2.0f);
		
		if (FACE_DETECTOR)
		{
			FaceDetector.Face[] faces=new FaceDetector.Face[1];
			if ((width&0x1)!=0)
			{
				--width;
				max=width>height ? height : width;
				final Bitmap newBitmap=Bitmap.createBitmap(bitmap,0,0, width, height);
				bitmap.recycle();
				bitmap=newBitmap;
			}
			FaceDetector faceDetector=new FaceDetector(width, height, 1);
			if (faceDetector.findFaces(bitmap, faces)==1)
			{
				final FaceDetector.Face face=faces[0];
				int newMax=0;
				if (face.confidence()>=Face.CONFIDENCE_THRESHOLD)
				{
					face.getMidPoint(p);
					newMax=(int)(face.eyesDistance()*4f);
					max=(newMax>max) ? max : newMax;
	
					if (FACE_SHOW)
					{
						Bitmap nbitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true);
						bitmap.recycle(); // Help GC
						bitmap=nbitmap;
						bitmap.prepareToDraw();
						Canvas canvas=new Canvas(bitmap);
						Paint paint=new Paint();
						paint.setColor(Color.RED);
						paint.setStyle(Style.STROKE);
						paint.setStrokeWidth(20);
						canvas.drawCircle((int)p.x, (int)p.y, newMax/2, paint);
					}
					final int dmax=max/2;
					p.x=p.x-dmax;
					p.y=p.y-dmax;
					if (p.x+max>width) p.x=width-max;
					if (p.y+max>height) p.y=height-max;
					if (p.x<0) p.x=0;
					if (p.y<0) p.y=0;
					int[] crops=new int[max*max];
					bitmap.getPixels(crops, 0,  max, (int)p.x, (int)p.y, max, max);
					bitmap=Bitmap.createBitmap(crops, max, max, Config.ARGB_8888);
					width=max;
					height=max;
					crops=null;
				}
			}
		}
		int dmax=max/2;
		p.x=p.x-dmax;
		p.y=p.y-dmax;
		if (p.x+max>width) 
			p.x=width-max;
		if (p.y+max>height) 
			p.y=height-max;
		if (p.x<0) p.x=0;
		if (p.y<0) p.y=0;
		float scale = ((float) _photoSize) / max;
		if (scale<1)
			matrix.postScale(scale, scale);
		final Bitmap rc=Bitmap.createBitmap(bitmap, (int)p.x, (int)p.y,max, max, matrix, true);
		return rc;
	}

}
