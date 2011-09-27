package boom;

/** cph - move compatibility levels here so we can use them in d_server.c
 * 
 * @author cph
 *
 */
 
public final class Compatibility {

      /** Doom v1.2 */
      public static final int doom_12_compatibility=0;
      
      public static final int doom_1666_compatibility=1; /* Doom v1.666 */
      public static final int doom2_19_compatibility=2;  /* Doom & Doom 2 v1.9 */
      public static final int ultdoom_compatibility=3;   /* Ultimate Doom and Doom95 */
      public static final int finaldoom_compatibility=4;     /* Final Doom */
      public static final int dosdoom_compatibility=5;     /* DosDoom 0.47 */
      public static final int tasdoom_compatibility=6;     /* TASDoom */
      public static final int boom_compatibility_compatibility=7;      /* Boom's compatibility mode */
      public static final int boom_201_compatibility=8;                /* Boom v2.01 */
      public static final int boom_202_compatibility=9;                /* Boom v2.02 */
      public static final int lxdoom_1_compatibility=10;                /* LxDoom v1.3.2+ */
      public static final int mbf_compatibility=11;                     /* MBF */
      public static final int prboom_1_compatibility=12;                /* PrBoom 2.03beta? */
      public static final int prboom_2_compatibility=13;                /* PrBoom 2.1.0-2.1.1 */
      public static final int prboom_3_compatibility=14;                /* PrBoom 2.2.x */
      public static final int prboom_4_compatibility=15;                /* PrBoom 2.3.x */
      public static final int prboom_5_compatibility=16;              /* PrBoom 2.4.0 */
      public static final int prboom_6_compatibility=17;             /* Latest PrBoom */
      public static final int MAX_COMPATIBILITY_LEVEL=18;           /* Must be last entry */
      /* Aliases follow */
      public static final int boom_compatibility = boom_201_compatibility; /* Alias used by G_Compatibility */
      public static final int best_compatibility = prboom_6_compatibility;
      
      public static final prboom_comp_t[] prboom_comp = {
              new prboom_comp_t(0xffffffff, 0x02020615, false, "-force_monster_avoid_hazards"),
              new prboom_comp_t(0x00000000, 0x02040601, false, "-force_remove_slime_trails"),
              new prboom_comp_t(0x02020200, 0x02040801, false, "-force_no_dropoff"),
              new prboom_comp_t(0x00000000, 0x02040801, false, "-force_truncated_sector_specials"),
              new prboom_comp_t(0x00000000, 0x02040802, false, "-force_boom_brainawake"),
              new prboom_comp_t(0x00000000, 0x02040802, false, "-force_prboom_friction"),
              new prboom_comp_t(0x02020500, 0x02040000, false, "-reject_pad_with_ff"),
              new prboom_comp_t(0xffffffff, 0x02040802, false, "-force_lxdoom_demo_compatibility"),
              new prboom_comp_t(0x00000000, 0x0202061b, false, "-allow_ssg_direct"),
              new prboom_comp_t(0x00000000, 0x02040601, false, "-treat_no_clipping_things_as_not_blocking"),
              new prboom_comp_t(0x00000000, 0x02040803, false, "-force_incorrect_processing_of_respawn_frame_entry"),
              new prboom_comp_t(0x00000000, 0x02040601, false, "-force_correct_code_for_3_keys_doors_in_mbf"),
              new prboom_comp_t(0x00000000, 0x02040601, false, "-uninitialize_crush_field_for_stairs"),
              new prboom_comp_t(0x00000000, 0x02040802, false, "-force_boom_findnexthighestfloor"),
              new prboom_comp_t(0x00000000, 0x02040802, false, "-allow_sky_transfer_in_boom"),
              new prboom_comp_t(0x00000000, 0x02040803, false, "-apply_green_armor_class_to_armor_bonuses"),
              new prboom_comp_t(0x00000000, 0x02040803, false, "-apply_blue_armor_class_to_megasphere"),
              new prboom_comp_t(0x02050001, 0x02050003, false, "-wrong_fixeddiv"),
              new prboom_comp_t(0x02020200, 0x02050003, false, "-force_incorrect_bobbing_in_boom"),
              new prboom_comp_t(0xffffffff, 0x00000000, false, "-boom_deh_parser"),
              new prboom_comp_t(0x00000000, 0x02050007, false, "-mbf_remove_thinker_in_killmobj"),
              new prboom_comp_t(0x00000000, 0x02050007, false, "-do_not_inherit_friendlyness_flag_on_spawn"),
              new prboom_comp_t(0x00000000, 0x02050007, false, "-do_not_use_misc12_frame_parameters_in_a_mushroom")
            };
      
    }
