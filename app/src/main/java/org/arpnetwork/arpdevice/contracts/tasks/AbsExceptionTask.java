/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arpnetwork.arpdevice.contracts.tasks;

import android.os.AsyncTask;

public abstract class AbsExceptionTask<Params, Progress, Result> extends AsyncTask<Params, Progress, AbsExceptionTask.AsyncTaskResult<Result>> {
    private OnValueResult<Result> onResult;

    public AbsExceptionTask(OnValueResult<Result> onValueResult) {
        onResult = onValueResult;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (onResult != null) {
            onResult.onPreExecute();
        }
    }

    @Override
    protected AsyncTaskResult<Result> doInBackground(Params... params) {
        Result res = null;
        try {
            res = onInBackground(params);
        } catch (Exception e) {
            return new AsyncTaskResult<>(e);
        }
        return new AsyncTaskResult<>(res);
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<Result> result) {
        if (isCancelled()) return;

        if (onResult != null && result.getError() != null) {
            onResult.onFail(result.getError());
        } else if (onResult != null) {
            onResult.onValueResult(result.getResult());
        }
    }

    public abstract Result onInBackground(Params... params) throws Exception;

    static class AsyncTaskResult<T> {
        private T result;
        private Exception error;

        public T getResult() {
            return result;
        }

        public Exception getError() {
            return error;
        }

        public AsyncTaskResult(T result) {
            this.result = result;
        }

        public AsyncTaskResult(Exception error) {
            this.error = error;
        }
    }
}


