package upload.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Invoice {

    private String invoiceDate;
    private List<InvoiceRow> invoiceRows = new ArrayList<>();

    public List<InvoiceRow> getInvoiceRows() {
        return invoiceRows;
    }

    public void setInvoiceRows(List<InvoiceRow> invoiceRows) {
        this.invoiceRows = invoiceRows;
    }

    public Double getTotal() {
        double sum = 0;
        List<InvoiceRow> invoiceRows1 = getInvoiceRows();
        for (InvoiceRow r : invoiceRows1) {
            sum += r.getRowtotal();
        }
        return sum;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

}
