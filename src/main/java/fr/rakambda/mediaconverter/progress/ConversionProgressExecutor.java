package fr.rakambda.mediaconverter.progress;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

@Log4j2
public class ConversionProgressExecutor implements ExecutorService, AutoCloseable {
    private final ExecutorService delegate;
    private final ProgressBar progressBar;
    private final Object progressBarLock;

    public ConversionProgressExecutor(@NonNull ExecutorService executorService) {
        delegate = executorService;
        progressBar = new ProgressBarBuilder()
                .setTaskName("Conversion")
                .setInitialMax(-1)
                .build();
        progressBarLock = new Object();
    }

    public static ConversionProgressExecutor of(@NonNull ExecutorService executorService) {
        return new ConversionProgressExecutor(executorService);
    }

    @NonNull
    public static <T> CompletableFuture<T> makeCompletableFuture(@NonNull Future<T> future) {
        if (future.isDone()) {
            return transformDoneFuture(future);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!future.isDone()) {
                    awaitFutureIsDoneInForkJoinPool(future);
                }
                return future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    @NonNull
    private static <T> CompletableFuture<T> transformDoneFuture(@NonNull Future<T> future) {
        var cf = new CompletableFuture<T>();
        try {
            var result = future.get();
            cf.complete(result);
            return cf;
        } catch (Throwable ex) {
            cf.completeExceptionally(ex);
            return cf;
        }
    }

    private static void awaitFutureIsDoneInForkJoinPool(@NonNull Future<?> future) throws InterruptedException {
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
            @Override
            public boolean block() throws InterruptedException {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }

            @Override
            public boolean isReleasable() {
                return future.isDone();
            }
        });
    }

    @Override
    public void close() {
        try {
            shutdown();
            awaitTermination(30, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.error("Failed to wait for executor to close");
        } finally {
            progressBar.close();
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        var neverRun = delegate.shutdownNow();
        stepProgressBar(neverRun.size());
        return neverRun;
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        incrementProgressbarMax();
        return makeCompletableFuture(delegate.submit(task)).thenApply(val -> {
            stepProgressBar(1);
            return val;
        });
    }

    private void incrementProgressbarMax() {
        synchronized (progressBarLock) {
            progressBar.maxHint(progressBar.getMax() + 1);
        }
    }

    private void stepProgressBar(long amount) {
        synchronized (progressBarLock) {
            progressBar.stepBy(amount);
        }
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        incrementProgressbarMax();
        return makeCompletableFuture(delegate.submit(task, result)).thenApply(val -> {
            stepProgressBar(1);
            return val;
        });
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        incrementProgressbarMax();
        return makeCompletableFuture(delegate.submit(task))
                .exceptionally(throwable -> {
                    stepProgressBar(1);
                    throw new CompletionException(throwable);
                })
                .thenApply(val -> {
                    stepProgressBar(1);
                    return val;
                });
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) {
        throw new NotImplementedException();
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) {
        throw new NotImplementedException();
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) {
        throw new NotImplementedException();
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) {
        throw new NotImplementedException();
    }

    @Override
    public void execute(@NonNull Runnable command) {
        throw new NotImplementedException();
    }
}
