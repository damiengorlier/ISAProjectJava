package ac.ulb.enums;

public enum ActionEnum {
    PLUS_ANGLE_X("plusAngleX"),
    LESS_ANGLE_X("lessAngleX"),
    PLUS_ANGLE_Y("plusAngleY"),
    LESS_ANGLE_Y("lessAngleY"),
    PLUS_ANGLE_Z("plusAngleZ"),
    LESS_ANGLE_Z("lessAngleZ")
    ;

    private String action;

    ActionEnum(String action) {
        this.action = action;
    }

    public String action() {
        return action;
    }
}
