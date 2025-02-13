/*
 *    Copyright 2016-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.dynamic.sql;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractListValueCondition<T> implements VisitableCondition<T> {
    protected final Collection<T> values;

    protected AbstractListValueCondition(Collection<T> values) {
        this.values = Objects.requireNonNull(values);
    }

    public final <R> Stream<R> mapValues(Function<T, R> mapper) {
        return values.stream().map(mapper);
    }

    @Override
    public boolean shouldRender() {
        return !values.isEmpty();
    }

    @Override
    public <R> R accept(ConditionVisitor<T, R> visitor) {
        return visitor.visit(this);
    }

    private <R> Collection<R> applyMapper(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return values.stream().map(mapper).collect(Collectors.toList());
    }

    private Collection<T> applyFilter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return values.stream().filter(predicate).collect(Collectors.toList());
    }

    protected <S extends AbstractListValueCondition<T>> S filterSupport(Predicate<? super T> predicate,
            Function<Collection<T>, S> constructor, S self, Supplier<S> emptySupplier) {
        if (shouldRender()) {
            Collection<T> filtered = applyFilter(predicate);
            return filtered.isEmpty() ? emptySupplier.get() : constructor.apply(filtered);
        } else {
            return self;
        }
    }

    protected <R, S extends AbstractListValueCondition<R>> S mapSupport(Function<? super T, ? extends R> mapper,
            Function<Collection<R>, S> constructor, Supplier<S> emptySupplier) {
        if (shouldRender()) {
            return constructor.apply(applyMapper(mapper));
        } else {
            return emptySupplier.get();
        }
    }

    /**
     * If renderable, apply the predicate to each value in the list and return a new condition with the filtered values.
     *     Else returns a condition that will not render (this). If all values are filtered out of the value
     *     list, then the condition will not render.
     *
     * @param predicate
     *            predicate applied to the values, if renderable
     *
     * @return a new condition with filtered values if renderable, otherwise a condition that will not render.
     */
    public abstract AbstractListValueCondition<T> filter(Predicate<? super T> predicate);

    public abstract String operator();
}
