package com.genshin.gm.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB配置类 - 允许应用在MongoDB不可用时也能启动
 */
@Configuration
public class MongoDBConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoDBConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public MongoClient mongoClient() {
        AppConfig config = ConfigLoader.getConfig();
        AppConfig.MongoDBConfig mongoConfig = config.getMongodb();

        try {
            logger.info("正在连接MongoDB: {}:{}", mongoConfig.getHost(), mongoConfig.getPort());

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder ->
                            builder.hosts(Collections.singletonList(
                                    new ServerAddress(mongoConfig.getHost(), mongoConfig.getPort())
                            ))
                                    .serverSelectionTimeout(5, TimeUnit.SECONDS))  // 5秒超时
                    .build();

            MongoClient client = MongoClients.create(settings);

            // 测试连接
            try {
                client.getDatabase(mongoConfig.getDatabase()).listCollectionNames().first();
                logger.info("✅ MongoDB连接成功: {}", mongoConfig.getDatabase());
            } catch (MongoException e) {
                logger.warn("⚠️ MongoDB连接失败，但应用将继续运行（用户认证功能将不可用）");
                logger.warn("MongoDB错误: {}", e.getMessage());
                logger.warn("请确保MongoDB已安装并运行在 {}:{}", mongoConfig.getHost(), mongoConfig.getPort());
            }

            return client;

        } catch (Exception e) {
            logger.error("❌ 创建MongoDB客户端失败", e);
            logger.error("应用将继续运行，但用户认证功能将不可用");
            logger.error("要启用用户认证，请安装并启动MongoDB");

            // 返回一个空的MongoDB客户端（允许应用启动）
            return MongoClients.create("mongodb://localhost:27017/?serverSelectionTimeoutMS=1000");
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        AppConfig config = ConfigLoader.getConfig();
        String database = config.getMongodb().getDatabase();
        return new MongoTemplate(mongoClient, database);
    }
}
