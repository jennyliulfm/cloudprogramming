package upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.servlet.view.RedirectView;
import upload.model.Invoice;
import upload.model.InvoiceRow;
import upload.model.Job;
import upload.repository.JobRepository;
import upload.service.JobService;
import upload.util.Consts;

@Controller
public class DashboardController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/dashboard")
    public String initDashboard(Model model) throws IOException {
        return "dashboard";
    }

    @PostMapping("/dashboard")
    public String handleDashboard(@RequestParam(value="passcode") String passcode,
                                   RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("passcode",
                passcode);
        List<Job> jobs = jobRepository.findByPasscode(passcode);
        if (jobs.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", messageSource.getMessage("msg.no.record", new String[]{passcode},null));
        } else {
            redirectAttributes.addFlashAttribute("file",
                    jobs.get(0).getFilename());
            String label = "";
            String uri = "";
            switch(jobs.get(0).getStatus()){
                case Consts.JOB_INIT :
                    label = messageSource.getMessage("label.job.init", null,null);
                    break;
                case Consts.JOB_PROCESSING :
                    label = messageSource.getMessage("label.job.processing", null,null);
                    break;
                case Consts.JOB_COMPLETED:
                    label = messageSource.getMessage("label.job.completed", null,null);
                    if (jobs.get(0).getResult() != null && !jobs.get(0).getResult().isEmpty()) {
                        uri = MvcUriComponentsBuilder
                                .fromMethodName(FileUploadController.class,
                                        "serveFile", jobs.get(0).getPasscode(), jobs.get(0).getResult()).build().toString();
                        redirectAttributes.addFlashAttribute("result", uri);
                    }
                    if (jobs.get(0).getCost() > 0) {
                        redirectAttributes.addFlashAttribute("cost", messageSource.getMessage("label.currency", null, null) + jobs.get(0).getCost());
                    }
                    break;
                case Consts.JOB_CANCELLING:
                    label = messageSource.getMessage("label.job.cancelling", null,null);
                    break;
                case Consts.JOB_TERMINATED :
                    label = messageSource.getMessage("label.job.terminated", null,null);
                    break;
            }
            redirectAttributes.addFlashAttribute("message", label);
            if (Consts.JOB_CANCELABLE.contains(jobs.get(0).getStatus())) {
                redirectAttributes.addFlashAttribute("cancelable", true);
            }
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/cancel")
    public ModelAndView handleCancel(@RequestParam(value="passcode") String passcode,
                               ModelAndView view) {
        jobService.cancelJob(passcode);
        RedirectView redirectView = new RedirectView("/dashboard");
        redirectView.setExpandUriTemplateVariables(false);
        redirectView.setExposeModelAttributes(false);
        redirectView.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
        redirectView.setHttp10Compatible(Boolean.TRUE);
        view.setView(redirectView);
        view.addObject("passcode", passcode);
        return view;
    }

    @ResponseBody
    @RequestMapping("/invoice/{passcode}")
    public ResponseEntity getInvoice(@PathVariable String passcode) {
        List<Job> jobs = jobRepository.findByPasscode(passcode);

        ByteArrayOutputStream out= new ByteArrayOutputStream();

        ArrayList<InvoiceRow> invoiceRows = new ArrayList();
        InvoiceRow invoiceRow = new InvoiceRow();
        if (!jobs.isEmpty()) {
            invoiceRow.setDescription("Run " + jobs.get(0).getFilename());
            invoiceRow.setQuantity(jobs.get(0).getDuration());
            invoiceRow.setPrice(jobs.get(0).getCost());
        }
        invoiceRows.add(invoiceRow);
        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(new SimpleDateFormat("dd-MMM-yyyy").format(new Date()));
        invoice.setInvoiceRows(invoiceRows);
        try {
            InputStream in = getClass()
                    .getClassLoader().getResourceAsStream("Invoice_Template.odt");
            IXDocReport report = XDocReportRegistry.getRegistry().
                    loadReport(in, TemplateEngineKind.Freemarker);

            FieldsMetadata metadata = report.createFieldsMetadata();
            metadata.load("r", InvoiceRow.class, true);

            IContext ctx = report.createContext();
            ctx.put("invoice", invoice);
            ctx.put("r", invoice.getInvoiceRows());

            report.process(ctx, out);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("charset", "utf-8");
//        responseHeaders.setContentType(MediaType.valueOf("text/html"));
        responseHeaders.setContentLength(out.toByteArray().length);
        responseHeaders.set("Content-disposition", "attachment; filename=Invoice.odt");

        return new ResponseEntity<byte[]>(out.toByteArray(), responseHeaders, HttpStatus.OK);
    }
}
