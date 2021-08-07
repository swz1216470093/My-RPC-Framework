# 自己实现RPC

## RPC简介

RPC（Remote Procedure Call）全称远程过程调用，简而言之就是像调用本地方法一样调用远程服务，整个过程用户是无感知的。

一款分布式RPC框架离不开三个基本要素：

* 服务提供方（Service Provider）
* 服务消费方（Service Consumer）
* 注册中心（Registery）

<img src="https://raw.githubusercontent.com/swz1216470093/blogComments/master/img/20210728163441.png" style="zoom:50%;" />

* 注册中心

  主要是用来完成服务注册和发现的工作。虽然服务调用是服务消费方直接发向服务提供方的，但是现在服务都是集群部署，服务的提供者数量也是动态变化的，所以服务的地址也就无法预先确定。因此如何发现这些服务就需要一个统一注册中心来承载。

* 服务提供方（RPC服务端）

  其需要对外提供服务接口，它需要在应用启动时连接注册中心，将服务名及其服务元数据发往注册中心。同时需要提供服务下线的机制。需要维护服务名和真正服务地址映射。服务端还需要启动Socket服务监听客户端请求。

* 服务消费方（RPC客户端）

  客户端需要有从注册中心获取服务的基本能力，它需要在应用启动时，扫描依赖的RPC服务，并为其生成代理调用对象，同时从注册中心拉取服务元数据存入本地缓存，然后发起监听各服务的变动做到及时更新缓存。在发起服务调用时，通过代理调用对象，从本地缓存中获取服务地址列表，然后选择一种负载均衡策略筛选出一个目标地址发起调用。调用时会对请求数据进行序列化，并采用一种约定的通信协议进行socket通信。

## 特性

1. IO通信框架 ：本实现支持Netty和Socket两种通信框架。

2. 消费端如采用 Netty 方式，会复用 Channel 避免多次连接

3. 如消费端和提供者都采用 Netty 方式，会采用 Netty 的心跳机制进行空闲检测，解决**连接假死**问题

   连接假死原因

   * 网络设备出现故障，例如网卡，机房等，底层的 TCP 连接已经断开了，但应用程序没有感知到，仍然占用着资源。
   * 公网网络不稳定，出现丢包。如果连续出现丢包，这时现象就是客户端数据发不出去，服务端也一直收不到数据，就这么一直耗着
   * 应用程序线程阻塞，无法进行数据读写

   问题

   * 假死的连接占用的资源不能自动释放
   * 向假死的连接发送数据，得到的反馈是发送超时

   服务器端解决：如果能收到客户端数据，说明没有假死。因此策略就可以定为，每隔一段时间就检查这段时间内是否接收到客户端数据，没有就可以判定为连接假死

   客户端解决：客户端可以定时向服务器端发送数据，只要这个时间间隔小于服务器定义的空闲检测的时间间隔，那么就能防止前面提到的误判

4. 通信协议

   因为TCP是基于流的传输协议，消息没有边界，如果不通过通信协议来传输，会产生**粘包**、**半包**问题

   主流的协议的解决方案可以归纳如下：

   * 定长消息，如每个消息固定长度为100字节，如果不过就用空格填充。这种方式的缺点是：长度定的太大，浪费；长度定的太小，对某些数据包又显得不够。
   * 分隔符，在消息结尾用特定分隔符分隔。这种方式的缺点是：处理字符数据比较合适，但如果内容本身包含了分隔符，那么就会解析错误
   * 将消息分为消息头和消息体，消息头中包含表示消息总长度（或者消息体长度）的字段。

   因为上边两种方案有明显缺陷，所以我选用第三种方案，自定义协议如下：

   ```java
   ---------------------------------------------------------
    magic  | version | codec | messageType |padding|length |
   (4byte) | (1byte) |(1byte)|   (1byte)   |(1byte)|(4byte)|
   ---------------------------------------------------------
                          content
                     ($(length) byte)
   ---------------------------------------------------------
   ```

   * 前4个字节是魔法数，我定义为｛(byte) 'm', (byte) 'r', (byte) 'p', (byte) 'c'｝
   * 第5个字节代表协议版本号，以便对协议进行扩展。
   * 第6个字节是序列化类型，方便对序列化方式进行扩展。
   * 第7个字节是消息类型，比如101是请求，102是响应，103和104分别是ping心跳包和pong心跳包
   * 第8个字节是填充 无意义
   * 紧接着4个字节是内容长度 即此四个字节后面此长度的内容是消息content。

5. 实现简单容器，在容器启动时扫描加了@RpcService和@RpcComponent注解的类注册为Bean，将加了@RpcService注解的bean注册到注册中心中，对加了@RpcAutowired注解的属性完成依赖注入

6. 序列化协议 本实现支持JDK和JSON序列化协议	

7. 负载均衡 本实现支持随机和轮询两种策略。

8. 注册中心 本实现选用Nacos作为注册中心。

## 实现

### 项目总体结构

<img src="https://raw.githubusercontent.com/swz1216470093/blogComments/master/img/image-20210807222359093.png" alt="image-20210807222359093" style="zoom:50%;" />

### netty传输模块

<img src="https://raw.githubusercontent.com/swz1216470093/blogComments/master/img/20210728180220.png" style="zoom:50%;" />

```java
@Slf4j
public class NettyServer implements RpcServer {
    public void start(){
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        DefaultEventExecutorGroup executors = new DefaultEventExecutorGroup(4);
        serverBootstrap.group(boss, worker)
                //启用nagle算法，尽可能的发送大数据包
                .childOption(ChannelOption.TCP_NODELAY, true)
                //全连接队列长度
                .option(ChannelOption.SO_BACKLOG,128)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
//                                空闲检测 解决连接假死问题
//                                当读空闲时即30秒内没有收到客户端请求就关闭连接
                                .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
//                               帧解码器 解决半包粘包问题
                                .addLast(new ProtocolDecoder())
//                                消息编解码器
                                .addLast(new MessageCodec())
                                .addLast(executors,new ServerHandler());
                    }
                });
        try {
            serverBootstrap.bind(RpcConfig.PORT)
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            log.debug("服务端启动失败");
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            executors.shutdownGracefully();
        }
    }

}
```

ServerHandler当接收到客户端请求，从容器中找到服务对象，反射调用请求的方法 用响应消息包装返回值

```java
@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private final BeanContainer container;

    public ServerHandler(){
        container = BeanContainer.getInstance();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg){
        if (msg.getMessageType() == Message.PING_MESSAGE) {
            //是ping心跳包 直接写回一个pong心跳包
            ctx.writeAndFlush(new PongMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }else if (msg.getMessageType() == Message.RPC_MESSAGE_TYPE_REQUEST){
//            是Rpc请求消息
            ResponseMessage responseMessage = new ResponseMessage();
            RequestMessage requestMessage = (RequestMessage) msg;
            responseMessage.setCodec(requestMessage.getCodec());
            responseMessage.setRequestId(requestMessage.getRequestId());
            if (ctx.channel().isActive() && ctx.channel().isWritable()){
                //从容器中找到服务对象 
                Class<?> interfaceClass = ClassUtils.loadClass(requestMessage.getInterfaceName());
                Set<Class<?>> classesBySuper = container.getClassesBySuper(interfaceClass);
                if (classesBySuper == null || classesBySuper.size() > 1 ) {
//                   未找到实现类或存在多个实现类
                    throw new RpcException("未找到实现类或存在多个实现类");
                }
                Object service = container.getBean(classesBySuper.iterator().next());
//                反射调用请求的方法 用响应消息包装返回值
                try {
                    Method method = service.getClass().getMethod(requestMessage.getMethodName(), requestMessage.getParameterTypes());
                    Object returnValue = method.invoke(service, requestMessage.getParameterValue());
                    responseMessage.setReturnValue(returnValue);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    log.error("远程调用出错",e);
                    responseMessage.setExceptionValue(new RpcException("远程调用出错"+e.getCause().getMessage()));
                }
            }else {
                log.error("当前Channel不可写，丢弃消息");
                responseMessage.setExceptionValue(new RpcException("远程调用失败"));
            }
            ctx.writeAndFlush(responseMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE){
//        客户端30秒内未发送请求  关闭连接
                log.debug("客户端30秒内未发送请求 关闭连接");
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
```

客户端复用Channel，发送请求时从注册中心找到服务地址，如果已经有与该地址连接的Channel则复用该Channel，如果没有则建立连接，

```java
public class NettyClient implements RpcTransport {

    private final Bootstrap bootstrap;
    private final Registry registry;
    private final ChannelProvider channelProvider;

    public NettyClient() {
        registry = NacosRegistry.getInstance();
        channelProvider = ChannelProvider.getInstance();
        bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
//                连接超时时间5毫秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
//                              // 用来判断是不是 读空闲时间过长，或 写空闲时间过长
//                              20秒内如果没有向服务器写数据，会触发一个 IdleState#WRITER_IDLE 事件
                                .addLast(new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS))
//                                帧解码器
                                .addLast(new ProtocolDecoder())
//                                消息编解码器
                                .addLast(new MessageCodec())
//                                客户端处理器
                                .addLast(new ClientHandler());
                    }
                });
    }

    private Channel doConnect(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("与服务端{}建立连接成功", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    public Channel getChannel(InetSocketAddress address) {
        Channel channel = channelProvider.get(address);
//        先去缓存中找
        if (channel != null) {
            return channel;
        } else {
//            缓存中找不到 建立连接 放入缓存
            try {
                channel = doConnect(address);
                channelProvider.put(address, channel);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                log.error("获取Channel时出错");
            }
        }
        return channel;
    }

    @Override
    public Object sendRpcRequest(RequestMessage requestMessage) {
//        从注册中心找到服务地址
        InetSocketAddress address = registry.lookupServiceAddress(requestMessage.getInterfaceName());
//        找到channel
        Channel channel = getChannel(address);
        log.debug("channel{}", channel.toString());
        if (channel.isActive()) {
            DefaultPromise<Object> promise = new DefaultPromise<>(channel.eventLoop());
            UnProcessRequest.getInstance().put(requestMessage.getRequestId(), promise);
            channel.writeAndFlush(requestMessage).addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.debug("client send message: [{}]", requestMessage);
                } else {
                    channelFuture.channel().close();
                    promise.setFailure(channelFuture.cause());
                    log.error("Send failed:", channelFuture.cause());
                }
            });
            try {
                promise.await();
            } catch (InterruptedException e) {
                throw new RpcException("等待远程调用结果时异常 " + e.getMessage());
            }
            if (promise.isSuccess()) {
//                调用正常
                return promise.getNow();
            } else {
//                调用异常
                throw new RpcException(promise.cause());
            }
        } else {
            throw new IllegalStateException();
        }
    }
}
```

```java
@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (msg.getMessageType() == Message.RPC_MESSAGE_TYPE_RESPONSE){
            ResponseMessage responseMessage = (ResponseMessage) msg;
            String requestId = responseMessage.getRequestId();
//            通过requestID得到promise 将结果放入promise容器中
            final Promise<Object> promise = UnProcessRequest.getInstance().remove(requestId);
            if (responseMessage.getExceptionValue() == null) {
//               成功 将结果放入promise中
                promise.setSuccess(responseMessage.getReturnValue());
            }else {
//                失败 将异常对象放入promise中
                promise.setFailure(responseMessage.getExceptionValue());
            }
        }else if (msg.getMessageType() == Message.PONG_MESSAGE) {
            log.debug("heart beat");
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (((IdleStateEvent) evt).state() == IdleState.WRITER_IDLE){
//                写空闲 发送一个ping 命令
                ctx.writeAndFlush(new PingMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                log.debug("write idle happened");
            }
        }else{
            super.userEventTriggered(ctx, evt);
        }
    }
}
```

自定义协议编解码器

```java
/**
 * 消息编解码器
 * -------------------------------------------------------------------
 * magic  | version | codec | messageType |padding|length |
 *(4byte) | (1byte) |(1byte)|   (1byte)   |(1byte)|(4byte)|
 * -------------------------------------------------------------------
 *                       content
 *                  ($(length) byte)
 *--------------------------------------------------------------------
 */
@ChannelHandler.Sharable
public class MessageCodec extends MessageToMessageCodec<ByteBuf, Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        final ByteBuf buffer = ctx.alloc().buffer();
//          4字节的魔数
        buffer.writeBytes(RpcConfig.MAGIC_NUMBER);
//        1个字节的版本号
        buffer.writeByte(RpcConfig.VERSION);
//        1个字节的序列化方法
        buffer.writeByte(msg.getCodec());
//        1个字节的消息类型
        buffer.writeByte(msg.getMessageType());
//        1个字节的对齐填充
        buffer.writeByte(0xff);
        Serializer serializer = SerializationTypeEnum.getSerializer(SerializationTypeEnum.getName(msg.getCodec()));
        byte[] content = serializer.serialize(msg);
        int length = content.length;
//        4个字节的内容长度
        buffer.writeInt(length);
//        length个字节的内容
        buffer.writeBytes(content);
        out.add(buffer);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        checkMagicNumber(byteBuf);
        checkVersion(byteBuf);
//      1个字节的序列化类型
        byte codecType = byteBuf.readByte();
        //        1个字节的消息类型
        byte messageType = byteBuf.readByte();
        Serializer serializer = SerializationTypeEnum.getSerializer(SerializationTypeEnum.getName(codecType));
        //        1个字节的对齐填充
        byteBuf.readByte();
        // 4个字节的长度
        int length = byteBuf.readInt();
        Message.getMessageClass(messageType);

        byte[] temp = new byte[length];
        byteBuf.readBytes(temp,0,length);
        Class<? extends Message> messageClass = Message.getMessageClass(messageType);
        Message message = serializer.deserialize(messageClass, temp);
        out.add(message);
    }

    public void checkMagicNumber(ByteBuf in){
        final byte[] bytes = new byte[RpcConfig.MAGIC_NUMBER.length];
        in.readBytes(bytes);
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != RpcConfig.MAGIC_NUMBER[i]){
                throw new RpcException("魔数不匹配，未知的魔数 " + Arrays.toString(bytes));
            }
        }
    }
    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (RpcConfig.VERSION != version){
            throw new RpcException("版本不匹配 期待版本为" + RpcConfig.VERSION );
        }
    }
}

```

### 注册中心

```java
@Slf4j
public class NacosRegistry implements Registry {
    private final Set<String> registeredService;
    private final Map<String ,List<Instance>> serviceInstanceCache;
    private static final String SERVER_ADDR = "127.0.0.1:8849";
    private static final NamingService namingService;
    private final LoadBalance loadBalance;
    static {
        namingService = getNamingService();
    }
    private static NamingService getNamingService(){
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr",SERVER_ADDR);
            return NamingFactory.createNamingService(properties);
        } catch (NacosException e) {
            log.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException("连接到nacos时错误");
        }
    }

    private NacosRegistry() {
        registeredService = ConcurrentHashMap.newKeySet();
        loadBalance = RandomLoadBalance.getInstance();
        serviceInstanceCache = new ConcurrentHashMap<>();
    }

    @Override
    public void registerService(String serviceName, InetSocketAddress address) {
        registeredService.add(serviceName);
        Instance instance = new Instance();
        instance.setPort(address.getPort());
        instance.setIp(address.getHostName());
        instance.setServiceName(serviceName);
        try {
            namingService.registerInstance(serviceName,instance);
            subscribe(serviceName);
        } catch (NacosException e) {
            log.debug("注册服务时出错" + e.getMessage());
        }
    }

    public void subscribe(String serviceName){
        try {
            namingService.subscribe(serviceName, event -> {
                if (event instanceof NamingEvent){
                    final List<Instance> instances = ((NamingEvent) event).getInstances();
                    if (instances == null || instances.isEmpty()){
                        serviceInstanceCache.remove(serviceName);
                    }else{
                        serviceInstanceCache.put(serviceName,instances);
                    }
                }
            });
        } catch (NacosException e) {
            log.debug("订阅服务时出错" + e.getMessage());
        }
    }
    @Override
    public InetSocketAddress lookupServiceAddress(String serviceName) {
        try {
            List<InetSocketAddress> addresses = getServiceAddress(serviceName);
            return loadBalance.selectServiceAddress(addresses);
        } catch (NacosException e) {
           throw new RpcException("服务发现时出错");
        }
    }

    private List<InetSocketAddress> getServiceAddress(String serviceName) throws NacosException {
        final List<Instance> instances;
        if (serviceInstanceCache.get(serviceName) != null){
            instances = serviceInstanceCache.get(serviceName);
        }else {
            instances = namingService.getAllInstances(serviceName);
        }
        List<InetSocketAddress> addresses =  new ArrayList<>();
        for (Instance instance : instances) {
            addresses.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
        }
        return addresses;
    }


    static class Holder{
        private static final NacosRegistry INSTANCE = new NacosRegistry();
    }

    public static NacosRegistry getInstance() {
        return Holder.INSTANCE;
    }
}
```

## 使用

```java
public interface HelloService {
    String say(String name);
}
```

```java
@RpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String say(String name) {
//        int i = 1/0;
        return "hello " + name;
    }
}
```

```java
@RpcComponent
public class HelloController {
    @RpcAutowired
    HelloService helloService;

    public void test(String name) {
        System.out.println(helloService.say(name));
    }
}
```

```java
public class ClientTest {
    public static void main(String[] args) {
        //指定扫描包
        new RpcClientStarter().start("com.swz.client");
        BeanContainer container = BeanContainer.getInstance();
        //从容器中获取bean
        HelloController helloController = (HelloController)container.getBean(HelloController.class);
        helloController.test("java");
        helloController.test("netty");
        helloController.test("rpc");
    }
}
```

```java
public class ServerTest {
    public static void main(String[] args) {
        //指定容器扫描的包
        new RpcServerStarter(new NettyServer()).start("com.swz.server");
    }
}
```

输出

```java
22:25:56.654 [nioEventLoopGroup-2-1] DEBUG com.swz.rpc.transport.netty.client.NettyClient - client send message: [RequestMessage(super=Message(messageType=101, codec=1), interfaceName=com.swz.api.HelloService, methodName=say, parameterTypes=[class java.lang.String], parameterValue=[java], requestId=b1f5832f-6991-4893-922d-d378fbffba44)]
hello java
22:25:56.665 [main] DEBUG com.swz.rpc.transport.netty.client.ChannelProvider - channel key  LAPTOP-75CDDR1U/172.18.144.1:9090
22:25:56.665 [main] DEBUG com.swz.rpc.transport.netty.client.NettyClient - channel[id: 0xad82a474, L:/172.18.144.1:11604 - R:LAPTOP-75CDDR1U/172.18.144.1:9090]
22:25:56.666 [nioEventLoopGroup-2-1] DEBUG com.swz.rpc.transport.netty.client.NettyClient - client send message: [RequestMessage(super=Message(messageType=101, codec=1), interfaceName=com.swz.api.HelloService, methodName=say, parameterTypes=[class java.lang.String], parameterValue=[netty], requestId=a5a8266c-5820-450e-8d76-931545e105d5)]
hello netty
22:25:56.667 [main] DEBUG com.swz.rpc.transport.netty.client.ChannelProvider - channel key  LAPTOP-75CDDR1U/172.18.144.1:9090
22:25:56.668 [main] DEBUG com.swz.rpc.transport.netty.client.NettyClient - channel[id: 0xad82a474, L:/172.18.144.1:11604 - R:LAPTOP-75CDDR1U/172.18.144.1:9090]
22:25:56.668 [nioEventLoopGroup-2-1] DEBUG com.swz.rpc.transport.netty.client.NettyClient - client send message: [RequestMessage(super=Message(messageType=101, codec=1), interfaceName=com.swz.api.HelloService, methodName=say, parameterTypes=[class java.lang.String], parameterValue=[rpc], requestId=01935eea-4b73-4d1f-a514-48651f69ea31)]
hello rpc
```

从输出可以看到三次请求都复用了0xad82a474同一个Channel

## 待完善

~~1.实现依赖注入 将helloService的代理对象注入到helloService成员变量中~~

```java
@RpcComponent
public class HelloController {
    @RpcResource
    HelloService helloService;

    public void test() {
        helloService.say("rpc");
    }
}
```

~~2.整合Spring，在spring启动时扫描加了@RpcService和@RpcComponent注解的类注册为Bean 通过spring的SpringBeanPostProcessor将加了@RpcService注解的bean注册到注册中心中，对加了@RpcResource注解的属性完成依赖注入~~

3.引入SPI机制解耦

