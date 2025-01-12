/*
Simple Web Server in Java which allows you to call
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a
little easier is used. This is done so you see exactly how to pars the request and
write a response back
*/

package funHttpServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

class WebServer {
  public static void main(String[] args) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   *
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private final Random random = new Random();
  private static final SecureRandom rnd = new SecureRandom();

  int length = 6; // default length for password generator

  /**
   * Reads in socket stream and generates a response
   *
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) throws IOException {


    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
          // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(new String(readFileInBytes(file)));
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          query_pairs = splitQuery(request.replace("multiply?", ""));

          if (!query_pairs.containsKey("num1") || !query_pairs.containsKey("num2")) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Both num1 and num2 parameters are required.");
          } else {
            try {
              Integer num1 = Integer.parseInt(query_pairs.get("num1"));
              Integer num2 = Integer.parseInt(query_pairs.get("num2"));

              Integer result = num1 * num2;

              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Result is: " + result);
            } catch (NumberFormatException e) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Both num1 and num2 must be valid integers.");
            }
          }
        } else if (request.contains("github?")) {
          try {
            // Pulls the query from the request and runs it with GitHub's REST API
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            query_pairs = splitQuery(request.replace("github?", ""));

            if (!query_pairs.containsKey("query") || query_pairs.get("query").isBlank()) {
              response = ("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: Query parameter is missing or blank.</html>").getBytes();
            } else {
              try {
                String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));

                // Check if the json is null or empty
                if (json == null || json.trim().isEmpty()) {
                  throw new IOException("Invalid response from GitHub API.");
                }

                // Start parsing JSON
                JSONArray jsonArray = new JSONArray(json);
                builder = new StringBuilder();

                for (int i = 0; i < jsonArray.length(); i++) {
                  JSONObject jsonObject = jsonArray.getJSONObject(i);

                  // Extract required details
                  String fullName = jsonObject.getString("full_name");
                  int id = jsonObject.getInt("id");
                  String ownerLogin = jsonObject.getJSONObject("owner").getString("login");

                  // Append details to builder
                  builder.append("Full Name: ").append(fullName).append("<br>");
                  builder.append("ID: ").append(id).append("<br>");
                  builder.append("Owner Login: ").append(ownerLogin).append("<br>");
                  builder.append("<hr>");
                }

                builder.insert(0, "HTTP/1.1 200 OK\nContent-Type: text/html; charset=utf-8\n\n");
                response = builder.toString().getBytes();

              } catch (MalformedURLException e) {
                response = ("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: Malformed URL - " + e.getMessage() + "</html>").getBytes();
              } catch (JSONException e) {
                response = ("HTTP/1.1 500 Internal Server Error\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: Invalid JSON format - " + e.getMessage() + "</html>").getBytes();
              } catch (IOException e) {
                response = ("HTTP/1.1 503 Service Unavailable\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: Could not reach GitHub API - " + e.getMessage() + "</html>").getBytes();
              } catch (UserNotFoundException e) {
                response = ("HTTP/1.1 404 Not Found\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: The requested user does not exist on GitHub.</html>").getBytes();
              } catch (BadRequestException e) {
                response = ("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: Bad request - " + e.getMessage() + "</html>").getBytes();
              }
            }
          } catch (Exception e) {
            // This is a catch-all for any other exceptions.
            response = ("HTTP/1.1 500 Internal Server Error\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: " + e.getMessage() + "</html>").getBytes();
          }
        }
        else if (request.contains("githubActivity?")) {
          // Pulls the user from the request and runs it with GitHub's REST API
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          query_pairs = splitQuery(request.replace("githubActivity?", ""));

          if (!query_pairs.containsKey("user") || query_pairs.get("user").isBlank()) {
            response = ("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: User parameter is missing or blank.</html>").getBytes();
          } else {
            try {
              String json = fetchURL("https://api.github.com/users/" + query_pairs.get("user") + "/events/public");

              // Start parsing JSON
              JSONArray jsonArray = new JSONArray(json);
              builder = new StringBuilder();

              DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
              DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);

              for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Extract required details
                String type = jsonObject.getString("type");
                String repoName = jsonObject.getJSONObject("repo").getString("name");

                // Extract and format date of event
                String eventDate = jsonObject.getString("created_at");
                LocalDateTime date = LocalDateTime.parse(eventDate, inputFormatter);
                String formattedDate = date.format(outputFormatter);

                // Append details to builder
                builder.append("Type: ").append(type).append("<br>");
                builder.append("Repo: ").append(repoName).append("<br>");
                builder.append("Date: ").append(formattedDate).append("<br>"); // use formatted date
                builder.append("<hr>");
              }

              builder.insert(0, "HTTP/1.1 200 OK\nContent-Type: text/html; charset=utf-8\n\n");

              response = builder.toString().getBytes();

            } catch (BadRequestException e) {
              response = ("HTTP/1.1 400 Bad Request\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: Bad request - " + e.getMessage() + "</html>").getBytes();
            } catch (UserNotFoundException e) {
              response = ("HTTP/1.1 404 Not Found\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: The requested user does not exist on GitHub.</html>").getBytes();
            } catch (JSONException | DateTimeParseException e) {
              response = ("HTTP/1.1 500 Internal Server Error\nContent-Type: text/html; charset=utf-8\n\n<html>ERROR: " + e.getMessage() + "</html>").getBytes();
            }
          }
        }

        else if (request.contains("pass?")) {
          try {
            // Pulls the parameters from the request
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            String queryString = request.replace("pass?", "");

            // Check if query parameters are correctly formatted
            if (queryString == null || queryString.equals("")) {
              throw new IllegalArgumentException();
            }

            query_pairs = passQuery(queryString);

            System.out.println("Query Pairs: " + query_pairs); // Debug print statement

            // Validate the length parameter
            boolean errorOccurred = false;
            try {
              String lengthString = query_pairs.get("l");
              if (lengthString == null || lengthString.equals("")) {
                length = 6; // Default length
              } else {
                length = Integer.parseInt(lengthString);
              }

              if (length < 6 || length > 24) {
                response = ("<html>ERROR: Invalid length parameter. It must be an integer between 6 and 24. Using default length 6.</html>").getBytes();
                length = 6; // Set a default value of 6
              }
            } catch (NumberFormatException nfe) {
              response = ("<html>ERROR: " + nfe.getMessage() + ". Using default length 6.</html>").getBytes();
              length = 6; // Set a default value of 6
              errorOccurred = true;
            }

            // Only proceed if no error has occurred
            if (!errorOccurred) {
              // Get the options parameter
              String opt = query_pairs.getOrDefault("OPT", "");

              // Create the character sets
              String lowerCase = "abcdefghijklmnopqrstuvwxyz";
              String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
              String specialCharacters = "!@#$%^&*()-_=+[]{}|;:,.<>?/~`";
              String numbers = "0123456789";

              // Remove character sets based on the options
              String passwordSet = lowerCase;
              if (!opt.contains("1")) passwordSet += upperCase;
              if (!opt.contains("2")) passwordSet += specialCharacters;
              if (!opt.contains("3")) passwordSet += numbers;

              // Check if passwordSet is empty, if so, return an error response
              if (passwordSet.isEmpty()) {
                response = ("<html>ERROR: No valid character set options were provided. Please use OPT parameter with valid values.</html>").getBytes();
              } else {
                // Generate the password
                StringBuilder password = new StringBuilder(length);
                for (int i = 0; i < length; i++) {
                  password.append(passwordSet.charAt(rnd.nextInt(passwordSet.length())));
                }

                // Create the HTTP response
                builder = new StringBuilder();
                builder.append("HTTP/1.1 200 OK\nContent-Type: text/plain; charset=utf-8\n\n");
                builder.append(password);

                response = builder.toString().getBytes();
              }
            }
          } catch (IllegalArgumentException iae) {
            response = ("<html>ERROR: Malformed query parameters. Please ensure your query parameters are correctly formatted.</html>").getBytes();
          }
        } else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }
        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   *
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
              URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Method to read in a password generator query and split it up correctly
   *
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> passQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      if (idx == -1 || idx + 1 >= pair.length()) continue;
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
    }
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   *
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte[] buffer = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * Updated to throw UserNotFoundException if the user is not found in github API
   * I've replaced URLConnection with HttpURLConnection. I've added a check for the HTTP response status code right
   * after the connection is opened. If the status code is HTTP_NOT_FOUND (which corresponds to a 404 status code),
   * a UserNotFoundException is thrown. If the status code is anything other than HTTP_OK (which corresponds to a 200 status code),
   * an IOException is thrown.
   *  added  handling for a 400 Bad Request error. If the status code is HTTP_BAD_REQUEST (which corresponds to a 400 status code),
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   **/
  public String fetchURL(String aUrl) throws IOException, UserNotFoundException, BadRequestException {
    StringBuilder sb = new StringBuilder();

    try {
      URL url = new URL(aUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds

      int status = conn.getResponseCode();

      // Handle specific HTTP response status codes
      if (status == HttpURLConnection.HTTP_NOT_FOUND) {
        throw new UserNotFoundException("User not found");
      } else if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
        throw new BadRequestException("Bad request");
      } else if (status != HttpURLConnection.HTTP_OK) {
        throw new IOException("Received HTTP error: " + status);
      }

      InputStreamReader in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
      BufferedReader br = new BufferedReader(in);
      if (br != null) {
        int ch;
        // read the next character until end of reader
        while ((ch = br.read()) != -1) {
          sb.append((char) ch);
        }
        br.close();
      }
      in.close();
    } catch (IOException ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
      throw ex;
    }
    return sb.toString();
  }
  public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) {
      super(message);
    }
  }
  public class BadRequestException extends Exception {
    public BadRequestException(String message) {
      super(message);
    }
  }
}