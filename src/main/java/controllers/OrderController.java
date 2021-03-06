package controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.cbsexam.OrderEndpoints;
import model.Address;
import model.LineItem;
import model.Order;
import model.User;
import utils.Log;

public class OrderController {

  private static DatabaseController dbCon;

  public OrderController() {
    dbCon = new DatabaseController();
  }

  public static Order getOrder( int id ) {

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Databasekald til at undgå nested queries
    String sql = "SELECT *,\n" +
            "            billing.street_address as billing,\n" +
            "            shipping.street_address as shipping\n" +
            "            FROM orders\n" +
            "            JOIN user ON orders.user_id = user.id\n" +
            "            JOIN address AS billing ON orders.billing_address_id = billing.id\n" +
            "            JOIN address AS shipping ON orders.shipping_address_id = shipping.id WHERE orders.id =" + id;




    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);
    Order order = null;

    try {
      if (rs.next()) {

        // TODO: Perhaps we could optimize things a bit here and get rid of nested queries. : FIXED
        ArrayList<LineItem> lineItems = LineItemController.getLineItemsForOrder(rs.getInt("id"));

        // Create an object instance of user from the database data
        User user = new User(
                rs.getInt("user_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getLong("created_at"));

        // Create an object instance of address from the database data
        Address billingAddress = new Address(
                rs.getInt("billing_address_id"),
                rs.getString("name"),
                rs.getString("billing"),
                rs.getString("city"),
                rs.getString("zipcode"));

        // Create an object instance of address from the database data
        Address shippingAddress = new Address(
                rs.getInt("shipping_address_id"),
                rs.getString("name"),
                rs.getString("shipping"),
                rs.getString("city"),
                rs.getString("zipcode"));

        // Create an object instance of order from the database data
        order =
                new Order(
                        rs.getInt("id"),
                        user,
                        lineItems,
                        billingAddress,
                        shippingAddress,
                        rs.getFloat("order_total"),
                        rs.getLong("created_at"),
                        rs.getLong("updated_at"));

        // Returns the build order
        return order;
      } else {
        System.out.println("No order found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Returns null
    return order;
  }

  /**
   * Get all orders in database
   *
   * @return
   */
  public static ArrayList<Order> getOrders() {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    //Database kald for at undgå nested queries
    String sql = "SELECT *,\n" +
            "            billing.street_address as billing,\n" +
            "            shipping.street_address as shipping\n" +
            "            FROM orders\n" +
            "            JOIN user ON user.id = orders.user_id\n" +
            "            JOIN address AS billing ON orders.billing_address_id = billing.id\n" +
            "            JOIN address AS shipping ON orders.shipping_address_id = shipping.id";

    ResultSet rs = dbCon.query(sql);
    ArrayList<Order> orders = new ArrayList<Order>();

    try {
      while (rs.next()) {

        // TODO: Perhaps we could optimize things a bit here and get rid of nested queries. - FIXED
        // Create an object instance of user from the database data
        User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getLong("created_at"));

        ArrayList<LineItem> lineItems = LineItemController.getLineItemsForOrder(rs.getInt("id"));

        // Create an object instance of address from the database data
        Address billingAddress = new Address(
                        rs.getInt("billing_address_id"),
                        rs.getString("name"),
                        rs.getString("billing"),
                        rs.getString("city"),
                        rs.getString("zipcode"));

        // Create an object instance of address from the database data
        Address shippingAddress = new Address(
                rs.getInt("shipping_address_id"),
                rs.getString("name"),
                rs.getString("shipping"),
                rs.getString("city"),
                rs.getString("zipcode"));


        // Create an order from the database data
        Order order =
                new Order(
                        rs.getInt("id"),
                        user,
                        lineItems,
                        billingAddress,
                        shippingAddress,
                        rs.getFloat("order_total"),
                        rs.getLong("created_at"),
                        rs.getLong("updated_at"));

        // Add order to our list
        orders.add(order);

      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // return the orders
    return orders;
  }

  public static Order createOrder( Order order ) {

    // Write in log that we've reach this step
    Log.writeLog(OrderController.class.getName(), order, "Actually creating a order in DB", 0);

    // Set creation and updated time for order.
    order.setCreatedAt(System.currentTimeMillis() / 1000L);
    order.setUpdatedAt(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Save addresses to database and save them back to initial order instance
    order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
    order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

    // Save the user to the database and save them back to initial order instance
    order.setCustomer(UserController.createUser(order.getCustomer()));

    // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts.: FIXED


    Connection connection = null;

    try {
      //Auto-commit mode slås fra, så der kan udføres transaktioner
      connection.setAutoCommit(false);

      // Insert the product in the DB
      int orderID = dbCon.insert(
              "INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, created_at, updated_at) VALUES("
                      + order.getCustomer().getId()
                      + ", "
                      + order.getBillingAddress().getId()
                      + ", "
                      + order.getShippingAddress().getId()
                      + ", "
                      + order.calculateOrderTotal()
                      + ", "
                      + order.getCreatedAt()
                      + ", "
                      + order.getUpdatedAt()
                      + ")");

      if (orderID != 0) {
        //Update the productid of the product before returning
        order.setId(orderID);
      }

      // Create an empty list in order to go trough items and then save them back with ID
      ArrayList<LineItem> items = new ArrayList<LineItem>();

      // Save line items to database
      for (LineItem item : order.getLineItems()) {
        item = LineItemController.createLineItem(item, order.getId());
        items.add(item);
      }

      order.setLineItems(items);

      //Der committes hvis orden er lykkedes
      connection.commit();

    } catch (SQLException e) {
      try {
        connection.rollback();

        System.out.println("Transaktionen foretager rollback");
      } catch (SQLException e1) {

        //Hvis ordren ikke er lykkedes, "roller" den tilbage
        System.out.println("Transaktionen foretager ikke rollback" + e1.getMessage());
      } finally {
        //AutoCommit sættes tilbage til true, så hver statement igen committes automatisk, når transaktioner er færdiggjort.
        try {
          connection.setAutoCommit(true);
        } catch (SQLException e2) {
          e2.printStackTrace();
        }
      }
    }

    //Return order
    return order;
  }
}
