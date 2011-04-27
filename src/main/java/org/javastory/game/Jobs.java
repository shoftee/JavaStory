/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.game;

/**
 *
 * @author Tosho
 */
public final class Jobs {

    private Jobs() {
    }

    public static boolean isBeginner(int jobId) {
        return jobId == 0 || jobId == 1000 || jobId == 2000 || jobId == 2001;
    }

    public static boolean isJobFamily(final int baseJob, final int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isAdventurer(final int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isCygnus(final int job) {
        return job >= 1000 && job < 2000;
    }

    public static boolean isAran(final int job) {
        return job >= 2000 && job <= 2112 && job != 2001;
    }

    public static boolean isEvan(final int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

}
