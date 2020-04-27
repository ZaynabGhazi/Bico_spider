import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Bico_spider{

  private static final String USER_AGENT= "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537.36";
  private static final String BMC_HOST = "brynmawr.edu";
  private static final String HC_HOST = "haverford.edu";
  private static final String robot_EP = "/robots.txt";
  private static final String no = "Disallow:";

  private class URL_MD{

    private String url;
    private String last_updated;
    private String size;

    private URL_MD(String url, String last_updated,String size){
      this.url=url;
      this.last_updated=last_updated;
      this.size=size;
    }

    private String get_url(){ return this.url;}
    private String get_last_updated(){return this.last_updated;}
    private String size() {return this.size;}

    public String toString(){
      return "***\n"+this.url+"\n"+this.last_updated+"\n"+this.size+"\n";
    }
  }//URL_MD__class_end
  public Bico_spider() throws IOException{
    logger= new FileWriter("bmc_crawl.log");
  }

  //main DS for the crawler:
  public HashMap<String, URL_MD> seen_pages = new HashMap<>();
  public HashSet<String> seen_fingerprints = new HashSet<String>();
  public LinkedList<String> url_frontier = new LinkedList<>();
  public HashMap<String,ArrayList<String>> forbidden_paths = new HashMap<>();
  public FileWriter logger;

  //Recording:


  public boolean crawl_page(String url) throws NoSuchAlgorithmException,IOException{
    try{
      Connection con = Jsoup.connect(url).userAgent(USER_AGENT);
      Document html_doc = con.get();
      //200 is safe HTTP status code
      if (con.response().statusCode() == 200) System.out.println("Connection established successfully to "+url);
      if (con.response().contentType() == null) return false;
      if (!con.response().contentType().contains("text/html")){
        System.out.println("No HTML content retrieved.");
        logger.write("***\n"+url+"\nThere is no html content to be fetched from this content.\n");
        logger.flush();
        return false;
      }
      //content-seen check:
      //MessageDigest md5Digest = MessageDigest.getInstance("MD5");
      MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");
      //String fingerprint = Fingerprint.getFileChecksum(md5Digest, html_doc.toString());
      String fingerprint = Fingerprint.getFileChecksum(shaDigest, html_doc.toString());
      if (seen_fingerprints.contains(fingerprint)) return false;
      else{
        seen_fingerprints.add(fingerprint);
      }
      //add to seen_urls:
      Connection.Response rs = Jsoup.connect(url).ignoreContentType(true).execute();
      URL_MD current = new URL_MD(url,rs.header("Last-Modified")==null?"Last-modified data not available":rs.header("Last-Modified"),rs.header("Content-Length")==null?"Size data not available.":rs.header("Content-Length"));
      seen_pages.put(current.get_url().trim(),current);
      logger.write(current.toString());
      logger.flush();


      //System.out.println(rs.header("Last-Modified"));
      //fetch urls from html:
      Elements in_urls = html_doc.select("a[href]");
      //filter urls before adding to url_frontier
      for(Element link: in_urls){
        String sub_url = link.absUrl("href").toString();
        if(sub_url != null){
        if (sub_url.toString().length()!=0 && filter_url(sub_url.toString()) && !url_frontier.contains(sub_url.toString())){
            //System.out.println(sub_url);
            url_frontier.add(sub_url.toString());
        }
      }
      }
      return true;
    }
    catch(IOException e){return false;}
  }

  public boolean filter_url(String url){
    URL urll= null;
    Boolean allowed = true;
    //System.out.println("PROBLEMO "+url);
    try {
       urll = new URL(url);
    }
    catch(MalformedURLException e){
      System.out.println("Malformed URL ignored."+url);
    }
    String host = urll.getHost();
    //perform set of checks:
    //if (!host.contains (BMC_HOST) && !host.contains(HC_HOST)) return false;
    if (!host.contains (BMC_HOST)) return false;
    System.out.println(seen_pages.containsKey(url.trim())+" "+url+"\n");
    if (seen_pages.containsKey(url.trim())) return false;
    //robot exclusion protocol:
    String path = urll.getPath();
    if(forbidden_paths.containsKey(host)){
      ArrayList forbidden = forbidden_paths.get(host);
      for(int j=0; j<forbidden.size();j++){
        if (forbidden.get(j).equals(path) || forbidden.get(j).equals("/")) return false;
      }
    }
      else{
        String robot_url = urll.getProtocol()+"://"+host+robot_EP;
        allowed = parse_robotTXT(robot_url,path);
      }
      return allowed;
    }


    public boolean parse_robotTXT(String txtFile, String path){
      try{
      //  System.out.println("ROBOT LINK"+txtFile);
      //  Connection con = Jsoup.connect(txtFile).followRedirects(true);
      //  con.request().removeHeader("User-Agent");
      String html = Jsoup.connect(txtFile).get().html();
        //System.out.println(con.response().statusCode());
        //System.out.println(con.response().contentType() == null);
      //  Document html_doc = con.get();
      //  if (con.response().statusCode() == 200 && con.response().contentType().contains("text/html")) {
      //  String robot_file = html_doc.toString();
        String robot_file = html;
        if (!robot_file.contains(no)) return true;
        boolean self_found=false;
        URL robotxt = new URL(txtFile);
        String host = robotxt.getHost();
        ArrayList<String> privateL = new ArrayList<>();
          String[] rules = robot_file.split("\n");
          //PARSING LOOP
          for(int k=0; k< rules.length;k++){
            String line = rules[k].trim().toLowerCase();
            if (line.startsWith("user_agent")){
              String usr = line.substring(line.indexOf(":")+1);
              if (usr.contains(USER_AGENT) || usr.contains("*")){
                self_found = true;
              }
              else{
                self_found=false;
              }
            }//usrag
          if (line.startsWith(no) && self_found){
            String pathR = line.substring(line.indexOf(":")+1);
            if (pathR.length() == 0) return true;
            if (pathR.equals("/")){
              privateL.add("/");
              return false;
            }
            else{
              privateL.add(pathR.trim());
            }
          }
        }//loop-rules-end
        if (privateL.size()!=0) forbidden_paths.put(host,privateL);
        if (privateL.contains(path)) return false;
      //}//if_connected_successful
    }//try
    catch (IOException e) {return true;}
    return true;
  }//METHOD_END



public static void main(String[] args) throws NoSuchAlgorithmException,IOException{
  Bico_spider spider_bot = new Bico_spider();
  String current = "https://www.brynmawr.edu";
  /*int depth=0;
  while (depth <= Integer.parseInt(args[0])){
    spider_bot.crawl_page(current);
    System.out.println("Currentling crawling "+current);
    current = spider_bot.url_frontier.pollFirst();
    depth++;
  }*/
  spider_bot.url_frontier.addFirst(current);
  while(!spider_bot.url_frontier.isEmpty()){
    System.out.println("Currentling crawling "+spider_bot.url_frontier.peek());
    spider_bot.crawl_page(spider_bot.url_frontier.pollFirst());
    //current = spider_bot.url_frontier.pollFirst();
  }

  spider_bot.logger.close();
  //collect data
  FileWriter data_logger = new FileWriter("data.log");
  data_logger.write("The number of unique fingerprints is "+spider_bot.seen_fingerprints.size()+"\n");
  data_logger.write("The number of unique forbidden paths is "+spider_bot.forbidden_paths.size()+"\n");
  data_logger.write("The number of seen urls is "+spider_bot.seen_pages.size()+"\n");
  data_logger.write("The number of urls is "+spider_bot.url_frontier.size()+"\n");
  data_logger.close();



}

}
