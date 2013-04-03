package org.mccaughey.proxy.servlet;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.servlets.ProxyServlet;
import org.mccaughey.pathGenerator.config.ConnectionsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * ProxyServletWithAuth Proxy.
 * 
 * This convenience extension to ProxyServlet configures the servlet as a
 * ProxyServletWithAuth proxy. The servlet is configured with init parameters:
 * <ul>
 * <li>ProxyTo - a URI like http://host:80/context to which the request is
 * proxied.
 * <li>Prefix - a URI prefix that is striped from the start of the forwarded
 * URI.
 * </ul>
 * For example, if a request was received at /foo/bar and the ProxyTo was
 * http://host:80/context and the Prefix was /foo, then the request would be
 * proxied to http://host:80/context/bar
 * 
 */
public class ProxyServletWithAuth extends ProxyServlet {
	private ConnectionsInfo connectionsInfo = null;

	private String _prefix;
	private String _proxyTo;

	public ProxyServletWithAuth() {
	}

	public ProxyServletWithAuth(String prefix, String host, int port) {
		this(prefix, "http", host, port, null);
	}

	public ProxyServletWithAuth(String prefix, String schema, String host,
			int port, String path) {
		try {
			if (prefix != null) {
				_prefix = new URI(prefix).normalize().toString();
			}
			_proxyTo = new URI(schema, null, host, port, path, null, null)
					.normalize().toString();
		} catch (URISyntaxException ex) {
			_log.debug("Invalid URI syntax", ex);
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String prefix = config.getInitParameter("Prefix");
		_prefix = prefix == null ? _prefix : prefix;

		// Adjust prefix value to account for context path
		String contextPath = _context.getContextPath();
		_prefix = _prefix == null ? contextPath : (contextPath + _prefix);

		String proxyTo = config.getInitParameter("ProxyTo");
		_proxyTo = proxyTo == null ? _proxyTo : proxyTo;

		if (_proxyTo == null) {
			throw new UnavailableException("ProxyTo parameter is requred.");
		}
		if (!_prefix.startsWith("/")) {
			throw new UnavailableException(
					"Prefix parameter must start with a '/'.");
		}
		_log.info(config.getServletName() + " @ " + _prefix + " to " + _proxyTo);
	}

	@Override
	protected HttpURI proxyHttpURI(final String scheme,
			final String serverName, int serverPort, final String uri)
			throws MalformedURLException {
		try {
			if (!uri.startsWith(_prefix)) {
				return null;
			}

			URI dstUri = new URI(_proxyTo + uri.substring(_prefix.length()))
					.normalize();

			if (!validateDestination(dstUri.getHost(), dstUri.getPath())) {
				return null;
			}
			return new HttpURI(dstUri.toString());
		} catch (URISyntaxException ex) {
			throw new MalformedURLException(ex.getMessage());
		}
	}

	/**
	 * Extension point for subclasses to customize an exchange. Useful for
	 * setting timeouts etc. The default implementation does nothing.
	 * 
	 * @param exchange
	 * @param request
	 */
	protected void customizeExchange(HttpExchange exchange,
			HttpServletRequest request) {
		String digest = null;
		ConnectionsInfo connectionInfo = getConnectionsInfo(request
				.getSession());
		String user = connectionInfo.getGeoserverUser();
		String password = connectionInfo.getGeoserverPassword();
		// String user="aurin";
		// String password="aurinaccess";
		if (user != null && password != null) {
			digest = "Basic "
					+ new String(Base64.encodeBase64((user + ":" + password)
							.getBytes()));
		}

		exchange.addRequestHeader("Authorization", digest);
	}

	public synchronized ConnectionsInfo getConnectionsInfo(HttpSession session) {
		if (this.connectionsInfo == null) {
			ApplicationContext ctx = WebApplicationContextUtils
					.getWebApplicationContext(session.getServletContext());
			this.connectionsInfo = (ConnectionsInfo) ctx
					.getBean(ConnectionsInfo.class);
		}
		return this.connectionsInfo;
	}

}