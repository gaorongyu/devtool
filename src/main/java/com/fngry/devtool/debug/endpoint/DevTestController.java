package com.fngry.devtool.debug.endpoint;

import com.fngry.devtool.debug.metadata.MethodMetaData;
import com.fngry.devtool.debug.DebugException;
import com.fngry.devtool.debug.DevTestExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 测试工具入口
 * @author gaorongyu
 */
@RestController()
@RequestMapping("dev_test")
public class DevTestController {

    private static final String LOCAL = "local";

    @Autowired
    private DevTestExecutor devTestExecutor;

    @Autowired
    private Environment environment;

    @GetMapping("query_method_metadata_from_bean")
    public Result<List<MethodMetaData>> queryMethodMetaDataFromBean(@RequestParam String beanName) throws Exception {
        this.checkProfile();
        return Result.success(devTestExecutor.getMethodMetasForBean(beanName));
    }

    @GetMapping("method_parameter_template_for_bean")
    public Result<Map<String, Object>> methodParameterTemplateForBean(@RequestParam String beanName, @RequestParam String methodSignature) throws Exception {
        this.checkProfile();
        return Result.success(devTestExecutor.methodParameterTemplateForBean(beanName, methodSignature));
    }

    @PostMapping("execute_method_for_bean")
    public Object executeMethodForBean(@RequestBody ExecuteParameter param) throws Exception {
        this.checkProfile();
        return Result.success(devTestExecutor.executeMethodForBean(param.getObjectName(),
                param.getMethodSignature(), param.getParamMap()));
    }

    private void checkProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 1) {
            throw new DebugException("profiles size > 1 ");
        }
        if (!LOCAL.equals(profiles[0])) {
            throw new DebugException("只能在local环境使用哦");
        }
    }

}
