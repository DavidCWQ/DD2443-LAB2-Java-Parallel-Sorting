/**
 * Sort using Java's ExecutorService.
 */

import java.util.concurrent.*;

/**
 * ExecutorService provides a powerful and flexible way to manage and execute tasks
 * asynchronously in Java. It simplifies threading issues by managing a pool of threads
 * and offers methods to submit tasks, obtain results, and shut down the service gracefully.
 * */

public class ExecutorServiceSort implements Sorter {

        public final int threads;
        private final ExecutorService executor;

        public ExecutorServiceSort(int threads) {
                // Create a thread pool with the specified number of threads in Constructor!
                this.threads = threads;
                this.executor = Executors.newFixedThreadPool(threads);
        }

        public void sort(int[] arr) {
                try {
                        myQuickSort(arr, 0, arr.length - 1);
                } finally {
                        executor.shutdown();
                }
        }

        public int getThreads() {
                return threads;
        }

        private boolean hasAvailableThreads() {
            // Check if there are available threads in the pool
            ThreadPoolExecutor TPExecutor = (ThreadPoolExecutor) executor;
            return TPExecutor.getActiveCount() < threads;
        }

        private void myQuickSort(int[] array, int low, int high) {
            if (low < high) {
                // Switch to Sequential Sort for small arrays
                int THRESHOLD = 16;
                if (high - low + 1 <= THRESHOLD) {
                    SequentialSort.quickSort(array, low, high);
                }

                else {
                    int pivot = SequentialSort.partition(array, low, high);

                    Callable<Void> leftSortTask = () -> {
                        myQuickSort(array, low, pivot - 1);
                        return null;
                    };

                    Callable<Void> rightSortTask = () -> {
                        myQuickSort(array, pivot + 1, high);
                        return null;
                    };

                    try {
                        // Run the tasks directly if threads are not available
                        /* Otherwise If the tasks recursively submit more child tasks and
                           there aren't enough threads to execute them, it can lead to the
                           deadlock where child tasks are waiting for parent tasks to finish,
                           but the parent tasks are waiting for the child tasks to complete.
                           <p>
                           BUG: Child tasks would never be scheduled to run because there
                           would be no available worker threads.
                        */
                        if (executor.isShutdown() || !hasAvailableThreads()) {
                            // Run tasks directly in current thread
                            leftSortTask.call();
                            rightSortTask.call();
                        } else {
                            // Otherwise, submit tasks to the executor service
                            Future<?> leftFuture = executor.submit(leftSortTask);
                            Future<?> rightFuture = executor.submit(rightSortTask);

                            // Wait for right to complete [Bug Fixed]
                            // Otherwise, it may cause only part of the array to be sorted.
                            rightFuture.get();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        private static class Worker implements Runnable {
                Worker(int[] array, int low, int high) {
                }

                public void run() {
                }
        }
}
