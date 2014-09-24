<%
String reqAddr = "index.jsp";
if (request.getParameter("redirectURL")!=null) {
    reqAddr = request.getParameter("redirectURL");
}
cz.incad.vdk.vdkcr_client.Knihovna kn = new cz.incad.vdk.vdkcr_client.Knihovna(request.getRemoteUser());
request.getSession().setAttribute("knihovna", kn);
System.out.println(kn.getNazev());
// no redirect with error
reqAddr = reqAddr.replace("?error=accessdenied","");
response.sendRedirect(reqAddr);
%>

