<%--
    © Bart Kampers
--%>

<%@page import="java.util.Enumeration"%>
<%@page import="java.util.Map"%>
<%@page import="nl.bartkampers.diagrams.DiagramMaker"%>
<%@page language="java" contentType="text/html" pageEncoding="UTF-8"%>

<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" scope="session" />

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Java Diagram Renderer</title>
    </head>
    <body>
        <h1>Java Diagram Renderer</h1>
        <%
            diagramMaker.setRequest(request);
            String source = request.getParameter("source");
            if ("form".equals(source) || "example".equals(source)) {
                diagramMaker.setSource(source);
                diagramMaker.setConfiguration(request.getParameter("configuration"));
                diagramMaker.setFigures(request.getParameter("figures"));
            }
        %>
        <div>
            <div style="display: table-row;">
                <div style="display: table-cell;">
                    Drop your figures.<br/>
                    View your diagram immediately.<br/>
                    Customize until you like it.<br/>
                </div>
                <div style="display: table-cell;">
                </div>
                <div style="display: table-cell;">
                    <form method="post"action="<%=request.getContextPath()%>/diagramexamples.jsp">
                        <input type="submit" name="submit" value="Try one of the examples" />
                    </form>
                </div>
            </div>
        </div>
        <div><img src="data:image/png;base64, <%=diagramMaker.getBase64()%>" alt="Image not created" /></div>
        <div><%=diagramMaker.getStatusText()%></div>
        <div>
            <form method="post" action="<%=request.getContextPath()+request.getServletPath()%>">
                <div style="display: table-row;">
                    <div style="display: table-cell;">
                        Figures (JSON or CSV):<br/> 
                        <textarea style="font-family:Courier" name="figures" cols="85" rows="30"><%=diagramMaker.getFigures()%></textarea><br/>
                    </div>
                    <div style="display: table-cell;">
                        Configuration (Yaml):<br/>
                        <textarea style="font-family:Courier" name="configuration" cols="85" rows="30"><%=diagramMaker.getConfiguration()%></textarea><br/>
                    </div>
                </div>
                <div style="display: table-row;">
                    <input type="hidden" name="source" value="form" />
                    <input type="submit" name="submit" value="Draw chart" />
                 </div>
            </form>
            <!-- <a href="download.jsp">download the jpg file</a> -->
        </div>
        <p/>
        <div>
            <form method="post" action="<%=request.getContextPath()%>/leavemessage.jsp">
                <input type="submit" name="submit" value="Leave a message" />
            </form>
        </div>
    </body>
</html>
