package org.arpnetwork.arpdevice.contracts.tasks;

public abstract class SimpleOnValueResult<T> implements OnValueResult<T> {
    @Override
    public void onPreExecute() {
    }

    @Override
    public abstract void onValueResult(T result);

    @Override
    public abstract void onFail(Throwable throwable);
}
