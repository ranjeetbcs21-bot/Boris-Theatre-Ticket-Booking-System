import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CustomerService {
    private List<Customer> customers = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private int nextCustomerId = 1;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public CustomerService() {
        seedSampleData();
    }

    private void seedSampleData() {
        Customer c1 = new Customer(nextCustomerId++, "Alice Johnson", "alice@example.com", "07700900111", LocalDate.now().minusDays(10), true);
        Customer c2 = new Customer(nextCustomerId++, "Bob Smith", "bob.smith@example.com", "+447700900222", LocalDate.now().minusDays(3), false);
        Customer c3 = new Customer(nextCustomerId++, "Charlie Brown", "charlie@example.com", "", LocalDate.now(), false);

        customers.add(c1);
        customers.add(c2);
        customers.add(c3);

        orders.add(new Order(101, 1, LocalDate.now().minusDays(5), 45.00, "Confirmed"));
        orders.add(new Order(102, 1, LocalDate.now().minusDays(1), 30.00, "Pending"));
    }

    public String validateCustomerInput(String fullName, String email, Integer excludeCustomerId) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Validation Error: Full Name is required.";
        }
        if (email == null || email.trim().isEmpty()) {
            return "Validation Error: Email Address is required.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Validation Error: Invalid email address format.";
        }
        for (Customer c : customers) {
            if (c.getEmailAddress().equalsIgnoreCase(email.trim())) {
                if (excludeCustomerId == null || c.getCustomerID() != excludeCustomerId) {
                    return "Validation Error: Email address is already registered.";
                }
            }
        }
        return null;
    }

    public Customer registerCustomer(String fullName, String email, String phone) {
        String error = validateCustomerInput(fullName, email, null);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        Customer newCustomer = new Customer(
                nextCustomerId++,
                fullName.trim(),
                email.trim(),
                phone != null ? phone.trim() : ""
        );
        customers.add(newCustomer);
        return newCustomer;
    }

    public Customer loginByEmail(String email) {
        for (Customer c : customers) {
            if (c.login(email)) {
                return c;
            }
        }
        return null;
    }

    public Customer findById(int id) {
        for (Customer c : customers) {
            if (c.getCustomerID() == id) {
                return c;
            }
        }
        return null;
    }

    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }

    public List<Customer> searchCustomers(String keyword) {
        List<Customer> results = new ArrayList<>();
        String query = keyword.trim().toLowerCase();
        for (Customer c : customers) {
            if (c.getFullName().toLowerCase().contains(query) ||
                c.getEmailAddress().toLowerCase().contains(query)) {
                results.add(c);
            }
        }
        return results;
    }

    public void updateCustomer(int customerId, String fullName, String email, String phone, Boolean loyaltyMember) {
        Customer target = findById(customerId);
        if (target == null) {
            throw new IllegalArgumentException("Customer not found.");
        }
        String error = validateCustomerInput(fullName, email, customerId);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        target.updateDetails(fullName.trim(), email.trim(), phone != null ? phone.trim() : "");
        if (loyaltyMember != null) {
            target.setLoyaltyMember(loyaltyMember);
        }
    }

    public boolean deleteCustomer(int customerId) {
        Customer target = findById(customerId);
        if (target == null) {
            throw new IllegalArgumentException("Customer ID not found.");
        }
        List<Order> linkedOrders = target.viewBookingHistory(orders);
        if (!linkedOrders.isEmpty()) {
            throw new IllegalStateException(
                "Cannot delete customer #" + customerId + ": customer has " + linkedOrders.size() + " linked order(s)."
            );
        }
        return customers.remove(target);
    }

    public void anonymizeCustomer(int customerId) {
        Customer target = findById(customerId);
        if (target == null) {
            throw new IllegalArgumentException("Customer ID not found.");
        }
        target.updateDetails("Closed Account #" + customerId, "closed_" + customerId + "@boristheatre.local", "");
        target.setLoyaltyMember(false);
    }

    public List<Order> getPurchaseHistory(int customerId) {
        Customer target = findById(customerId);
        if (target == null) {
            throw new IllegalArgumentException("Customer not found.");
        }
        return target.viewBookingHistory(orders);
    }
}
