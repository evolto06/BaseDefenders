package components;

/** Steadily regrows a fraction of the monster's maximum health each second. */
public final class Regen {
    public final float perSecond;

    public Regen(float perSecond) {
        this.perSecond = perSecond;
    }
}
