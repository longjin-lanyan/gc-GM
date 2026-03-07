package com.genshin.gm.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MySQL数据源配置类 - 从config.json读取MySQL配置
 */
@Configuration
public class DataSourceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfiguration.class);

    @Bean
    public DataSource dataSource() {
        AppConfig config = ConfigLoader.getConfig();
        AppConfig.MySQLConfig mysqlConfig = config.getMysql();

        logger.info("正在配置MySQL数据源: {}:{}/{}", mysqlConfig.getHost(), mysqlConfig.getPort(), mysqlConfig.getDatabase());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(mysqlConfig.getJdbcUrl());
        dataSource.setUsername(mysqlConfig.getUsername());
        dataSource.setPassword(mysqlConfig.getPassword());
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池配置
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(5000);

        logger.info("MySQL数据源配置完成: {}", mysqlConfig.getJdbcUrl());
        return dataSource;
    }
}
