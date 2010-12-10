package doom;

import p.mobj_t;
import p.pspdef_t;

//Historically, "think_t" is yet another
//function pointer to a routine to handle
//an actor.

//
//Experimental stuff.
//To compile this as "ANSI C with classes"
//we will need to handle the various
//action functions cleanly.
//
//typedef  void (*actionf_v)();
//typedef  void (*actionf_p1)( void* );
//typedef  void (*actionf_p2)( void*, void* );

/*typedef union
{
actionf_p1	acp1;
actionf_v	acv;
actionf_p2	acp2;

} actionf_t;

*/

public enum think_t {
	
	A_Light0(0),
    A_WeaponReady(2),
    A_Lower(2),
    A_Raise(2),
    A_Punch(2),
    A_ReFire(2),
    A_FirePistol(2),
    A_Light1(2),
    A_FireShotgun(2),
    A_Light2(2),
    A_FireShotgun2(2),
    A_CheckReload(2),
    A_OpenShotgun2(2),
    A_LoadShotgun2(2),
    A_CloseShotgun2(2),
    A_FireCGun(2),
    A_GunFlash(2),
    A_FireMissile(2),
    A_Saw(2),
    A_FirePlasma(2),
    A_BFGsound(2),
    A_FireBFG(2),
    A_BFGSpray(2),
    A_Explode(2),
    A_Pain,
    A_PlayerScream,
    A_Fall,
    A_XScream,
    A_Look,
    A_Chase,
    A_FaceTarget,
    A_PosAttack,
    A_Scream,
    A_SPosAttack,
    A_VileChase,
    A_VileStart,
    A_VileTarget,
    A_VileAttack,
    A_StartFire,
    A_Fire,
    A_FireCrackle,
    A_Tracer,
    A_SkelWhoosh,
    A_SkelFist,
    A_SkelMissile,
    A_FatRaise,
    A_FatAttack1,
    A_FatAttack2,
    A_FatAttack3,
    A_BossDeath,
    A_CPosAttack,
    A_CPosRefire,
    A_TroopAttack,
    A_SargAttack,
    A_HeadAttack,
    A_BruisAttack,
    A_SkullAttack,
    A_Metal,
    A_SpidRefire,
    A_BabyMetal,
    A_BspiAttack,
    A_Hoof,
    A_CyberAttack,
    A_PainAttack,
    A_PainDie,
    A_KeenDie,
    A_BrainPain,
    A_BrainScream,
    A_BrainDie,
    A_BrainAwake,
    A_BrainSpit,
    A_SpawnSound,
    A_SpawnFly,
    A_BrainExplode,
    P_MobjThinker(1),
    T_FireFlicker(1),
	T_LightFlash(1),
	T_StrobeFlash(1),
	T_Glow(1),
	T_MoveCeiling(1),
	T_MoveFloor(1),
	T_VerticalDoor(1),
	T_PlatRaise(1);
    think_t(){
	    type=0;
	}
	
    think_t(int type){
        this.type=type;
    }

    /** 0 for void, 1 for acp1, 2 for acp2 */
    public int getType() {
        return type;
    }

    private int type;
	
}
