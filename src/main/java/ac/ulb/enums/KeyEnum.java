package ac.ulb.enums;

public enum KeyEnum {
    LOWER_A("A"),
    UPPER_A("shift A"),
    LOWER_D("D"),
    UPPER_D("shift D"),
    LOWER_I("I"),
    UPPER_I("shift I"),
    LOWER_L("L"),
    UPPER_L("shift L"),
    LOWER_O("O"),
    UPPER_O("shift O"),
    LOWER_R("R"),
    UPPER_R("shift R"),
    LOWER_U("U"),
    UPPER_U("shift U"),
    LOWER_X("X"),
    UPPER_X("shift X"),
    LOWER_Y("Y"),
    UPPER_Y("shift Y"),
    LOWER_Z("Z"),
    UPPER_Z("shift Z"),

    LEFT("LEFT"),
    RIGHT("RIGHT"),
    UP("UP"),
    DOWN("DOWN")
    ;

    private String keyString;

    KeyEnum(String keyString) {
        this.keyString = keyString;
    }

    public String key() {
        return keyString;
    }

}
