<%-- 
    © Bart Kampers
--%>

<%@page import="java.net.*"%>
<%@page import="java.util.*"%>
<%@page import="nl.bartkampers.diagrams.DiagramMaker"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" scope="session" />

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Diagram examples</title>
    </head>
    <body>
        <h1>Diagram examples</h1>
        <%
            diagramMaker.setRequest(request);
            Map<String, String[]> examples = diagramMaker.getExamples();
        %>
        <div>
            <%for (Map.Entry<String, String[]> example : examples.entrySet()) {%>
                <%
                    String configuration = diagramMaker.readString(example.getValue()[0]);
                    String figures = diagramMaker.readString(example.getValue()[1]);
                    diagramMaker.setSource(null);
                    diagramMaker.setConfiguration(configuration);
                    diagramMaker.setFigures(figures);
                %>
                <div>
                    <div style="display: table-row;">
                        <div style="display: table-cell;"><h2><%=example.getKey()%>&nbsp;</h2></div>
                        <div style=" display: table-cell;">
                            <form method="post" action="<%=request.getContextPath()%>/diagrammaker.jsp">
                                <input type="hidden" name="configuration" value="<%=example.getValue()[0]%>" />
                                <input type="hidden" name="figures" value="<%=example.getValue()[1]%>" />
                                <input type="hidden" name="source" value="example" />
                                <input type="submit" name="submit" value="Copy" />
                            </form>
                        </div>
                    </div>
                </div>
                <img src="data:image/png;base64, <%=diagramMaker.getBase64()%>" alt="Image not created" />
                <table border="0">
                    <tr>
                        <td>Figures</td>
                        <td>Configuration</td>
                    </tr>
                    <tr>
                        <td valign="top"><textarea style="font-family:Courier" cols="50" rows="10" readonly><%=figures%></textarea></td>
                        <td valign="top"><textarea style="font-family:Courier" name="configuration" cols="50" rows="10" readonly><%=configuration%></textarea></td>
                    </tr>
                </table>
                <hr/>    
            <%}%>
        </div>
    </body>
</html>
