# eds-tracer
概要介绍：
       基于Zipkin2、Sleuth2、brave4、kafka、elastic search5、eds-tracer（javaagent，bytebuddy）构建，致力于微服务架构下高效的性能分析和故障诊断。

       同时参考：pinpoint，skywalking，cat等其他开源分布式追踪或APM系统。

架构简介：
      

追踪平台使用：
 

平台地址：
 

http://trace.eduonline.hz.netease.com/zipkin/  （线上环境）

http://trace.edutest.hz.netease.com/zipkin/   （测试环境）

 

追踪链搜索：


强大的Tag过滤：
语法：
      tagKey=tagValue，并且支持and，or等逻辑语法。

      注意：原生的zipkin查询细节比较差。要注意语句的前后都不能有额外的空格。中间如果有and，or等逻辑语法时，也必须严格用一个空格隔开。

 

示例：
       http.method=GET

       env=online and http.method=GET

 

哪些标签可以查询？
       每种服务都有一些各自的标签，因为内容比较多，这里先只告诉大家怎么去找，后面会整理一个表格出来。

 

       case1，点开一个web请求发现支持如下tags。那么我们可以通过指定http.path=/cp/application实现过滤追踪请求。



 

         case2，点开一个数据库请求发现支持如下tags。那么我们可以通过限定longSql=true,来筛选存在访问的sql语句过长的请求。



 

        caseN...其他例如dubbo，redis，memcache，ndir等其他类型请求也类似，不服赘述。

追踪链详情：


 

单请求详情：
 

例1. 对于数据库请求，你可以查看到具体的sql:



 

 

例2. 对于Ndir请求，你可以查看到你的查询语法：



 

例3. 对于dubbo请求，你可以看到对应接口的输入输出：



 

其他memcache、redis、rabbit等的内容详情就不再一一赘述。请大家后续自行查看。

 

后面这些展示内容也将不断丰富。

同时，大家在使用时如果有发现需要增加什么辅助信息，欢迎联系我hzfjd@corp.netease.com。我会根据情况及时补充上。

 

线上分布式调试：
         大家可能会有如下疑问：

既然是按照概率采样，那要是我感兴趣的请求没有被采样怎么办？
能否像eclipse的条件断点一样，对特定的请求进行拦截呢？

答案是：能！

现场举例:线上某个老师说他发布课程慢或出错了。怎么办？

首先, “打断点”，即在对应的study-teach-web应用增加调试配置：
    edu.trace.debug.httpUri=/j/course/publish.json
    edu.trace.debug.httpParams.courseId=123
    edu.trace.debug.loginType=-1
    edu.trace.debug.loginId=teacherLaoWang
         edu.trace.debug.debugMark=teach-cant-publish-course-20180601
 
然后，到追踪平台，在Annotations Query栏中，添加“DebugMark=teach-cant-publish-course-20180601”, 找到相关的请求追踪链。

最后，通过追踪链上丰富的时延、输入输出、异常、相关资源的负载情况分析问题。
       

接入指南
      （只需三步）：

       1. 工程依赖添加。修改pom.xml （单、多模块工程根据各自pom文件分工来指定）。

版本限定方面: (注意dependencyManagement的配置顺序)

<dependencyManagement>
   <dependencies>
       <!-- 对rabbitClient的版本有要求，确保是这个版本或以上。edu-third-party-base中已经指定版本号已经大于该版本了。 -->
       <dependency>
         <groupId>com.rabbitmq</groupId>
         <artifactId>amqp-client</artifactId>
         <version>4.0.0</version>
      </dependency>
      <!-- 因为很多自定义版本控制纳入二方库统一管理，所以需要确保edu-second-party-base在最前面。版本要求1.0.361及以上。 -->
      <dependency>
         <groupId>com.netease.edu</groupId>
         <artifactId>edu-second-party-base</artifactId>
         <version>${edu-second-party-base.version}</version>
         <type>pom</type>
         <scope>import</scope>
      </dependency>
 
      <!-- 确保springcloud版本是在该版本或以上。 -->
      <dependency>
         <groupId>org.springframework.cloud</groupId>
         <artifactId>spring-cloud-dependencies</artifactId>
         <version>Camden.SR7</version>
         <type>pom</type>
         <scope>import</scope>
      </dependency>
      <!-- 确保springboot版本是在该版本或以上。 -->
      <dependency>
         <!-- Import dependency management from Spring Boot -->
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-dependencies</artifactId>
         <version>1.5.10.RELEASE</version>
         <type>pom</type>
         <scope>import</scope>
      </dependency>
</dependencyManagement>
依赖限定方面:
<!--增加追踪客户端的依赖-->
<dependency>
   <groupId>com.netease.edu.eds</groupId>
   <artifactId>eds-tracer</artifactId>
</dependency>
 

2. 修改工程内部的build文件。

    修改工程根目录下的deploy/ndp/backend/build.xml文件。

<target name="cp-sentry-collector">
   <mkdir dir="${baseline.dir}/unzip"/>
   <exec dir="." executable="unzip" failonerror="true">
      <arg line="-o ${compress.dir}/app.jar -d ${baseline.dir}/unzip"/>
   </exec>
   <copy todir="${compress.dir}/lib" overwrite="true">
       <fileset dir="${baseline.dir}/unzip/BOOT-INF/lib" includes="sentry-javaagent-collector-*.jar" />
   </copy>
   <!-- 添加如下文件复制脚本。目的是copy trace-agent.jar到指定的部署目录，以保证jvm启动时能正确加载到该jar包。 -->
   <copy tofile="${compress.dir}/lib/eds-tracer-agent.jar" overwrite="true">
       <fileset dir="${baseline.dir}/unzip/BOOT-INF/lib"  includes="eds-tracer-agent-*.jar" excludes="*-sources.jar" />
   </copy>
</target>
 

3. ndp启动参数修改。

    在ndp对应应用的发布配置面板里，修改jvm_extra项内容，追加如下内容：-javaagent:$APP_HOME/lib/eds-tracer-agent.jar 

    

 

配置优化及监控：
应用通用配置application.properties (已经配置好。新接入应用无需修改)

#trace
spring.zipkin.service.name=${spring.application.name}
#采样率，目前是0.01，即1%。
spring.sleuth.sampler.probability=0.01f
#禁止hystrix追踪。等其超时机制导致过多空span的bug修复后再打开。目前都是SEMAPHORE+InheritableThreadLocal模式，所以也不需要修改并发策略。
spring.sleuth.hystrix.strategy.enabled=false
#异步与定时任务配置。与默认配置相同，可以去除。
spring.sleuth.async.enabled=true
spring.sleuth.scheduled.enabled=true
#禁止。因为spring integration的默认追踪支持无法正确记录相关span的时长。
spring.sleuth.integration.enabled=false
 
#框架通用的web请求uri过滤
spring.sleuth.web.skipPattern=/api-docs.*|/autoconfig|/configprops|/dump|/health|/info|/metrics.*|/mappings|/trace|/swagger.*|.*\.png$|.*\.css$|.*\.js$|.*\.html$|/favicon.ico$|/hystrix.stream|/application/.*
#额外web请求uri过滤。后续health相关请求成为标准后，也可以放到标准pattern中去。
spring.sleuth.web.additionalSkipPattern=/health/status
 
#采样数据通过kafka发送
spring.zipkin.sender.type=kafka
#采样数据的kafka topic名称
spring.zipkin.kafka.topic=zipkin
#kafka集群地址
spring.kafka.bootstrapServers=study-kafka1.dg.163.org:9092,study-kafka2.dg.163.org:9092,study-kafka3.dg.163.org:9092
#如果trace的kafka和业务kafka代理地址不同，通过"trace.kafka"这个prefix单独定义相关配置。目前配置=${spring.kafka.bootstrapServers}是为了兼容新老版本的tracer。后面统一更新到新版本
#的tracer后，统一使用"trace.kafka"这个prefix进行定义kafka配置。
trace.kafka.bootstrapServers=${spring.kafka.bootstrapServers}
 
#springcloud老版本自动配置排除。后面等springboot进一步升级后，通过增加自动配置过滤器来代替该配置项。消除该配置，并无差别对待应用和单元测试。
spring.autoconfigure.exclude[0]=org.springframework.cloud.sleuth.zipkin2.ZipkinAutoConfiguration
spring.autoconfigure.exclude[1]=org.springframework.cloud.sleuth.instrument.web.client.feign.TraceFeignClientAutoConfiguration
spring.autoconfigure.exclude[2]=org.springframework.cloud.sleuth.instrument.web.TraceWebServletAutoConfiguration
spring.autoconfigure.exclude[3]=org.springframework.cloud.sleuth.instrument.web.TraceWebFluxAutoConfiguration
spring.autoconfigure.exclude[4]=org.springframework.cloud.sleuth.instrument.web.TraceWebAutoConfiguration
 

应用独立配置，例如study-web.properties配置部分

#采样率调节。根据各自的丢包率请情况进行调节，动态生效。
spring.sleuth.sampler.probability=0.01f
#调试配置（有调试需求时才配置。只支持在web应用上配置）
#调试条件过滤，目前只支持精确匹配，所有条件如果存在的话就是逻辑与AND的效果，后面会支持正则表达式，SPEL表达式。
#限定请求uri
edu.trace.debug.httpUri=/web/setValue
#限定特定的请求头，本例为Host头
edu.trace.debug.httpHeaders.Host=127.0.0.1:20022
#限定特定的请求参数，本例为orderId参数
edu.trace.debug.httpParams.orderId=123
#限定请求方式
edu.trace.debug.httpMethod=POST
#限定登录用户，并且账号类型为指定类型
edu.trace.debug.loginType=-1
#限定登录用户，并且账号ID为指定值
edu.trace.debug.loginId=123456
#调试代号。因为请求会非常多，后面可以通过该标记进行过滤符合条件的追踪信息。例如本例可以通过标签搜索DebugMark=hzfjd-20180601进行过滤。
edu.trace.debug.debugMark=hzfjd-20180601
 

在哨兵监控平台，为对应的应用集群添加监控项。

菜单栏目：应用-》集群-》配置-》添加监控项-》

       其中采集器搜索“TraceReporterMetricsCollector”，其他选项均默认即可。

然后切换到：应用-》集群-》视图-》多实例

      其中找到并打开TraceReporterMetricsCollector视图。

过2分钟后，对应的监控视图就会类似有如下数据。



 

项目RoadMap及进展：
环境搭建	完成	
ES搭建，集群地址见平台索引。kafka是现有集群。zipkin部署，地址见平台索引。

 	 
原生集成	完成	zipkin1+sleuth1	 	 
版本升级	完成	zipkin2客户端和服务器端（修复spanName小写问题）+sleuth2（内部替换为brave实现）+brave4（修改内部暂未提供扩展的部分追踪增强）	
 

 
dubbo集成	完成	参考brave中实现方案。	 	 
增强调整为java agent方案	完成	Agent项目构建。bytebuddy调研，集成。ServiceLoader插件方案测试。Agent因classloader可见性无法加载springboot fat jar中的插件问题修复。	 	 
DDB集成	完成	事务检测支持。长Sql语句检测支持。Sql详情展示支持。现有数据库连接负载情况展示支持。	 	 
NDir集成	完成	Ndir查询输入展示支持。节点路由检测支持。	 	 
rabbit集成	完成	exchange，topic，queue的展示。后续增加payload的展示。	 	 
Memcache集成	完成	输入输出显示支持。节点路由检测支持。支持命名空间检测。	 	 
Redis集成	完成	输入输出显示支持。节点路由检测支持。同时支持RedisTemplate和JedisClient。支持命名空间检测。	 	 
调试器支持	完成	支持请求路径、请求头、请求参数、用户账号等过滤。目前暂只支持精确匹配，后期会增加正则表达式、SPEL表达式。	 	 
丢包率哨兵监控	完成	支持发送消息数、字节数、丢包率等监控。动态生效。便于线上运行监控，并给采样率调整提供数据支撑。	 	 
异步追踪	完成	对方目前支持new Thread()，spring托管的异步、定时、延迟调度。对直接调用jdk的线程池进行处理的，还需要后面追加追踪实现。	 	 
部分tag增强。	完成	增加环境信息。	 	 
单元测试框架适配	完成	修复新模块引入导致的自动mock异常问题。	 	 
已知bug修复	完成	dubbo span的输入、输出打印两次。事务提交bug。个别监控指标聚合错误。	 	 
服务穿梭支持dubbo	完成	根据请求来源，对基准环境和测试环境的下游服务进行选择。	 	 
服务穿梭支持

rabbit

完成	双队列发送，消费端根据trace信息选择对应环境的消费者。	 	 
采样数据内容分级	未开始	
1.为支持业务级分布式追踪需求，例如统计追踪信息、企业id传递等（信息追踪固定100%采样）

2.采样率仅影响调试数据信息的输出。

 	 
服务穿梭支持

事务消息

未开始	 	 	 
服务穿梭支持

kafka

未开始	 	 	 
服务穿梭支持

http client

未开始	 	 	 
补全现有追踪中的输入输出。	完成。	增加dwr，controller的请求输入输出。	 	 
追踪聚合计算一期	未开始	根据接口名称分析依赖的所有顶层web接口。健全服务类别信息。TAG名称规范化。集成flink，sloth做实时流分析。聚合展示工程独立于厚重的zipkin。	 	 
ElasticSearch追踪	未开始	支持rest和tcp两种连接模式。	 	 
现有异常展示缺陷改进	未开始	由于现有web层框架的异常处理比较混乱，导致异常捕获和展示有很多缺陷。需要结合框架做优化。	 	 
Kafka追踪	未开始	需要排除自身追踪数据发送部分。	 	 
http client追踪	未开始	支持部门现场常用的集中http client。	 	 
Hystrix追踪bug修复	未开始	hystrix超时线程处理追踪的bug修复，避免无意义的span的创建干扰整个链路信息。	 	 
追踪插件初始化时机前提。	未开始	从现有Spring boot Listener前提到Context Initializer的构造器，进一步降低类已经被加载的风险。	 	 
原生线程池支持	完成	需解决Bootstrap Class loader加载类的类增强问题。	 	 
追踪聚合计算二期	未开始	服务依赖分析。	 	 
追踪聚合计算二期	未开始	服务接口依赖分析。	 	 
调试器增强	未开始	支持正则匹配，支持SPEL语法。	 	 
基于调试内容的统计分析	未开始	j基于被调试接口的准实时统计分析。类似于分布式版本的greys TT。	 	 
UI增强	未开始	提供华丽丽的UI。前端志愿者招募中。	 	 
调试内容动态分级	未开始	后期随着产品流量上来，需要对追踪做性能优化。会考虑对调试内容对分级，支持在性能和内容详尽度之间做动态的权衡。	 	 
 

二次开发：
 

追踪项目源码：ssh://git@g.hz.netease.com:22222/eduos/eds-tracer.git

追踪Java Agent源码 ：ssh://git@g.hz.netease.com:22222/eduos/eds-trace-agent.git

Zipkin2 Fork：https://github.com/fangjinde/zipkin.git

Brave Fork：https://github.com/fangjinde/brave.git

 

FAQ：
由于历史遗留，部分工程ndp上可能配置的构建文件是deploy/ndp/backend/build-new.xml。则需修改build-new.xml，而不是build.xml文件。需要修改的内容一样。建议都统一为build.xml这个文件名。
 

中间踩过的一些坑：
      1.  Java agent无法通过ServiceLoader加载打包在spring boot fat jar中的插件。

      2.  打包出来的java agent的premain方法没有被虚拟机调用到。

      3.  bytebuddy做字节码增强时，没有拦截到并没有任何出错信息。
