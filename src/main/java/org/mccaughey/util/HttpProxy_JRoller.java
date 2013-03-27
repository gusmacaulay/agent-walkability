package org.mccaughey.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

public class HttpProxy_JRoller extends HttpServlet {

	private static final Logger LOG = Logger.getLogger(HttpProxy_JRoller.class
			.getName());

	/**
	 * To set a referer limitation, add an init parameter to the Servlet
	 * reference in your web.xml, setting the parameter name limitReferer to the
	 * url prefix being allowed for referals using this Servlet.
	 */
	String limitReferer = null;
	String proxyTo = null;
	String prefix = null;
	String proxyUserName = null;
	String proxyPassword = null;

	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		limitReferer = getServletConfig().getInitParameter("limitReferer");
		proxyTo = servletConfig.getInitParameter("ProxyTo");
		prefix = servletConfig.getInitParameter("Prefix");
		proxyUserName = servletConfig.getInitParameter("ProxyUserName");
		proxyPassword = servletConfig.getInitParameter("ProxyPassword");
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		if (limitReferer != null && limitReferer.trim().length() > 0) {
			String referer = req.getHeader("Referer");
			String localName = req.getLocalName();
			if (!referer.startsWith(limitReferer)) {
				final String msg = new StringBuilder()
						.append("Blocked request for referer ").append(referer)
						.append(", localName->").append(localName)
						.append(". The referer acceptance is limited to ")
						.append(limitReferer).toString();
				// if (LOG.isEnabledFor(Level.ERROR)) {
				// LOG.error("[doPost]: " + msg);
				// }
				PrintWriter out = res.getWriter();
				out.println(msg);
				out.close();
				return;
			}
		}
		String uri = req.getRequestURI();
		if (req.getQueryString() != null)
			uri += "?" + req.getQueryString();
		URI dstUri = null;
		try {
			if (!uri.startsWith(prefix)) {
				dstUri = new URI(proxyTo + uri).normalize();
			}
			dstUri = new URI(proxyTo + uri.substring(prefix.length()))
					.normalize();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		URL url =  dstUri.toURL();
		String user = null, password = null, method = "POST", post = null;
		int timeout = 0;

		Set entrySet = req.getParameterMap().entrySet();
		Map headers = new HashMap();
		for (Object anEntrySet : entrySet) {
			Map.Entry header = (Map.Entry) anEntrySet;
			String key = (String) header.getKey();
			String value = ((String[]) header.getValue())[0];
			// if ("user".equals(key)) {
			// user = value;
			// } else if ("password".equals(key)) {
			// password = value;
			// } else
			if ("timeout".equals(key)) {
				timeout = Integer.parseInt(value);
			} else if ("method".equals(key)) {
				method = value;
			} else if ("post".equals(key)) {
				post = value;
			}
			// else if ("url".equals(key)) {
			// url = new URL(value);
			// }
			else {
				headers.put(key, value);
			}
		}
		user =proxyUserName;
		password = proxyPassword;
		
		if (url != null) {
			String digest = null;
			if (user != null && password != null) {
				digest = "Basic "
						+ new String(
								Base64.encodeBase64((user + ":" + password)
										.getBytes()));
			}

			boolean foundRedirect = false;
			do {

				HttpURLConnection urlConnection = (HttpURLConnection) url
						.openConnection();
				if (digest != null) {
					urlConnection.setRequestProperty("Authorization", digest);
				}
				urlConnection.setDoOutput(true);
				urlConnection.setDoInput(true);
				urlConnection.setUseCaches(false);
				urlConnection.setInstanceFollowRedirects(false);
				urlConnection.setRequestMethod(method);
				if (timeout > 0) {
					urlConnection.setConnectTimeout(timeout);
				}

				// set headers
				Set headersSet = headers.entrySet();
				for (Object aHeadersSet : headersSet) {
					Map.Entry header = (Map.Entry) aHeadersSet;
					urlConnection.setRequestProperty((String) header.getKey(),
							(String) header.getValue());
				}

				// send post
				if (post != null) {
					OutputStreamWriter outRemote = new OutputStreamWriter(
							urlConnection.getOutputStream());
					outRemote.write(post);
					outRemote.flush();
				}

				// get content type
				String contentType = urlConnection.getContentType();
				if (contentType != null) {
					res.setContentType(contentType);
				}

				// get reponse code
				int responseCode = urlConnection.getResponseCode();

				if (responseCode == 302) {
					// follow redirects
					String location = urlConnection.getHeaderField("Location");
					url = new URL(location);
					foundRedirect = true;
				} else {
					res.setStatus(responseCode);
					BufferedInputStream in;
					if (responseCode == 200 || responseCode == 201) {
						in = new BufferedInputStream(
								urlConnection.getInputStream());
					} else {
						in = new BufferedInputStream(
								urlConnection.getErrorStream());
					}

					// send output to client
					BufferedOutputStream out = new BufferedOutputStream(
							res.getOutputStream());
					int c;
					while ((c = in.read()) >= 0) {
						out.write(c);
					}
					out.flush();
				}
			} while (foundRedirect);

		} else {
			// if (LOG.isEnabledFor(Level.ERROR)) {
			// LOG.error("[doPost]: Given url was null.");
			// }
		}
	}
}