package game;

import static game.Assets.ART;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Tower sprites, one hand-drawn pixel map per tower kind on the {@link Assets#ART} canvas.
 * Turrets that turn are seen from above pointing right, and are symmetric about the barrel line
 * so only their top half is written out. Towers that stand still are seen from the front; the
 * ones that are symmetric left to right are written as left halves ({@link Assets#symmetric}).
 */
final class TowerArt {
    /** Platform rim colour per tier: stone, bronze, silver, gold. */
    private static final int[] TIER_RIM = {0x5B6472, 0xB87333, 0xC9D1DC, 0xF5C542};
    private static final int[] TIER_RIM_DARK = {0x2B303A, 0x6B3F16, 0x6B7280, 0x92660A};
    private static final String BLANK = "........................";

    private TowerArt() {}

    /**
     * The platform a tower sits on. Its rim shows the tier and its trim shows the tower's parents;
     * turning turrets get a turntable, standing towers a flat dais.
     */
    static BufferedImage base(TowerType type, int tier) {
        BufferedImage image = new BufferedImage(ART, ART, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Assets.fillRound(g, TIER_RIM_DARK[tier - 1], 1, 1, 22, 22, 6);
        Assets.fillRound(g, TIER_RIM[tier - 1], 2, 2, 20, 19, 5);
        Assets.fillRound(g, 0x6E7786, 3, 3, 18, 17, 4);
        Assets.fillRound(g, 0x8891A0, 3, 3, 18, 15, 4);
        Assets.rect(g, 0xA7AFBC, 5, 4, 14, 1);
        g.setColor(type.color);
        g.fillRect(4, 19, 8, 2);
        g.setColor(type.colorB);
        g.fillRect(12, 19, 8, 2);
        if (type.rotates) {
            Assets.oval(g, 0x3A414D, 4, 4, 16, 16);
            Assets.oval(g, 0x596273, 5, 5, 14, 14);
            Assets.oval(g, 0x4A5361, 6, 7, 12, 12);
            Assets.oval(g, 0x535C6B, 6, 6, 12, 12);
        } else {
            Assets.fillRound(g, 0x6E7786, 5, 6, 14, 13, 4);
            Assets.fillRound(g, 0x9AA3B2, 5, 5, 14, 13, 4);
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(4, 15, 16, 5);
        }
        if (tier >= 3) {
            // Studs at the corners for the upper tiers.
            int stud = tier == 4 ? 0xFFF3B0 : 0xFFFFFF;
            Assets.rect(g, stud, 3, 3, 2, 2);
            Assets.rect(g, stud, 19, 3, 2, 2);
            Assets.rect(g, stud, 3, 16, 2, 2);
            Assets.rect(g, stud, 19, 16, 2, 2);
        }
        g.dispose();
        return image;
    }

    /** The tower itself on a transparent canvas. Turning turrets rotate about the centre. */
    static BufferedImage turret(TowerType type) {
        return switch (type) {
            // A wooden ballista: swept-back limbs, a drawn string and a steel-tipped bolt.
            case ARROW -> Assets.pixelArt(mirrored(
                BLANK,
                "..........oo............",
                ".........oWbo...........",
                "........s.oWbo..........",
                "........s.oWbo..........",
                ".......s...oWbo.........",
                ".......s....oWbo........",
                "......s.....oWbo........",
                "......s......oWbo.......",
                "...oosooooooooWbo..oo...",
                "..owwswwwwwwwwoWboottoo.",
                "..oisfffffffffffffftttTo"
            ), "oWbwsftTi", 0x2E1A0B, 0xC8934A, 0x9A6A35, 0x6B4423, 0xE5E7EB, 0xE8D5A8, 0xCBD5E1,
                    0xFFFFFF, 0x4B5563);
            // A squat iron mortar, tilted up so the bore shows inside its brass muzzle.
            case CANNON -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                "......oooooo............",
                ".....oddddddoooooRRRo...",
                "....odmmmmmmddddRRrrRo..",
                "...odmmllllmmdmmRrbbrRo.",
                "...odmllllllmdmmRbbbbRo.",
                "..odmmlllhhlmdllRbbbbRo.",
                "..odmlllhhhlmdhhRbbbbRo."
            ), "odmlhRrb", 0x0B1220, 0x1F2937, 0x374151, 0x4B5563, 0x8891A0, 0xE0A030, 0x92400E,
                    0x05070C);
            // A viper's head: broad venom glands tapering to the snout, diamond markings, fangs and tongue.
            case VENOM -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                ".........ooo............",
                "........oGGGo...........",
                ".......oGGLLGGo.........",
                "......oGGLLLLeeGo.......",
                ".ooooooGGLLLLekLLGo.....",
                "ogGGGGGGLLLLLLLLLLGo....",
                "oGGLLLLLLgLLgLLgLLLfo..t",
                "oGLLLLLLggLggLggLLLLLot."
            ), "ogGLyekft", 0x14260A, 0x3F6212, 0x65A30D, 0x84CC16, 0xECFCCB, 0xFDE047, 0x111111,
                    0xFFFFFF, 0xF43F5E);
            // Twin anti-air barrels with muzzle brakes on an olive hull, ammo box between them.
            case FLAK -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                ".....oooooooo...........",
                "....oaAAAAAAao.....ooo..",
                "...oaALLLLLAAaooooosSso.",
                "...oaALLLLLAAaSSSSSSkSo.",
                "...oaALLLLLAAasssssSkSo.",
                "...oaALaaaaLAaooooossso.",
                "...oaAaLLLLaAaoooo.ooo..",
                "...oaAaLhhLaAaobBbo....."
            ), "oaALhsSkbB", 0x161D0C, 0x4B5A2A, 0x6B7F3A, 0x8FA552, 0xB7C97A, 0x4B5563, 0xA7B0BE,
                    0x111827, 0xB5803A, 0xF2C14E);
            // A long ice lance clamped in a steel hub, flanked by two forward-swept ice prongs.
            case ICE_LANCE -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                "................oo......",
                "..............ooIwo.....",
                "............ooiIIo......",
                "..........ooiiIo........",
                "........ooiiIo..........",
                ".....ooodiIo............",
                "..ooosSsdoooooooooooo...",
                ".oiiisSsdIIIIIIIIIIIwo..",
                "oIIIIsSsdwwwwwwwwwwwWWo."
            ), "odiIwWsS", 0x0C4A6E, 0x0369A1, 0x38BDF8, 0x7DD3FC, 0xE0F2FE, 0xFFFFFF, 0x475569,
                    0xA7B0BE);
            // Two copper-banded rails with a charge crackling between them, fed by a capacitor.
            case RAILGUN -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                "..ooooooo...............",
                ".odmmmmmdo..............",
                ".omlllllmoooooooooooooo.",
                ".omlyyylmdhhChhhChhhCho.",
                ".omlyyylmdmmcmmmcmmmcmo.",
                ".omlllllmoooooooooooooo.",
                ".omldddlmoyyyyyyyyy.....",
                ".omldWdlmoYWWWWWWWWYy..."
            ), "odmlhcCyYW", 0x0B1220, 0x1F2937, 0x374151, 0x4B5563, 0x9CA3AF, 0x92400E, 0xF59E0B,
                    0xFACC15, 0xFEF08A, 0xFFFFFF);
            // A vine-wood bow with hooked tips, loosing a fan of three poison-tipped bolts.
            case BLIGHT -> Assets.pixelArt(mirrored(
                BLANK,
                ".............oooo.......",
                "............oVVVVo......",
                "............oVvooo..oTTo",
                "............oVvo...ottTo",
                "............oVvo.osstto.",
                "............oVvosso.....",
                "............oVsso.......",
                "...........ossvo........",
                "..oooooooossoVvo........",
                "..obbbbbssbboVvo...oto..",
                "..obssssssssssssssssttTo"
            ), "oVvbstT", 0x14260A, 0x65A30D, 0x3F6212, 0x7A4B24, 0xE8D5A8, 0xA3E635, 0xECFCCB);
            // A heavy gilded arbalest: capped straight limbs and a blazing sun disc at the cross.
            case SUNBOW -> Assets.pixelArt(mirrored(
                BLANK,
                "...........ooooooo......",
                "..........oGGYYYGGo.....",
                "...........ooGYgoo......",
                "..........w.oGYgo.......",
                "........ww..oGYgo.......",
                ".......w....oGYgoo......",
                ".....ww....oGGGGGGo.....",
                "...ww.....oGrRRRRrGo....",
                ".oooooooooGrRYYYYRrGo...",
                ".osSSSSSSoGrRYWWYRrGoRo.",
                ".osGGGGGGGGrRYWWYRrGYWYo"
            ), "ogGYWrRsSw", 0x451A03, 0xB45309, 0xF59E0B, 0xFDE047, 0xFFFBEB, 0xC2410C, 0xFB923C,
                    0x5C2D0C, 0x92400E, 0xFDE68A);
            // A frozen ball bristling with icicles, with a frost-rimmed stub of a barrel.
            case BLIZZARD -> Assets.pixelArt(mirrored(
                BLANK,
                "...o............o.......",
                "..oWo..........oWo......",
                "..oIIo........oIIo......",
                "...oIwo......owIo.......",
                "....oIwo....owIo........",
                ".....oIwoooowIo....ooo..",
                ".....odiiiiiidooooowWwo.",
                "....odiIIIIIIidSSSSwWwo.",
                "....odiIwwwwIidsssswbwo.",
                ".ooodiIwwWWwIiidssswbwo.",
                "owIIdiIwWWWwIiidSSSwbwo."
            ), "odiIwWsSb", 0x0C4A6E, 0x0369A1, 0x38BDF8, 0x7DD3FC, 0xE0F2FE, 0xFFFFFF, 0x64748B,
                    0xA7B0BE, 0x082F49);
            // A purple reactor whose two claws hold a ball of plasma out in front.
            case PLASMA -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                "..........ooooooooo.....",
                ".....oooooSSSSSSSSSoo...",
                "...oopPPPpossssssssSSo..",
                "..opPLLLLPpooooommmmsSo.",
                "..opPLyyLPpo...mMMMMmso.",
                ".opPLyMMyLPpooomMWWMmo..",
                ".opPLyMWyLPpPLPMWWWWMm.."
            ), "opPLyMWmsS", 0x1E0A3C, 0x581C87, 0x7E22CE, 0xA855F7, 0xFACC15, 0xF0ABFC, 0xFFFFFF,
                    0xD946EF, 0x4B5563, 0xA7B0BE);
            // A glass tank of acid feeding a flared brass funnel.
            case ACID -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                ".....oooooo.........oo..",
                "....oGGGGGGo.......oBYo.",
                "...oGGLLLLGGo.....oBBYo.",
                "..oGGLyyLLLLGo...obBkYo.",
                "..oGLyWyLLLLGoooobBkkYo.",
                ".oGLLyyLLLLyLGbBbBBkkYo.",
                ".oGLLLLLLLLLLGBYBYYkLYo."
            ), "oGLyWbBYk", 0x14260A, 0x4D7C0F, 0x84CC16, 0xD9F99D, 0xFFFFFF, 0x78350F, 0xB45309,
                    0xFBBF24, 0x0F1A03);
            // Three launch tubes side by side, a bomb nosing out of each.
            case CLUSTER -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                "..ooooooooooooooooo.....",
                ".odmmmmmmdhhhhhhhhkRo...",
                ".omlllllmdllllllllkRYo..",
                ".omlrrrlmdmmmmmmmmkRYo..",
                ".omlllllmdddddddddkRo...",
                ".omlllllmoooooooooooo...",
                ".omlhhhlmooooooooooooo..",
                ".omlhYhlmdhhhhhhhhhkRRo.",
                ".omlhYhlmdlllllllllkRYo."
            ), "odmlhrkRY", 0x0B1220, 0x1F2937, 0x374151, 0x4B5563, 0x7C8696, 0xDC2626, 0x05070C,
                    0xEA580C, 0xFDE047);
            // A cluster of three ice shards.
            case FROST -> Assets.pixelArt(new String[] {
                BLANK,
                "...........oo...........",
                "..........oWIo..........",
                "..........oWIio.........",
                ".........oWwIio.........",
                ".........oWwIido........",
                ".........owIIido..oo....",
                ".........owIIido.oWio...",
                "....oo...owIIidooWIido..",
                "...oWIo..owIIidowIiido..",
                "..oWwIio.owIIidowIiido..",
                "..oWwIidowwIIidowIiido..",
                "..owIIidowIIIidowIiido..",
                "..owIIidowIIiidowIiido..",
                "..owIIidoIIIiidoIIiido..",
                "..oIIIidoIIIiidoIIiido..",
                "..oIIiidoIIiiidoIiiido..",
                "..oIiiidoIiiiddoIiiddo..",
                "..oooooooooooooooooooo..",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "odiIwW", 0x0C4A6E, 0x0284C7, 0x38BDF8, 0x7DD3FC, 0xBAE6FD, 0xFFFFFF);
            // The classic coil: a copper winding under a polished ball throwing sparks.
            case TESLA -> Assets.pixelArt(new String[] {
                "..........oooo..........",
                "...y....oohhhhoo....y...",
                "..y....ohWWhhSSSo....y..",
                "...y...ohWhhSSSSo...y...",
                "......ohhhSSSSSSmo......",
                ".y....oSSSSSSSSmmo....y.",
                "..y....oSSSSSSmmo....y..",
                ".......oSSSSmmmmo.......",
                "........oommmmoo........",
                ".........oCYCco.........",
                ".........occcco.........",
                ".........oCYCco.........",
                ".........occcco.........",
                ".........oCYCco.........",
                ".........occcco.........",
                ".........oCYCco.........",
                ".......oooooooooo.......",
                "......oSSSSSSSSmmo......",
                "......ommmmmmmmddo......",
                ".......oooooooooo.......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oShWmdCYcy", 0x111827, 0x8793AA, 0xD9DEE6, 0xFFFFFF, 0x56617A, 0x374151, 0xD97706,
                    0xFCD34D, 0x92400E, 0xFDE047);
            // A golden sun: a burning lens in a ring of rays, on a stand.
            case PRISM -> Assets.pixelArt(new String[] {
                "...........YY...........",
                "....Y......YY......Y....",
                ".....Y...oooooo...Y.....",
                ".......ooGGGGGGoo.......",
                "......oGGGrrrrGGGo......",
                "......oGGrRRRRrGGo......",
                ".....oGGrRYYYYRrGGo.....",
                ".YY..oGGrRwWWwRrGGo..YY.",
                ".YY..oGGrRwWWwRrGGo..YY.",
                ".....oGgrRYYYYRrgGo.....",
                "......oGgrRRRRrgGo......",
                "......oGggrrrrggGo......",
                ".......ooggggggoo.......",
                ".....Y...oooooo...Y.....",
                "....Y.....oGgo.....Y....",
                "..........oGgo..........",
                ".........oGGggo.........",
                "........oGGYGggo........",
                ".......oGGGGGGggo.......",
                ".......oooooooooo.......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "ogGYrRwW", 0x7C2D12, 0xB45309, 0xF59E0B, 0xFDE047, 0xEA580C, 0xFB923C, 0xFEF3C7,
                    0xFFFFFF);
            // A thin spire piercing a thundercloud, lightning forking down either side.
            case STORM -> Assets.pixelArt(new String[] {
                "...........oo...........",
                "..........otWo..........",
                ".........ootWoo.........",
                "...oooo.oWWLLLLo.oooo...",
                "..oWWLLooLLLLLLooWWLLo..",
                ".oLLLLLLoLLLLLLoLLLCCCo.",
                ".oLLLLLLCLLLLLLCLLCCCCo.",
                ".oLLCCCCCCCCLCCCCCCCcco.",
                "..oCCccccCCCCccccccco...",
                "...oooooooosSooooooo....",
                ".....yy...osSo...yy.....",
                "....yy....osSo....yy....",
                "....yyy...osSo...yyy....",
                "......y..ostSSo..y......",
                ".....y...ostSSo...y.....",
                ".........ostSSo.........",
                "........osstSSSo........",
                ".......oooooooooo.......",
                "......osssstSSSSSo......",
                "......oooooooooooo......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oLCctsSWy", 0x0F172A, 0x93A8D8, 0x5B6FA8, 0x3B4A7A, 0xA5F3FC, 0x155E75, 0x22B8CF,
                    0xFFFFFF, 0xFDE047);
            // A dark monolith carved with glowing runes, fumes curling off it.
            case MIASMA -> Assets.pixelArt(new String[] {
                BLANK,
                "...........oo...........",
                "..........oLSo.....f....",
                "....f....oLLSSo...ff....",
                "...ff...oLLSSSso...f....",
                "...f....oLLGGSso........",
                "........oLGSSGso........",
                "........oLLGGSso..f.....",
                ".....f..oLLSSSso.ff.....",
                "....ff..oLGGSSso..f.....",
                "....f...oLLGGSso........",
                "........oLLSGGso........",
                "........oLLSSSso........",
                "........oLGSSGso........",
                "........oLLGGSso........",
                "........oLLSSSso........",
                ".......oLLLSSSSso.......",
                "......oooooooooooo......",
                ".....oLLLSSSSSSSsso.....",
                "..gGGooooooooooooooGGg..",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "osSLGgf", 0x0B140B, 0x1F2A1F, 0x354535, 0x56705A, 0xBEF264, 0x65A30D, 0xC084CC16);
            // A glass prism banded with aurora light, on a small stand.
            case AURORA -> Assets.pixelArt(new String[] {
                BLANK,
                "...........oo...........",
                "..........oWgo..........",
                "....W.....oWgo..........",
                "...WWW...oWggco.........",
                "....W...oWgggcco........",
                "........oWggccbo........",
                ".......oWgggccbbo...W...",
                "......oWwgggccbbpo.WWW..",
                "......oWgggcccbbpo..W...",
                ".....oWwgggcccbbppo.....",
                "....oWwgggcccbbbppmo....",
                "....oWggggcccbbbppmo....",
                "...oWwggggccccbbbppmo...",
                "...oooooooooooooooooo...",
                "..........osSo..........",
                "..........osSo..........",
                "........ossSSSSo........",
                ".......osssSSSSSo.......",
                ".......oooooooooo.......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oWwgcbpmsS", 0x1E3A5F, 0xFFFFFF, 0xD1FAE5, 0x6EE7B7, 0x67E8F9, 0x60A5FA, 0xA78BFA,
                    0xF0ABFC, 0x475569, 0xA7B0BE);
            // A skull with burning green eyes mounted on a corroded coil.
            case PLAGUE -> Assets.pixelArt(new String[] {
                BLANK,
                "........oooooooo........",
                "...g..ooBBBBBBbboo......",
                ".....oBBBBBBBBbbbdo.....",
                "..g..oBBBBBBBBbbbdo..g..",
                ".....oBkkkBBbkkkbdo.....",
                ".....oBkGkBBbkGkbdo.....",
                "...g.oBkkkBBbkkkbdo.g...",
                "......oBBBBkkbbbdo......",
                "....g..oBBBBbbbdo..g....",
                ".......oBkBkbkbo........",
                "........ooooooo.........",
                "........oCYCCCco........",
                "........occcccco........",
                "........oCYCCCco........",
                "........occcccco........",
                "......oooooooooooo......",
                ".....oSSSSSSSSSSmmo.....",
                ".....ommmmmmmmmmDDo.....",
                "......oooooooooooo......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oBbdkGgCYcSmD", 0x14260A, 0xF5F5DC, 0xD6D3B8, 0xA8A58C, 0x14200A, 0xBEF264, 0x84CC16,
                    0x65A30D, 0xBEF264, 0x3F6212, 0x6B7280, 0x4B5563, 0x374151);
            // A three-armed stand, a burning lens on each arm and sparks arcing between them.
            case ARC -> Assets.pixelArt(new String[] {
                "..........oooo..........",
                ".........oRRRro.........",
                ".......yyoRWwRoyy.......",
                "......yy.oRwwro.yy......",
                "......yy..oooo..yy......",
                "..ooooy...oSSo...yoooo..",
                ".oRRRro...osSo...oRRRro.",
                ".oRWwRo...osSo...oRWwRo.",
                ".oRwwro...osSo...oRwwro.",
                "..oooo....osSo....oooo..",
                "..osSo....osSo....osSo..",
                "..osSo....osSo....osSo..",
                "..osSoooooosSoooooosSo..",
                "..osSSSSSSSSSSSSSSSSSo..",
                "..osssssssssssssssssso..",
                "...ooooooosSSsooooooo...",
                ".........osSSso.........",
                ".......ooosSSsooo.......",
                "......osssSSSSSSso......",
                "......oooooooooooo......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oRrWwsSy", 0x0B1220, 0xFB923C, 0xC2410C, 0xFFFFFF, 0xFEF3C7, 0x273043, 0x56637F,
                    0xFDE047);
            // A round flask slowly filling with drained mana, a green crystal for a stopper.
            case SIPHON -> Assets.pixelArt(new String[] {
                "...........oo...........",
                "......V...oVTo...V......",
                ".........oVVvvo.........",
                ".......V..ovvo..V.......",
                ".........oGGGGo.........",
                "..........oGGo..........",
                "..........oGGo..........",
                "........ooGGGGoo........",
                ".......oGGWGGGGGo.......",
                "......oGGWWGGGGGGo......",
                "......oGWWGGGGGGGo......",
                ".....oLLLLLLLLLLLLo.....",
                ".....oMWMMMMMMMMmmo.....",
                ".....oMWMMMLMMMMmmo.....",
                ".....oMMMMMMMMLMmmo.....",
                ".....oMMMLMMMMMmmmo.....",
                "......oMMMMMMMmmmo......",
                "......oMMMMMMmmmmo......",
                ".......oommmmmmoo.......",
                ".........oooooo.........",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oVvTGWLMm", 0x172554, 0xA3E635, 0x4D7C0F, 0xECFCCB, 0xA0E0F2FE, 0xFFFFFF, 0x93C5FD,
                    0x3B82F6, 0x1D4ED8);
            // A carved totem pole: two stacked faces with glowing eyes, feathered wings, a crystal on top.
            case HEX -> Assets.pixelArt(Assets.symmetric(
                "...........o",
                "..........oP",
                "..........oV",
                ".......ooooo",
                ".ooo...oWWWW",
                "oRRRo.oWkkWW",
                "oRrRRooWkVkW",
                ".oRrRRRWWWWW",
                "..ooooowWWkk",
                "......owwWWW",
                "......oooooo",
                ".......owwww",
                ".......owVVw",
                ".......owwww",
                ".......owkkk",
                ".......owkBk",
                ".......owwww",
                "......oooooo",
                ".....owwWWWW",
                ".....ooooooo",
                "............",
                "............",
                "............",
                "............"
            ), "oWwkVPRrB", 0x1A0F0A, 0xB57A40, 0x7A4B24, 0x24130A, 0xC084FC, 0xF3E8FF, 0x7E22CE,
                    0xD8B4FE, 0xF5F5DC);
            // A long silver-barrelled stake rifle on a wooden stock, a hexed lens for a sight.
            case HUNTER -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                "...ooo..................",
                "..oWWWooo........oo.....",
                ".oWwwwWWWooooooooSSo....",
                ".oWwwwwwwdmmmmdooSso....",
                ".oWwwwwwwdmVVmdoSsoooooo",
                ".oWwwwwwwdmVPmdSSSSSSSTo"
            ), "oWwdmVPSsT", 0x1A1208, 0xC8934A, 0x8A5A2B, 0x1F2937, 0x4B5563, 0xA855F7, 0xF3E8FF,
                    0xD7DCE5, 0x8E96A8, 0xFFFFFF);
            // A horned mortar of dark violet iron, its flared mouth glowing from within.
            case DOOM -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                BLANK,
                "....o...................",
                "...obo..........oooo....",
                "...obbo.......ooPPPVo...",
                "....obboooooooPPVVkVo...",
                "....oppppppppPPVkkkVo...",
                "...opPLLPPPPpPVVkkkkVo..",
                "...opLLLLPPPpPVkkGGkVo..",
                "...opLLyLPPPpPVkGGGGVo.."
            ), "opPLVkGby", 0x12091F, 0x3B2A5A, 0x5B4690, 0x8672C4, 0xB794F4, 0x0A0612, 0xE9D5FF,
                    0xE8E0D0, 0xFBBF24);
            // Two stone horns cradling a great staring eye.
            case DREAD -> Assets.pixelArt(Assets.symmetric(
                "..o.........",
                "..oo........",
                "..oDo.......",
                "..oDDo......",
                "...oDdo..ooo",
                "...oDDo.oPPV",
                "...oDDdooPVk",
                "....oDdooPVk",
                "....oDDo.oPV",
                ".....oDdo.oo",
                ".....oDDdooo",
                "......oDDDdd",
                ".......oDDDd",
                "........oDDd",
                "........oDDd",
                "........oDDd",
                ".......ooooo",
                "......osSSSS",
                "......oooooo",
                "............",
                "............",
                "............",
                "............",
                "............"
            ), "oDdPVksS", 0x0D0A14, 0x4A4266, 0x2C2642, 0xF5F3FF, 0xA855F7, 0x1A0530, 0x4B4560,
                    0x7A7396);
            // A scythe: the blade sweeps out to one side above a violet winding on the shaft.
            case REAPER -> Assets.pixelArt(new String[] {
                "....oooooooooo..........",
                "..ooSSSSSSSSSSoo........",
                ".oSSSsssssssSSSSo.......",
                "oSSsooooooowwooo........",
                "oSso......owwo..........",
                "oSo.......owwo..........",
                ".o........owwo..........",
                "..........oVVo..........",
                ".........oCYCco.........",
                ".........occcco.........",
                ".........oCYCco.........",
                ".........occcco.........",
                ".........oCYCco.........",
                ".........occcco.........",
                "..........owwo..........",
                "..........owwo..........",
                ".......oooooooooo.......",
                "......oSSSSSSSSmmo......",
                "......ommmmmmmmddo......",
                ".......oooooooooo.......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oSswVCYcmd", 0x111827, 0xE2E6EE, 0x8793AA, 0x9A6A35, 0xE9D5FF, 0x9333EA, 0xD8B4FE,
                    0x4C1D95, 0x56617A, 0x374151);
            // A warty plague toad seen from above: bulging yellow eyes and a wide, dark mouth.
            case ROT -> Assets.pixelArt(mirrored(
                BLANK,
                BLANK,
                BLANK,
                ".......ooooooo..........",
                ".....oorrrrrrroo........",
                "....orrRRgRRRrrrooo.....",
                "...orRRRRRRpRRroYYYo....",
                "..orRgRRRRRRRRroYkkYo...",
                "..orRRRRpRRRRRRroYYooo..",
                ".orRRRRRRRRRgRRRrooRRro.",
                ".orRpRRRRRRRRRRRRRRRRkto",
                ".orRRRRRgRRRRRRRRRkkkkto"
            ), "orRgpYkt", 0x1C1408, 0x6B4A1E, 0x9A7B2F, 0x6B8E23, 0xA855F7, 0xFDE047, 0x120C04,
                    0xE8E0D0);
            // A violet soul gem hovering over a clawed stand, wisps drifting off it.
            case SOUL -> Assets.pixelArt(new String[] {
                "...........oo...........",
                "..........oPVo..........",
                ".........oPVVvo.........",
                "........oPPVVvvo........",
                ".......oPPVVVvvdo.......",
                "......oPPVVVVvvddo..g...",
                ".....oPPPVVVVvvvddo.gg..",
                ".....oVVVVVVvvvvddo.....",
                "......oVVVVvvvvddo......",
                "..g....oVVvvvvddo.......",
                ".gg.....oVvvvddo........",
                ".........ovvddo.........",
                "..........ovdo..........",
                "....o......oo......o....",
                "....oso..........oSo....",
                ".....oso........oSo.....",
                "......ossooooooSSo......",
                ".......osssSSSSSo.......",
                "......osssssSSSSSo......",
                "......oooooooooooo......",
                BLANK,
                BLANK,
                BLANK,
                BLANK,
            }, "oPVvdsSg", 0x1E0A3C, 0xF3E8FF, 0xC084FC, 0x9333EA, 0x581C87, 0x2A2140, 0x5B4B8A,
                    0xC0E9D5FF);
        };
    }

    /** Completes a turret that is symmetric about its barrel line from its top twelve rows. */
    private static String[] mirrored(String... top) {
        String[] rows = new String[top.length * 2];
        for (int i = 0; i < top.length; i++) {
            rows[i] = top[i];
            rows[rows.length - 1 - i] = top[i];
        }
        return rows;
    }
}
