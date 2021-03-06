package com.cbsexam;

import cache.UserCache;
import com.google.gson.Gson;
import controllers.UserController;
import java.util.ArrayList;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.User;
import utils.Encryption;
import utils.Log;

@Path("user")
public class UserEndpoints {

  public static UserCache userCache;
  private UserController userController;

  // Oprettes en constructor, og der laves et objekt af userCache og userController
  public UserEndpoints() {
    this.userCache = new UserCache();
    this.userController = new UserController();
  }

  /**
   * @param idUser
   * @return Responses
   */

  @GET
  @Path("/{idUser}")
  public Response getUser(@PathParam("idUser") int idUser) {

    // Use the ID to get the user from the controller.
    User user = UserController.getUser(idUser);

    // TODO: Add Encryption to JSON: FIXED
    // Convert the user object to json in order to return the object
    String json = new Gson().toJson(user);

    // Json formatet krypteres ved brug af XOR
    json = Encryption.encryptDecryptXOR(json);

    // Return the user with the status code 200
    // TODO: What should happen if something breaks down? : FIXED

    // Leder efter hvorvidt der findes en bruger med et tilhørende ID
    if (idUser != 0) {

      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();

    } else {

      // Rapporter en 404-error, idet der ikke ligger en user med et tilhørende ID
      return Response.status(404).entity("Could not find user").build();
    }
  }

  /** @return Responses */
  @GET
  @Path("/")
  public Response getUsers() {

    // Write to log that we are here
    Log.writeLog(this.getClass().getName(), this, "Get all users", 0);

    // Get a list of users
    // Forceupdate sættes til false i Cachen
    ArrayList<User> users = userCache.getUsers(false);

    // TODO: Add Encryption to JSON: FIXED
    // Transfer users to json in order to return it to the user
    String json = new Gson().toJson(users);

    // Json formatet krypteres ved brug af XOR
    json = Encryption.encryptDecryptXOR(json);

    // Return the users with the status code 200
    return Response.status(200).type(MediaType.APPLICATION_JSON).entity(json).build();
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createUser(String body) {

    // Read the json from body and transfer it to a user class
    User newUser = new Gson().fromJson(body, User.class);

    // Use the controller to add the user
    User createUser = UserController.createUser(newUser);

    // Get the user back with the added ID and return it to the user
    String json = new Gson().toJson(createUser);

    // Return the data to the user
    if (createUser != null) {
      // Return a response with status 200 and JSON as type

      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();

    } else {

      return Response.status(400).entity("Could not create user").build();
    }
  }

  // TODO: Make the system able to login users and assign them a token to use throughout the system.: FIXED
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response loginUser(String body) {

    // Indlæser Json formatet fra body, og videresender det til user klassen
    User user = new Gson().fromJson(body, User.class);

    // PHIL
    String token = userController.login(user);

    // Try-catch der indeholer response statuser, for hvorvidt brugeren kan logge ind eller ej
    try {

      if (token != null) {
        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(token).build();

      } else {
        // Returnerer et svar med status 400 hvis man ikke kan logge ind.
        return Response.status(400).entity("Could not login").build();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    // Returner null
    return null;
  }


  // TODO: Make the system able to delete users : FIXED
  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteUser(String token) {

    // Try-catch statement for hvorvidt det lykkedes at slette brugeren eller ej
    try {
      if (userController.delete(token)) {
        // Return a response with status 200 and JSON as type
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity("Brugeren med tilhørende token er nu slettet" + token).build();

      } else {
        // Returnerer et svar med status 400 hvis brugeren ikke kan slettes
        return Response.status(400).entity("Kunne ikke slette denne bruger").build();
      }

    } catch (Exception e1) {
      e1.printStackTrace();
    } 
    return null;
  }


}
