package cn.bugstack.springframework.context.support;

import cn.bugstack.springframework.beans.BeansException;
import cn.bugstack.springframework.beans.factory.BeanFactory;
import cn.bugstack.springframework.beans.factory.ConfigurableListableBeanFactory;
import cn.bugstack.springframework.beans.factory.config.BeanFactoryPostProcessor;
import cn.bugstack.springframework.beans.factory.config.BeanPostProcessor;
import cn.bugstack.springframework.beans.factory.config.ConfigurableBeanFactory;
import cn.bugstack.springframework.context.ConfigurableApplicationContext;
import cn.bugstack.springframework.cores.io.DefaultResourceLoader;
import cn.hutool.core.bean.BeanException;

import java.util.Map;

public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    @Override
    public void refresh() throws BeanException {
        // 1创建 BeanFactory，加载（register）BeanDefinition
        refreshBeanFactory();

        // 2获取BeanFactory
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        // 3 添加ApplicationContextAwareProcessor，手动注册BPP用于处理继承了ApplicationContextAware接口的Bean被处理，进行感知
        // 在这里添加BPP是因为无法在BeanFactory中感知ApplicationContext，所以在借助BPP传递Context，再在createBean统一处理
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

        // 4在bean实例化前，执行bfpp(BeanFactoryPostProcessor)
        invokeBeanFactoryPostProcessors(beanFactory);

        // 5BeanPostProcessor 需要提前在其他Bean对象实例化前执行注册操作
        registerBeanPostProcessors(beanFactory);

        // 6提前实例化单例Bean对象
        beanFactory.preInstantiateSingletons();
    }

    protected abstract void refreshBeanFactory() throws BeanException;

    protected abstract ConfigurableListableBeanFactory getBeanFactory();

    private void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
        for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorMap.values()) {
            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
        }
    }

    private void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanPostProcessor> beanPostProcessorMap = beanFactory.getBeansOfType(BeanPostProcessor.class);
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorMap.values()) {
            beanFactory.addBeanPostProcessor(beanPostProcessor);
        }
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeanException {
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }


    @Override
    public Object getBean(String name) throws BeansException {
        return getBeanFactory().getBean(name);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeanException {
        return getBeanFactory().getBean(name, requiredType);
    }

    /**
     * 注册虚拟机钩子的方法
     */
    @Override
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    /**
     * 手动执行关闭的方法
     */
    @Override
    public void close() {
        getBeanFactory().destroySingletons();
    }
}
