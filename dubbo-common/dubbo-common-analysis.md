### dubbo-common源码分析
    重点知识点：
    1.
    Dubbo扩展点加载机制 - ExtensionLoader
    https://wely.iteye.com/blog/2304718
    
    2.
    
#### 1.dubbo-common依赖分析
``` gradle
dependencies {
    //java字节码操作，动态编译
    compile 'org.javassist:javassist:3.20.0-GA'
    
    //hessian 是一种跨语言的高效二进制的序列化方式
    compile 'com.alibaba:hessian-lite:3.2.3'
    
    //json转换工具类
    compile 'com.alibaba:fastjson:1.2.46'
    
    //kryo是一个高性能的序列化/反序列化工具
    compile 'com.esotericsoftware:kryo:4.0.1'
    compile 'de.javakaffee:kryo-serializers:0.42'
    
    //fst是完全兼容JDK序列化协议的系列化框架，序列化速度大概是JDK的4-10倍，大小是JDK大小的1/3左右。
    compile 'de.ruedigermoeller:fst:2.48-jdk-6'
    
    //日志组件
    compile 'commons-logging:commons-logging:1.2'
    compile 'log4j:log4j:1.2.16'
    compileOnly 'org.slf4j:slf4j-api:1.7.25'
    compileOnly 'org.apache.logging.log4j:log4j-api:2.11.1'
    compileOnly 'org.apache.logging.log4j:log4j-core:2.11.1'
}
```
#### 2.包目录及资源文件
```      
beanutil: java-bean操作工具类:JavaBeanSerializeUtil
bytecode: ClassGenerator和Proxy byte操作
config: 配置模块,系统,环境变量等操作
compiler: support:JavassistCompiler,JdkCompiler等
extension
     factory: spi工厂，AdaptiveExtensionFactory，SpiExtensionFactory
     support: 
     详情请参照博客:https://wely.iteye.com/blog/2304718
io: io操作
json: 以基本不使用
logger: 日志操作模块
     jcl
     jdk
     log4j2
     log4j
     slf4j
     support
status: 状态监测模块
     support: LoadStatusChecker，MemoryStatusChecker
store
     support: ConcurrentMap存储
threadlocal: NamedInternalThreadFactory工厂
    
timer
threadpool: 线程池组件类
     manager: DefaultExecutorRepository
     support
          cached: CachedThreadPool
          eager: TaskQueue，EagerThreadPoolExecutor
          fixed: FixedThreadPool
          limited: LimitedThreadPool
utils:各种各样的工具类,StringUtils,JVMUtil


dubbo-common/src/main/resources/META-INF/dubbo/internal
    org.apache.dubbo.common.compiler.Compiler
    org.apache.dubbo.common.threadpool.ThreadPool
    org.apache.dubbo.common.status.StatusChecker
    org.apache.dubbo.common.store.DataStore
    org.apache.dubbo.common.extension.ExtensionFactory
    org.apache.dubbo.common.threadpool.manager.ExecutorRepository
    org.apache.dubbo.common.logger.LoggerAdapter
    
```
