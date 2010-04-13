package cmview;

import java.awt.Color;

public class ColorScale {
	
	public Color getColor4BlueRedScale(double val, float alpha, float minAlpha){
		float alphaScaled = minAlpha + (1-minAlpha)*alpha;
		return getColor4HotColdScale(val, alphaScaled);
	}
	
	public Color getColor4BlueRedScale(double val, float alpha){
		Color color;
		int alphaInt = Math.round(alpha*255 + 0.5f);
		if (alphaInt>255)
			alphaInt = 255;
		if(val<0){
//			color = new Color(24/255,116/255,205/255,alpha);
			color = new Color(24,116,205,alphaInt);
		}
		else if (val>0){
//			color = new Color(238/255,44/255,44/255,alpha);
			color = new Color(238,44,44,alphaInt);
		}
		else // if (val==0)
			color = Color.white;
		return color;
	}
	
	public Color getColor4HotColdScale(double val, float alpha, float minAlpha){
		float alphaScaled = minAlpha + (1-minAlpha)*alpha;
		return getColor4HotColdScale(val, alphaScaled);
	}
	
	public Color getColor4HotColdScale(double val, float alpha){
		
		float redComp=1.0f, greenComp=1.0f, blueComp=1.0f;
		float minNegRC=0*255/100,  minNegGC=69*255/100, minNegBC=39*255/100;
		float maxNegRC=11*255/100, maxNegGC=11*255/100, maxNegBC=70*255/100;
		float minPosRC=100*255/100,  minPosGC=0*255/100, minPosBC=0*255/100;
		float maxPosRC=100*255/100, maxPosGC=67*255/100, maxPosBC=0*255/100;
		
		Color color;
		
		// [-1:val:+1]
			
		if (val<0){
			// --> Mapping on blue scale
			val = Math.abs(val);
			redComp = (minNegRC+((maxNegRC-minNegRC)*(float)(val)))/255;
			greenComp = (minNegGC+((maxNegGC-minNegGC)*(float)(val)))/255;
			blueComp = (minNegBC+((maxNegBC-minNegBC)*(float)(val)))/255;
		}
		else if (val>0){
			// --> Mapping on red scale
			redComp = (minPosRC+((maxPosRC-minPosRC)*(float)(val)))/255;
			greenComp = (minPosGC+((maxPosGC-minPosGC)*(float)(val)))/255;
			blueComp = (minPosBC+((maxPosBC-minPosBC)*(float)(val)))/255;
		}

//		System.out.println(val+":"+redComp+"_"+greenComp+"_"+blueComp);//+"\t");
		
		color = new Color(redComp,greenComp,blueComp,alpha);
		
		return color;
	}
	
	public Color getColor4YellowRedScale (float val, float alpha){
		Color color;
		float redComp=1.0f, greenComp=1.0f, blueComp=1.0f;
		float minRC=100*255/100,  minGC=0*255/100, minBC=0*255/100;
		float maxRC=100*255/100, maxGC=80*255/100, maxBC=0*255/100;		
		
		redComp = (minRC+((maxRC-minRC)*(float)Math.abs(val)))/255;
		greenComp = (minGC+((maxGC-minGC)*(float)Math.abs(val)))/255;
		blueComp = (minBC+((maxBC-minBC)*(float)Math.abs(val)))/255;
		
//		System.out.println(val+":"+redComp+"_"+greenComp+"_"+blueComp);
		
		color = new Color(redComp,greenComp,blueComp,alpha);
		return color;
	}
	
	public Color getColor4GreyValueRange(float val, float alpha){
		Color color = getColor4GreyValueRange(val, alpha, +1);
		return color;
	}
	
	public Color getColor4GreyValueRange(float val, int dir){
		Color color = getColor4GreyValueRange(val, 1.0f, dir);
		return color;
	}
	
	public Color getColor4GreyValueRange(float val, float alpha, int dir){
		Color color;
		if (dir<0)
			val = 1-val;
		// val is supposed to be within range [0.0:1.0]
		// 0->black 1->white
		color = new Color(val, val, val, alpha);
		return color;
	}
	
	public Color getColor4RGBscale(float val, float alpha){
		Color color = getColor4RGBscale(val, alpha, +1);
		return color;
	}
	
	public Color getColor4RGBscale(float val, float alpha, int dir){
		float rC=0, gC=0, bC=0;
		float midpoint = 0.5f;
		
		if (dir<0)
			val = 1-val; // inversion:  color scale from blue to red instead of red to blue
		
		if (val<=midpoint){
			rC = (-1/midpoint)*val + 1;
			gC = (1/midpoint)*val;
			bC = 0;
		}
		else if (val>midpoint){
			rC = 0;
			gC = (1/(midpoint-1))*(val-1); // (x/midpoint-1)-(1/midpoint-1);
			bC = (1/(1-midpoint))*(val-midpoint); // (x/1-midpoint)-(1/1-midpoint);
		}
//		else  {
//			rC = 0;
//			gC = 1;
//			bC = 0;
//		}
				
		Color color = new Color(rC, gC, bC, alpha);
		return color;
	}
	
	public Color getColor4RGBscalePolar(float val, float alpha){
		Color color = getColor4RGBscalePolar(val, alpha, +1);
		return color;
	}
	
	public Color getColor4RGBscalePolar(float val, float alpha, int dir){
		float rC=0, gC=0, bC=0;
		float midpoint = 0.5f;
		
		if (dir<0){
			val = -1*val;
//			if (val > 0)
//				val = 1-val; // inversion:  color scale from blue to red instead of red to blue
//			else 
//				val = val +1;
		}
		
		if (val<0){
			val = Math.abs(val);
			// scaling to range 0.0:midpoint (red:green)
			val = midpoint - (val*midpoint);
			rC = (-1/midpoint)*val + 1;
			gC = (1/midpoint)*val;
			bC = 0;
		}
		else if (val>0){
			// scaling to range midpoint:1.0 (green:blue)
			val = midpoint + val*midpoint;
			rC = 0;
			gC = (1/(midpoint-1))*(val-1); // (x/midpoint-1)-(1/midpoint-1);
			bC = (1/(1-midpoint))*(val-midpoint); // (x/1-midpoint)-(1/1-midpoint);
		}
		else { // val=0
			val = midpoint;
			rC = (-1/midpoint)*val + 1;
			gC = (1/midpoint)*val;
			bC = 0;
		}
		
		System.out.println(val+":"+rC+"_"+gC+"_"+bC);
		Color color = new Color(rC, gC, bC, alpha);
		return color;
	}

}
