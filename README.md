## devtool
    本工程提供一个调试bean的方法的工具, 以节约本地调试的时间、提高开发效率

* 使用方法
  * 执行 mvn clean install -Dmaven.test.skip 打包
  * 在工程的pom中引入如下依赖  
    ```        
        <dependency>
            <groupId>com.fngry</groupId>
            <artifactId>devtool</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    ```
  * 启动本地工程
  * 浏览器进入http://localhost:port/context_path/debug_index.html
  
* FQA  
  1. bean的非public方法能不能调试？  
     可以。已完美支持。  
  2. 遇到父类中 public boolean saveOrUpdate(T entity); 这种T不定的方法如何调试？  
  以finance的FinanceAccountServiceImpl为例:  
     1.1 T的实际类型需要传com.eggmalltech.erp.finance.entity.FinanceAccount, 所以在启动参数增加-Dfastjson.parser.autoTypeAccept=com.eggmalltech.erp.finance.entity. 暂时打开fastjson的安全校验   
     1.2  在界面参数 entity输入框输入带@type类型信息的json参数即可:
     ```json
     {
        "@type": "com.fngry.entity.FinanceAccount",
        "id": null,
        "companyGroupId": null,
        "name": "",
        "code": "",
        "accountType": null,
        "direction": null,
        "accountLevel": null,
        "pid": null,
        "isLeaf": null,
        "groupId": null,
        "createUser": "",
        "status": null,
        "createBy": null,
        "createTime": "2021-09-02T08:18:53.865+00:00",
        "updateBy": null,
        "updateTime": "2021-09-02T08:18:53.865+00:00",
        "lastUpdate": "2021-09-02T08:18:53.865+00:00",
        "alterDate": "2021-09-02 09:18"
     }
     ```
  3. 为什么点方法Detail生成参数模版时，json参数里的Integer、Long字段默认为null，不是默认为0？  
    默认为0，调试的时候需要一个一个改掉值，比如一个分页查询接口字段默认0，执行时生成的条件都是xField=0 and yField=0 and ...；而默认为null，只需要改写关注的字段即可
      