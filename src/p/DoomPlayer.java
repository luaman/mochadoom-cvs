package p;

public interface DoomPlayer {
	
	fixed_t
	P_AimLineAttack
	( mobj_t*	t1,
	  angle_t	angle,
	  fixed_t	distance );

	void
	P_LineAttack
	( mobj_t*	t1,
	  angle_t	angle,
	  fixed_t	distance,
	  fixed_t	slope,
	  int		damage );

	void
	P_RadiusAttack
	( mobj_t*	spot,
	  mobj_t*	source,
	  int		damage );
	
	void
	P_TouchSpecialThing
	( mobj_t*	special,
	  mobj_t*	toucher );

	void
	P_DamageMobj
	( mobj_t*	target,
	  mobj_t*	inflictor,
	  mobj_t*	source,
	  int		damage );
	}
}
