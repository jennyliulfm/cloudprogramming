package upload.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Consts {
    public static final String JOB_INIT = "I";
    public static final String JOB_PROCESSING = "P";
    public static final String JOB_COMPLETED = "C";
    public static final String JOB_CANCELLING = "X";
    public static final String JOB_TERMINATED = "T";
    public static final List<String> JOB_CANCELABLE = Collections.unmodifiableList(Arrays.asList(JOB_INIT, JOB_PROCESSING));
}
