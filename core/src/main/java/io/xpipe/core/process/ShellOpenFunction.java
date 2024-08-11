package io.xpipe.core.process;

import lombok.NonNull;

public interface ShellOpenFunction {

    static ShellOpenFunction unsupported() {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static ShellOpenFunction of(String b) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return CommandBuilder.of().add(b);
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                return CommandBuilder.of().add(command);
            }
        };
    }

    static ShellOpenFunction of(CommandBuilder b) {
        return new ShellOpenFunction() {
            @Override
            public CommandBuilder prepareWithoutInitCommand() {
                return b;
            }

            @Override
            public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                return CommandBuilder.ofString(command);
            }
        };
    }

    CommandBuilder prepareWithoutInitCommand() throws Exception;

    CommandBuilder prepareWithInitCommand(@NonNull String command) throws Exception;
}
