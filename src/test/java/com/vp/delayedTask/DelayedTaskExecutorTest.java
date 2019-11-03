package com.vp.delayedTask;


import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DelayedTaskExecutorTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static int MAX_THREADS = 50;

    public ProductDAO productDAO = ProductDAO.getInstance();

    @Test
    public void createAndLanchSampleTest(){
        List<Thread> threadList = createTread();
        threadList.forEach(Thread::start);
        threadList.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }



    public List<Thread> createTread() {
        List<Thread> threadList = new ArrayList<>(MAX_THREADS);
        for (int threadCounter = 0; threadCounter < MAX_THREADS; threadCounter++) {
            final Integer productId = threadCounter;
            Thread thread = new Thread(() -> {
                Product product = productDAO.getProductById(productId);
                Assert.assertEquals(product.getDescription(), ProductDAO.DESCRIPTION + productId);
                log.info("find " + product.getDescription());
            });
            threadList.add(thread);
        }
        return threadList;
    }


}
