// Java client program for ANU's comp3310 sockets Lab
// Peter Strazdins, RSCS ANU, 03/18

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    /* The main method is used to send a request to the given URL:http://comp3310.dnns.net:7880. Received information is stored a
       HashMap<String,Object[]): the key is the string of paths got from the given URL, the value is the properties about the path. All received
       unique link is store in an ArrayList<String> list. By for-loop each string in the arraylist, all non-external path is crawled. (Note,
       during the loop, if new unique link is founded, it will be added to the list which makes sure all the found links are crawled.) After
       crawling all relevant links, the main method all different methods (like findHTMLpage, findNonhtml..) to print the answers for the question.
     */
    public static void main (String[] args) throws IOException, ParseException, InterruptedException {
        URL url = new URL("http://comp3310.ddns.net:7880");
        int port = url.getPort();
        String host = url.getHost();
        /* The list is used to store all unique links. */
        ArrayList<String> list = new ArrayList<>();
        /* The hashmap is used to store all crawled information.*/
        HashMap<String,Object[]> map = new HashMap<>();
        /* Firstly, main method send the request to the given URL and the default path "/" should be add to the list. */
        list.add("/");
        sendRequest(host,"/",port,list,map);
        /* Make sure no more than 1 request is sent per 2 seconds. */
        Thread.sleep(2000);
        /* No external link need to be crawled. So a regular expression is used for determining whether a link is external.
        If a link with http://host:port/path pattern, check the host information, if the host is not the given host, then the link
        is external. If a link doesn't match the pattern, it is seen as a host path which is not a external link. */
        Pattern pattern = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
        /* For-loop the list, send request to every unique link.*/
        for (int i = 1; i < list.size(); i++) {
            Matcher matcher = pattern.matcher(list.get(i));
            if (matcher.find() && !(matcher.group(2).equals(host))) {
                continue;
            }
            sendRequest(host,findPath(list.get(i)),port,list,map);
            Thread.sleep(2000);
        }
        System.out.println("The total number of distinct URLs found on the site is "+ list.size());
        findHTMLpage(map);
        findNonhtml(map);
        returnMinMaxSize(map);
        returnMinMaxTime(map);
        invalidLinks(map);
        findRedirection(map);
    }//main()

    /* The method checkForVisit is used to check whether a url is already being visited, if not, it will be added to the list.
       Otherwise, don't add. (which makes sure that no same url is crawled more than once.
     */
    static ArrayList<String> checkForVisit(ArrayList<String> VisitedList, String url) {
        if (VisitedList.contains(url)) {
            return VisitedList;
        } else {
            VisitedList.add(url);
            return VisitedList;
        }
    }

    /* The sendRequest method is used to send a request to a given path and read the replied message to the given hashmap.
       The key of the hashmap is the given path, the value of hashmap is Object[5] (first element: the number of HTML like 200, 301 or 404,
       second element stores the last-modified time for the html, third element stores the length of the page, forth element stores
       a list of redirections of page, fifth element stores the non-html objects link of page, sixth element stores the content-type of page.
       */
    public static void sendRequest(String host,String path,int port,ArrayList<String> list,HashMap<String,Object[]> dictionary) throws IOException {
        /* Establish the socket for sending request and receiving the information. */
        Socket sock = new Socket(host, port);
        /* Get the output and input stream and use the buffer to read and write. */
        OutputStream out = sock.getOutputStream();
        InputStream in = sock.getInputStream();
        BufferedWriter stdOut = new BufferedWriter(new OutputStreamWriter(out));
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(in));
        /* Send the request. */
        stdOut.write("GET /" + path + " HTTP/1.0\r\n");
        stdOut.write("Accept: text/plain, text/html, text/*\r\n");
        stdOut.write("\r\n");
        stdOut.flush();
        String inMsg;
        Object[] array = new Object[6];
        /* There might be more than one redirection link and non-html objects in one page. So use the arraylist to store information. */
        ArrayList<String> redirection = new ArrayList<>();
        ArrayList<String> nonhtml = new ArrayList<>();
        while ((inMsg = stdIn.readLine()) != null) {
            String[] mes = inMsg.split(" ");
            if (mes[0].equals("HTTP/1.1")) {
                array[0] = mes[1];
            } else if (mes[0].equals("Last-Modified:")) {
                array[1] = inMsg;
            } else if (mes[0].equals("Content-Length:")) {
                array[2] = mes[1];
            } else if (inMsg.contains("<a href=")) {
                /* Use the regular expression to do pattern match for a link. */
                Pattern p = Pattern.compile("a href=\"(.+?)\"");
                Matcher m = p.matcher(inMsg);
                m.find();
                String link = m.group(1);
                /* Check, if haven't been visited, add to the list. */
                list = checkForVisit(list, link);
                redirection.add(link);
            } else if (inMsg.contains("src")) {
                /* The non-html objects like img, video and audio, in html page, also with 'src'. So check whether a line contains 'src', if contains, use
                   use the regular expression to get the non-html object link. Combine the link with upper path to store to the link for crawling.
                   For example, if found <img src = "example.jpg> with path /a/b. Then the link is /a/example.jpg.
                 */
                Pattern p = Pattern.compile("[^/]src=\"(.+?)\"");
                Matcher m = p.matcher(inMsg);
                m.find();
                String link = m.group(1);
                int index = path.lastIndexOf("/");
                String deep_path = path.substring(0,index);
                link = deep_path + "/" + link;
                list = checkForVisit(list,link);
                nonhtml.add(link);
            } else if (mes[0].equals("Content-Type:")) {
                String type = mes[1];
                array[5] = type;
            }
        }
        array[3] = redirection;
        array[4] = nonhtml;
        dictionary.put(path,array);
        sock.close();
    }

    /* The method findPath is used to find a path for a given URL, if there is no path in a URL, set the return string to default "/"
      if the given URL doesn't contains HTTPs, see the URL as the path
    */
    public static String findPath(String url) {
        Pattern pattern = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String path = matcher.group(4);
            if (path.length() == 0) {
                return "/";
            } else {
                return path.substring(1);
            }
        } else {
            return url;
        }
    }

    /* The method returnMinMaxSize is used to determine the maximum and minimum in visited pages.
        Note only the 'ok' page with 200 is considered.*/

    public static void returnMinMaxSize(HashMap<String,Object[]> map) {
        HashMap<String,Integer> dictionary = new HashMap<>();
        for(Map.Entry<String,Object[]> entry:map.entrySet()) {
            String key = entry.getKey();
            Object[] value = entry.getValue();
            if (value[0].equals("200") && value[5].equals("text/html")) {
                dictionary.put(key, Integer.parseInt(value[2].toString()));
            }
        }
        String max = Collections.max(dictionary.entrySet(), Map.Entry.comparingByValue()).getKey();
        Integer maxx = dictionary.get(max);
        String min = Collections.min(dictionary.entrySet(), Map.Entry.comparingByValue()).getKey();
        Integer minn = dictionary.get(min);
        System.out.println("The smallest html page is http://comp3310.ddns.net:7880/"+min + " with content-length(bytes) " + minn);
        System.out.println("The largest html page is http://comp3310.ddns.net:7880/"+max+" with content-length(bytes) "+maxx);
    }

    /* The method returnMinMaxTime is used to determine last-modified page and oldest page. */
    public static void returnMinMaxTime(HashMap<String,Object[]> map) throws ParseException {
        HashMap<String,Date> dictionary = new HashMap<>();
        for(Map.Entry<String,Object[]> entry:map.entrySet()) {
            String key = entry.getKey();
            Object[] value = entry.getValue();
            /* There are some conditions the value[1] is empty since the last modified time is not defined. */
            if (value[1] != null && value[0].equals("200")) {
                String time = value[1].toString().substring(20);
                SimpleDateFormat info = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                Date date = info.parse(time);
                dictionary.put(key,date);
            }
        }
        String latest = Collections.max(dictionary.entrySet(), Map.Entry.comparingByValue()).getKey();
        Date latestt = dictionary.get(latest);
        String oldest = Collections.min(dictionary.entrySet(), Map.Entry.comparingByValue()).getKey();
        Date oldestt = dictionary.get(oldest);
        System.out.println("The most-recently modified page is http://comp3310.ddns.net:7880/"+latest+ " with date "+latestt);
        System.out.println("The oldest modified page is http://comp3310.ddns.net:7880/"+oldest+" with date "+oldestt);
    }

    /* The method invalidLinks is used to find the invalid method. */
    public static void invalidLinks(HashMap<String,Object[]> map) {
        ArrayList<String> list = new ArrayList<>();
        for (Map.Entry<String,Object[]> entry:map.entrySet()) {
            String key = entry.getKey();
            Object[] value = entry.getValue();
            if (value[0].toString().equals("404")) {
                list.add("http://comp3310.ddns.net:7880/"+key);
            }
        }
        System.out.print("A list of invalid URLs (not) found (404):");
        for(int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
    }

    /* The method findRedirection is used to find all redirection links for a path. */
    public static void findRedirection(HashMap<String,Object[]> map) {
        System.out.println("A list of redirected URLs found (30x) and where they redirect to：");
        for (Map.Entry<String,Object[]> entry:map.entrySet()) {
            String key = entry.getKey();
            Object[] value = entry.getValue();
            //First make sure the number is starting with 30x
            if (value[0].toString().startsWith("30")) {
                if (value[3] != null) {
                    String redirectionLink = value[3].toString();
                    System.out.println("http://comp3310.ddns.net:7880/"+key + " redirects to " + redirectionLink);
                }
            }
        }
    }

    /* The method findHTMLpage is used to find all html pages in visited links. When the page is 'ok' and the content-type in header
        shows 'text/html', the page is html page.
     */
    public static void findHTMLpage(HashMap<String,Object[]> map) {
        int count = 0;
        for (Map.Entry<String,Object[]> entry:map.entrySet()) {
            Object[] value = entry.getValue();
            if (value[0].toString().equals("200") && value[5].equals("text/html")) {
                count ++;
            }
        }
        System.out.println("The html page number is " + count);
    }

    /* The method findNonhtml is used to find all non-html objects. Once the content-type in header is not 'text/html', it's a
        non-html object.
     */
    public static void findNonhtml(HashMap<String,Object[]> map) {
        int count = 0;
        for (Map.Entry<String,Object[]> entry:map.entrySet()) {
            Object[] value = entry.getValue();
            if (value[0].toString().equals("200") && !(value[5].toString().equals("text/html"))) {
                count++;
            }
        }
        System.out.println("The non-html object number is "+count);
    }
}//Client
