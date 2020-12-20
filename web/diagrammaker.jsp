<%--
    © Bart Kampers
--%>

<%@page import="java.util.Enumeration"%>
<%@page import="java.util.Map"%>
<%@page import="nl.bartkampers.diagrams.DiagramMaker"%>
<%@page language="java" contentType="text/html" pageEncoding="UTF-8"%>

<jsp:useBean id="diagramMaker" class="nl.bartkampers.diagrams.DiagramMaker" scope="session" />

<!DOCTYPE html>
<style>
.tooltip {
  position: relative;
  display: inline-block;
}

.tooltip .tooltiptext {
  visibility: hidden;
  /*width: 120px;
  height: 40px;*/
  background-color: #555;
  color: #fff;
  text-align: center;
  border-radius: 6px;
  padding: 5px 0;
  position: absolute;
  z-index: 1;
  bottom: 125%;
  left: 50%;
  margin-left: -60px;
  opacity: 0;
  transition: opacity 0.3s;
}

.tooltip .tooltiptext::after {
  content: "";
  position: absolute;
  top: 100%;
  left: 50%;
  margin-left: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: #555 transparent transparent transparent;
}
.tooltip:hover .tooltiptext {
  visibility: visible;
  opacity: 1;
}


.tooltip:hover span {
    display:block;
    position:fixed;
    overflow:hidden;
}
</style>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Java Diagram Renderer</title>
    </head>
    <body>
        <h1>Java Diagram Renderer</h1>
        <%
            diagramMaker.setRequest(request);
            diagramMaker.setSession(session);
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
                    <form method="post" action="<%=request.getContextPath()%>/diagramexamples.jsp">
                        <input type="submit" name="submit" value="Try one of the examples" />
                    </form>
                </div>
            </div>
        </div>
        <div onmousemove="onMouseMove(event)" onclick="onClick(event)" class="tooltip">
            <span id="tooltip-span" class="tooltiptext"></span>
            <img src="data:image/png;base64, <%=diagramMaker.getBase64()%>" alt="Image not created" />
            <script>
                var tooltipSpan = document.getElementById('tooltip-span');
                var coordinates = <%=diagramMaker.getDataCoordinates()%>;
                /**
                 * @param event mouse move
                 */
                function onMouseMove(event) {
                    tooltipSpan.textContent = getTooltipText(event);
                    if (tooltipSpan.textContent !== "") {
                        tooltipSpan.style.top = (event.clientY + 20) + "px";
                        tooltipSpan.style.left = (event.clientX + 20) + "px";
                        tooltipSpan.style.width = "120px";
                        tooltipSpan.style.height = "40px";
                    }
                    else {
                        tooltipSpan.style.width = "0px";
                        tooltipSpan.style.height = "0px";
                    }
                }
                function getTooltipText(event) {
                    for (var i in coordinates) {
                        var tooltips = coordinates[i];
                        for (var t = 0; t < tooltips.length; ++t) {
                            var tooltip = tooltips[t];
                            if (pointInPolygon(pointOf(event), tooltips[t].polygon)) {
                                return tooltip.text;
                            }
                        }
                    }
                    return "";
                }
                /**
                 * @param event
                 * @return point of ebent
                 */
                function pointOf(event) {
                    var point = {};
                    point.x = event.offsetX;
                    point.y = event.offsetY;
                    return point;
                }
                /**
                 * @param point
                 * @param polygon
                 * @return true if point in polygon, false otherwise,
                 */
                function pointInPolygon(point, polygon) {
                    var j = polygon.length - 1;
                    var oddNodes = false;
                    for (var i = 0; i < polygon.length; ++i) {
                        if ((polygon[i].y < point.y && polygon[j].y >= point.y
                        ||   polygon[j].y < point.y && polygon[i].y >= point.y)
                        && (polygon[i].x <= point.x || polygon[j].x <= point.x)) {
                            oddNodes ^= (polygon[i].x + (point.y - polygon[i].y) / (polygon[j].y - polygon[i].y) * (polygon[j].x - polygon[i].x) < point.x);
                        }
                        j = i;
                    }
                    return oddNodes;
                }
                /**
                 *  @param event mouse click
                 */
                function onClick(event) {
                    location.href = stripParameters(window.location.href) + "?clickX=" + event.offsetX + "&clickY=" + event.offsetY;
                }
                /**
                 * @param url as string
                 * @return url without parameters
                 */
                function stripParameters(url) {
                    var parametersIndex = url.indexOf("?");
                    if (parametersIndex < 0) {
                        return url;
                    }
                    return url.substr(0, parametersIndex);
                }
            </script>
        </div>
        <div><%=diagramMaker.getStatusText()%></div>
        <div>
            <form method="post" action="<%=request.getContextPath()+request.getServletPath()%>">
                <div style="display: table-row;">
                    <div style="display: table-cell;">
                        Figures (Yaml or CSV):<br/> 
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
