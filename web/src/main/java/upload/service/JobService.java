package upload.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import upload.repository.JobRepository;
import upload.util.Consts;

import javax.transaction.Transactional;

@Component
public class JobService {
    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public void cancelJob(String passcode){
        jobRepository.updateStatus(passcode, Consts.JOB_CANCELLING, Consts.JOB_CANCELABLE);;
    }
}
