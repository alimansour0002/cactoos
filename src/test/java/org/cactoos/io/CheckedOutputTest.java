/*
 * The MIT License (MIT)
 *
 * SPDX-FileCopyrightText: Copyright (c) 2017-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package org.cactoos.io;

import java.io.IOException;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Assertion;
import org.llorllale.cactoos.matchers.Throws;

/**
 * Test case for {@link CheckedOutput}.
 *
 * @since 0.31
 * @checkstyle JavadocMethodCheck (500 lines)
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
final class CheckedOutputTest {

    @Test
    void runtimeExceptionIsNotWrapped() {
        new Assertion<>(
            "must not wrap runtime exception",
            new CheckedOutput<>(
                () -> {
                    throw new IllegalStateException("runtime1");
                },
                IOException::new
            )::stream,
            new Throws<>(IllegalStateException.class)
        ).affirm();
    }

    @Test
    void checkedExceptionIsWrapped() {
        new Assertion<>(
            "must wrap checked exception",
            new CheckedOutput<>(
                () -> {
                    throw new IOException("runtime2");
                },
                IOException::new
            )::stream,
            new Throws<>(IOException.class)
        ).affirm();
    }

    @Test
    void extraWrappingIgnored() {
        try {
            new CheckedOutput<>(
                () -> {
                    throw new IOException("runtime3");
                },
                IOException::new
            ).stream();
        } catch (final IOException exp) {
            new Assertion<>(
                "must not add extra wrapping of IOException",
                exp.getCause(),
                new IsNull<>()
            ).affirm();
        }
    }
}
