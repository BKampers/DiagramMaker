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
            <span id="tooltip-span" class="tooltiptext"> data-html="true"</span>
            <img src="data:image/png;base64, <%=diagramMaker.getBase64()%>" alt="Image not created" />
            <script>
                var tooltipSpan = document.getElementById('tooltip-span');
                var tooltips = <%=diagramMaker.getDataCoordinates()%>;
                /**
                 * @param event mouse move
                 */
                function onMouseMove(event) {
                    var text = getTooltipText(point(event.offsetX, event.offsetY));
                    if (text !== null) {
                        tooltipSpan.textContent = text;
                        tooltipSpan.style.top = (event.clientY + 20) + "px";
                        tooltipSpan.style.left = (event.clientX + 20) + "px";
                        tooltipSpan.style.width = "120px";
                        tooltipSpan.style.height = "40px";
                    }
                    else {
                        tooltipSpan.textContent = "";
                        tooltipSpan.style.width = "0px";
                        tooltipSpan.style.height = "0px";
                    }
                }
                /**
                 * @param x
                 * @@param y 
                 * @return point (x,y)
                 */
                function point(x, y) {
                    var point = {};
                    point.x = x;
                    point.y = y;
                    return point;
                }
                /**
                 * @param point
                 * @returns tool tip text for point of mouse event, if available
                 *          null if not available
                 */
                function getTooltipText(point) {
                    for (var i = 0; i < tooltips.length; ++i) {
                        var tooltip = tooltips[i];
                        if (pointInsidePolygon(point, tooltip.polygon)) {
                            return tooltip.text;
                        }
                    }
                    return null;
                }
                /**
                 * @param point
                 * @param polygon
                 * @return true if point inside polygon, false otherwise,
                 */
                function pointInsidePolygon(point, polygon) {
                    var inside = false;
                    var corner1 = polygon[polygon.length - 1];
                    for (var i = 0; i < polygon.length; ++i) {
                        var corner2 = polygon[i];
                        if (yBetween(point, corner1, corner2) && xAfter(point, corner1, corner2)) {
                            inside ^= point.x > corner2.x + interpolatedX(point, corner1, corner2);
                        }
                        corner1 = corner2;
                    }
                    return inside;
                }
                /**
                 * @param point
                 * @param corner1
                 * @param corner2
                 * @return interpolated x coordinate for point's y coordinate in rectangle between corner1 and corner2
                 */
                function interpolatedX(point, corner1, corner2) {
                    return (point.y - corner2.y) / height(corner1, corner2) * width(corner1, corner2);
                }
                /**
                 * @param corner1
                 * @param corner2
                 * @return height of rectangle between corner1 and corner2
                 */
                function height(corner1, corner2) {
                    return corner1.y - corner2.y;
                }
                /**
                 * @param corner1
                 * @param corner2
                 * @return width of rectangle between corner1 and corner2
                 */
                function width(corner1, corner2) {
                    return corner1.x - corner2.x;
                }
                /**
                 * @param point
                 * @param p1
                 * @param p2
                 * @returns true if y of point between p1 and p2
                 */
                function yBetween(point, p1, p2) {
                    return p1.y < point.y && point.y <= p2.y || p2.y < point.y && point.y <= p1.y;
                }
                /**
                 * @param point
                 * @param p1
                 * @param p2
                 * @returns true if x of point above p1 or p2
                 */
                function xAfter(point, p1, p2) {
                    return p1.x <= point.x || p2.x <= point.x;
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
        </div>
        <p/>
        <div>
            <form method="post" action="<%=request.getContextPath()%>/leavemessage.jsp">
                <input type="submit" name="submit" value="Leave a message" />
            </form>
        </div>
    </body>
</html>
