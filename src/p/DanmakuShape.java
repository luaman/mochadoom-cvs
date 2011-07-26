package p;

import static m.fixed_t.FRACUNIT;

public class DanmakuShape {
	static int FU = FRACUNIT;
	
	private static DanmakuShape[] shapes;
	
	public DanmakuBullet[] bullets;
	private float velocityMod;
	private float variationMod;
	private float angleMod;
	public int beginTic;
	
	public int GetShapeDuration(){
		int ticks = 0;
		for(int i = 0; i < bullets.length; i++){
			ticks += bullets[i].timing;
		}
		
		return ticks;
	}
	
	public void SetVelocityMod(float newVelocityMod){
		velocityMod = newVelocityMod;
		for(int i = 0; i < bullets.length; i++){
			bullets[i].velocity = (int)(bullets[i].velocity * velocityMod);
		}
	}
	
	public void SetVariationMod(float newVariationMod){
		variationMod = newVariationMod;
		for(int i = 0; i < bullets.length; i++){
			bullets[i].variation = (int)(bullets[i].variation * variationMod);
		}
	}
	
	public void SetAngleMod(float newAngleMod){
		angleMod = newAngleMod;
		for(int i = 0; i < bullets.length; i++){
			bullets[i].angle = (int)(bullets[i].angle + angleMod);
		}
	}
	
	////////////////////////////////////
	// Static inits & getters //////////
	////////////////////////////////////
	
	public static DanmakuShape GetShape(int index, float angleMod, float velocityMod, float variationMod, int beginTics){
		DanmakuShape tempShape = new DanmakuShape();
		tempShape.bullets = new DanmakuBullet[shapes[index].bullets.length];
		for(int i = 0; i < tempShape.bullets.length; i++){
			DanmakuBullet bullet = new DanmakuBullet(shapes[index].bullets[i].angle,
													 shapes[index].bullets[i].timing,
													 shapes[index].bullets[i].variation,
													 shapes[index].bullets[i].velocity);
			tempShape.bullets[i] = bullet;
		}
		tempShape.beginTic = beginTics;
		if (angleMod != 0) tempShape.SetAngleMod(angleMod);
		if (velocityMod != 0) tempShape.SetVelocityMod(velocityMod);
		if (variationMod != 0) tempShape.SetVariationMod(variationMod);
		
		return tempShape;
	}
	
	public static void InitShapes(){
		//Init all danmaku shapes into the static array~!
		shapes = new DanmakuShape[2];
		
		//Shape #1
		DanmakuShape shape = new DanmakuShape();
		shape.bullets = new DanmakuBullet[16];
		
		shape.bullets[0] = new DanmakuBullet(   0, 0, 1, 1*FU);
		shape.bullets[1] = new DanmakuBullet( 180, 0, 1, 1*FU);
		shape.bullets[2] = new DanmakuBullet(  90, 0, 1, 1*FU);
		shape.bullets[3] = new DanmakuBullet( -90, 0, 1, 1*FU);
		shape.bullets[4] = new DanmakuBullet(  45, 0, 1, 1*FU);
		shape.bullets[5] = new DanmakuBullet(-135, 0, 1, 1*FU);
		shape.bullets[6] = new DanmakuBullet( 135, 0, 1, 1*FU);
		shape.bullets[7] = new DanmakuBullet( -45, 0, 1, 1*FU);
		shape.bullets[8] = new DanmakuBullet(  15, 0, 1, 1*FU);
		shape.bullets[9] = new DanmakuBullet(-165, 0, 1, 1*FU);
		shape.bullets[10] = new DanmakuBullet( -15, 0, 1, 1*FU);
		shape.bullets[11] = new DanmakuBullet( 165, 0, 1, 1*FU);
		shape.bullets[12] = new DanmakuBullet(  30, 0, 1, 1*FU);
		shape.bullets[13] = new DanmakuBullet(-150, 0, 1, 1*FU);
		shape.bullets[14] = new DanmakuBullet( -30, 0, 1, 1*FU);
		shape.bullets[15] = new DanmakuBullet( 150, 8, 1, 1*FU);
		
		shapes[0] = shape;
		
		//Shape #2
		
		shape = new DanmakuShape();
		shape.bullets = new DanmakuBullet[7];
		
		shape.bullets[0] = new DanmakuBullet(   0, 4, 0, 1*FU);
		shape.bullets[1] = new DanmakuBullet(   5, 0, 0, 1*FU);
		shape.bullets[2] = new DanmakuBullet(  -5, 4, 0, 1*FU);
		shape.bullets[3] = new DanmakuBullet(  10, 0, 0, 1*FU);
		shape.bullets[4] = new DanmakuBullet( -10, 4, 0, 1*FU);
		shape.bullets[5] = new DanmakuBullet(  15, 0, 0, 1*FU);
		shape.bullets[6] = new DanmakuBullet( -15, 4, 0, 1*FU);
		
		shapes[1] = shape;
	}
}
