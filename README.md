
优化了一些功能，如jackson利用链挖掘等。并增加了web项目扫描策略，source点为路由入口，使用命令：
```
--config
webservice
--noTaintTrack
--maxChainLength
16
--similarLevel
4
--maxRepeatBranchesTimes
50
--skipSourcesFile
/myGadgetinspector/webservice-skip-sources.demo
/temp/halo.jar
```
参数介绍：

--similarLevel n:解决路径爆炸，即中间路径重复率过高的问题，对每条链，会取它的前n条链和最后1条链作为去重因子，如有重复，则取最短链

--maxRepeatBranchesTimes n:表示某个分支函数最多可以出现在所有利用链中几次，默认20

文件说明：
webservice-skip-sources.demo为需要忽略的路由类

如halo开源项目扫描结果如下：
```
Using classpath: [/temp/halo.jar]
run/halo/app/controller/content/ContentArchiveController.password(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String; (0)
  java/util/Formatter.format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/Formatter; (0)
  java/util/Formatter.format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/Formatter; (0)
  java/util/Formatter.parse(Ljava/lang/String;)[Ljava/util/Formatter$FormatString; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/content/ContentArchiveController.post(Ljava/lang/String;Ljava/lang/String;Lorg/springframework/ui/Model;)Ljava/lang/String; (0)
  run/halo/app/utils/MarkdownUtils.renderHtml(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.compile(Ljava/lang/String;)Ljava/util/regex/Pattern; (0)
  java/util/regex/Pattern.<init>(Ljava/lang/String;I)V (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/admin/api/PostController.createBy(Lrun/halo/app/model/params/PostParam;Ljava/lang/Boolean;)Lrun/halo/app/model/vo/PostDetailVO; (0)
  run/halo/app/model/params/PostParam.convertTo()Lrun/halo/app/model/entity/Post; (0)
  run/halo/app/utils/SlugUtils.slug(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forPOSIXName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/content/ContentSheetController.sheet(Ljava/lang/String;Ljava/lang/String;Lorg/springframework/ui/Model;)Ljava/lang/String; (0)
  run/halo/app/utils/MarkdownUtils.renderHtml(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.compile(Ljava/lang/String;)Ljava/util/regex/Pattern; (0)
  java/util/regex/Pattern.<init>(Ljava/lang/String;I)V (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/content/ContentArchiveController.post(Ljava/lang/String;Ljava/lang/String;Lorg/springframework/ui/Model;)Ljava/lang/String; (0)
  run/halo/app/utils/MarkdownUtils.renderHtml(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/admin/api/SheetController.createBy(Lrun/halo/app/model/params/SheetParam;Ljava/lang/Boolean;)Lrun/halo/app/model/vo/SheetDetailVO; (0)
  run/halo/app/model/params/SheetParam.convertTo()Lrun/halo/app/model/entity/Sheet; (0)
  run/halo/app/utils/SlugUtils.slug(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forPOSIXName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/admin/api/PostController.updateBy(Lrun/halo/app/model/params/PostParam;Ljava/lang/Integer;Ljava/lang/Boolean;)Lrun/halo/app/model/vo/PostDetailVO; (0)
  run/halo/app/model/params/PostParam.update(Lrun/halo/app/model/entity/Post;)V (0)
  run/halo/app/utils/SlugUtils.slug(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forPOSIXName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/content/ContentArchiveController.post(Ljava/lang/String;Ljava/lang/String;Lorg/springframework/ui/Model;)Ljava/lang/String; (0)
  java/lang/String.format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String; (0)
  java/util/Formatter.format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/Formatter; (0)
  java/util/Formatter.format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/Formatter; (0)
  java/util/Formatter.parse(Ljava/lang/String;)[Ljava/util/Formatter$FormatString; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/admin/api/SheetController.updateBy(Ljava/lang/Integer;Lrun/halo/app/model/params/SheetParam;Ljava/lang/Boolean;)Lrun/halo/app/model/vo/SheetDetailVO; (0)
  run/halo/app/model/params/SheetParam.update(Lrun/halo/app/model/entity/Sheet;)V (0)
  run/halo/app/utils/SlugUtils.slug(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

run/halo/app/controller/content/ContentSheetController.sheet(Ljava/lang/String;Ljava/lang/String;Lorg/springframework/ui/Model;)Ljava/lang/String; (0)
  run/halo/app/utils/MarkdownUtils.renderHtml(Ljava/lang/String;)Ljava/lang/String; (0)
  java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; (0)
  java/util/regex/Pattern.matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher; (0)
  java/util/regex/Pattern.compile()V (0)
  java/util/regex/Pattern.expr(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.sequence(Ljava/util/regex/Pattern$Node;)Ljava/util/regex/Pattern$Node; (0)
  java/util/regex/Pattern.family(ZZ)Ljava/util/regex/Pattern$CharProperty; (0)
  java/util/regex/UnicodeProp.forPOSIXName(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/util/regex/UnicodeProp.valueOf(Ljava/lang/String;)Ljava/util/regex/UnicodeProp; (0)
  java/lang/Enum.valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum; (0)
  java/lang/Class.enumConstantDirectory()Ljava/util/Map; (0)
  java/lang/Class.getEnumConstantsShared()[Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)

```

## threedr3am版本https://github.com/threedr3am/gadgetinspector
================
##### 一、加入了Fastjson的gadget chain挖掘

使用方式，main方法启动，启动参数：
```
--config fastjson /xxxxx/xxxxx/xxxxx/xxxxx.jar /xxxxx/xxxxx/xxxxx/xxxxx2.jar /xxxxx/xxxxx/xxxxx/xxxxx3.jar
```

##### 二、加入SQLInject的检测

slink暂时只加入了JdbcTemplate的检测，后续慢慢加入mybatis、原生jdbc、jpa、hibernate等。

使用方式，main方法启动，启动参数（新加入--boot参数，用于在spring-boot项目的jar包含有其它jar依赖的情况下）：
```
--config sqlinject --boot /xxxxx/xxxx/jdbc-1.0-SNAPSHOT.jar
```

#### 三、不使用污点分析（挖掘更全面的链）

最新加入--NoTaintTrack参数，指定不使用污点分析，将会把所有链都搜索出来，好处是不会遗漏，坏处是需要大量的人工审计

使用方式，main方法启动，启动参数：
```
--config fastjson --boot --NoTaintTrack /xxxxx/xxxxx/xxxxx/xxxxx.jar
```
建议使用：
```
--config fastjson
--noTaintTrack
--craw 0
--max 30
--history scan-history-fastjson-jndi.dat
--slink JNDI
--skipSourcesFile /Users/threedr3am/xxx/gadgetinspector/fastjson-skip-sources.demo
/Users/threedr3am/.m2/repository/
```
遍历/Users/threedr3am/.m2/repository/目录，把找到的jar包，30个一批的形式去不使用污点分析，挖掘JNDI slink的fastjson gadget

#### 参数描述
1. --config xxx：挖掘什么样的gadget chains（jackson、fastjson、sqlinject、jserial...）
2. --boot：指定该jar为SpringBoot项目jar包
3. --noTaintTrack：不使用污点分析，将会把所有链都搜索出来，好处是不会遗漏，坏处是需要大量的人工审计
4. --mybatis.xml xxx：当挖掘sqlinject时，若工程使用了Mybatis，则可通过指定mapper xml所在目录，进行挖掘Mybatis的sql注入
5. --resume：是否项目启动时不删除所有dat数据文件
6. --opLevel 1：链聚合优化等级，1表示一层优化，默认0不优化
7. --history recordFileName：启用历史扫描jar包记录，方便大规模扫描时不重复扫描旧jar包，好处时减少工作时间，坏处是遇到依赖组合的gadget可能扫不出来
8. --max 100：最多扫描100个jar包
10. --onlyJDK：仅扫描jdk依赖（rt.jar、jce.jar）
11. --maxChainLength 5：只输出chain长度小于等于5的chain
12. --crawMaven /Users/threedr3am/jar/：使用内置爬虫，自动爬取maven仓库，jar存储至/Users/threedr3am/jar/，maven仓库极慢，可以挂代理
13. --onlyCrawMaven：只启动maven爬虫
14. --onlyCrawMavenPopular：只启动maven-popular爬虫
15. --onlyCrawNexus：只启动nexus爬虫
16. --craw 10：启用爬虫功能，每10分钟分析一遍，出一次报告。若配置--crawMaven，则使用内置爬虫功能，爬取maven仓库
17. --slink JNDI：指定挖掘的slinks，可选JNDI、SSRFAndXXE、EXEC、FileIO、Reflect、BCEL（hessian专用），默认不填挖掘除专用外的所有slinks
18. --skipSourcesFile /xxx/xxxx/xxx.txt: 跳过哪些经常误报的class source，参考文件fastjson-skip-sources.demo
19. --slinksFile /xxx/xxxx/xxx.txt: 自定义挖掘的slinks，使用后--slink参数忽略，参考文件fastjson-slinks.demo

## JackOfMostTrades版本https://github.com/JackOfMostTrades/gadgetinspector
================

This project inspects Java libraries and classpaths for gadget chains. Gadgets chains are used to construct exploits for deserialization vulnerabilities. By automatically discovering possible gadgets chains in an application's classpath penetration testers can quickly construct exploits and application security engineers can assess the impact of a deserialization vulnerability and prioritize its remediation.

This project was presented at Black Hat USA 2018. Learn more about it there! (Links pending)

DISCLAIMER: This project is alpha at best. It needs tests and documentation added. Feel free to help by adding either!

Building
========

Assuming you have a JDK installed on your system, you should be able to just run `./gradlew shadowJar`. You can then run the application with `java -jar build/libs/gadget-inspector-all.jar <args>`.
 
How to Use
==========

This application expects as argument(s) either a path to a war file (in which case the war will be exploded and all of its classes and libraries used as a classpath) or else any number of jars.

Note that the analysis can be memory intensive (and so far gadget inspector has not been optimized at all to be less memory greedy). For small libraries you probably want to allocate at least 2GB of heap size (i.e. with the `-Xmx2G` flag). For larger applications you will want to use as much memory as you can spare.

The toolkit will go through several stages of classpath inspection to build up datasets for use in later stages. These datasets are written to files with a `.dat` extension and can be discarded after your run (they are written mostly so that earlier stages can be skipped during development).

After the analysis has run the file `gadget-chains.txt` will be written.


Example
=======

The following is an example from running against [`commons-collections-3.2.1.jar`](http://central.maven.org/maven2/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar), e.g. with

```
wget http://central.maven.org/maven2/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar
java -Xmx2G -jar build/libs/gadget-inspector-all.jar commons-collections-3.2.1.jar
```

In gadget-chains.txt there is the following chain:
```
com/sun/corba/se/spi/orbutil/proxy/CompositeInvocationHandlerImpl.invoke(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object; (-1)
  com/sun/corba/se/spi/orbutil/proxy/CompositeInvocationHandlerImpl.invoke(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object; (0)
  org/apache/commons/collections/map/DefaultedMap.get(Ljava/lang/Object;)Ljava/lang/Object; (0)
  org/apache/commons/collections/functors/InvokerTransformer.transform(Ljava/lang/Object;)Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)
```

The entry point of this chain is an implementation of the JDK `InvocationHandler` class. Using the same trick as in the original commons-collections gadget chain, any serializable implementation of this class is reachable in a gadget chain, so the discovered chain starts here. This method invokes `classToInvocationHandler.get()`. The discovered gadget chain indicates that the `classToInvocationHandler` can be serialized as a `DefaultedMap` so that the this invocation jumps to `DefaultedMap.get()`. The next step in the chain invokes `value.transform()` from this method. The parameter `value` in this class can be serialized as a `InvokerTransformer`. Inside this class's `transform` method we see that we call `cls.getMethodName(iMethodName, ...).invoke(...)`. Gadget inspector determined that `iMethodName` is attacker controllable as a serialized member, and thus an attacker can execute an arbitrary method on the class.
 
This gadget chain is the building block of the [full commons-collections gadget](https://github.com/frohoff/ysoserial/blob/master/src/main/java/ysoserial/payloads/CommonsCollections1.java) chain discovered by Frohoff. In the above case, the gadget inspector happened to discovery entry through `CompositeInvocationHandlerImpl` and `DefaultedMap` instead of `AnnotationInvocationHandler` and `LazyMap`, but is largely the same.


Other Examples
==============

If you're looking for more examples of what kind of chains this tool can find, the following libraries also have some interesting results:

* http://central.maven.org/maven2/org/clojure/clojure/1.8.0/clojure-1.8.0.jar
* https://mvnrepository.com/artifact/org.scala-lang/scala-library/2.12.5
* http://central.maven.org/maven2/org/python/jython-standalone/2.5.3/jython-standalone-2.5.3.jar

Don't forget that you can also point gadget inspector at a complete application (packaged as a JAR or WAR). For example, when analyzing the war for the [Zksample2](https://sourceforge.net/projects/zksample2/) application we get the following gadget chain:

```
net/sf/jasperreports/charts/design/JRDesignPieDataset.readObject(Ljava/io/ObjectInputStream;)V (1)
  org/apache/commons/collections/FastArrayList.add(Ljava/lang/Object;)Z (0)
  java/util/ArrayList.clone()Ljava/lang/Object; (0)
  org/jfree/data/KeyToGroupMap.clone()Ljava/lang/Object; (0)
  org/jfree/data/KeyToGroupMap.clone(Ljava/lang/Object;)Ljava/lang/Object; (0)
  java/lang/reflect/Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; (0)
```

As you can see, this utilizes several different libraries contained in the application in order to build up the chain.

FAQ
===

**Q:** If gadget inspector finds a gadget chain, can an exploit be built from it?

**A:** Not always. The analysis uses some simplifying assumptions and can report false positives (gadget chains that don't actually exist). As a simple example, it doesn't try to solve for the satisfiability of branch conditions. Thus it will report the following as a gadget chain:

```java
public class MySerializableClass implements Serializable {
    public void readObject(ObjectInputStream ois) {
        if (false) System.exit(0);
        ois.defaultReadObject();
    }
}
```

Furthermore, gadget inspector has pretty broad conditions on those functions it considers interesting. For example, it treats reflection as interesting (i.e. calls to `Method.invoke()` where an attacker can control the method), but often times overlooked assertions mean that an attacker can *influence* the method invoked but does not have complete control. For example, an attacker may be able to invoke the "getError()" method in any class, but not any other method name.


**Q:** If no gadget chains were found, does that mean my application is safe from exploitation?

**A:** No! For one, the gadget inspector has a very narrow set of "sink" functions which it considers to have "interesting" side effects. This certainly doesn't mean there aren't other interesting or dangerous behaviors not in the list.

Furthermore, there are a number of limitations to static analysis that mean the gadget inspector will always have blindspots. As an example, gadget inspector would presently miss this because it doesn't follow reflection calls.

```java
public class MySerializableClass implements Serializable {
    public void readObject(ObjectInputStream ois) {
        System.class.getMethod("exit", int.class).invoke(null, 0);
    }
}
```
