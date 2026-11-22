package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * All art, generated in code at startup: monsters and towers ({@link TowerArt}) from ASCII pixel
 * maps, terrain from primitives on a small canvas, then scaled up with nearest-neighbour for a
 * pixel-art look.
 */
public final class Assets {
    /** Towers and terrain are drawn at this tile size and doubled. */
    static final int ART = Level.TILE / 2;

    private final Map<MapDef, BufferedImage> backgrounds = new EnumMap<>(MapDef.class);
    /** Darkens the edges of the field; painted into every terrain. */
    private final BufferedImage vignette;
    /** Per monster: each animation frame, facing right and mirrored. Most monsters have one frame. */
    private final Map<EnemyType, BufferedImage[][]> enemies = new EnumMap<>(EnemyType.class);
    private final Map<EnemyType, BufferedImage[][]> silhouettes = new EnumMap<>(EnemyType.class);
    private final Map<TowerType, BufferedImage[]> bases = new EnumMap<>(TowerType.class);
    private final Map<TowerType, BufferedImage> turrets = new EnumMap<>(TowerType.class);
    private final Map<TowerType, BufferedImage[]> icons = new EnumMap<>(TowerType.class);
    private final Map<TowerType, BufferedImage> smallIcons = new EnumMap<>(TowerType.class);

    public Assets() {
        for (EnemyType type : EnemyType.values()) {
            BufferedImage[] art = frameArt(type);
            BufferedImage[][] frames = new BufferedImage[art.length][], flashes = new BufferedImage[art.length][];
            for (int f = 0; f < art.length; f++) {
                BufferedImage image = scale(art[f], type.scale);
                BufferedImage white = silhouette(image);
                frames[f] = new BufferedImage[] {image, mirror(image)};
                flashes[f] = new BufferedImage[] {white, mirror(white)};
            }
            enemies.put(type, frames);
            silhouettes.put(type, flashes);
        }
        for (TowerType type : TowerType.values()) {
            BufferedImage turret = TowerArt.turret(type);
            BufferedImage[] tierBases = new BufferedImage[TowerType.MAX_TIER];
            BufferedImage[] tierIcons = new BufferedImage[TowerType.MAX_TIER];
            for (int tier = 1; tier <= TowerType.MAX_TIER; tier++) {
                BufferedImage base = TowerArt.base(type, tier);
                BufferedImage icon = new BufferedImage(ART, ART, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = icon.createGraphics();
                g.drawImage(base, 0, 0, null);
                g.drawImage(turret, 0, 0, null);
                g.dispose();
                if (tier == 1) smallIcons.put(type, icon);
                tierBases[tier - 1] = scale(base, 2);
                tierIcons[tier - 1] = scale(icon, 2);
            }
            bases.put(type, tierBases);
            icons.put(type, tierIcons);
            turrets.put(type, scale(turret, 2));
        }
        vignette = vignette();
    }

    /** The painted terrain of a map, built the first time it is asked for. */
    public BufferedImage background(MapDef map) {
        return backgrounds.computeIfAbsent(map, m -> {
            BufferedImage terrain = paintTerrain(new Level(m));
            Graphics2D g = terrain.createGraphics();
            g.drawImage(vignette, 0, 0, null);
            g.dispose();
            return terrain;
        });
    }

    /** The monster's portrait: its first frame. Index 0 faces right, index 1 is mirrored. */
    public BufferedImage[] enemy(EnemyType type) {
        return enemies.get(type)[0];
    }

    /**
     * How many drawn frames the monster cycles through. Most have one and are brought to life by
     * squashing it; the Dragon has two, for its wingbeat.
     */
    public int enemyFrames(EnemyType type) {
        return enemies.get(type).length;
    }

    /** One frame of the monster. Index 0 faces right, index 1 is mirrored. */
    public BufferedImage[] enemy(EnemyType type, int frame) {
        return enemies.get(type)[frame];
    }

    /** All-white copies of a frame, drawn over it as a hit flash. */
    public BufferedImage[] enemySilhouette(EnemyType type, int frame) {
        return silhouettes.get(type)[frame];
    }

    public BufferedImage base(TowerType type, int tier) {
        return bases.get(type)[tier - 1];
    }

    public BufferedImage turret(TowerType type) {
        return turrets.get(type);
    }

    /** Full-tile picture of the whole tower. */
    public BufferedImage icon(TowerType type, int tier) {
        return icons.get(type)[tier - 1];
    }

    public BufferedImage smallIcon(TowerType type) {
        return smallIcons.get(type);
    }

    // ---- monsters ------------------------------------------------------------------------

    /** The frames a monster is drawn with, in order. */
    private static BufferedImage[] frameArt(EnemyType type) {
        if (type == EnemyType.DRAGON) return new BufferedImage[] {dragon(true), dragon(false)};
        return new BufferedImage[] {enemyArt(type)};
    }

    private static BufferedImage enemyArt(EnemyType type) {
        return switch (type) {
            case SLIME, MINI_SLIME -> pixelArt(new String[] {
                "................",
                "................",
                "................",
                "................",
                ".....oooooo.....",
                "...oogggggGoo...",
                "..ogggggggggGo..",
                ".ogghgggggggGGo.",
                ".oghgggggggggGo.",
                "ogggwwgggwwggGGo",
                "ogggwkgggwkggGGo",
                "oggggggggggggGGo",
                "oggggmmmmmgggGGo",
                "oGggggggggggGGGo",
                ".oGGGGGGGGGGGGo.",
                "..oooooooooooo..",
            }, "ogGhwkm", 0x1F5C1F, 0x5BBF4A, 0x3E9A38, 0xB8F0A0, 0xFFFFFF, 0x102010, 0x2A6B2A);
            case GOBLIN -> pixelArt(new String[] {
                "................",
                "................",
                ".....oooooo.....",
                ".o..oggggggo..o.",
                ".ogoggggggggogo.",
                ".ogggwkggwkgggo.",
                "..oggggggggggo..",
                "...ogggmmgggo...",
                "....oggggggo....",
                "...oobbbbbboo...",
                "..ogobbbbbbogo..",
                "..ogobbyybbogo..",
                "..oo.obbbbo.oo..",
                ".....obooobo....",
                "....ogo..ogo....",
                "....ooo..ooo....",
            }, "ogwkmby", 0x243B14, 0x8BC34A, 0xFFFFFF, 0x101010, 0x3B1F1F, 0x7A4B24, 0xE0B040);
            case ORC -> pixelArt(new String[] {
                "................",
                "....oooooooo....",
                "...oggggggggo...",
                "..oggggggggggo..",
                "..ogrkggggrkgo..",
                "..oggggggggggo..",
                "..ogwgmmmmgwgo..",
                "...oggggggggo...",
                ".oossssssssssoo.",
                "ogosssSssSsssogo",
                "ogossssssssssogo",
                "ogosssSssSsssogo",
                ".oo.obbbbbbo.oo.",
                "....obbo.obbo...",
                "....obbo.obbo...",
                "....oooo.oooo...",
            }, "ogrkwmsSb", 0x1B2E14, 0x4E8F3A, 0xE23B3B, 0x101010, 0xF2F2E0, 0x2A1A1A, 0x6B5A48,
                    0x9C8A70, 0x4A3A2C);
            case BAT -> pixelArt(new String[] {
                "................",
                "................",
                "................",
                ".o....o..o....o.",
                "opo...oooo...opo",
                "oppo.oppppo.oppo",
                "opppoppppppopppo",
                "opppprpppprppppo",
                "oppppppwwppppppo",
                ".oppppppppppppo.",
                ".op.oppppppo.po.",
                ".o...oppppo...o.",
                "......oppo......",
                ".......oo.......",
                "................",
                "................",
            }, "oprw", 0x2A1846, 0x7C4DBE, 0xFF4040, 0xFFFFFF);
            case SHAMAN -> pixelArt(new String[] {
                "..............y.",
                "....oooooo...yYy",
                "...owwwwwwo...y.",
                "...owkwwkwo...s.",
                "...owwwwwwo...s.",
                "....owkkwo....s.",
                "...oobbbboo...s.",
                "..obbbbbbbbo..s.",
                ".obbbBbbBbbbooso",
                ".obbbBbbBbbbbgso",
                ".oobbBbbBbboo.s.",
                "..obbBbbBbbo..s.",
                "..obbbbbbbbo..s.",
                "..obbbbbbbbo..s.",
                ".obbbbbbbbbbo.s.",
                ".oooooooooooo.s.",
            }, "owkbBsyYg", 0x16204A, 0xE8E0D0, 0x101010, 0x3F6FD8, 0x2B4FA8, 0x7A4B24, 0xFFE14A,
                    0xFFF6B0, 0x8BC34A);
            // A hooded spectre: an empty cowl with two cold lights, trailing rags.
            case WRAITH -> pixelArt(new String[] {
                ".......ooo......",
                "......oCCCo.....",
                ".....oCcccCo....",
                "....oCckkkcCo...",
                "....oCkkkkkDo...",
                "...oCckekekcCo..",
                "...oCckkkkkcDo..",
                "...oCckkkkkcDo..",
                "..oCcccckccccCo.",
                ".oCcccccccccccCo",
                "oCDcccccccccccDC",
                "bboCccccccDccDbb",
                "...oCccDcDoCDo..",
                "....oCDoCDooo...",
                "....oCooCo......",
                ".....o..o.......",
            }, "ocCDkeb", 0xB0204058, 0xD0BFE3EE, 0xE6E6F7FB, 0xC88FB8CC, 0xF00B1420, 0x67E8F9, 0xF0F2EEDD);
            case TROLL -> pixelArt(new String[] {
                "................",
                "....oooooo......",
                "...oMMMMMMo.....",
                "..ogggggggGo.oo.",
                "..ogykggykGoobBo",
                "..oggggggGGoobBo",
                "..ogwmmmmwGo.obo",
                "...oggggGGo..obo",
                ".ooggggggGGooobo",
                "oggggggggGGGggbo",
                "ogGoggggGGGoggo.",
                "ogGoggggGGGo.o..",
                ".oo.ollllllo....",
                "....oggoogGo....",
                "...ogggoogGGo...",
                "...oooo.ooooo...",
            }, "ogGMykwmbBl", 0x16261A, 0x6FA07A, 0x4E7A5C, 0x3F7D3A, 0xFDE047, 0x101010, 0xF2F2E0,
                    0x2A1A1A, 0x7A4B24, 0xA06A3A, 0x8B5E34);
            case KNIGHT -> pixelArt(new String[] {
                ".......pp.......",
                "......oppo......",
                "....ooSSSsoo....",
                "...oSSSssssdo...",
                "...oSkkkkkkdo...",
                "...oSkekkekdo...",
                "...oSSsssssdo...",
                "....oSsvsdoo....",
                ".ooossSvVsssooo.",
                "oVvosSvVVvsdosSo",
                "ovVosSsvVssdosso",
                "oVvossSvVsddosso",
                ".oo.osssssdo.oo.",
                "....osso.osdo...",
                "....osso.osdo...",
                "....oooo.oooo...",
            }, "osSdvVkpe", 0x161426, 0x8E96AD, 0xC3CAD9, 0x5A6178, 0x8B5CF6, 0xDDD6FE, 0x0B0B14,
                    0x6D5BD0, 0x67E8F9);
            // Stone wings arched high over a crouched, horned body.
            case GARGOYLE -> pixelArt(new String[] {
                "..o..o....o..o..",
                ".omooLo..oLoomo.",
                "omMmoLooooLomMmo",
                "ommMmoLLLLomMmmo",
                "ommmmmessemmmmmo",
                "ommmMmssssmMmmmo",
                ".ommmMswwsMmmmo.",
                ".ommmmmssmmmmmo.",
                ".ommmmssssmmmmo.",
                "..oomssssssmoo..",
                "...omssddssmo...",
                "....oLsddsdo....",
                "...oLssddssLo...",
                "...oLsdooLsdo...",
                "..oLdddooLddLo..",
                "...oooo..oooo...",
            }, "osLdmMew", 0x1C2028, 0x7B8494, 0xA5AEBD, 0x566070, 0x3F4756, 0x5A6475, 0xF59E0B, 0xF1F5F9);
            case DRUMMER -> pixelArt(new String[] {
                "................",
                ".....oooooo.....",
                ".o..oRRRRrro..o.",
                ".oooRRRRRRrrooo.",
                "..orRwkRRwkrro..",
                "...oRRRRRRrro...",
                "s...oRmmmmro...s",
                ".s...oooooo...s.",
                "..sooDDDDDDoos..",
                "..orobbbbbboro..",
                "..orodDDDDdoro..",
                "...oodDDDDdoo...",
                "....obbbbbbo....",
                ".....orooro.....",
                "....oro..oro....",
                "....ooo..ooo....",
            }, "orRwkmsdDb", 0x2B160C, 0xC2562B, 0xE07B4A, 0xFFFFFF, 0x101010, 0x3B1F1F, 0xE8D5A8,
                    0x8B5E34, 0xF2E6C8, 0xFACC15);
            case NECROMANCER -> pixelArt(new String[] {
                ".g.....oo.......",
                "gGg...oPPo......",
                ".g...oPPPpo.....",
                ".s..oPPPPPpo....",
                ".s..oPbbbbpo....",
                ".s..oPgbbgpo....",
                ".s..oPbkkbpo....",
                ".so.oPPbbppo....",
                ".sboPPPPPPppo...",
                ".soPPPPPPPpppo..",
                ".s.oPPPdPPpppo..",
                ".s.oPPPdPPpppo..",
                ".s.oPPdPPPpppo..",
                ".s.oPPdPPppppo..",
                ".soPPPPPPPppppo.",
                ".sooooooooooooo.",
            }, "opPdbkgGs", 0x150A1F, 0x5B2A86, 0x7E3FA8, 0x3B1A5A, 0xE8E0D0, 0x101010, 0x86EFAC,
                    0xECFDF5, 0x6B5A48);
            case SKELETON -> pixelArt(new String[] {
                "................",
                ".....oooooo.....",
                "....oBBBBBbo....",
                "....oBkBBkbo....",
                "....oBkBBkbo....",
                "....oBBBBbbo....",
                ".....obkbkbo....",
                "......oooo......",
                "..oo.obbbbo.oo..",
                ".obooBkBBkBoobo.",
                ".obo.obbbbo.obo.",
                ".oo..oBkkBo..oo.",
                ".....obbbbo.....",
                ".....obo.obo....",
                ".....obo.obo....",
                "....ooo..ooo....",
            }, "obBk", 0x1F1F1F, 0xD6CFBE, 0xFFFFFF, 0x101010);
            case DRAGON -> dragon(true);
            // A crowned skull over a tattered robe, a staff with a cold orb in one bony hand.
            case LICH -> pixelArt(new String[] {
                "........o.oyyo.o........",
                ".......oyoyyyyoyo...ooo.",
                ".......oyyyeeyyyo..oEEeo",
                ".......oyyyyyyyyo..oEeeo",
                "........oooBbnoo...oeeeo",
                "........oBBbbbBBo..oeeeo",
                "........oBkkbkkno...oso.",
                "........oBekbkeno...oso.",
                "........oBbbkbbno...oso.",
                "......ooooBbbbnoooo.oso.",
                ".....odVVokbknooVVdooso.",
                "....odvdvVvvvvVVvdvdoso.",
                "....oVvvdyyyyyyvdvvvVso.",
                "...oVvvdvvvyyvvvvdvvvso.",
                "..oVvvddvVveevvdvddvvso.",
                "..oVvdoVvVvvdvvdvdoVvsVo",
                ".oVvvdoVvVvvdvvdvdoobbb.",
                "..oVdoVvvVvvdvvdvdo.oso.",
                "..bbboVvvVvvdvvdvvVooso.",
                "..b.boVvvvvvdvvvvvdooso.",
                ".....oVvvdvvdvvdvvdooso.",
                ".....oVvdoVdoVdoVvdooso.",
                "......oVooVdoVdooVo.oso.",
                ".......o..oo.oo..o...o..",
            }, "obBnvVdyYeEks", 0x12091F, 0xD6CFBE, 0xFFFFFF, 0x9C947F, 0x5B21B6, 0x8B5CF6, 0x3B1578,
                    0xFBBF24, 0xFDE68A, 0x67E8F9, 0xECFEFF, 0x0B0B14, 0x6B5A48);
            // Three snake heads on long necks, fanning out of a coiled body.
            case HYDRA -> pixelArt(new String[] {
                "..............o.............",
                "............ooToo...........",
                "...........oTTtTTo..........",
                "...oooo....oTytydo....oooo..",
                "..oTTTTo...oTtttdo...oTTTTo.",
                ".ooTyttTo..oTwtwdo..oTttydoo",
                "oTTttttdo...oTptdo..oTttttTT",
                "pTttttdo....oTptdo...oTttttd",
                ".wTwdttTo....oTttTo.oTttdwdw",
                "..oooTttTo...oTbtdooTttdooo.",
                ".....oTttTo.oTbbdooTttdo....",
                ".....oTttdooTtbtdooTttdo....",
                ".....oTttdoTtbbdo.oTttdo....",
                "....oTttdooTtbtdo..oTttTo...",
                "...oTtttdooTbbdo...oTtttTo..",
                "...oTttdoooTtbtTo..ooTttdo..",
                "...oTtttTToTtbbdoooTTtttdo..",
                "....oTttttTttbbtTTTttttdoTo.",
                "....oTdtttttttttttttttddoTTo",
                ".....ooTtttttttttttttdoo.oTT",
                "......oTtttbbbbbbbtttdoooTtd",
                ".......oTtbbbbbbbbbtttTTTttd",
                "........oTdBbbbbbBdddtttttdo",
                ".........oooTddddooooTddddo.",
            }, "otTdykwbBp", 0x06302B, 0x1F9E89, 0x4FD1BB, 0x13705F, 0xFDE047, 0x101010, 0xFFFFFF, 0xB7F0C8,
                    0x7FD4A8, 0xF43F5E);
            // One small head on a short neck: what crawls out of a slain Hydra.
            case HYDRA_SPAWN -> pixelArt(new String[] {
                "................",
                "........ooo.....",
                "......ooTTTo....",
                ".....oTTytyTo...",
                ".....oTttttdo...",
                ".....oTttwtdo...",
                "....oTtttdpo....",
                "...oTtttdoo.....",
                "...oTttdo.......",
                "...oTttdo.......",
                "...oTtttToo...o.",
                "....oTtttTTo.oTo",
                "...oTttttttTooTT",
                "..oTtbbbbbbtTTdo",
                "...oTbbbbbbddddo",
                "....ooooooooooo.",
            }, "otTdykwbBp", 0x06302B, 0x1F9E89, 0x4FD1BB, 0x13705F, 0xFDE047, 0x101010, 0xFFFFFF, 0xB7F0C8,
                    0x7FD4A8, 0xF43F5E);
            // A knight behind a great round shield of gold, a red plume on the helm.
            case WARDEN -> pixelArt(new String[] {
                "....oo..........",
                "...orro.........",
                "..oaaaao........",
                ".oaaaaaAo.......",
                ".oakkekAo.......",
                ".oaaaaaAo.......",
                "..oaaaAo.oooo...",
                ".occaaAooGGGGo..",
                "occcaaaoGGggGGo.",
                "occcaaaoGgGGgGGo",
                "occcaaaoGgGrGgGo",
                "occcaaaoGgGGgGGo",
                ".occaaaoGGggGGo.",
                "..oaaAAooGGGGo..",
                "..oaAoaAo.oooo..",
                "..ooo.ooo.......",
            }, "oraAkecGg", 0x2A1E08, 0xDC2626, 0xA8B0C0, 0x6B7385, 0x101010, 0xFDE68A, 0x7A2E2E,
                    0xD4A72C, 0xFDE68A);
            // A grinning little horned imp on stubby wings.
            case IMP -> pixelArt(new String[] {
                "................",
                "...o......o.....",
                "...oho....oho...",
                "....oppppppo....",
                "...oppppppppo...",
                "...opypppyppo...",
                "...oppwwwwppo...",
                "....oppppppo....",
                ".ov.oppppppo.vo.",
                ".ovvopPPPPpovvo.",
                "..ovopPPPPpovo..",
                "...oopppppoo....",
                "....op..po......",
                "....op..po......",
                "...oop.oop......",
                "...ooo.ooo......",
            }, "ohpywvP", 0x3B0A2A, 0x4A044E, 0xE05AA8, 0xFDE047, 0xFFFFFF, 0xA21CAF, 0xF9A8D4);
            // A banded sand beetle with a drill for a horn.
            case BORER -> pixelArt(new String[] {
                "................",
                "................",
                "................",
                "................",
                "................",
                "......oooo......",
                "....ooSSSSoo....",
                "...oSSsSSsSSo...",
                "..oSsSSsSSsSSoo.",
                ".oSsSSsSSsSSoyDo",
                ".oSsSSsSSsSSodDD",
                ".oddddddddddoDdo",
                "..oddddddddddoo.",
                "..olo.olo.olo...",
                ".olo.olo.olo....",
                ".oo..oo..oo.....",
            }, "oSsdyDl", 0x2B1A0A, 0xC2915A, 0xE0B77A, 0x7A5530, 0xFDE047, 0x9CA3AF, 0x4A3420);
            // A shaggy white snow beast with a blue face and black claws.
            case YETI -> pixelArt(new String[] {
                "................",
                ".....oooooo.....",
                "....owwwwwwo....",
                "...owwwwwwwwo...",
                "...owwfffffwo...",
                "...owfkfffkfo...",
                "...owfmmmmfwo...",
                "..owwwffffwwwo..",
                ".owwwwwwwwwwwwo.",
                "owwWwwwwwwwwWwwo",
                "owWwowwwwwwowWwo",
                "owwwowwwwwwowwwo",
                ".okoowwwwwwooko.",
                "...owwWooWwwo...",
                "...owwwo.owwwo..",
                "...ooooo.ooooo..",
            }, "owWfkm", 0x1E2A3A, 0xF1F5F9, 0xBFD3E6, 0x7B9AB8, 0x0B1220, 0x1E3A5F);
            // A spotted fire lizard with flames licking along its back.
            case SALAMANDER -> pixelArt(new String[] {
                "................",
                "................",
                "................",
                "................",
                "................",
                "................",
                "......F...F.....",
                ".....FyF.FyF....",
                "....oooooooooo..",
                "...ossssssssssoo",
                "oo.osbsbsbsbssko",
                "osoosssssssssssm",
                ".ossossssssssoo.",
                "..oo.olo...olo..",
                ".....oo....oo...",
                "................",
            }, "osbkmFyl", 0x3B1205, 0xF97316, 0xFDE047, 0x101010, 0xB91C1C, 0xEF4444, 0xFDE047, 0xC2410C);
            // A hulk of mossy boulders with burning eyes and a molten crack down its chest.
            case GOLEM -> pixelArt(new String[] {
                "........................",
                ".........oooooo.........",
                "........LLLLLLLL........",
                ".......oLssssssSo.......",
                ".......oSSSSSSSSo.......",
                "..ooooooLyYssYySoooooo..",
                ".oLggLLoLssssssSoLggLLo.",
                ".oLsggsLSskkkksSLssggSo.",
                "oLsssssssSssssSsssssssLo",
                "oLsssssssSssssSsssssssSo",
                "oLsssssssSssysSsssssssSo",
                ".oLsssssSssysysSsssssSo.",
                ".oLssssSsssYssssSssssSo.",
                "oLsssSSsssysyssssSSsssLo",
                "oLssSoLsssssYssssSoLssSo",
                "oLssSoLssssysssssSoLssSo",
                "oLgssLoLsSssssSsSoLsssSo",
                "oLsssSoLsssSSsssSoLssgSo",
                "oLsssSoLssSooLssSoLsssSo",
                "oLSsSSoLsSSooLssSoLSsSSo",
                ".ooLoooLssSooLSsSoooLoo.",
                "...o.oLsssSooLsssLo.o...",
                ".....oLSSSSooLSSSSo.....",
                "......ooooo..ooooo......",
            }, "osLSyYkg", 0x1D2026, 0x8A8F98, 0xB4B9C2, 0x5F646D, 0xFFA020, 0xFFE08A, 0x15171C, 0x5E8C4A);
        };
    }

    /**
     * The Dragon, seen from the side and facing right, with its near wing raised or beaten down:
     * the two frames of its wingbeat.
     */
    private static BufferedImage dragon(boolean wingUp) {
        String[] rows = wingUp ? new String[] {
            "............o...................",
            "...........oro..................",
            "........oooMoo..o.....o..o......",
            ".......oMMMMorooDoo..owoowo.....",
            ".......omMMmmoo.oDDo..owowwo....",
            ".......oMmmmmorooDDDo..owowoo...",
            ".....ooMoMmmmmoooDDDDo.oRwekRoo.",
            "....oMomMmmmmmorooDDDo.oRrrrrRdo",
            ".oo.ommmmmmmmmorooDDDooRrrrrrrrR",
            "oddo.omMmmmmmmmoroDDDooRrrdddddd",
            ".oddooMommmmmmmoroDDDoRrrdowowow",
            ".oddooommmmmmmmmoroDoRrryYo.....",
            ".oRooMoooommmmmmorRrRrrrYo......",
            ".oRo.o..oooooooorrrrrrryYo......",
            ".oRRooooRRrrrrrrrrrrrryYo.......",
            ".oRrRRRRrrrrrrrrrrrryYYo........",
            "..oRrrrrrrrrryyyyrrrYoo.........",
            "...oRrrrddYyyyyyyyyYo...........",
            "....oRddoooyyYYYYyYo............",
            ".....ooo..oRdooooRdo............",
            ".........oRrdo..oRdo............",
            "..........www...owwo............",
            "...........o.....oo.............",
            "................................",
        } : new String[] {
            "................................",
            "................................",
            "......................o..o......",
            ".....................owoowo.....",
            "......................owowwo....",
            ".......................owowoo...",
            ".......................oRwekRoo.",
            ".......................oRrrrrRdo",
            ".oo...................oRrrrrrrrR",
            "oddo..................oRrrdddddd",
            ".oddo................oRrrdowowow",
            ".oddo..........ooooooRrryYo.....",
            ".oRoo.....oooooRrRRRRrrrYo......",
            ".oRoMooooommmmmorrrrrrryYo......",
            ".oRRoMmmmmmmmmmorrrrrryYo.......",
            ".oRrRommmmmmmmorrrrryYYo........",
            "..oRommmmmmmmmoyyrrrYoo.........",
            "...oMomMmmmmmoryyyyYo...........",
            "....ooMoMmmmorYYYyYo............",
            "......omMMmmoroooRdo............",
            "......oMMMMoro..oRdo............",
            ".......oooMoo...oRdo............",
            "..........o......oo.............",
            "................................",
        };
        return pixelArt(rows, "orRdmMDyYekw", 0x3B0A0A, 0xD9482B, 0xF47B4F, 0x9C2A17, 0x8E2318, 0xC7452C,
                0x5C1410, 0xFCD34D, 0xD9A21B, 0xFDE047, 0x140606, 0xFFF7ED);
    }

    /**
     * Builds an image from rows of characters. '.' is transparent; every other character is
     * looked up in {@code keys} to pick a colour. Colours without an alpha byte are opaque.
     */
    static BufferedImage pixelArt(String[] rows, String keys, int... colors) {
        int width = rows[0].length();
        BufferedImage image = new BufferedImage(width, rows.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != width) {
                throw new IllegalStateException("Sprite row " + y + " is " + rows[y].length() + " wide: " + rows[y]);
            }
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') continue;
                int key = keys.indexOf(c);
                if (key < 0) throw new IllegalStateException("Unknown sprite colour '" + c + "'");
                int argb = colors[key];
                image.setRGB(x, y, argb >>> 24 == 0 ? argb | 0xFF000000 : argb);
            }
        }
        return image;
    }

    /** Completes a picture that is symmetric left to right from the left half of each row. */
    static String[] symmetric(String... left) {
        String[] rows = new String[left.length];
        for (int i = 0; i < left.length; i++) rows[i] = left[i] + new StringBuilder(left[i]).reverse();
        return rows;
    }

    private static BufferedImage silhouette(BufferedImage source) {
        BufferedImage white = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                white.setRGB(x, y, source.getRGB(x, y) | 0x00FFFFFF);
            }
        }
        return white;
    }

    // ---- terrain -------------------------------------------------------------------------

    private static BufferedImage paintTerrain(Level level) {
        Theme theme = level.def.theme;
        int t = ART;
        int width = Level.COLS * t, height = Level.ROWS * t;
        BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42 + level.def.seed);
        float[][] groundNoise = noise(random, width, height, 14);
        float[][] roadNoise = noise(random, width, height, 7);

        // Ground: posterised noise gives soft patches instead of a checkerboard.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = x / t, r = y / t;
                int rgb;
                if (level.path[c][r]) {
                    rgb = theme.road[Math.min(theme.road.length - 1, (int) (roadNoise[x][y] * theme.road.length))];
                    int lx = x % t, ly = y % t;
                    // A dark kerb where road meets ground, with a lighter lip inside it.
                    int edge = Math.min(
                            Math.min(level.isPath(c - 1, r) ? t : lx, level.isPath(c + 1, r) ? t : t - 1 - lx),
                            Math.min(level.isPath(c, r - 1) ? t : ly, level.isPath(c, r + 1) ? t : t - 1 - ly));
                    if (edge <= 1) rgb = theme.kerb;
                    else if (edge == 2) rgb = theme.lip;
                } else {
                    rgb = theme.ground[Math.min(theme.ground.length - 1, (int) (groundNoise[x][y] * theme.ground.length))];
                }
                small.setRGB(x, y, rgb);
            }
        }

        Graphics2D g = small.createGraphics();
        for (int c = 0; c < Level.COLS; c++) {
            for (int r = 0; r < Level.ROWS; r++) {
                int x = c * t, y = r * t;
                if (level.path[c][r]) {
                    for (int i = 0; i < 3; i++) {
                        int px = x + 4 + random.nextInt(t - 9), py = y + 4 + random.nextInt(t - 9);
                        rect(g, theme.pebbleDark, px, py, 2, 1);
                        rect(g, theme.pebbleLight, px, py - 1, 1, 1);
                    }
                } else if (level.decor[c][r] == Level.CLEAR) {
                    for (int i = 0; i < 2; i++) {
                        // Tufts.
                        int px = x + 2 + random.nextInt(t - 6), py = y + 3 + random.nextInt(t - 6);
                        rect(g, theme.tuftDark, px, py, 1, 2);
                        rect(g, theme.tuftDark, px + 2, py, 1, 2);
                        rect(g, theme.tuftLight, px + 1, py - 1, 1, 2);
                    }
                    if (theme.specks.length > 0 && random.nextInt(7) == 0) {
                        int px = x + 3 + random.nextInt(t - 7), py = y + 3 + random.nextInt(t - 7);
                        int speck = theme.specks[random.nextInt(theme.specks.length)];
                        rect(g, speck, px - 1, py, 3, 1);
                        rect(g, speck, px, py - 1, 1, 3);
                    }
                }
            }
        }
        for (int r = 0; r < Level.ROWS; r++) {
            for (int c = 0; c < Level.COLS; c++) {
                int x = c * t, y = r * t;
                if (level.decor[c][r] == Level.TREE) {
                    g.setColor(new Color(0, 0, 0, 70));
                    g.fillOval(x + 2, y + 15, 21, 8);
                    tree(g, theme, x, y);
                } else if (level.decor[c][r] == Level.ROCK) {
                    g.setColor(new Color(0, 0, 0, 70));
                    g.fillOval(x + 2, y + 14, 21, 8);
                    oval(g, theme.rock[0], x + 3, y + 6, 18, 15);
                    oval(g, theme.rock[1], x + 4, y + 6, 15, 12);
                    oval(g, theme.rock[2], x + 5, y + 6, 10, 7);
                    rect(g, theme.rock[3], x + 8, y + 8, 3, 1);
                    rect(g, theme.rock[0], x + 13, y + 12, 1, 4);
                }
            }
        }

        // A dotted trail marks the straight line flyers take from the cave to the keep.
        g.setColor(theme.trail);
        float fx0 = level.flyX[0] / 2, fy0 = level.flyY[0] / 2, fx1 = level.flyX[1] / 2, fy1 = level.flyY[1] / 2;
        int dots = (int) (Math.hypot(fx1 - fx0, fy1 - fy0) / 8);
        for (int i = 2; i < dots - 3; i++) {
            float along = i / (float) dots;
            g.fillRect(Math.round(fx0 + (fx1 - fx0) * along), Math.round(fy0 + (fy1 - fy0) * along), 2, 1);
        }

        // The monster cave where the road enters, and the keep it leads to.
        int entryY = (int) (level.pathY[0] / Level.TILE) * t;
        oval(g, 0x2A1F3D, -12, entryY - 4, 26, 32);
        oval(g, 0x4B3470, -11, entryY - 2, 22, 28);
        oval(g, 0x0F0919, -10, entryY + 2, 18, 20);
        int exitY = (int) (level.pathY[level.pathY.length - 1] / Level.TILE) * t;
        int bx = (Level.COLS - 1) * t + 6;
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(bx - 4, exitY + 24, 26, 10);
        rect(g, 0x3F4753, bx, exitY - 12, 18, 46);
        rect(g, 0x6B7280, bx + 2, exitY - 10, 16, 42);
        rect(g, 0x858D9B, bx + 2, exitY - 10, 3, 42);
        for (int i = 0; i < 3; i++) {
            rect(g, 0x3F4753, bx + i * 7, exitY - 17, 5, 6);
            rect(g, 0x6B7280, bx + 1 + i * 7, exitY - 16, 3, 4);
        }
        for (int i = 0; i < 4; i++) rect(g, 0x565E6B, bx + 5 + (i % 2) * 6, exitY - 6 + i * 9, 4, 2);
        oval(g, 0x14171D, bx - 5, exitY + 2, 16, 12);
        rect(g, 0x14171D, bx, exitY + 8, 11, 14);
        rect(g, 0x2A2F38, bx + 3, exitY + 6, 1, 16);
        rect(g, 0x2A2F38, bx + 7, exitY + 6, 1, 16);
        rect(g, 0x2B303A, bx + 8, exitY - 28, 1, 12);
        rect(g, 0xDC2626, bx + 9, exitY - 28, 8, 3);
        rect(g, 0xB91C1C, bx + 9, exitY - 25, 6, 3);
        g.dispose();
        return scale(small, 2);
    }

    /** One piece of tree-like scenery in the theme's style, within the tile at (x, y). */
    private static void tree(Graphics2D g, Theme theme, int x, int y) {
        int[] c = theme.canopy;
        switch (theme.tree) {
            case ROUND -> {
                rect(g, theme.trunk, x + 10, y + 12, 4, 9);
                oval(g, c[0], x + 1, y, 22, 17);
                oval(g, c[1], x + 2, y + 1, 19, 14);
                oval(g, c[2], x + 4, y + 1, 13, 10);
                oval(g, c[3], x + 6, y + 2, 7, 5);
            }
            case CACTUS -> {
                rect(g, c[0], x + 9, y + 4, 6, 17);
                rect(g, c[1], x + 10, y + 4, 4, 17);
                oval(g, c[1], x + 9, y + 2, 6, 5);
                rect(g, c[1], x + 5, y + 11, 5, 3);
                rect(g, c[1], x + 5, y + 7, 3, 6);
                rect(g, c[1], x + 14, y + 13, 5, 3);
                rect(g, c[1], x + 16, y + 9, 3, 6);
                rect(g, c[3], x + 11, y + 4, 1, 16);
                rect(g, c[3], x + 6, y + 8, 1, 4);
                rect(g, c[3], x + 17, y + 10, 1, 4);
            }
            case PINE -> {
                rect(g, theme.trunk, x + 11, y + 17, 2, 4);
                g.setColor(new Color(c[0]));
                g.fillPolygon(new int[] {x + 3, x + 12, x + 21}, new int[] {y + 19, y + 9, y + 19}, 3);
                g.setColor(new Color(c[1]));
                g.fillPolygon(new int[] {x + 5, x + 12, x + 19}, new int[] {y + 14, y + 5, y + 14}, 3);
                g.setColor(new Color(c[2]));
                g.fillPolygon(new int[] {x + 7, x + 12, x + 17}, new int[] {y + 9, y + 1, y + 9}, 3);
                rect(g, c[3], x + 11, y + 2, 2, 2);
                rect(g, c[3], x + 9, y + 7, 3, 1);
                rect(g, c[3], x + 13, y + 12, 4, 1);
                rect(g, c[3], x + 6, y + 17, 4, 1);
            }
            case DEAD -> {
                rect(g, c[0], x + 10, y + 7, 4, 14);
                rect(g, c[1], x + 11, y + 7, 2, 14);
                g.setColor(new Color(c[1]));
                g.drawLine(x + 11, y + 12, x + 6, y + 6);
                g.drawLine(x + 12, y + 10, x + 18, y + 4);
                g.drawLine(x + 12, y + 15, x + 17, y + 12);
                g.drawLine(x + 6, y + 6, x + 5, y + 3);
                g.drawLine(x + 18, y + 4, x + 20, y + 5);
            }
        }
    }

    /** Smooth value noise in 0..1, interpolated from a coarse random lattice. */
    private static float[][] noise(Random random, int width, int height, int cell) {
        int gw = width / cell + 2, gh = height / cell + 2;
        float[][] lattice = new float[gw][gh];
        for (float[] column : lattice) {
            for (int j = 0; j < gh; j++) column[j] = random.nextFloat();
        }
        float[][] out = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int gx = x / cell, gy = y / cell;
                float fx = smooth((x % cell) / (float) cell), fy = smooth((y % cell) / (float) cell);
                float top = lattice[gx][gy] + (lattice[gx + 1][gy] - lattice[gx][gy]) * fx;
                float bottom = lattice[gx][gy + 1] + (lattice[gx + 1][gy + 1] - lattice[gx][gy + 1]) * fx;
                // A little per-pixel grain keeps the patch borders from looking like contours.
                out[x][y] = Math.clamp(top + (bottom - top) * fy + (random.nextFloat() - 0.5f) * 0.12f, 0f, 0.999f);
            }
        }
        return out;
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static BufferedImage vignette() {
        BufferedImage image = new BufferedImage(Level.WIDTH, Level.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        float radius = (float) Math.hypot(Level.WIDTH, Level.HEIGHT) / 2;
        g.setPaint(new RadialGradientPaint(Level.WIDTH / 2f, Level.HEIGHT / 2f, radius,
                new float[] {0f, 0.62f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 10, 20, 120)}));
        g.fillRect(0, 0, Level.WIDTH, Level.HEIGHT);
        g.dispose();
        return image;
    }

    // ---- helpers -------------------------------------------------------------------------

    static void rect(Graphics2D g, int rgb, int x, int y, int w, int h) {
        g.setColor(new Color(rgb));
        g.fillRect(x, y, w, h);
    }

    static void oval(Graphics2D g, int rgb, int x, int y, int w, int h) {
        g.setColor(new Color(rgb));
        g.fillOval(x, y, w, h);
    }

    static void fillRound(Graphics2D g, int rgb, int x, int y, int w, int h, int arc) {
        g.setColor(new Color(rgb));
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    private static BufferedImage scale(BufferedImage source, float factor) {
        int w = Math.round(source.getWidth() * factor), h = Math.round(source.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(w, h, source.getType());
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage mirror(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage mirrored = new BufferedImage(w, h, source.getType());
        Graphics2D g = mirrored.createGraphics();
        g.drawImage(source, w, 0, -w, h, null);
        g.dispose();
        return mirrored;
    }
}
