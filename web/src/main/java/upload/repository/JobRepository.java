package upload.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import upload.model.Job;

import java.util.List;

public interface JobRepository extends CrudRepository<Job, Integer> {
    List<Job> findByPasscode(String passcode);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Job SET status = :newStatus WHERE passcode = :passcode and status in :oldStatus")
    int updateStatus(@Param("passcode") String passcode, @Param("newStatus") String newStatus, @Param("oldStatus") List oldStatus);
}
