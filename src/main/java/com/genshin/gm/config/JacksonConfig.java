package com.genshin.gm.config;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson配置类 - 注册Hibernate5模块，正确序列化Hibernate代理对象和懒加载集合
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate5Module hibernate5Module() {
        Hibernate5Module module = new Hibernate5Module();
        // 强制序列化懒加载对象（如果已初始化）
        module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, false);
        // 序列化实体标识符
        module.configure(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        return module;
    }
}
