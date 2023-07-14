package com.jawnz.gate.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.KafkaContainer;

public class TestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

    private Logger log = LoggerFactory.getLogger(TestContainersSpringContextCustomizerFactory.class);

    private static KafkaTestContainer kafkaBean;
    private static MongoDbTestContainer mongoDbBean;
    private static ElasticsearchTestContainer elasticsearchBean;

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            TestPropertyValues testValues = TestPropertyValues.empty();
            EmbeddedMongo mongoAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedMongo.class);
            if (null != mongoAnnotation) {
                log.debug("detected the EmbeddedMongo annotation on class {}", testClass.getName());
                log.info("Warming up the mongo database");
                if (null == mongoDbBean) {
                    mongoDbBean = beanFactory.createBean(MongoDbTestContainer.class);
                    beanFactory.registerSingleton(MongoDbTestContainer.class.getName(), mongoDbBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(MongoDbTestContainer.class.getName(), mongoDbBean);
                }
                testValues = testValues.and("spring.data.mongodb.uri=" + mongoDbBean.getMongoDBContainer().getReplicaSetUrl());
            }
            EmbeddedKafka kafkaAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedKafka.class);
            if (null != kafkaAnnotation) {
                log.debug("detected the EmbeddedKafka annotation on class {}", testClass.getName());
                log.info("Warming up the kafka broker");
                if (null == kafkaBean) {
                    kafkaBean = beanFactory.createBean(KafkaTestContainer.class);
                    beanFactory.registerSingleton(KafkaTestContainer.class.getName(), kafkaBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(KafkaTestContainer.class.getName(), kafkaBean);
                }
                testValues =
                    testValues.and(
                        "spring.cloud.stream.kafka.binder.brokers=" +
                        kafkaBean.getKafkaContainer().getHost() +
                        ':' +
                        kafkaBean.getKafkaContainer().getMappedPort(KafkaContainer.KAFKA_PORT)
                    );
            }
            EmbeddedElasticsearch elasticsearchAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                testClass,
                EmbeddedElasticsearch.class
            );
            if (null != elasticsearchAnnotation) {
                log.debug("detected the EmbeddedElasticsearch annotation on class {}", testClass.getName());
                log.info("Warming up the elastic database");
                if (null == elasticsearchBean) {
                    elasticsearchBean = beanFactory.createBean(ElasticsearchTestContainer.class);
                    beanFactory.registerSingleton(ElasticsearchTestContainer.class.getName(), elasticsearchBean);
                    // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(ElasticsearchTestContainer.class.getName(), elasticsearchBean);
                }
                testValues =
                    testValues.and(
                        "spring.elasticsearch.uris=http://" + elasticsearchBean.getElasticsearchContainer().getHttpHostAddress()
                    );
            }
            testValues.applyTo(context);
        };
    }
}
