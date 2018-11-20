package utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.util.encoders.Hex;

import model.User;

public final class Hashing {

  //private String salt = "hiko";

  // TODO: You should add a salt and make this secure:
  public static String md5( String rawString ) {
    try {

      // We load the hashing algoritm we wish to use.
      MessageDigest md = MessageDigest.getInstance("MD5");

      // We convert to byte array
      byte[] byteArray = md.digest(rawString.getBytes());

      // Initialize a string buffer
      StringBuffer sb = new StringBuffer();

      // Run through byteArray one element at a time and append the value to our stringBuffer
      for (int i = 0; i < byteArray.length; ++i) {
        sb.append(Integer.toHexString((byteArray[i] & 0xFF) | 0x100).substring(1, 3));
      }

      //Convert back to a single string and return
      return sb.toString();

    } catch (java.security.NoSuchAlgorithmException e) {

      //If somethings breaks
      System.out.println("Could not hash string");
    }

    return null;
  }

  // TODO: You should add a salt and make this secure: FIXED
  //PHIL md5 er en gammel metode som er let at hacke/knække, derfor bruger vi sha
  public static String sha( String rawString ) {
    try {
      // We load the hashing algoritm we wish to use.
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      //PHIL - Saltet er gemt i Config-klassen, og dermed gemt mere sikkert
      String salt = Config.getHashWithSalt();

      //PHIL
      rawString = rawString + User.getCreatedTime() + salt;

      // We convert to byte array
      byte[] hash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));

      // We create the hashed string
      String sha256hex = new String(Hex.encode(hash));

      // And return the string
      return sha256hex;

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }

    return rawString;
  }
  //PHIL

  /*
  public String hashWithSalt( String string ) {
    String salt = string + this.salt;
    //PHIL md5 er en gammel metode som er let at hacke/knække
    return sha(salt);
  }
  //PHIL
  public void setSalt( String salt ) {
    this.salt = salt;
  } */

}