package ac.ulb.enums;

public enum AnimationStep {

    MOVE_VIEW,
    OPEN_UTERUS,
    ROTATE_BABY_OUT,
    TRANSLATE_BABY,
    END;

    public AnimationStep next() {
        return this.ordinal() < AnimationStep.values().length - 1
                ? AnimationStep.values()[this.ordinal() + 1]
                : null;
    }
}
