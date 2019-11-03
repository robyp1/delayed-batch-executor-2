package com.vp.delayedTask;


import java.time.Duration;
import java.util.List;

public class DelayedTask2Executor<R,P1> extends  DelayedTasks{

    private DelayedParameterFun2 userFunctionToBeInvoked;


    public  DelayedTask2Executor(Duration windowTime, int size, DelayedParameterFun2<R, P1> userFunctionToBeInvoked) {
        super(windowTime, size);
        this.userFunctionToBeInvoked = userFunctionToBeInvoked;
    }


    @Override
    <R, P1> R execute(P1 param) {
        return (R) super.executeWithArgs(param).getResult();
    }

    @Override
    protected List<Object> getResultFromTupleList(TupleListArgs tupleListArgs) {
        return  userFunctionToBeInvoked.apply(tupleListArgs.getArgsList(0));
    }
}
