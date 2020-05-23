<%-- 
    © Bart Kampers
--%>
<%@page import="nl.bartkampers.diagrams.DiagramMaker"%>
<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" scope="session" />
<%@ page contentType="applicaton/octet-stream" %>
<%
    byte[] data = diagramMaker.createJpgBytes();
    response.setHeader("Content-length", Integer.toString(data.length));
    response.setHeader("Content-Disposition", "attachment; filename=xyz.jpg");
    response.getOutputStream().write(data, 0, data.length);
    response.getOutputStream().flush();
%>
<%--    
<%@page contentType="text/html" pageEncoding="UTF-8"%>
//            String filename = "home.jsp";   
//            String filepath = "e:\\";
            response.setContentType("APPLICATION/OCTET-STREAM");   
            response.setHeader("Content-Disposition","attachment; filename=\"" + "the.txt" + "\"");   
//            java.io.FileInputStream fileInputStream=new java.io.FileInputStream(filepath + filename);  
            byte[] bytes = "123".getBytes();
            int i = 0;   
            while (i < bytes.length) {  
                out.write(bytes[i]);
                i++;
            }   
//            fileInputStream.close();   
--%>   
