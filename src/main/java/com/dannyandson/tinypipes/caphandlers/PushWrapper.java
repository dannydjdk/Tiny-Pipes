package com.dannyandson.tinypipes.caphandlers;

import com.dannyandson.tinypipes.components.AbstractCapPipe;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

public class PushWrapper<T> {
    private final SortedSet<PushTarget<T>> pushTargets = new TreeSet<>();

    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private final long id = NEXT_ID.getAndIncrement();
    public long getId() {
        return id;
    }

    public void addPushTarget(T target, AbstractCapPipe<T> pipe, int distance) {
        addPushTarget(target, pipe, distance, 0);
    }

    public void addPushTarget(T target, AbstractCapPipe<T> pipe, int distance, int priority) {
        pushTargets.add(new PushTarget<>(target, pipe, distance, priority));
    }

    public Set<PushTarget<T>> getSortedTargets() {
        return pushTargets;
    }

    public static class PushTarget<T> implements Comparable<PushTarget<T>>{
        private final T target;
        private final AbstractCapPipe<T> pipe;
        private final int distance;
        private final int priority;

        private PushTarget(T capability, AbstractCapPipe<T> pipe, int distance, int priority){
            this.target =capability;
            this.pipe=pipe;
            this.distance=distance;
            this.priority=priority;
        }

        public T getTarget()
        {
             return target;
        }

        public AbstractCapPipe<T> getPipe() {
            return pipe;
        }

        /**
         * Compares this object with the specified object for order.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        @Override
        public int compareTo(PushTarget o) {
            if (this.priority!=o.priority)
                return o.priority-this.priority;
            if (this.distance==o.distance)
                return this.pipe.hashCode()-o.pipe.hashCode();
            return this.distance-o.distance;
        }
    }
}
