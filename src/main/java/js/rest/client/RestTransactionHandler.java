package js.rest.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import js.json.Json;
import js.lang.BugError;
import js.lang.DefaultTrustManager;
import js.util.Classes;
import js.util.Strings;

public class RestTransactionHandler implements InvocationHandler {

	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String PUT = "PUT";
	private static final String DELETE = "DELETE";
	private static final String OPTIONS = "OPTIONS";

	private final Json json;
	private final String implementationURL;

	public RestTransactionHandler(String implementationURL) {
		this.json = Classes.loadService(Json.class);
		if (implementationURL.charAt(implementationURL.length() - 1) != '/') {
			implementationURL += '/';
		}
		this.implementationURL = implementationURL.replace(":rest", "");
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		URL url = new URL(implementationURL + path(method, args));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		if (isSecure(url.getProtocol())) {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
			((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
		}

		String requestMethod = requestMethod(method);
		connection.setRequestMethod(requestMethod);
		if (requestMethod.equals(POST) || requestMethod.equals(PUT)) {
			connection.setDoOutput(true);
		}

		for (Map.Entry<String, String> headerParam : headerParams(method, args).entrySet()) {
			connection.setRequestProperty(headerParam.getKey(), headerParam.getValue());
		}

		if (method.getAnnotation(BasicAuth.class) != null) {
			if (args.length != 2 || !(args[0] instanceof String) || !(args[1] instanceof String)) {
				throw new IllegalArgumentException("BasicAuth requires two parameters of type String.");
			}
			connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(Strings.concat(args[0], ":", args[1]).getBytes()));
		}

		Reader reader = new InputStreamReader(connection.getInputStream());
		try {
			return json.parse(reader, method.getGenericReturnType());
		} finally {
			reader.close();
		}
	}

	private static String requestMethod(Method method) {
		if (method.getAnnotation(GET.class) != null) {
			return GET;
		}
		if (method.getAnnotation(POST.class) != null) {
			return POST;
		}
		if (method.getAnnotation(PUT.class) != null) {
			return PUT;
		}
		if (method.getAnnotation(DELETE.class) != null) {
			return DELETE;
		}
		if (method.getAnnotation(OPTIONS.class) != null) {
			return OPTIONS;
		}

		HttpMethod httpMethod = method.getAnnotation(HttpMethod.class);
		if (httpMethod != null) {
			return httpMethod.value();
		}

		if (method.getName().startsWith("get")) {
			return GET;
		}
		if (method.getName().startsWith("post")) {
			return POST;
		}
		if (method.getName().startsWith("set")) {
			return POST;
		}
		if (method.getName().startsWith("put")) {
			return PUT;
		}

		throw new BugError("Unable to infer HTTP method for service method |%s|.", method);
	}

	private static String path(Method method, Object[] args) {
		Path pathAnnotation = method.getAnnotation(Path.class);
		if (pathAnnotation == null) {
			throw new BugError("Missing <Path> annotation from method |%s|.", method);
		}

		String pathFormat = pathAnnotation.value();

		Map<String, Object> variables = new HashMap<>();

		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < args.length; ++i) {
			if (method.getAnnotation(BasicAuth.class) != null) {
				continue;
			}
			if (annotations[i].length == 0) {
				throw new BugError("Missing annotation on parameter |%d| from method |%s|.", i, method);
			}
			for (int j = 0; j < annotations[i].length; ++j) {
				Annotation annotation = annotations[i][j];
				if (annotation instanceof PathParam) {
					PathParam pathParam = (PathParam) annotation;
					variables.put(pathParam.value(), args[i]);
				}
			}
		}

		return format(pathFormat, variables);
	}

	private static Map<String, String> headerParams(Method method, Object[] args) {
		Map<String, String> headerParams = new HashMap<>();

		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < args.length; ++i) {
			if (method.getAnnotation(BasicAuth.class) != null) {
				continue;
			}
			if (annotations[i].length == 0) {
				throw new BugError("Missing annotation on parameter |%d| from method |%s|.", i, method);
			}
			for (int j = 0; j < annotations[i].length; ++j) {
				Annotation annotation = annotations[i][j];
				if (annotation instanceof HeaderParam) {
					HeaderParam pathParam = (HeaderParam) annotation;
					headerParams.put(pathParam.value(), args[i] instanceof String ? (String) args[i] : args[i].toString());
				}
			}
		}

		return headerParams;

	}

	public static String format(String pathFormat, Map<String, Object> variables) {
		// 0: NONE
		// 1: TEXT
		// 2: VARIABLE
		int state = 1;

		StringBuilder pathBuilder = new StringBuilder();
		StringBuilder variableNameBuilder = new StringBuilder();

		for (int charIndex = 0; charIndex < pathFormat.length(); ++charIndex) {
			char c = pathFormat.charAt(charIndex);
			switch (state) {
			case 1:
				if (c != '{') {
					pathBuilder.append(c);
					break;
				}
				state = 2;
				variableNameBuilder.setLength(0);
				break;

			case 2:
				if (c != '}') {
					variableNameBuilder.append(c);
					break;
				}
				state = 1;
				pathBuilder.append(variables.get(variableNameBuilder.toString()));
				break;
			}
		}
		return pathBuilder.toString();
	}

	/** Standard HTTP protocol name. */
	private static final String HTTP = "http";

	/** Secure HTTP protocol name. */
	private static final String HTTPS = "https";

	/**
	 * Predicate to test if given protocol is secure.
	 * 
	 * @param protocol protocol to test if secure.
	 * @return true it given <code>protocol</code> is secure.
	 * @throws BugError if given protocol is not supported.
	 */
	private static boolean isSecure(String protocol) throws BugError {
		if (HTTP.equalsIgnoreCase(protocol)) {
			return false;
		} else if (HTTPS.equalsIgnoreCase(protocol)) {
			return true;
		} else {
			throw new BugError("Unsupported protocol |%s| for HTTP transaction.", protocol);
		}
	}
}