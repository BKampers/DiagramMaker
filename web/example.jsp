<%-- 
    © Bart Kampers
--%>

<%@page import="java.io.*"%>
<%@page import="java.net.*"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Diagram</title>
    </head>
    <body>
        <h1>Diagram</h1>
        <%
            StringBuilder builder = new StringBuilder();
            URL url = new URL("http://bartkampers.nl/pngdiagram.jsp?figures=x,y%0A0,0%0A1,1%0A2,2&configuration=%7B%22graph%22%3A%7B%22type%22%3A%22line%22,%22color%22%3A%220000ff%22%7D%7D");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            in.close();
        %>
        <img src="data:image/png;base64, <%=builder.toString()%>" alt="Image not created" />
    </body>
</html>
