package upload;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import upload.model.Job;
import upload.repository.JobRepository;
import upload.storage.StorageFileNotFoundException;
import upload.storage.StorageService;
import upload.util.Consts;

@Controller
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Autowired
    private JobRepository jobRepository;

    @GetMapping("/")
    public String initUpload(Model model) throws IOException {
        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @GetMapping("/files/{path}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String path, @PathVariable String filename) {

        Resource file = storageService.loadAsResource(path, filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("data") MultipartFile data,
                                   @RequestParam(value="minutes", defaultValue="5") int expectation,
            RedirectAttributes redirectAttributes) {

        Job job = new Job();
        job.setFilename(file.getOriginalFilename());
        job.setStatus(Consts.JOB_INIT);
        job.setExpectation(expectation);
        job.setCreated(new Date());
        job.setPasscode(job.getCreated().getTime()
                + RandomStringUtils.random(7, true, true));

        storageService.store(job.getPasscode(), file);

        if (data != null && !data.isEmpty()) {
            storageService.store(job.getPasscode(), data);
            job.setInput(data.getOriginalFilename());
        }

        jobRepository.save(job);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "! "
                        + "Passcode: " + job.getPasscode());

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleError(MaxUploadSizeExceededException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Error:" + e.getCause().getMessage().split(":")[1]);
        return "redirect:/";
    }
}
