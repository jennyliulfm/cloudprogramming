package edu.utas.util;

import edu.utas.vo.Job;

import java.util.Comparator;

public class JobComparator implements Comparator<Job> {

    public Long getJobPriority(Job j)
    {
        return j.getCreated().getTime() + j.getExpectation() * 1000 * 60;
    }

    @Override
    public int compare(Job o1, Job o2) {
        // write comparison logic here like below , it's just a sample
        return getJobPriority(o1).compareTo(getJobPriority(o2));
    }
}
