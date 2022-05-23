package springframework.beans.factory.config;

public class BeanDefinition {
    private Class beanClass;
    public BeanDefinition(Class beanClass){
        this.beanClass = beanClass;
    }
    public Class getBean(){
        return this.beanClass;
    }
    public void setBeanClass(Class beanClass){
        this.beanClass = beanClass;
    }
}
