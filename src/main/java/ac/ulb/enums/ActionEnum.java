package ac.ulb.enums;

public enum ActionEnum {
	//Reset
	RESET("reset"),
	
    // Animation
    START_ANIMATION("startAnimation"),

    // Rotations
    PLUS_ANGLE_X("plusAngleX"),
    LESS_ANGLE_X("lessAngleX"),
    PLUS_ANGLE_Y("plusAngleY"),
    LESS_ANGLE_Y("lessAngleY"),
    PLUS_ANGLE_Z("plusAngleZ"),
    LESS_ANGLE_Z("lessAngleZ"),

    // Translations
    PLUS_DIST_X("plusDistX"),
    LESS_DIST_X("lessDistX"),
    PLUS_DIST_Y("plusDistY"),
    LESS_DIST_Y("lessDistY"),
    PLUS_DIST_Z("plusDistZ"),
    LESS_DIST_Z("lessDistZ")
    ;

    private String action;

    ActionEnum(String action) {
        this.action = action;
    }

    public String action() {
        return action;
    }
}
