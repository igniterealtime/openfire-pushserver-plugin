<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="org.jivesoftware.util.*" %>
<%@ page import="org.igniterealtime.openfire.plugins.pushserver.PushServerManager" %>
<%@ page import="org.igniterealtime.openfire.plugins.pushserver.models.PushRecord" %>
<%@ page import="org.igniterealtime.openfire.plugins.pushserver.PushServerProperty" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/pushservermanager.tld" prefix="pm"%>

<%
    boolean androidUpdate = request.getParameter("androidUpdate") != null;
    boolean iosUpdate = request.getParameter("iosUpdate") != null;
    boolean androidCredentialUpdate = request.getParameter("androidCredentialUpdate") != null;
    boolean iosCredentialUpdate = request.getParameter("iosCredentialUpdate") != null;

    String baseUrl = "pushserver-settings.jsp"

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
    pageContext.setAttribute("fcmcredentialpath", PushServerProperty.FCM_CREDENTIAL_FILE_PATH);
    pageContext.setAttribute("apnscredentialpath", PushServerProperty.APNS_PKCS8_FILE_PATH);

    if (csrfCheck) {
        String newUrl = baseUrl + "?savesucceeded=true";
        if (iosUpdate) {
            String bundleId = ParamUtils.getStringParameter(request, "bundleId", null);
            String key = ParamUtils.getStringParameter(request, "key", null);
            String teamId = ParamUtils.getStringParameter(request, "teamId", null);
            String sandbox = ParamUtils.getStringParameter(request, "sandbox", null);

            PushServerManager.setIosSettings(bundleId, key, teamId, sandbox);
            response.sendRedirect(newUrl);
        }

        if (androidUpdate) {
            String projectId = ParamUtils.getStringParameter(request, "projectId", null);
            PushServerManager.setAndroidSettings(projectId);
            response.sendRedirect(newUrl);
        }

        if (iosCredentialUpdate) {
            String apns = ParamUtils.getStringParameter(request, "apns", null);
            PushServerManager.writeCredentialFileContent(apns, PushRecord.Type.ios);
            response.sendRedirect(newUrl);
        }

        if (androidCredentialUpdate) {
            String fcm = ParamUtils.getStringParameter(request, "fcm", null);
            PushServerManager.writeCredentialFileContent(fcm, PushRecord.Type.android);
            response.sendRedirect(newUrl);
        }
    }
%>

<html>
    <head>
        <title>
            <fmt:message key="pushserver.settings.title"/>
        </title>
        <meta name="pageID" content="settings"/>
    </head>
    <body>
        <c:choose>
            <c:when test="${param.savesucceeded eq 'true'}">
                <div class="jive-success">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key='pushserver.settings.savesuccess' />"/></td>
                                <td class="jive-icon-label">
                                    <fmt:message key='pushserver.settings.savesuccess' />
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <br />
            </c:when>
            <c:when test="${param.savesucceeded eq 'false'}">
                <div class="jive-error">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key='pushserver.settings.savefail' />"/></td>
                                <td class="jive-icon-label">
                                    <fmt:message key='pushserver.settings.savefail' />
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <br />
            </c:when>
        </c:choose>

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
                        <tr>
                            <td>
                                <fmt:message key="pushserver.settings.ios.sandbox" />
                            </td>
                            <td>
                                <input type="text" name="sandbox" size="80" value='${pm:getProperty("pushserver.apple.apns.sandbox")}' />
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
