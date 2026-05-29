package com.lld.realworldexamples.LRUCache;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {
    private int capacity;
    private DoublyLinkedList<K, V> list;
    private Map<K, Node<K, V>> map;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.list = new DoublyLinkedList<>();
        this.map = new HashMap<>();

    }

    public synchronized V get(K key) {
        if (!map.containsKey(key)) {
            return null;
        }
        Node<K, V> node = map.get(key);
        list.moveToFront(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        if (map.containsKey(key)) {
            Node<K, V> node = map.get(key);
            node.value = value;
            list.moveToFront(node);
        }

        if (map.size() == capacity) {
            Node<K, V> last = list.removeLast();
            map.remove(last.key);
        }
        Node<K, V> newNode = new Node<>(key, value);
        map.put(key, newNode);
        list.addFirst(newNode);
    }

}
