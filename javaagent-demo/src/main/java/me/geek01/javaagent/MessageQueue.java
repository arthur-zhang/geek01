package me.geek01.javaagent;

import com.google.common.collect.Queues;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created By Arthur Zhang at 08/06/2017
 */
public class MessageQueue {
    
    private static final int SIZE = 5000;
    private static final int BATCH_FETCH_ITEM_COUNT = 200;
    private static final int MAX_WAIT_TIMEOUT = 30;
    private BlockingQueue<Record> queue = new LinkedBlockingQueue<>(SIZE);
    
    private MessageQueue() {
    }
    
    public int availableSize() {
        return queue.remainingCapacity();
    }
    
    private static class SingletonHolder {
        private static MessageQueue helper = new MessageQueue();
    }
    
    public static MessageQueue getInstance() {
        return SingletonHolder.helper;
    }
    
    public boolean add(final Record logItem) {
        return queue.offer(logItem);
    }
    
    public List<Record> drain() {
        List<Record> bulkData = new ArrayList<>();
        try {
            Queues.drain(queue, bulkData, BATCH_FETCH_ITEM_COUNT, MAX_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
        return bulkData;
    }
}
