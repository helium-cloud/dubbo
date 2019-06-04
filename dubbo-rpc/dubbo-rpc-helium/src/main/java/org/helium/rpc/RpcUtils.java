//package org.helium.rpc;
//
//import org.helium.rpc.channel.RpcEndpoint;
//import org.helium.rpc.client.RpcProxyFactory;
//import org.helium.rpc.client.RpcTransparentClient;
//import org.helium.util.Outer;
//import org.helium.util.StringUtils;
//
//import java.util.Arrays;
//
//public class RpcUtils {
//
//	public static <E> E getService(String url, Class<E> intfClazz, ) {
//        Outer<String> host = new Outer<>();
//        Outer<String> service = new Outer<>();
//        StringUtils.splitWithLast(url, "/", host, service);
//        RpcEndpoint rpcEndpoint = RpcEndpointFactory.parse(host.value());
//        return RpcProxyFactory.getTransparentProxy(service.value(), intfClazz, () -> rpcEndpoint);
//    }
//
//	public static void check(Object o){
//		if (o instanceof RpcTransparentClient){
//			RpcTransparentClient client = (RpcTransparentClient) o;
//			client.
//		}
//	}
//}
