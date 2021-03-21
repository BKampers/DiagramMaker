<%-- 
    © Bart Kampers
--%>

<%@page import="java.io.*"%>
<%@page import="java.net.*"%>
<%@page import="java.util.logging.*"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Diagram Example</title>
    </head>
    <body>
        <h1>Diagram Example</h1>
        <%
            StringBuilder builder = new StringBuilder();
            URL url = new URL("http://bartkampers.nl/pngdiagram.jsp?figures=x,y%0A0,0%0A1,1%0A2,2&configuration=%7B%22graphDefaults%22%3A%7B%22type%22%3A%22line%22,%22graphDrawStyle%22%3A%7B%22color%22%3A%220000ff%22%7D%7D%7D");
            try (InputStreamReader in = new InputStreamReader(url.openStream())) {
                boolean eof = false;
                while (!eof) {
                    int read = in.read();
                    eof = read < 0;
                    if (!eof) {
                        builder.append((char) read);
                    }
                }
            }
            catch (IOException ex) {
                Logger.getLogger("example.jsp").log(Level.SEVERE, ex.getMessage());
            }
        %>
        <img src="data:image/png;base64, <%=builder.toString()%>" alt="Image not created" />
    </body>
</html>
