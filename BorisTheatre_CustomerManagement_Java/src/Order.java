import java.time.LocalDate;

public class Order {
    private int orderID;
    private int customerID;
    private LocalDate orderDate;
    private double totalAmount;
    private String orderStatus;

    public Order(int orderID, int customerID, LocalDate orderDate, double totalAmount, String orderStatus) {
        this.orderID = orderID;
        this.customerID = customerID;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
    }

    public int getOrderID() { return orderID; }
    public int getCustomerID() { return customerID; }
    public LocalDate getOrderDate() { return orderDate; }
    public double getTotalAmount() { return totalAmount; }
    public String getOrderStatus() { return orderStatus; }

    @Override
    public String toString() {
        return String.format("Order #%d | Date: %s | Total: £%.2f | Status: %s",
                orderID, orderDate, totalAmount, orderStatus);
    }
}
