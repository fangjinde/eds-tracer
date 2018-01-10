package com.netease.edu.boot.hystrix.core.exception;/**
 * Created by hzfjd on 18/1/10.
 */

/**
 * @author hzfjd
 * @create 18/1/10
 */
public class CommandExecuteException extends RuntimeException {

    private Object result;

    public <R> CommandExecuteException withResult(R result){
        this.result=result;
        return this;
    }

    public <R> R getResult(){
        return (R)result;
    }

    public CommandExecuteException(){

    }

    public CommandExecuteException(Throwable cause){
        this("",cause);

    }

    public CommandExecuteException(String message){
        this(message,null);
    }

    public CommandExecuteException(String message,Throwable cause){
        super(message,cause);
    }


}
