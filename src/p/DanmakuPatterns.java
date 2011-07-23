package p;

import static m.fixed_t.FRACUNIT;

public class DanmakuPatterns {
	static int FU = FRACUNIT;
	
	public static final int angles[]=new int[]{0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
		15, -165, -15, 165, 30, -150, -30, 150};

public static final int timing[]=new int[]{0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 8,0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 8,0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 8,0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 8,0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0,
	0, 0, 0, 0, 0, 0, 0, 8};

public static final int variation[] = new int[]{2, 2, 2, 2, 2, 2, 2, 2,
	2, 2, 2, 2, 2, 2, 2, 2,3, 3, 3, 3, 3, 3, 3, 3,
	3, 3, 3, 3, 3, 3, 3, 3,4, 4, 4, 4, 4, 4, 4, 4,
	4, 4, 4, 4, 4, 4, 4, 4,4, 4, 4, 4, 4, 4, 4, 4,
	4, 4, 4, 4, 4, 4, 4, 4,5, 5, 5, 5, 5, 5, 5, 5,
	5, 5, 5, 5, 5, 5, 5, 5,5, 5, 5, 5, 5, 5, 5, 5,
	5, 5, 5, 5, 5, 5, 5, 5,6, 6, 6, 6, 6, 6, 6, 6,
	6, 6, 6, 6, 6, 6, 6, 6};

//Imp default velocity = 10*FRACUNIT

public static final int velocity[] = new int[]{4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU,
	4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU,8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU,
	8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU,12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU,
	12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
	16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
	16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
	16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
	16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU};

}


//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////
////////// P A T T E R N    W A R E H O U S E ////////////////////////////
//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////

// no.1 the original pattern

/*
public static final int angles[]=new int[]{0,0,
	6,-6,
	12,-12,
	18,-18,
	24,-24,
	30,-30,
	36,-36,
	42,-42,
	48,-48,
	54,-54,
	60,-60,177,-177,
	66,-66,171,-171,
	72,-72,165,-165,
	78,-78,159,-159,
	84,-84,153,-153,
	90,-90,147,-147,
	96,-96,141,-141,
	102,-102,141,-141,
	108,-108,135,-135,
	114,-114,129,-129,
	120,-120,123,-123,
	126,-126,117,-117,
	132,-132,111,-111,
	138,-138,105,-105,
	144,-144,99,-99,
	150,-150,93,-93,
	156,-156,87,-87,
	162,-162,81,-81,
	168,-168,75,-75,
	174,-174,69,-69,
	180,-180,63,-63,
	57,-57,
	51,-51,
	45,-45,
	39,-39,
	33,-33,
	27,-27,
	21,-21,
	15,-15,
	9,-9,
	3,-3};

public static final int timing[]=new int[]{	1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,1,1,5,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7,
		1,7};
*/

//no 2. Timed round shot

/*
public static final int angles[]=new int[]{0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150,0, 180, 90, -90, 45, -135, 135, -45,
15, -165, -15, 165, 30, -150, -30, 150};

public static final int timing[]=new int[]{1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 8,1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 8,1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 1,1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 1,1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 1,1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 1,1, 1, 1, 1, 1, 1, 1, 1,
1, 1, 1, 1, 1, 1, 1, 8};

public static final int variation[] = new int[]{2, 2, 2, 2, 2, 2, 2, 2,
2, 2, 2, 2, 2, 2, 2, 2,3, 3, 3, 3, 3, 3, 3, 3,
3, 3, 3, 3, 3, 3, 3, 3,4, 4, 4, 4, 4, 4, 4, 4,
4, 4, 4, 4, 4, 4, 4, 4,4, 4, 4, 4, 4, 4, 4, 4,
4, 4, 4, 4, 4, 4, 4, 4,4, 4, 4, 4, 4, 4, 4, 4,
4, 4, 4, 4, 4, 4, 4, 4,4, 4, 4, 4, 4, 4, 4, 4,
4, 4, 4, 4, 4, 4, 4, 4,4, 4, 4, 4, 4, 4, 4, 4,
4, 4, 4, 4, 4, 4, 4, 4};

//Imp default velocity = 10*FRACUNIT

public static final int velocity[] = new int[]{4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU,
4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU, 4*FU,8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU,
8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU, 8*FU,12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU,
12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU, 12*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU,
16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU, 16*FU};
*/