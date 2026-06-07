package PSM.Checkout.api.cart;

import java.util.ArrayList;
import java.util.List;

public class CartResponseDTO {
    private List<CartItemDTO> items = new ArrayList<>();
    private double subtotal;
    private double taxes;
    private double total;
    private double discount;

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTaxes() {
        return taxes;
    }

    public void setTaxes(double taxes) {
        this.taxes = taxes;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
}
