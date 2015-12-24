package ac.ulb.utils;

import ac.ulb.enums.AnimationStep;

public class Const {

    public static class Path {
        public static final String TEXTURES_ROOT = "/textures";
        public static final String UTERUS_TEXTURE_PATH = TEXTURES_ROOT + "/" + "uterus_text.png";
        public static final String UTERUS_BUMP_PATH = TEXTURES_ROOT + "/" + "uterus_bump.png";

        public static final String MODEL_ROOT = "/model";
        public static final String BABY_MODEL_OBJ_PATH = MODEL_ROOT + "/baby_original_triangles.obj";

        public static final String SHADERS_ROOT = "/shaders";
        public static final String SHADER_EXT = "glsl";
        public static final String VERTEX_SHADER = "vs";
        public static final String FRAGMENT_SHADER = "fs";
        public static final String SPHERE_SHADERS_PATH = "/sphere";
        public static final String BABY_SHADERS_PATH = "/baby";
        public static final String TEST_SHADERS_PATH = "/test";
    }

    public static class Uniform {
        public static final String LIGHT_POSITION = "lightPosition";
        public static final String UTERUS_TEXTURE = "uterusTexture";
        public static final String UTERUS_BUMP_MAP = "uterusBumpMap";
    }

    public static class Sphere {
        public static final int R = 8;

        public static final float[][][] CP_SPHERE_UP_4D = {
                {{ 0, 0, 1, 9},   { 0, 0, 1, 3},   { 0, 0, 1, 3},   { 0, 0, 1, 9}},
                {{ 2, 0, 1, 3},   { 2, 4, 1, 1},   {-2, 4, 1, 1},   {-2, 0, 1, 3}},
                {{ 2, 0,-1, 3},   { 2, 4,-1, 1},   {-2, 4,-1, 1},   {-2, 0,-1, 3}},
                {{ 0, 0,-1, 9},   { 0, 0,-1, 3},   { 0, 0,-1, 3},   { 0, 0,-1, 9}}
        };
        public static final float[][][] CP_SPHERE_DOWN_4D = {
                {{ 0, 0, 1, 9},   { 0, 0, 1, 3},   { 0, 0, 1, 3},   { 0, 0, 1, 9}},
                {{ 2, 0, 1, 3},   { 2,-4, 1, 1},   {-2,-4, 1, 1},   {-2, 0, 1, 3}},
                {{ 2, 0,-1, 3},   { 2,-4,-1, 1},   {-2,-4,-1, 1},   {-2, 0,-1, 3}},
                {{ 0, 0,-1, 9},   { 0, 0,-1, 3},   { 0, 0,-1, 3},   { 0, 0,-1, 9}}
        };
        public static final int NBR_SAMPLE_POINTS = 20;
    }

    public static class Baby {
        public static final float[] SCALE = {1.1f, 1.1f, 1.1f};
    }

    public static class Light {
        public static final int NUM_LIGHTS = 6;
        public static final int R_LIGHT = 3 * Sphere.R / 4;
        public static final float[] POS_X = {1, -0.5f};
        public static final float[] POS_Y = {(float) Math.sin(Math.PI / 4), (float) -Math.sin(Math.PI / 4)};
        public static final float[] POS_Z = {0, (float) Math.sin(2 * Math.PI / 3), (float) -Math.sin(2 * Math.PI / 3)};

        public static final float[] LIGHTS_POSITIONS = {
                R_LIGHT * POS_X[0],     R_LIGHT * POS_Y[0],     R_LIGHT * POS_Z[0],
                R_LIGHT * POS_X[0],     R_LIGHT * POS_Y[1],     R_LIGHT * POS_Z[0],
                R_LIGHT * POS_X[1],     R_LIGHT * POS_Y[0],     R_LIGHT * POS_Z[1],
                R_LIGHT * POS_X[1],     R_LIGHT * POS_Y[1],     R_LIGHT * POS_Z[1],
                R_LIGHT * POS_X[1],     R_LIGHT * POS_Y[0],     R_LIGHT * POS_Z[2],
                R_LIGHT * POS_X[1],     R_LIGHT * POS_Y[1],     R_LIGHT * POS_Z[2]
        };
    }

    public static class Anim {
        public static final AnimationStep FIRST_STEP = AnimationStep.MOVE_VIEW;
        public static final float[] EYE_MAX_STEP = {1, 1, 1};
        public static final float[] EYE_END_POSITION = {0, 0, Const.Sphere.R * 4};
        public static final float[] BABY_MAX_STEP = {1, 1, 1};
        public static final float[] BABY_ANGLE_MAX_STEP = {2, 2, 2};
        public static final float[] BABY_OUT_ANGLE = {270, 0, 180};
        public static final float[] BABY_END_ANGLE = {0, 0, 0};
        public static final float[] BABY_END_POSITION = {0, Const.Sphere.R, 0};
        public static final float UTERUS_MAX_OPENING = 0.75f;
        public static final float UTERUS_STEP_OPENING = 0.01f;
    }

}
