package org.novapeng.jpax.spring;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * execute jpa enhancer before spring init
 *
 * Created by pengchangguo on 15/10/28.
 */
@Component
public class JPAXEnhancer implements ApplicationContextInitializer {


    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        SpringJPAX.go();
    }
}
