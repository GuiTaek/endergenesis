package com.gmail.guitaekm.endergenesis.blocks;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class TreeTraverser<T> {
    private final List<TreeTraverser<T>> children;
    private final T vertex;
    private static int firstUnusedId = 0;
    public final int id;

    protected TreeTraverser(T vertex, List<TreeTraverser<T>> children) {
        this.children = children;
        this.vertex = vertex;
        this.id = firstUnusedId++;
    }
    public List<TreeTraverser<T>> children() {
        return Collections.unmodifiableList(this.children);
    }
    public static <T> TreeTraverser<T> createFromList(List<Integer> childList, List<T> valList) {
        final int[] oldEnd = {0};
        return  TreeTraverser
                .parseVertex(
                    0,
                    ind -> {
                        int start = oldEnd[0] + 1;
                        int end = oldEnd[0] + childList.get(ind);
                        if (start > end) {
                            return new ArrayList<>();
                        }
                        oldEnd[0] = end;
                        return IntStream.rangeClosed(start, end).boxed().toList();
                    }
                ).mapValue(
                    valList::get
                );
    }
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        result.add(this.vertex);
        this.breadthFirstSearch((parent, child) -> result.add(child));
        return result;
    }

    public List<Integer> toChildList() {
        return this.mapVertex(vertex -> vertex.children().size()).toList();
    }

    public T getVertex() {
        return vertex;
    }

    public TreeTraverser<T> addChild(T vertex) {
        TreeTraverser<T> child = new TreeTraverser<>(vertex, new ArrayList<>());
        this.children.add(child);
        return child;
    }

    public static <T> TreeTraverser<T> parseVertex(
            T root,
            Function<T, List<T>> childExtractor
    ) {
        List<TreeTraverser<T>> children = childExtractor
                .apply(root)
                .stream()
                .map(
                        (T child) -> TreeTraverser.parseVertex(
                                child,
                                childExtractor
                        )
                ).toList();

        return new TreeTraverser<>(root, children);
    }

    public void depthFirstSearch(BiConsumer<T, T> consumeRelations) {
        for (TreeTraverser<T> child : this.children) {
            child.depthFirstSearch(consumeRelations);
            consumeRelations.accept(this.vertex, child.vertex);
        }
    }

    public void breadthFirstSearch(BiConsumer<T, T> consumeRelations) {
        Queue<TreeTraverser<T>> queue = new ArrayDeque<>();
        queue.add(this);
        while(!queue.isEmpty()) {
            TreeTraverser<T> parent = queue.remove();
            parent.children.forEach(
                    child -> {
                        consumeRelations.accept(parent.vertex, child.vertex);
                        queue.add(child);
                    }
            );
        }
    }

    public <R> TreeTraverser<R> mapValue(Function<T, R> func) {
        List<TreeTraverser<R>> children = this.children.stream().map(
                (TreeTraverser<T> child) -> (child.mapValue(func))
        ).toList();

        return new TreeTraverser<>(func.apply(this.vertex), children);
    }

    public <R> TreeTraverser<R> mapVertex(Function<TreeTraverser<T>, R> func) {
        List<TreeTraverser<R>> children = this.children.stream().map(
                child -> child.mapVertex(func)
        ).toList();
        return new TreeTraverser<>(func.apply(this), children);
    }

    public void pruneNulls() {
        this.children.removeIf(
                child -> child.vertex == null
        );
        for (TreeTraverser<T> child : this.children) {
            child.pruneNulls();
        }
    }
}
