package game;

import java.awt.Color;

/** The colours and scenery a map's terrain is painted with. */
public enum Theme {
    MEADOW(new int[] {0x3F7F3A, 0x478A3F, 0x4F9645, 0x58A24C}, new int[] {0xBE9B62, 0xC8A56A, 0xD2B178},
            0x86663A, 0xDFC28C, 0x2F6A30, 0x6DB85A, 0xA98852, 0xE6CC98,
            new int[] {0xF5E663, 0xF2F2F2, 0xF4A4C0}, Tree.ROUND,
            new int[] {0x1E4D22, 0x2A6B2C, 0x3A8A38, 0x55AA4A}, 0x5A3A1E,
            new int[] {0x474C55, 0x6E747E, 0x969CA6, 0xC5CAD2}, new Color(255, 255, 255, 120)),
    DESERT(new int[] {0xC9A45C, 0xD2AE66, 0xDBB872, 0xE3C380}, new int[] {0x9A6B43, 0xA5754B, 0xB08055},
            0x6E4A2C, 0xC49466, 0xA8873F, 0xEAD08F, 0x7E5636, 0xC79A6C,
            new int[] {}, Tree.CACTUS,
            new int[] {0x2E5E2A, 0x3C7A36, 0x4F9645, 0x7CC06A}, 0x3C7A36,
            new int[] {0x6B4A32, 0x8E6645, 0xB0855E, 0xD6AE86}, new Color(90, 50, 20, 120)),
    SNOW(new int[] {0xC9D8E6, 0xD6E2EE, 0xE2ECF5, 0xEEF4FA}, new int[] {0x6F7F92, 0x7B8B9E, 0x8898AA},
            0x4A586A, 0xA9B8C8, 0xA9BDD0, 0xFFFFFF, 0x5A6878, 0xA9B8C8,
            new int[] {}, Tree.PINE,
            new int[] {0x163B2A, 0x1F5238, 0x2B6A48, 0xF2F7FB}, 0x4A2F1A,
            new int[] {0x4A5563, 0x6F7C8C, 0x9AA7B6, 0xEEF4FA}, new Color(40, 70, 110, 130)),
    EMBER(new int[] {0x2E2630, 0x362C38, 0x3E3340, 0x473A48}, new int[] {0x5E4E4A, 0x6A5853, 0x76635C},
            0x1E171F, 0x8A746B, 0x221B24, 0x5A4A5C, 0x45383A, 0x94807A,
            new int[] {0xFB923C, 0xF87171}, Tree.DEAD,
            new int[] {0x120E14, 0x241C28, 0x241C28, 0x241C28}, 0x241C28,
            new int[] {0x2A1F22, 0x4A3030, 0xB4472B, 0xFDBA4D}, new Color(253, 186, 77, 120));

    public enum Tree { ROUND, CACTUS, PINE, DEAD }

    /** Ground shades, dark to light, and road shades likewise. */
    public final int[] ground, road;
    /** The dark edge where road meets ground, and the lighter lip inside it. */
    public final int kerb, lip;
    public final int tuftDark, tuftLight, pebbleDark, pebbleLight;
    /** Colours of the occasional bright speck (flowers, embers); empty for none. */
    public final int[] specks;
    public final Tree tree;
    /** Foliage shades, dark to light. */
    public final int[] canopy;
    public final int trunk;
    public final int[] rock;
    /** Colour of the dotted trail flyers follow. */
    public final Color trail;

    Theme(int[] ground, int[] road, int kerb, int lip, int tuftDark, int tuftLight, int pebbleDark,
          int pebbleLight, int[] specks, Tree tree, int[] canopy, int trunk, int[] rock, Color trail) {
        this.ground = ground;
        this.road = road;
        this.kerb = kerb;
        this.lip = lip;
        this.tuftDark = tuftDark;
        this.tuftLight = tuftLight;
        this.pebbleDark = pebbleDark;
        this.pebbleLight = pebbleLight;
        this.specks = specks;
        this.tree = tree;
        this.canopy = canopy;
        this.trunk = trunk;
        this.rock = rock;
        this.trail = trail;
    }
}
