/* =============================================================================
*
*                  COPYRIGHT 2007 BBN Technologies Corp.
*                  10 Moulton St
*                  Cambridge MA 02138
*                  (617) 873-8000
*
*       This program is the subject of intellectual property rights
*       licensed from BBN Technologies
*
*       This legend must continue to appear in the source code
*       despite modifications or enhancements by any party.
*
*
* =============================================================================
*
* Created : Aug 9, 2007
* Workfile: RegressionCondition.java
* $Revision: 1.1 $
* $Date: 2007-10-19 15:01:53 $
* $Author: rshapiro $
*
* =============================================================================
*/
 
package org.cougaar.test.regression;

import org.cougaar.test.sequencer.Context;

public class RegressionContext implements Context {
    private final int workerTimout;
    private final boolean failed;

    public RegressionContext(int workerTimeout, boolean failed) {
        super();
        this.workerTimout = workerTimeout;
        this.failed = failed;
    }

    public int getWorkerTimeout() {
        return workerTimout;
    }

    public boolean hasFailed() {
        return failed;
    }
    
    public String toString() {
        return "Context: hasFailed="+failed+" workerTimeout="+workerTimout;
    }
    
}