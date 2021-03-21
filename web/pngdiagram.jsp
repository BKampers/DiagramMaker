<%-- 
    © Bart Kampers
--%>

<%@page import="java.io.OutputStream"%>
<%@page import="nl.bartkampers.diagrams.DiagramMaker"%>
<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" />

<%
    diagramMaker.setRequest(request);
    diagramMaker.setSession(session);
    diagramMaker.setFigures(request.getParameter("figures"));
    String configuration = request.getParameter("configuration");
    diagramMaker.setConfiguration((configuration == null) ? "" : configuration);
    diagramMaker.setSource("form");
    String base64 = diagramMaker.getBase64();
    OutputStream os = response.getOutputStream();
    os.write(base64.getBytes(), 0, base64.length());
    os.flush();
%>
