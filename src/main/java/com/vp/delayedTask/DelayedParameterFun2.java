package com.vp.delayedTask;

import java.util.List;

@FunctionalInterface
public interface DelayedParameterFun2<R,P1>{
    List<R> apply (List<P1> param);
}