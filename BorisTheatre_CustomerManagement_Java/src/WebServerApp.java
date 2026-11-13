import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServerApp {
    private static final CustomerService service = new CustomerService();
    private static Customer loggedInCustomer = null;

    public static void main(String[] args) throws IOException {
        int port = 8090;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/static/style.css", exchange -> {
            byte[] css = Files.readAllBytes(Path.of("static/style.css"));
            exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
            exchange.sendResponseHeaders(200, css.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(css); }
        });

        server.createContext("/", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendRedirect(exchange, "/");
                return;
            }
            sendHtml(exchange, renderTemplate("templates/index.html", Map.of()));
        });

        server.createContext("/register", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseFormData(exchange);
                try {
                    Customer created = service.registerCustomer(
                            form.get("fullName"),
                            form.get("emailAddress"),
                            form.get("phoneNumber")
                    );
                    loggedInCustomer = created;
                    sendRedirect(exchange, "/dashboard?msg=Account+registered+successfully");
                } catch (IllegalArgumentException e) {
                    Map<String, String> model = Map.of(
                            "error", e.getMessage(),
                            "fullName", escapeHtml(form.getOrDefault("fullName", "")),
                            "emailAddress", escapeHtml(form.getOrDefault("emailAddress", "")),
                            "phoneNumber", escapeHtml(form.getOrDefault("phoneNumber", ""))
                    );
                    sendHtml(exchange, renderTemplate("templates/register.html", model));
                }
            } else {
                sendHtml(exchange, renderTemplate("templates/register.html", Map.of(
                        "error", "", "fullName", "", "emailAddress", "", "phoneNumber", ""
                )));
            }
        });

        server.createContext("/login", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseFormData(exchange);
                Customer found = service.loginByEmail(form.getOrDefault("emailAddress", ""));
                if (found != null) {
                    loggedInCustomer = found;
                    sendRedirect(exchange, "/dashboard");
                } else {
                    sendHtml(exchange, renderTemplate("templates/login.html",
                            Map.of("error", "No customer account found with that email address.")));
                }
            } else {
                sendHtml(exchange, renderTemplate("templates/login.html", Map.of("error", "")));
            }
        });

        server.createContext("/logout", exchange -> {
            loggedInCustomer = null;
            sendRedirect(exchange, "/");
        });

        server.createContext("/dashboard", exchange -> {
            if (loggedInCustomer == null) {
                sendRedirect(exchange, "/login");
                return;
            }
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            List<Order> orders = service.getPurchaseHistory(loggedInCustomer.getCustomerID());
            StringBuilder orderRows = new StringBuilder();
            if (orders.isEmpty()) {
                orderRows.append("<tr><td colspan='4'>No purchase history found.</td></tr>");
            } else {
                for (Order o : orders) {
                    orderRows.append(String.format("<tr><td>#%d</td><td>%s</td><td>£%.2f</td><td>%s</td></tr>",
                            o.getOrderID(), o.getOrderDate(), o.getTotalAmount(), escapeHtml(o.getOrderStatus())));
                }
            }

            Map<String, String> model = new HashMap<>();
            model.put("message", escapeHtml(query.getOrDefault("msg", "")));
            model.put("customerID", String.valueOf(loggedInCustomer.getCustomerID()));
            model.put("fullName", escapeHtml(loggedInCustomer.getFullName()));
            model.put("emailAddress", escapeHtml(loggedInCustomer.getEmailAddress()));
            model.put("phoneNumber", escapeHtml(loggedInCustomer.getPhoneNumber().isEmpty() ? "N/A" : loggedInCustomer.getPhoneNumber()));
            model.put("dateJoined", loggedInCustomer.getDateJoined().toString());
            model.put("loyaltyStatus", loggedInCustomer.isLoyaltyMember() ? "Active Loyalty Member" : "Standard Member");
            model.put("orderRows", orderRows.toString());

            sendHtml(exchange, renderTemplate("templates/dashboard.html", model));
        });

        server.createContext("/edit-account", exchange -> {
            if (loggedInCustomer == null) {
                sendRedirect(exchange, "/login");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseFormData(exchange);
                try {
                    service.updateCustomer(
                            loggedInCustomer.getCustomerID(),
                            form.get("fullName"),
                            form.get("emailAddress"),
                            form.get("phoneNumber"),
                            null
                    );
                    sendRedirect(exchange, "/dashboard?msg=Personal+details+updated");
                } catch (IllegalArgumentException e) {
                    Map<String, String> model = Map.of(
                            "error", e.getMessage(),
                            "fullName", escapeHtml(form.getOrDefault("fullName", "")),
                            "emailAddress", escapeHtml(form.getOrDefault("emailAddress", "")),
                            "phoneNumber", escapeHtml(form.getOrDefault("phoneNumber", ""))
                    );
                    sendHtml(exchange, renderTemplate("templates/edit_account.html", model));
                }
            } else {
                Map<String, String> model = Map.of(
                        "error", "",
                        "fullName", escapeHtml(loggedInCustomer.getFullName()),
                        "emailAddress", escapeHtml(loggedInCustomer.getEmailAddress()),
                        "phoneNumber", escapeHtml(loggedInCustomer.getPhoneNumber())
                );
                sendHtml(exchange, renderTemplate("templates/edit_account.html", model));
            }
        });

        server.createContext("/admin/customers", exchange -> {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            String search = query.getOrDefault("search", "").trim();
            String loyalty = query.getOrDefault("loyalty", "all");

            List<Customer> list = search.isEmpty() ? service.getAllCustomers() : service.searchCustomers(search);
            if ("true".equals(loyalty)) {
                list = list.stream().filter(Customer::isLoyaltyMember).toList();
            } else if ("false".equals(loyalty)) {
                list = list.stream().filter(c -> !c.isLoyaltyMember()).toList();
            }

            List<Customer> all = service.getAllCustomers();
            long loyaltyCount = all.stream().filter(Customer::isLoyaltyMember).count();
            long todayCount = all.stream().filter(c -> c.getDateJoined().equals(LocalDate.now())).count();

            StringBuilder rows = new StringBuilder();
            if (list.isEmpty()) {
                rows.append("<tr><td colspan='7'>No customers found.</td></tr>");
            } else {
                for (Customer c : list) {
                    rows.append(String.format(
                        "<tr>" +
                        "<td>%d</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>" +
                        "<td class='actions'>" +
                        "<a class='btn-sm' href='/admin/edit?id=%d'>Edit</a> " +
                        "<a class='btn-sm' href='/admin/history?id=%d'>Orders</a> " +
                        "<a class='btn-sm danger' href='/admin/delete?id=%d'>Close</a>" +
                        "</td></tr>",
                        c.getCustomerID(),
                        escapeHtml(c.getFullName()),
                        escapeHtml(c.getEmailAddress()),
                        escapeHtml(c.getPhoneNumber().isEmpty() ? "N/A" : c.getPhoneNumber()),
                        c.getDateJoined(),
                        c.isLoyaltyMember() ? "Yes" : "No",
                        c.getCustomerID(), c.getCustomerID(), c.getCustomerID()
                    ));
                }
            }

            Map<String, String> model = new HashMap<>();
            model.put("message", escapeHtml(query.getOrDefault("msg", "")));
            model.put("error", escapeHtml(query.getOrDefault("err", "")));
            model.put("totalCustomers", String.valueOf(all.size()));
            model.put("loyaltyCount", String.valueOf(loyaltyCount));
            model.put("todayCount", String.valueOf(todayCount));
            model.put("search", escapeHtml(search));
            model.put("selectedAll", "all".equals(loyalty) ? "selected" : "");
            model.put("selectedTrue", "true".equals(loyalty) ? "selected" : "");
            model.put("selectedFalse", "false".equals(loyalty) ? "selected" : "");
            model.put("customerRows", rows.toString());

            sendHtml(exchange, renderTemplate("templates/admin_customers.html", model));
        });

        server.createContext("/admin/edit", exchange -> {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            int id = Integer.parseInt(query.getOrDefault("id", "0"));
            Customer target = service.findById(id);
            if (target == null) {
                sendRedirect(exchange, "/admin/customers?err=Customer+not+found");
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> form = parseFormData(exchange);
                try {
                    boolean loyalty = "on".equals(form.get("isLoyaltyMember"));
                    service.updateCustomer(id, form.get("fullName"), form.get("emailAddress"), form.get("phoneNumber"), loyalty);
                    sendRedirect(exchange, "/admin/customers?msg=Customer+record+updated");
                } catch (IllegalArgumentException e) {
                    Map<String, String> model = Map.of(
                            "error", e.getMessage(),
                            "customerID", String.valueOf(id),
                            "fullName", escapeHtml(form.getOrDefault("fullName", "")),
                            "emailAddress", escapeHtml(form.getOrDefault("emailAddress", "")),
                            "phoneNumber", escapeHtml(form.getOrDefault("phoneNumber", "")),
                            "loyaltyChecked", "on".equals(form.get("isLoyaltyMember")) ? "checked" : ""
                    );
                    sendHtml(exchange, renderTemplate("templates/admin_edit_customer.html", model));
                }
            } else {
                Map<String, String> model = Map.of(
                        "error", "",
                        "customerID", String.valueOf(target.getCustomerID()),
                        "fullName", escapeHtml(target.getFullName()),
                        "emailAddress", escapeHtml(target.getEmailAddress()),
                        "phoneNumber", escapeHtml(target.getPhoneNumber()),
                        "loyaltyChecked", target.isLoyaltyMember() ? "checked" : ""
                );
                sendHtml(exchange, renderTemplate("templates/admin_edit_customer.html", model));
            }
        });

        server.createContext("/admin/history", exchange -> {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            int id = Integer.parseInt(query.getOrDefault("id", "0"));
            Customer target = service.findById(id);
            if (target == null) {
                sendRedirect(exchange, "/admin/customers?err=Customer+not+found");
                return;
            }
            List<Order> orders = service.getPurchaseHistory(id);
            StringBuilder rows = new StringBuilder();
            if (orders.isEmpty()) {
                rows.append("<tr><td colspan='4'>No purchase history for this customer.</td></tr>");
            } else {
                for (Order o : orders) {
                    rows.append(String.format("<tr><td>#%d</td><td>%s</td><td>£%.2f</td><td>%s</td></tr>",
                            o.getOrderID(), o.getOrderDate(), o.getTotalAmount(), escapeHtml(o.getOrderStatus())));
                }
            }
            Map<String, String> model = Map.of(
                    "customerID", String.valueOf(target.getCustomerID()),
                    "fullName", escapeHtml(target.getFullName()),
                    "emailAddress", escapeHtml(target.getEmailAddress()),
                    "orderRows", rows.toString()
            );
            sendHtml(exchange, renderTemplate("templates/purchase_history.html", model));
        });

        server.createContext("/admin/delete", exchange -> {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            int id = Integer.parseInt(query.getOrDefault("id", "0"));
            try {
                service.deleteCustomer(id);
                sendRedirect(exchange, "/admin/customers?msg=Customer+record+deleted");
            } catch (IllegalStateException e) {
                service.anonymizeCustomer(id);
                sendRedirect(exchange, "/admin/customers?msg=Customer+had+linked+orders.+Account+anonymized+to+preserve+records.");
            } catch (Exception e) {
                sendRedirect(exchange, "/admin/customers?err=" + e.getMessage().replace(" ", "+"));
            }
        });

        server.createContext("/admin/export", exchange -> {
            StringBuilder csv = new StringBuilder("CustomerID,FullName,EmailAddress,PhoneNumber,DateJoined,IsLoyaltyMember\n");
            for (Customer c : service.getAllCustomers()) {
                csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%s,%b\n",
                        c.getCustomerID(), c.getFullName(), c.getEmailAddress(),
                        c.getPhoneNumber(), c.getDateJoined(), c.isLoyaltyMember()));
            }
            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"boris_theatre_customers.csv\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Boris Theatre Customer Management System running at: http://localhost:" + port);
    }

    private static String renderTemplate(String path, Map<String, String> model) throws IOException {
        String html = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entry : model.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return html;
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseQueryParams(body);
    }

    private static Map<String, String> parseQueryParams(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, val);
        }
        return map;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
