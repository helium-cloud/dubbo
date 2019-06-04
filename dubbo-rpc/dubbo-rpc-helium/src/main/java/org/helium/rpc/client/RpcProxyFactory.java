/*
 * FAE, Feinno App Engine
 *
 * Create by gaolei 2010-11-25
 *
 * Copyright (c) 2010 北京新媒传信科技有限公司
 */
package org.helium.rpc.client;

import org.helium.rpc.api.proxy.ProxyFactory;
import org.helium.rpc.channel.RpcChannelSettings;
import org.helium.rpc.channel.RpcChannelSupportFlag;
import org.helium.rpc.channel.RpcClientChannel;
import org.helium.rpc.channel.RpcEndpoint;
import org.helium.rpc.server.RpcMethod;
import org.helium.rpc.server.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 客户端使用Rpc的工厂类, 全静态方法
 * <p>
 * Created by Coral
 */
public class RpcProxyFactory implements ProxyFactory {
	private final Logger LOGGER = LoggerFactory.getLogger(RpcProxyFactory.class);
	private Map<RpcMethodStubKey, RpcMethodStub> stubs = new ConcurrentHashMap<RpcMethodStubKey, RpcMethodStub>();

	private Object lock = new Object();
	private Map<String, RpcProtocolResolver> resolvers;


	public static RpcProxyFactory INSTANCE = new RpcProxyFactory();

	private RpcProxyFactory() {
		resolvers = new HashMap<>();
		resolvers.put("helium", new RpcProtocolResolverSimple("helium"));
		resolvers.put("http", new RpcProtocolResolverSimple("http"));
	}

	@Override
	public ProxyFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * 增加一个协议解析器
	 *
	 * @param protocol
	 * @param resolver
	 */
	public void addProtocolResolver(String protocol, RpcProtocolResolver resolver) {
		//
		// 避免对resolvers上锁
		synchronized (lock) {
			Map<String, RpcProtocolResolver> map = new HashMap<>();
			resolvers.forEach((k, v) -> map.put(k, v));
			map.put(protocol, resolver);
			resolvers = map;
		}
	}


	/**
	 * 获取一个用于调用服务器端的代理类
	 *
	 * @param ep
	 * @param service
	 * @param method
	 * @return
	 */
	public RpcMethodStub getMethodStub(RpcEndpoint ep, String service, String method) {
		//
		// 与ep中的模式存在依赖性
		// 生存周期,
		try {
			RpcMethodStubKey key = new RpcMethodStubKey(ep, service, method);
			RpcMethodStub stub = stubs.get(key);
			if (stub == null) {
				RpcClientChannel channel = ep.getClientChannel();
				RpcClientTransactionHandler handler = null;
				RpcChannelSettings settings = channel.getSettings();
				if (settings.getSupportFlags().has(RpcChannelSupportFlag.CONNECTION)) {
					if (ep.getParameter("NLB") != null) {
						handler = new RpcClientTransactionHandlerNLB(ep, service, method);
					} else {
						handler = new RpcClientTransactionHandlerDirect(ep, service, method, null);
					}
				} else {
					handler = new RpcClientTransactionHandlerShort(ep, service, method);
				}
				stub = new RpcMethodStub(handler);
				stubs.put(key, stub);
			}
			return stub;
		} catch (Exception ex) {
			String msg = "GetMethodStub error " + ep.toString() + "/" + service + "." + method;
			throw new IllegalArgumentException(msg, ex);
		}
	}

	/**
	 * 获取一个用于调用的透明代理
	 *
	 * @param ep
	 * @param intf 透明代理的声明类
	 * @return
	 */
	public <I> I getService(RpcEndpoint ep, Class<I> intf) {
		RpcService sa = intf.getAnnotation(RpcService.class);
		if (sa == null) {
			throw new IllegalArgumentException("@RpcService not found in:" + intf);
		}
		String serviceName = sa.value();
		Map<String, RpcMethodStub> stubs = new HashMap<String, RpcMethodStub>();
		for (Method method : intf.getMethods()) {
			RpcMethod ma = method.getAnnotation(RpcMethod.class);
			String methodName = ma.value();
			RpcMethodStub stub = getMethodStub((RpcEndpoint) ep, serviceName, methodName);
			if (!method.getReturnType().equals(Void.class) && method.getReturnType() != void.class) {
				stub.setResultsClass(method.getReturnType());
			}
			stubs.put(method.getName(), stub);
		}
		//ClassLoader cl = Thread.currentThread().getContextClassLoader();
		ClassLoader cl = intf.getClassLoader();
		InvocationHandler handler = new RpcInvocationHandler(stubs);
		//noinspection unchecked
		return (I) Proxy.newProxyInstance(cl, new Class<?>[]{intf}, handler);
	}

	public <E> E getTransparentProxy(ClassLoader cl, String serviceName, Class<E> serviceInterface, Supplier<RpcEndpoint> router) {
		return getTransparentProxy(cl, serviceName, serviceInterface, router, 0);
	}

	/**
	 * 获取透明代理
	 *
	 * @param cl
	 * @param serviceName
	 * @param serviceInterface
	 * @param router
	 * @param <E>
	 * @return
	 */
	public <E> E getTransparentProxy(ClassLoader cl, String serviceName, Class<E> serviceInterface, Supplier<RpcEndpoint> router, int timeout) {
		return (E) RpcTransparentClient.createProxy(cl, serviceName, serviceInterface, router, timeout);
	}

	public <E> E getTransparentProxy(String serviceName, Class<E> serviceInterface, Supplier<RpcEndpoint> router) {
		return getTransparentProxy(serviceName, serviceInterface, router, 0);
	}

	/**
	 * 获取透明Rpc代理
	 *
	 * @param serviceName
	 * @param serviceInterface
	 * @param router
	 * @param <E>
	 * @return
	 */
	public <E> E getTransparentProxy(String serviceName, Class<E> serviceInterface, Supplier<RpcEndpoint> router, int timeout) {
		return getTransparentProxy(RpcProxyFactory.class.getClassLoader(), serviceName, serviceInterface, router, timeout);
	}


	public <E> E getTransparentProxy(String url, Class<E> serviceInterface) {
		return getTransparentProxy(url, serviceInterface, 0);
	}

	/**
	 * 获取透明Rpc代理
	 * helium://group.MessageReceiver
	 *
	 * @param url
	 * @param <E>
	 * @return
	 */
	public <E> E getTransparentProxy(String url, Class<E> serviceInterface, int timeout) {
		RpcServiceUrl serviceUrl = RpcServiceUrl.parse(url);
		RpcProtocolResolver resolver = resolvers.get(serviceUrl.getProtocol());
		if (resolver == null) {
			throw new IllegalArgumentException("unknown protocol: " + serviceUrl);
		}
		RpcProtocolResolver.ResolveResult result = resolver.resolveServiceUrl(serviceUrl);
		if (result == null) {
			throw new IllegalArgumentException("empty resolve result: " + serviceUrl);
		}

		return getTransparentProxy(RpcProxyFactory.class.getClassLoader(), result.getServiceName(), serviceInterface, result.getRouter(), timeout);
	}

}
