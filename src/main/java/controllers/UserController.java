package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cbsexam.UserEndpoints;
import model.User;
import org.apache.solr.common.util.Hash;
import utils.Hashing;
import utils.Log;

public class UserController {

  private static DatabaseController dbCon;
  //Definerer token som en String
  private String token;

  public UserController() {
    dbCon = new DatabaseController();
    this.token = token;
  }

  public static User getUser(int id) {

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the query for DB
    String sql = "SELECT * FROM user where id=" + id;


    // Actually do the query
    ResultSet rs = dbCon.query(sql);
    User user = null;

    try {
      // Get first object, since we only have one
      if (rs.next()) {
        user =
            new User(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getLong("created_at"));

        // return the create object
        return user;
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return null
    return user;
  }

  /**
   * Get all users in database
   *
   * @return
   */
  public static ArrayList<User> getUsers() {

    // Check for DB connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL
    String sql = "SELECT * FROM user";

    // Do the query and initialyze an empty list for use if we don't get results
    ResultSet rs = dbCon.query(sql);
    ArrayList<User> users = new ArrayList<User>();

    try {
      // Loop through DB Data
      while (rs.next()) {
        User user =
            new User(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getLong("created_at"));

        // Add element to list
        users.add(user);
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return the list of users
    return users;
  }

  public static User createUser(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);

    // Set creation time for user.
    user.setCreatedTime(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }


    // TODO: Hash the user password before saving it.: FIXED

    // Insert the user in the DB
    int userID = dbCon.insert(
        "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES('"
            + user.getFirstname()
            + "', '"
            + user.getLastname()
            + "', '"
                // Hasher kodeordet inden den gemmes i databasen
            + Hashing.sha(user.getPassword())
            + "', '"
            + user.getEmail()
            + "', "
            + user.getCreatedTime()
            + ")");

    if (userID != 0) {
      //Update the userid of the user before returning
      user.setId(userID);
    } else{

      // Forceupdate sættes til true
      UserEndpoints.userCache.getUsers(true);

      // Return null if user has not been inserted into database
      return null;
    }

    // Return user
    return user;
  }

  public String login(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Login", 0);


    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Databasekald for login, hvor passwordet til login skal passe med det hashede password i databasen
    String sql = "SELECT * FROM user where email='" + user.getEmail() + "'AND password='" + Hashing.sha(user.getPassword()) +"'";


    // Actually do the query
    ResultSet rs = dbCon.query(sql);
    User loginUser;

    try {
      // Get first object, since we only have one
      if (rs.next()) {
        loginUser = new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getLong("created_at"));

          // Try catch hvor der JWT algoritmen benyttes
          try {

            Algorithm algorithm = Algorithm.HMAC256("secret");
             token = JWT.create()
                     // Tilføjer withClaim så man kan slette en bruger
                    .withIssuer("auth0").withClaim("userId",loginUser.id)
                    .sign(algorithm);

             // Token returneres
             return token;

          } catch (JWTCreationException exception){
            //Invalid Signing configuration / Couldn't convert Claims.
          }

        } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
      return ex.getMessage();

    }

    return null;
  }

  public boolean delete( String token) {
    DecodedJWT jwt = null;
    try {
      jwt  = JWT.decode(token);
    } catch (JWTDecodeException exception){
      //Invalid token
    }

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), null, "Delete user by ID", 0);

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }
    // Databasekald som sletter det userID, som passer med den token der er tildelt som claim i JWT
    String sql = "DELETE FROM user WHERE id=" + jwt.getClaim("userId").asInt();

    int i = dbCon.insert(sql);

    if (i == 1) {
      return true;
    } else
      return false;
  }


}
