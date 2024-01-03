package de.mr_pine.doctex

typealias Stack<T> = ArrayDeque<T>
fun <T> Stack<T>.push(element: T) {
    addFirst(element);
}

fun <T> Stack<T>.pop() = removeFirst()

fun <T> Stack<T>.peek() = first()