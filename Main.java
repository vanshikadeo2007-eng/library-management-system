import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Library Management System
 * Java 17+ built-in HTTP server, file-based persistence, and DSA structures.
 */
public class Main {
    private static final int PORT = 8080;
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path BOOKS_FILE = DATA_DIR.resolve("books.txt");
    private static final Path RECORDS_FILE = DATA_DIR.resolve("transactions.txt");
    private static final Path WAITLIST_FILE = DATA_DIR.resolve("waitlist.txt");

    // DSA: HashMap gives average O(1) lookup by book ID.
    private static final Map<String, Book> books = new HashMap<>();
    // DSA: ArrayList is useful for ordered transaction history and sorting.
    private static final List<Transaction> transactions = new ArrayList<>();
    // DSA: FIFO queue for students waiting for an unavailable book.
    private static final Map<String, Queue<WaitRequest>> waitlists = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DATA_DIR);
        loadBooks();
        loadTransactions();
        loadWaitlists();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", Main::serveStatic);
        server.createContext("/api/books", Main::handleBooks);
        server.createContext("/api/issue", Main::handleIssue);
        server.createContext("/api/return", Main::handleReturn);
        server.createContext("/api/transactions", Main::handleTransactions);
        server.createContext("/api/waitlist", Main::handleWaitlist);
        server.start();
        System.out.println("Library Management System running at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop.");
    }

    static class Book {
        String id, title, author;
        int total, available;
        Book(String id, String title, String author, int total, int available) {
            this.id=id; this.title=title; this.author=author; this.total=total; this.available=available;
        }
        Map<String,Object> toMap() {
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("id",id); m.put("title",title); m.put("author",author);
            m.put("total",total); m.put("available",available);
            return m;
        }
    }
    static class Transaction {
        String type, bookId, bookTitle, studentId, studentName, time;
        Transaction(String type,String bookId,String bookTitle,String studentId,String studentName,String time) {
            this.type=type; this.bookId=bookId; this.bookTitle=bookTitle;
            this.studentId=studentId; this.studentName=studentName; this.time=time;
        }
        Map<String,Object> toMap() {
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("type",type); m.put("bookId",bookId); m.put("bookTitle",bookTitle);
            m.put("studentId",studentId); m.put("studentName",studentName); m.put("time",time);
            return m;
        }
    }
    static class WaitRequest {
        String studentId, studentName;
        WaitRequest(String id,String name){studentId=id;studentName=name;}
        Map<String,Object> toMap(){
            Map<String,Object> m=new LinkedHashMap<>();m.put("studentId",studentId);m.put("studentName",studentName);return m;
        }
    }

    private static synchronized void loadBooks() throws IOException {
        if (!Files.exists(BOOKS_FILE)) {
            books.put("B101", new Book("B101","Data Structures in Java","N. Sharma",3,3));
            books.put("B102", new Book("B102","Introduction to Algorithms","T. Cormen",2,2));
            books.put("B103", new Book("B103","Clean Code","Robert C. Martin",1,1));
            saveBooks();
            return;
        }
        for (String line : Files.readAllLines(BOOKS_FILE, StandardCharsets.UTF_8)) {
            String[] p=line.split("\\|",-1);
            if(p.length==5) {
                try {
                    Book b=new Book(dec(p[0]),dec(p[1]),dec(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]));
                    books.put(b.id,b);
                } catch(Exception ignored) {}
            }
        }
    }
    private static synchronized void saveBooks() throws IOException {
        List<String> lines=new ArrayList<>();
        for(Book b:books.values()) lines.add(enc(b.id)+"|"+enc(b.title)+"|"+enc(b.author)+"|"+b.total+"|"+b.available);
        Files.write(BOOKS_FILE,lines,StandardCharsets.UTF_8);
    }
    private static synchronized void loadTransactions() throws IOException {
        if(!Files.exists(RECORDS_FILE)) return;
        for(String line:Files.readAllLines(RECORDS_FILE,StandardCharsets.UTF_8)){
            String[] p=line.split("\\|",-1);
            if(p.length==6) transactions.add(new Transaction(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),dec(p[4]),dec(p[5])));
        }
    }
    private static synchronized void saveTransactions() throws IOException {
        List<String> lines=new ArrayList<>();
        for(Transaction t:transactions) lines.add(enc(t.type)+"|"+enc(t.bookId)+"|"+enc(t.bookTitle)+"|"+enc(t.studentId)+"|"+enc(t.studentName)+"|"+enc(t.time));
        Files.write(RECORDS_FILE,lines,StandardCharsets.UTF_8);
    }
    private static synchronized void loadWaitlists() throws IOException {
        if(!Files.exists(WAITLIST_FILE)) return;
        for(String line:Files.readAllLines(WAITLIST_FILE,StandardCharsets.UTF_8)){
            String[] p=line.split("\\|",-1);
            if(p.length==3) waitlists.computeIfAbsent(dec(p[0]),k->new LinkedList<>()).add(new WaitRequest(dec(p[1]),dec(p[2])));
        }
    }
    private static synchronized void saveWaitlists() throws IOException {
        List<String> lines=new ArrayList<>();
        for(Map.Entry<String,Queue<WaitRequest>> e:waitlists.entrySet())
            for(WaitRequest w:e.getValue()) lines.add(enc(e.getKey())+"|"+enc(w.studentId)+"|"+enc(w.studentName));
        Files.write(WAITLIST_FILE,lines,StandardCharsets.UTF_8);
    }

    private static void handleBooks(HttpExchange ex) throws IOException {
        try {
            String method=ex.getRequestMethod();
            if(method.equals("GET")) {
                String query=ex.getRequestURI().getRawQuery();
                Map<String,String> q=parseQuery(query);
                String search=q.getOrDefault("search","").toLowerCase(Locale.ROOT);
                String sort=q.getOrDefault("sort","");
                List<Book> list;
                synchronized(Main.class) {
                    list=new ArrayList<>(books.values());
                }
                if(!search.isBlank()) list=list.stream().filter(b ->
                    b.id.toLowerCase(Locale.ROOT).contains(search) ||
                    b.title.toLowerCase(Locale.ROOT).contains(search) ||
                    b.author.toLowerCase(Locale.ROOT).contains(search)).collect(Collectors.toList());
                if("title".equals(sort)) list.sort(Comparator.comparing(b->b.title.toLowerCase(Locale.ROOT)));
                else if("id".equals(sort)) list.sort(Comparator.comparing(b->b.id.toLowerCase(Locale.ROOT)));
                json(ex,200,list.stream().map(Book::toMap).collect(Collectors.toList()));
            } else if(method.equals("POST")) {
                Map<String,String> d=readForm(ex);
                String id=required(d,"id").trim().toUpperCase(Locale.ROOT);
                String title=required(d,"title").trim(), author=required(d,"author").trim();
                int total=parsePositive(d.get("total"),"Total copies");
                if(title.isBlank()||author.isBlank()) throw new IllegalArgumentException("Title and author are required.");
                synchronized(Main.class) {
                    if(books.containsKey(id)) throw new IllegalArgumentException("A book with this ID already exists.");
                    books.put(id,new Book(id,title,author,total,total)); saveBooks();
                }
                json(ex,201,Map.of("message","Book added successfully."));
            } else if(method.equals("DELETE")) {
                Map<String,String> q=parseQuery(ex.getRequestURI().getRawQuery());
                String id=required(q,"id");
                synchronized(Main.class) {
                    Book b=books.get(id);
                    if(b==null) throw new IllegalArgumentException("Book not found.");
                    if(b.available!=b.total) throw new IllegalArgumentException("Return all issued copies before deleting this book.");
                    books.remove(id); waitlists.remove(id); saveBooks(); saveWaitlists();
                }
                json(ex,200,Map.of("message","Book deleted."));
            } else methodNotAllowed(ex);
        } catch(IllegalArgumentException e){ error(ex,400,e.getMessage()); }
          catch(Exception e){ error(ex,500,"Server error: "+safe(e.getMessage())); }
    }

    private static void handleIssue(HttpExchange ex) throws IOException {
        if(!ex.getRequestMethod().equals("POST")){methodNotAllowed(ex);return;}
        try {
            Map<String,String> d=readForm(ex);
            String id=required(d,"bookId").trim().toUpperCase(Locale.ROOT);
            String studentId=required(d,"studentId").trim(), studentName=required(d,"studentName").trim();
            if(studentName.isBlank()||studentId.isBlank()) throw new IllegalArgumentException("Student name and ID are required.");
            synchronized(Main.class) {
                Book b=books.get(id);
                if (b == null) {
    throw new IllegalArgumentException("Book ID not found.");
}

boolean alreadyIssued = false;

for (int i = transactions.size() - 1; i >= 0; i--) {
    Transaction t = transactions.get(i);

    if (t.bookId.equals(id) && t.studentId.equals(studentId)) {
        alreadyIssued = t.type.equals("ISSUE");
        break;
    }
}

if (alreadyIssued) {
    throw new IllegalArgumentException(
        "This student already has an active issue record for this book."
    );
}
                if(b.available<=0) {
                    Queue<WaitRequest> q=waitlists.computeIfAbsent(id,k->new LinkedList<>());
                    boolean duplicate=q.stream().anyMatch(w->w.studentId.equals(studentId));
                    if(!duplicate) q.offer(new WaitRequest(studentId,studentName));
                    saveWaitlists();
                    throw new IllegalArgumentException("No copies available. Student added to the waiting list.");
                }
                b.available--;
                transactions.add(new Transaction("ISSUE",id,b.title,studentId,studentName,now()));
                saveBooks();saveTransactions();
            }
            json(ex,200,Map.of("message","Book issued successfully."));
        } catch(IllegalArgumentException e){error(ex,400,e.getMessage());}
          catch(Exception e){error(ex,500,"Server error: "+safe(e.getMessage()));}
    }

    private static void handleReturn(HttpExchange ex) throws IOException {
        if(!ex.getRequestMethod().equals("POST")){methodNotAllowed(ex);return;}
        try {
            Map<String,String> d=readForm(ex);
            String id=required(d,"bookId").trim().toUpperCase(Locale.ROOT);
            String studentId=required(d,"studentId").trim();
            synchronized(Main.class) {
                Book b=books.get(id);
                if(b==null) throw new IllegalArgumentException("Book ID not found.");
                Transaction active=null;
                for(int i=transactions.size()-1;i>=0;i--){
                    Transaction t=transactions.get(i);
                    if(t.bookId.equals(id)&&t.studentId.equals(studentId)){
                        if(t.type.equals("ISSUE")){active=t;break;}
                        if(t.type.equals("RETURN")) break;
                    }
                }
                if(active==null) throw new IllegalArgumentException("No active issue found for this book and student ID.");
                b.available=Math.min(b.total,b.available+1);
                transactions.add(new Transaction("RETURN",id,b.title,active.studentId,active.studentName,now()));
                saveBooks();saveTransactions();
                Queue<WaitRequest> q=waitlists.get(id);
                if(q!=null&&!q.isEmpty()) {
                    WaitRequest next=q.poll();
                    // Reserve the returned copy for the next student in FIFO order.
                    b.available--;
                    transactions.add(new Transaction("ISSUE",id,b.title,next.studentId,next.studentName,now()));
                    saveBooks();saveTransactions();saveWaitlists();
                    json(ex,200,Map.of("message","Book returned. It has automatically been issued to waiting student "+next.studentName+" ("+next.studentId+")."));
                    return;
                }
            }
            json(ex,200,Map.of("message","Book returned successfully."));
        } catch(IllegalArgumentException e){error(ex,400,e.getMessage());}
          catch(Exception e){error(ex,500,"Server error: "+safe(e.getMessage()));}
    }

    private static void handleTransactions(HttpExchange ex) throws IOException {
        if(!ex.getRequestMethod().equals("GET")){methodNotAllowed(ex);return;}
        synchronized(Main.class) {
            List<Map<String,Object>> out=new ArrayList<>();
            for(int i=transactions.size()-1;i>=0;i--) out.add(transactions.get(i).toMap());
            json(ex,200,out);
        }
    }
    private static void handleWaitlist(HttpExchange ex) throws IOException {
        if(!ex.getRequestMethod().equals("GET")){methodNotAllowed(ex);return;}
        synchronized(Main.class) {
            List<Map<String,Object>> out=new ArrayList<>();
            for(Map.Entry<String,Queue<WaitRequest>> e:waitlists.entrySet())
                for(WaitRequest w:e.getValue()) {
                    Map<String,Object> m=new LinkedHashMap<>();m.put("bookId",e.getKey());m.put("studentId",w.studentId);m.put("studentName",w.studentName);out.add(m);
                }
            json(ex,200,out);
        }
    }

    private static void serveStatic(HttpExchange ex) throws IOException {
        String path=ex.getRequestURI().getPath();
        if(path.equals("/")) path="/index.html";
        Path root=Paths.get("public").toAbsolutePath().normalize();
        Path file=root.resolve(path.substring(1)).normalize();
        if(!file.startsWith(root)||!Files.exists(file)||Files.isDirectory(file)){send(ex,404,"Not found","text/plain; charset=utf-8");return;}
        String type=file.toString().endsWith(".html")?"text/html; charset=utf-8":
            file.toString().endsWith(".css")?"text/css; charset=utf-8":
            file.toString().endsWith(".js")?"application/javascript; charset=utf-8":"application/octet-stream";
        send(ex,200,Files.readString(file,StandardCharsets.UTF_8),type);
    }

    private static Map<String,String> readForm(HttpExchange ex) throws IOException {
        String body=new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
        return parseQuery(body);
    }
    private static Map<String,String> parseQuery(String raw) {
        Map<String,String> out=new HashMap<>();
        if(raw==null||raw.isBlank()) return out;
        for(String pair:raw.split("&")){
            String[] p=pair.split("=",2);
            out.put(decode(p[0]),p.length>1?decode(p[1]):"");
        }
        return out;
    }
    private static String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
    private static String enc(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String dec(String s){return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);}
    private static String required(Map<String,String> m,String key){
        String v=m.get(key); if(v==null||v.trim().isEmpty()) throw new IllegalArgumentException(key+" is required."); return v;
    }
    private static int parsePositive(String s,String label){
        try{int n=Integer.parseInt(s);if(n<1)throw new NumberFormatException();return n;}
        catch(Exception e){throw new IllegalArgumentException(label+" must be a whole number greater than 0.");}
    }
    private static String now(){return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));}
    private static String safe(String s){return s==null?"Unknown error":s.replaceAll("[\\r\\n]"," ");}
    private static void json(HttpExchange ex,int status,Object value)throws IOException{
        send(ex,status,toJson(value),"application/json; charset=utf-8");
    }
    private static String toJson(Object o){
        if(o==null)return "null";
        if(o instanceof String)return "\""+escapeJson((String)o)+"\"";
        if(o instanceof Number||o instanceof Boolean)return o.toString();
        if(o instanceof Map){
            List<String> parts=new ArrayList<>();
            ((Map<?,?>)o).forEach((k,v)->parts.add(toJson(String.valueOf(k))+":"+toJson(v)));
            return "{"+String.join(",",parts)+"}";
        }
        if(o instanceof Iterable){
            List<String> parts=new ArrayList<>();for(Object x:(Iterable<?>)o)parts.add(toJson(x));
            return "["+String.join(",",parts)+"]";
        }
        return toJson(o.toString());
    }
    private static String escapeJson(String s){
        StringBuilder b=new StringBuilder();
        for(char c:s.toCharArray()){
            switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}
        }return b.toString();
    }
    private static void send(HttpExchange ex,int status,String body,String type)throws IOException{
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET,POST,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
        if(ex.getRequestMethod().equals("OPTIONS")){ex.sendResponseHeaders(204,-1);return;}
        ex.sendResponseHeaders(status,bytes.length);
        try(OutputStream os=ex.getResponseBody()){os.write(bytes);}
    }
    private static void error(HttpExchange ex,int status,String message)throws IOException{json(ex,status,Map.of("error",message));}
    private static void methodNotAllowed(HttpExchange ex)throws IOException{error(ex,405,"Method not allowed.");}
}
