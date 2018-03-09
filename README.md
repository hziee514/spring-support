# spring-support

为spring增加一些扩展功能

## feign-form-support

扩展feign, 支持上传文件

### 演示代码

#### 定义服务接口

```java
@FeignClient(name = "name-of-service",
        configuration = UploadService.MultipartSupportConfig.class)
public interface UploadService {
    @PostMapping(value = "/upload",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    boolean upload(@Param("form") Map<String, ?> file);
    
    class MultipartSupportConfig {
        @Bean
        public Encoder feignFormEncoder() {
            return new MultipartFormEncoder();
        }
        /**
         * 修改超时参数,不然会超时
         * @return
         */
        @Bean
        public Request.Options options() {
            return new Request.Options();
        }
    }
}
```

#### 调用服务接口

```java
@RestController
public class SampleController {
    @PostMapping("/upload")
    public bool upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> form = new HashMap<>();
        form.put("file", file);
        return uploadService.upload(form);
    }
}
```

#### 断路器超时设置

```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    'UploadService#upload(Map)':
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 60000
```

## swagger-spring-boot-starter

集成swagger2

### swagger配置

```yaml
swagger
  enabled: true
  title: 
  description: 
  version: 
  contactName: 
  contactUrl: 
  contactEmail: 
  termsOfServiceUrl: 
  license: 
  licenseUrl: 
  groupName: default
  basePackage: 
```

### security配置

```yaml
security:
  ignored: /swagger-resources/**
```

### 演示代码

```java
@SpringBootApplication
@EnableSwagger2Doc
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RcServiceApplication.class, args);
    }
}

@Api(description = "演示")
@RestController
public class SampleController {
    @ApiOperation("回显")
    @GetMapping("/echo")
    @ResponseBody
    public String echo(@ApiParam(value = "内容", required = true) @RequestParam String text) {
        return text;
    }
}
```

### 访问地址
http://<ip>:<port>/swagger-ui.html
