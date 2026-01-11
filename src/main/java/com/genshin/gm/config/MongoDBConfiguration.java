package com.genshin.gm.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB配置类
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.genshin.gm.repository")
public class MongoDBConfiguration extends AbstractMongoClientConfiguration {

    private final AppConfig.MongoDBConfig mongoConfig;

    public MongoDBConfiguration() {
        AppConfig config = ConfigLoader.getConfig();
        if (config != null && config.getMongodb() != null) {
            this.mongoConfig = config.getMongodb();
        } else {
            this.mongoConfig = new AppConfig.MongoDBConfig();
        }
    }

    @Override
    protected String getDatabaseName() {
        return mongoConfig.getDatabase();
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoConfig.getConnectionString());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}
