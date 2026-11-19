import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class OrderApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root"; // Update if your MySQL password is different

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to ecommerce_db successfully!");

            while (true) {
                System.out.println("\n=== E-COMMERCE ORDER MANAGEMENT SYSTEM ===");
                System.out.println("1. Insert Order Records (At least 3)");
                System.out.println("2. Retrieve Orders by Status (Quantity > 2, Sorted by Unit Price DESC)");
                System.out.println("3. Increase Order Quantity");
                System.out.println("4. Search & Update Order Status");
                System.out.println("5. Total Order Value & Conditional Deletion (Status = Cancelled)");
                System.out.println("6. Exit");
                System.out.print("Enter your choice (1-6): ");

                int choice = sc.nextInt();
                sc.nextLine(); // Clear newline buffer

                switch (choice) {
                    case 1 -> insertOrders(conn, sc);
                    case 2 -> retrieveAndSortOrders(conn, sc);
                    case 3 -> increaseOrderQuantity(conn, sc);
                    case 4 -> searchAndUpdateStatus(conn, sc);
                    case 5 -> totalValueAndConditionalDelete(conn, sc);
                    case 6 -> {
                        System.out.println("Exiting Order Management System...");
                        return;
                    }
                    default -> System.out.println("Invalid choice! Please enter 1-6.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Connection Error: " + e.getMessage());
        }
    }

    // (a) Insert at least 3 Order Records & handle duplicate order IDs (2 Marks)
    private static void insertOrders(Connection conn, Scanner sc) {
        System.out.print("How many orders do you want to insert (minimum 3)? ");
        int count = sc.nextInt();
        sc.nextLine();

        if (count < 3) {
            System.out.println("At least 3 order records are required. Setting count to 3.");
            count = 3;
        }

        String insertSQL = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (int i = 1; i <= count; i++) {
                System.out.println("\n--- Enter Details for Order #" + i + " ---");
                System.out.print("Order ID: ");
                int orderId = sc.nextInt();
                sc.nextLine();

                System.out.print("Customer Name: ");
                String customerName = sc.nextLine();

                System.out.print("Product Name: ");
                String productName = sc.nextLine();

                System.out.print("Quantity: ");
                int quantity = sc.nextInt();

                System.out.print("Unit Price: ");
                double unitPrice = sc.nextDouble();
                sc.nextLine();

                System.out.print("Status (Pending / Shipped / Delivered / Cancelled): ");
                String status = sc.nextLine().trim();

                pstmt.setInt(1, orderId);
                pstmt.setString(2, customerName);
                pstmt.setString(3, productName);
                pstmt.setInt(4, quantity);
                pstmt.setDouble(5, unitPrice);
                pstmt.setString(6, status);

                try {
                    pstmt.executeUpdate();
                    System.out.println("SUCCESS: Order ID " + orderId + " inserted.");
                } catch (SQLIntegrityConstraintViolationException dupEx) {
                    System.out.println("DUPLICATE ERROR: Order ID " + orderId + " already exists! Skipping duplicate record.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Insert Error: " + e.getMessage());
        }
    }

    // (b) Retrieve orders with given status and quantity > 2, sorted by unit_price DESC (2 Marks)
    private static void retrieveAndSortOrders(Connection conn, Scanner sc) {
        System.out.print("Enter Order Status to filter: ");
        String status = sc.nextLine().trim();

        String query = "SELECT * FROM Orders WHERE LOWER(status) = LOWER(?) AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Orders with Status '" + status + "' and Quantity > 2 (Sorted by Unit Price DESC) ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Customer: %s | Product: %s | Qty: %d | Unit Price: %.2f | Status: %s%n",
                        rs.getInt("order_id"),
                        rs.getString("customer_name"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price"),
                        rs.getString("status"));
            }
            if (!found) {
                System.out.println("No orders found with status '" + status + "' and quantity > 2.");
            }
        } catch (SQLException e) {
            System.err.println("Retrieval Error: " + e.getMessage());
        }
    }

    // (c) Increase existing quantity of specified order (2 Marks)
    private static void increaseOrderQuantity(Connection conn, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int orderId = sc.nextInt();
        System.out.print("Enter Additional Quantity to add: ");
        int addQty = sc.nextInt();
        sc.nextLine();

        String updateSQL = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, addQty);
            pstmt.setInt(2, orderId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("SUCCESS: Quantity updated for Order ID " + orderId + ".");
            } else {
                System.out.println("NOT FOUND: Order ID " + orderId + " does not exist.");
            }
        } catch (SQLException e) {
            System.err.println("Update Quantity Error: " + e.getMessage());
        }
    }

    // (d) Retrieve order details and update status only if order exists & new status is valid (2 Marks)
    private static void searchAndUpdateStatus(Connection conn, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int orderId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter New Status (Pending / Shipped / Delivered): ");
        String newStatus = sc.nextLine().trim();

        List<String> validStatuses = Arrays.asList("Pending", "Shipped", "Delivered");
        String normalizedStatus = null;
        for (String valid : validStatuses) {
            if (valid.equalsIgnoreCase(newStatus)) {
                normalizedStatus = valid;
                break;
            }
        }

        if (normalizedStatus == null) {
            System.out.println("INVALID STATUS: Status must be Pending, Shipped, or Delivered.");
            return;
        }

        String selectSQL = "SELECT * FROM Orders WHERE order_id = ?";
        String updateSQL = "UPDATE Orders SET status = ? WHERE order_id = ?";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
            selectStmt.setInt(1, orderId);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("NOT FOUND: Order ID " + orderId + " does not exist.");
                return;
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                updateStmt.setString(1, normalizedStatus);
                updateStmt.setInt(2, orderId);
                updateStmt.executeUpdate();
            }

            // Display updated order details
            ResultSet updatedRs = selectStmt.executeQuery();
            if (updatedRs.next()) {
                System.out.println("\n--- Updated Order Details ---");
                System.out.printf("ID: %d | Customer: %s | Product: %s | Qty: %d | Unit Price: %.2f | Updated Status: %s%n",
                        updatedRs.getInt("order_id"),
                        updatedRs.getString("customer_name"),
                        updatedRs.getString("product_name"),
                        updatedRs.getInt("quantity"),
                        updatedRs.getDouble("unit_price"),
                        updatedRs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("Status Update Error: " + e.getMessage());
        }
    }

    // (e) Display total order value using SUM(quantity * unit_price) & delete only if status is Cancelled (2 Marks)
    private static void totalValueAndConditionalDelete(Connection conn, Scanner sc) {
        String sumSQL = "SELECT SUM(quantity * unit_price) AS total_order_value FROM Orders";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sumSQL)) {
            if (rs.next()) {
                System.out.printf("\nTotal Order Value: %.2f%n", rs.getDouble("total_order_value"));
            }
        } catch (SQLException e) {
            System.err.println("Aggregate Query Error: " + e.getMessage());
        }

        System.out.print("Enter Order ID to delete (will only delete if status is Cancelled): ");
        int orderId = sc.nextInt();
        sc.nextLine();

        String checkSQL = "SELECT status FROM Orders WHERE order_id = ?";
        String deleteSQL = "DELETE FROM Orders WHERE order_id = ? AND LOWER(status) = 'cancelled'";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
            checkStmt.setInt(1, orderId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("CANNOT DELETE: Order ID " + orderId + " does not exist.");
                return;
            }

            String currentStatus = rs.getString("status");
            if (!"Cancelled".equalsIgnoreCase(currentStatus)) {
                System.out.println("CANNOT DELETE: Order ID " + orderId + " has status '" + currentStatus + "' (only Cancelled orders can be deleted).");
                return;
            }

            try (PreparedStatement delStmt = conn.prepareStatement(deleteSQL)) {
                delStmt.setInt(1, orderId);
                if (delStmt.executeUpdate() > 0) {
                    System.out.println("SUCCESS: Cancelled Order ID " + orderId + " deleted.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Conditional Deletion Error: " + e.getMessage());
        }
    }
}