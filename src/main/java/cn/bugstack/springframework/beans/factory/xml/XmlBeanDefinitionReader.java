package cn.bugstack.springframework.beans.factory.xml;

import cn.bugstack.springframework.beans.BeansException;
import cn.bugstack.springframework.beans.PropertyValue;
import cn.bugstack.springframework.beans.factory.config.BeanDefinition;
import cn.bugstack.springframework.beans.factory.config.BeanReference;
import cn.bugstack.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import cn.bugstack.springframework.beans.factory.support.BeanDefinitionReader;
import cn.bugstack.springframework.beans.factory.support.BeanDefinitionRegistry;
import cn.bugstack.springframework.cores.io.Resource;
import cn.bugstack.springframework.cores.io.ResourceLoader;
import cn.hutool.core.bean.BeanException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Documented;

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry){
        super(registry);
    }
    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader){
        super(registry,resourceLoader);
    }
    @Override
    public void loadBeanDefinitions(Resource resource) throws BeanException {
        try {
            try(InputStream inputStream = resource.getInputStream()) {
                doLoadBeanDefinitions(inputStream);
            }
        }catch (IOException | ClassNotFoundException e){
            throw new BeanException("IOException parsing XML document from " + resource,e);
        }
    }

    private void doLoadBeanDefinitions(InputStream inputStream) throws ClassNotFoundException{
        Document doc = XmlUtil.readXML(inputStream);
        Element root = doc.getDocumentElement();
        NodeList childNodes = root.getChildNodes();
        for (int i = 0;i< childNodes.getLength();i++){
            if(!(childNodes.item(i) instanceof Element)) continue;
            if (!"bean".equals(childNodes.item(i).getNodeName())) continue;
            // 解析标签
            Element bean = (Element) childNodes.item(i);
            String id = bean.getAttribute("id");
            String name = bean.getAttribute("name");
            String className = bean.getAttribute("class");
            // 获取 Class，方便获取类中的名称
            Class<?> clazz = Class.forName(className);
            // 优先级 id > name
            String beanName = StrUtil.isNotEmpty(id) ? id : name;
            if (StrUtil.isEmpty(beanName)) {
                beanName = StrUtil.lowerFirst(clazz.getSimpleName());
            }

            //定义bean
            BeanDefinition beanDefinition = new BeanDefinition(clazz);
            for (int j = 0;j<bean.getChildNodes().getLength();j++){
                if (!(bean.getChildNodes().item(j) instanceof Element)) continue;
                if (!"property".equals(bean.getChildNodes().item(j).getNodeName())) continue;
                // 解析标签 ： property
                Element property = (Element) bean.getChildNodes().item(j);
                String attrName = property.getAttribute("name");
                String attrValue = property.getAttribute("value");
                String attrRef = property.getAttribute("ref");
                // 获取属性值：引入对象、值对象
                Object value = StrUtil.isNotEmpty(attrRef) ? new BeanReference(attrRef) : attrValue;
                // 创建属性信息
                PropertyValue propertyValue = new PropertyValue(attrName,value);
                beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
            }
            if (getRegistry().containsBeanDefinition(beanName)){
                throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
            }
            // 注册BeanDefinition
            getRegistry().registerBeanDefinition(beanName,beanDefinition);
        }
    }

    @Override
    public void loadBeanDefinitions(Resource... resources) throws BeanException {
        for (Resource resource: resources){
            loadBeanDefinitions(resource);
        }
    }

    @Override
    public void loadBeanDefinitions(String location) throws BeanException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        loadBeanDefinitions(resource);
    }
}
