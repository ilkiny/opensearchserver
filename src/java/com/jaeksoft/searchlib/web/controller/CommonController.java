/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2012 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Tab;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.ClientCatalog;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.analysis.LanguageEnum;
import com.jaeksoft.searchlib.user.Role;
import com.jaeksoft.searchlib.user.User;
import com.jaeksoft.searchlib.web.AbstractServlet;
import com.jaeksoft.searchlib.web.StartStopListener;
import com.jaeksoft.searchlib.web.Version;

public abstract class CommonController implements EventInterface {

	@WireVariable
	protected Session session;

	@WireVariable
	protected Desktop desktop;

	public CommonController() throws SearchLibException {
		super();
		reset();
	}

	final protected static StringBuffer getBaseUrl(Execution exe) {
		int port = exe.getServerPort();
		StringBuffer sb = new StringBuffer();
		sb.append(exe.getScheme());
		sb.append("://");
		sb.append(exe.getServerName());
		if (port != 80) {
			sb.append(":");
			sb.append(port);
		}
		sb.append(exe.getContextPath());
		return sb;

	}

	final public static StringBuffer getBaseUrl() {
		Execution exe = Executions.getCurrent();
		return getBaseUrl(exe);
	}

	final public static StringBuffer getApiUrl(String servletPathName)
			throws UnsupportedEncodingException {
		Execution exe = Executions.getCurrent();
		StringBuffer sb = getBaseUrl();
		Client client = (Client) exe.getSession().getAttribute(
				ScopeAttribute.CURRENT_CLIENT.name());
		User user = (User) exe.getSession().getAttribute(
				ScopeAttribute.LOGGED_USER.name());
		return AbstractServlet.getApiUrl(sb, servletPathName, client, user);
	}

	protected Object getAttribute(ScopeAttribute scopeAttribute,
			Object defaultValue) {
		Object o = scopeAttribute.get(session);
		return o == null ? defaultValue : o;
	}

	protected Object getAttribute(ScopeAttribute scopeAttribute) {
		return scopeAttribute.get(session);
	}

	protected void setAttribute(ScopeAttribute scopeAttribute, Object value) {
		scopeAttribute.set(session, value);
	}

	protected Object getRecursiveComponentAttribute(Component component,
			String attributeName) {
		Object attr = null;
		while (component != null)
			if ((attr = component.getAttribute(attributeName)) != null)
				return attr;
			else
				component = component.getParent();
		return null;
	}

	public Version getVersion() throws IOException {
		return StartStopListener.getVersion();
	}

	public Client getClient() throws SearchLibException {
		return (Client) getAttribute(ScopeAttribute.CURRENT_CLIENT);
	}

	protected void setClient(Client client) {
		setAttribute(ScopeAttribute.CURRENT_CLIENT, client);
		PushEvent.eventClientChange.publish();
	}

	public boolean isInstanceValid() throws SearchLibException {
		return getClient() != null;
	}

	public boolean isInstanceNotValid() throws SearchLibException {
		return getClient() == null;
	}

	public String getCurrentPage() throws SearchLibException {
		String page = isLogged() ? "controlpanel.zul" : "login.zul";
		return "WEB-INF/zul/" + page;
	}

	public User getLoggedUser() {
		return (User) getAttribute(ScopeAttribute.LOGGED_USER);
	}

	public boolean isAdmin() throws SearchLibException {
		User user = getLoggedUser();
		if (user == null)
			return false;
		return user.isAdmin();
	}

	public boolean isNoUserList() throws SearchLibException {
		return ClientCatalog.getUserList().isEmpty();
	}

	public boolean isAdminOrNoUser() throws SearchLibException {
		if (isNoUserList())
			return true;
		return isAdmin();
	}

	public boolean isAdminOrMonitoringOrNoUser() throws SearchLibException {
		if (isNoUserList())
			return true;
		User user = getLoggedUser();
		if (user == null)
			return false;
		return user.isAdmin() || user.isMonitoring();
	}

	public boolean isLogged() throws SearchLibException {
		if (isNoUserList())
			return true;
		return getLoggedUser() != null;
	}

	@Command
	@GlobalCommand
	public void reload() throws SearchLibException {
		BindUtils.postNotifyChange(null, null, this, "*");
	}

	@Command
	@GlobalCommand
	public void refresh() throws SearchLibException {
		reset();
		reload();
	}

	public LanguageEnum[] getLanguageEnum() {
		return LanguageEnum.values();
	}

	public List<String> getAnalyzerNameList() throws SearchLibException {
		Client client = getClient();
		if (client == null)
			return null;
		List<String> analyzerNameList = new ArrayList<String>(0);
		analyzerNameList.add("");
		for (String n : client.getSchema().getAnalyzerList().getNameSet())
			analyzerNameList.add(n);
		return analyzerNameList;
	}

	protected void flushPrivileges(User user) {
		PushEvent.eventFlushPrivileges.publish(user);
	}

	@Command
	public void onLogout() {
		for (ScopeAttribute attr : ScopeAttribute.values())
			setAttribute(attr, null);
		PushEvent.eventLogout.publish();
		Executions.sendRedirect("/");
	}

	protected abstract void reset() throws SearchLibException;

	@Override
	@GlobalCommand
	public void eventClientChange() throws SearchLibException {
		System.out.println("eventClientChange " + this);
		refresh();
	}

	@Override
	@GlobalCommand
	public void eventClientSwitch(Client client) throws SearchLibException {
		if (client == null)
			return;
		Client currentClient = getClient();
		if (currentClient == null)
			return;
		if (!client.getIndexName().equals(currentClient.getIndexName()))
			return;
		refresh();
	}

	@Override
	@GlobalCommand
	public void eventFlushPrivileges(User user) throws SearchLibException {
		refresh();
	}

	@Override
	@GlobalCommand
	public void eventDocumentUpdate(Client client) throws SearchLibException {
	}

	@Override
	@GlobalCommand
	public void eventRequestListChange(Client client) throws SearchLibException {
	}

	@Override
	@GlobalCommand
	public void eventSchemaChange(Client client) throws SearchLibException {
		System.out.println("eventSchemaChange " + this);
	}

	@Override
	@GlobalCommand
	public void eventLogout(User user) throws SearchLibException {
		refresh();
	}

	protected String getIndexName() throws SearchLibException {
		Client client = getClient();
		if (client == null)
			return null;
		return getClient().getIndexName();
	}

	public boolean isQueryRights() throws SearchLibException {
		if (!isLogged() || !isInstanceValid())
			return false;
		if (isNoUserList())
			return true;
		return getLoggedUser().hasAnyRole(getIndexName(), Role.GROUP_INDEX);
	}

	public boolean isUpdateRights() throws SearchLibException {
		if (!isLogged() || !isInstanceValid())
			return false;
		if (isNoUserList())
			return true;
		return getLoggedUser().hasAnyRole(getIndexName(), Role.INDEX_UPDATE);
	}

	public boolean isSchemaRights() throws SearchLibException {
		if (!isLogged() || !isInstanceValid())
			return false;
		if (isNoUserList())
			return true;
		return getLoggedUser().hasAnyRole(getIndexName(), Role.INDEX_SCHEMA);
	}

	protected final static void buildTabPath(Component component,
			List<String> tabPath) throws SearchLibException {
		if (component == null)
			return;
		if (!component.isVisible())
			return;
		if (component instanceof Tab) {
			Tab tab = (Tab) component;
			if (tab.isSelected()) {
				String lbl = tab.getTooltiptext();
				if (lbl == null || lbl.length() == 0)
					lbl = tab.getLabel();
				tabPath.add(lbl);
			}
		}
		List<Component> children = component.getChildren();
		if (children == null)
			return;
		for (Component comp : children)
			buildTabPath(comp, tabPath);
	}

	@Command
	final public void onHelp(@BindingParam("target") Component component)
			throws SearchLibException, UnsupportedEncodingException {
		List<String> tabPath = new ArrayList<String>();
		buildTabPath(component.getRoot(), tabPath);
		String path = URLEncoder.encode(StringUtils.join(tabPath, " - "),
				"UTF-8");
		Executions.getCurrent().sendRedirect(
				"http://www.open-search-server.com/confluence/display/EN/Inline+help+-+"
						+ path, "_blank");
	}

}