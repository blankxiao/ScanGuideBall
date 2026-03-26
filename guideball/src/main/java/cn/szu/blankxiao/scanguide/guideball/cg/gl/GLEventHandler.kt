package cn.szu.blankxiao.scanguide.guideball.cg.gl

import java.util.LinkedList

/**
 * GL 线程内串行执行的任务队列（与 Panorama 的 [GLEventHandler] 同构）。
 */
internal class GLEventHandler {

	private val queue = LinkedList<Runnable?>()

	fun dequeueEventAndRun() {
		synchronized(queue) {
			while (queue.isNotEmpty()) {
				queue.removeFirst()!!.run()
			}
		}
	}

	fun enqueueEvent(runnable: Runnable?) {
		synchronized(queue) {
			queue.addLast(runnable)
		}
	}
}
