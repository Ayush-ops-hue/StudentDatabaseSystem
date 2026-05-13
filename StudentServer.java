import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.sql.*;

public class StudentServer {

    static final String URL = "jdbc:mysql://localhost:3306/student_db";
    static final String USER = "root";
    static final String PASS = "Ayush@1701";

    public static void main(String[] args) throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new HomeHandler());
        server.createContext("/add", new AddHandler());
        server.createContext("/delete", new DeleteHandler());
        server.createContext("/search", new SearchHandler());
        server.createContext("/update", new UpdateHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server running at http://localhost:8080");
    }

    // HOME PAGE
    static class HomeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            StringBuilder table = new StringBuilder();

            try {
                Connection conn = DriverManager.getConnection(URL, USER, PASS);

                ResultSet rs = conn.createStatement()
                        .executeQuery("SELECT * FROM students");

                while (rs.next()) {

                    table.append("<tr>")
                            .append("<td>").append(rs.getInt("id")).append("</td>")
                            .append("<td>").append(rs.getString("name")).append("</td>")
                            .append("<td>").append(rs.getString("email")).append("</td>")
                            .append("<td>").append(rs.getString("course")).append("</td>")
                            .append("<td>").append(rs.getDouble("gpa")).append("</td>")
                            .append("</tr>");
                }

                conn.close();

            } catch (Exception e) {
                table.append("<tr><td colspan='5'>")
                        .append(e.getMessage())
                        .append("</td></tr>");
            }

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Student Database</title>
                    <style>
                        body{
                            font-family:Arial;
                            background:#f4f4f4;
                            padding:20px;
                        }

                        h1{
                            color:#333;
                        }

                        .box{
                            background:white;
                            padding:20px;
                            border-radius:10px;
                            margin-bottom:20px;
                        }

                        input{
                            padding:10px;
                            margin:5px;
                            width:200px;
                        }

                        button{
                            padding:10px 20px;
                            background:#007bff;
                            color:white;
                            border:none;
                            border-radius:5px;
                            cursor:pointer;
                        }

                        table{
                            width:100%;
                            border-collapse:collapse;
                        }

                        th,td{
                            border:1px solid #ddd;
                            padding:10px;
                            text-align:center;
                        }

                        th{
                            background:#007bff;
                            color:white;
                        }
                    </style>
                </head>

                <body>

                    <h1>Student Database Manager</h1>

                    <div class='box'>
                        <h2>Add Student</h2>

                        <form action='/add' method='get'>

                            <input type='text' name='name' placeholder='Name' required>

                            <input type='email' name='email' placeholder='Email' required>

                            <input type='text' name='course' placeholder='Course' required>

                            <input type='number' step='0.1' name='gpa' placeholder='GPA' required>

                            <button type='submit'>Add</button>

                        </form>
                    </div>

                    <div class='box'>
                        <h2>Search Student</h2>

                        <form action='/search' method='get'>

                            <input type='text' name='keyword' placeholder='Enter Name'>

                            <button type='submit'>Search</button>

                        </form>
                    </div>

                    <div class='box'>
                        <h2>Update GPA</h2>

                        <form action='/update' method='get'>

                            <input type='number' name='id' placeholder='Student ID' required>

                            <input type='number' step='0.1' name='gpa' placeholder='New GPA' required>

                            <button type='submit'>Update</button>

                        </form>
                    </div>

                    <div class='box'>
                        <h2>Delete Student</h2>

                        <form action='/delete' method='get'>

                            <input type='number' name='id' placeholder='Student ID' required>

                            <button type='submit'>Delete</button>

                        </form>
                    </div>

                    <div class='box'>

                        <h2>All Students</h2>

                        <table>

                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Course</th>
                                <th>GPA</th>
                            </tr>

                            """ + table + """

                        </table>

                    </div>

                </body>
                </html>
            """;

            send(exchange, html);
        }
    }

    // ADD
    static class AddHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            try {

                String query = exchange.getRequestURI().getQuery();

                String name = getValue(query, "name");
                String email = getValue(query, "email");
                String course = getValue(query, "course");
                double gpa = Double.parseDouble(getValue(query, "gpa"));

                Connection conn = DriverManager.getConnection(URL, USER, PASS);

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO students(name,email,course,gpa) VALUES(?,?,?,?)");

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, course);
                ps.setDouble(4, gpa);

                ps.executeUpdate();

                conn.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            redirect(exchange, "/");
        }
    }

    // DELETE
    static class DeleteHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            try {

                String query = exchange.getRequestURI().getQuery();

                int id = Integer.parseInt(getValue(query, "id"));

                Connection conn = DriverManager.getConnection(URL, USER, PASS);

                PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM students WHERE id=?");

                ps.setInt(1, id);

                ps.executeUpdate();

                conn.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            redirect(exchange, "/");
        }
    }

    // SEARCH
    static class SearchHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            String keyword = getValue(
                    exchange.getRequestURI().getQuery(),
                    "keyword"
            );

            StringBuilder rows = new StringBuilder();

            try {

                Connection conn = DriverManager.getConnection(URL, USER, PASS);

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM students WHERE name LIKE ?"
                );

                ps.setString(1, "%" + keyword + "%");

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {

                    rows.append("<tr>")
                            .append("<td>").append(rs.getInt("id")).append("</td>")
                            .append("<td>").append(rs.getString("name")).append("</td>")
                            .append("<td>").append(rs.getString("email")).append("</td>")
                            .append("<td>").append(rs.getString("course")).append("</td>")
                            .append("<td>").append(rs.getDouble("gpa")).append("</td>")
                            .append("</tr>");
                }

                conn.close();

            } catch (Exception e) {
                rows.append(e.getMessage());
            }

            String html = """
                <html>
                <body style='font-family:Arial;padding:20px'>

                    <h1>Search Results</h1>

                    <table border='1' cellpadding='10'>

                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Course</th>
                            <th>GPA</th>
                        </tr>

                        """ + rows + """

                    </table>

                    <br><br>

                    <a href='/'>Back</a>

                </body>
                </html>
            """;

            send(exchange, html);
        }
    }

    // UPDATE
    static class UpdateHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            try {

                String query = exchange.getRequestURI().getQuery();

                int id = Integer.parseInt(getValue(query, "id"));

                double gpa = Double.parseDouble(getValue(query, "gpa"));

                Connection conn = DriverManager.getConnection(URL, USER, PASS);

                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE students SET gpa=? WHERE id=?"
                );

                ps.setDouble(1, gpa);
                ps.setInt(2, id);

                ps.executeUpdate();

                conn.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            redirect(exchange, "/");
        }
    }

    static String getValue(String query, String key) {

        if (query == null) return "";

        for (String pair : query.split("&")) {

            String[] kv = pair.split("=");

            if (kv[0].equals(key) && kv.length > 1) {

                return URLDecoder.decode(kv[1]);
            }
        }

        return "";
    }

    static void send(HttpExchange exchange, String response) throws IOException {

        exchange.getResponseHeaders().set("Content-Type", "text/html");

        exchange.sendResponseHeaders(200, response.getBytes().length);

        OutputStream os = exchange.getResponseBody();

        os.write(response.getBytes());

        os.close();
    }

    static void redirect(HttpExchange exchange, String url) throws IOException {

        exchange.getResponseHeaders().add("Location", url);

        exchange.sendResponseHeaders(302, -1);

        exchange.close();
    }
}