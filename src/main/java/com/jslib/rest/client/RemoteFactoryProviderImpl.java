package com.jslib.rest.client;

import js.rmi.RemoteFactory;
import js.rmi.RemoteFactoryProvider;

public class RemoteFactoryProviderImpl implements RemoteFactoryProvider {
	private static final String[] PROTOCOLS = new String[] { "http:rest", "https:rest" };
	private static final RemoteFactory FACTORY = new RestClientFactory();

	@Override
	public String[] getProtocols() {
		return PROTOCOLS;
	}

	@Override
	public RemoteFactory getRemoteFactory() {
		return FACTORY;
	}
}
