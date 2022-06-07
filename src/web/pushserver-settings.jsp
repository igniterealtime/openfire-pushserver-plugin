<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="org.jivesoftware.util.*" %>
<%@ page import="org.igniterealtime.openfire.plugins.pushserver.PushServerManager" %>
<%@ page import="org.igniterealtime.openfire.plugins.pushserver.models.PushRecord" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>

<%@ taglib uri="admin" prefix="admin" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/pushservermanager.tld" prefix="pm"%>

<%
    final ArrayList<PushServerManager.Message> messageList = new ArrayList<>();

    boolean androidUpdate = request.getParameter("androidUpdate") != null;
    boolean iosUpdate = request.getParameter("iosUpdate") != null;
    boolean androidCredentialUpdate = request.getParameter("androidCredentialUpdate") != null;
    boolean iosCredentialUpdate = request.getParameter("iosCredentialUpdate") != null;

    String baseUrl = "pushserver-settings.jsp";

    boolean csrfCheck = false;
    if (androidUpdate || iosUpdate || androidCredentialUpdate || iosCredentialUpdate) {
        Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
        String csrfParam = ParamUtils.getParameter(request, "csrf");
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            String newUrl = baseUrl + "?savesucceeded=false";
            response.sendRedirect(newUrl);
        }
        else
        {
            csrfCheck = true;
        }
    }

    String csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);
    pageContext.setAttribute("fcmcredentialpath", PushServerManager.getFilePath(PushRecord.Type.android));
    pageContext.setAttribute("apnscredentialpath", PushServerManager.getFilePath(PushRecord.Type.ios));
    pageContext.setAttribute( "messageList", messageList );

    if (csrfCheck) {
        if (iosUpdate) {
            String bundleId = ParamUtils.getStringParameter(request, "bundleId", null);
            String key = ParamUtils.getStringParameter(request, "key", null);
            String teamId = ParamUtils.getStringParameter(request, "teamId", null);

            List<PushServerManager.Message> iosUpdateMessageList = PushServerManager.setIosSettings(bundleId, key, teamId);
            messageList.addAll(iosUpdateMessageList);
        }

        if (androidUpdate) {
            String projectId = ParamUtils.getStringParameter(request, "projectId", null);

            PushServerManager.Message androidUpdateMessage = PushServerManager.setAndroidSettings(projectId);
            messageList.add(androidUpdateMessage);
        }

        if (iosCredentialUpdate) {
            String apns = ParamUtils.getStringParameter(request, "apns", null);
            if (apns == null) {
                messageList.add(PushServerManager.Message.IosCredentialMissing);
            } else {
                messageList.add(PushServerManager.Message.IosCredentialSaved);
                PushServerManager.writeCredentialFileContent(apns, PushRecord.Type.ios);
            }
        }

        if (androidCredentialUpdate) {
            String fcm = ParamUtils.getStringParameter(request, "fcm", null);
            if (fcm == null) {
                messageList.add(PushServerManager.Message.AndroidCredentialMissing);
            } else {
                messageList.add(PushServerManager.Message.AndroidCredentialSaved);
                PushServerManager.writeCredentialFileContent(fcm, PushRecord.Type.android);
            }
        }
    }
%>

<html>
    <head>
        <title>
            <fmt:message key="pushserver.settings.title"/>
        </title>
        <meta name="pageID" content="pushserver-settings"/>
    </head>
    <body>

        <!-- Display all error and success messages -->
        <c:forEach var="message" items="${messageList}">
            <admin:infobox type="${message.success ? 'success' : 'error'}">
                <fmt:message key="${message.value}"/>
            </admin:infobox>
        </c:forEach>

        <form action="pushserver-settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="pushserver.settings.ios" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.ios.bundleid" />
                            </td>
                            <td>
                                <input type="text" name="bundleId" size="80" value='${pm:getProperty("pushserver.apple.apns.bundleId")}' />
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.ios.key" />
                            </td>
                            <td>
                                <input type="text" name="key" size="80" value='${pm:getProperty("pushserver.apple.apns.key")}' />
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.ios.teamid" />
                            </td>
                            <td>
                                <input type="text" name="teamId" size="80" value='${pm:getProperty("pushserver.apple.apns.teamId")}' />
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="iosUpdate">
                <fmt:message key="pushserver.settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="pushserver-settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="pushserver.settings.android" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.android.projectid" />
                            </td>
                            <td>
                                <input type="text" name="projectId" size="80" value='${pm:getProperty("pushserver.google.fcm.projectId")}' />
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="androidUpdate">
                <fmt:message key="pushserver.settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="pushserver-settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="pushserver.settings.ios.file" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.path"/>: <text readonly size="80">${apnscredentialpath}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <textarea name="apns" cols="70" rows="10"></textarea>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="iosCredentialUpdate">
                <fmt:message key="pushserver.settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="pushserver-settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="pushserver.settings.android.file" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.path" />: <text readonly size="80">${fcmcredentialpath}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <textarea name="fcm" cols="70" rows="10"></textarea>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="androidCredentialUpdate">
                <fmt:message key="pushserver.settings.save" />
            </button>
        </form>
    </body>
</html>
