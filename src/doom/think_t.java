package doom;

import p.mobj;
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
    A_WeaponReady,
    A_Lower,
    A_Raise,
    A_Punch,
    A_ReFire,
    A_FirePistol,
    A_Light1,
    A_FireShotgun,
    A_Light2,
    A_FireShotgun2,
    A_CheckReload,
    A_OpenShotgun2,
    A_LoadShotgun2,
    A_CloseShotgun2,
    A_FireCGun,
    A_GunFlash,
    A_FireMissile,
    A_Saw,
    A_FirePlasma,
    A_BFGsound,
    A_FireBFG,
    A_BFGSpray,
    A_Explode,
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
