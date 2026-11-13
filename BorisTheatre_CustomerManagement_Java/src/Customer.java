import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Customer {
    private int customerID;
    private String fullName;
    private String emailAddress;
    private String phoneNumber;
    private LocalDate dateJoined;
    private boolean isLoyaltyMember;

    public Customer(int customerID, String fullName, String emailAddress, String phoneNumber) {
        this.customerID = customerID;
        this.fullName = fullName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.dateJoined = LocalDate.now();
        this.isLoyaltyMember = false;
    }

    public Customer(int customerID, String fullName, String emailAddress, String phoneNumber,
                    LocalDate dateJoined, boolean isLoyaltyMember) {
        this.customerID = customerID;
        this.fullName = fullName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.dateJoined = dateJoined;
        this.isLoyaltyMember = isLoyaltyMember;
    }

    public void register() {
        System.out.println("Customer registered: " + this.fullName + " (ID: " + this.customerID + ")");
    }

    public boolean login(String email) {
        return this.emailAddress != null && this.emailAddress.equalsIgnoreCase(email.trim());
    }

    public void updateDetails(String fullName, String emailAddress, String phoneNumber) {
        this.fullName = fullName;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
    }

    public List<Order> viewBookingHistory(List<Order> allOrders) {
        List<Order> customerOrders = new ArrayList<>();
        for (Order order : allOrders) {
            if (order.getCustomerID() == this.customerID) {
                customerOrders.add(order);
            }
        }
        return customerOrders;
    }

    public int getCustomerID() { return customerID; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getDateJoined() { return dateJoined; }

    public boolean isLoyaltyMember() { return isLoyaltyMember; }
    public void setLoyaltyMember(boolean loyaltyMember) { isLoyaltyMember = loyaltyMember; }
}
