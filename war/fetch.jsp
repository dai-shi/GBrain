<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.axlight.gbrain.server.GBrainServiceImpl" %>

<html>
  <body>

<%
    GBrainServiceImpl impl = GBrainServiceImpl.getInstance();
    if (impl != null) {
    	impl.fetchNeuron();
%>
OK
<%
    }else{
%>
NG
<%
	}
%>

  </body>
</html>
