package io.github.lumkit.sweeteditor.highlight.internal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SerialAnalyzeQueueTest {
    @Test
    fun submitDoesNotRunOnCallerThread() {
        val generation = 1
        val queue = SerialAnalyzeQueue { generation }
        val caller = Thread.currentThread()
        val ranOn = AtomicReference<Thread>()
        val latch = CountDownLatch(1)
        try {
            queue.submit(generation) {
                ranOn.set(Thread.currentThread())
                latch.countDown()
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            val worker = checkNotNull(ranOn.get())
            assertNotEquals(caller, worker)
            assertEquals("sweetline-analyze", worker.name)
        } finally {
            queue.close()
        }
    }

    @Test
    fun staleGenerationIsDroppedBeforeAndInsideBlock() {
        var generation = 1
        val queue = SerialAnalyzeQueue { generation }
        val first = CountDownLatch(1)
        val done = CountDownLatch(1)
        var secondRan = false
        try {
            queue.submit(1) {
                generation = 2
                first.countDown()
            }
            queue.submit(1) {
                secondRan = true
                done.countDown()
            }
            assertTrue(first.await(5, TimeUnit.SECONDS))
            assertTrue(!done.await(200, TimeUnit.MILLISECONDS))
            assertTrue(!secondRan)
        } finally {
            queue.close()
        }
    }
}
