package com.vp.delayedTask;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Tuple {

    private AtomicBoolean resultCommitted = new AtomicBoolean();
    private Object result;
    private final Object[] argsAsArray;

    private Lock lock = new ReentrantLock();
    private Condition commitCondition = lock.newCondition();


    public Tuple(Object... argsAsArray) {
        this.resultCommitted.set(false);
        this.result = null;
        this.argsAsArray = argsAsArray;
    }

    void waitIfResultHasNotCommitted() {
        if (!resultCommitted.get()) {
            final Lock lockOnResult = lock;
            try {
                lockOnResult.lock();
                commitCondition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted waiting.  it shouldn't happen ever", e);
            } finally {
                lockOnResult.unlock();
            }
        }
    }

    void continueIfIsWaiting() {
        final Lock lockOnResult = lock;
        lockOnResult.lock();
        try {
            commitCondition.signal();
        } finally {
            lockOnResult.unlock();
        }

    }

    void setResult(Object result) {
        this.result = result;
    }

    void commitResult() {
        this.resultCommitted.getAndSet(true);

    }

    public Object getResult() {
        return result;
    }

    int getArgsSize() {
        return argsAsArray.length;
    }


    Object getArgumentByPosition(int argPosition) {
        return argsAsArray[argPosition];
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "resultCommitted=" + resultCommitted +
                ", result=" + result +
                ", argsAsArray=" + Arrays.toString(argsAsArray) +
                '}';
    }
}
