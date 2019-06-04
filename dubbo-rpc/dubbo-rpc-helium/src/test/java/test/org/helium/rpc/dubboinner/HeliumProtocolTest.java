/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.org.helium.rpc.dubboinner;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.serialize.nativejava.NativeJavaSerialization;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * HeliumProtocolTest
 */
public class HeliumProtocolTest {

    @Test
    public void testHeliumProtocol() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("helium://127.0.0.1:5342/heliumService?test");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<HeliumService> invoker = protocol.refer(HeliumService.class, url);
        HeliumService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testGenericInvoke() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker, true);
        String result = (String) client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"haha"});
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testGenericInvokeWithNativeJava() throws IOException, ClassNotFoundException {
        HeliumServiceImpl server = new HeliumServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0&generic=nativejava");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker);

        Serialization serialization = new NativeJavaSerialization();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
        objectOutput.writeObject("haha");
        objectOutput.flushBuffer();

        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{byteArrayOutputStream.toByteArray()});
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) result);
        ObjectInput objectInput = serialization.deserialize(url, byteArrayInputStream);
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", objectInput.readObject());
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testGenericInvokeWithBean() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0&generic=bean");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<GenericService> invoker = protocol.refer(GenericService.class, url);
        GenericService client = proxyFactory.getProxy(invoker);

        JavaBeanDescriptor javaBeanDescriptor = JavaBeanSerializeUtil.serialize("haha");

        Object result = client.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{javaBeanDescriptor});
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) result));
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testOverload() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0&hessian.overload.method=true&hessian2.request=false");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<HeliumService> invoker = protocol.refer(HeliumService.class, url);
        HeliumService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertEquals("Hello, haha", result);
        result = client.sayHello("haha", 1);
        Assertions.assertEquals("Hello, haha. ", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testSimpleClient() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        Assertions.assertFalse(server.isCalled());
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0&client=simple");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<HeliumService> invoker = protocol.refer(HeliumService.class, url);
        HeliumService client = proxyFactory.getProxy(invoker);
        String result = client.sayHello("haha");
        Assertions.assertTrue(server.isCalled());
        Assertions.assertEquals("Hello, haha", result);
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testTimeOut() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0&timeout=10");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<HeliumService> invoker = protocol.refer(HeliumService.class, url);
        HeliumService client = proxyFactory.getProxy(invoker);
        try {
            client.timeOut(6000);
            fail();
        } catch (RpcException expected) {
            Assertions.assertEquals(true, expected.isTimeout());
        } finally {
            invoker.destroy();
            exporter.unexport();
        }

    }

    @Test
    public void testCustomException() {
        HeliumServiceImpl server = new HeliumServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf("http://127.0.0.1:5342/" + HeliumService.class.getName() + "?release=2.7.0");
        Exporter<HeliumService> exporter = protocol.export(proxyFactory.getInvoker(server, HeliumService.class, url));
        Invoker<HeliumService> invoker = protocol.refer(HeliumService.class, url);
        HeliumService client = proxyFactory.getProxy(invoker);
        try {
            client.customException();
            fail();
        } catch (HeliumServiceImpl.MyException expected) {
        }
        invoker.destroy();
        exporter.unexport();
    }

}
