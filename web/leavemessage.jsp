<%-- 
    © Bart Kampers
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Leave a message</title>
    </head>
    <body>
        <h2>Leave a message</h2><br/>
        <form method="post" action="<%=request.getContextPath()%>/thankyou.jsp">
            Your name: <input type="text" name="name" /><br/>
            Your email address: <input type="text" name="address" /><br/>
            Your message:<br/>
            <textarea type="text" name="message" cols="40" rows="10"></textarea><br/>
            <input type="submit" name="submit" value="Send" />
        </form>
    </body>
</html>
