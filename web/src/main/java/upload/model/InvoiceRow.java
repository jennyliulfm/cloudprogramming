package upload.model;

import java.io.Serializable;

public class InvoiceRow implements Serializable {

    private String description;
    private Double quantity;
    private Double price ;

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getRowtotal() {
            return price;
    }

}
