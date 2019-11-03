package com.vp.delayedTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class DelayedTasks {


    public static final int MAX_SIZE = 1024;
    public static final Duration MAX_TIME = Duration.ofSeconds(10);
    private static final Duration MIN_TIME=Duration.ofMillis(1);

    private static final int QUEUE_SIZE = 4096; // max elements queued

    private final int size;
    private final Duration windowTime;

    private final UnicastProcessor<Tuple> source;

    private final Logger logger = LoggerFactory.getLogger(DelayedTasks.class);

    //DEFINIZIONE OGGETTI TASKS CON 2 O PIU PARAMETRI
    public static <R,P1> DelayedTask2Executor<R,P1> define(Duration windowTime, int size, DelayedParameterFun2<R,P1> delayedParameterFun2){
        return new DelayedTask2Executor<>(windowTime,size,delayedParameterFun2);
    }

    // 3 PARAM
// define a new functional interface DelayedParameterFun3 with 3 generic params and uncomment this method:
//    public static <Z,A,B> DelayedTask3Executor<Z,A,B> define(Duration windowTime, int size, DelayedParameterFun3<Z,A,B> delayedParameterFunction) {
//        return new DelayedTask3Executor<>(windowTime, size, delayedParameterFunction);
//    }

    //4 PARAM ECC..


    public DelayedTasks(Duration windowTime, int bufferSize) {
        super();
        this.windowTime  = windowTime;
        this.size = bufferSize;
        validateBoundaries(size, windowTime);
        this.source = UnicastProcessor.create(new ArrayBlockingQueue<>(QUEUE_SIZE));
        Flux<Tuple> tupleFlux = this.source.publish().autoConnect();
        tupleFlux.bufferTimeout(size, windowTime).log()
                .subscribeOn(Schedulers.newParallel("DelayedTaskSubscriber"))
                .subscribe(this::executedList, this::errorAccept);

    }

    private void errorAccept(Throwable throwable) {
        logger.error(" error : " + throwable.getMessage());
    }

    abstract <R,P1>  R execute (P1 param);

    protected  void validateBoundaries(int size, Duration time){
        if (size < 1 || size > MAX_SIZE)  {
            throw new IllegalArgumentException("max elements parameter must be in range ["+ 1 + ","+ MAX_SIZE + "]");
        }


        if (MAX_TIME.compareTo(time) < 0 || time.compareTo(MIN_TIME) < 0) {
            throw new IllegalArgumentException("time window parameter must be in range ["+ 1 + ","+ MAX_TIME.toMillis() + "] ms");
        }

    }

    protected abstract List<Object> getResultFromTupleList(TupleListArgs tupleListArgs);

    public void executedList(List<Tuple> paramList){
        //lettura dei risultati
        List<Object> resultFromTupleList = getResultFromTupleList(new TupleListArgs(paramList));
        List resizedList = ensureSizeFillingWithNullsIfNecessary(resultFromTupleList, paramList.size());

        for (int index=0; index<paramList.size(); index++) {
            Tuple tuple = paramList.get(index);
            tuple.setResult(resizedList.get(index));
            tuple.commitResult();
            tuple.continueIfIsWaiting();
        }
    }



    protected <P1> Tuple executeWithArgs(P1 param) {
        Tuple tuple = new Tuple(param);
        source.onNext(tuple);
        tuple.waitIfResultHasNotCommitted();
        return tuple;
    }

    private <T> List<T> ensureSizeFillingWithNullsIfNecessary(List<T> list, int size) {
        if (list==null) {
            list= Collections.nCopies(size,  null);
        } else if (list.size()<size) {
            list = new ArrayList(list); // make it mutable in case it isn't
            list.addAll(Collections.nCopies(size-list.size(),null));
        }
        return list;
    }

}
