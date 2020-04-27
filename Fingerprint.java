import java.util.*;
import java.io.*;
import java.security.*;


public class Fingerprint{
  public static void main(String[] args) throws IOException, NoSuchAlgorithmException{
    File file = new File(args[0]);
    String f = file.toString();
    MessageDigest md5Digest = MessageDigest.getInstance("MD5");
    String checksum = getFileChecksum(md5Digest, f);
    System.out.println(checksum);
  }
  public static String getFileChecksum(MessageDigest digest, String str) throws IOException
{
    byte[] byteArr = str.getBytes();
    //Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;

    //Read file data and update in message digest
    int j=0;
    while ( j<byteArr.length && ((bytesCount = (byteArr[j]&0xff)) != -1)) {
      //  System.out.println(j);
      //  System.out.println(byteArr.length);
      //  System.out.println(bytesCount);
        digest.update(byteArray, 0, bytesCount);
        j++;
    };

    //close the stream; We don't need it now.

    //Get the hash's bytes
    byte[] bytes = digest.digest();

    //This bytes[] has bytes in decimal format;
    //Convert it to hexadecimal format
    StringBuilder sb = new StringBuilder();
    for(int i=0; i< bytes.length ;i++)
    {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }

    //return complete hash
   return sb.toString();
}
}
