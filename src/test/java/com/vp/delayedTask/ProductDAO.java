package com.vp.delayedTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProductDAO {

    private static final ProductDAO instance = new ProductDAO();

    private final DelayedTask2Executor<Product, Integer> delayedTaskExecutorProductById =
            DelayedTasks.define(Duration.ofMillis(50), 10, this::retrieveProductsByIds);

    public static final String DESCRIPTION="description ";
    private final Logger log = LoggerFactory.getLogger(getClass());



    private ProductDAO() {
    }

    public static ProductDAO getInstance() {
        return instance;
    }

    public Product getProductById(Integer productId) {
        return  delayedTaskExecutorProductById.execute(productId);
    }


    private List<Product> retrieveProductsByIds(List<Integer> productIdsList) {
        log.info("executed for" + productIdsList.toString());
        List<Product> productList = simulateLaunchQuery(productIdsList);  // execute query:SELECT * FROM PRODUCT WHERE ID IN (idList.get(0), ..., idList.get(n));


        // The positions of the elements of the list to return must match the ones in the parameters list.
        // For instance, the first Product of the list to be returned must be the one with
        // the Id in the first position of productIdsList and so on...
        // NOTE: null could be used as value, meaning that no Product exist for the given productId


        List<Product> result = guaranteeMatching(productIdsList, productList);
        return result;
    }


    private List<Product> guaranteeMatching(List<Integer> productIdsList, List<Product> productListFromDatabase) {
        List<Product> result = new ArrayList<>();
        for (Integer productId : productIdsList) {
            Product product = findProductByIdOrNull(productListFromDatabase, productId);
            result.add(product);
        }
        return result;
    }


    private Product findProductByIdOrNull(List<Product> productListFromDatabase, Integer productId) {
        return productListFromDatabase.stream().filter(product -> Objects.equals(product.getId(), productId)).findFirst().orElse(null);
    }


    private List<Product> simulateLaunchQuery(List<Integer> productIdsList) {
        List<Product> productList = new ArrayList<>();
        for (Integer integer : productIdsList) {
            productList.add(new Product(integer, DESCRIPTION + integer));

        }
        Collections.shuffle(productList); // this is done on purpose to simulate that database don't guarantee order
        // simulate a random delay
        randomPause(0, 5);
        return productList;
    }


    private void randomPause(int millisecondsInit, int millisecondsEnd) {
        try {
            Thread.sleep(millisecondsInit + (int) (Math.random() * millisecondsEnd));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
