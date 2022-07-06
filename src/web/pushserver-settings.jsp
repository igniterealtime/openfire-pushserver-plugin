<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="org.jivesoftware.util.*" %>
<%@ page import="org.igniterealtime.openfire.plugins.pushserver.PushServerManager" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>

<%@ taglib uri="admin" prefix="admin" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    final ArrayList<PushServerManager.Message> messageList = new ArrayList<>();

    boolean iosUpdate = request.getParameter("iosUpdate") != null;
    boolean androidUpdate = request.getParameter("androidUpdate") != null;

    // If 'save operation' has been done, adds its result(s) to `messageList`.
    if (androidUpdate || iosUpdate) {
        Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
        String csrfParam = ParamUtils.getParameter(request, "csrf");

        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            messageList.add(PushServerManager.Message.CSRFError);
        } else {
            if (iosUpdate) {
                String bundleId = ParamUtils.getStringParameter(request, "bundleId", null);
                String teamId = ParamUtils.getStringParameter(request, "teamId", null);
                String keyId = ParamUtils.getStringParameter(request, "keyId", null);
                String encryptionKeyContent = ParamUtils.getStringParameter(request, "encryptionKeyContent", null);

                List<PushServerManager.Message> iosUpdateMessageList =
                        PushServerManager.setIosSettings(bundleId, teamId, keyId, encryptionKeyContent);
                messageList.addAll(iosUpdateMessageList);
            }

            if (androidUpdate) {
                String projectId = ParamUtils.getStringParameter(request, "projectId", null);
                String serviceAccountKeyContent = ParamUtils.getStringParameter(request, "serviceAccountKeyContent", null);

                List<PushServerManager.Message> androidUpdateMessageList =
                        PushServerManager.setAndroidSettings(projectId, serviceAccountKeyContent);
                messageList.addAll(androidUpdateMessageList);
            }
        }
    }

    String csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);
    pageContext.setAttribute( "messageList", messageList );
%>

<html>
    <head>
        <title>
            <fmt:message key="push.server.settings.title"/>
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

        <p><fmt:message key="push.server.settings.description.detail" /></p>
        <br />

        <form action="pushserver-settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="push.server.settings.ios.title" />
            </div>
            <div class="jive-contentBox">
                <p><fmt:message key="push.server.settings.ios.description" /></p>
                <br />
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td style="vertical-align:top">
                                <label for="bundleId"><fmt:message key="push.server.settings.ios.bundleId" /></label>
                            </td>
                            <td>
                                <input type="text" name="bundleId" id="bundleId" size="80" value='${admin:getProperty("pushserver.apple.apns.bundleId", "")}' />
                            </td>
                        </tr>
                        <tr>
                            <td style="vertical-align:top">
                                <label for="teamId"><fmt:message key="push.server.settings.ios.teamId" /></label>
                            </td>
                            <td>
                                <input type="text" name="teamId" id="teamId" size="80" value='${admin:getProperty("pushserver.apple.apns.teamId", "")}' />
                            </td>
                        </tr>
                        <tr>
                            <td style="vertical-align:top">
                                <label for="keyId"><fmt:message key="push.server.settings.ios.apns.keyId" /></label>
                            </td>
                            <td>
                                <input type="text" name="keyId" id="keyId" size="80" value='${admin:getProperty("pushserver.apple.apns.key", "")}' />
                            </td>
                        </tr>
                        <tr>
                        <td style="vertical-align:top">
                            <label for="encryptionKeyContent"><fmt:message key="push.server.settings.ios.apns.encryptionKey" /></label>
                        </td>
                        <td>
                            <textarea name="encryptionKeyContent" id="encryptionKeyContent" cols="70" rows="10"></textarea>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="iosUpdate">
                <fmt:message key="push.server.settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="pushserver-settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="push.server.settings.android" />
            </div>
            <div class="jive-contentBox">
                <p><fmt:message key="push.server.settings.android.description" /></p>
                <br />
                <table cellspacing="0" border="0">
                    <tbody>
                    <tr>
                        <td style="vertical-align:top">
                            <label for="projectId"><fmt:message key="push.server.settings.android.projectId" /></label>
                        </td>
                        <td>
                            <input type="text" name="projectId" id="projectId" size="80" value='${admin:getProperty("pushserver.google.fcm.projectId", "")}' />
                        </td>
                    </tr>
                    <tr>
                        <td style="vertical-align:top">
                            <label for="serviceAccountKeyContent"><fmt:message key="push.server.settings.android.serviceAccountKey" /></label>
                        </td>
                        <td>
                            <textarea name="serviceAccountKeyContent" id="serviceAccountKeyContent" cols="70" rows="10"></textarea>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="androidUpdate">
                <fmt:message key="push.server.settings.save" />
            </button>
        </form>
        <br />
        <br />

    </body>
</html>
