<%-- 
    © Bart Kampers
--%>

<%@page import="nl.bartkampers.diagrams.DiagramMaker" %>
<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" scope="session" />

<%
    diagramMaker.sendMail(request.getParameter("name"), request.getParameter("address"), request.getParameter("message"));
%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Mail sent</title>
    </head>
    <body>
        <h1>Thank you</h1><br/>
        Your message has been sent.<br/>
        <hr/>
        <div><%=request.getParameter("message")%></div><br/>
        <hr/>
        <form method="post" action="<%=request.getContextPath()%>/diagrammaker.jsp">
            <input type="submit" name="submit" value="Back" />
            <input type="hidden" name="source" value="back" />
        </form>
    </body>
</html>
