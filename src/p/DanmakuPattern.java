package p;

import java.util.ArrayList;

public class DanmakuPattern {
	/*public int angles[];
	public int timing[];
	public int variation[];
	public int velocity[];*/
	
	//public int patternTic = 0;
	private int patternLastTic = -1;
	public DanmakuShape[] shapes;
	public boolean repeat = false;
	
	public ArrayList<DanmakuBullet> Tic(mobj_t source){
		ArrayList<DanmakuBullet> bullets = new ArrayList<DanmakuBullet>();
		
		//Set how long this pattern lasts
		if(patternLastTic == -1) SetLastTic();
		
		//Iterate through all shapes and see if we have bullets to spawn
		for(int i = 0; i < shapes.length; i++){
			//Ignore shapes that haven't started yet
			if(source.d_tic >= shapes[i].beginTic){
				int shapeTics = 0;
				//Go through bullets in the pattern
				for(int j = 0; j < shapes[i].bullets.length; j++){
					shapeTics += shapes[i].bullets[j].timing;
					
					//If valid bullet -> Add to array
					if(source.d_tic == shapeTics + shapes[i].beginTic){
						bullets.add(shapes[i].bullets[j]);
					}else if (source.d_tic < shapeTics + shapes[i].beginTic) break;
				}
			}
		}
		source.d_tic++;
		
		return bullets;
	}
	
	public void BeginPattern(mobj_t source){
		source.d_tic = 0;
	}
	
	public boolean IsPatternFinished(mobj_t source){
		if(source.d_tic >= patternLastTic) return true;
		else return false;
	}
	
	public int GetLength(){
		if(patternLastTic == -1) SetLastTic();
		return patternLastTic;
	}
	
	private void SetLastTic(){
		int temp = -1;
		
		for(int i = 0; i < shapes.length; i++){
			temp = Math.max(temp, shapes[i].GetShapeDuration() + shapes[i].beginTic);
		}
		patternLastTic = temp;
	}
}
