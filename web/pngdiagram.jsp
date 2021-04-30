<%-- 
    © Bart Kampers
--%>

<%@page import="java.io.IOException"%>
<%@page import="java.io.OutputStream"%>
<%@page import="nl.bartkampers.diagrams.DiagramMaker"%>
<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" />

<%!
    public void write(OutputStream stream, String string) throws IOException {
        stream.write(string.getBytes(), 0, string.length());
    } 
%>
<%
    diagramMaker.setRequest(request);
    diagramMaker.setSession(session);
    diagramMaker.setFigures(request.getParameter("figures"));
    String configuration = request.getParameter("configuration");
    diagramMaker.setConfiguration((configuration == null) ? "" : configuration);
    diagramMaker.setSource("form");
    OutputStream stream = response.getOutputStream();
    write(stream, "{png:\"");
    write(stream, diagramMaker.getBase64());
    write(stream, "\",coordinates:");
    write(stream, diagramMaker.getDataCoordinates());
    write(stream, "}");
    stream.flush();
%>
