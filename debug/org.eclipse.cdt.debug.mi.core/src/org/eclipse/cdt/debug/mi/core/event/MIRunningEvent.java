package org.eclipse.cdt.debug.mi.core.event;

import org.eclipse.cdt.debug.mi.core.output.MIConst;
import org.eclipse.cdt.debug.mi.core.output.MIExecAsyncOutput;
import org.eclipse.cdt.debug.mi.core.output.MIFrame;
import org.eclipse.cdt.debug.mi.core.output.MIResult;
import org.eclipse.cdt.debug.mi.core.output.MIResultRecord;
import org.eclipse.cdt.debug.mi.core.output.MITuple;
import org.eclipse.cdt.debug.mi.core.output.MIValue;

/**
 *
 *  ^running
 */
public class MIRunningEvent extends MIEvent {

	public static final int CONTINUE = 1;
	public static final int NEXT = 1;
	public static final int NEXTI = 2;
	public static final int STEP = 3;
	public static final int STEPI = 4;
	public static final int FINISH = 5;
	public static final int UNTIL = 6;

	int type;

	public MIRunningEvent(int t) {
		type = t;
	}

	public int getType() {
		return type;
	}

	public String toString() {
		return "Running";
	}
}
